package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE username = :username LIMIT 1")
    suspend fun getCustomerByUsername(username: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCustomerCount(): Int
}

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items WHERE isDeleted = 0 ORDER BY orderIndex ASC, name ASC")
    fun getAllActiveItemsFlow(): Flow<List<MarketItemEntity>>

    @Query("SELECT * FROM market_items WHERE isDeleted = 0 ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllActiveItems(): List<MarketItemEntity>

    @Query("SELECT * FROM market_items ORDER BY orderIndex ASC")
    suspend fun getAllItems(): List<MarketItemEntity>

    @Query("SELECT * FROM market_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): MarketItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MarketItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<MarketItemEntity>)

    @Update
    suspend fun updateItem(item: MarketItemEntity)

    @Query("UPDATE market_items SET name = :newName, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateItemName(id: String, newName: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE market_items SET currentRate = :newRate, previousRate = :prevRate, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateItemRate(id: String, newRate: Double, prevRate: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE market_items SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteItem(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM market_items WHERE id = :id")
    suspend fun hardDeleteItem(id: String)

    @Query("SELECT COUNT(*) FROM market_items WHERE isDeleted = 0")
    suspend fun getActiveItemCount(): Int
}

@Dao
interface MarketRatesDao {
    @Query("SELECT * FROM market_rates WHERE id = 'current' LIMIT 1")
    fun getCurrentRatesFlow(): Flow<MarketRatesEntity?>

    @Query("SELECT * FROM market_rates WHERE id = 'current' LIMIT 1")
    suspend fun getCurrentRates(): MarketRatesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMarketRates(rates: MarketRatesEntity)
}

@Dao
interface RateHistoryDao {
    @Query("SELECT * FROM rate_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<RateHistoryEntity>>

    @Query("SELECT * FROM rate_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<RateHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: RateHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryList(entries: List<RateHistoryEntity>)

    @Query("DELETE FROM rate_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE customerId = :customerId OR customerId IS NULL ORDER BY timestamp DESC")
    fun getNotificationsForCustomerFlow(customerId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)
}

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins WHERE username = :username LIMIT 1")
    suspend fun getAdminByUsername(username: String): AdminEntity?

    @Query("SELECT * FROM admins WHERE id = :id")
    suspend fun getAdminById(id: String): AdminEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: AdminEntity)

    @Query("SELECT COUNT(*) FROM admins")
    suspend fun getAdminCount(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getTransactionsForCustomer(customerId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}
