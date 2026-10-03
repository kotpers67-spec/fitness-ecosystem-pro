package com.trainerapp.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trainerapp.pro.ui.MainViewModel
import com.trainerapp.pro.ui.components.FloatingRestTimerOverlay
import com.trainerapp.pro.ui.screens.HistoryScreen
import com.trainerapp.pro.ui.screens.HomeScreen
import com.trainerapp.pro.ui.screens.SettingsScreen
import com.trainerapp.pro.ui.screens.TrainerAuthScreen
import com.trainerapp.pro.ui.screens.WorkoutScreen
import com.trainerapp.pro.ui.theme.TrainerProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            val remainingSeconds by viewModel.timerManager.remainingSeconds.collectAsState()
            val isTimerRunning by viewModel.timerManager.isRunning.collectAsState()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

            TrainerProTheme(themeName = settings.currentThemeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isLoggedIn) {
                        TrainerAuthScreen(viewModel = viewModel)
                    } else {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = {
                                if (isTimerRunning) {
                                    FloatingRestTimerOverlay(
                                        remainingSeconds = remainingSeconds,
                                        isRunning = isTimerRunning,
                                        onAddTime = { viewModel.timerManager.addTime(it) },
                                        onStop = { viewModel.timerManager.stopTimer() }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) {
                                TrainerAppNavigation(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncActiveClientWithGoogleDrive()
    }
}

@Composable
fun TrainerAppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToWorkout = { navController.navigate("workout") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("workout") {
            WorkoutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
