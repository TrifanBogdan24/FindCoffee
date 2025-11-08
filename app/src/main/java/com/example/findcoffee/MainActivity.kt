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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.json.JSONArray
import org.json.JSONObject
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement

// Navigare simpla intre ecrane
sealed class Screen {
    object Connection : Screen()
    data class CoffeeList(
        val coffees: List<String>,
        val ip: String,
        val port: String
    ) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindCoffeeTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Connection) }
                var IP = null;
                var PORT = null;

                when (val screen = currentScreen) {
                    is Screen.Connection -> {
                        ConnectionScreen(onSuccess = { ip, port ->
                            // Apelam GET /coffees
                            CoroutineScope(Dispatchers.IO).launch {
                                val coffees = getCoffees(ip, port)
                                withContext(Dispatchers.Main) {
                                    currentScreen = Screen.CoffeeList(coffees, ip, port)
                                }
                            }
                        })

                    }
                    is Screen.CoffeeList -> {
                        CoffeeListScreen(coffees = screen.coffees, ip = screen.ip, port = screen.port)
                    }
                }
            }
        }
    }
}


suspend fun getCoffees(ip: String, port: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = URL("http://$cleanIp:$port/coffees")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            doInput = true
        }

        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return@withContext emptyList<String>()
        }

        val inputStream = connection.inputStream
        val response = inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val jsonArray = JSONArray(response)
        val coffeeList = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            coffeeList.add(jsonArray.getString(i))
        }
        return@withContext coffeeList
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext emptyList<String>()
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
                Text("Trying to connect to http://$ip:$port/")
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    )
}

@Composable
fun CoffeeListScreen(coffees: List<String>, ip: String, port: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(coffees) { coffee ->
            CoffeeCard(coffeeName = coffee, ip = ip, port = port)
        }
    }
}





@Composable
fun CoffeeCard(coffeeName: String, ip: String, port: String) {
    val context = LocalContext.current
    val imageName = coffeeName.lowercase().replace(" ", "_")
    val imageUrl = "http://$ip:$port/images/$imageName"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = coffeeName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            // Spatiul ramas sub imagine - textul centrat vertical in el
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // ocupa tot spatiul ramas:
                    .weight(1f),
                // centreaza textul in inaltime:
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = coffeeName,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


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
