package com.example.data.repository

import android.content.Context
import com.example.data.local.AdminEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CustomerEntity
import com.example.data.local.MarketItemEntity
import com.example.data.local.MarketRatesEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.RateHistoryEntity
import com.example.data.local.TransactionEntity
import com.example.model.AdminUser
import com.example.model.AppNotification
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.model.MarketItem
import com.example.model.MarketRates
import com.example.model.NotificationType
import com.example.model.RateHistoryEntry
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.util.FormatUtils
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MujahidRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val customerDao = database.customerDao()
    private val marketItemDao = database.marketItemDao()
    private val marketRatesDao = database.marketRatesDao()
    private val rateHistoryDao = database.rateHistoryDao()
    private val notificationDao = database.notificationDao()
    private val adminDao = database.adminDao()
    private val transactionDao = database.transactionDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Seed Master Admin if not present
        if (adminDao.getAdminCount() == 0) {
            val masterAdmin = AdminUser(
                id = "admin_master_1",
                name = "Mujahid Accounts Admin",
                username = "admin",
                passwordHash = SecurityUtils.hashPassword("admin123"),
                email = "admin@mujahidaccounts.com"
            )
            adminDao.insertAdmin(AdminEntity.fromDomain(masterAdmin))
        }

        // Seed Market Items if not present
        if (marketItemDao.getActiveItemCount() == 0) {
            val defaultItems = listOf(
                MarketItem(id = "item_1", name = "Item 1", currentRate = 295.0, previousRate = 292.0, orderIndex = 1),
                MarketItem(id = "item_2", name = "Item 2", currentRate = 300.0, previousRate = 298.0, orderIndex = 2),
                MarketItem(id = "item_3", name = "Item 3", currentRate = 305.0, previousRate = 303.0, orderIndex = 3),
                MarketItem(id = "item_4", name = "Item 4", currentRate = 310.0, previousRate = 308.0, orderIndex = 4)
            )
            marketItemDao.insertItems(defaultItems.map { MarketItemEntity.fromDomain(it) })
        }

        // Seed Initial Market Rates if not present
        if (marketRatesDao.getCurrentRates() == null) {
            val initialRates = MarketRates(
                id = "current",
                date = FormatUtils.formatDateOnly(),
                item1 = 295.0,
                item2 = 300.0,
                item3 = 305.0,
                item4 = 310.0,
                isMarketOpen = true,
                previousItem1 = 292.0,
                previousItem2 = 298.0,
                previousItem3 = 303.0,
                previousItem4 = 308.0,
                updatedTime = System.currentTimeMillis(),
                updatedBy = "Admin"
            )
            marketRatesDao.setMarketRates(MarketRatesEntity.fromDomain(initialRates))

            // Seed initial history entries for clean startup display
            val history1 = RateHistoryEntry(
                id = UUID.randomUUID().toString(),
                date = FormatUtils.formatDateOnly(System.currentTimeMillis()),
                timestamp = System.currentTimeMillis(),
                item1 = 295.0,
                item2 = 300.0,
                item3 = 305.0,
                item4 = 310.0,
                itemsSummary = "Item 1: 295.0 | Item 2: 300.0 | Item 3: 305.0 | Item 4: 310.0",
                isMarketOpen = true,
                note = "Daily morning rate published"
            )
            val history2 = RateHistoryEntry(
                id = UUID.randomUUID().toString(),
                date = FormatUtils.formatDateOnly(System.currentTimeMillis() - 86400000L),
                timestamp = System.currentTimeMillis() - 86400000L,
                item1 = 292.0,
                item2 = 298.0,
                item3 = 303.0,
                item4 = 308.0,
                itemsSummary = "Item 1: 292.0 | Item 2: 298.0 | Item 3: 303.0 | Item 4: 308.0",
                isMarketOpen = true,
                note = "Standard closing rate"
            )
            rateHistoryDao.insertHistoryList(listOf(history1, history2).map { RateHistoryEntity.fromDomain(it) })
        }

        // Seed Sample Customer if empty for instant testing
        if (customerDao.getCustomerCount() == 0) {
            val demoCustomer1 = Customer(
                id = "cust_demo_1",
                name = "Tariq Mahmood",
                username = "tariq",
                passwordHash = SecurityUtils.hashPassword("tariq123"),
                phone = "0300-1234567",
                balance = 45000.0,
                balanceType = BalanceType.RECEIVABLE,
                isActive = true,
                hasCustomRates = false
            )
            val demoCustomer2 = Customer(
                id = "cust_demo_2",
                name = "Ali Hassan",
                username = "ali",
                passwordHash = SecurityUtils.hashPassword("ali123"),
                phone = "0321-9876543",
                balance = 28500.0,
                balanceType = BalanceType.PAYABLE,
                isActive = true,
                hasCustomRates = true,
                customRatesMap = mapOf("item_1" to 290.0, "item_2" to 295.0)
            )
            customerDao.insertCustomer(CustomerEntity.fromDomain(demoCustomer1))
            customerDao.insertCustomer(CustomerEntity.fromDomain(demoCustomer2))
        }

        // Seed Sample Transactions if empty
        if (transactionDao.getTransactionCount() == 0) {
            val now = System.currentTimeMillis()
            val sampleTx1 = TransactionEntity(
                id = "tx_seed_1",
                customerId = "cust_demo_1",
                customerName = "Tariq Mahmood",
                type = TransactionType.BILL.name,
                itemId = "item_1",
                itemName = "Item 1 (Cotton)",
                quantity = 50.0,
                unit = "Bags (بورے)",
                rate = 295.0,
                amount = 14750.0,
                paymentMethod = "Credit",
                billNumber = "BILL-1001",
                date = FormatUtils.formatDateOnly(now - 172800000L),
                timestamp = now - 172800000L,
                notes = "50 Bags Maal Purchase from customer",
                balanceBefore = 30250.0,
                balanceAfter = 45000.0,
                balanceTypeAfter = BalanceType.RECEIVABLE.name,
                recordedBy = "Admin"
            )
            val sampleTx2 = TransactionEntity(
                id = "tx_seed_2",
                customerId = "cust_demo_2",
                customerName = "Ali Hassan",
                type = TransactionType.PAYMENT.name,
                itemId = null,
                itemName = "",
                quantity = 0.0,
                unit = "",
                rate = 0.0,
                amount = 15000.0,
                paymentMethod = "Cash",
                billNumber = "",
                date = FormatUtils.formatDateOnly(now - 86400000L),
                timestamp = now - 86400000L,
                notes = "Cash Adaigi / payment given to Ali Hassan",
                balanceBefore = 13500.0,
                balanceAfter = 28500.0,
                balanceTypeAfter = BalanceType.PAYABLE.name,
                recordedBy = "Admin"
            )
            transactionDao.insertTransactions(listOf(sampleTx1, sampleTx2))
        }
    }

    // ==================== AUTHENTICATION ====================

    suspend fun authenticateAdmin(username: String, plainPass: String): AdminUser? = withContext(Dispatchers.IO) {
        val admin = adminDao.getAdminByUsername(username.trim()) ?: return@withContext null
        if (SecurityUtils.verifyPassword(plainPass, admin.passwordHash)) {
            admin.toDomain()
        } else {
            null
        }
    }

    suspend fun authenticateCustomer(username: String, plainPass: String): Result<Customer> = withContext(Dispatchers.IO) {
        val customerEntity = customerDao.getCustomerByUsername(username.trim().lowercase())
            ?: return@withContext Result.failure(Exception("Customer username not found"))

        if (!customerEntity.isActive) {
            return@withContext Result.failure(Exception("Your account is deactivated by Admin. Please contact office."))
        }

        if (SecurityUtils.verifyPassword(plainPass, customerEntity.passwordHash)) {
            Result.success(customerEntity.toDomain())
        } else {
            Result.failure(Exception("Invalid password. Please check your credentials."))
        }
    }

    suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)?.toDomain()
    }

    fun getCustomerByIdFlow(id: String): Flow<Customer?> {
        return customerDao.getCustomerByIdFlow(id).map { it?.toDomain() }
    }

    // ==================== ITEM MANAGEMENT (ADMIN ONLY) ====================

    fun getAllActiveItemsFlow(): Flow<List<MarketItem>> {
        return marketItemDao.getAllActiveItemsFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAllActiveItems(): List<MarketItem> = withContext(Dispatchers.IO) {
        marketItemDao.getAllActiveItems().map { it.toDomain() }
    }

    suspend fun addItem(
        name: String,
        initialRate: Double,
        updatedBy: String = "Admin"
    ): Result<MarketItem> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("Item name cannot be empty"))
        }
        val existingItems = marketItemDao.getAllActiveItems()
        if (existingItems.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return@withContext Result.failure(Exception("An item with name '$trimmed' already exists"))
        }

        val newItemId = "item_${System.currentTimeMillis()}_${(100..999).random()}"
        val newItem = MarketItem(
            id = newItemId,
            name = trimmed,
            currentRate = initialRate,
            previousRate = initialRate,
            orderIndex = existingItems.size + 1,
            isDeleted = false,
            updatedAt = System.currentTimeMillis()
        )
        marketItemDao.insertItem(MarketItemEntity.fromDomain(newItem))

        // Create log & notification
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = "New Item Added",
            message = "New item '$trimmed' added with rate ${FormatUtils.formatPkr(initialRate)}.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.ITEM_MANAGEMENT
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        // Add history log
        recordRateHistorySnapshot("Added new item: $trimmed (Rate: ${FormatUtils.formatPkr(initialRate)})", updatedBy)

        Result.success(newItem)
    }

    suspend fun editItemName(
        itemId: String,
        newName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("Item name cannot be empty"))
        }
        val existing = marketItemDao.getItemById(itemId)
            ?: return@withContext Result.failure(Exception("Item not found"))

        val oldName = existing.name
        marketItemDao.updateItemName(itemId, trimmed, System.currentTimeMillis())

        // Broadcast notification
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = "Item Renamed",
            message = "Item '$oldName' has been renamed to '$trimmed'.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.ITEM_MANAGEMENT
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    suspend fun removeItem(
        itemId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = marketItemDao.getItemById(itemId)
            ?: return@withContext Result.failure(Exception("Item not found"))

        val activeCount = marketItemDao.getActiveItemCount()
        if (activeCount <= 1) {
            return@withContext Result.failure(Exception("At least one item must remain in the market list."))
        }

        marketItemDao.softDeleteItem(itemId, System.currentTimeMillis())

        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = "Item Removed",
            message = "Item '${existing.name}' has been removed from market listings.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.ITEM_MANAGEMENT
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    suspend fun updateItemRate(
        itemId: String,
        newRate: Double,
        updatedBy: String = "Admin"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = marketItemDao.getItemById(itemId)
            ?: return@withContext Result.failure(Exception("Item not found"))

        val prevRate = existing.currentRate
        marketItemDao.updateItemRate(itemId, newRate, prevRate, System.currentTimeMillis())

        // Update legacy table if it's one of the first 4 items for backward safety
        syncLegacyMarketRates()

        recordRateHistorySnapshot("${existing.name} rate updated to ${FormatUtils.formatPkr(newRate)}", updatedBy)

        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = "Rate Updated: ${existing.name}",
            message = "${existing.name} rate updated to ${FormatUtils.formatPkr(newRate)} (Previous: ${FormatUtils.formatPkr(prevRate)}).",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.RATE_UPDATE
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    suspend fun updateAllItemRates(
        ratesMap: Map<String, Double>, // itemId -> newRate
        updatedBy: String = "Admin"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val activeItems = marketItemDao.getAllActiveItems()
        for (item in activeItems) {
            val newRate = ratesMap[item.id]
            if (newRate != null) {
                marketItemDao.updateItemRate(item.id, newRate, item.currentRate, System.currentTimeMillis())
            }
        }

        syncLegacyMarketRates()
        recordRateHistorySnapshot("Daily Market Rates published for all items", updatedBy)

        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = "Today's Market Rates Published",
            message = "Daily market rates have been updated for all items.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.RATE_UPDATE
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    private suspend fun syncLegacyMarketRates() {
        val items = marketItemDao.getAllActiveItems()
        val current = marketRatesDao.getCurrentRates()
        val isMarketOpen = current?.isMarketOpen ?: true

        val r1 = items.getOrNull(0)?.currentRate ?: current?.item1 ?: 0.0
        val p1 = items.getOrNull(0)?.previousRate ?: current?.previousItem1 ?: r1
        val r2 = items.getOrNull(1)?.currentRate ?: current?.item2 ?: 0.0
        val p2 = items.getOrNull(1)?.previousRate ?: current?.previousItem2 ?: r2
        val r3 = items.getOrNull(2)?.currentRate ?: current?.item3 ?: 0.0
        val p3 = items.getOrNull(2)?.previousRate ?: current?.previousItem3 ?: r3
        val r4 = items.getOrNull(3)?.currentRate ?: current?.item4 ?: 0.0
        val p4 = items.getOrNull(3)?.previousRate ?: current?.previousItem4 ?: r4

        val updated = MarketRatesEntity(
            id = "current",
            date = FormatUtils.formatDateOnly(),
            item1 = r1,
            item2 = r2,
            item3 = r3,
            item4 = r4,
            previousItem1 = p1,
            previousItem2 = p2,
            previousItem3 = p3,
            previousItem4 = p4,
            isMarketOpen = isMarketOpen,
            updatedTime = System.currentTimeMillis(),
            updatedBy = "Admin"
        )
        marketRatesDao.setMarketRates(updated)
    }

    private suspend fun recordRateHistorySnapshot(note: String, updatedBy: String) {
        val items = marketItemDao.getAllActiveItems()
        val summary = items.joinToString(" | ") { "${it.name}: ${FormatUtils.formatPkr(it.currentRate)}" }
        val isMarketOpen = marketRatesDao.getCurrentRates()?.isMarketOpen ?: true

        val history = RateHistoryEntry(
            id = UUID.randomUUID().toString(),
            date = FormatUtils.formatDateOnly(),
            timestamp = System.currentTimeMillis(),
            item1 = items.getOrNull(0)?.currentRate ?: 0.0,
            item2 = items.getOrNull(1)?.currentRate ?: 0.0,
            item3 = items.getOrNull(2)?.currentRate ?: 0.0,
            item4 = items.getOrNull(3)?.currentRate ?: 0.0,
            itemsSummary = summary,
            isMarketOpen = isMarketOpen,
            updatedBy = updatedBy,
            note = note
        )
        rateHistoryDao.insertHistory(RateHistoryEntity.fromDomain(history))
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    fun getAllCustomersFlow(): Flow<List<Customer>> {
        return customerDao.getAllCustomersFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun addCustomer(
        name: String,
        username: String,
        plainPass: String,
        phone: String,
        balance: Double,
        balanceType: BalanceType,
        hasCustomRates: Boolean = false,
        customRateItem1: Double? = null,
        customRateItem2: Double? = null,
        customRateItem3: Double? = null,
        customRateItem4: Double? = null,
        customRatesMap: Map<String, Double> = emptyMap()
    ): Result<Customer> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().lowercase()
        if (customerDao.getCustomerByUsername(cleanUsername) != null) {
            return@withContext Result.failure(Exception("Username '$cleanUsername' already exists. Please choose a unique username."))
        }

        val newCustomer = Customer(
            id = "cust_${System.currentTimeMillis()}_${(1000..9999).random()}",
            name = name.trim(),
            username = cleanUsername,
            passwordHash = SecurityUtils.hashPassword(plainPass),
            phone = phone.trim(),
            balance = balance,
            balanceType = balanceType,
            isActive = true,
            hasCustomRates = hasCustomRates,
            customRateItem1 = customRateItem1,
            customRateItem2 = customRateItem2,
            customRateItem3 = customRateItem3,
            customRateItem4 = customRateItem4,
            customRatesMap = customRatesMap,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        customerDao.insertCustomer(CustomerEntity.fromDomain(newCustomer))

        // Also create welcome notification for the customer
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = newCustomer.id,
            title = "Welcome to Mujahid Accounts",
            message = "Your account for ${newCustomer.name} has been created successfully. Track your balance and daily market rates here.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.SYSTEM_ALERT
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(newCustomer)
    }

    suspend fun updateCustomer(customer: Customer, newPlainPassword: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = customerDao.getCustomerById(customer.id)
            ?: return@withContext Result.failure(Exception("Customer not found"))

        val finalPasswordHash = if (!newPlainPassword.isNullOrBlank()) {
            SecurityUtils.hashPassword(newPlainPassword)
        } else {
            existing.passwordHash
        }

        val updated = customer.copy(
            passwordHash = finalPasswordHash,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(CustomerEntity.fromDomain(updated))
        Result.success(Unit)
    }

    suspend fun updateCustomerBalance(
        customerId: String,
        newBalance: Double,
        newBalanceType: BalanceType,
        note: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = customerDao.getCustomerById(customerId)
            ?: return@withContext Result.failure(Exception("Customer not found"))

        val updated = existing.copy(
            balance = newBalance,
            balanceType = newBalanceType.name,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updated)

        // Create balance update notification
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            title = "Balance Updated",
            message = "Your balance is now ${FormatUtils.formatPkr(newBalance)} (${newBalanceType.name}). ${if (note.isNotBlank()) "Note: $note" else ""}",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.BALANCE_UPDATE
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    suspend fun toggleCustomerActiveStatus(customerId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val existing = customerDao.getCustomerById(customerId)
            ?: return@withContext Result.failure(Exception("Customer not found"))
        val newStatus = !existing.isActive
        val updated = existing.copy(isActive = newStatus, updatedAt = System.currentTimeMillis())
        customerDao.updateCustomer(updated)
        Result.success(newStatus)
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        customerDao.deleteCustomerById(customerId)
        Result.success(Unit)
    }

    // ==================== DAILY MARKET RATES (GLOBAL STATUS) ====================

    fun getCurrentRatesFlow(): Flow<MarketRates?> {
        return marketRatesDao.getCurrentRatesFlow().map { it?.toDomain() }
    }

    suspend fun getCurrentRates(): MarketRates? = withContext(Dispatchers.IO) {
        marketRatesDao.getCurrentRates()?.toDomain()
    }

    suspend fun setMarketStatus(isOpen: Boolean, updatedBy: String = "Admin"): Result<Unit> = withContext(Dispatchers.IO) {
        val current = getCurrentRates() ?: MarketRates(date = FormatUtils.formatDateOnly())
        val updated = current.copy(
            isMarketOpen = isOpen,
            updatedTime = System.currentTimeMillis(),
            updatedBy = updatedBy
        )
        marketRatesDao.setMarketRates(MarketRatesEntity.fromDomain(updated))

        // Create history log for market open/close
        recordRateHistorySnapshot(if (isOpen) "Market Opened" else "Market Closed", updatedBy)

        // Broadcast alert
        val statusText = if (isOpen) "Market is now OPEN." else "Market is CLOSED today."
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = if (isOpen) "🟢 Market Open" else "🔴 Market Closed",
            message = statusText,
            timestamp = System.currentTimeMillis(),
            type = NotificationType.MARKET_STATUS
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(Unit)
    }

    // ==================== RATE HISTORY ====================

    fun getAllRateHistoryFlow(): Flow<List<RateHistoryEntry>> {
        return rateHistoryDao.getAllHistoryFlow().map { list -> list.map { it.toDomain() } }
    }

    // ==================== NOTIFICATIONS ====================

    fun getNotificationsForCustomerFlow(customerId: String): Flow<List<AppNotification>> {
        return notificationDao.getNotificationsForCustomerFlow(customerId).map { list -> list.map { it.toDomain() } }
    }

    fun getAllNotificationsFlow(): Flow<List<AppNotification>> {
        return notificationDao.getAllNotificationsFlow().map { list -> list.map { it.toDomain() } }
    }

    suspend fun markNotificationAsRead(id: String) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun sendBroadcastNotification(title: String, message: String) = withContext(Dispatchers.IO) {
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = null,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = NotificationType.SYSTEM_ALERT
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))
    }

    // ==================== TRANSACTIONS (BILLS & PAYMENTS / KHATA) ====================

    fun getAllTransactionsFlow(): Flow<List<TransactionRecord>> {
        return transactionDao.getAllTransactionsFlow().map { list -> list.map { it.toDomain() } }
    }

    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<TransactionRecord>> {
        return transactionDao.getTransactionsForCustomerFlow(customerId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addBillPurchase(
        customerId: String,
        itemId: String?,
        itemName: String,
        quantity: Double,
        unit: String,
        rate: Double,
        totalAmount: Double,
        billNumber: String,
        notes: String,
        date: String = FormatUtils.formatDateOnly(),
        recordedBy: String = "Admin"
    ): Result<TransactionRecord> = withContext(Dispatchers.IO) {
        val customerEntity = customerDao.getCustomerById(customerId)
            ?: return@withContext Result.failure(Exception("Customer not found"))

        val currentBalance = customerEntity.balance
        val currentType = try { BalanceType.valueOf(customerEntity.balanceType) } catch (_: Exception) { BalanceType.RECEIVABLE }

        // When admin buys goods (Bill) from customer, admin owes this amount to customer.
        // In customer terms: Receivable increases (+), Payable decreases.
        val signedCurrent = if (currentType == BalanceType.RECEIVABLE) currentBalance else -currentBalance
        val signedNew = signedCurrent + totalAmount
        val newBalance = if (signedNew >= 0) signedNew else -signedNew
        val newType = if (signedNew >= 0) BalanceType.RECEIVABLE else BalanceType.PAYABLE

        val txId = "bill_${System.currentTimeMillis()}_${(100..999).random()}"
        val finalBillNo = if (billNumber.isNotBlank()) billNumber.trim() else "BILL-${System.currentTimeMillis().toString().takeLast(4)}"

        val transaction = TransactionRecord(
            id = txId,
            customerId = customerId,
            customerName = customerEntity.name,
            type = TransactionType.BILL,
            itemId = itemId,
            itemName = itemName.trim(),
            quantity = quantity,
            unit = unit.trim(),
            rate = rate,
            amount = totalAmount,
            paymentMethod = "Credit (Udhaar)",
            billNumber = finalBillNo,
            date = if (date.isNotBlank()) date.trim() else FormatUtils.formatDateOnly(),
            timestamp = System.currentTimeMillis(),
            notes = notes.trim(),
            balanceBefore = currentBalance,
            balanceAfter = newBalance,
            balanceTypeAfter = newType,
            recordedBy = recordedBy
        )

        // Save transaction
        transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))

        // Update customer balance
        val updatedCustomer = customerEntity.copy(
            balance = newBalance,
            balanceType = newType.name,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updatedCustomer)

        // Notify customer
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            title = "New Bill: ${FormatUtils.formatPkr(totalAmount)}",
            message = "Maal Purchase of ${itemName} (${quantity} ${unit}) recorded. New Balance: ${FormatUtils.formatPkr(newBalance)} (${newType.name}).",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.BALANCE_UPDATE
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(transaction)
    }

    suspend fun addPaymentGiven(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        referenceNo: String,
        notes: String,
        date: String = FormatUtils.formatDateOnly(),
        recordedBy: String = "Admin"
    ): Result<TransactionRecord> = withContext(Dispatchers.IO) {
        val customerEntity = customerDao.getCustomerById(customerId)
            ?: return@withContext Result.failure(Exception("Customer not found"))

        if (amount <= 0) {
            return@withContext Result.failure(Exception("Payment amount must be greater than 0"))
        }

        val currentBalance = customerEntity.balance
        val currentType = try { BalanceType.valueOf(customerEntity.balanceType) } catch (_: Exception) { BalanceType.RECEIVABLE }

        // When admin gives payment to customer, customer's receivable reduces (-) or customer becomes payable.
        val signedCurrent = if (currentType == BalanceType.RECEIVABLE) currentBalance else -currentBalance
        val signedNew = signedCurrent - amount
        val newBalance = if (signedNew >= 0) signedNew else -signedNew
        val newType = if (signedNew >= 0) BalanceType.RECEIVABLE else BalanceType.PAYABLE

        val txId = "pay_${System.currentTimeMillis()}_${(100..999).random()}"
        val transaction = TransactionRecord(
            id = txId,
            customerId = customerId,
            customerName = customerEntity.name,
            type = TransactionType.PAYMENT,
            itemId = null,
            itemName = "Payment (Adaigi)",
            quantity = 0.0,
            unit = "",
            rate = 0.0,
            amount = amount,
            paymentMethod = if (paymentMethod.isNotBlank()) paymentMethod.trim() else "Cash",
            billNumber = if (referenceNo.isNotBlank()) referenceNo.trim() else "PAY-${System.currentTimeMillis().toString().takeLast(4)}",
            date = if (date.isNotBlank()) date.trim() else FormatUtils.formatDateOnly(),
            timestamp = System.currentTimeMillis(),
            notes = notes.trim(),
            balanceBefore = currentBalance,
            balanceAfter = newBalance,
            balanceTypeAfter = newType,
            recordedBy = recordedBy
        )

        // Save transaction
        transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))

        // Update customer balance
        val updatedCustomer = customerEntity.copy(
            balance = newBalance,
            balanceType = newType.name,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updatedCustomer)

        // Notify customer
        val notif = AppNotification(
            id = UUID.randomUUID().toString(),
            customerId = customerId,
            title = "Payment Received: ${FormatUtils.formatPkr(amount)}",
            message = "Payment of ${FormatUtils.formatPkr(amount)} received via ${transaction.paymentMethod}. Remaining Balance: ${FormatUtils.formatPkr(newBalance)} (${newType.name}).",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.BALANCE_UPDATE
        )
        notificationDao.insertNotification(NotificationEntity.fromDomain(notif))

        Result.success(transaction)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionById(transactionId)
            ?: return@withContext Result.failure(Exception("Transaction not found"))

        // Revert balance impact on customer
        val customerEntity = customerDao.getCustomerById(tx.customerId)
        if (customerEntity != null) {
            val currentBalance = customerEntity.balance
            val currentType = try { BalanceType.valueOf(customerEntity.balanceType) } catch (_: Exception) { BalanceType.RECEIVABLE }
            val signedCurrent = if (currentType == BalanceType.RECEIVABLE) currentBalance else -currentBalance

            // If it was a BILL (+amount), deleting it means subtract amount.
            // If it was a PAYMENT (-amount), deleting it means add amount.
            val signedReverted = if (tx.type == TransactionType.BILL.name) {
                signedCurrent - tx.amount
            } else {
                signedCurrent + tx.amount
            }

            val newBalance = if (signedReverted >= 0) signedReverted else -signedReverted
            val newType = if (signedReverted >= 0) BalanceType.RECEIVABLE else BalanceType.PAYABLE

            customerDao.updateCustomer(customerEntity.copy(
                balance = newBalance,
                balanceType = newType.name,
                updatedAt = System.currentTimeMillis()
            ))
        }

        transactionDao.deleteTransactionById(transactionId)
        Result.success(Unit)
    }
}
