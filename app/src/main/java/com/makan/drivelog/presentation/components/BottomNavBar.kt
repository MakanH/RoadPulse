package com.makan.drivelog.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.makan.drivelog.navigation.Screen
import com.makan.drivelog.presentation.theme.BackgroundSecondary
import com.makan.drivelog.presentation.theme.NavInactive
import com.makan.drivelog.presentation.theme.TealActive

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem(Screen.Map,      "Map",      Icons.Filled.Map),
    NavItem(Screen.Trips,    "Trips",    Icons.Filled.History),
    NavItem(Screen.Navigate, "Navigate", Icons.Filled.Navigation),
    NavItem(Screen.Board,    "Board",    Icons.Filled.EmojiEvents),
    NavItem(Screen.Friends,  "Friends",  Icons.Filled.People),
    NavItem(Screen.Groups,   "Groups",   Icons.Filled.DirectionsCar),
    NavItem(Screen.Profile,  "Profile",  Icons.Filled.Person)
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(containerColor = BackgroundSecondary) {
        navItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = TealActive,
                    selectedTextColor   = TealActive,
                    unselectedIconColor = NavInactive,
                    unselectedTextColor = NavInactive,
                    indicatorColor      = Color.Transparent
                )
            )
        }
    }
}
