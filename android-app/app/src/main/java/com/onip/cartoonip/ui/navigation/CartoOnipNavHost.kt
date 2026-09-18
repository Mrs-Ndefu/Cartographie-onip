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
import com.onip.cartoonip.ui.capture.CaptureScreen
import com.onip.cartoonip.ui.journal.JournalScreen
import com.onip.cartoonip.ui.login.LoginScreen
import com.onip.cartoonip.ui.overview.OverviewScreen

@Composable
fun CartoOnipNavHost(navController: NavHostController = rememberNavController()) {
    val session by AppContainer.sessionManager.session.collectAsState()
    val startDestination = if (session != null) Routes.CAPTURE else Routes.LOGIN

    val onNavigate: (String) -> Unit = { route ->
        if (route == Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) }
        } else {
            navController.navigate(route)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.CAPTURE) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(onNavigate = onNavigate)
        }
        composable(
            route = Routes.CAPTURE_EDIT,
            arguments = listOf(navArgument("householdId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("householdId")
            CaptureScreen(onNavigate = onNavigate, editId = id)
        }
        composable(Routes.JOURNAL) {
            JournalScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.captureEdit(id)) },
            )
        }
        composable(Routes.OVERVIEW) {
            OverviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
