package com.example.findcoffee

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // redirectioneaza imediat catre ConnectActivity
        startActivity(Intent(this, ConnectActivity::class.java))
        finish() // inchide MainActivity
    }
}
