package com.example.model

enum class BalanceType {
    RECEIVABLE, // GET -> Customer has to receive money (Red)
    PAYABLE     // GIVE -> Customer has to pay money (Green)
}

enum class MarketStatus {
    OPEN,
    CLOSED
}

enum class NotificationType {
    RATE_UPDATE,
    MARKET_STATUS,
    BALANCE_UPDATE,
    ITEM_MANAGEMENT,
    SYSTEM_ALERT
}

data class MarketItem(
    val id: String,
    val name: String,
    val currentRate: Double,
    val previousRate: Double = currentRate,
    val orderIndex: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Customer(
    val id: String,
    val name: String,
    val username: String,
    val passwordHash: String,
    val phone: String = "",
    val balance: Double = 0.0,
    val balanceType: BalanceType = BalanceType.RECEIVABLE,
    val isActive: Boolean = true,
    val hasCustomRates: Boolean = false,
    val customRateItem1: Double? = null,
    val customRateItem2: Double? = null,
    val customRateItem3: Double? = null,
    val customRateItem4: Double? = null,
    val customRatesMap: Map<String, Double> = emptyMap(), // itemId -> customRate
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MarketRates(
    val id: String = "current",
    val date: String,
    val item1: Double = 0.0,
    val item2: Double = 0.0,
    val item3: Double = 0.0,
    val item4: Double = 0.0,
    val isMarketOpen: Boolean = true,
    val previousItem1: Double = item1,
    val previousItem2: Double = item2,
    val previousItem3: Double = item3,
    val previousItem4: Double = item4,
    val updatedTime: Long = System.currentTimeMillis(),
    val updatedBy: String = "Admin"
)

data class RateHistoryItemSnapshot(
    val itemId: String,
    val itemName: String,
    val rate: Double,
    val previousRate: Double = rate
)

data class RateHistoryEntry(
    val id: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val item1: Double = 0.0,
    val item2: Double = 0.0,
    val item3: Double = 0.0,
    val item4: Double = 0.0,
    val itemsSummary: String = "",
    val itemsSnapshot: List<RateHistoryItemSnapshot> = emptyList(),
    val isMarketOpen: Boolean = true,
    val updatedBy: String = "Admin",
    val note: String = ""
)

data class AppNotification(
    val id: String,
    val customerId: String? = null, // null means broadcast to all customers
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.SYSTEM_ALERT
)

data class AdminUser(
    val id: String,
    val name: String,
    val username: String,
    val passwordHash: String,
    val email: String = "",
    val lastLogin: Long = System.currentTimeMillis()
)

enum class TransactionType {
    BILL,    // Maal Purchase (Goods bought from customer) -> Increases Customer Receivable (+)
    PAYMENT  // Adaigi (Cash/Bank payment given to customer) -> Decreases Customer Receivable (-)
}

data class TransactionRecord(
    val id: String,
    val customerId: String,
    val customerName: String,
    val type: TransactionType,
    val itemId: String? = null,
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "Kg",
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val paymentMethod: String = "Cash", // Cash, Bank Transfer, Cheque, Online
    val billNumber: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val balanceTypeAfter: BalanceType = BalanceType.RECEIVABLE,
    val recordedBy: String = "Admin"
)
