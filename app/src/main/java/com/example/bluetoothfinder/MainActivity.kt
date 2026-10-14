package com.example.bluetoothfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.bluetoothfinder.ui.navigation.AppNavigation
import com.example.bluetoothfinder.ui.theme.BluetoothFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothFinderTheme {
                AppNavigation()
            }
        }
    }
}
