package com.direccionve.estasbien.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direccionve.estasbien.R
import com.direccionve.estasbien.ui.theme.*

@Composable
fun AlertScreen(
    magnitude: String,
    place: String,
    responded: Boolean,
    isSafe: Boolean?,
    onSafe: () -> Unit,
    onHelp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        StatusHelp.copy(alpha = 0.15f),
                        BgDark
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (responded) {
            // Response confirmed
            Icon(
                imageVector = if (isSafe == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSafe == true) StatusSafe else StatusHelp,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isSafe == true) {
                    stringResource(R.string.alert_responded_safe)
                } else {
                    stringResource(R.string.alert_responded_help)
                },
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else {
            // Alert — waiting for response
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.alert_title),
                color = StatusHelp,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "M$magnitude — $place",
                color = TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.alert_question),
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // "I'm safe" button
            Button(
                onClick = onSafe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusSafe)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.alert_yes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Need help" button
            OutlinedButton(
                onClick = onHelp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusHelp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.alert_no),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.alert_timeout_warning),
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
