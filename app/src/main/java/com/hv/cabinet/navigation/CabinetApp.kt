package com.hv.cabinet.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hv.cabinet.core.PerfMonitor
import com.hv.cabinet.feature.home.HomeScreen
import com.hv.cabinet.feature.login.LoginScreen
import com.hv.cabinet.feature.returning.ReturnScreen
import com.hv.cabinet.feature.take.TakeScreen

@Composable
fun CabinetApp() {
    val rootViewModel = hiltViewModel<RootViewModel>()
    val state by rootViewModel.state.collectAsState()

    Surface(color = MaterialTheme.colorScheme.background) {
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(text = "正在初始化...")
            }
            return@Surface
        }

        val navController = rememberNavController()
        LaunchedEffect(navController) {
            PerfMonitor.mark("router_nav_host_ready")
        }
        CabinetNavHost(
            navController = navController,
            startDestination = state.startDestination
        )
    }
}

@Composable
private fun CabinetNavHost(
    navController: NavHostController,
    startDestination: String
) {
    val navActions = remember(navController) {
        CabinetNavActions(navController)
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            onDestinationChanged(destination)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navActions.toHome()
                }
            )
        }

        composable(AppRoutes.HOME) {
            HomeScreen(
                onNavigateTake = { navActions.toTake() },
                onNavigateReturn = { navActions.toReturning() },
                onLogout = { navActions.toLogin() }
            )
        }

        composable(AppRoutes.TAKE) {
            TakeScreen(
                onBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navActions.toHome()
                    }
                },
                onLogout = { navActions.toLogin() }
            )
        }

        composable(AppRoutes.RETURNING) {
            ReturnScreen(
                onBack = {
                    val popped = navController.popBackStack()
                    if (!popped) {
                        navActions.toHome()
                    }
                },
                onLogout = { navActions.toLogin() }
            )
        }
    }
}

private fun onDestinationChanged(destination: NavDestination) {
    val route = destination.hierarchy.firstOrNull { it.route != null }?.route ?: destination.route ?: return
    PerfMonitor.mark("router_after_each_end_$route")
    when (route) {
        AppRoutes.HOME -> {
            PerfMonitor.measure("router_before_each_start_home", "router_after_each_end_home", "router_home_cost")
        }
        AppRoutes.TAKE -> {
            PerfMonitor.measure("router_before_each_start_take", "router_after_each_end_take", "router_take_cost")
        }
        AppRoutes.RETURNING -> {
            PerfMonitor.measure("router_before_each_start_returning", "router_after_each_end_returning", "router_return_cost")
        }
    }
}

private class CabinetNavActions(
    private val navController: NavHostController
) {
    fun toHome() {
        PerfMonitor.mark("router_before_each_start_home")
        navController.navigate(AppRoutes.HOME) {
            popUpTo(AppRoutes.LOGIN) { inclusive = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun toTake() {
        PerfMonitor.mark("router_before_each_start_take")
        navController.navigate(AppRoutes.TAKE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun toReturning() {
        PerfMonitor.mark("router_before_each_start_returning")
        navController.navigate(AppRoutes.RETURNING) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun toLogin() {
        PerfMonitor.mark("router_before_each_start_login")
        navController.navigate(AppRoutes.LOGIN) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}
