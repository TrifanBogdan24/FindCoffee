package com.example.findcoffee.coroutines

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun Check_HTTP_ServerConnection(
    ip: String,
    port: String
) {
    var isReachableServer by remember { mutableStateOf(true) }
    var showMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(ip, port) {
        while (true) {
            val reachable = checkServerReachability(ip, port)

            // Transition between ONLINE SERVER -> OFFLINE SERVER
            if (isReachableServer && !reachable) {
                isReachableServer = false
                showMessage = "⚠️\nServer is down\nUsing local cache"

                scope.launch {
                    delay(2500)
                    showMessage = null
                }
            }

            // Transition between OFFLINE SERVER -> ONLINE SERVER
            else if (!isReachableServer && reachable) {
                isReachableServer = true
                showMessage = "📡\nServer is up again"

                scope.launch {
                    delay(2500)
                    showMessage = null
                }
            }

            delay(10_000)
        }
    }

    // Auto-closing dialog
    if (showMessage != null) {
        MiniStatusDialog(message = showMessage!!)
    }
}

@Composable
fun MiniStatusDialog(message: String) {

    // Extract emoji (first line before newline)
    val lines = message.split("\n")
    val emoji = lines.firstOrNull() ?: ""
    val textMessage = lines.drop(1).joinToString("\n")

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big emoji
                Text(
                    emoji,
                    style = MaterialTheme.typography.displayLarge,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message text centered
                Text(
                    textMessage,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    )
}


/** Server reachability check */
suspend fun checkServerReachability(ip: String, port: String): Boolean =
    withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("http://${ip}:${port}/api")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            connection.connect()
            val code = connection.responseCode
            connection.disconnect()

            code == 200
        } catch (e: Exception) {
            false
        }
    }
