package com.athleteapp.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.athleteapp.pro.ui.AthleteViewModel
import com.athleteapp.pro.ui.screens.AthleteHistoryScreen
import com.athleteapp.pro.ui.screens.AthleteSettingsScreen
import com.athleteapp.pro.ui.screens.AthleteTodayScreen
import com.athleteapp.pro.ui.theme.AthleteProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AthleteViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        viewModel.autoSync()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val themeName = settings?.currentThemeName ?: "Cyber Lime"
            val lang = settings?.language ?: "ru"

            AthleteProTheme(themeName = themeName) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "today",
                                onClick = {
                                    navController.navigate("today") {
                                        popUpTo("today") { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                                label = { Text(if (lang == "en") "Workout" else "Тренировка") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "history",
                                onClick = {
                                    navController.navigate("history") {
                                        popUpTo("today")
                                    }
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                                label = { Text(if (lang == "en") "Progress" else "Прогресс") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "settings",
                                onClick = {
                                    navController.navigate("settings") {
                                        popUpTo("today")
                                    }
                                },
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                label = { Text(if (lang == "en") "Profile" else "Профиль") }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "today",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("today") {
                            AthleteTodayScreen(viewModel = viewModel)
                        }
                        composable("history") {
                            AthleteHistoryScreen(viewModel = viewModel)
                        }
                        composable("settings") {
                            AthleteSettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
