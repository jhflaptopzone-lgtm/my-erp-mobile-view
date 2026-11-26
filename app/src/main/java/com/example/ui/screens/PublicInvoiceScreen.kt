package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.example.ui.components.JhfCard
import com.example.ui.components.JhfTextField
import com.example.ui.components.NeonButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.viewmodel.ErpUiState
import com.example.util.Formatters
import com.example.util.PdfGenerator

@Composable
fun PublicInvoiceScreen(
    uiState: ErpUiState,
    onLookup: (invoiceNumber: String, token: String?) -> Unit,
    onBack: () -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var invoiceNumber by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("public_invoice_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                }
                Column {
                    Text("Public Invoice Viewer", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Verify customer invoice directly from live server", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        item {
            JhfCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    JhfTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = "Invoice Number *",
                        placeholder = "JHF-20260401-0001",
                        testTag = "public_invoice_number_input"
                    )
                    JhfTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = "Access Token (Optional)",
                        testTag = "public_invoice_token_input"
                    )
                    NeonButton(
                        text = if (uiState.publicInvoiceLoading) "Searching..." else "Lookup Invoice",
                        icon = Icons.Default.Search,
                        enabled = !uiState.publicInvoiceLoading,
                        onClick = { onLookup(invoiceNumber, accessToken.takeIf { it.isNotBlank() }) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "public_invoice_lookup_submit"
                    )
                }
            }
        }

        if (uiState.publicInvoiceLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }
        }

        uiState.publicInvoiceError?.let { err ->
            item {
                JhfCard(accentBorder = DangerRed) {
                    Text(err, color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        uiState.publicInvoice?.let { res ->
            val sale = res.sale
            item {
                JhfCard(accentBorder = NeonGreen.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sale.invoiceNumber, color = NeonGreen, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Date: ${Formatters.formatDateBD(sale.saleDate)}", color = TextSecondary, fontSize = 12.sp)
                        }
                        StatusBadge(label = sale.paymentType, color = NeonGreen)
                    }
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                    Text("Customer: ${sale.customerName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Phone: ${sale.customerPhone}", color = TextSecondary, fontSize = 13.sp)
                    if (!sale.customerAddress.isNullOrBlank()) {
                        Text("Address: ${sale.customerAddress}", color = TextMuted, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 10.dp))
                    Text(sale.model ?: "Laptop", color = Color.White, fontWeight = FontWeight.Bold)
                    if (!sale.config.isNullOrBlank()) Text(sale.config, color = TextSecondary, fontSize = 12.sp)
                    Text("Serial No: ${sale.serialNumber ?: "N/A"}", color = NeonGreen, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(Formatters.formatTaka(sale.sellPrice), color = NeonGreen, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                    Text("In Words: ${Formatters.numberToWords(sale.sellPrice)} Taka Only", color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Warranty: 1 Month Parts & 5 Years Service", color = TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    NeonButton(
                        text = "View / Share PDF Invoice",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = {
                            runCatching {
                                val file = PdfGenerator.generateInvoicePdf(context, sale, res.installments)
                                PdfGenerator.openPdfOrShare(context, file)
                            }.onFailure { onNotify("PDF error: ${it.message}", true) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "public_invoice_pdf_button"
                    )
                }
            }

            if (res.installments.isNotEmpty()) {
                item {
                    Text("Installment Schedule", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                items(res.installments, key = { it.id }) { inst ->
                    JhfCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Installment #${inst.installmentNumber}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Due: ${Formatters.formatDateBD(inst.dueDate)}", color = TextMuted, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Formatters.formatTaka(inst.amount), color = NeonGreen, fontWeight = FontWeight.Bold)
                                Text(
                                    inst.status,
                                    color = if (inst.status.equals("Paid", true)) NeonGreen else WarningYellow,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
