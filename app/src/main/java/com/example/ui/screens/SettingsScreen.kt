package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.SyncBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun SettingsScreen(
    uiState: ErpUiState,
    firebaseUserEmail: String?,
    onUpdateServerUrl: (String) -> Unit,
    onCheckHealth: () -> Unit,
    onForceCloudSync: () -> Unit,
    onRehydrateWeb: () -> Unit,
    onDownloadBackup: (onReady: (String, String) -> Unit) -> Unit,
    onRestoreBackup: (String) -> Unit,
    onResetDemoData: () -> Unit,
    onGoogleAuthSuccess: () -> Unit,
    onGoogleAuthError: (String) -> Unit,
    onGoogleSignOut: () -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val isAdmin = uiState.currentUser?.isAdmin == true

    var serverUrlDraft by remember(uiState.serverUrl) { mutableStateOf(uiState.serverUrl) }
    var googleBusy by remember { mutableStateOf(false) }
    var confirmResetStep1 by remember { mutableStateOf(false) }
    var confirmResetStep2 by remember { mutableStateOf(false) }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to read selected JSON file")
                onRestoreBackup(raw)
            }.onFailure {
                onNotify("Restore read failed: ${it.message}", true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Settings & Sync", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Live web sync, cloud audit & backups", color = TextSecondary, fontSize = 12.sp)
                }
                SyncBadge(connected = uiState.healthConnected)
            }
        }

        // Live Web Endpoint Card
        item {
            JhfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Web Sync Endpoint", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "All sales, inventory, EMI payments, suppliers, and staff edits sync directly with this live web URL.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                JhfTextField(
                    value = serverUrlDraft,
                    onValueChange = { serverUrlDraft = it },
                    label = "Server URL",
                    keyboardType = KeyboardType.Uri,
                    testTag = "settings_server_url_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonButton(
                        text = "Save URL",
                        onClick = { onUpdateServerUrl(serverUrlDraft) },
                        modifier = Modifier.weight(1f),
                        testTag = "settings_save_url_button"
                    )
                    DarkOutlineButton(
                        text = "Test Link",
                        onClick = onCheckHealth,
                        modifier = Modifier.weight(1f),
                        testTag = "settings_test_health_button"
                    )
                }
            }
        }

        // Firebase Google Cloud Account Card
        item {
            JhfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Cloud Audit Sync", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (firebaseUserEmail != null) {
                    Text("Linked Account: $firebaseUserEmail", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    DarkOutlineButton(
                        text = "Unlink Google Account",
                        icon = Icons.Default.Logout,
                        tint = DangerRed,
                        onClick = {
                            GoogleAuthHelper.signOut(credentialManager, scope) {
                                onGoogleSignOut()
                            }
                        },
                        testTag = "settings_google_signout_button"
                    )
                } else {
                    Text(
                        "Link your Google account to save workspace preferences and audit mobile sync events in Firebase Firestore.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NeonButton(
                        text = if (googleBusy) "Linking..." else "Link Google Cloud Account",
                        enabled = !googleBusy,
                        onClick = {
                            googleBusy = true
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                scope = scope,
                                onAuthSuccess = {
                                    googleBusy = false
                                    onGoogleAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    googleBusy = false
                                    onGoogleAuthError(msg)
                                },
                                onAuthCancelled = { googleBusy = false }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_google_signin_button"
                    )
                }

                if (uiState.firebaseSyncEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Recent Mobile Sync Audit", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    uiState.firebaseSyncEvents.take(6).forEachIndexed { idx, ev ->
                        if (idx > 0) HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("[${ev.action}] ${ev.summary}", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text(ev.entityType, color = NeonGreen, fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Admin-Only Web Cloud & Database Tools
        if (isAdmin) {
            item {
                JhfCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = InfoBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Web Cloud Mirror & Backup", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.cloudStatus?.let { st ->
                        Text(
                            text = if (st.connected) "Web Firestore Connected (${st.counts.entries.joinToString { "${it.key}: ${it.value}" }})"
                            else "Web Firestore Disconnected${st.error?.let { ": $it" } ?: ""}",
                            color = if (st.connected) NeonGreen else WarningYellow,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkOutlineButton(
                            text = "Force Cloud Sync",
                            icon = Icons.Default.CloudUpload,
                            tint = NeonGreen,
                            onClick = onForceCloudSync,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_force_cloud_sync"
                        )
                        DarkOutlineButton(
                            text = "Re-Hydrate Web",
                            icon = Icons.Default.CloudSync,
                            tint = InfoBlue,
                            onClick = onRehydrateWeb,
                            modifier = Modifier.weight(1f),
                            testTag = "settings_rehydrate_web"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkOutlineButton(
                            text = "Export JSON",
                            icon = Icons.Default.Backup,
                            onClick = {
                                onDownloadBackup { fileName, raw ->
                                    runCatching {
                                        val file = PdfGenerator.createJsonFile(context, fileName, raw)
                                        PdfGenerator.shareFile(context, file, "application/json", "Export JHF Backup")
                                        onNotify("Backup exported: $fileName", false)
                                    }.onFailure { onNotify("Backup export error: ${it.message}", true) }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_export_backup"
                        )
                        DarkOutlineButton(
                            text = "Restore JSON",
                            icon = Icons.Default.Restore,
                            tint = WarningYellow,
                            onClick = { restoreLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_restore_backup"
                        )
                    }

                    if (uiState.backupLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recent Backup Logs", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        uiState.backupLogs.take(5).forEach { log ->
                            Text(
                                "• ${log.fileName} (${Formatters.formatDateBD(log.backupDate)})",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    DarkOutlineButton(
                        text = "Reset Demo Data on Web Server",
                        icon = Icons.Default.DeleteForever,
                        tint = DangerRed,
                        onClick = { confirmResetStep1 = true },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_reset_demo_button"
                    )
                }
            }
        }
    }

    if (confirmResetStep1) {
        ConfirmActionDialog(
            title = "Reset Demo Data (Step 1/2)",
            message = "This will wipe live inventory, sales, installments, and suppliers on ${uiState.serverUrl} and restore initial seed data.",
            confirmLabel = "Continue",
            onConfirm = { confirmResetStep2 = true },
            onDismiss = { confirmResetStep1 = false }
        )
    }

    if (confirmResetStep2) {
        ConfirmActionDialog(
            title = "Final Confirmation (Step 2/2)",
            message = "Are you completely sure? All production sales and stock records on the live web portal will be replaced.",
            confirmLabel = "Reset Now",
            onConfirm = onResetDemoData,
            onDismiss = { confirmResetStep2 = false }
        )
    }
}
