package com.makan.drivelog.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.makan.drivelog.presentation.leaderboard.LeaderboardScreen
import com.makan.drivelog.presentation.map.MapScreen
import com.makan.drivelog.presentation.navigate.NavigateScreen
import com.makan.drivelog.presentation.profile.ProfileScreen
import com.makan.drivelog.presentation.tripdetail.TripDetailScreen
import com.makan.drivelog.presentation.trips.TripsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Map.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(Screen.Map.route)      { MapScreen() }
        composable(Screen.Trips.route)    {
            TripsScreen(onTripClick = { tripId ->
                navController.navigate(Screen.TripDetail.createRoute(tripId))
            })
        }
        composable(Screen.Navigate.route) { NavigateScreen() }
        composable(Screen.Board.route)    { LeaderboardScreen() }
        composable(Screen.Profile.route)  { ProfileScreen() }
        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(navArgument(Screen.TripDetail.ARG_TRIP_ID) { type = NavType.LongType })
        ) { back ->
            val tripId = back.arguments?.getLong(Screen.TripDetail.ARG_TRIP_ID) ?: return@composable
            TripDetailScreen(tripId = tripId)
        }
    }
}
