package com.onip.cartoonip.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onip.cartoonip.data.AppContainer
import com.onip.cartoonip.ui.agents.AgentListScreen
import com.onip.cartoonip.ui.dashboard.DashboardScreen
import com.onip.cartoonip.ui.households.HouseholdDetailScreen
import com.onip.cartoonip.ui.households.HouseholdListScreen
import com.onip.cartoonip.ui.login.LoginScreen

@Composable
fun CartoOnipNavHost(navController: NavHostController = rememberNavController()) {
    val session by AppContainer.sessionManager.session.collectAsState()
    val startDestination = if (session != null) Routes.DASHBOARD else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.DASHBOARD) {
            DashboardScreen(navController = navController)
        }
        composable(Routes.HOUSEHOLDS) {
            HouseholdListScreen(navController = navController)
        }
        composable(
            route = Routes.HOUSEHOLD_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            HouseholdDetailScreen(householdId = id, navController = navController)
        }
        composable(Routes.AGENTS) {
            AgentListScreen(navController = navController)
        }
    }
}
