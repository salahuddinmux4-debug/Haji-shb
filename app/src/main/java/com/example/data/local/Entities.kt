package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketRates
import com.example.model.MarketItem
import com.example.model.RateHistoryEntry
import com.example.model.RateHistoryItemSnapshot
import com.example.model.AppNotification
import com.example.model.NotificationType
import com.example.model.AdminUser
import com.example.model.TransactionType
import com.example.model.TransactionRecord

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val passwordHash: String,
    val phone: String = "",
    val balance: Double = 0.0,
    val balanceType: String = BalanceType.RECEIVABLE.name,
    val isActive: Boolean = true,
    val hasCustomRates: Boolean = false,
    val customRateItem1: Double? = null,
    val customRateItem2: Double? = null,
    val customRateItem3: Double? = null,
    val customRateItem4: Double? = null,
    val customRatesJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Customer {
        val map = mutableMapOf<String, Double>()
        try {
            if (customRatesJson.isNotBlank()) {
                val obj = org.json.JSONObject(customRatesJson)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.optDouble(key, 0.0)
                }
            }
        } catch (_: Exception) {}

        if (customRateItem1 != null) map["item_1"] = customRateItem1
        if (customRateItem2 != null) map["item_2"] = customRateItem2
        if (customRateItem3 != null) map["item_3"] = customRateItem3
        if (customRateItem4 != null) map["item_4"] = customRateItem4

        return Customer(
            id = id,
            name = name,
            username = username,
            passwordHash = passwordHash,
            phone = phone,
            balance = balance,
            balanceType = try { BalanceType.valueOf(balanceType) } catch (_: Exception) { BalanceType.RECEIVABLE },
            isActive = isActive,
            hasCustomRates = hasCustomRates,
            customRateItem1 = customRateItem1,
            customRateItem2 = customRateItem2,
            customRateItem3 = customRateItem3,
            customRateItem4 = customRateItem4,
            customRatesMap = map,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(customer: Customer): CustomerEntity {
            val jsonObj = org.json.JSONObject()
            customer.customRatesMap.forEach { (k, v) -> jsonObj.put(k, v) }
            return CustomerEntity(
                id = customer.id,
                name = customer.name,
                username = customer.username,
                passwordHash = customer.passwordHash,
                phone = customer.phone,
                balance = customer.balance,
                balanceType = customer.balanceType.name,
                isActive = customer.isActive,
                hasCustomRates = customer.hasCustomRates,
                customRateItem1 = customer.customRateItem1,
                customRateItem2 = customer.customRateItem2,
                customRateItem3 = customer.customRateItem3,
                customRateItem4 = customer.customRateItem4,
                customRatesJson = jsonObj.toString(),
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt
            )
        }
    }
}

@Entity(tableName = "market_items")
data class MarketItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currentRate: Double,
    val previousRate: Double,
    val orderIndex: Int = 0,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MarketItem = MarketItem(
        id = id,
        name = name,
        currentRate = currentRate,
        previousRate = previousRate,
        orderIndex = orderIndex,
        isDeleted = isDeleted,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(item: MarketItem): MarketItemEntity = MarketItemEntity(
            id = item.id,
            name = item.name,
            currentRate = item.currentRate,
            previousRate = item.previousRate,
            orderIndex = item.orderIndex,
            isDeleted = item.isDeleted,
            updatedAt = item.updatedAt
        )
    }
}

@Entity(tableName = "market_rates")
data class MarketRatesEntity(
    @PrimaryKey val id: String = "current",
    val date: String,
    val item1: Double,
    val item2: Double,
    val item3: Double,
    val item4: Double,
    val isMarketOpen: Boolean = true,
    val previousItem1: Double = item1,
    val previousItem2: Double = item2,
    val previousItem3: Double = item3,
    val previousItem4: Double = item4,
    val updatedTime: Long = System.currentTimeMillis(),
    val updatedBy: String = "Admin"
) {
    fun toDomain(): MarketRates = MarketRates(
        id = id,
        date = date,
        item1 = item1,
        item2 = item2,
        item3 = item3,
        item4 = item4,
        isMarketOpen = isMarketOpen,
        previousItem1 = previousItem1,
        previousItem2 = previousItem2,
        previousItem3 = previousItem3,
        previousItem4 = previousItem4,
        updatedTime = updatedTime,
        updatedBy = updatedBy
    )

    companion object {
        fun fromDomain(domain: MarketRates): MarketRatesEntity = MarketRatesEntity(
            id = domain.id,
            date = domain.date,
            item1 = domain.item1,
            item2 = domain.item2,
            item3 = domain.item3,
            item4 = domain.item4,
            isMarketOpen = domain.isMarketOpen,
            previousItem1 = domain.previousItem1,
            previousItem2 = domain.previousItem2,
            previousItem3 = domain.previousItem3,
            previousItem4 = domain.previousItem4,
            updatedTime = domain.updatedTime,
            updatedBy = domain.updatedBy
        )
    }
}

@Entity(tableName = "rate_history")
data class RateHistoryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val item1: Double,
    val item2: Double,
    val item3: Double,
    val item4: Double,
    val isMarketOpen: Boolean = true,
    val updatedBy: String = "Admin",
    val note: String = ""
) {
    fun toDomain(): RateHistoryEntry = RateHistoryEntry(
        id = id,
        date = date,
        timestamp = timestamp,
        item1 = item1,
        item2 = item2,
        item3 = item3,
        item4 = item4,
        isMarketOpen = isMarketOpen,
        updatedBy = updatedBy,
        note = note
    )

    companion object {
        fun fromDomain(entry: RateHistoryEntry): RateHistoryEntity = RateHistoryEntity(
            id = entry.id,
            date = entry.date,
            timestamp = entry.timestamp,
            item1 = entry.item1,
            item2 = entry.item2,
            item3 = entry.item3,
            item4 = entry.item4,
            isMarketOpen = entry.isMarketOpen,
            updatedBy = entry.updatedBy,
            note = entry.note
        )
    }
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val customerId: String? = null,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = NotificationType.SYSTEM_ALERT.name
) {
    fun toDomain(): AppNotification = AppNotification(
        id = id,
        customerId = customerId,
        title = title,
        message = message,
        timestamp = timestamp,
        isRead = isRead,
        type = try { NotificationType.valueOf(type) } catch (_: Exception) { NotificationType.SYSTEM_ALERT }
    )

    companion object {
        fun fromDomain(notification: AppNotification): NotificationEntity = NotificationEntity(
            id = notification.id,
            customerId = notification.customerId,
            title = notification.title,
            message = notification.message,
            timestamp = notification.timestamp,
            isRead = notification.isRead,
            type = notification.type.name
        )
    }
}

@Entity(tableName = "admins")
data class AdminEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val passwordHash: String,
    val email: String = "",
    val lastLogin: Long = System.currentTimeMillis()
) {
    fun toDomain(): AdminUser = AdminUser(
        id = id,
        name = name,
        username = username,
        passwordHash = passwordHash,
        email = email,
        lastLogin = lastLogin
    )

    companion object {
        fun fromDomain(admin: AdminUser): AdminEntity = AdminEntity(
            id = admin.id,
            name = admin.name,
            username = admin.username,
            passwordHash = admin.passwordHash,
            email = admin.email,
            lastLogin = admin.lastLogin
        )
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val type: String, // BILL, PAYMENT
    val itemId: String? = null,
    val itemName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "Kg",
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val paymentMethod: String = "Cash",
    val billNumber: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val balanceTypeAfter: String = BalanceType.RECEIVABLE.name,
    val recordedBy: String = "Admin"
) {
    fun toDomain(): TransactionRecord = TransactionRecord(
        id = id,
        customerId = customerId,
        customerName = customerName,
        type = try { TransactionType.valueOf(type) } catch (_: Exception) { TransactionType.BILL },
        itemId = itemId,
        itemName = itemName,
        quantity = quantity,
        unit = unit,
        rate = rate,
        amount = amount,
        paymentMethod = paymentMethod,
        billNumber = billNumber,
        date = date,
        timestamp = timestamp,
        notes = notes,
        balanceBefore = balanceBefore,
        balanceAfter = balanceAfter,
        balanceTypeAfter = try { BalanceType.valueOf(balanceTypeAfter) } catch (_: Exception) { BalanceType.RECEIVABLE },
        recordedBy = recordedBy
    )

    companion object {
        fun fromDomain(record: TransactionRecord): TransactionEntity = TransactionEntity(
            id = record.id,
            customerId = record.customerId,
            customerName = record.customerName,
            type = record.type.name,
            itemId = record.itemId,
            itemName = record.itemName,
            quantity = record.quantity,
            unit = record.unit,
            rate = record.rate,
            amount = record.amount,
            paymentMethod = record.paymentMethod,
            billNumber = record.billNumber,
            date = record.date,
            timestamp = record.timestamp,
            notes = record.notes,
            balanceBefore = record.balanceBefore,
            balanceAfter = record.balanceAfter,
            balanceTypeAfter = record.balanceTypeAfter.name,
            recordedBy = record.recordedBy
        )
    }
}
