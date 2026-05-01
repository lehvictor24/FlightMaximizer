package com.flightmaximizer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Routes : Screen("routes", "Routes", Icons.Filled.Map)
    object Schedule : Screen("schedule", "Schedule", Icons.Filled.Schedule)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavScreens = listOf(Screen.Home, Screen.Routes, Screen.Schedule, Screen.Settings)
