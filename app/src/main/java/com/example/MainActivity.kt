package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainDashboard
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val configState by viewModel.config.collectAsState()

            if (configState == null) {
                // Instantiating/Preloading database rows
                MyApplicationTheme(darkTheme = isSystemInDarkTheme()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val currentConfig = configState!!
                
                // Determine Dark Theme override based on configuration database
                val isDark = when (currentConfig.themeMode) {
                    "LIGHT" -> false
                    "DARK" -> true
                    else -> isSystemInDarkTheme()
                }

                MyApplicationTheme(darkTheme = isDark) {
                    if (currentConfig.isFirstLaunch) {
                        OnboardingScreen(viewModel = viewModel)
                    } else {
                        MainDashboard(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
