package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.example.findcoffee.data_base.CoffeeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoffeeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""

        setContent {
            FindCoffeeTheme {
                var coffees by remember { mutableStateOf<List<String>>(emptyList()) }
                val context = LocalContext.current

                // Fetch coffees from database
                LaunchedEffect(Unit) {
                    coffees = getCoffeesFromDb(context)
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

// Fetch coffee names from Room database
suspend fun getCoffeesFromDb(context: android.content.Context): List<String> = withContext(Dispatchers.IO) {
    val db = CoffeeDatabase.getDatabase(context)
    db.coffeeDao().getAllNames()
}
