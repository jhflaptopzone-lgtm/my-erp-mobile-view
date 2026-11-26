package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.SyncBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    uiState: ErpUiState,
    firebaseUserEmail: String?,
    onLogin: (accessId: String, password: String, serverUrl: String) -> Unit,
    onGoogleAuthSuccess: () -> Unit,
    onGoogleAuthError: (String) -> Unit,
    onOpenPublicInvoice: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var accessId by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var serverUrl by rememberSaveable(uiState.serverUrl) { mutableStateOf(uiState.serverUrl) }
    var showServerEditor by rememberSaveable { mutableStateOf(false) }
    var googleLoading by remember { mutableStateOf(false) }

    val fullTitle = "JHF LAPTOP ZONE"
    var typedCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..fullTitle.length) {
            typedCount = i
            delay(55)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art),
                    contentDescription = "JHF Laptop Zone Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = fullTitle.take(typedCount) + if (typedCount < fullTitle.length) "_" else "",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonGreen,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mobile Inventory, Sales & EMI Terminal",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            SyncBadge(connected = uiState.healthConnected)

            Spacer(modifier = Modifier.height(20.dp))

            JhfCard(accentBorder = NeonGreen.copy(alpha = 0.25f)) {
                Text(
                    text = "Operator Sign In",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Changes made on mobile sync live to the web portal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                JhfTextField(
                    value = accessId,
                    onValueChange = { accessId = it },
                    label = "Access ID",
                    placeholder = "e.g. jhfadmin",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen) },
                    testTag = "login_access_id_input"
                )

                Spacer(modifier = Modifier.height(12.dp))

                JhfTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NeonGreen) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password",
                                tint = TextSecondary
                            )
                        }
                    },
                    testTag = "login_password_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showServerEditor = !showServerEditor }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = serverUrl,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = if (showServerEditor) "Hide URL" else "Edit URL",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showServerEditor) {
                    Spacer(modifier = Modifier.height(6.dp))
                    JhfTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = "Live Web Sync URL",
                        placeholder = "https://jhflaptop.cloud.run",
                        keyboardType = KeyboardType.Uri,
                        testTag = "login_server_url_input"
                    )
                }

                if (!uiState.authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DangerRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.authError,
                            color = DangerRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.authLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(28.dp))
                    }
                } else {
                    NeonButton(
                        text = "Sign In & Connect Web",
                        onClick = { onLogin(accessId, password, serverUrl) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "login_submit_button"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (firebaseUserEmail != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonGreen.copy(alpha = 0.08f))
                            .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Cloud Linked: $firebaseUserEmail",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    DarkOutlineButton(
                        text = if (googleLoading) "Linking Google Cloud..." else "Link Google Cloud Account",
                        onClick = {
                            if (!googleLoading) {
                                googleLoading = true
                                GoogleAuthHelper.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onAuthSuccess = {
                                        googleLoading = false
                                        onGoogleAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        googleLoading = false
                                        onGoogleAuthError(msg)
                                    },
                                    onAuthCancelled = { googleLoading = false }
                                )
                            }
                        },
                        icon = Icons.Default.CloudQueue,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "google_signin_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onOpenPublicInvoice,
                modifier = Modifier.testTag("public_invoice_lookup_button")
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Public Customer Invoice Lookup", color = NeonGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
