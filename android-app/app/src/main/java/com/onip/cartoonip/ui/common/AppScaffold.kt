package com.onip.cartoonip.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.ui.navigation.Routes

private data class BottomDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.DASHBOARD, "Accueil", Icons.Filled.Home),
    BottomDestination(Routes.HOUSEHOLDS, "Ménages", Icons.Filled.Groups),
    BottomDestination(Routes.AGENTS, "Agents", Icons.Filled.People),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavHostController,
    currentRoute: String,
    title: String,
    content: @Composable (paddingValues: androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = {
                        AppContainer.sessionManager.clear()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Déconnexion")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                bottomDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.DASHBOARD)
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        content(paddingValues)
    }
}
