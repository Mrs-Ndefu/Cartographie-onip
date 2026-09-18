package com.onip.cartoonip

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.onip.cartoonip.ui.navigation.CartoOnipNavHost
import com.onip.cartoonip.ui.theme.CartoOnipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CartoOnipTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CartoOnipNavHost()
                }
            }
        }
    }
}
