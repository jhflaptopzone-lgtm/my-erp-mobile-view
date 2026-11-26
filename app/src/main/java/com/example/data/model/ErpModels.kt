package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Accepts numeric JSON values OR string-encoded numbers (Postgres DECIMAL/NUMERIC rows) OR null -> 0.0.
 */
object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    override fun deserialize(decoder: Decoder): Double {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonNull) return 0.0
            if (element is JsonPrimitive) {
                return element.doubleOrNull ?: element.content.toDoubleOrNull() ?: 0.0
            }
        }
        return runCatching { decoder.decodeDouble() }.getOrDefault(0.0)
    }
}

/**
 * Accepts numeric JSON values OR string-encoded ints OR null -> 0.
 */
object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonNull) return 0
            if (element is JsonPrimitive) {
                return element.intOrNull ?: element.content.toDoubleOrNull()?.toInt() ?: 0
            }
        }
        return runCatching { decoder.decodeInt() }.getOrDefault(0)
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Serializable
data class UserProfile(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val name: String = "",
    @SerialName("access_id") val accessId: String = "",
    val role: String = "Staff"
) {
    val isAdmin: Boolean get() = role.equals("Admin", ignoreCase = true)
    val isManagerOrAdmin: Boolean get() = isAdmin || role.equals("Manager", ignoreCase = true)
}

@Serializable
data class LoginResponse(
    val token: String = "",
    val user: UserProfile = UserProfile()
)

@Serializable
data class InventoryItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val model: String = "",
    val config: String = "",
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("buy_price") @Serializable(with = FlexibleDoubleSerializer::class) val buyPrice: Double = 0.0,
    @SerialName("sell_price") @Serializable(with = FlexibleDoubleSerializer::class) val sellPrice: Double = 0.0,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("supplier_id") @Serializable(with = FlexibleIntSerializer::class) val supplierId: Int = 0,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    val status: String = "Available",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SaleRecord(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("inventory_id") @Serializable(with = FlexibleIntSerializer::class) val inventoryId: Int = 0,
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_address") val customerAddress: String? = null,
    @SerialName("sell_price") @Serializable(with = FlexibleDoubleSerializer::class) val sellPrice: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val profit: Double = 0.0,
    @SerialName("buy_price") @Serializable(with = FlexibleDoubleSerializer::class) val buyPrice: Double = 0.0,
    @SerialName("payment_type") val paymentType: String = "Cash",
    @SerialName("due_estimate_date") val dueEstimateDate: String? = null,
    @SerialName("due_reference_name") val dueReferenceName: String? = null,
    @SerialName("due_reference_phone") val dueReferencePhone: String? = null,
    @SerialName("due_terms") val dueTerms: String? = null,
    @SerialName("staff_id") @Serializable(with = FlexibleIntSerializer::class) val staffId: Int = 0,
    @SerialName("staff_name") val staffName: String? = null,
    @SerialName("sale_date") val saleDate: String = "",
    @SerialName("invoice_pdf_path") val invoicePdfPath: String? = null,
    @SerialName("invoice_public_token") val invoicePublicToken: String? = null,
    val model: String? = null,
    val config: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null
)

@Serializable
data class InstallmentRecord(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("sale_id") @Serializable(with = FlexibleIntSerializer::class) val saleId: Int = 0,
    @SerialName("installment_number") @Serializable(with = FlexibleIntSerializer::class) val installmentNumber: Int = 1,
    @SerialName("due_date") val dueDate: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val amount: Double = 0.0,
    @SerialName("paid_amount") @Serializable(with = FlexibleDoubleSerializer::class) val paidAmount: Double = 0.0,
    @SerialName("payment_date") val paymentDate: String? = null,
    val status: String = "Pending",
    @SerialName("remaining_balance") @Serializable(with = FlexibleDoubleSerializer::class) val remainingBalance: Double = 0.0
)

@Serializable
data class SalePaymentRecord(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("sale_id") @Serializable(with = FlexibleIntSerializer::class) val saleId: Int = 0,
    @SerialName("installment_id") @Serializable(with = FlexibleIntSerializer::class) val installmentId: Int = 0,
    @Serializable(with = FlexibleDoubleSerializer::class) val amount: Double = 0.0,
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("payment_type") val paymentType: String? = "Cash",
    val note: String? = null
)

@Serializable
data class InstallmentsResponse(
    val installments: List<InstallmentRecord> = emptyList(),
    val payments: List<SalePaymentRecord> = emptyList()
)

@Serializable
data class EmiPlanPayload(
    @SerialName("down_payment") val downPayment: Double,
    @SerialName("duration_months") val durationMonths: Int
)

@Serializable
data class DuePlanPayload(
    @SerialName("down_payment") val downPayment: Double,
    @SerialName("installment_plan") val installmentPlan: Int,
    @SerialName("estimate_date") val estimateDate: String,
    @SerialName("reference_name") val referenceName: String,
    @SerialName("reference_phone") val referencePhone: String
)

@Serializable
data class CreateSaleRequest(
    @SerialName("inventory_id") val inventoryId: Int,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_phone") val customerPhone: String,
    @SerialName("customer_address") val customerAddress: String,
    @SerialName("payment_type") val paymentType: String,
    @SerialName("sale_date") val saleDate: String,
    @SerialName("emi_data") val emiData: EmiPlanPayload? = null,
    @SerialName("due_data") val dueData: DuePlanPayload? = null
)

@Serializable
data class CreateSaleResponse(
    val message: String = "",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("public_token") val publicToken: String = "",
    val installments: List<InstallmentRecord> = emptyList()
)

@Serializable
data class DueReportItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_address") val customerAddress: String? = null,
    @SerialName("sell_price") @Serializable(with = FlexibleDoubleSerializer::class) val sellPrice: Double = 0.0,
    @SerialName("buy_price") @Serializable(with = FlexibleDoubleSerializer::class) val buyPrice: Double = 0.0,
    @SerialName("payment_type") val paymentType: String = "",
    @SerialName("sale_date") val saleDate: String = "",
    val model: String = "",
    val config: String = "",
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("staff_name") val staffName: String? = null,
    @SerialName("total_sell_price") @Serializable(with = FlexibleDoubleSerializer::class) val totalSellPrice: Double = 0.0,
    @SerialName("total_paid") @Serializable(with = FlexibleDoubleSerializer::class) val totalPaid: Double = 0.0,
    @SerialName("final_due_date") val finalDueDate: String? = null
)

@Serializable
data class SupplierItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val name: String = "",
    val mobile: String? = null,
    val address: String? = null,
    @SerialName("opening_balance") @Serializable(with = FlexibleDoubleSerializer::class) val openingBalance: Double = 0.0,
    @SerialName("total_purchase") @Serializable(with = FlexibleDoubleSerializer::class) val totalPurchase: Double = 0.0,
    @SerialName("total_paid") @Serializable(with = FlexibleDoubleSerializer::class) val totalPaid: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null
) {
    val due: Double get() = (totalPurchase + openingBalance) - totalPaid
}

@Serializable
data class SupplierPurchaseItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("supplier_id") @Serializable(with = FlexibleIntSerializer::class) val supplierId: Int = 0,
    @SerialName("inventory_id") @Serializable(with = FlexibleIntSerializer::class) val inventoryId: Int = 0,
    @SerialName("purchase_amount") @Serializable(with = FlexibleDoubleSerializer::class) val purchaseAmount: Double = 0.0,
    @SerialName("purchase_date") val purchaseDate: String = "",
    val model: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    val config: String? = null
)

@Serializable
data class SupplierPaymentItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("supplier_id") @Serializable(with = FlexibleIntSerializer::class) val supplierId: Int = 0,
    @SerialName("payment_date") val paymentDate: String = "",
    @SerialName("payment_amount") @Serializable(with = FlexibleDoubleSerializer::class) val paymentAmount: Double = 0.0,
    @SerialName("payment_type") val paymentType: String = "Cash",
    val note: String? = null
)

@Serializable
data class SupplierLedgerResponse(
    val purchases: List<SupplierPurchaseItem> = emptyList(),
    val payments: List<SupplierPaymentItem> = emptyList(),
    val supplier: SupplierItem? = null
)

@Serializable
data class SupplierReportEntry(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val name: String = "",
    val mobile: String? = null,
    val address: String? = null,
    @SerialName("opening_balance") @Serializable(with = FlexibleDoubleSerializer::class) val openingBalance: Double = 0.0,
    @SerialName("total_purchase") @Serializable(with = FlexibleDoubleSerializer::class) val totalPurchase: Double = 0.0,
    @SerialName("total_paid") @Serializable(with = FlexibleDoubleSerializer::class) val totalPaid: Double = 0.0,
    val purchases: List<SupplierPurchaseItem> = emptyList(),
    val payments: List<SupplierPaymentItem> = emptyList()
)

@Serializable
data class MonthlySalePoint(
    val month: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val total: Double = 0.0
)

@Serializable
data class PaymentMethodCount(
    @SerialName("payment_type") val paymentType: String = "",
    @Serializable(with = FlexibleIntSerializer::class) val count: Int = 0
)

@Serializable
data class TopSupplierDue(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    val name: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val due: Double = 0.0
)

@Serializable
data class SupplierDashboardStats(
    @Serializable(with = FlexibleDoubleSerializer::class) val totalDue: Double = 0.0,
    val topDues: List<TopSupplierDue> = emptyList(),
    @Serializable(with = FlexibleDoubleSerializer::class) val monthlyPurchase: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val monthlyPayment: Double = 0.0
)

@Serializable
data class DashboardStats(
    @Serializable(with = FlexibleIntSerializer::class) val totalSalesCount: Int = 0,
    @Serializable(with = FlexibleDoubleSerializer::class) val totalSalesAmount: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val totalProfit: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val emiOutstanding: Double = 0.0,
    @Serializable(with = FlexibleIntSerializer::class) val lowStockCount: Int = 0,
    val monthlySales: List<MonthlySalePoint> = emptyList(),
    val paymentMethods: List<PaymentMethodCount> = emptyList(),
    val lastBackupDate: String? = null,
    val supplierStats: SupplierDashboardStats = SupplierDashboardStats()
)

@Serializable
data class CloudStatusResponse(
    val connected: Boolean = false,
    @SerialName("firestore_db") val firestoreDb: String = "",
    val counts: Map<String, Int> = emptyMap(),
    @SerialName("last_checked") val lastChecked: String? = null,
    val error: String? = null
)

@Serializable
data class BackupLogItem(
    @Serializable(with = FlexibleIntSerializer::class) val id: Int = 0,
    @SerialName("backup_date") val backupDate: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("user_id") @Serializable(with = FlexibleIntSerializer::class) val userId: Int = 0,
    @SerialName("user_name") val userName: String? = null
)

@Serializable
data class PublicInvoiceResponse(
    val sale: SaleRecord = SaleRecord(),
    val installments: List<InstallmentRecord> = emptyList()
)

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val database: String = "",
    val firebase: String = "",
    val storage: String = "",
    val ready: Boolean = true
)

// Firestore models for Android cloud workspace & sync audit (matches firebase-blueprint.json & firestore.rules)
@Keep
@IgnoreExtraProperties
data class UserWorkspace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val erpAccessId: String = "",
    val serverUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SyncEvent(
    @DocumentId val id: String = "",
    val action: String = "",
    val entityType: String = "",
    val summary: String = "",
    val serverSynced: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
