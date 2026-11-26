package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstallmentRecord
import com.example.data.model.SaleRecord
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters

@Composable
fun EmiTrackerScreen(
    uiState: ErpUiState,
    onLoadInstallments: (Int) -> Unit,
    onRecordPayment: (saleId: Int, installmentId: Int, amount: Double, paymentType: String, note: String, onDone: () -> Unit) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val emiSales = remember(uiState.sales) {
        uiState.sales.filter {
            it.paymentType.equals("EMI", ignoreCase = true) || it.paymentType.equals("Due", ignoreCase = true)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSale by remember { mutableStateOf<SaleRecord?>(null) }
    var payingInstallment by remember { mutableStateOf<InstallmentRecord?>(null) }

    val filteredSales = remember(emiSales, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emiSales
        else emiSales.filter {
            it.customerName.lowercase().contains(q) ||
                it.customerPhone.lowercase().contains(q) ||
                it.invoiceNumber.lowercase().contains(q) ||
                (it.model ?: "").lowercase().contains(q)
        }
    }

    LaunchedEffect(selectedSale?.id) {
        selectedSale?.let { onLoadInstallments(it.id) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                    EmiSalesListPane(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        sales = filteredSales,
                        selectedId = selectedSale?.id,
                        onSelect = { selectedSale = it }
                    )
                }
                Column(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    val active = selectedSale
                    if (active == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select an EMI or Due sale to inspect schedule", color = TextMuted)
                        }
                    } else {
                        EmiDetailPane(
                            sale = active,
                            uiState = uiState,
                            showBackButton = false,
                            onBack = {},
                            onPayClick = { payingInstallment = it }
                        )
                    }
                }
            }
        } else {
            val active = selectedSale
            if (active != null) {
                BackHandler { selectedSale = null }
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    EmiDetailPane(
                        sale = active,
                        uiState = uiState,
                        showBackButton = true,
                        onBack = { selectedSale = null },
                        onPayClick = { payingInstallment = it }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    EmiSalesListPane(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        sales = filteredSales,
                        selectedId = null,
                        onSelect = { selectedSale = it }
                    )
                }
            }
        }
    }

    payingInstallment?.let { inst ->
        val saleId = selectedSale?.id ?: return@let
        RecordInstallmentPaymentDialog(
            installment = inst,
            busy = uiState.globalBusy,
            onDismiss = { payingInstallment = null },
            onSubmit = { amount, type, note ->
                if (amount <= 0.0) {
                    onNotify("Enter a valid payment amount", true)
                    return@RecordInstallmentPaymentDialog
                }
                onRecordPayment(saleId, inst.id, amount, type, note) {
                    payingInstallment = null
                }
            }
        )
    }
}

@Composable
private fun EmiSalesListPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    sales: List<SaleRecord>,
    selectedId: Int?,
    onSelect: (SaleRecord) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        Text("EMI & Due Tracker", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        JhfTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            label = "Search customer, phone, invoice",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
            testTag = "emi_search_input"
        )
        if (sales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No EMI or Due sales found", color = TextMuted)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sales, key = { it.id }) { sale ->
                    val isSelected = selectedId == sale.id
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NeonGreen else DarkBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(sale) }
                            .testTag("emi_sale_card_${sale.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sale.customerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                StatusBadge(
                                    label = sale.paymentType,
                                    color = if (sale.paymentType.equals("EMI", true)) InfoBlue else WarningYellow
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${sale.invoiceNumber} • ${sale.customerPhone}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sale.model ?: "Laptop", color = TextMuted, fontSize = 12.sp)
                                Text(
                                    Formatters.formatTaka(sale.sellPrice),
                                    color = NeonGreen,
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

@Composable
private fun EmiDetailPane(
    sale: SaleRecord,
    uiState: ErpUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onPayClick: (InstallmentRecord) -> Unit
) {
    val totalPaid = remember(uiState.selectedInstallments) {
        uiState.selectedInstallments.sumOf { it.paidAmount }
    }
    val remaining = (sale.sellPrice - totalPaid).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("emi_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to EMI list", tint = NeonGreen)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(sale.customerName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("${sale.invoiceNumber} • ${sale.model ?: "Laptop"}", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        item {
            JhfCard(accentBorder = NeonGreen.copy(alpha = 0.3f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryMetric("Total Price", Formatters.formatTaka(sale.sellPrice), Color.White)
                    SummaryMetric("Paid", Formatters.formatTaka(totalPaid), NeonGreen)
                    SummaryMetric("Remaining", Formatters.formatTaka(remaining), WarningYellow)
                }
            }
        }

        if (uiState.installmentsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }
        } else {
            item {
                Text("Installment Schedule", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            items(uiState.selectedInstallments, key = { it.id }) { inst ->
                val isPaid = inst.status.equals("Paid", ignoreCase = true)
                JhfCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Installment #${inst.installmentNumber}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Due: ${Formatters.formatDateBD(inst.dueDate)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                "Paid ${Formatters.formatTaka(inst.paidAmount)} of ${Formatters.formatTaka(inst.amount)}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        if (isPaid) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Paid", tint = NeonGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PAID", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            NeonButton(
                                text = "Pay",
                                icon = Icons.Default.Payments,
                                onClick = { onPayClick(inst) },
                                testTag = "emi_pay_button_${inst.id}"
                            )
                        }
                    }
                }
            }

            if (uiState.selectedSalePayments.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Payment History", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                item {
                    JhfCard {
                        uiState.selectedSalePayments.forEachIndexed { idx, pay ->
                            if (idx > 0) HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "${pay.paymentType ?: "Cash"} • ${Formatters.formatDateBD(pay.paymentDate)}",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!pay.note.isNullOrBlank()) {
                                        Text(pay.note, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    Formatters.formatTaka(pay.amount),
                                    color = NeonGreen,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun RecordInstallmentPaymentDialog(
    installment: InstallmentRecord,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, type: String, note: String) -> Unit
) {
    val suggested = (installment.amount - installment.paidAmount).coerceAtLeast(0.0)
    var amountText by remember(installment.id) { mutableStateOf(suggested.toInt().toString()) }
    var paymentType by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Pay Installment #${installment.installmentNumber}", color = NeonGreen, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Remaining on installment: ${Formatters.formatTaka(suggested)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                JhfTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "Payment Amount (TK)",
                    keyboardType = KeyboardType.Number,
                    testTag = "emi_payment_amount_input"
                )
                JhfDropdown(
                    label = "Payment Method",
                    items = listOf("Cash", "Bank", "Bkash", "Nagad", "Rocket", "POS"),
                    selectedText = paymentType,
                    itemLabel = { it },
                    onSelect = { paymentType = it }
                )
                JhfTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Note (Optional)",
                    testTag = "emi_payment_note_input"
                )
            }
        },
        confirmButton = {
            NeonButton(
                text = if (busy) "Saving..." else "Confirm Pay",
                enabled = !busy,
                onClick = { onSubmit(amountText.toDoubleOrNull() ?: 0.0, paymentType, note) },
                testTag = "emi_confirm_payment_button"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
