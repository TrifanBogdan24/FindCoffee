package com.example.findcoffee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindCoffeeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectionScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen() {
    var ipAddress by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port Server") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val ipTrimmed = ipAddress.trim()
                    val portTrimmed = port.trim()

                    scope.launch {
                        showDialog = true
                        delay(2000) // asteapta 2 secunde inainte de request

                        isLoading = true
                        val result = connectToServer(ipTrimmed, portTrimmed)
                        isLoading = false
                        showDialog = false

                        snackbarMessage = if (result) "Connection successful" else "Connection failed"
                        snackbarHostState.showSnackbar(snackbarMessage!!)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Connect", fontSize = 18.sp)
            }
        }

        if (showDialog) {
            LoadingDialog(ipAddress, port)
        }
    }
}

@Composable
fun LoadingDialog(ip: String, port: String) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("Connecting") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Trying to connect to http://$ip:$port/")
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    )
}


/**
 * Tries to connect to http://<ip>:<port>/ and returns true if response == 200 OK
 */
suspend fun connectToServer(ip: String, port: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val urlString = "http://$cleanIp:$port/"

        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            doInput = true
        }

        connection.connect()
        val responseCode = connection.responseCode

        connection.disconnect()
        return@withContext (responseCode == HttpURLConnection.HTTP_OK)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}

