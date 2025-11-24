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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.findcoffee.data_base.CoffeeDatabase
import com.example.findcoffee.data_base.Step
import com.example.findcoffee.ui.theme.FindCoffeeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoffeeStepsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ip = intent.getStringExtra("IP") ?: ""
        val port = intent.getStringExtra("PORT") ?: ""
        val coffeeName = intent.getStringExtra("COFFEE_NAME") ?: ""
        val sizeName = intent.getStringExtra("SIZE_NAME") ?: "" // rămâne pentru back-navigation

        setContent {
            FindCoffeeTheme {
                // monitorizare globala:
                CheckInternetConnection()
                Check_HTTP_ServerConnection(ip = ip, port = port)

                CoffeeStepsScreen(
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
fun CoffeeStepsScreen(
    ip: String,
    port: String,
    coffeeName: String,
    sizeName: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var steps by remember { mutableStateOf<List<Step>>(emptyList()) }
    var currentStep by remember { mutableStateOf(1) }

    // Fetch steps from DB on load
    LaunchedEffect(Unit) {
        steps = getCoffeeStepsFromDb(context, coffeeName)
    }

    val totalSteps = steps.size
    val step = steps.getOrNull(currentStep - 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Preparation Steps\n${
                            coffeeName.replace("_", " ")
                                .replaceFirstChar { it.uppercase() }
                        }",
                        fontSize = 22.sp,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (currentStep == 1) {
                            val intent = Intent(context, CoffeeIngredientsActivity::class.java).apply {
                                putExtra("IP", ip)
                                putExtra("PORT", port)
                                putExtra("COFFEE_NAME", coffeeName)
                                putExtra("SIZE_NAME", sizeName)
                            }
                            context.startActivity(intent)
                        } else {
                            currentStep = (currentStep - 1).coerceAtLeast(1)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⬅️ PREV", fontSize = 18.sp)
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps) {
                            currentStep++
                        } else {
                            onClose()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("NEXT ➡️", fontSize = 18.sp)
                }
            }
        }
    ) { padding ->
        if (step != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = if (totalSteps > 0) currentStep / totalSteps.toFloat() else 0f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(horizontal = 16.dp)
                )

                Text(
                    text = "Step $currentStep of $totalSteps",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = step.title ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = step.description ?: "",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

// Fetch steps from local Room database
suspend fun getCoffeeStepsFromDb(context: android.content.Context, coffeeName: String): List<Step> =
    withContext(Dispatchers.IO) {
        try {
            val db = CoffeeDatabase.getDatabase(context)
            val coffeeDao = db.coffeeDao()
            val stepDao = db.stepDao()

            val coffee = coffeeDao.getByName(coffeeName.lowercase())
            if (coffee == null) return@withContext emptyList<Step>()

            val steps = stepDao.getStepsForCoffee(coffee.id)
            return@withContext steps.sortedBy { it.stepNumber }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<Step>()
        }
    }
