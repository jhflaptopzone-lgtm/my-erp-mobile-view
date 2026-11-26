package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.JhfCard
import com.example.ui.components.KpiCard
import com.example.ui.components.MonthlySalesLineChart
import com.example.ui.components.PaymentDistributionChart
import com.example.ui.components.SyncBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    uiState: ErpUiState,
    onRefresh: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    val stats = uiState.dashboardStats
    val isAdmin = uiState.currentUser?.isAdmin == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (uiState.dashboardLoading && stats == null) {
            CircularProgressIndicator(
                color = NeonGreen,
                modifier = Modifier.align(Alignment.Center)
            )
            return
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 680.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Welcome, ${uiState.currentUser?.name ?: "Operator"} (${uiState.currentUser?.role ?: "Staff"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SyncBadge(connected = uiState.healthConnected)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.testTag("dashboard_refresh_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh dashboard", tint = NeonGreen)
                            }
                        }
                    }
                }

                // Primary KPIs
                item {
                    val safeStats = stats
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiCard(
                                title = if (isAdmin) "Total Sales" else "My Total Sales",
                                value = Formatters.formatTaka(safeStats?.totalSalesAmount ?: 0.0),
                                subtitle = "${safeStats?.totalSalesCount ?: 0} laptops sold",
                                icon = Icons.Default.ShoppingBag,
                                accent = NeonGreen,
                                modifier = Modifier.weight(1f)
                            )
                            if (isAdmin) {
                                KpiCard(
                                    title = "Total Profit",
                                    value = Formatters.formatTaka(safeStats?.totalProfit ?: 0.0),
                                    subtitle = "Net margin realized",
                                    icon = Icons.Default.TrendingUp,
                                    accent = InfoBlue,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            KpiCard(
                                title = "EMI / Due",
                                value = Formatters.formatTaka(safeStats?.emiOutstanding ?: 0.0),
                                subtitle = "Pending installments",
                                icon = Icons.Default.AccountBalanceWallet,
                                accent = WarningYellow,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Low Stock",
                                value = "${safeStats?.lowStockCount ?: 0}",
                                subtitle = "Models < 2 units",
                                icon = Icons.Default.Warning,
                                accent = DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KpiCard(
                                    title = if (isAdmin) "Total Sales" else "My Sales",
                                    value = Formatters.formatTaka(safeStats?.totalSalesAmount ?: 0.0),
                                    subtitle = "${safeStats?.totalSalesCount ?: 0} units",
                                    icon = Icons.Default.ShoppingBag,
                                    accent = NeonGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiCard(
                                    title = if (isAdmin) "Net Profit" else "EMI / Due",
                                    value = if (isAdmin) Formatters.formatTaka(safeStats?.totalProfit ?: 0.0)
                                    else Formatters.formatTaka(safeStats?.emiOutstanding ?: 0.0),
                                    subtitle = if (isAdmin) "Realized profit" else "Outstanding",
                                    icon = if (isAdmin) Icons.Default.TrendingUp else Icons.Default.AccountBalanceWallet,
                                    accent = if (isAdmin) InfoBlue else WarningYellow,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isAdmin) {
                                    KpiCard(
                                        title = "EMI / Due",
                                        value = Formatters.formatTaka(safeStats?.emiOutstanding ?: 0.0),
                                        subtitle = "Installments due",
                                        icon = Icons.Default.AccountBalanceWallet,
                                        accent = WarningYellow,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                KpiCard(
                                    title = "Low Stock",
                                    value = "${safeStats?.lowStockCount ?: 0}",
                                    subtitle = "Models < 2 units",
                                    icon = Icons.Default.Warning,
                                    accent = DangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Quick shortcuts
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickShortcutChip(
                            label = "New Sale POS",
                            icon = Icons.Default.ShoppingBag,
                            onClick = { onQuickAction("sales_pos") },
                            modifier = Modifier.weight(1f).testTag("quick_sales_pos")
                        )
                        QuickShortcutChip(
                            label = "Add Stock",
                            icon = Icons.Default.LaptopMac,
                            onClick = { onQuickAction("inventory") },
                            modifier = Modifier.weight(1f).testTag("quick_inventory")
                        )
                        QuickShortcutChip(
                            label = "Due Tracker",
                            icon = Icons.Default.Payments,
                            onClick = { onQuickAction("emi_tracker") },
                            modifier = Modifier.weight(1f).testTag("quick_emi")
                        )
                    }
                }

                // Charts
                item {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            JhfCard(modifier = Modifier.weight(1f)) {
                                Text("Monthly Sales Trend", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                MonthlySalesLineChart(points = stats?.monthlySales ?: emptyList())
                            }
                            JhfCard(modifier = Modifier.weight(1f)) {
                                Text("Payment Distribution", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                PaymentDistributionChart(methods = stats?.paymentMethods ?: emptyList())
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            JhfCard {
                                Text("Monthly Sales Trend", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                MonthlySalesLineChart(points = stats?.monthlySales ?: emptyList())
                            }
                            JhfCard {
                                Text("Payment Distribution", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                PaymentDistributionChart(methods = stats?.paymentMethods ?: emptyList())
                            }
                        }
                    }
                }

                // Admin Supplier Summary
                if (isAdmin && stats != null) {
                    item {
                        JhfCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Supplier Summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SupplierMiniBox(
                                    label = "Total Supplier Due",
                                    value = Formatters.formatTaka(stats.supplierStats.totalDue),
                                    color = DangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                                SupplierMiniBox(
                                    label = "Monthly Purchase",
                                    value = Formatters.formatTaka(stats.supplierStats.monthlyPurchase),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                SupplierMiniBox(
                                    label = "Monthly Payment",
                                    value = Formatters.formatTaka(stats.supplierStats.monthlyPayment),
                                    color = NeonGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (stats.supplierStats.topDues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Top Supplier Dues", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                stats.supplierStats.topDues.forEachIndexed { idx, sup ->
                                    if (idx > 0) HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sup.name, color = Color.White, fontSize = 13.sp)
                                        Text(
                                            Formatters.formatTaka(sup.due),
                                            color = if (sup.due > 0) DangerRed else NeonGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = com.example.ui.theme.DarkCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = NeonGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SupplierMiniBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBg)
            .padding(10.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
