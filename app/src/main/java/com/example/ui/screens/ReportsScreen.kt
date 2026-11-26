package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.KpiCard
import com.example.ui.components.NeonButton
import com.example.ui.theme.DarkBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun ReportsScreen(
    uiState: ErpUiState,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = uiState.currentUser?.isAdmin == true
    val isManagerOrAdmin = uiState.currentUser?.isManagerOrAdmin == true

    var reportType by remember { mutableStateOf("monthly") }
    var monthInput by remember { mutableStateOf(Formatters.currentMonthDhakaIso()) }
    var yearInput by remember { mutableStateOf(Formatters.currentYearDhaka()) }

    val filteredSales = remember(uiState.sales, reportType, monthInput, yearInput) {
        uiState.sales.filter { s ->
            if (reportType == "monthly") {
                Formatters.extractYearMonth(s.saleDate) == monthInput.trim()
            } else {
                Formatters.extractYear(s.saleDate) == yearInput.trim()
            }
        }
    }

    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.sellPrice } }
    val totalProfit = remember(filteredSales) { filteredSales.sumOf { it.profit } }
    val avgMargin = if (totalRevenue > 0) (totalProfit / totalRevenue) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Reports & Analytics", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Sales performance & downloadable PDF statements", color = TextSecondary, fontSize = 12.sp)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "Period Revenue",
                    value = Formatters.formatTaka(totalRevenue),
                    subtitle = "${filteredSales.size} units sold",
                    icon = Icons.Default.AttachMoney,
                    accent = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                if (isAdmin) {
                    KpiCard(
                        title = "Period Profit",
                        value = Formatters.formatTaka(totalProfit),
                        subtitle = String.format(java.util.Locale.US, "%.1f%% avg margin", avgMargin),
                        icon = Icons.Default.TrendingUp,
                        accent = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    KpiCard(
                        title = "Stock Available",
                        value = "${uiState.inventory.count { it.status.equals("Available", true) }}",
                        subtitle = "Units ready for sale",
                        icon = Icons.Default.ShoppingBag,
                        accent = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (isManagerOrAdmin) {
            item {
                JhfCard {
                    Text("Sales Statement PDF", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    JhfDropdown(
                        label = "Report Period Type",
                        items = listOf("monthly", "yearly"),
                        selectedText = if (reportType == "monthly") "Monthly Report" else "Yearly Report",
                        itemLabel = { if (it == "monthly") "Monthly Report (YYYY-MM)" else "Yearly Report (YYYY)" },
                        onSelect = { reportType = it },
                        testTag = "reports_type_dropdown"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (reportType == "monthly") {
                        JhfTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it },
                            label = "Month (YYYY-MM)",
                            testTag = "reports_month_input"
                        )
                    } else {
                        JhfTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            label = "Year (YYYY)",
                            testTag = "reports_year_input"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonButton(
                        text = "Generate Sales Report PDF (${filteredSales.size} sales)",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            if (filteredSales.isEmpty()) {
                                onNotify("No sales found for selected period", true)
                                return@NeonButton
                            }
                            runCatching {
                                val label = if (reportType == "monthly") monthInput.trim() else yearInput.trim()
                                val file = PdfGenerator.generateSalesReportPdf(context, label, filteredSales)
                                PdfGenerator.openPdfOrShare(context, file)
                            }.onFailure { onNotify("PDF error: ${it.message}", true) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "reports_generate_sales_pdf_button"
                    )
                }
            }
        }

        item {
            JhfCard {
                Text("Inventory Stock Statement", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Generates a full landscape PDF of all currently available stock${if (isAdmin) " including buy/sell margin totals" else ""}.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                DarkOutlineButton(
                    text = "Generate Inventory Stock PDF",
                    icon = Icons.Default.Inventory2,
                    tint = NeonGreen,
                    onClick = {
                        runCatching {
                            val file = PdfGenerator.generateInventoryReportPdf(context, uiState.inventory, isAdmin)
                            PdfGenerator.openPdfOrShare(context, file)
                        }.onFailure { onNotify("PDF error: ${it.message}", true) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "reports_generate_stock_pdf_button"
                )
            }
        }
    }
}
