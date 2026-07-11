package com.direccionve.estasbien

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.direccionve.estasbien.data.EarthquakeRepository
import com.direccionve.estasbien.data.PlusCodeUtils
import com.direccionve.estasbien.data.UserPreferences
import com.direccionve.estasbien.data.model.UserProfile
import com.direccionve.estasbien.data.model.UsgsFeature
import com.direccionve.estasbien.ui.navigation.NavGraph
import com.direccionve.estasbien.ui.theme.EstasBienTheme
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: UserPreferences
    private val repo = EarthquakeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = UserPreferences(this)

        // Handle notification actions
        val alertAction = intent.getStringExtra("action")
        val earthquakeId = intent.getStringExtra("earthquake_id")

        if (alertAction == "safe" && earthquakeId != null) {
            lifecycleScope.launch { prefs.saveAlertResponse(earthquakeId, true) }
        }

        setContent {
            var isOnboardingDone by remember { mutableStateOf(true) }
            var isRegistered by remember { mutableStateOf(false) }
            var userName by remember { mutableStateOf("") }
            var lastLocationUpdate by remember { mutableStateOf<Long?>(null) }
            var recentQuakes by remember { mutableStateOf<List<UsgsFeature>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }

            // Observe onboarding state
            LaunchedEffect(Unit) {
                prefs.isOnboardingDone.collect { done ->
                    isOnboardingDone = done
                }
            }

            // Observe registration state
            LaunchedEffect(Unit) {
                prefs.isRegistered.collect { registered ->
                    isRegistered = registered
                }
            }

            LaunchedEffect(Unit) {
                prefs.userProfile.collect { profile ->
                    userName = profile?.name ?: ""
                }
            }

            LaunchedEffect(Unit) {
                prefs.lastLocationUpdate.collect { ts ->
                    lastLocationUpdate = ts
                }
            }

            // Load recent earthquakes
            LaunchedEffect(Unit) {
                loadQuakes { quakes, loading ->
                    recentQuakes = quakes
                    isLoading = loading
                }
            }

            EstasBienTheme {
                NavGraph(
                    isOnboardingDone = isOnboardingDone,
                    isRegistered = isRegistered,
                    userName = userName,
                    lastLocationUpdate = lastLocationUpdate,
                    recentQuakes = recentQuakes,
                    isLoading = isLoading,
                    alertAction = alertAction,
                    onRegister = { name, phone, plusCode ->
                        lifecycleScope.launch {
                            val coords = PlusCodeUtils.decode(plusCode)
                            prefs.saveProfile(
                                UserProfile(
                                    name = name,
                                    phone = phone,
                                    plusCode = plusCode,
                                    latitude = coords?.latitude ?: 0.0,
                                    longitude = coords?.longitude ?: 0.0
                                )
                            )
                        }
                    },
                    onRefresh = {
                        lifecycleScope.launch {
                            loadQuakes { quakes, loading ->
                                recentQuakes = quakes
                                isLoading = loading
                            }
                        }
                    },
                    onNameChange = { newName ->
                        lifecycleScope.launch {
                            prefs.updateName(newName)
                        }
                    },
                    onSafe = {
                        lifecycleScope.launch {
                            earthquakeId?.let { prefs.saveAlertResponse(it, true) }
                        }
                    },
                    onHelp = {
                        lifecycleScope.launch {
                            earthquakeId?.let { prefs.saveAlertResponse(it, true) }
                            // TODO: Send help request to backend
                        }
                    },
                    onExportCsv = { prefs.exportCsv() },
                    onDeleteData = { prefs.clearAllData() },
                    onOnboardingDone = {
                        lifecycleScope.launch { prefs.setOnboardingDone() }
                    }
                )
            }
        }
    }

    private suspend fun loadQuakes(
        onResult: (List<UsgsFeature>, Boolean) -> Unit
    ) {
        onResult(emptyList(), true)
        val result = repo.getRecentQuakes(hoursBack = 72, minMagnitude = 3.0)
        onResult(result.getOrDefault(emptyList()), false)
    }
}
