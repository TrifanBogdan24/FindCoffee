package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Forteaza deconectare: ecranul de Login va aparea de fiecare data,
        // la lansarea aplicatiei, indiferent de sesiunile vechi
        Firebase.auth.signOut()

        setContent {
            FindCoffeeTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    // Ecranul de Login: dupa logare -> apeleaza onLoginSuccess
                    LoginScreen(onLoginSuccess = {
                        isLoggedIn = true
                        navigateToConnect()
                    })
                }
            }
        }
    }

    /**
     * Functie responsabila pentru navigarea catre ConnectActivity.
     * Apel finish() pentru a scoate MainActivity din stiva de activitati,
     * astfel incat utilizatorul sa nu revina la login apasand butonul 'Back'.
     */
    private fun navigateToConnect() {
        val intent = Intent(this, ConnectActivity::class.java)
        startActivity(intent)
        finish()
    }
}
