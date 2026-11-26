package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudSyncRepository
import com.example.data.local.SessionPreferences
import com.example.data.model.BackupLogItem
import com.example.data.model.CloudStatusResponse
import com.example.data.model.CreateSaleRequest
import com.example.data.model.CreateSaleResponse
import com.example.data.model.DashboardStats
import com.example.data.model.DuePlanPayload
import com.example.data.model.DueReportItem
import com.example.data.model.EmiPlanPayload
import com.example.data.model.InstallmentRecord
import com.example.data.model.InventoryItem
import com.example.data.model.PublicInvoiceResponse
import com.example.data.model.SalePaymentRecord
import com.example.data.model.SaleRecord
import com.example.data.model.SupplierItem
import com.example.data.model.SupplierLedgerResponse
import com.example.data.model.SupplierReportEntry
import com.example.data.model.SyncEvent
import com.example.data.model.UserProfile
import com.example.data.model.UserWorkspace
import com.example.data.network.ApiException
import com.example.data.network.ErpApiClient
import com.example.util.Formatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToastMessage(
    val id: Long,
    val message: String,
    val isError: Boolean = false
)

data class InvoiceReadyState(
    val response: CreateSaleResponse,
    val item: InventoryItem,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val paymentType: String,
    val saleDate: String,
    val staffName: String
)

data class ErpUiState(
    val serverUrl: String = SessionPreferences.DEFAULT_SERVER_URL,
    val isLoggedIn: Boolean = false,
    val currentUser: UserProfile? = null,
    val authLoading: Boolean = false,
    val authError: String? = null,

    val toast: ToastMessage? = null,
    val globalBusy: Boolean = false,

    val dashboardStats: DashboardStats? = null,
    val dashboardLoading: Boolean = false,

    val inventory: List<InventoryItem> = emptyList(),
    val inventoryLoading: Boolean = false,

    val sales: List<SaleRecord> = emptyList(),
    val salesLoading: Boolean = false,
    val lastInvoiceReady: InvoiceReadyState? = null,

    val selectedInstallments: List<InstallmentRecord> = emptyList(),
    val selectedSalePayments: List<SalePaymentRecord> = emptyList(),
    val installmentsLoading: Boolean = false,

    val dueReport: List<DueReportItem> = emptyList(),
    val dueReportLoading: Boolean = false,

    val suppliers: List<SupplierItem> = emptyList(),
    val suppliersLoading: Boolean = false,
    val selectedSupplierLedger: SupplierLedgerResponse? = null,
    val supplierReportEntries: List<SupplierReportEntry> = emptyList(),

    val staffList: List<UserProfile> = emptyList(),
    val staffLoading: Boolean = false,

    val cloudStatus: CloudStatusResponse? = null,
    val backupLogs: List<BackupLogItem> = emptyList(),
    val healthConnected: Boolean? = null,

    val publicInvoice: PublicInvoiceResponse? = null,
    val publicInvoiceLoading: Boolean = false,
    val publicInvoiceError: String? = null,

    val firebaseWorkspace: UserWorkspace? = null,
    val firebaseSyncEvents: List<SyncEvent> = emptyList()
)

class ErpViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SessionPreferences(application)
    private val api = ErpApiClient()
    private val cloudRepo = CloudSyncRepository(application)

    private val _uiState = MutableStateFlow(
        ErpUiState(
            serverUrl = prefs.getServerUrl(),
            isLoggedIn = prefs.getToken() != null && prefs.getUser() != null,
            currentUser = prefs.getUser()
        )
    )
    val uiState: StateFlow<ErpUiState> = _uiState.asStateFlow()

    init {
        api.baseUrl = prefs.getServerUrl()
        api.authToken = prefs.getToken()
        checkServerHealth()
        if (_uiState.value.isLoggedIn) {
            refreshAllData()
        }
    }

    fun observeFirebaseForUser(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            cloudRepo.observeWorkspace()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.w("ErpViewModel", "Workspace observe error: ${e.message}")
                }
                .collect { ws ->
                    _uiState.update { it.copy(firebaseWorkspace = ws) }
                }
        }
        viewModelScope.launch {
            cloudRepo.observeSyncEvents()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Log.w("ErpViewModel", "Sync events observe error: ${e.message}")
                }
                .collect { events ->
                    _uiState.update { it.copy(firebaseSyncEvents = events) }
                }
        }
        viewModelScope.launch {
            val user = _uiState.value.currentUser
            cloudRepo.upsertWorkspace(
                displayName = user?.name ?: "JHF Operator",
                erpAccessId = user?.accessId ?: "jhfadmin",
                serverUrl = _uiState.value.serverUrl
            )
        }
    }

    fun clearFirebaseState() {
        _uiState.update { it.copy(firebaseWorkspace = null, firebaseSyncEvents = emptyList()) }
    }

    fun notify(message: String, isError: Boolean = false) {
        val id = System.currentTimeMillis()
        _uiState.update { it.copy(toast = ToastMessage(id, message, isError)) }
        viewModelScope.launch {
            delay(3200)
            _uiState.update { state ->
                if (state.toast?.id == id) state.copy(toast = null) else state
            }
        }
    }

    fun updateServerUrl(newUrl: String) {
        val normalized = prefs.normalizeUrl(newUrl)
        prefs.setServerUrl(normalized)
        api.baseUrl = normalized
        _uiState.update { it.copy(serverUrl = normalized) }
        checkServerHealth()
        viewModelScope.launch {
            val user = _uiState.value.currentUser
            cloudRepo.upsertWorkspace(
                displayName = user?.name ?: "JHF Operator",
                erpAccessId = user?.accessId ?: "jhfadmin",
                serverUrl = normalized
            )
        }
        notify("Web Sync URL updated: $normalized")
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            try {
                api.health()
                _uiState.update { it.copy(healthConnected = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(healthConnected = false) }
            }
        }
    }

    fun login(accessId: String, password: String, serverUrl: String) {
        val cleanId = accessId.trim()
        if (cleanId.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Access ID and Password are required") }
            return
        }
        val normalizedUrl = prefs.normalizeUrl(serverUrl)
        prefs.setServerUrl(normalizedUrl)
        api.baseUrl = normalizedUrl

        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, serverUrl = normalizedUrl) }
            try {
                val res = api.login(cleanId, password)
                prefs.saveSession(res.token, res.user)
                api.authToken = res.token
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        currentUser = res.user,
                        authLoading = false,
                        authError = null,
                        healthConnected = true
                    )
                }
                cloudRepo.upsertWorkspace(res.user.name, res.user.accessId, normalizedUrl)
                cloudRepo.logSyncEvent("LOGIN", "auth", "Mobile session connected (${res.user.accessId})")
                refreshAllData()
                notify("Connected to live web ERP as ${res.user.name}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = e.message ?: "Unable to connect to server"
                _uiState.update { it.copy(authLoading = false, authError = msg) }
            }
        }
    }

    fun logout() {
        prefs.clearSession()
        api.authToken = null
        _uiState.update {
            ErpUiState(
                serverUrl = prefs.getServerUrl(),
                isLoggedIn = false,
                healthConnected = it.healthConnected
            )
        }
    }

    fun refreshAllData() {
        loadDashboard()
        loadInventory()
        loadSales()
        loadSuppliers()
        loadDueReport()
        if (_uiState.value.currentUser?.isManagerOrAdmin == true || _uiState.value.currentUser?.role == "Staff") {
            loadUsers()
        }
        if (_uiState.value.currentUser?.isAdmin == true) {
            loadCloudStatus()
            loadBackupLogs()
        }
    }

    private fun handleApiError(e: Exception, fallback: String) {
        if (e is ApiException && e.code == 401) {
            logout()
            notify("Session expired. Please sign in again.", isError = true)
        } else {
            notify(e.message ?: fallback, isError = true)
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(dashboardLoading = true) }
            try {
                val stats = api.getDashboardStats()
                _uiState.update { it.copy(dashboardStats = stats, dashboardLoading = false, healthConnected = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(dashboardLoading = false) }
                handleApiError(e, "Failed to load dashboard")
            }
        }
    }

    fun loadInventory() {
        viewModelScope.launch {
            _uiState.update { it.copy(inventoryLoading = true) }
            try {
                val list = api.getInventory()
                _uiState.update { it.copy(inventory = list, inventoryLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(inventoryLoading = false) }
                handleApiError(e, "Failed to load inventory")
            }
        }
    }

    fun addInventoryItem(
        model: String,
        config: String,
        serialNumber: String,
        buyPrice: Double,
        sellPrice: Double,
        supplierName: String,
        supplierId: Int?,
        purchaseDate: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.addInventory(
                    model = model.trim(),
                    config = config.trim(),
                    serialNumber = serialNumber.trim(),
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    supplierName = supplierName.trim(),
                    supplierId = supplierId,
                    purchaseDate = purchaseDate.ifBlank { Formatters.todayDhakaIso() }
                )
                cloudRepo.logSyncEvent("CREATE", "inventory", "Added $model ($serialNumber) to web")
                loadInventory()
                loadDashboard()
                loadSuppliers()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to add laptop")
            }
        }
    }

    fun updateInventoryItem(
        id: Int,
        model: String,
        config: String,
        serialNumber: String,
        buyPrice: Double,
        sellPrice: Double,
        supplierName: String,
        purchaseDate: String,
        status: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.updateInventory(
                    id = id,
                    model = model.trim(),
                    config = config.trim(),
                    serialNumber = serialNumber.trim(),
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    supplierName = supplierName.trim(),
                    purchaseDate = purchaseDate,
                    status = status
                )
                cloudRepo.logSyncEvent("UPDATE", "inventory", "Updated $model ($serialNumber) on web")
                loadInventory()
                loadDashboard()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to update laptop")
            }
        }
    }

    fun deleteInventoryItem(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.deleteInventory(id)
                cloudRepo.logSyncEvent("DELETE", "inventory", "Deleted inventory #$id from web")
                loadInventory()
                loadDashboard()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to delete item")
            }
        }
    }

    fun loadSales() {
        viewModelScope.launch {
            _uiState.update { it.copy(salesLoading = true) }
            try {
                val list = api.getSales()
                _uiState.update { it.copy(sales = list, salesLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(salesLoading = false) }
                handleApiError(e, "Failed to load sales")
            }
        }
    }

    fun createSale(
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
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val req = CreateSaleRequest(
                    inventoryId = item.id,
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim(),
                    customerAddress = customerAddress.trim(),
                    paymentType = paymentType,
                    saleDate = saleDate,
                    emiData = if (paymentType == "EMI") EmiPlanPayload(emiDownPayment, emiDurationMonths) else null,
                    dueData = if (paymentType == "Due") DuePlanPayload(
                        downPayment = dueDownPayment,
                        installmentPlan = duePlanMonths,
                        estimateDate = dueEstimateDate,
                        referenceName = dueReferenceName.trim(),
                        referencePhone = dueReferencePhone.trim()
                    ) else null
                )
                val res = api.createSale(req)
                val staffName = _uiState.value.currentUser?.name ?: "Jahid Hasan"
                cloudRepo.logSyncEvent("SALE", "sales", "Invoice ${res.invoiceNumber} created for $customerName")
                loadInventory()
                loadSales()
                loadDashboard()
                loadDueReport()
                _uiState.update {
                    it.copy(
                        globalBusy = false,
                        lastInvoiceReady = InvoiceReadyState(
                            response = res,
                            item = item,
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            customerAddress = customerAddress.trim(),
                            paymentType = paymentType,
                            saleDate = saleDate,
                            staffName = staffName
                        )
                    )
                }
                notify("Sale ${res.invoiceNumber} completed & synced to Web!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Error processing sale")
            }
        }
    }

    fun dismissInvoiceReady() {
        _uiState.update { it.copy(lastInvoiceReady = null) }
    }

    fun returnSale(saleId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.returnSale(saleId)
                cloudRepo.logSyncEvent("RETURN", "sales", "Returned sale #$saleId & restored stock")
                loadSales()
                loadInventory()
                loadDashboard()
                loadDueReport()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to return sale")
            }
        }
    }

    fun loadInstallments(saleId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(installmentsLoading = true) }
            try {
                val res = api.getInstallments(saleId)
                _uiState.update {
                    it.copy(
                        selectedInstallments = res.installments,
                        selectedSalePayments = res.payments,
                        installmentsLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(installmentsLoading = false) }
                handleApiError(e, "Failed to load installments")
            }
        }
    }

    fun recordInstallmentPayment(
        saleId: Int,
        installmentId: Int,
        amount: Double,
        paymentType: String,
        note: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.recordInstallmentPayment(installmentId, amount, paymentType, note)
                cloudRepo.logSyncEvent("PAYMENT", "installments", "Recorded ${Formatters.formatTaka(amount)} for sale #$saleId")
                loadInstallments(saleId)
                loadDashboard()
                loadDueReport()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Payment failed")
            }
        }
    }

    fun loadDueReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(dueReportLoading = true) }
            try {
                val list = api.getDueReport()
                _uiState.update { it.copy(dueReport = list, dueReportLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(dueReportLoading = false) }
                handleApiError(e, "Failed to load due report")
            }
        }
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            _uiState.update { it.copy(suppliersLoading = true) }
            try {
                val list = api.getSuppliers()
                _uiState.update { it.copy(suppliers = list, suppliersLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(suppliersLoading = false) }
                handleApiError(e, "Failed to load suppliers")
            }
        }
    }

    fun saveSupplier(
        editingId: Int?,
        name: String,
        mobile: String,
        address: String,
        openingBalance: Double,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = if (editingId != null) {
                    api.updateSupplier(editingId, name.trim(), mobile.trim(), address.trim(), openingBalance)
                } else {
                    api.addSupplier(name.trim(), mobile.trim(), address.trim(), openingBalance)
                }
                cloudRepo.logSyncEvent(if (editingId != null) "UPDATE" else "CREATE", "suppliers", "Saved supplier $name")
                loadSuppliers()
                loadDashboard()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to save supplier")
            }
        }
    }

    fun deleteSupplier(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.deleteSupplier(id)
                cloudRepo.logSyncEvent("DELETE", "suppliers", "Deleted supplier #$id")
                loadSuppliers()
                loadDashboard()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to delete supplier")
            }
        }
    }

    fun loadSupplierLedger(supplierId: Int) {
        viewModelScope.launch {
            try {
                val ledger = api.getSupplierLedger(supplierId)
                _uiState.update { it.copy(selectedSupplierLedger = ledger) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleApiError(e, "Failed to load supplier ledger")
            }
        }
    }

    fun recordSupplierPayment(
        supplierId: Int,
        paymentDate: String,
        amount: Double,
        paymentType: String,
        note: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.recordSupplierPayment(supplierId, paymentDate, amount, paymentType, note)
                cloudRepo.logSyncEvent("PAYMENT", "suppliers", "Paid supplier #$supplierId ${Formatters.formatTaka(amount)}")
                loadSuppliers()
                loadDashboard()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to record supplier payment")
            }
        }
    }

    fun fetchSupplierReport(supplierId: Int?, startDate: String, endDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val entries = api.getSupplierReport(supplierId, startDate, endDate)
                _uiState.update { it.copy(supplierReportEntries = entries, globalBusy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to generate supplier report")
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(staffLoading = true) }
            try {
                val list = api.getUsers()
                _uiState.update { it.copy(staffList = list, staffLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(staffLoading = false) }
                handleApiError(e, "Failed to load staff")
            }
        }
    }

    fun saveUser(
        editingId: Int?,
        name: String,
        accessId: String,
        password: String,
        role: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = if (editingId != null) {
                    api.updateUser(editingId, name.trim(), accessId.trim(), password.takeIf { it.isNotBlank() }, role)
                } else {
                    api.createUser(name.trim(), accessId.trim(), password, role)
                }
                cloudRepo.logSyncEvent(if (editingId != null) "UPDATE" else "CREATE", "users", "Saved staff $name")
                loadUsers()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to save staff member")
            }
        }
    }

    fun deleteUser(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.deleteUser(id)
                cloudRepo.logSyncEvent("DELETE", "users", "Deleted staff #$id")
                loadUsers()
                _uiState.update { it.copy(globalBusy = false) }
                notify("$msg (Synced to Web)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Failed to delete staff member")
            }
        }
    }

    fun loadCloudStatus() {
        viewModelScope.launch {
            try {
                val status = api.getCloudStatus()
                _uiState.update { it.copy(cloudStatus = status) }
            } catch (_: Exception) {
            }
        }
    }

    fun loadBackupLogs() {
        viewModelScope.launch {
            try {
                val logs = api.getBackupLogs()
                _uiState.update { it.copy(backupLogs = logs) }
            } catch (_: Exception) {
            }
        }
    }

    fun forceCloudSyncAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.syncAllToCloud()
                cloudRepo.logSyncEvent("FULL_SYNC", "system", "Triggered web full cloud backup")
                loadCloudStatus()
                _uiState.update { it.copy(globalBusy = false) }
                notify(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Cloud sync failed")
            }
        }
    }

    fun rehydrateWebFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.hydrateFromCloud()
                refreshAllData()
                _uiState.update { it.copy(globalBusy = false) }
                notify(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Re-hydrate failed")
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.resetDemoData()
                cloudRepo.logSyncEvent("RESET", "system", "System data reset performed")
                refreshAllData()
                _uiState.update { it.copy(globalBusy = false) }
                notify(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Reset failed")
            }
        }
    }

    fun downloadBackup(onReady: (fileName: String, rawJson: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val (fileName, raw) = api.downloadBackupRaw()
                loadBackupLogs()
                _uiState.update { it.copy(globalBusy = false) }
                onReady(fileName, raw)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Backup failed")
            }
        }
    }

    fun restoreBackup(rawJson: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(globalBusy = true) }
            try {
                val msg = api.restoreBackupRaw(rawJson)
                cloudRepo.logSyncEvent("RESTORE", "system", "Database restored from mobile JSON backup")
                refreshAllData()
                _uiState.update { it.copy(globalBusy = false) }
                notify(msg)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(globalBusy = false) }
                handleApiError(e, "Restore failed")
            }
        }
    }

    fun lookupPublicInvoice(invoiceNumber: String, token: String?) {
        if (invoiceNumber.isBlank()) {
            _uiState.update { it.copy(publicInvoiceError = "Enter an invoice number (e.g. JHF-20260401-0001)") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(publicInvoiceLoading = true, publicInvoiceError = null) }
            try {
                val res = api.getPublicInvoice(invoiceNumber, token)
                _uiState.update { it.copy(publicInvoice = res, publicInvoiceLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        publicInvoice = null,
                        publicInvoiceLoading = false,
                        publicInvoiceError = e.message ?: "Invoice not found"
                    )
                }
            }
        }
    }
}
