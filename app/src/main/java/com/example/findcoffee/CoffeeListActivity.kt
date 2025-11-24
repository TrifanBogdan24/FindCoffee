package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import com.example.findcoffee.data_base.CoffeeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition


class CoffeeListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""

        setContent {
            FindCoffeeTheme {
                var allCoffees by remember { mutableStateOf<List<String>>(emptyList()) }
                var searchQuery by remember { mutableStateOf("") }
                val context = LocalContext.current

                // Fetch all coffees from DB
                LaunchedEffect(Unit) {
                    allCoffees = getCoffeesFromDb(context)
                }

                // monitorizare globala:
                CheckInternetConnection()
                Check_HTTP_ServerConnection(ip = ip, port = port)


                CoffeeListScreen(
                    coffees = allCoffees,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    ip = ip,
                    port = port
                )
            }
        }
    }
}

@Composable
fun CoffeeListScreen(
    coffees: List<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    ip: String,
    port: String
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onSearchChange(it) },
            label = { Text("Search Coffee") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Filter coffees
        val filteredCoffees = if (searchQuery.isBlank()) {
            coffees
        } else {
            coffees.filter { it.contains(searchQuery, ignoreCase = true) }
        }

        if (filteredCoffees.isEmpty()) {
            // Lottie animation when no results
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 50.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.no_coffee)
                )
                LottieAnimation(
                    composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(200.dp)
                )
            }
        } else {
            // Show coffee list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredCoffees) { coffee ->
                    CoffeeCard(coffeeName = coffee, ip = ip, port = port, highlight = searchQuery)
                }
            }
        }
    }
}


@Composable
fun CoffeeCard(coffeeName: String, ip: String, port: String, highlight: String = "") {
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
                    putExtra("COFFEE_NAME", coffeeName)
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
                    text = buildHighlightedText(coffeeName, highlight),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun buildHighlightedText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    val annotated = buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
            if (matchIndex == -1) {
                append(text.substring(currentIndex))
                break
            }
            if (matchIndex > currentIndex) append(text.substring(currentIndex, matchIndex))
            withStyle(SpanStyle(background = Color.Yellow, fontWeight = FontWeight.Bold)) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }
            currentIndex = matchIndex + query.length
        }
    }
    return annotated
}

// Fetch coffee names from Room database
suspend fun getCoffeesFromDb(context: android.content.Context): List<String> =
    withContext(Dispatchers.IO) {
        val db = CoffeeDatabase.getDatabase(context)
        db.coffeeDao().getAllNames()
    }
