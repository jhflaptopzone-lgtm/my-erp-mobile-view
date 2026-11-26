package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.GoogleAuthHelper
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.JhfTheme
import com.example.ui.viewmodel.ErpViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val erpViewModel: ErpViewModel = viewModel()
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(this) }

            var firebaseUid by remember { mutableStateOf(Firebase.auth.currentUser?.uid) }
            var firebaseEmail by remember { mutableStateOf(Firebase.auth.currentUser?.email) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    firebaseUid = auth.currentUser?.uid
                    firebaseEmail = auth.currentUser?.email
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose {
                    Firebase.auth.removeAuthStateListener(listener)
                }
            }

            LaunchedEffect(Unit) {
                GoogleAuthHelper.attemptAutoSignIn(
                    context = this@MainActivity,
                    credentialManager = credentialManager,
                    scope = scope,
                    onAuthSuccess = {
                        firebaseUid = Firebase.auth.currentUser?.uid
                        firebaseEmail = Firebase.auth.currentUser?.email
                    },
                    onUnauthenticated = {}
                )
            }

            JhfTheme {
                AppNavigation(
                    viewModel = erpViewModel,
                    firebaseUid = firebaseUid,
                    firebaseUserEmail = firebaseEmail
                )
            }
        }
    }
}
