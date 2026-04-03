package com.example.banka_2_mobile.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.cards.CardsScreen
import com.example.banka_2_mobile.ui.exchange.ExchangeScreen
import com.example.banka_2_mobile.ui.home.HomeScreen
import com.example.banka_2_mobile.ui.login.LoginScreen
import com.example.banka_2_mobile.ui.orders.CreateOrderScreen
import com.example.banka_2_mobile.ui.orders.MyOrdersScreen
import com.example.banka_2_mobile.ui.otp.OtpScreen
import com.example.banka_2_mobile.ui.portfolio.PortfolioScreen
import com.example.banka_2_mobile.ui.securities.SecuritiesScreen
import com.example.banka_2_mobile.ui.securities.SecurityDetailScreen
import com.example.banka_2_mobile.ui.transactions.TransactionsScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val CARDS = "cards"
    const val EXCHANGE = "exchange"
    const val OTP = "otp"

    // ─── Celina 3: Berza routes ──────────────────────────
    const val SECURITIES = "securities"
    const val SECURITY_DETAIL = "securities/{listingId}"
    const val PORTFOLIO = "portfolio"
    const val CREATE_ORDER = "create_order/{listingId}/{direction}"
    const val MY_ORDERS = "my_orders"

    // Helper functions for building routes with arguments
    fun securityDetail(listingId: Long) = "securities/$listingId"
    fun createOrder(listingId: Long, direction: String) = "create_order/$listingId/$direction"
}

// ─── Bottom nav items: Berza, Portfolio, Pocetna (center), Nalozi, OTP ───────
private val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.SECURITIES,
        label = "Berza",
        icon = Icons.AutoMirrored.Filled.ShowChart
    ),
    BottomNavItem(
        route = Routes.PORTFOLIO,
        label = "Portfolio",
        icon = Icons.Filled.WorkOutline
    ),
    BottomNavItem(
        route = Routes.HOME,
        label = "Početna",
        icon = Icons.Filled.AccountBalanceWallet,
        isCenter = true
    ),
    BottomNavItem(
        route = Routes.MY_ORDERS,
        label = "Nalozi",
        icon = Icons.Filled.Receipt
    ),
    BottomNavItem(
        route = Routes.OTP,
        label = "OTP",
        icon = Icons.Filled.Shield
    )
)

// Routes where the bottom nav bar is visible (main tabs + quick-action screens)
private val mainRoutes = setOf(
    Routes.HOME,
    Routes.TRANSACTIONS,
    Routes.CARDS,
    Routes.EXCHANGE,
    Routes.OTP,
    Routes.SECURITIES,
    Routes.PORTFOLIO,
    Routes.MY_ORDERS
)

// ─── Transition configuration ────────────────────────────────────────────────

private const val TRANSITION_DURATION = 300

private fun defaultEnterTransition(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        initialOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun defaultExitTransition(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        targetOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun defaultPopEnterTransition(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        initialOffsetX = { fullWidth -> -(fullWidth * 0.08f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun defaultPopExitTransition(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * 0.08f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

// Detail screens slide up from the bottom
private fun detailEnterTransition(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInVertically(
        initialOffsetY = { fullHeight -> (fullHeight * 0.06f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

private fun detailPopExitTransition(): ExitTransition =
    fadeOut(
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutVertically(
        targetOffsetY = { fullHeight -> (fullHeight * 0.06f).toInt() },
        animationSpec = tween(
            durationMillis = TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    )

// Login uses a simple crossfade (no slide)
private fun loginEnterTransition(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = 400))

private fun loginExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 400))

// ═════════════════════════════════════════════════════════════════════════════
//  NavGraph
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }

    val startDestination = if (authRepository.isLoggedIn()) Routes.HOME else Routes.LOGIN

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val showBottomBar = currentRoute in mainRoutes

    val navigateToLogin: () -> Unit = {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { defaultEnterTransition() },
            exitTransition = { defaultExitTransition() },
            popEnterTransition = { defaultPopEnterTransition() },
            popExitTransition = { defaultPopExitTransition() }
        ) {
            // ─── Auth ────────────────────────────────────────────────────

            composable(
                route = Routes.LOGIN,
                enterTransition = { loginEnterTransition() },
                exitTransition = { loginExitTransition() },
                popEnterTransition = { loginEnterTransition() },
                popExitTransition = { loginExitTransition() }
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // ─── Main tabs ──────────────────────────────────────────────

            composable(Routes.HOME) {
                HomeScreen(onLogout = navigateToLogin)
            }

            // Quick-action screens accessible from HomeScreen
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(onLogout = navigateToLogin)
            }

            composable(Routes.CARDS) {
                CardsScreen(onLogout = navigateToLogin)
            }

            composable(Routes.EXCHANGE) {
                ExchangeScreen(onLogout = navigateToLogin)
            }

            composable(Routes.OTP) {
                OtpScreen(onLogout = navigateToLogin)
            }

            // ─── Celina 3: Berza ────────────────────────────────────────

            composable(Routes.SECURITIES) {
                SecuritiesScreen(
                    onLogout = navigateToLogin,
                    onListingClick = { listingId ->
                        navController.navigate(Routes.securityDetail(listingId))
                    }
                )
            }

            composable(
                route = Routes.SECURITY_DETAIL,
                arguments = listOf(
                    navArgument("listingId") { type = NavType.LongType }
                ),
                enterTransition = { detailEnterTransition() },
                exitTransition = { defaultExitTransition() },
                popEnterTransition = { defaultPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getLong("listingId") ?: 0L
                SecurityDetailScreen(
                    listingId = listingId,
                    onBack = { navController.popBackStack() },
                    onLogout = navigateToLogin,
                    onOrderClick = { id, direction ->
                        navController.navigate(Routes.createOrder(id, direction))
                    }
                )
            }

            composable(Routes.PORTFOLIO) {
                PortfolioScreen(
                    onLogout = navigateToLogin,
                    onSellClick = { listingId, direction ->
                        navController.navigate(Routes.createOrder(listingId, direction))
                    }
                )
            }

            composable(
                route = Routes.CREATE_ORDER,
                arguments = listOf(
                    navArgument("listingId") { type = NavType.LongType },
                    navArgument("direction") { type = NavType.StringType }
                ),
                enterTransition = { detailEnterTransition() },
                exitTransition = { defaultExitTransition() },
                popEnterTransition = { defaultPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getLong("listingId") ?: 0L
                val direction = backStackEntry.arguments?.getString("direction") ?: "BUY"
                CreateOrderScreen(
                    listingId = listingId,
                    initialDirection = direction,
                    onBack = { navController.popBackStack() },
                    onLogout = navigateToLogin
                )
            }

            composable(Routes.MY_ORDERS) {
                MyOrdersScreen(onLogout = navigateToLogin)
            }
        }
    }
}
