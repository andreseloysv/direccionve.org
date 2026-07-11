package com.direccionve.estasbien.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direccionve.estasbien.R
import com.direccionve.estasbien.data.PlusCodeUtils
import com.direccionve.estasbien.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegister: (name: String, phone: String, plusCode: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var plusCode by remember { mutableStateOf("") }
    var locationLoading by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var autoDetectDone by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Function to detect phone number — pre-fill country code from SIM
    fun detectPhoneNumber() {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Try to get actual number (rarely works on modern carriers)
            var number: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    number = sm.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                } catch (_: Exception) {}
            }
            if (number.isNullOrBlank()) {
                number = tm.line1Number
            }

            if (!number.isNullOrBlank() && number.length > 4) {
                phone = number
            } else {
                // At least pre-fill country code from SIM
                val countryIso = tm.simCountryIso?.uppercase()
                if (phone.isBlank()) {
                    phone = when (countryIso) {
                        "VE" -> "+58 "
                        "ES" -> "+34 "
                        "CO" -> "+57 "
                        "MX" -> "+52 "
                        "US" -> "+1 "
                        "AR" -> "+54 "
                        "CL" -> "+56 "
                        "PE" -> "+51 "
                        "EC" -> "+593 "
                        "BR" -> "+55 "
                        else -> "+"
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Function to detect location
    fun detectLocation() {
        try {
            locationLoading = true
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                plusCode = PlusCodeUtils.encode(location.latitude, location.longitude)
                locationError = null
            } else {
                locationError = "Activa el GPS e intenta de nuevo"
            }
        } catch (_: Exception) {
            locationError = "No se pudo obtener ubicación"
        } finally {
            locationLoading = false
        }
    }

    // Phone-only permission launcher
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) detectPhoneNumber()
    }

    // Location-only permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) detectLocation()
        else locationError = "Permiso de ubicación denegado"
    }

    // Combined permissions launcher — for initial auto-detect
    val allPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val phoneGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true ||
                permissions[Manifest.permission.READ_PHONE_NUMBERS] == true

        if (locationGranted) detectLocation()
        if (phoneGranted) detectPhoneNumber()

        autoDetectDone = true
    }

    // Trigger auto-detect once the screen is visible (not during composition)
    LaunchedEffect(Unit) {
        delay(300)
        if (!autoDetectDone) {
            allPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Logo — white text on VE flag gradient background
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(VeYellow, VeBlue, VeRed)
                    )
                )
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text(
                text = "¿EstásBien?",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.app_tagline),
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Title
        Text(
            text = stringResource(R.string.register_subtitle),
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.register_name_label)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = CardDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone field (country code auto-detected from SIM)
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
            label = { Text(stringResource(R.string.register_phone_label)) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            placeholder = { Text("+58 4XX XXXXXXX", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = CardDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GPS Location button (replaces manual Plus Code input)
        OutlinedButton(
            onClick = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
        ) {
            if (locationLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentBlue, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = null)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (plusCode.isNotBlank()) "📍 $plusCode" else "Obtener mi ubicación",
                fontSize = 14.sp
            )
        }

        if (locationError != null) {
            Text(
                text = locationError!!,
                color = StatusHelp,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Register button
        Button(
            onClick = {
                onRegister(name.trim(), phone.trim(), plusCode.trim())
            },
            enabled = name.isNotBlank() && phone.length >= 7 && plusCode.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple
            )
        ) {
            Text(
                text = stringResource(R.string.register_button),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.register_privacy),
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
