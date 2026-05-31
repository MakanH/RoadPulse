package com.makan.drivelog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.makan.drivelog.data.local.PreferencesManager
import com.makan.drivelog.data.service.AuthService
import com.makan.drivelog.navigation.NavGraph
import com.makan.drivelog.navigation.Screen
import com.makan.drivelog.presentation.auth.LoginScreen
import com.makan.drivelog.presentation.auth.SignUpScreen
import com.makan.drivelog.presentation.components.BottomNavBar
import com.makan.drivelog.presentation.theme.DriveLogTheme
import com.makan.drivelog.presentation.util.LocalUnitPrefs
import com.makan.drivelog.presentation.util.UnitPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var authService: AuthService

    private val mainRoutes = setOf(
        Screen.Map.route, Screen.Trips.route, Screen.Navigate.route,
        Screen.Board.route, Screen.Profile.route
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val speedUnit    by preferencesManager.speedUnit.collectAsStateWithLifecycle(initialValue = "km/h")
            val distanceUnit by preferencesManager.distanceUnit.collectAsStateWithLifecycle(initialValue = "km")
            val currentUser  by authService.currentUser.collectAsStateWithLifecycle(initialValue = authService.auth.currentUser)

            DriveLogTheme {
                CompositionLocalProvider(LocalUnitPrefs provides UnitPrefs(speedUnit, distanceUnit)) {
                    val authNavController  = rememberNavController()
                    val mainNavController  = rememberNavController()

                    if (currentUser == null) {
                        // Auth flow
                        NavHost(navController = authNavController, startDestination = Screen.Login.route) {
                            composable(Screen.Login.route) {
                                LoginScreen(onNavigateToSignUp = {
                                    authNavController.navigate(Screen.SignUp.route)
                                })
                            }
                            composable(Screen.SignUp.route) {
                                SignUpScreen(onNavigateToLogin = {
                                    authNavController.popBackStack()
                                })
                            }
                        }
                    } else {
                        // Main app
                        val backStackEntry by mainNavController.currentBackStackEntryAsState()
                        val currentRoute = backStackEntry?.destination?.route
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (currentRoute in mainRoutes) {
                                    BottomNavBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { screen ->
                                            mainNavController.navigate(screen.route) {
                                                popUpTo(Screen.Map.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        ) { paddingValues ->
                            NavGraph(navController = mainNavController, paddingValues = paddingValues)
                        }
                    }
                }
            }
        }
    }
}
