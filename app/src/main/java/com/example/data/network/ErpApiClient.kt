package com.example.data.network

import com.example.data.model.BackupLogItem
import com.example.data.model.CloudStatusResponse
import com.example.data.model.CreateSaleRequest
import com.example.data.model.CreateSaleResponse
import com.example.data.model.DashboardStats
import com.example.data.model.DueReportItem
import com.example.data.model.HealthResponse
import com.example.data.model.InstallmentsResponse
import com.example.data.model.InventoryItem
import com.example.data.model.LoginResponse
import com.example.data.model.PublicInvoiceResponse
import com.example.data.model.SaleRecord
import com.example.data.model.SupplierItem
import com.example.data.model.SupplierLedgerResponse
import com.example.data.model.SupplierReportEntry
import com.example.data.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiException(val code: Int, override val message: String) : Exception(message)

class ErpApiClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Volatile
    var baseUrl: String = "https://jhflaptop.cloud.run"

    @Volatile
    var authToken: String? = null

    private suspend fun request(
        method: String,
        path: String,
        bodyJson: String? = null,
        requiresAuth: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val fullUrl = "$cleanBase$path"
        val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (requiresAuth) {
                authToken?.let { token ->
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            if (bodyJson != null) {
                doOutput = true
            }
        }

        try {
            if (bodyJson != null) {
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(bodyJson)
                    writer.flush()
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            if (code !in 200..299) {
                val parsedMsg = runCatching {
                    json.parseToJsonElement(responseText).jsonObject["message"]?.jsonPrimitive?.content
                }.getOrNull()
                throw ApiException(
                    code = code,
                    message = parsedMsg ?: "Request failed (HTTP $code)"
                )
            }
            responseText
        } finally {
            conn.disconnect()
        }
    }

    suspend fun login(accessId: String, password: String): LoginResponse {
        val payload = buildJsonObject {
            put("access_id", accessId)
            put("password", password)
        }.toString()
        val raw = request("POST", "/api/auth/login", payload, requiresAuth = false)
        return json.decodeFromString(raw)
    }

    suspend fun health(): HealthResponse {
        val raw = request("GET", "/api/health", requiresAuth = false)
        return json.decodeFromString(raw)
    }

    suspend fun getDashboardStats(): DashboardStats {
        val raw = request("GET", "/api/dashboard/stats")
        return json.decodeFromString(raw)
    }

    suspend fun getInventory(): List<InventoryItem> {
        val raw = request("GET", "/api/inventory")
        return json.decodeFromString(raw)
    }

    suspend fun addInventory(
        model: String,
        config: String,
        serialNumber: String,
        buyPrice: Double,
        sellPrice: Double,
        supplierName: String?,
        supplierId: Int?,
        purchaseDate: String
    ): String {
        val payload = buildJsonObject {
            put("model", model)
            put("config", config)
            put("serial_number", serialNumber)
            put("buy_price", buyPrice)
            put("sell_price", sellPrice)
            put("supplier_name", supplierName ?: "")
            if (supplierId != null && supplierId > 0) put("supplier_id", supplierId)
            put("purchase_date", purchaseDate)
        }.toString()
        return extractMessage(request("POST", "/api/inventory", payload), "Laptop added to stock")
    }

    suspend fun updateInventory(
        id: Int,
        model: String,
        config: String,
        serialNumber: String,
        buyPrice: Double,
        sellPrice: Double,
        supplierName: String?,
        purchaseDate: String,
        status: String
    ): String {
        val payload = buildJsonObject {
            put("model", model)
            put("config", config)
            put("serial_number", serialNumber)
            put("buy_price", buyPrice)
            put("sell_price", sellPrice)
            put("supplier_name", supplierName ?: "")
            put("purchase_date", purchaseDate)
            put("status", status)
        }.toString()
        return extractMessage(request("PUT", "/api/inventory/$id", payload), "Inventory updated")
    }

    suspend fun deleteInventory(id: Int): String {
        return extractMessage(request("DELETE", "/api/inventory/$id"), "Inventory item deleted")
    }

    suspend fun suggestModels(query: String): List<String> {
        val encoded = withContext(Dispatchers.IO) { URLEncoder.encode(query, "UTF-8") }
        val raw = request("GET", "/api/ai/suggest-models?q=$encoded", requiresAuth = false)
        val arr = json.parseToJsonElement(raw).jsonObject["suggestions"] as? JsonArray ?: return emptyList()
        return arr.mapNotNull { it.jsonPrimitive.content }
    }

    suspend fun suggestConfig(model: String): String {
        val encoded = withContext(Dispatchers.IO) { URLEncoder.encode(model, "UTF-8") }
        val raw = request("GET", "/api/ai/suggest-config?model=$encoded", requiresAuth = false)
        return json.parseToJsonElement(raw).jsonObject["config"]?.jsonPrimitive?.content.orEmpty()
    }

    suspend fun getSales(): List<SaleRecord> {
        val raw = request("GET", "/api/sales")
        return json.decodeFromString(raw)
    }

    suspend fun createSale(req: CreateSaleRequest): CreateSaleResponse {
        val payload = json.encodeToString(CreateSaleRequest.serializer(), req)
        val raw = request("POST", "/api/sales", payload)
        return json.decodeFromString(raw)
    }

    suspend fun returnSale(saleId: Int): String {
        return extractMessage(request("DELETE", "/api/sales/$saleId"), "Sale returned and stock restored")
    }

    suspend fun getInstallments(saleId: Int): InstallmentsResponse {
        val raw = request("GET", "/api/installments/$saleId")
        return json.decodeFromString(raw)
    }

    suspend fun recordInstallmentPayment(
        installmentId: Int,
        amount: Double,
        paymentType: String,
        note: String
    ): String {
        val payload = buildJsonObject {
            put("amount", amount)
            put("payment_type", paymentType)
            put("note", note)
        }.toString()
        return extractMessage(request("POST", "/api/installments/payment/$installmentId", payload), "Payment recorded")
    }

    suspend fun getDueReport(): List<DueReportItem> {
        val raw = request("GET", "/api/reports/due")
        return json.decodeFromString(raw)
    }

    suspend fun getSuppliers(): List<SupplierItem> {
        val raw = request("GET", "/api/suppliers")
        return json.decodeFromString(raw)
    }

    suspend fun addSupplier(
        name: String,
        mobile: String,
        address: String,
        openingBalance: Double
    ): String {
        val payload = buildJsonObject {
            put("name", name)
            put("mobile", mobile)
            put("address", address)
            put("opening_balance", openingBalance)
        }.toString()
        return extractMessage(request("POST", "/api/suppliers", payload), "Supplier added")
    }

    suspend fun updateSupplier(
        id: Int,
        name: String,
        mobile: String,
        address: String,
        openingBalance: Double
    ): String {
        val payload = buildJsonObject {
            put("name", name)
            put("mobile", mobile)
            put("address", address)
            put("opening_balance", openingBalance)
        }.toString()
        return extractMessage(request("PUT", "/api/suppliers/$id", payload), "Supplier updated")
    }

    suspend fun deleteSupplier(id: Int): String {
        return extractMessage(request("DELETE", "/api/suppliers/$id"), "Supplier deleted")
    }

    suspend fun getSupplierLedger(supplierId: Int): SupplierLedgerResponse {
        val raw = request("GET", "/api/suppliers/ledger/$supplierId")
        return json.decodeFromString(raw)
    }

    suspend fun recordSupplierPayment(
        supplierId: Int,
        paymentDate: String,
        paymentAmount: Double,
        paymentType: String,
        note: String
    ): String {
        val payload = buildJsonObject {
            put("supplier_id", supplierId)
            put("payment_date", paymentDate)
            put("payment_amount", paymentAmount)
            put("payment_type", paymentType)
            put("note", note)
        }.toString()
        return extractMessage(request("POST", "/api/suppliers/payments", payload), "Supplier payment recorded")
    }

    suspend fun getSupplierReport(
        supplierId: Int?,
        startDate: String?,
        endDate: String?
    ): List<SupplierReportEntry> {
        val params = buildList {
            if (supplierId != null && supplierId > 0) add("supplier_id=$supplierId")
            if (!startDate.isNullOrBlank()) add("start_date=${URLEncoder.encode(startDate, "UTF-8")}")
            if (!endDate.isNullOrBlank()) add("end_date=${URLEncoder.encode(endDate, "UTF-8")}")
        }.joinToString("&")
        val suffix = if (params.isNotBlank()) "?$params" else ""
        val raw = request("GET", "/api/reports/supplier$suffix")
        return json.decodeFromString(raw)
    }

    suspend fun getUsers(): List<UserProfile> {
        val raw = request("GET", "/api/users")
        return json.decodeFromString(raw)
    }

    suspend fun createUser(name: String, accessId: String, password: String, role: String): String {
        val payload = buildJsonObject {
            put("name", name)
            put("access_id", accessId)
            put("password", password)
            put("role", role)
        }.toString()
        return extractMessage(request("POST", "/api/users", payload), "Staff member created")
    }

    suspend fun updateUser(
        id: Int,
        name: String,
        accessId: String,
        password: String?,
        role: String
    ): String {
        val payload = buildJsonObject {
            put("name", name)
            put("access_id", accessId)
            if (!password.isNullOrBlank()) put("password", password)
            put("role", role)
        }.toString()
        return extractMessage(request("PUT", "/api/users/$id", payload), "Staff member updated")
    }

    suspend fun deleteUser(id: Int): String {
        return extractMessage(request("DELETE", "/api/users/$id"), "Staff member deleted")
    }

    suspend fun getCloudStatus(): CloudStatusResponse {
        val raw = request("GET", "/api/cloud/status", requiresAuth = false)
        return json.decodeFromString(raw)
    }

    suspend fun syncAllToCloud(): String {
        return extractMessage(request("POST", "/api/cloud/sync-all", "{}"), "Full cloud sync completed")
    }

    suspend fun hydrateFromCloud(): String {
        return extractMessage(request("POST", "/api/cloud/hydrate", "{}"), "Cloud data hydrated")
    }

    suspend fun resetDemoData(): String {
        return extractMessage(request("POST", "/api/system/reset-demo-data", "{}"), "System data reset completed")
    }

    suspend fun downloadBackupRaw(): Pair<String, String> {
        val raw = request("GET", "/api/backup")
        val obj = json.parseToJsonElement(raw).jsonObject
        val fileName = obj["fileName"]?.jsonPrimitive?.content ?: "JHF_Backup.json"
        return fileName to raw
    }

    suspend fun restoreBackupRaw(rawBackupJson: String): String {
        val element = json.parseToJsonElement(rawBackupJson).jsonObject
        val payloadObj: JsonObject = if (element.containsKey("data")) {
            element
        } else {
            buildJsonObject { put("data", element) }
        }
        return extractMessage(request("POST", "/api/restore", payloadObj.toString()), "Database restored and synced")
    }

    suspend fun getBackupLogs(): List<BackupLogItem> {
        val raw = request("GET", "/api/backup/logs")
        return json.decodeFromString(raw)
    }

    suspend fun getPublicInvoice(invoiceNumber: String, token: String?): PublicInvoiceResponse {
        val encodedInv = withContext(Dispatchers.IO) { URLEncoder.encode(invoiceNumber.trim(), "UTF-8") }
        val query = if (!token.isNullOrBlank()) {
            val encodedToken = withContext(Dispatchers.IO) { URLEncoder.encode(token.trim(), "UTF-8") }
            "?token=$encodedToken"
        } else ""
        val raw = request("GET", "/api/public/invoice/$encodedInv$query", requiresAuth = false)
        return json.decodeFromString(raw)
    }

    private fun extractMessage(raw: String, fallback: String): String {
        return runCatching {
            json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull() ?: fallback
    }
}
