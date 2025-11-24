package com.example.findcoffee.coroutines

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CheckInternetConnection() {
    var isConnected by remember { mutableStateOf(true) }

    // Coroutine pentru a verifica conexiunea periodic (la fiecare secunda)
    LaunchedEffect(Unit) {
        while (true) {
            isConnected = verifyInternetConnection()
            delay(1_000)
        }
    }

    if (!isConnected) {
        NoInternetDialog()
    }
}

@Composable
fun NoInternetDialog() {
    AlertDialog(
        onDismissRequest = { /* Block UI interaction until regain Internet connectivity */ },
        confirmButton = {},
        title = {
            Text(
                "Connection Lost",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(48.dp)
                )
                Text(
                    "Trying to reconnect...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

suspend fun verifyInternetConnection(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val url = URL("https://www.google.com")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 1000
        connection.connect()
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode == 200
    } catch (e: Exception) {
        false
    }
}
