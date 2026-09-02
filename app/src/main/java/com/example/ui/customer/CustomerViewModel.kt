package com.example.ui.customer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MujahidRepository
import com.example.model.AppNotification
import com.example.model.Customer
import com.example.model.MarketItem
import com.example.model.MarketRates
import com.example.model.RateHistoryEntry
import com.example.model.TransactionRecord
import com.example.util.NetworkMonitor
import com.example.util.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EffectiveItemRate(
    val itemId: String = "",
    val itemIndex: Int,
    val itemName: String,
    val currentRate: Double,
    val previousRate: Double,
    val isCustomRate: Boolean
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MujahidRepository(application)
    private val sessionManager = SessionManager(application)
    private val networkMonitor = NetworkMonitor(application)

    val currentCustomerId: String = sessionManager.getUserId() ?: ""

    val customerFlow: StateFlow<Customer?> = if (currentCustomerId.isNotBlank()) {
        repository.getCustomerByIdFlow(currentCustomerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    val marketItemsFlow: StateFlow<List<MarketItem>> = repository.getAllActiveItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRatesFlow: StateFlow<MarketRates?> = repository.getCurrentRatesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rateHistoryFlow: StateFlow<List<RateHistoryEntry>> = repository.getAllRateHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationsFlow: StateFlow<List<AppNotification>> = if (currentCustomerId.isNotBlank()) {
        repository.getNotificationsForCustomerFlow(currentCustomerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val myTransactionsFlow: StateFlow<List<TransactionRecord>> = if (currentCustomerId.isNotBlank()) {
        repository.getTransactionsForCustomerFlow(currentCustomerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val isOnlineFlow: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isAccountBlocked = MutableStateFlow(false)
    val isAccountBlocked: StateFlow<Boolean> = _isAccountBlocked.asStateFlow()

    init {
        // Monitor account active status
        viewModelScope.launch {
            customerFlow.collect { customer ->
                if (customer != null && !customer.isActive) {
                    _isAccountBlocked.value = true
                    sessionManager.clearSession()
                }
            }
        }
    }

    fun getEffectiveRates(
        customer: Customer?,
        marketRates: MarketRates?
    ): List<EffectiveItemRate> = getEffectiveRates(customer, marketItemsFlow.value, marketRates)

    fun getEffectiveRates(
        customer: Customer?,
        items: List<MarketItem>,
        marketRates: MarketRates?
    ): List<EffectiveItemRate> {
        val hasCustom = customer?.hasCustomRates == true
        if (items.isNotEmpty()) {
            return items.mapIndexed { index, item ->
                val customVal = if (hasCustom) {
                    customer?.customRatesMap?.get(item.id)
                        ?: when (index) {
                            0 -> customer?.customRateItem1
                            1 -> customer?.customRateItem2
                            2 -> customer?.customRateItem3
                            3 -> customer?.customRateItem4
                            else -> null
                        }
                } else null

                val effectiveRate = customVal ?: item.currentRate
                val isCustom = customVal != null

                EffectiveItemRate(
                    itemId = item.id,
                    itemIndex = index + 1,
                    itemName = item.name,
                    currentRate = effectiveRate,
                    previousRate = item.previousRate,
                    isCustomRate = isCustom
                )
            }
        }

        // Fallback if items table empty yet
        val rates = marketRates ?: MarketRates(date = "", item1 = 0.0, item2 = 0.0, item3 = 0.0, item4 = 0.0)
        val r1 = if (hasCustom && customer?.customRateItem1 != null) customer.customRateItem1 else rates.item1
        val r2 = if (hasCustom && customer?.customRateItem2 != null) customer.customRateItem2 else rates.item2
        val r3 = if (hasCustom && customer?.customRateItem3 != null) customer.customRateItem3 else rates.item3
        val r4 = if (hasCustom && customer?.customRateItem4 != null) customer.customRateItem4 else rates.item4

        return listOf(
            EffectiveItemRate("item_1", 1, "Item 1", r1, rates.previousItem1, hasCustom && customer?.customRateItem1 != null),
            EffectiveItemRate("item_2", 2, "Item 2", r2, rates.previousItem2, hasCustom && customer?.customRateItem2 != null),
            EffectiveItemRate("item_3", 3, "Item 3", r3, rates.previousItem3, hasCustom && customer?.customRateItem3 != null),
            EffectiveItemRate("item_4", 4, "Item 4", r4, rates.previousItem4, hasCustom && customer?.customRateItem4 != null)
        )
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
