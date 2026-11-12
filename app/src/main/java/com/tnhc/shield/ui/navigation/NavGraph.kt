package com.tnhc.shield.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tnhc.shield.ui.screens.apps.AppsScreen
import com.tnhc.shield.ui.screens.dashboard.DashboardScreen
import com.tnhc.shield.ui.screens.filters.FiltersScreen
import com.tnhc.shield.ui.screens.home.HomeScreen
import com.tnhc.shield.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Shield)
    object Dashboard : Screen("dashboard", "Activity", Icons.Filled.BarChart)
    object Filters : Screen("filters", "Filters", Icons.Filled.FilterList)
    object Apps : Screen("apps", "Apps", Icons.Filled.Apps)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Dashboard, Screen.Filters, Screen.Apps, Screen.Settings
)

@Composable
fun ShieldNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            contentPadding = paddingValues
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Filters.route) { FiltersScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
