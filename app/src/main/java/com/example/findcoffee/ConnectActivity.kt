package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import com.example.findcoffee.coroutines.CheckInternetConnection
import kotlinx.coroutines.*
import com.example.findcoffee.data_base.Coffee
import com.example.findcoffee.data_base.CoffeeSize
import com.example.findcoffee.data_base.Ingredient
import com.example.findcoffee.data_base.Step
import com.example.findcoffee.data_base.CoffeeDatabase
import com.example.findcoffee.data_base.fetchCoffeeRecipes
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.net.HttpURLConnection
import java.net.URL

class ConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindCoffeeTheme {
                CheckInternetConnection()
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
    // Specificam explicit tipul String pentru a evita eroarea Nothing?
    var ipAddress by remember { mutableStateOf<String>("") }
    var port by remember { mutableStateOf<String>("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scanner = remember { GmsBarcodeScanning.getClient(context) }


    // Functie extrasa pentru a putea fi apelata si de scanner si de buton manual
    fun performConnection(targetIp: String, targetPort: String) {
        if (targetIp.isBlank()) return

        scope.launch {
            showDialog = true
            isLoading = true

            // Masuram timpul de start pentru a calcula cat mai trebuie sa asteptam la final
            val startTime = System.currentTimeMillis()

            // Executam incercarea de conectare
            val result = connectToServer(targetIp, targetPort)

            if (result) {
                val db = CoffeeDatabase.getDatabase(context)
                withContext(Dispatchers.IO) {
                    db.coffeeDao().deleteAll()
                    db.coffeeSizeDao().deleteAll()
                    db.ingredientDao().deleteAll()
                    db.stepDao().deleteAll()

                    val recipes = fetchCoffeeRecipes(targetIp, targetPort)
                    recipes.forEach { coffeeJson ->
                        val coffeeId = db.coffeeDao().insert(Coffee(
                            category = coffeeJson.getString("category"),
                            name = coffeeJson.getString("name"),
                            notes = coffeeJson.optString("notes", null)
                        ))

                        val fvObj = coffeeJson.getJSONObject("final_volume")
                        fvObj.keys().forEach { sizeKey ->
                            db.coffeeSizeDao().insert(CoffeeSize(
                                coffeeId = coffeeId.toInt(),
                                size = sizeKey,
                                finalVolume = fvObj.getString(sizeKey)
                            ))
                        }

                        val ingObj = coffeeJson.getJSONObject("ingredients")
                        ingObj.keys().forEach { sizeKey ->
                            val subIng = ingObj.getJSONObject(sizeKey)
                            subIng.keys().forEach { ingName ->
                                db.ingredientDao().insert(Ingredient(
                                    coffeeId = coffeeId.toInt(),
                                    size = sizeKey,
                                    ingredient = ingName,
                                    quantity = subIng.getString(ingName)
                                ))
                            }
                        }

                        val stepsObj = coffeeJson.getJSONObject("steps")
                        stepsObj.keys().forEach { stepNum ->
                            val stepJson = stepsObj.getJSONObject(stepNum)
                            db.stepDao().insert(Step(
                                coffeeId = coffeeId.toInt(),
                                stepNumber = stepNum.toInt(),
                                title = stepJson.optString("title", null),
                                description = stepJson.optString("description", null)
                            ))
                        }
                    }
                }
            }

            // Calculam cat timp a trecut deja
            val elapsedTime = System.currentTimeMillis() - startTime
            val remainingTime = 3000L - elapsedTime

            // Daca operatiunea a durat mai putin de 3 secunde, asteptam diferenta
            if (remainingTime > 0) {
                delay(remainingTime)
            }

            isLoading = false
            showDialog = false

            // Navigam doar daca rezultatul a fost de succes
            if (result) {
                onSuccess(targetIp, targetPort)
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Find Coffee Server", fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))

            Button(
                onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue ?: ""
                            try {
                                val uri = Uri.parse(rawValue)
                                val host = uri.host ?: ""
                                val portVal = uri.port.toString()
                                if (host.isNotEmpty()) {
                                    val finalIp = host
                                    val finalPort = if (portVal == "-1") "5000" else portVal

                                    // Actualizam UI-ul
                                    ipAddress = finalIp
                                    port = finalPort

                                    // Declansam conexiunea automat dupa scanare
                                    performConnection(finalIp, finalPort)
                                }
                            } catch (e: Exception) {
                                // Format invalid
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Scan QR Code 📷")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("OR MANUALLY", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))

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
                    performConnection(ipAddress.trim(), port.trim())
                },
                enabled = !isLoading && ipAddress.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp)
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

suspend fun connectToServer(ip: String, port: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = URL("http://$cleanIp:$port/api")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            doInput = true
        }

        connection.connect()
        val responseCode = connection.responseCode
        connection.disconnect()
        responseCode == HttpURLConnection.HTTP_OK
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
