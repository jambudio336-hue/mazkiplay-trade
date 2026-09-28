package com.mazkiplay.trade.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mazkiplay.trade.MazkiplayApp
import com.mazkiplay.trade.ui.screens.AlarmScreen
import com.mazkiplay.trade.ui.screens.AnalysisScreen
import com.mazkiplay.trade.ui.screens.CalculatorScreen
import com.mazkiplay.trade.ui.screens.CopyTradeScreen
import com.mazkiplay.trade.ui.screens.DashboardScreen
import com.mazkiplay.trade.ui.screens.HistoryScreen
import com.mazkiplay.trade.ui.screens.LiveMarketScreen
import com.mazkiplay.trade.ui.screens.MarketAnalysisScreen
import com.mazkiplay.trade.ui.screens.MarketScreen
import com.mazkiplay.trade.ui.screens.NewsScreen
import com.mazkiplay.trade.ui.screens.RiskCalculatorScreen
import com.mazkiplay.trade.ui.screens.SessionsScreen
import com.mazkiplay.trade.ui.screens.SettingsScreen
import com.mazkiplay.trade.ui.screens.SplashScreen
import com.mazkiplay.trade.ui.screens.TradeScreen
import com.mazkiplay.trade.ui.screens.WatchlistScreen
import com.mazkiplay.trade.ui.screens.VideoNewsScreen
import kotlinx.coroutines.launch

/** Every destination in the app. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val MARKET = "market"
    const val NEWS = "news"
    const val ANALYSIS = "analysis"
    const val TRADE = "trade"
    const val CALCULATOR = "calculator"
    const val RISK = "risk"
    const val HISTORY = "history"
    const val COPY_TRADE = "copytrade"
    const val ALARM = "alarm"
    const val SESSIONS = "sessions"
    const val MARKET_ANALYSIS = "marketanalysis"
    const val WATCHLIST = "watchlist"
    const val LIVE_MARKET = "livemarket"
    const val VIDEO_NEWS = "videonews"
    const val SETTINGS = "settings"
}

data class NavItem(val route: String, val labelKey: String, val icon: ImageVector)

/**
 * Root navigation graph.
 *
 * Five primary destinations live in the bottom bar; the remaining analytic and
 * configuration screens sit in the drawer so the bar stays uncluttered on phones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazkiplayNavHost(app: MazkiplayApp) {
    var splashDone by remember { mutableStateOf(false) }
    val prefs = rememberPreferences(app)

    if (!splashDone) {
        SplashScreen(prefs = prefs, onFinished = { splashDone = true })
        return
    }

    val s = stringsOf(prefs)
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val bottomItems = listOf(
        NavItem(Routes.DASHBOARD, s.dashboard, Icons.Filled.Dashboard),
        NavItem(Routes.MARKET, s.market, Icons.Filled.ShowChart),
        NavItem(Routes.NEWS, s.news, Icons.Filled.Article),
        NavItem(Routes.ANALYSIS, s.analysis, Icons.Filled.Analytics),
        NavItem(Routes.TRADE, s.trade, Icons.Filled.SwapVert)
    )

    val drawerItems = listOf(
        NavItem(Routes.LIVE_MARKET, "Live Market · TradingView", Icons.Filled.ShowChart),
        NavItem(Routes.VIDEO_NEWS, "Video News · YouTube", Icons.Filled.VideoLibrary),
        NavItem(Routes.CALCULATOR, s.calculator, Icons.Filled.Calculate),
        NavItem(Routes.RISK, s.risk, Icons.Filled.Shield),
        NavItem(Routes.HISTORY, s.history, Icons.Filled.History),
        NavItem(Routes.COPY_TRADE, s.copyTrade, Icons.Filled.People),
        NavItem(Routes.ALARM, s.alarm, Icons.Filled.NotificationsActive),
        NavItem(Routes.SESSIONS, s.sessions, Icons.Filled.Schedule),
        NavItem(Routes.MARKET_ANALYSIS, s.marketAnalysis, Icons.Filled.TrendingUp),
        NavItem(Routes.WATCHLIST, s.watchlist, Icons.Filled.Star),
        NavItem(Routes.SETTINGS, s.settings, Icons.Filled.Settings)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentLabel = (bottomItems + drawerItems).firstOrNull { it.route == currentRoute }?.labelKey
        ?: s.dashboard

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Mazkiplay Trade",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(18.dp)
                )
                Text(
                    text = "Nusantara Forex \u2022 By.mazkiplayTrade",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
                HorizontalDivider()
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.labelKey) },
                        icon = { Icon(item.icon, contentDescription = item.labelKey) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) { launchSingleTop = true }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                    )
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Tentang Aplikasi") },
                    icon = { Icon(Icons.Filled.Public, contentDescription = null) },
                    selected = false,
                    onClick = {
                        navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        androidx.compose.foundation.layout.Column {
                            Text(currentLabel, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "By.mazkiplayTrade",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.labelKey) },
                            label = { Text(item.labelKey, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.DASHBOARD,
                modifier = Modifier.padding(padding)
            ) {
                composable(Routes.DASHBOARD) {
                    DashboardScreen(app, prefs) { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    }
                }
                composable(Routes.MARKET) { MarketScreen(app, prefs) }
                composable(Routes.NEWS) { NewsScreen(app, prefs) }
                composable(Routes.ANALYSIS) { AnalysisScreen(app, prefs) }
                composable(Routes.TRADE) { TradeScreen(app, prefs) }
                composable(Routes.CALCULATOR) { CalculatorScreen(app, prefs) }
                composable(Routes.RISK) { RiskCalculatorScreen(app, prefs) }
                composable(Routes.HISTORY) { HistoryScreen(app, prefs) }
                composable(Routes.COPY_TRADE) { CopyTradeScreen(app, prefs) }
                composable(Routes.ALARM) { AlarmScreen(app, prefs) }
                composable(Routes.SESSIONS) { SessionsScreen(app, prefs) }
                composable(Routes.MARKET_ANALYSIS) { MarketAnalysisScreen(app, prefs) }
                composable(Routes.WATCHLIST) { WatchlistScreen(app, prefs) }
                composable(Routes.LIVE_MARKET) { LiveMarketScreen(app, prefs) }
                composable(Routes.VIDEO_NEWS) { VideoNewsScreen(app, prefs) }
                composable(Routes.SETTINGS) { SettingsScreen(app, prefs) }
            }
        }
    }
}
