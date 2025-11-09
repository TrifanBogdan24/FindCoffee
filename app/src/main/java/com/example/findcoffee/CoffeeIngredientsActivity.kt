package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import androidx.compose.ui.platform.LocalContext

class CoffeeIngredientsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""
        val coffeeName = intent.getStringExtra("COFFEE_NAME") ?: ""
        val sizeName = intent.getStringExtra("SIZE_NAME") ?: ""

        setContent {
            FindCoffeeTheme {
                ConnectionMonitor() // monitorizare globala
                
                CoffeeIngredientsScreen(
                    ip = ip,
                    port = port,
                    coffeeName = coffeeName,
                    sizeName = sizeName,
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeIngredientsScreen(
    ip: String,
    port: String,
    coffeeName: String,
    sizeName: String,
    onClose: () -> Unit
) {
    var ingredientsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedIngredients by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current

    // Fetch ingredients on load
    LaunchedEffect(Unit) {
        ingredientsMap = getCoffeeIngredients(ip, port, coffeeName, sizeName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingredients\n${coffeeName.replace("_"," ").replaceFirstChar { it.uppercase() }}", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Text(
                        "X",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                val intent = Intent(context, CoffeeListActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                            },
                        fontSize = 20.sp
                    )
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PREV
                Button(
                    onClick = {
                        val intent = Intent(context, CoffeeSizeActivity::class.java).apply {
                            putExtra("IP", ip)
                            putExtra("PORT", port)
                            putExtra("COFFEE_NAME", coffeeName)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⬅\uFE0F PREV", fontSize = 18.sp)
                }

                // NEXT
                Button(
                    onClick = {
                        val intent = Intent(context, CoffeeStepsActivity::class.java).apply {
                            putExtra("IP", ip)
                            putExtra("PORT", port)
                            putExtra("COFFEE_NAME", coffeeName)
                            putExtra("SIZE_NAME", sizeName)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("NEXT ➡\uFE0F", fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ingredientsMap.forEach { (name, amount) ->
                val isSelected = selectedIngredients.contains(name)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIngredients = if (isSelected) {
                                selectedIngredients - name
                            } else {
                                selectedIngredients + name
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "• $amount ${name.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            selectedIngredients = if (it) {
                                selectedIngredients + name
                            } else {
                                selectedIngredients - name
                            }
                        }
                    )
                }
            }
        }
    }
}




suspend fun getCoffeeIngredients(ip: String, port: String, coffeeName: String, sizeName: String): Map<String, String> =
    withContext(Dispatchers.IO) {
        try {
            val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
            val url = URL("http://$cleanIp:$port/api/coffees/$sizeName/$coffeeName/ingredients")
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
                return@withContext emptyMap<String, String>()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val jsonObj = JSONObject(response)
            val map = mutableMapOf<String, String>()
            jsonObj.keys().forEach { key ->
                map[key] = jsonObj.getString(key)
            }
            return@withContext map
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap<String, String>()
        }
    }
