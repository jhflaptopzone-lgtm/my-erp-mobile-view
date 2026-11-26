package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.ToastBanner
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DueReportScreen
import com.example.ui.screens.EmiTrackerScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PublicInvoiceScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.SalesPosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StaffScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpViewModel
import kotlinx.serialization.Serializable

@Serializable data object RouteLogin
@Serializable data object RoutePublicInvoice
@Serializable data object RouteDashboard
@Serializable data object RouteInventory
@Serializable data object RouteSalesPos
@Serializable data object RouteSalesHistory
@Serializable data object RouteEmiTracker
@Serializable data object RouteDueReport
@Serializable data object RouteSuppliers
@Serializable data object RouteReports
@Serializable data object RouteStaff
@Serializable data object RouteSettings

private data class NavMenuItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: ErpViewModel,
    firebaseUid: String?,
    firebaseUserEmail: String?
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var showMoreSheet by remember { mutableStateOf(false) }

    // Scope Firestore listeners strictly to the signed-in Google user
    if (firebaseUid != null) {
        key(firebaseUid) {
            LaunchedEffect(firebaseUid) {
                viewModel.observeFirebaseForUser(firebaseUid)
            }
        }
    } else {
        LaunchedEffect(Unit) {
            viewModel.clearFirebaseState()
        }
    }

    val isLoggedIn = uiState.isLoggedIn
    val isAdmin = uiState.currentUser?.isAdmin == true
    val isManagerOrAdmin = uiState.currentUser?.isManagerOrAdmin == true

    // React cleanly to login/logout state changes
    LaunchedEffect(isLoggedIn) {
        val currentEntry = navController.currentBackStackEntry
        val onAuthRoute = currentEntry?.destination?.hasRoute<RouteLogin>() == true ||
            currentEntry?.destination?.hasRoute<RoutePublicInvoice>() == true
        if (isLoggedIn && onAuthRoute) {
            navController.navigate(RouteDashboard) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!isLoggedIn && !onAuthRoute) {
            navController.navigate(RouteLogin) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(RouteDashboard) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val primaryNavItems = remember {
        listOf(
            NavMenuItem(RouteDashboard, "Home", Icons.Default.Dashboard, "nav_dashboard"),
            NavMenuItem(RouteInventory, "Stock", Icons.Default.LaptopMac, "nav_inventory"),
            NavMenuItem(RouteSalesPos, "POS", Icons.Default.PointOfSale, "nav_sales_pos"),
            NavMenuItem(RouteEmiTracker, "EMI", Icons.Default.AccountBalanceWallet, "nav_emi_tracker")
        )
    }

    val secondaryNavItems = remember(isAdmin, isManagerOrAdmin) {
        buildList {
            add(NavMenuItem(RouteSalesHistory, "Sales History", Icons.Default.History, "nav_sales_history"))
            add(NavMenuItem(RouteDueReport, "Due Report", Icons.Default.Warning, "nav_due_report"))
            if (isManagerOrAdmin) {
                add(NavMenuItem(RouteSuppliers, "Suppliers", Icons.Default.LocalShipping, "nav_suppliers"))
            }
            add(NavMenuItem(RouteReports, "Reports & PDF", Icons.Default.BarChart, "nav_reports"))
            if (isManagerOrAdmin) {
                add(NavMenuItem(RouteStaff, "Staff", Icons.Default.People, "nav_staff"))
            }
            add(NavMenuItem(RouteSettings, "Settings & Sync", Icons.Default.Settings, "nav_settings"))
            add(NavMenuItem(RoutePublicInvoice, "Public Invoice", Icons.Default.ReceiptLong, "nav_public_invoice"))
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val showAppChrome = isLoggedIn &&
        backStackEntry?.destination?.hasRoute<RouteLogin>() != true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = DarkBg,
                bottomBar = {
                    if (showAppChrome && !useRail) {
                        NavigationBar(
                            containerColor = DarkCard,
                            tonalElevation = 0.dp
                        ) {
                            primaryNavItems.forEach { item ->
                                val selected = backStackEntry?.destination?.hasRoute(item.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                            NavigationBarItem(
                                selected = showMoreSheet,
                                onClick = { showMoreSheet = true },
                                icon = { Icon(Icons.Default.Apps, contentDescription = "More tools") },
                                label = { Text("More", fontSize = 11.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_more")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showAppChrome && useRail) {
                        NavigationRail(
                            containerColor = DarkCard,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            (primaryNavItems + secondaryNavItems).forEach { item ->
                                val selected = backStackEntry?.destination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = NeonGreen,
                                        indicatorColor = NeonGreen,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            NavigationRailItem(
                                selected = false,
                                onClick = { viewModel.logout() },
                                icon = { Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = DangerRed) },
                                label = { Text("Sign Out", fontSize = 10.sp, color = DangerRed) },
                                modifier = Modifier.testTag("nav_logout")
                            )
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) RouteDashboard else RouteLogin,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        composable<RouteLogin> {
                            LoginScreen(
                                uiState = uiState,
                                firebaseUserEmail = firebaseUserEmail,
                                onLogin = viewModel::login,
                                onGoogleAuthSuccess = {
                                    viewModel.notify("Google Cloud account linked")
                                },
                                onGoogleAuthError = { msg ->
                                    viewModel.notify(msg, isError = true)
                                },
                                onOpenPublicInvoice = {
                                    navController.navigate(RoutePublicInvoice)
                                }
                            )
                        }

                        composable<RoutePublicInvoice> {
                            PublicInvoiceScreen(
                                uiState = uiState,
                                onLookup = viewModel::lookupPublicInvoice,
                                onBack = { navController.popBackStack() },
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteDashboard> {
                            DashboardScreen(
                                uiState = uiState,
                                onRefresh = { viewModel.refreshAllData() },
                                onQuickAction = { key ->
                                    when (key) {
                                        "sales_pos" -> navigateTopLevel(RouteSalesPos)
                                        "inventory" -> navigateTopLevel(RouteInventory)
                                        "emi_tracker" -> navigateTopLevel(RouteEmiTracker)
                                    }
                                }
                            )
                        }

                        composable<RouteInventory> {
                            InventoryScreen(
                                uiState = uiState,
                                onAddItem = viewModel::addInventoryItem,
                                onUpdateItem = viewModel::updateInventoryItem,
                                onDeleteItem = viewModel::deleteInventoryItem,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteSalesPos> {
                            SalesPosScreen(
                                uiState = uiState,
                                onCreateSale = viewModel::createSale,
                                onDismissInvoice = viewModel::dismissInvoiceReady,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteSalesHistory> {
                            SalesHistoryScreen(
                                uiState = uiState,
                                onRefresh = viewModel::loadSales,
                                onReturnSale = viewModel::returnSale,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteEmiTracker> {
                            EmiTrackerScreen(
                                uiState = uiState,
                                onLoadInstallments = viewModel::loadInstallments,
                                onRecordPayment = viewModel::recordInstallmentPayment,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteDueReport> {
                            DueReportScreen(
                                uiState = uiState,
                                onRefresh = viewModel::loadDueReport,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteSuppliers> {
                            SuppliersScreen(
                                uiState = uiState,
                                onSaveSupplier = viewModel::saveSupplier,
                                onDeleteSupplier = viewModel::deleteSupplier,
                                onLoadLedger = viewModel::loadSupplierLedger,
                                onRecordPayment = viewModel::recordSupplierPayment,
                                onFetchReport = viewModel::fetchSupplierReport,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteReports> {
                            ReportsScreen(
                                uiState = uiState,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteStaff> {
                            StaffScreen(
                                uiState = uiState,
                                onSaveUser = viewModel::saveUser,
                                onDeleteUser = viewModel::deleteUser,
                                onNotify = viewModel::notify
                            )
                        }

                        composable<RouteSettings> {
                            SettingsScreen(
                                uiState = uiState,
                                firebaseUserEmail = firebaseUserEmail,
                                onUpdateServerUrl = viewModel::updateServerUrl,
                                onCheckHealth = viewModel::checkServerHealth,
                                onForceCloudSync = viewModel::forceCloudSyncAll,
                                onRehydrateWeb = viewModel::rehydrateWebFromCloud,
                                onDownloadBackup = viewModel::downloadBackup,
                                onRestoreBackup = viewModel::restoreBackup,
                                onResetDemoData = viewModel::resetDemoData,
                                onGoogleAuthSuccess = {
                                    viewModel.notify("Google Cloud account linked")
                                },
                                onGoogleAuthError = { msg ->
                                    viewModel.notify(msg, isError = true)
                                },
                                onGoogleSignOut = {
                                    viewModel.clearFirebaseState()
                                    viewModel.notify("Google Cloud account unlinked")
                                },
                                onNotify = viewModel::notify
                            )
                        }
                    }
                }
            }
        }

        ToastBanner(
            toast = uiState.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (showAppChrome) 80.dp else 16.dp)
        )
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${uiState.currentUser?.name ?: "Operator"} (${uiState.currentUser?.role ?: "Staff"})",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Synced with ${uiState.serverUrl}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                secondaryNavItems.forEach { item ->
                    Surface(
                        color = DarkBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreSheet = false
                                navigateTopLevel(item.route)
                            }
                            .testTag("more_${item.testTag}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(item.icon, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(item.label, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = DangerRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMoreSheet = false
                            viewModel.logout()
                        }
                        .testTag("more_logout_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Sign Out", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
