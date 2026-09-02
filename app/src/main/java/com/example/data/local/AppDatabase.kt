package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomerEntity::class,
        MarketItemEntity::class,
        MarketRatesEntity::class,
        RateHistoryEntity::class,
        NotificationEntity::class,
        AdminEntity::class,
        TransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun marketItemDao(): MarketItemDao
    abstract fun marketRatesDao(): MarketRatesDao
    abstract fun rateHistoryDao(): RateHistoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun adminDao(): AdminDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mujahid_accounts_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
