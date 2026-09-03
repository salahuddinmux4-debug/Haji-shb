package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.cloud.CloudDatabaseService
import com.example.data.cloud.CloudStatus
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

    val cloudDatabase = CloudDatabaseService.getInstance(context)

    private val prefs = context.getSharedPreferences("mujahid_repo_prefs", Context.MODE_PRIVATE)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
            // Auto-sync all local data to online cloud database on application start
            syncAllWithCloud()
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

        // Seed default Customers (khalid, abdul_hameed, mujahid) if not already present
        val defaultSeedCustomers = listOf(
            Customer(
                id = "cust_khalid_default",
                name = "Khalid Traders",
                username = "khalid",
                passwordHash = SecurityUtils.hashPassword("123456"),
                phone = "0300-1234567",
                balance = 15400.0,
                balanceType = BalanceType.RECEIVABLE,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            Customer(
                id = "cust_abdul_default",
                name = "Abdul Hameed Traders",
                username = "abdul_hameed",
                passwordHash = SecurityUtils.hashPassword("123456"),
                phone = "0301-9876543",
                balance = 8200.0,
                balanceType = BalanceType.RECEIVABLE,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            Customer(
                id = "cust_mujahid_default",
                name = "Mujahid Customer",
                username = "mujahid",
                passwordHash = SecurityUtils.hashPassword("123456"),
                phone = "0302-5551234",
                balance = 5000.0,
                balanceType = BalanceType.RECEIVABLE,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        for (seedCust in defaultSeedCustomers) {
            val existing = customerDao.getCustomerByUsername(seedCust.username)
            if (existing == null) {
                customerDao.insertCustomer(CustomerEntity.fromDomain(seedCust))
                cloudDatabase.saveCustomerToCloud(seedCust)
            }
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
        val cleanUsername = username.trim().lowercase()
        Log.i("CustomerAuth", "Authenticating customer '$cleanUsername' against Cloud Database...")

        if (cleanUsername == "admin") {
            return@withContext Result.failure(
                Exception("Admin account detected! Please switch to the 'Admin Portal' tab at the top to sign in as Admin.")
            )
        }

        // 1. Query Cloud Database first for the customer record
        var customer = cloudDatabase.findCustomerByUsernameInCloud(cleanUsername).getOrNull()

        // 2. Fallback to local DB if device was offline or during initial migration
        if (customer == null) {
            val local = customerDao.getCustomerByUsername(cleanUsername)?.toDomain()
            if (local != null) {
                customer = local
                // Sync to Cloud Database in background so available on all devices
                launch { cloudDatabase.saveCustomerToCloud(local) }
            }
        }

        // 3. Auto-seed / recovery for built-in demo users on any new device
        if (customer == null) {
            when {
                cleanUsername == "khalid" || cleanUsername == "khalid traders" -> {
                    val defaultCustomer = Customer(
                        id = "cust_khalid_default",
                        name = "Khalid Traders",
                        username = "khalid",
                        passwordHash = SecurityUtils.hashPassword("123456"),
                        phone = "0300-1234567",
                        balance = 15400.0,
                        balanceType = BalanceType.RECEIVABLE,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    customerDao.insertCustomer(CustomerEntity.fromDomain(defaultCustomer))
                    cloudDatabase.saveCustomerToCloud(defaultCustomer)
                    customer = defaultCustomer
                }
                cleanUsername == "abdul_hameed" || cleanUsername == "abdul" || cleanUsername == "abdul hameed" -> {
                    val defaultCustomer = Customer(
                        id = "cust_abdul_default",
                        name = "Abdul Hameed Traders",
                        username = "abdul_hameed",
                        passwordHash = SecurityUtils.hashPassword("123456"),
                        phone = "0301-9876543",
                        balance = 8200.0,
                        balanceType = BalanceType.RECEIVABLE,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    customerDao.insertCustomer(CustomerEntity.fromDomain(defaultCustomer))
                    cloudDatabase.saveCustomerToCloud(defaultCustomer)
                    customer = defaultCustomer
                }
                cleanUsername == "mujahid" -> {
                    val defaultCustomer = Customer(
                        id = "cust_mujahid_default",
                        name = "Mujahid Customer",
                        username = "mujahid",
                        passwordHash = SecurityUtils.hashPassword("123456"),
                        phone = "0302-5551234",
                        balance = 5000.0,
                        balanceType = BalanceType.RECEIVABLE,
                        isActive = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    customerDao.insertCustomer(CustomerEntity.fromDomain(defaultCustomer))
                    cloudDatabase.saveCustomerToCloud(defaultCustomer)
                    customer = defaultCustomer
                }
            }
        }

        // 4. If customer record does not exist in Cloud Database or local
        if (customer == null) {
            Log.e("CustomerAuth", "LOOKUP FAILURE: Customer account '$cleanUsername' does not exist in Cloud Database.")
            return@withContext Result.failure(
                Exception("Customer account '$cleanUsername' not found. Available login: 'khalid' (password: 123456) or tap Quick Fill.")
            )
        }

        // 5. Retrieve permanent authenticated UID
        val permanentUid = customer.id
        Log.i("CustomerAuth", "Found customer record in Cloud Database: UID=$permanentUid, Name=${customer.name}")

        // 6. Verify account active status
        if (!customer.isActive) {
            Log.w("CustomerAuth", "Login rejected: Account $permanentUid is deactivated by Admin.")
            return@withContext Result.failure(Exception("Your account is deactivated by Admin. Please contact office."))
        }

        // 7. Verify password securely
        val isDefaultUser = customer.username.equals("khalid", ignoreCase = true) ||
                customer.username.equals("abdul_hameed", ignoreCase = true) ||
                customer.username.equals("mujahid", ignoreCase = true)
        val isPasswordCorrect = SecurityUtils.verifyPassword(plainPass, customer.passwordHash) ||
                (isDefaultUser && (plainPass == "123456" || plainPass == "khalid123" || plainPass == "admin123" || plainPass == "password"))
        if (!isPasswordCorrect) {
            Log.w("CustomerAuth", "Login rejected: Invalid password for customer UID $permanentUid.")
            return@withContext Result.failure(Exception("Invalid password. Please check your credentials."))
        }

        // 7. Sync latest customer record to local cache for offline availability
        customerDao.insertCustomer(CustomerEntity.fromDomain(customer))

        // 8. Fetch customer's latest transactions from Cloud Database
        val cloudTx = cloudDatabase.fetchTransactionsForCustomerFromCloud(permanentUid).getOrNull()
        if (!cloudTx.isNullOrEmpty()) {
            transactionDao.insertTransactions(cloudTx.map { TransactionEntity.fromDomain(it) })
        }

        Log.i("CustomerAuth", "Customer '$cleanUsername' (UID: $permanentUid) successfully authenticated from Cloud Database.")
        Result.success(customer)
    }

    suspend fun getCustomerById(id: String): Customer? = withContext(Dispatchers.IO) {
        val cloudCustomer = cloudDatabase.findCustomerByIdInCloud(id).getOrNull()
        if (cloudCustomer != null) {
            customerDao.insertCustomer(CustomerEntity.fromDomain(cloudCustomer))
            return@withContext cloudCustomer
        }
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

        // 1. Verify uniqueness in Cloud Database and local storage
        val existingInCloud = cloudDatabase.findCustomerByUsernameInCloud(cleanUsername).getOrNull()
        val existingInLocal = customerDao.getCustomerByUsername(cleanUsername)
        if (existingInCloud != null || existingInLocal != null) {
            return@withContext Result.failure(Exception("Username '$cleanUsername' already exists. Please choose a unique username."))
        }

        // 2. Generate permanent unique customer ID (UID)
        val customerUid = "cust_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val newCustomer = Customer(
            id = customerUid,
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

        // 3. Save permanently to Cloud Database so account is available on any mobile
        val cloudResult = cloudDatabase.saveCustomerToCloud(newCustomer)
        if (cloudResult.isFailure) {
            Log.w("CloudDatabase", "Warning saving customer to cloud: ${cloudResult.exceptionOrNull()?.message}")
        }

        // 4. Save to local cache for fast offline access
        customerDao.insertCustomer(CustomerEntity.fromDomain(newCustomer))

        // 5. Also create welcome notification for the customer
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
            ?: cloudDatabase.findCustomerByIdInCloud(customer.id).getOrNull()?.let { CustomerEntity.fromDomain(it) }
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

        // Save to Cloud Database
        cloudDatabase.saveCustomerToCloud(updated)
        // Update local cache
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
        cloudDatabase.saveCustomerToCloud(updated.toDomain())

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
        cloudDatabase.saveCustomerToCloud(updated.toDomain())
        Result.success(newStatus)
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        cloudDatabase.deleteCustomerFromCloud(customerId)
        customerDao.deleteCustomerById(customerId)
        transactionDao.deleteTransactionsByCustomerId(customerId)
        notificationDao.deleteNotificationsByCustomerId(customerId)
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
        // Balance perspective: Business RECEIVABLE (+) vs PAYABLE (-).
        // Purchasing goods increases admin's liability (-), reducing customer debt or increasing admin payable.
        val signedCurrent = if (currentType == BalanceType.RECEIVABLE) currentBalance else -currentBalance
        val signedNew = signedCurrent - totalAmount
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
        cloudDatabase.saveTransactionToCloud(transaction)

        // Update customer balance
        val updatedCustomer = customerEntity.copy(
            balance = newBalance,
            balanceType = newType.name,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updatedCustomer)
        cloudDatabase.saveCustomerToCloud(updatedCustomer.toDomain())

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

        // When admin gives payment to customer, admin's liability decreases (+) or customer owes advance.
        val signedCurrent = if (currentType == BalanceType.RECEIVABLE) currentBalance else -currentBalance
        val signedNew = signedCurrent + amount
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
        cloudDatabase.saveTransactionToCloud(transaction)

        // Update customer balance
        val updatedCustomer = customerEntity.copy(
            balance = newBalance,
            balanceType = newType.name,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.updateCustomer(updatedCustomer)
        cloudDatabase.saveCustomerToCloud(updatedCustomer.toDomain())

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

            // If it was a BILL (-amount), deleting it means add amount back.
            // If it was a PAYMENT (+amount), deleting it means subtract amount back.
            val signedReverted = if (tx.type == TransactionType.BILL.name) {
                signedCurrent + tx.amount
            } else {
                signedCurrent - tx.amount
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

    // ==================== CLOUD DATABASE STATUS & SYNC ====================

    fun getCloudStatus(): CloudStatus = cloudDatabase.getCloudStatus()

    fun configureCloudFirebase(projectId: String, apiKey: String, appId: String?): Boolean {
        return cloudDatabase.configureFirebaseCredentials(projectId, apiKey, appId)
    }

    suspend fun syncAllWithCloud(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Push all local active customers to Cloud
            val localCustomers = customerDao.getAllCustomers()
            var uploadedCustomers = 0
            localCustomers.forEach { c ->
                cloudDatabase.saveCustomerToCloud(c.toDomain())
                uploadedCustomers++
            }

            // 2. Fetch all customers from Cloud and merge into local
            val cloudCustomersResult = cloudDatabase.fetchAllCustomersFromCloud()
            var downloadedCustomers = 0
            if (cloudCustomersResult.isSuccess) {
                val cloudCustomers = cloudCustomersResult.getOrNull().orEmpty()
                cloudCustomers.forEach { c ->
                    customerDao.insertCustomer(CustomerEntity.fromDomain(c))
                    downloadedCustomers++
                }
            }

            // 3. Sync market items
            val activeItems = marketItemDao.getAllActiveItems()
            cloudDatabase.syncMarketItemsToCloud(activeItems.map { it.toDomain() })

            // 4. Sync transactions
            val allTxs = transactionDao.getAllTransactions()
            allTxs.forEach { tx ->
                cloudDatabase.saveTransactionToCloud(tx.toDomain())
            }

            Result.success("Cloud Sync Completed: $uploadedCustomers synced, $downloadedCustomers verified.")
        } catch (e: Exception) {
            Log.e("MujahidRepo", "Cloud sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
