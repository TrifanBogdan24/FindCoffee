package com.example.findcoffee

import androidx.compose.foundation.clickable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import android.content.Intent

class CoffeeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""

        setContent {
            FindCoffeeTheme {
                var coffees by remember { mutableStateOf<List<String>>(emptyList()) }

                LaunchedEffect(Unit) {
                    coffees = getCoffees(ip, port)
                }

                ConnectionMonitor() // monitorizare globala
                CoffeeListScreen(coffees = coffees, ip = ip, port = port)
            }
        }
    }
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
    val imageUrl = "http://$ip:$port/api/images/coffee_list/$imageName"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable {
                val intent = Intent(context, CoffeeSizeActivity::class.java).apply {
                    putExtra("IP", ip)
                    putExtra("PORT", port)
                    putExtra("COFFEE_NAME", coffeeName.lowercase().replace(" ", "_"))
                }
                context.startActivity(intent)
            },
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

suspend fun getCoffees(ip: String, port: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = URL("http://$cleanIp:$port/api/coffees")
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

        val response = connection.inputStream.bufferedReader().use { it.readText() }
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
        return@withContext responseCode == HttpURLConnection.HTTP_OK
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}
