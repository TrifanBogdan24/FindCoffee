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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findcoffee.coroutines.CheckInternetConnection
import com.example.findcoffee.coroutines.Check_HTTP_ServerConnection
import com.example.findcoffee.data_base.CoffeeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoffeeSizeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""
        val coffeeName = intent.getStringExtra("COFFEE_NAME") ?: ""

        setContent {
            // monitorizare globala:
            CheckInternetConnection()
            Check_HTTP_ServerConnection(ip = ip, port = port)

            CoffeeSizeScreen(
                ip = ip,
                port = port,
                coffeeName = coffeeName,
                onClose = { finish() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeSizeScreen(ip: String, port: String, coffeeName: String, onClose: () -> Unit) {
    val context = LocalContext.current
    var sizes by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSize by remember { mutableStateOf<String?>(null) }

    // Fetch sizes din DB
    LaunchedEffect(Unit) {
        sizes = getCoffeeSizesFromDb(context, coffeeName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${coffeeName.replace("_", " ").replaceFirstChar { it.uppercase() }} Sizes",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
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
        },
        bottomBar = {
            if (selectedSize != null) {
                Button(
                    onClick = {
                        val intent = Intent(context, CoffeeIngredientsActivity::class.java).apply {
                            putExtra("IP", ip)
                            putExtra("PORT", port)
                            putExtra("COFFEE_NAME", coffeeName)
                            putExtra("SIZE_NAME", selectedSize ?: "")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "NEXT ➡️", fontSize = 18.sp)
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
            sizes.forEach { size ->
                SizeCard(
                    sizeName = size,
                    isSelected = selectedSize == size,
                    onSelect = { selectedSize = size },
                    context = context,
                    coffeeName = coffeeName
                )
            }
        }
    }
}

@Composable
fun SizeCard(
    sizeName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    context: android.content.Context,
    coffeeName: String
) {
    var finalVolume by remember { mutableStateOf<String?>(null) }

    // Preia volumul final din DB
    LaunchedEffect(sizeName) {
        finalVolume = getFinalVolumeFromDb(context, coffeeName, sizeName)
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

suspend fun getCoffeeSizesFromDb(context: android.content.Context, coffeeName: String): List<String> =
    withContext(Dispatchers.IO) {
        try {
            val db = CoffeeDatabase.getDatabase(context)
            val coffeeDao = db.coffeeDao()
            val sizeDao = db.coffeeSizeDao()

            val coffee = coffeeDao.getByName(coffeeName.lowercase())
            if (coffee == null) return@withContext emptyList<String>()

            val sizes = sizeDao.getSizesForCoffee(coffee.id)
            return@withContext sizes.map { it.size }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

suspend fun getFinalVolumeFromDb(
    context: android.content.Context,
    coffeeName: String,
    sizeName: String
): String? = withContext(Dispatchers.IO) {
    try {
        val db = CoffeeDatabase.getDatabase(context)
        val coffeeDao = db.coffeeDao()
        val sizeDao = db.coffeeSizeDao()

        val coffee = coffeeDao.getByName(coffeeName.lowercase()) ?: return@withContext null
        val size = sizeDao.getSizeForCoffeeAndName(coffee.id, sizeName.lowercase())
        return@withContext size?.finalVolume
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
