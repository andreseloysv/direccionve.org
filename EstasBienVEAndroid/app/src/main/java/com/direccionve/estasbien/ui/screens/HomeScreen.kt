package com.direccionve.estasbien.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direccionve.estasbien.R
import com.direccionve.estasbien.data.model.UsgsFeature
import com.direccionve.estasbien.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private fun formatLastUpdate(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "Nunca"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        minutes < 1440 -> "Hace ${minutes / 60}h"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale("es", "VE")).format(Date(timestamp))
    }
}

@Composable
fun HomeScreen(
    userName: String,
    lastLocationUpdate: Long?,
    recentQuakes: List<UsgsFeature>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNameChange: (String) -> Unit,
    onPrivacy: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(userName) }

    // Sync when userName changes externally
    LaunchedEffect(userName) { editedName = userName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(VeYellow, VeBlue, VeRed)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "¿EstásBien?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Editable name
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditingName) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = CardDark,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    if (editedName.isNotBlank()) {
                        onNameChange(editedName.trim())
                    }
                    isEditingName = false
                }) {
                    Text("OK", color = AccentBlue)
                }
            } else {
                Text(
                    text = "Hola, $userName",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isEditingName = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Cambiar nombre", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StatusSafe,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_status_safe),
                        color = StatusSafe,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Alertas activas • Venezuela",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last location update
        Text(
            text = "📍 Ubicación actualizada: ${formatLastUpdate(lastLocationUpdate)}",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Recent earthquakes
        Text(
            text = stringResource(R.string.home_last_quake),
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(32.dp))
            }
        } else if (recentQuakes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Text(
                    text = stringResource(R.string.home_no_quakes),
                    color = TextSecondary,
                    modifier = Modifier.padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            recentQuakes.take(10).forEach { quake ->
                EarthquakeCard(quake)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Refresh
        TextButton(onClick = onRefresh, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Actualizar", color = AccentBlue)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy
        TextButton(onClick = onPrivacy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.privacy_title), color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EarthquakeCard(quake: UsgsFeature) {
    val mag = quake.properties.mag ?: 0.0
    val magColor = when {
        mag >= 6.0 -> StatusHelp
        mag >= 4.5 -> StatusMissing
        else -> StatusSafe
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale("es", "VE")) }
    val timeStr = dateFormat.format(Date(quake.properties.time))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Magnitude badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = magColor.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "%.1f".format(mag),
                        color = magColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quake.properties.place ?: "—",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$timeStr • ${"%.0f".format(quake.geometry.depthKm)} km prof.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
