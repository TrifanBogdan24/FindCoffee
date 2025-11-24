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
import androidx.compose.ui.platform.LocalContext
import com.example.findcoffee.coroutines.CheckInternetConnection
import kotlinx.coroutines.*
import com.example.findcoffee.data_base.Coffee
import com.example.findcoffee.data_base.CoffeeSize
import com.example.findcoffee.data_base.Ingredient
import com.example.findcoffee.data_base.Step
import com.example.findcoffee.data_base.CoffeeDatabase
import com.example.findcoffee.data_base.fetchCoffeeRecipes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class ConnectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindCoffeeTheme {
                CheckInternetConnection() // monitorizare globala

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
    val context = LocalContext.current

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
                            // 1. Sterge toate datele vechi
                            val db = CoffeeDatabase.getDatabase(context)
                            db.coffeeDao().deleteAll()
                            db.coffeeSizeDao().deleteAll()
                            db.ingredientDao().deleteAll()
                            db.stepDao().deleteAll()

                            // 2. Fetch toate datele din /coffee_recipes
                            val recipes = fetchCoffeeRecipes(ipTrimmed, portTrimmed)

                            // 3. Populeaza baza de date
                            recipes.forEach { coffeeJson ->
                                val coffee = Coffee(
                                    category = coffeeJson.getString("category"),
                                    name = coffeeJson.getString("name"),
                                    notes = coffeeJson.optString("notes", null)
                                )
                                val coffeeId = db.coffeeDao().insert(coffee)

                                // Dimensiuni
                                val sizes = coffeeJson.getJSONObject("final_volume").keys()
                                while (sizes.hasNext()) {
                                    val sizeName = sizes.next()
                                    val finalVolume = coffeeJson.getJSONObject("final_volume").optString(sizeName)
                                    db.coffeeSizeDao().insert(
                                        CoffeeSize(coffeeId = coffeeId.toInt(), size = sizeName, finalVolume = finalVolume)
                                    )
                                }

                                // Ingrediente
                                val ingredientsObj = coffeeJson.getJSONObject("ingredients")
                                ingredientsObj.keys().forEach { sizeKey ->
                                    val ingForSize = ingredientsObj.getJSONObject(sizeKey)
                                    ingForSize.keys().forEach { ingName ->
                                        val qty = ingForSize.getString(ingName)
                                        db.ingredientDao().insert(
                                            Ingredient(coffeeId = coffeeId.toInt(), size = sizeKey, ingredient = ingName, quantity = qty)
                                        )
                                    }
                                }

                                // Steps
                                val stepsObj = coffeeJson.getJSONObject("steps")
                                stepsObj.keys().forEach { stepNum ->
                                    val stepJson = stepsObj.getJSONObject(stepNum)
                                    db.stepDao().insert(
                                        Step(
                                            coffeeId = coffeeId.toInt(),
                                            stepNumber = stepNum.toInt(),
                                            title = stepJson.optString("title", null),
                                            description = stepJson.optString("description", null)
                                        )
                                    )
                                }
                            }

                            // 4. Navigheaza la lista de cafele
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