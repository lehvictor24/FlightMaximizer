package com.flightmaximizer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.flightmaximizer.ui.home.HomeScreen
import com.flightmaximizer.ui.routes.RoutesScreen
import com.flightmaximizer.ui.schedule.ScheduleScreen
import com.flightmaximizer.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(
            route = "home?scanRunId={scanRunId}",
            arguments = listOf(
                navArgument("scanRunId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "flightmaximizer://home?scanRunId={scanRunId}" },
                navDeepLink { uriPattern = "flightmaximizer://home" }
            )
        ) { backStackEntry ->
            HomeScreen(scanRunId = backStackEntry.arguments?.getString("scanRunId"))
        }
        composable(Screen.Routes.route) { RoutesScreen() }
        composable(Screen.Schedule.route) { ScheduleScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
