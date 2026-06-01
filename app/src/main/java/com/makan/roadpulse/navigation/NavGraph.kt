package com.makan.roadpulse.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.makan.roadpulse.presentation.friends.FriendsScreen
import com.makan.roadpulse.presentation.groups.GroupDetailScreen
import com.makan.roadpulse.presentation.groups.GroupMapScreen
import com.makan.roadpulse.presentation.groups.GroupsScreen
import com.makan.roadpulse.presentation.leaderboard.LeaderboardScreen
import com.makan.roadpulse.presentation.map.MapScreen
import com.makan.roadpulse.presentation.navigate.NavigateScreen
import com.makan.roadpulse.presentation.profile.LicensesScreen
import com.makan.roadpulse.presentation.profile.ProfileScreen
import com.makan.roadpulse.presentation.profile.SettingsScreen
import com.makan.roadpulse.presentation.tripdetail.TripDetailScreen
import com.makan.roadpulse.presentation.trips.TripsScreen

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
        // ── 5 main tab destinations ────────────────────────────────────────────
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
                onNavigateToFriends = { navController.navigate(Screen.Friends.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToFriends  = { navController.navigate(Screen.Friends.route) },
                onNavigateToGroups   = { navController.navigate(Screen.Groups.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // ── Detail / non-tab destinations (bottom nav hidden on these) ─────────
        composable(Screen.Friends.route) { FriendsScreen() }
        composable(Screen.Groups.route) {
            GroupsScreen(onGroupClick = { groupId ->
                navController.navigate(Screen.GroupDetail.createRoute(groupId))
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLicenses = { navController.navigate(Screen.Licenses.route) }
            )
        }
        composable(Screen.Licenses.route) {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
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
