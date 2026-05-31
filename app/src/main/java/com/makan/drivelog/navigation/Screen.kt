package com.makan.drivelog.navigation

sealed class Screen(val route: String) {
    object Map      : Screen("map")
    object Trips    : Screen("trips")
    object Navigate : Screen("navigate")
    object Board    : Screen("board")
    object Profile  : Screen("profile")
    object Friends  : Screen("friends")
    object Login    : Screen("login")
    object SignUp   : Screen("signup")
    object TripDetail : Screen("trip_detail/{tripId}") {
        const val ARG_TRIP_ID = "tripId"
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }
}
