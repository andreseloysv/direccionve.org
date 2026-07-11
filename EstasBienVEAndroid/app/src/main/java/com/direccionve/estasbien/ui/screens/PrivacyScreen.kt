package com.direccionve.estasbien.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direccionve.estasbien.R
import com.direccionve.estasbien.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onExportCsv: suspend () -> String,
    onDeleteData: suspend () -> Unit,
    onDataDeleted: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val saveFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && pendingCsv != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(pendingCsv!!.toByteArray()) }
            Toast.makeText(context, "Datos guardados", Toast.LENGTH_SHORT).show()
        }
        pendingCsv = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = AccentPurple,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.privacy_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy policy card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.privacy_policy_heading),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.privacy_policy_body),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data collected card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.privacy_data_collected_heading),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.privacy_data_collected_body),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Project info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.privacy_project_heading),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.privacy_project_body),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Export CSV button
        OutlinedButton(
            onClick = {
                scope.launch {
                    val csv = onExportCsv()
                    if (csv.isNotBlank()) {
                        pendingCsv = csv
                        saveFileLauncher.launch("estasbien_datos.csv")
                    } else {
                        Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_export_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Delete data button
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusHelp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.privacy_delete_button))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.privacy_delete_warning),
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Confirm delete dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    stringResource(R.string.privacy_delete_confirm_title),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.privacy_delete_confirm_body),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            onDeleteData()
                            onDataDeleted()
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.privacy_delete_confirm_yes),
                        color = StatusHelp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(
                        stringResource(R.string.privacy_delete_confirm_no),
                        color = AccentBlue
                    )
                }
            }
        )
    }
}
