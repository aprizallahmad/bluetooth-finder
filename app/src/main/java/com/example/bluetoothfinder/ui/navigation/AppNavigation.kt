package com.example.bluetoothfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bluetoothfinder.ui.screen.DetailScreen
import com.example.bluetoothfinder.ui.screen.HistoryScreen
import com.example.bluetoothfinder.ui.screen.HomeScreen
import com.example.bluetoothfinder.viewmodel.RadarViewModel

/**
 * Navigasi aplikasi (3 layar sesuai brief):
 * 1. "home"    -> Dashboard Scanner
 * 2. "detail"  -> Detail Pelacakan / Radar View (argumen: alamat perangkat)
 * 3. "history" -> Riwayat Perangkat
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable(
            route = "detail/{${RadarViewModel.ARG_ADDRESS}}",
            arguments = listOf(
                navArgument(RadarViewModel.ARG_ADDRESS) { type = NavType.StringType },
            ),
        ) {
            DetailScreen(navController)
        }
        composable("history") {
            HistoryScreen(navController)
        }
    }
}
