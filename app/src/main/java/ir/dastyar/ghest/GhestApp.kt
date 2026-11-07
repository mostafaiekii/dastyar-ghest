package ir.dastyar.ghest

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.dastyar.ghest.ui.screens.ChecksScreen
import ir.dastyar.ghest.ui.screens.DashboardScreen
import ir.dastyar.ghest.ui.screens.DebtsScreen
import ir.dastyar.ghest.ui.screens.LoanDetailScreen
import ir.dastyar.ghest.ui.screens.LockScreen
import ir.dastyar.ghest.ui.screens.LoansScreen
import ir.dastyar.ghest.ui.screens.ReportsScreen
import ir.dastyar.ghest.ui.screens.SettingsScreen
import ir.dastyar.ghest.ui.theme.GhestTheme
import ir.dastyar.ghest.ui.theme.ThemeMode
import ir.dastyar.ghest.viewmodel.SettingsViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "خانه", Icons.Rounded.Home),
    Tab("loans", "اقساط", Icons.Rounded.AccountBalance),
    Tab("checks", "چک‌ها", Icons.Rounded.ReceiptLong),
    Tab("debts", "قرض‌ها", Icons.Rounded.People),
    Tab("settings", "تنظیمات", Icons.Rounded.Settings)
)

private val TAB_ROUTES = TABS.map { it.route }.toSet()

@Composable
fun GhestApp() {
    val settings: SettingsViewModel = viewModel()
    val theme by settings.themeMode.collectAsStateWithLifecycle()
    val pinEnabled by settings.pinEnabled.collectAsStateWithLifecycle()
    var unlocked by rememberSaveable { mutableStateOf(false) }

    val dark = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    GhestTheme(darkTheme = dark) {
        // اپ فارسی است؛ چیدمان را همیشه راست‌به‌چپ می‌کنیم
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (pinEnabled && !unlocked) {
                    LockScreen(verify = { settings.verifyPin(it) }, onUnlock = { unlocked = true })
                } else {
                    MainScaffold()
                }
            }
        }
    }
}

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: TABS[0].route
    val showBar = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(pad)
        ) {
            composable("home") {
                DashboardScreen(
                    onOpenReports = { nav.navigate("reports") },
                    onOpenLoan = { id -> nav.navigate("loan/$id") }
                )
            }
            composable("loans") {
                LoansScreen(onOpen = { id -> nav.navigate("loan/$id") })
            }
            composable("checks") { ChecksScreen() }
            composable("debts") { DebtsScreen() }
            composable("settings") { SettingsScreen() }
            composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
            composable("loan/{loanId}") { entry ->
                // loanId از طریق SavedStateHandle داخل ViewModel خوانده می‌شود
                LoanDetailScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}
