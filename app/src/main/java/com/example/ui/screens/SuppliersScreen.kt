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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SupplierItem
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    uiState: ErpUiState,
    onSaveSupplier: (editingId: Int?, name: String, mobile: String, address: String, openingBalance: Double, onDone: () -> Unit) -> Unit,
    onDeleteSupplier: (Int) -> Unit,
    onLoadLedger: (Int) -> Unit,
    onRecordPayment: (supplierId: Int, paymentDate: String, amount: Double, paymentType: String, note: String, onDone: () -> Unit) -> Unit,
    onFetchReport: (supplierId: Int?, startDate: String, endDate: String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val tabs = listOf("Suppliers", "Ledger", "Pay Supplier", "Reports")
    var selectedTab by remember { mutableIntStateOf(0) }

    var showFormSheet by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierItem?>(null) }
    var deleteCandidate by remember { mutableStateOf<SupplierItem?>(null) }

    // Add/Edit form fields
    var supName by remember { mutableStateOf("") }
    var supMobile by remember { mutableStateOf("") }
    var supAddress by remember { mutableStateOf("") }
    var supOpening by remember { mutableStateOf("0") }

    // Ledger tab state
    var ledgerSupplier by remember(uiState.suppliers) { mutableStateOf<SupplierItem?>(uiState.suppliers.firstOrNull()) }

    // Pay Supplier tab state
    var paySupplier by remember(uiState.suppliers) { mutableStateOf<SupplierItem?>(uiState.suppliers.firstOrNull()) }
    var payDate by remember { mutableStateOf(Formatters.todayDhakaIso()) }
    var payAmount by remember { mutableStateOf("") }
    var payType by remember { mutableStateOf("Cash") }
    var payNote by remember { mutableStateOf("") }

    // Report tab state
    var reportSupplier by remember(uiState.suppliers) { mutableStateOf<SupplierItem?>(null) }
    var reportStart by remember { mutableStateOf("") }
    var reportEnd by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Suppliers", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("${uiState.suppliers.size} vendors synced", color = TextSecondary, fontSize = 12.sp)
            }
            if (selectedTab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkOutlineButton(
                        text = "Full PDF",
                        icon = Icons.Default.PictureAsPdf,
                        tint = NeonGreen,
                        onClick = {
                            runCatching {
                                val file = PdfGenerator.generateAllSuppliersSummaryPdf(context, uiState.suppliers)
                                PdfGenerator.openPdfOrShare(context, file)
                            }.onFailure { onNotify("PDF error: ${it.message}", true) }
                        },
                        testTag = "suppliers_full_pdf_button"
                    )
                    NeonButton(
                        text = "Add",
                        icon = Icons.Default.Add,
                        onClick = {
                            editingSupplier = null
                            supName = ""
                            supMobile = ""
                            supAddress = ""
                            supOpening = "0"
                            showFormSheet = true
                        },
                        testTag = "suppliers_add_button"
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = NeonGreen,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonGreen
                )
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = {
                        selectedTab = idx
                        if (idx == 1) {
                            ledgerSupplier?.let { onLoadLedger(it.id) }
                        }
                    },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == idx) NeonGreen else TextSecondary,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (uiState.suppliersLoading && uiState.suppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.suppliers, key = { it.id }) { sup ->
                            JhfCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sup.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (!sup.mobile.isNullOrBlank()) {
                                            Text(sup.mobile, color = TextSecondary, fontSize = 12.sp)
                                        }
                                        if (!sup.address.isNullOrBlank()) {
                                            Text(sup.address, color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingSupplier = sup
                                            supName = sup.name
                                            supMobile = sup.mobile ?: ""
                                            supAddress = sup.address ?: ""
                                            supOpening = sup.openingBalance.toInt().toString()
                                            showFormSheet = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit supplier", tint = NeonGreen)
                                        }
                                        IconButton(onClick = { deleteCandidate = sup }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete supplier", tint = DangerRed)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Purchase: ${Formatters.formatTaka(sup.totalPurchase)}", color = TextSecondary, fontSize = 12.sp)
                                    Text("Paid: ${Formatters.formatTaka(sup.totalPaid)}", color = NeonGreen, fontSize = 12.sp)
                                    Text(
                                        "Due: ${Formatters.formatTaka(sup.due)}",
                                        color = if (sup.due > 0) DangerRed else NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        JhfDropdown(
                            label = "Select Supplier for Ledger",
                            items = uiState.suppliers,
                            selectedText = ledgerSupplier?.name ?: "Choose supplier",
                            itemLabel = { it.name },
                            onSelect = { sup ->
                                ledgerSupplier = sup
                                onLoadLedger(sup.id)
                            },
                            testTag = "supplier_ledger_dropdown"
                        )
                    }
                    val ledger = uiState.selectedSupplierLedger
                    if (ledger != null) {
                        ledger.supplier?.let { s ->
                            item {
                                JhfCard(accentBorder = NeonGreen.copy(alpha = 0.3f)) {
                                    Text(s.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Purchase: ${Formatters.formatTaka(s.totalPurchase)}", color = TextSecondary, fontSize = 12.sp)
                                        Text("Paid: ${Formatters.formatTaka(s.totalPaid)}", color = NeonGreen, fontSize = 12.sp)
                                        Text("Due: ${Formatters.formatTaka(s.due)}", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        item { Text("Purchase History (${ledger.purchases.size})", color = Color.White, fontWeight = FontWeight.Bold) }
                        items(ledger.purchases, key = { "pur_${it.id}" }) { p ->
                            JhfCard {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.model ?: "Laptop", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        Text("SN: ${p.serialNumber ?: "N/A"} • ${p.purchaseDate}", color = TextMuted, fontSize = 12.sp)
                                    }
                                    Text(Formatters.formatTaka(p.purchaseAmount), color = NeonGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item { Text("Payment History (${ledger.payments.size})", color = Color.White, fontWeight = FontWeight.Bold) }
                        items(ledger.payments, key = { "pay_${it.id}" }) { pay ->
                            JhfCard {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${pay.paymentType} • ${pay.paymentDate}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                        if (!pay.note.isNullOrBlank()) Text(pay.note, color = TextMuted, fontSize = 12.sp)
                                    }
                                    Text(Formatters.formatTaka(pay.paymentAmount), color = NeonGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        JhfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Record Supplier Payment", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                                JhfDropdown(
                                    label = "Supplier *",
                                    items = uiState.suppliers,
                                    selectedText = paySupplier?.let { "${it.name} (Due: ${Formatters.formatTaka(it.due)})" } ?: "Select supplier",
                                    itemLabel = { "${it.name} (Due: ${Formatters.formatTaka(it.due)})" },
                                    onSelect = { paySupplier = it },
                                    testTag = "supplier_pay_dropdown"
                                )
                                JhfTextField(
                                    value = payDate,
                                    onValueChange = { payDate = it },
                                    label = "Payment Date (YYYY-MM-DD)"
                                )
                                JhfTextField(
                                    value = payAmount,
                                    onValueChange = { payAmount = it },
                                    label = "Amount (TK) *",
                                    keyboardType = KeyboardType.Number,
                                    testTag = "supplier_pay_amount_input"
                                )
                                JhfDropdown(
                                    label = "Payment Method",
                                    items = listOf("Cash", "Bank Account", "Bkash", "Nagad", "Rocket", "POS"),
                                    selectedText = payType,
                                    itemLabel = { it },
                                    onSelect = { payType = it }
                                )
                                JhfTextField(
                                    value = payNote,
                                    onValueChange = { payNote = it },
                                    label = "Note (Optional)"
                                )
                                NeonButton(
                                    text = if (uiState.globalBusy) "Recording..." else "Record Payment & Sync to Web",
                                    icon = Icons.Default.Payments,
                                    enabled = !uiState.globalBusy,
                                    onClick = {
                                        val s = paySupplier
                                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                                        if (s == null || amt <= 0.0) {
                                            onNotify("Select supplier and valid amount", true)
                                            return@NeonButton
                                        }
                                        onRecordPayment(s.id, payDate, amt, payType, payNote) {
                                            payAmount = ""
                                            payNote = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "supplier_pay_submit_button"
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        JhfCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Supplier Statement Filter", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                JhfDropdown(
                                    label = "Supplier (All or Specific)",
                                    items = listOf<SupplierItem?>(null) + uiState.suppliers,
                                    selectedText = reportSupplier?.name ?: "All Suppliers",
                                    itemLabel = { it?.name ?: "All Suppliers" },
                                    onSelect = { reportSupplier = it }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    JhfTextField(
                                        value = reportStart,
                                        onValueChange = { reportStart = it },
                                        label = "Start Date",
                                        modifier = Modifier.weight(1f)
                                    )
                                    JhfTextField(
                                        value = reportEnd,
                                        onValueChange = { reportEnd = it },
                                        label = "End Date",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                NeonButton(
                                    text = "Fetch Supplier Statement",
                                    onClick = { onFetchReport(reportSupplier?.id, reportStart, reportEnd) },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "supplier_fetch_report_button"
                                )
                            }
                        }
                    }

                    items(uiState.supplierReportEntries, key = { it.id }) { entry ->
                        val due = (entry.totalPurchase + entry.openingBalance) - entry.totalPaid
                        JhfCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "Purchases: ${entry.purchases.size} • Payments: ${entry.payments.size}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Due: ${Formatters.formatTaka(due)}",
                                        color = if (due > 0) DangerRed else NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                DarkOutlineButton(
                                    text = "PDF",
                                    icon = Icons.Default.PictureAsPdf,
                                    tint = NeonGreen,
                                    onClick = {
                                        runCatching {
                                            val file = PdfGenerator.generateSupplierEntryReportPdf(context, entry, reportStart, reportEnd)
                                            PdfGenerator.openPdfOrShare(context, file)
                                        }.onFailure { onNotify("PDF error: ${it.message}", true) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormSheet) {
        ModalBottomSheet(onDismissRequest = { showFormSheet = false }, containerColor = DarkCard) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (editingSupplier == null) "Add Supplier" else "Edit Supplier",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )
                JhfTextField(value = supName, onValueChange = { supName = it }, label = "Supplier Name *", testTag = "supplier_name_input")
                JhfTextField(value = supMobile, onValueChange = { supMobile = it }, label = "Mobile", keyboardType = KeyboardType.Phone)
                JhfTextField(value = supAddress, onValueChange = { supAddress = it }, label = "Address")
                JhfTextField(value = supOpening, onValueChange = { supOpening = it }, label = "Opening Balance (TK)", keyboardType = KeyboardType.Number)
                HorizontalDivider(color = DarkBorder)
                NeonButton(
                    text = if (uiState.globalBusy) "Saving..." else "Save Supplier",
                    enabled = !uiState.globalBusy,
                    onClick = {
                        if (supName.isBlank()) {
                            onNotify("Supplier name is required", true)
                            return@NeonButton
                        }
                        onSaveSupplier(
                            editingSupplier?.id,
                            supName,
                            supMobile,
                            supAddress,
                            supOpening.toDoubleOrNull() ?: 0.0
                        ) { showFormSheet = false }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "supplier_save_button"
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    deleteCandidate?.let { sup ->
        ConfirmActionDialog(
            title = "Delete Supplier",
            message = "Delete ${sup.name} from live database? Related payments will also be removed.",
            confirmLabel = "Delete",
            onConfirm = { onDeleteSupplier(sup.id) },
            onDismiss = { deleteCandidate = null }
        )
    }
}
