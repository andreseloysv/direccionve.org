package com.direccionve.estasbien.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.direccionve.estasbien.data.model.UsgsFeature
import com.direccionve.estasbien.ui.screens.*

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Privacy : Screen("privacy")
    data object Alert : Screen("alert/{magnitude}/{place}") {
        fun createRoute(magnitude: String, place: String) =
            "alert/$magnitude/${place.take(50)}"
    }
}

@Composable
fun NavGraph(
    isOnboardingDone: Boolean,
    isRegistered: Boolean,
    userName: String,
    lastLocationUpdate: Long?,
    recentQuakes: List<UsgsFeature>,
    isLoading: Boolean,
    alertAction: String?,
    onRegister: (name: String, phone: String, plusCode: String) -> Unit,
    onRefresh: () -> Unit,
    onNameChange: (String) -> Unit,
    onSafe: () -> Unit,
    onHelp: () -> Unit,
    onExportCsv: suspend () -> String,
    onDeleteData: suspend () -> Unit,
    onOnboardingDone: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = when {
        !isOnboardingDone -> Screen.Onboarding.route
        !isRegistered -> Screen.Register.route
        else -> Screen.Home.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onDone = {
                    onOnboardingDone()
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegister = { name, phone, plusCode ->
                    onRegister(name, phone, plusCode)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                userName = userName,
                lastLocationUpdate = lastLocationUpdate,
                recentQuakes = recentQuakes,
                isLoading = isLoading,
                onRefresh = onRefresh,
                onNameChange = onNameChange,
                onPrivacy = { navController.navigate(Screen.Privacy.route) }
            )
        }

        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onBack = { navController.popBackStack() },
                onExportCsv = onExportCsv,
                onDeleteData = onDeleteData,
                onDataDeleted = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("alert/{magnitude}/{place}") { backStackEntry ->
            val magnitude = backStackEntry.arguments?.getString("magnitude") ?: "?"
            val place = backStackEntry.arguments?.getString("place") ?: ""
            var responded by remember { mutableStateOf(alertAction != null) }
            var isSafe by remember { mutableStateOf(alertAction == "safe") }

            AlertScreen(
                magnitude = magnitude,
                place = place,
                responded = responded,
                isSafe = if (responded) isSafe else null,
                onSafe = {
                    onSafe()
                    responded = true
                    isSafe = true
                },
                onHelp = {
                    onHelp()
                    responded = true
                    isSafe = false
                }
            )
        }
    }
}
