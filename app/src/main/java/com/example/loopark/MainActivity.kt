package com.example.loopark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.loopark.ui.HomeScreen
import com.example.loopark.ui.theme.ParkMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkMateTheme {
                HomeScreen()
            }
        }
    }
}
