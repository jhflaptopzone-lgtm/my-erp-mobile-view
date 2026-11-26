package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfTextField
import com.example.ui.components.KpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun DueReportScreen(
    uiState: ErpUiState,
    onRefresh: () -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var customerQuery by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var staffQuery by remember { mutableStateOf("") }
    var overdueOnly by remember { mutableStateOf(false) }

    val filtered = remember(uiState.dueReport, customerQuery, startDate, endDate, staffQuery, overdueOnly) {
        val cq = customerQuery.trim().lowercase()
        val sq = staffQuery.trim().lowercase()
        uiState.dueReport.filter { item ->
            val rem = item.sellPrice - item.totalPaid
            if (rem <= 0.0) return@filter false
            val matchCust = cq.isEmpty() ||
                item.customerName.lowercase().contains(cq) ||
                item.customerPhone.lowercase().contains(cq) ||
                item.invoiceNumber.lowercase().contains(cq)
            val matchStaff = sq.isEmpty() || (item.staffName ?: "").lowercase().contains(sq)
            val salePrefix = item.saleDate.take(10)
            val matchStart = startDate.isBlank() || salePrefix >= startDate.trim()
            val matchEnd = endDate.isBlank() || salePrefix <= endDate.trim()
            val matchOverdue = !overdueOnly || Formatters.isPastDate(item.finalDueDate)
            matchCust && matchStaff && matchStart && matchEnd && matchOverdue
        }
    }

    val totalOutstanding = remember(filtered) { filtered.sumOf { (it.sellPrice - it.totalPaid).coerceAtLeast(0.0) } }
    val overdueCount = remember(filtered) { filtered.count { Formatters.isPastDate(it.finalDueDate) } }
    val dueThisMonthCount = remember(filtered) { filtered.count { Formatters.isDueThisMonth(it.finalDueDate) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Due & Overdue Report", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Outstanding customer receivables", color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("due_report_refresh_button")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh due report", tint = NeonGreen)
                }
            }
        }

        // 4 KPIs in 2x2 grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Total Outstanding",
                        value = Formatters.formatTaka(totalOutstanding),
                        subtitle = "Uncollected due",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = WarningYellow,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Customers w/ Due",
                        value = "${filtered.size}",
                        subtitle = "Active accounts",
                        icon = Icons.Default.People,
                        accent = NeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        title = "Overdue Accounts",
                        value = "$overdueCount",
                        subtitle = "Past final due date",
                        icon = Icons.Default.Warning,
                        accent = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Due This Month",
                        value = "$dueThisMonthCount",
                        subtitle = "Upcoming collections",
                        icon = Icons.Default.Event,
                        accent = InfoBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Filters & Export
        item {
            JhfCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JhfTextField(
                            value = customerQuery,
                            onValueChange = { customerQuery = it },
                            label = "Customer / Phone / Inv",
                            modifier = Modifier.weight(1f),
                            testTag = "due_filter_customer"
                        )
                        JhfTextField(
                            value = staffQuery,
                            onValueChange = { staffQuery = it },
                            label = "Sales Staff",
                            modifier = Modifier.weight(1f),
                            testTag = "due_filter_staff"
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        JhfTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = "From (YYYY-MM-DD)",
                            modifier = Modifier.weight(1f)
                        )
                        JhfTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = "To (YYYY-MM-DD)",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = overdueOnly,
                            onClick = { overdueOnly = !overdueOnly },
                            label = { Text("Overdue Only") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DangerRed,
                                selectedLabelColor = Color.White,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("due_overdue_chip")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DarkOutlineButton(
                                text = "CSV",
                                icon = Icons.Default.TableChart,
                                onClick = {
                                    runCatching {
                                        val csv = buildString {
                                            appendLine("Invoice,Customer,Phone,Model,Serial,SellPrice,Paid,Remaining,FinalDueDate,Staff")
                                            filtered.forEach { d ->
                                                val rem = (d.sellPrice - d.totalPaid).coerceAtLeast(0.0)
                                                appendLine("\"${d.invoiceNumber}\",\"${d.customerName}\",\"${d.customerPhone}\",\"${d.model}\",\"${d.serialNumber}\",${d.sellPrice},${d.totalPaid},$rem,\"${d.finalDueDate ?: ""}\",\"${d.staffName ?: ""}\"")
                                            }
                                        }
                                        val file = PdfGenerator.createCsvFile(context, "JHF_Due_Report_${Formatters.todayDhakaIso()}.csv", csv)
                                        PdfGenerator.shareFile(context, file, "text/csv", "Export Due CSV")
                                    }.onFailure {
                                        onNotify("CSV error: ${it.message}", true)
                                    }
                                },
                                testTag = "due_export_csv_button"
                            )
                            DarkOutlineButton(
                                text = "PDF",
                                icon = Icons.Default.PictureAsPdf,
                                tint = NeonGreen,
                                onClick = {
                                    runCatching {
                                        val file = PdfGenerator.generateDueReportPdf(context, filtered)
                                        PdfGenerator.openPdfOrShare(context, file)
                                    }.onFailure {
                                        onNotify("PDF error: ${it.message}", true)
                                    }
                                },
                                testTag = "due_export_pdf_button"
                            )
                        }
                    }
                }
            }
        }

        if (uiState.dueReportLoading && uiState.dueReport.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }
        } else if (filtered.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No due accounts match your filter", color = TextMuted)
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val remaining = (item.sellPrice - item.totalPaid).coerceAtLeast(0.0)
                val isOverdue = Formatters.isPastDate(item.finalDueDate)
                val isDueSoon = !isOverdue && Formatters.isDueWithinDays(item.finalDueDate, 7)

                JhfCard(
                    accentBorder = when {
                        isOverdue -> DangerRed.copy(alpha = 0.6f)
                        isDueSoon -> WarningYellow.copy(alpha = 0.5f)
                        else -> com.example.ui.theme.DarkBorder
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.customerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${item.customerPhone} • ${item.invoiceNumber}", color = TextSecondary, fontSize = 12.sp)
                        }
                        StatusBadge(
                            label = when {
                                isOverdue -> "Overdue"
                                isDueSoon -> "Due Soon"
                                else -> item.paymentType
                            },
                            color = when {
                                isOverdue -> DangerRed
                                isDueSoon -> WarningYellow
                                else -> NeonGreen
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${item.model} (SN: ${item.serialNumber})", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paid: ${Formatters.formatTaka(item.totalPaid)}", color = NeonGreen, fontSize = 12.sp)
                        Text(
                            "Remaining: ${Formatters.formatTaka(remaining)}",
                            color = if (isOverdue) DangerRed else WarningYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        "Final Due Date: ${Formatters.formatDateBD(item.finalDueDate)}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
