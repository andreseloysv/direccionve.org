package com.direccionve.estasbien.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direccionve.estasbien.ui.theme.*
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 5

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var termsAccepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = if (pagerState.currentPage == 2) termsAccepted else true
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> RemarksPage()
                2 -> TermsPage(accepted = termsAccepted, onAcceptChange = { termsAccepted = it })
                3 -> NotificationPage()
                4 -> LocationPage()
            }
        }

        // Dots
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(PAGE_COUNT) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pagerState.currentPage) AccentPurple
                            else TextSecondary.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation buttons
        val isLast = pagerState.currentPage == PAGE_COUNT - 1
        val canAdvance = pagerState.currentPage != 2 || termsAccepted

        if (isLast) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Comenzar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(Modifier.fillMaxWidth()) {
                if (pagerState.currentPage < 2) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(2) } }) {
                        Text("Saltar", color = TextSecondary)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    enabled = canAdvance,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Siguiente")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WelcomePage() {
    ScrollablePage {
        Text("🌍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "¿Qué es EstasBien?",
            color = TextPrimary, fontSize = 24.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "EstasBien es una app de prevención y respuesta ante desastres sísmicos. " +
                "Cuando ocurre un terremoto cerca de ti, te preguntamos: ¿Estás bien?\n\n" +
                "Tu respuesta ayuda a los equipos de rescate a saber dónde están las personas que necesitan ayuda.\n\n" +
                "Es parte del proyecto DirecciónVE (direccionve.org) — infraestructura digital soberana para Venezuela.",
            color = TextSecondary, fontSize = 15.sp,
            lineHeight = 24.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RemarksPage() {
    ScrollablePage {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Aviso importante",
            color = TextPrimary, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "EstasBien ha sido desarrollada para proporcionar información sobre desastres como " +
                "terremotos, tsunamis, erupciones volcánicas, inundaciones, deslizamientos y otros " +
                "eventos que pueden ocurrir en Venezuela, con el fin de ayudar a las personas que " +
                "viven o se encuentran en zonas de riesgo a reconocer la situación con precisión y " +
                "tomar decisiones y acciones rápidas.\n\n" +
                "Esta app NO garantiza la seguridad de las personas que la usan. La decisión de " +
                "evacuar y la acción de evacuar son responsabilidad exclusiva de cada individuo. " +
                "Por favor, tome sus propias decisiones y actúe según la información proporcionada " +
                "por las autoridades locales y la situación a su alrededor.\n\n" +
                "No dependa únicamente de la información del gobierno, medios de comunicación o esta " +
                "aplicación para la protección de su vida. Las observaciones y pronósticos tienen " +
                "limitaciones técnicas y no hay garantía de que la información sea siempre correcta.\n\n" +
                "En caso de desastre, no espere a que la información le llegue: observe su entorno " +
                "y actúe por cuenta propia. Esperamos que la información de esta app le sea útil y " +
                "que pueda salvar la mayor cantidad de vidas posible.",
            color = TextSecondary, fontSize = 13.sp,
            lineHeight = 20.sp, textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun TermsPage(accepted: Boolean, onAcceptChange: (Boolean) -> Unit) {
    ScrollablePage {
        Text("📋", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Términos de servicio y\nPolítica de privacidad",
            color = TextPrimary, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Términos de servicio:\n\n" +
                "• EstasBien se proporciona \"tal cual\", sin garantías de ningún tipo.\n" +
                "• El uso de la app es voluntario y bajo la responsabilidad del usuario.\n" +
                "• No nos hacemos responsables de daños derivados del uso o imposibilidad de uso.\n" +
                "• Nos reservamos el derecho de modificar o interrumpir el servicio.\n\n" +
                "Política de privacidad:\n\n" +
                "• Recopilamos: nombre, teléfono, ubicación general (Plus Code) y estado de respuesta.\n" +
                "• Tus datos se usan ÚNICAMENTE para alertarte durante emergencias sísmicas y " +
                "mostrar tu estado en el mapa de respuestas.\n" +
                "• Solo tu ubicación general se envía al servidor, no tu ubicación detallada.\n" +
                "• Tus datos nunca serán vendidos ni compartidos con terceros.\n" +
                "• Puedes descargar o eliminar todos tus datos en cualquier momento desde la app.\n\n" +
                "Este es un proyecto de código abierto bajo licencia MIT. Puedes revisar todo el código en GitHub.",
            color = TextSecondary, fontSize = 13.sp,
            lineHeight = 20.sp, textAlign = TextAlign.Start
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptChange,
                colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Acepto los términos de servicio y la política de privacidad",
                color = TextPrimary, fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NotificationPage() {
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, user continues */ }

    ScrollablePage {
        Text("🔔", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Permisos de notificación",
            color = TextPrimary, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "La información importante se entregará mediante notificaciones push basadas en " +
                "tu ubicación, el tipo de desastre y el nivel de urgencia.\n\n" +
                "Si un sismo ocurre cerca de ti, recibirás una notificación inmediata preguntando " +
                "\"¿Estás bien?\" para que puedas responder con un solo toque.\n\n" +
                "Sin este permiso, la app no podrá alertarte durante una emergencia.",
            color = TextSecondary, fontSize = 15.sp,
            lineHeight = 24.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Permitir notificaciones")
        }
    }
}

@Composable
private fun LocationPage() {
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* granted or not, user continues */ }

    ScrollablePage {
        Text("📍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Permisos de ubicación",
            color = TextPrimary, fontSize = 22.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Para entregarte información precisa según tu ubicación, necesitamos acceso a tu " +
                "ubicación.\n\n" +
                "Si eliges \"Permitir siempre\", verificaremos periódicamente tu ubicación en " +
                "segundo plano para poder notificarte información relevante para tu ubicación actual.\n\n" +
                "Solo tu ubicación general será enviada a nuestros servidores, no tu ubicación detallada.\n\n" +
                "Esto nos permite saber si un sismo te afectó y mostrarte en el mapa de respuestas.",
            color = TextSecondary, fontSize = 15.sp,
            lineHeight = 24.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Permitir ubicación")
        }
    }
}

@Composable
private fun ScrollablePage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}
