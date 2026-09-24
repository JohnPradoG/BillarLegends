package com.billarlegends.portfolio.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.billarlegends.portfolio.PortfolioApp
import com.billarlegends.portfolio.ui.accounts.AccountsScreen
import com.billarlegends.portfolio.ui.accounts.AccountsViewModel
import com.billarlegends.portfolio.ui.accounts.AddBinanceAccountScreen
import com.billarlegends.portfolio.ui.accounts.AddExnessConnectionScreen
import com.billarlegends.portfolio.ui.businesses.BusinessDetailScreen
import com.billarlegends.portfolio.ui.businesses.BusinessDetailViewModel
import com.billarlegends.portfolio.ui.businesses.BusinessesScreen
import com.billarlegends.portfolio.ui.businesses.BusinessesViewModel
import com.billarlegends.portfolio.ui.dashboard.DashboardScreen
import com.billarlegends.portfolio.ui.dashboard.DashboardViewModel

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Destinations.DASHBOARD, "Resumen", Icons.Default.Home),
    BottomTab(Destinations.ACCOUNTS, "Cuentas", Icons.Default.AccountBalance),
    BottomTab(Destinations.BUSINESSES, "Negocios", Icons.Default.Store),
)

@Composable
fun PortfolioNavHost() {
    val app = LocalContext.current.applicationContext as PortfolioApp
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.DASHBOARD,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destinations.DASHBOARD) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = viewModelFactory { initializer { DashboardViewModel(app.portfolioRepository) } },
                )
                DashboardScreen(viewModel)
            }

            composable(Destinations.ACCOUNTS) {
                val viewModel: AccountsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AccountsViewModel(app.binanceRepository, app.exnessRepository) }
                    },
                )
                AccountsScreen(
                    viewModel = viewModel,
                    onAddBinance = { navController.navigate(Destinations.ADD_BINANCE_ACCOUNT) },
                    onAddExness = { navController.navigate(Destinations.ADD_EXNESS_CONNECTION) },
                )
            }

            composable(Destinations.ADD_BINANCE_ACCOUNT) {
                val viewModel: AccountsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AccountsViewModel(app.binanceRepository, app.exnessRepository) }
                    },
                )
                AddBinanceAccountScreen(viewModel) { navController.popBackStack() }
            }

            composable(Destinations.ADD_EXNESS_CONNECTION) {
                val viewModel: AccountsViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { AccountsViewModel(app.binanceRepository, app.exnessRepository) }
                    },
                )
                AddExnessConnectionScreen(viewModel) { navController.popBackStack() }
            }

            composable(Destinations.BUSINESSES) {
                val viewModel: BusinessesViewModel = viewModel(
                    factory = viewModelFactory { initializer { BusinessesViewModel(app.businessRepository) } },
                )
                BusinessesScreen(viewModel) { businessId ->
                    navController.navigate(Destinations.businessDetail(businessId))
                }
            }

            composable(
                route = Destinations.BUSINESS_DETAIL,
                arguments = listOf(navArgument("businessId") { type = androidx.navigation.NavType.LongType }),
            ) { backStackEntry ->
                val businessId = backStackEntry.arguments?.getLong("businessId") ?: return@composable
                val businessesViewModel: BusinessesViewModel = viewModel(
                    factory = viewModelFactory { initializer { BusinessesViewModel(app.businessRepository) } },
                )
                val businesses by businessesViewModel.businesses.collectAsState()
                val businessName = businesses.firstOrNull { it.id == businessId }?.name ?: "Negocio"

                val viewModel: BusinessDetailViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { BusinessDetailViewModel(app.businessRepository, businessId) }
                    },
                )
                BusinessDetailScreen(businessName, viewModel)
            }
        }
    }
}
