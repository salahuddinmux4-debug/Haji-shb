package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MujahidRepository
import com.example.model.AppNotification
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketItem
import com.example.model.MarketRates
import com.example.model.RateHistoryEntry
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.util.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdminDashboardStats(
    val totalCustomers: Int = 0,
    val activeCustomers: Int = 0,
    val totalReceivable: Double = 0.0, // GET
    val totalPayable: Double = 0.0,    // GIVE
    val netBalance: Double = 0.0,
    val totalBillsAmount: Double = 0.0,
    val totalPaymentsAmount: Double = 0.0,
    val totalTransactionsCount: Int = 0
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MujahidRepository(application)
    private val sessionManager = SessionManager(application)

    val customersFlow: StateFlow<List<Customer>> = repository.getAllCustomersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketItemsFlow: StateFlow<List<MarketItem>> = repository.getAllActiveItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRatesFlow: StateFlow<MarketRates?> = repository.getCurrentRatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rateHistoryFlow: StateFlow<List<RateHistoryEntry>> = repository.getAllRateHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationsFlow: StateFlow<List<AppNotification>> = repository.getAllNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsFlow: StateFlow<List<TransactionRecord>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsFlow: StateFlow<AdminDashboardStats> = combine(customersFlow, transactionsFlow) { customers, transactions ->
        val activeCount = customers.count { it.isActive }
        var sumReceivable = 0.0
        var sumPayable = 0.0
        customers.forEach { c ->
            if (c.balanceType == BalanceType.RECEIVABLE) {
                sumReceivable += c.balance
            } else {
                sumPayable += c.balance
            }
        }

        var billsTotal = 0.0
        var paymentsTotal = 0.0
        transactions.forEach { tx ->
            if (tx.type == TransactionType.BILL) {
                billsTotal += tx.amount
            } else {
                paymentsTotal += tx.amount
            }
        }

        AdminDashboardStats(
            totalCustomers = customers.size,
            activeCustomers = activeCount,
            totalReceivable = sumReceivable,
            totalPayable = sumPayable,
            netBalance = sumReceivable - sumPayable,
            totalBillsAmount = billsTotal,
            totalPaymentsAmount = paymentsTotal,
            totalTransactionsCount = transactions.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardStats())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // ==================== ITEM MANAGEMENT ====================

    fun addItem(name: String, initialRate: Double, onSuccess: () -> Unit) {
        if (name.isBlank()) {
            _statusMessage.value = "Item name cannot be empty"
            return
        }
        viewModelScope.launch {
            val result = repository.addItem(name, initialRate, sessionManager.getUserName() ?: "Admin")
            result.onSuccess {
                _statusMessage.value = "Item '${it.name}' added successfully"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to add item"
            }
        }
    }

    fun editItemName(itemId: String, newName: String, onSuccess: () -> Unit) {
        if (newName.isBlank()) {
            _statusMessage.value = "Item name cannot be empty"
            return
        }
        viewModelScope.launch {
            val result = repository.editItemName(itemId, newName)
            result.onSuccess {
                _statusMessage.value = "Item renamed to '$newName' successfully"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to rename item"
            }
        }
    }

    fun removeItem(itemId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.removeItem(itemId)
            result.onSuccess {
                _statusMessage.value = "Item removed successfully"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to remove item"
            }
        }
    }

    fun updateItemRate(itemId: String, newRate: Double) {
        viewModelScope.launch {
            val result = repository.updateItemRate(itemId, newRate, sessionManager.getUserName() ?: "Admin")
            if (result.isSuccess) {
                _statusMessage.value = "Rate updated successfully to Rs. $newRate"
            } else {
                _statusMessage.value = "Error updating rate: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateAllItemRates(ratesMap: Map<String, Double>) {
        viewModelScope.launch {
            val result = repository.updateAllItemRates(ratesMap, sessionManager.getUserName() ?: "Admin")
            if (result.isSuccess) {
                _statusMessage.value = "All daily market rates published successfully!"
            } else {
                _statusMessage.value = "Error publishing rates: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    // Rate operations (legacy compatibility)
    fun updateIndividualRate(itemIndex: Int, newRate: Double) {
        val items = marketItemsFlow.value
        val targetItem = items.getOrNull(itemIndex - 1)
        if (targetItem != null) {
            updateItemRate(targetItem.id, newRate)
        }
    }

    fun toggleMarketStatus(isOpen: Boolean) {
        viewModelScope.launch {
            val result = repository.setMarketStatus(isOpen, sessionManager.getUserName() ?: "Admin")
            if (result.isSuccess) {
                _statusMessage.value = if (isOpen) "Market marked as OPEN" else "Market marked as CLOSED"
            }
        }
    }

    // ==================== CUSTOMER OPERATIONS ====================

    fun addCustomer(
        name: String,
        username: String,
        password: String,
        phone: String,
        balance: Double,
        balanceType: BalanceType,
        hasCustomRates: Boolean,
        cItem1: Double? = null,
        cItem2: Double? = null,
        cItem3: Double? = null,
        cItem4: Double? = null,
        customRatesMap: Map<String, Double> = emptyMap(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addCustomer(
                name = name,
                username = username,
                plainPass = password,
                phone = phone,
                balance = balance,
                balanceType = balanceType,
                hasCustomRates = hasCustomRates,
                customRateItem1 = cItem1,
                customRateItem2 = cItem2,
                customRateItem3 = cItem3,
                customRateItem4 = cItem4,
                customRatesMap = customRatesMap
            )
            result.onSuccess {
                _statusMessage.value = "Customer '${it.name}' added successfully"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to add customer"
            }
        }
    }

    fun updateCustomer(
        customer: Customer,
        newPassword: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateCustomer(customer, newPassword)
            result.onSuccess {
                _statusMessage.value = "Customer updated successfully"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to update customer"
            }
        }
    }

    fun updateCustomerBalance(customerId: String, newBalance: Double, balanceType: BalanceType, note: String = "") {
        viewModelScope.launch {
            val result = repository.updateCustomerBalance(customerId, newBalance, balanceType, note)
            result.onSuccess {
                _statusMessage.value = "Customer balance updated successfully"
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to update balance"
            }
        }
    }

    fun toggleCustomerActive(customerId: String) {
        viewModelScope.launch {
            val result = repository.toggleCustomerActiveStatus(customerId)
            result.onSuccess { newStatus ->
                _statusMessage.value = if (newStatus) "Customer activated" else "Customer deactivated"
            }
        }
    }

    fun deleteCustomer(customerId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            _statusMessage.value = "Customer deleted"
            onSuccess()
        }
    }

    fun sendBroadcastNotification(title: String, message: String) {
        viewModelScope.launch {
            repository.sendBroadcastNotification(title, message)
            _statusMessage.value = "Notification sent to all customers"
        }
    }

    // ==================== BILLS & PAYMENTS (KHATA TRANSACTIONS) ====================

    fun addBill(
        customerId: String,
        itemId: String?,
        itemName: String,
        quantity: Double,
        unit: String,
        rate: Double,
        totalAmount: Double,
        billNumber: String,
        notes: String,
        date: String,
        onSuccess: () -> Unit
    ) {
        if (customerId.isBlank()) {
            _statusMessage.value = "Please select a customer"
            return
        }
        if (itemName.isBlank()) {
            _statusMessage.value = "Please enter item or product name"
            return
        }
        if (totalAmount <= 0) {
            _statusMessage.value = "Total bill amount must be greater than 0"
            return
        }

        viewModelScope.launch {
            val result = repository.addBillPurchase(
                customerId = customerId,
                itemId = itemId,
                itemName = itemName,
                quantity = quantity,
                unit = unit,
                rate = rate,
                totalAmount = totalAmount,
                billNumber = billNumber,
                notes = notes,
                date = date,
                recordedBy = sessionManager.getUserName() ?: "Admin"
            )
            result.onSuccess {
                _statusMessage.value = "Bill #${it.billNumber} created successfully for ${it.customerName}"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to create bill"
            }
        }
    }

    fun addPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        referenceNo: String,
        notes: String,
        date: String,
        onSuccess: () -> Unit
    ) {
        if (customerId.isBlank()) {
            _statusMessage.value = "Please select a customer"
            return
        }
        if (amount <= 0) {
            _statusMessage.value = "Payment amount must be greater than 0"
            return
        }

        viewModelScope.launch {
            val result = repository.addPaymentGiven(
                customerId = customerId,
                amount = amount,
                paymentMethod = paymentMethod,
                referenceNo = referenceNo,
                notes = notes,
                date = date,
                recordedBy = sessionManager.getUserName() ?: "Admin"
            )
            result.onSuccess {
                _statusMessage.value = "Payment of Rs. ${amount} recorded for ${it.customerName}"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to record payment"
            }
        }
    }

    fun deleteTransaction(transactionId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(transactionId)
            result.onSuccess {
                _statusMessage.value = "Transaction deleted and balance adjusted"
                onSuccess()
            }.onFailure {
                _statusMessage.value = it.message ?: "Failed to delete transaction"
            }
        }
    }
}
