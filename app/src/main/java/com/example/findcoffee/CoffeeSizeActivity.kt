package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CoffeeSizeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""
        val coffeeName = intent.getStringExtra("COFFEE_NAME") ?: ""

        setContent {
            CoffeeSizeScreen(ip = ip, port = port, coffeeName = coffeeName, onClose = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeSizeScreen(ip: String, port: String, coffeeName: String, onClose: () -> Unit) {
    var sizes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSize by remember { mutableStateOf<String?>(null) }

    // Fetch sizes when screen loads
    LaunchedEffect(Unit) {
        sizes = getCoffeeSizes(ip, port, coffeeName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${coffeeName.replace("_"," ").replaceFirstChar { it.uppercase() }} Sizes") },
                navigationIcon = {
                    Text(
                        "X",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable { onClose() },
                        fontSize = 20.sp
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sizes.forEach { size ->
                SizeCard(
                    sizeName = size,
                    coffeeName = coffeeName,
                    ip = ip,
                    port = port,
                    isSelected = selectedSize == size,
                    onSelect = { selectedSize = size }
                )
            }

        }
    }
}

@Composable
fun SizeCard(
    sizeName: String,
    coffeeName: String,
    ip: String,
    port: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var finalVolume by remember { mutableStateOf<String?>(null) }

    // când componenta se lansează, cere volumul de pe server
    LaunchedEffect(sizeName) {
        finalVolume = getFinalVolume(ip, port, coffeeName, sizeName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.LightGray else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Text(
                    text = sizeName.replaceFirstChar { it.uppercase() },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (finalVolume != null) {
                    Text(
                        text = "(${finalVolume})",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
        }
    }
}


suspend fun getFinalVolume(
    ip: String,
    port: String,
    coffeeName: String,
    sizeName: String
): String? = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = URL("http://$cleanIp:$port/coffees/$coffeeName/size/$sizeName/final_volume")
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
            return@withContext null as String?
        }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()

        val jsonObj = org.json.JSONObject(response)
        jsonObj.optString("final_volume", null)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}



suspend fun getCoffeeSizes(ip: String, port: String, coffeeName: String): List<String> = withContext(Dispatchers.IO) {
    try {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = URL("http://$cleanIp:$port/coffees/$coffeeName/sizes")
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
        val sizeList = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            sizeList.add(jsonArray.getString(i))
        }
        return@withContext sizeList
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext emptyList<String>()
    }
}
