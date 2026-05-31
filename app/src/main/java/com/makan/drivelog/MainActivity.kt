package com.makan.drivelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.makan.drivelog.navigation.NavGraph
import com.makan.drivelog.navigation.Screen
import com.makan.drivelog.presentation.components.BottomNavBar
import com.makan.drivelog.presentation.theme.DriveLogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainRoutes = setOf(
        Screen.Map.route, Screen.Trips.route, Screen.Navigate.route,
        Screen.Board.route, Screen.Profile.route
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveLogTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute in mainRoutes) {
                            BottomNavBar(
                                currentRoute = currentRoute,
                                onNavigate = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Map.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    NavGraph(navController = navController, paddingValues = paddingValues)
                }
            }
        }
    }
}
