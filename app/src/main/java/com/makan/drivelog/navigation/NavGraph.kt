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
import com.makan.drivelog.presentation.friends.FriendsScreen
import com.makan.drivelog.presentation.groups.GroupDetailScreen
import com.makan.drivelog.presentation.groups.GroupMapScreen
import com.makan.drivelog.presentation.groups.GroupsScreen
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
        composable(Screen.Map.route) {
            MapScreen(
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId))
                },
                onOpenGroupMap = { groupId, sessionId ->
                    navController.navigate(Screen.GroupMap.createRoute(groupId, sessionId))
                }
            )
        }
        composable(Screen.Trips.route) {
            TripsScreen(onTripClick = { tripId ->
                navController.navigate(Screen.TripDetail.createRoute(tripId))
            })
        }
        composable(Screen.Navigate.route) { NavigateScreen() }
        composable(Screen.Board.route) {
            LeaderboardScreen(
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripDetail.createRoute(tripId))
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route) {
                        popUpTo(Screen.Map.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(Screen.Friends.route) { FriendsScreen() }
        composable(Screen.Groups.route) {
            GroupsScreen(onGroupClick = { groupId ->
                navController.navigate(Screen.GroupDetail.createRoute(groupId))
            })
        }
        composable(Screen.Profile.route) { ProfileScreen() }
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument(Screen.GroupDetail.ARG_GROUP_ID) { type = NavType.StringType })
        ) {
            GroupDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenMap = { sessionId ->
                    val groupId = it.arguments?.getString(Screen.GroupDetail.ARG_GROUP_ID) ?: return@GroupDetailScreen
                    navController.navigate(Screen.GroupMap.createRoute(groupId, sessionId))
                }
            )
        }
        composable(
            route = Screen.GroupMap.route,
            arguments = listOf(
                navArgument(Screen.GroupMap.ARG_GROUP_ID)   { type = NavType.StringType },
                navArgument(Screen.GroupMap.ARG_SESSION_ID) { type = NavType.StringType }
            )
        ) {
            GroupMapScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.TripDetail.route,
            arguments = listOf(navArgument(Screen.TripDetail.ARG_TRIP_ID) { type = NavType.LongType })
        ) { back ->
            val tripId = back.arguments?.getLong(Screen.TripDetail.ARG_TRIP_ID) ?: return@composable
            TripDetailScreen(
                tripId = tripId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
