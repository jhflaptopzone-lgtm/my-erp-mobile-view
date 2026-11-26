package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryItem
import com.example.data.model.SaleRecord
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState
import com.example.ui.viewmodel.InvoiceReadyState
import com.example.util.Formatters
import com.example.util.PdfGenerator
import com.example.util.QrCodeUtil

@Composable
fun SalesPosScreen(
    uiState: ErpUiState,
    onCreateSale: (
        item: InventoryItem,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        paymentType: String,
        saleDate: String,
        emiDownPayment: Double,
        emiDurationMonths: Int,
        dueDownPayment: Double,
        duePlanMonths: Int,
        dueEstimateDate: String,
        dueReferenceName: String,
        dueReferencePhone: String
    ) -> Unit,
    onDismissInvoice: () -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val availableItems = remember(uiState.inventory) {
        uiState.inventory.filter { it.status.equals("Available", ignoreCase = true) }
    }

    var selectedItem by remember(availableItems) { mutableStateOf<InventoryItem?>(null) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("Cash") }
    var saleDate by remember { mutableStateOf(Formatters.todayDhakaIso()) }

    var emiDown by remember { mutableStateOf("0") }
    var emiDuration by remember { mutableStateOf(3) }

    var dueDown by remember { mutableStateOf("0") }
    var duePlan by remember { mutableStateOf(2) }
    var dueEstimateDate by remember { mutableStateOf(Formatters.todayDhakaIso()) }
    var dueRefName by remember { mutableStateOf("") }
    var dueRefPhone by remember { mutableStateOf("") }

    var showScanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sales POS", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Create invoices & sync instantly to web", color = TextSecondary, fontSize = 12.sp)
                }
                NeonButton(
                    text = "Scan SN",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { showScanner = true },
                    testTag = "pos_scan_button"
                )
            }
        }

        item {
            JhfCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Sale Invoice", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(14.dp))

                JhfDropdown(
                    label = "Select Laptop (Available Stock)",
                    items = availableItems,
                    selectedText = selectedItem?.let { "${it.model} - ${it.serialNumber} (${Formatters.formatTaka(it.sellPrice)})" } ?: "",
                    itemLabel = { "${it.model} - ${it.serialNumber} (${Formatters.formatTaka(it.sellPrice)})" },
                    onSelect = { selectedItem = it },
                    testTag = "pos_laptop_dropdown"
                )

                Spacer(modifier = Modifier.height(12.dp))

                JhfTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = "Customer Name *",
                    testTag = "pos_customer_name_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                JhfTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = "Phone Number *",
                    keyboardType = KeyboardType.Phone,
                    testTag = "pos_customer_phone_input"
                )
                Spacer(modifier = Modifier.height(10.dp))
                JhfTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = "Address",
                    singleLine = false,
                    testTag = "pos_customer_address_input"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JhfDropdown(
                        label = "Payment Type",
                        items = listOf("Cash", "Bank", "Due", "EMI"),
                        selectedText = paymentType,
                        itemLabel = { if (it == "Bank") "Bank Transfer" else it },
                        onSelect = { paymentType = it },
                        modifier = Modifier.weight(1f),
                        testTag = "pos_payment_type_dropdown"
                    )
                    JhfTextField(
                        value = saleDate,
                        onValueChange = { saleDate = it },
                        label = "Sale Date (YYYY-MM-DD)",
                        modifier = Modifier.weight(1f),
                        testTag = "pos_sale_date_input"
                    )
                }

                if (paymentType == "EMI") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("EMI Installment Plan", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            JhfTextField(
                                value = emiDown,
                                onValueChange = { emiDown = it },
                                label = "1st / Down Payment",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            JhfDropdown(
                                label = "Duration",
                                items = listOf(2, 3, 4, 6, 9, 12),
                                selectedText = "$emiDuration Months",
                                itemLabel = { "$it Months (${it - 1} Future)" },
                                onSelect = { emiDuration = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (paymentType == "Due") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Due Plan & Reference", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            JhfTextField(
                                value = dueDown,
                                onValueChange = { dueDown = it },
                                label = "1st / Down Payment",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f)
                            )
                            JhfDropdown(
                                label = "Installment Plan",
                                items = listOf(2, 3, 4, 6, 9, 12),
                                selectedText = "$duePlan Months",
                                itemLabel = { "$it Months (${it - 1} Future)" },
                                onSelect = { duePlan = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        JhfTextField(
                            value = dueEstimateDate,
                            onValueChange = { dueEstimateDate = it },
                            label = "Estimate Pay Date (YYYY-MM-DD)"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            JhfTextField(
                                value = dueRefName,
                                onValueChange = { dueRefName = it },
                                label = "Reference Name *",
                                modifier = Modifier.weight(1f)
                            )
                            JhfTextField(
                                value = dueRefPhone,
                                onValueChange = { dueRefPhone = it },
                                label = "Reference Phone *",
                                keyboardType = KeyboardType.Phone,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            JhfCard(accentBorder = NeonGreen.copy(alpha = 0.35f)) {
                Text("Order Summary", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))
                val item = selectedItem
                if (item == null) {
                    Text("No laptop selected yet", color = TextMuted, fontSize = 13.sp)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Model", color = TextSecondary)
                        Text(item.model, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Serial", color = TextSecondary)
                        Text(item.serialNumber, color = NeonGreen, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(Formatters.formatTaka(item.sellPrice), color = NeonGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                NeonButton(
                    text = if (uiState.globalBusy) "Processing Sale..." else "Complete Sale & Generate Invoice",
                    enabled = !uiState.globalBusy,
                    onClick = {
                        val chosen = selectedItem
                        if (chosen == null) {
                            onNotify("Please select an available laptop", true)
                            return@NeonButton
                        }
                        if (customerName.isBlank() || customerPhone.isBlank()) {
                            onNotify("Customer Name and Phone are required", true)
                            return@NeonButton
                        }
                        if (paymentType == "Due" && (dueRefName.isBlank() || dueRefPhone.isBlank())) {
                            onNotify("Reference Name and Phone are required for Due sales", true)
                            return@NeonButton
                        }
                        onCreateSale(
                            chosen,
                            customerName,
                            customerPhone,
                            customerAddress,
                            paymentType,
                            saleDate,
                            emiDown.toDoubleOrNull() ?: 0.0,
                            emiDuration,
                            dueDown.toDoubleOrNull() ?: 0.0,
                            duePlan,
                            dueEstimateDate,
                            dueRefName,
                            dueRefPhone
                        )
                        selectedItem = null
                        customerName = ""
                        customerPhone = ""
                        customerAddress = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "pos_complete_sale_button"
                )
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onScan = { scanned ->
                val match = availableItems.firstOrNull {
                    it.serialNumber.equals(scanned.trim(), ignoreCase = true)
                }
                showScanner = false
                if (match != null) {
                    selectedItem = match
                    onNotify("Selected: ${match.model}", false)
                } else {
                    onNotify("No available stock found for serial: $scanned", true)
                }
            },
            onDismiss = { showScanner = false }
        )
    }

    uiState.lastInvoiceReady?.let { ready ->
        InvoiceReadyDialog(
            ready = ready,
            serverUrl = uiState.serverUrl,
            onDismiss = onDismissInvoice,
            onCopyLink = { link ->
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("Invoice Link", link))
                onNotify("Public Share Link copied!", false)
            },
            onOpenPdf = {
                val saleRecord = SaleRecord(
                    invoiceNumber = ready.response.invoiceNumber,
                    inventoryId = ready.item.id,
                    customerName = ready.customerName,
                    customerPhone = ready.customerPhone,
                    customerAddress = ready.customerAddress,
                    sellPrice = ready.item.sellPrice,
                    paymentType = ready.paymentType,
                    saleDate = ready.saleDate,
                    staffName = ready.staffName,
                    model = ready.item.model,
                    config = ready.item.config,
                    serialNumber = ready.item.serialNumber
                )
                val file = PdfGenerator.generateInvoicePdf(context, saleRecord, ready.response.installments)
                PdfGenerator.openPdfOrShare(context, file)
            }
        )
    }
}

@Composable
private fun InvoiceReadyDialog(
    ready: InvoiceReadyState,
    serverUrl: String,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onOpenPdf: () -> Unit
) {
    val shareLink = "${serverUrl.trimEnd('/')}/invoice/view/${ready.response.invoiceNumber}?token=${ready.response.publicToken}"
    val qrBitmap = remember(shareLink) { QrCodeUtil.generateQrBitmap(shareLink, 420) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeonGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("INVOICE READY", color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Sale completed & synced to live web", color = TextSecondary, fontSize = 12.sp)

                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ready.response.invoiceNumber, color = Color.White, fontFamily = JetBrainsMonoFamily, fontSize = 13.sp)
                        Text(ready.saleDate, color = NeonGreen, fontSize = 12.sp)
                    }
                }

                if (qrBitmap != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Invoice Share QR Code",
                            modifier = Modifier.size(160.dp)
                        )
                    }
                }

                Surface(
                    color = DarkBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = shareLink,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onCopyLink(shareLink) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Share Link", tint = NeonGreen)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "View PDF",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = onOpenPdf,
                        modifier = Modifier.weight(1f),
                        testTag = "invoice_ready_pdf_button"
                    )
                    DarkOutlineButton(
                        text = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
