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

        // Pasul 1: Fortam deconectarea utilizatorului la fiecare lansare a aplicatiei
        // Astfel, ecranul de Login va aparea de fiecare data, indiferent de sesiunile vechi
        Firebase.auth.signOut()

        setContent {
            FindCoffeeTheme {
                // Pasul 2: Definim starea pentru a urmari daca login-ul a reusit
                // Initial este null deoarece am dat signOut mai sus
                var isLoggedIn by remember { mutableStateOf(false) }

                if (!isLoggedIn) {
                    // Afisam ecranul de Login
                    // Cand login-ul reuseste in LoginScreen, acesta apeleaza onLoginSuccess
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
     * Folosim finish() pentru a scoate MainActivity din stiva de activitati,
     * astfel incat utilizatorul sa nu revina la login apasand butonul 'Back'.
     */
    private fun navigateToConnect() {
        val intent = Intent(this, ConnectActivity::class.java)
        startActivity(intent)
        finish()
    }
}
