package com.makan.drivelog.navigation

sealed class Screen(val route: String) {
    object Map      : Screen("map")
    object Trips    : Screen("trips")
    object Navigate : Screen("navigate")
    object Board    : Screen("board")
    object Profile  : Screen("profile")
    object Friends  : Screen("friends")
    object Groups   : Screen("groups")
    object Login    : Screen("login")
    object SignUp   : Screen("signup")
    object TripDetail : Screen("trip_detail/{tripId}") {
        const val ARG_TRIP_ID = "tripId"
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
    object GroupDetail : Screen("group_detail/{groupId}") {
        const val ARG_GROUP_ID = "groupId"
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object GroupMap : Screen("group_map/{groupId}/{sessionId}") {
        const val ARG_GROUP_ID  = "groupId"
        const val ARG_SESSION_ID = "sessionId"
        fun createRoute(groupId: String, sessionId: String) = "group_map/$groupId/$sessionId"
    }
}
