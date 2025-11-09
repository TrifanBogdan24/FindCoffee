package com.example.findcoffee

import android.content.Intent
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

class ConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindCoffeeTheme {
                ConnectionMonitor() // monitorizare globala

                ConnectionScreen(onSuccess = { ip, port ->
                    val intent = Intent(this, CoffeeListActivity::class.java)
                    intent.putExtra("IP", ip)
                    intent.putExtra("PORT", port)
                    startActivity(intent)
                })
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(onSuccess: (ip: String, port: String) -> Unit) {
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
                        delay(2000)

                        isLoading = true
                        val result = connectToServer(ipTrimmed, portTrimmed)
                        isLoading = false
                        showDialog = false

                        if (result) {
                            onSuccess(ipTrimmed, portTrimmed)
                        } else {
                            snackbarMessage = "Connection failed"
                            snackbarHostState.showSnackbar(snackbarMessage!!)
                        }
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
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Trying to connect to Coffee Server")
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    )
}
