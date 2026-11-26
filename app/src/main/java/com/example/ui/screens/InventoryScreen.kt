package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.catalog.LaptopCatalog
import com.example.data.model.InventoryItem
import com.example.data.model.SupplierItem
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.DarkOutlineButton
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfDropdown
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    uiState: ErpUiState,
    onAddItem: (String, String, String, Double, Double, String, Int?, String, () -> Unit) -> Unit,
    onUpdateItem: (Int, String, String, String, Double, Double, String, String, String, () -> Unit) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = uiState.currentUser?.isAdmin == true

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var deleteCandidate by remember { mutableStateOf<InventoryItem?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    // Form state
    var model by remember { mutableStateOf("") }
    var config by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var supplierId by remember { mutableStateOf<Int?>(null) }
    var supplierName by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(Formatters.todayDhakaIso()) }
    var status by remember { mutableStateOf("Available") }

    fun openAddModal() {
        editingItem = null
        model = ""
        config = ""
        serial = ""
        buyPrice = ""
        sellPrice = ""
        supplierId = null
        supplierName = ""
        purchaseDate = Formatters.todayDhakaIso()
        status = "Available"
        showSheet = true
    }

    fun openEditModal(item: InventoryItem) {
        editingItem = item
        model = item.model
        config = item.config
        serial = item.serialNumber
        buyPrice = item.buyPrice.toInt().toString()
        sellPrice = item.sellPrice.toInt().toString()
        supplierId = item.supplierId.takeIf { it > 0 }
        supplierName = item.supplierName ?: ""
        purchaseDate = item.purchaseDate ?: Formatters.todayDhakaIso()
        status = item.status
        showSheet = true
    }

    val filtered = remember(uiState.inventory, searchQuery, statusFilter) {
        uiState.inventory.filter { item ->
            val matchesStatus = when (statusFilter) {
                "Available" -> item.status.equals("Available", ignoreCase = true)
                "Sold" -> item.status.equals("Sold", ignoreCase = true)
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                item.model.lowercase().contains(q) ||
                item.serialNumber.lowercase().contains(q) ||
                item.config.lowercase().contains(q) ||
                (item.supplierName ?: "").lowercase().contains(q)
            matchesStatus && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header & Actions
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
                        Text("Inventory", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text(
                            "${uiState.inventory.count { it.status == "Available" }} available / ${uiState.inventory.size} total",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    DarkOutlineButton(
                        text = "Stock PDF",
                        icon = Icons.Default.PictureAsPdf,
                        tint = NeonGreen,
                        onClick = {
                            runCatching {
                                val file = PdfGenerator.generateInventoryReportPdf(context, uiState.inventory, isAdmin)
                                PdfGenerator.openPdfOrShare(context, file)
                                onNotify("Stock Report PDF generated", false)
                            }.onFailure {
                                onNotify("PDF error: ${it.message}", true)
                            }
                        },
                        testTag = "inventory_export_pdf_button"
                    )
                }

                JhfTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "Search model, serial, config, supplier",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
                    testTag = "inventory_search_input"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Available", "Sold").forEach { f ->
                        FilterChip(
                            selected = statusFilter == f,
                            onClick = { statusFilter = f },
                            label = { Text(f) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            if (uiState.inventoryLoading && uiState.inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No laptops match your filter", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        InventoryRowCard(
                            item = item,
                            isAdmin = isAdmin,
                            onEdit = { openEditModal(item) },
                            onDelete = { deleteCandidate = item }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { openAddModal() },
            containerColor = NeonGreen,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_inventory_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Laptop")
        }
    }

    if (showSheet) {
        val suggestions = remember(model) { LaptopCatalog.searchModels(model) }
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (editingItem == null) "Add Laptop Stock" else "Edit Laptop Stock",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonGreen
                )

                JhfTextField(
                    value = model,
                    onValueChange = { input ->
                        model = input
                        val exact = LaptopCatalog.items.firstOrNull { it.model.equals(input.trim(), ignoreCase = true) }
                        if (exact != null) config = exact.config
                    },
                    label = "Laptop Model *",
                    placeholder = "e.g. HP EliteBook 840 G6",
                    testTag = "inventory_model_input"
                )

                if (suggestions.isNotEmpty() && !LaptopCatalog.items.any { it.model.equals(model, ignoreCase = true) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkElevated)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Suggested Models:", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp))
                        suggestions.take(5).forEach { s ->
                            Surface(
                                color = DarkBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        model = s
                                        config = LaptopCatalog.getConfig(s)
                                    }
                            ) {
                                Text(
                                    text = s,
                                    color = NeonGreen,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                JhfTextField(
                    value = config,
                    onValueChange = { config = it },
                    label = "Configuration / Specs *",
                    placeholder = "Core i5 8th Gen, 8GB RAM, 256GB SSD",
                    testTag = "inventory_config_input"
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JhfTextField(
                        value = serial,
                        onValueChange = { serial = it },
                        label = "Serial Number *",
                        modifier = Modifier.weight(1f),
                        testTag = "inventory_serial_input"
                    )
                    IconButton(
                        onClick = { showScanner = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonGreen)
                            .testTag("inventory_scan_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Serial", tint = Color.Black)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    JhfTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = "Buy Price (TK) *",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "inventory_buy_price_input"
                    )
                    JhfTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = "Sell Price (TK) *",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                        testTag = "inventory_sell_price_input"
                    )
                }

                JhfDropdown<SupplierItem>(
                    label = "Supplier (Optional)",
                    items = uiState.suppliers,
                    selectedText = supplierName.ifBlank { "No supplier selected" },
                    itemLabel = { "${it.name}${if (!it.mobile.isNullOrBlank()) " (${it.mobile})" else ""}" },
                    onSelect = { sup ->
                        supplierId = sup.id
                        supplierName = sup.name
                    },
                    testTag = "inventory_supplier_dropdown"
                )

                JhfTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = "Purchase Date (YYYY-MM-DD)",
                    testTag = "inventory_date_input"
                )

                if (editingItem != null) {
                    JhfDropdown(
                        label = "Status",
                        items = listOf("Available", "Sold"),
                        selectedText = status,
                        itemLabel = { it },
                        onSelect = { status = it }
                    )
                }

                NeonButton(
                    text = if (uiState.globalBusy) "Saving to Web..." else if (editingItem == null) "Save & Sync to Web" else "Update & Sync to Web",
                    enabled = !uiState.globalBusy,
                    onClick = {
                        val buy = buyPrice.toDoubleOrNull() ?: 0.0
                        val sell = sellPrice.toDoubleOrNull() ?: 0.0
                        if (model.isBlank() || config.isBlank() || serial.isBlank() || sell <= 0.0) {
                            onNotify("Model, Config, Serial Number and Sell Price are required", true)
                            return@NeonButton
                        }
                        val edit = editingItem
                        if (edit == null) {
                            onAddItem(model, config, serial, buy, sell, supplierName, supplierId, purchaseDate) {
                                showSheet = false
                            }
                        } else {
                            onUpdateItem(edit.id, model, config, serial, buy, sell, supplierName, purchaseDate, status) {
                                showSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "inventory_save_button"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showScanner) {
        BarcodeScannerDialog(
            onScan = { scanned ->
                serial = scanned
                showScanner = false
                onNotify("Scanned serial: $scanned", false)
            },
            onDismiss = { showScanner = false }
        )
    }

    deleteCandidate?.let { item ->
        ConfirmActionDialog(
            title = "Delete Inventory Item",
            message = "Delete ${item.model} (${item.serialNumber}) from mobile and live web database?",
            confirmLabel = "Delete",
            onConfirm = { onDeleteItem(item.id) },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun InventoryRowCard(
    item: InventoryItem,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAvail = item.status.equals("Available", ignoreCase = true)
    JhfCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.model, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.config, color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SN: ${item.serialNumber}",
                    color = NeonGreen,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp
                )
            }
            StatusBadge(
                label = item.status,
                color = if (isAvail) NeonGreen else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (isAdmin) {
                    Text(
                        "Buy: ${Formatters.formatTaka(item.buyPrice)} | Est. Profit: ${Formatters.formatTaka(item.sellPrice - item.buyPrice)}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                Text(
                    "Sell: ${Formatters.formatTaka(item.sellPrice)}",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (!item.supplierName.isNullOrBlank()) {
                    Text("Supplier: ${item.supplierName}", color = TextSecondary, fontSize = 11.sp)
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_inventory_${item.id}")) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit laptop", tint = NeonGreen)
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_inventory_${item.id}")) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete laptop", tint = DangerRed)
                }
            }
        }
    }
}
