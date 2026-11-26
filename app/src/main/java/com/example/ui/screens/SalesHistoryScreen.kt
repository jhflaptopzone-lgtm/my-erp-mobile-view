package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.SaleRecord
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfTextField
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun SalesHistoryScreen(
    uiState: ErpUiState,
    onRefresh: () -> Unit,
    onReturnSale: (Int) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = uiState.currentUser?.isAdmin == true

    var searchQuery by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("") }
    var dueOnly by remember { mutableStateOf(false) }
    var returnCandidate by remember { mutableStateOf<SaleRecord?>(null) }

    val filtered = remember(uiState.sales, searchQuery, dateFilter, dueOnly) {
        val q = searchQuery.trim().lowercase()
        val d = dateFilter.trim()
        uiState.sales.filter { s ->
            val matchQuery = q.isEmpty() ||
                s.invoiceNumber.lowercase().contains(q) ||
                s.customerName.lowercase().contains(q) ||
                s.customerPhone.lowercase().contains(q) ||
                (s.model ?: "").lowercase().contains(q) ||
                (s.serialNumber ?: "").lowercase().contains(q)
            val matchDate = d.isEmpty() || s.saleDate.contains(d)
            val matchDue = !dueOnly || s.paymentType.equals("EMI", ignoreCase = true) || s.paymentType.equals("Due", ignoreCase = true)
            matchQuery && matchDate && matchDue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sales History", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("${filtered.size} invoices synced", color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("sales_history_refresh_button")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh sales", tint = NeonGreen)
                }
            }

            JhfTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search invoice, customer, phone, model, SN",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
                testTag = "sales_history_search_input"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                JhfTextField(
                    value = dateFilter,
                    onValueChange = { dateFilter = it },
                    label = "Filter Date (YYYY-MM-DD)",
                    modifier = Modifier.weight(1f),
                    testTag = "sales_history_date_input"
                )
                FilterChip(
                    selected = dueOnly,
                    onClick = { dueOnly = !dueOnly },
                    label = { Text("EMI / Due") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkCard,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("sales_history_due_chip")
                )
            }
        }

        if (uiState.salesLoading && uiState.sales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sales records match your filter", color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { sale ->
                    val badgeColor = when {
                        sale.paymentType.equals("EMI", ignoreCase = true) -> InfoBlue
                        sale.paymentType.equals("Due", ignoreCase = true) -> WarningYellow
                        else -> NeonGreen
                    }
                    JhfCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sale.invoiceNumber,
                                    color = NeonGreen,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(sale.customerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(sale.customerPhone, color = TextSecondary, fontSize = 12.sp)
                            }
                            StatusBadge(label = sale.paymentType, color = badgeColor)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${sale.model ?: "Laptop"} • SN: ${sale.serialNumber ?: "N/A"}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date: ${Formatters.formatDateBD(sale.saleDate)}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatTaka(sale.sellPrice),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isAdmin) {
                                    Text(
                                        text = "Profit: ${Formatters.formatTaka(sale.profit)}",
                                        color = NeonGreen,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DarkOutlineButton(
                                text = "PDF",
                                icon = Icons.Default.PictureAsPdf,
                                tint = NeonGreen,
                                onClick = {
                                    runCatching {
                                        val file = PdfGenerator.generateInvoicePdf(context, sale)
                                        PdfGenerator.openPdfOrShare(context, file)
                                    }.onFailure {
                                        onNotify("PDF error: ${it.message}", true)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "sale_pdf_${sale.id}"
                            )
                            if (!sale.invoicePublicToken.isNullOrBlank()) {
                                DarkOutlineButton(
                                    text = "Link",
                                    icon = Icons.Default.ContentCopy,
                                    onClick = {
                                        val link = "${uiState.serverUrl.trimEnd('/')}/invoice/view/${sale.invoiceNumber}?token=${sale.invoicePublicToken}"
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cb.setPrimaryClip(ClipData.newPlainText("Invoice Link", link))
                                        onNotify("Public share link copied", false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "sale_copy_link_${sale.id}"
                                )
                            }
                            DarkOutlineButton(
                                text = "Return",
                                icon = Icons.Default.RotateLeft,
                                tint = DangerRed,
                                onClick = { returnCandidate = sale },
                                modifier = Modifier.weight(1f),
                                testTag = "sale_return_${sale.id}"
                            )
                        }
                    }
                }
            }
        }
    }

    returnCandidate?.let { sale ->
        ConfirmActionDialog(
            title = "Return Sale & Restore Stock",
            message = "Return ${sale.invoiceNumber} (${sale.model ?: "Laptop"})? Stock will revert to Available on mobile & live web.",
            confirmLabel = "Confirm Return",
            onConfirm = { onReturnSale(sale.id) },
            onDismiss = { returnCandidate = null }
        )
    }
}
