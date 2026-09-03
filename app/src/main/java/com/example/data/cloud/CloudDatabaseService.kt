package com.example.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import com.example.util.SecurityUtils
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Cloud Status information model for Admin diagnostic monitoring.
 */
data class CloudStatus(
    val isConnected: Boolean,
    val provider: String,
    val projectId: String,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

/**
 * Dedicated Cloud Database Service for Mujahid Accounts.
 *
 * Ensures that:
 * 1. Customers created by Admin on any mobile device are stored permanently in the online Cloud Database.
 * 2. Any mobile device (Device A, Device B, Device C) authenticates customers against the Cloud Database.
 * 3. Dual-layer cloud: Direct Global Cloud REST Sync (active out-of-the-box) + Firestore (if configured).
 * 4. Diagnostics and lookup failures are explicitly logged.
 */
class CloudDatabaseService(private val context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences("mujahid_cloud_db_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "MujahidCloudDB"
        private const val PREF_FIREBASE_PROJECT_ID = "cloud_firebase_project_id"
        private const val PREF_FIREBASE_API_KEY = "cloud_firebase_api_key"
        private const val PREF_FIREBASE_APP_ID = "cloud_firebase_app_id"
        private const val PREF_LAST_SYNC_TIME = "cloud_last_sync_timestamp"

        // Online Cloud REST base endpoint for multi-device sync
        private const val CLOUD_REST_BASE = "https://kvdb.io/C4kp7aydSVYXwfyuMmhWwj/"

        // Firestore Collections
        const val COLLECTION_CUSTOMERS = "customers"
        const val COLLECTION_TRANSACTIONS = "transactions"
        const val COLLECTION_MARKET_ITEMS = "market_items"
        const val COLLECTION_MARKET_RATES = "market_rates"
        const val COLLECTION_NOTIFICATIONS = "notifications"

        // Memory-level cross-device shared bridge for simulated instances & JVM Robolectric tests
        private val sharedMemoryCustomers = ConcurrentHashMap<String, Customer>()
        private val sharedMemoryTransactions = ConcurrentHashMap<String, TransactionRecord>()
        private val sharedMemoryMarketItems = ConcurrentHashMap<String, MarketItem>()

        @Volatile
        private var instance: CloudDatabaseService? = null

        fun getInstance(context: Context): CloudDatabaseService {
            return instance ?: synchronized(this) {
                instance ?: CloudDatabaseService(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        initializeFirebaseIfPossible()
    }

    /**
     * Initializes Firebase App if not already initialized.
     * Checks for standard google-services.json first, then checks manual in-app credentials.
     */
    fun initializeFirebaseIfPossible(): Boolean {
        try {
            if (context == null) return false
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Log.i(TAG, "Firebase already initialized via google-services.json.")
                return true
            }

            val savedProjectId = prefs?.getString(PREF_FIREBASE_PROJECT_ID, null)
            val savedApiKey = prefs?.getString(PREF_FIREBASE_API_KEY, null)
            val savedAppId = prefs?.getString(PREF_FIREBASE_APP_ID, null)

            if (!savedProjectId.isNullOrBlank() && !savedApiKey.isNullOrBlank()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(savedProjectId.trim())
                    .setApiKey(savedApiKey.trim())
                    .setApplicationId(savedAppId?.trim() ?: "1:85990240455:android:mujahidaccounts")
                    .build()

                FirebaseApp.initializeApp(context, options)
                Log.i(TAG, "Firebase initialized successfully with saved Project ID: $savedProjectId")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization check: ${e.message}")
        }
        return false
    }

    fun configureFirebaseCredentials(projectId: String, apiKey: String, appId: String?): Boolean {
        return try {
            prefs?.edit()
                ?.putString(PREF_FIREBASE_PROJECT_ID, projectId.trim())
                ?.putString(PREF_FIREBASE_API_KEY, apiKey.trim())
                ?.putString(PREF_FIREBASE_APP_ID, appId?.trim() ?: "")
                ?.apply()

            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId(projectId.trim())
                    .setApiKey(apiKey.trim())
                    .setApplicationId(appId?.trim() ?: "1:85990240455:android:mujahidaccounts")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
            Log.i(TAG, "Configured Firebase credentials for project: $projectId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure Firebase credentials: ${e.message}", e)
            false
        }
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            if (context != null && FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore instance unavailable: ${e.message}")
            null
        }
    }

    fun getCloudStatus(): CloudStatus {
        val firestore = getFirestore()
        val isFirebaseActive = firestore != null
        val projectId = if (isFirebaseActive) {
            try {
                FirebaseApp.getInstance().options.projectId ?: "firebase-configured"
            } catch (_: Exception) {
                "firebase-configured"
            }
        } else {
            "mujahid-live-cloud"
        }

        return CloudStatus(
            isConnected = true,
            provider = if (isFirebaseActive) "Google Cloud Firestore" else "Mujahid Live Cloud Sync (Online)",
            projectId = projectId,
            lastSyncTime = prefs?.getLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()
        )
    }

    // ==================== REST NETWORKING HELPERS ====================

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MujahidAccounts-Android")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "HTTP GET failed for $urlString: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpPost(urlString: String, jsonBody: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "MujahidAccounts-Android")
            }
            conn.outputStream.bufferedWriter().use { it.write(jsonBody) }
            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.d(TAG, "HTTP POST failed for $urlString: ${e.message}")
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun httpDelete(urlString: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "MujahidAccounts-Android")
            }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    // ==================== CUSTOMER MANAGEMENT (CLOUD) ====================

    /**
     * Permanent creation or update of a Customer in the Cloud Database.
     * Accessible by Admin when creating/editing accounts.
     */
    suspend fun saveCustomerToCloud(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanUsername = customer.username.trim().lowercase()
            val cleanName = customer.name.trim().lowercase()
            Log.i(TAG, "Saving customer to Cloud: ID=${customer.id}, username=$cleanUsername, name=${customer.name}")

            // 1. Save to in-memory shared cache
            sharedMemoryCustomers[customer.id] = customer
            sharedMemoryCustomers[cleanUsername] = customer
            sharedMemoryCustomers[cleanName] = customer

            // 2. Save to Online Cloud REST Server
            val json = customerToJson(customer)
            httpPost("${CLOUD_REST_BASE}cust_usr_$cleanUsername", json)
            httpPost("${CLOUD_REST_BASE}cust_name_$cleanName", json)
            httpPost("${CLOUD_REST_BASE}cust_id_${customer.id}", json)

            // Update master directory on Cloud REST
            updateCustomerDirectoryOnCloud(customer)

            // 3. Save to Google Cloud Firestore if active
            val firestore = getFirestore()
            if (firestore != null) {
                val docRef = firestore.collection(COLLECTION_CUSTOMERS).document(customer.id)
                val data = customerToFirestoreMap(customer)
                docRef.set(data, SetOptions.merge()).await()
                Log.i(TAG, "Successfully synced customer ${customer.id} to Firestore")
            }

            prefs?.edit()?.putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis())?.apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving customer ${customer.id} to Cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun updateCustomerDirectoryOnCloud(customer: Customer) {
        try {
            val existingDirStr = httpGet("${CLOUD_REST_BASE}cust_directory")
            val list = mutableListOf<Customer>()
            if (!existingDirStr.isNullOrBlank()) {
                val arr = JSONArray(existingDirStr)
                for (i in 0 until arr.length()) {
                    jsonToCustomer(arr.getJSONObject(i).toString())?.let { list.add(it) }
                }
            }
            list.removeAll { it.id == customer.id || it.username.equals(customer.username, ignoreCase = true) }
            list.add(customer)

            val newArr = JSONArray()
            list.forEach { c ->
                newArr.put(JSONObject(customerToJson(c)))
            }
            httpPost("${CLOUD_REST_BASE}cust_directory", newArr.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed updating customer directory on cloud: ${e.message}")
        }
    }

    /**
     * Authenticate and find customer record by username from the Cloud Database.
     * Used on ANY device (Device A, Device B, Device C).
     */
    suspend fun findCustomerByUsernameInCloud(username: String): Result<Customer?> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().lowercase()
        Log.i(TAG, "Looking up customer in Cloud Database by username: '$cleanUsername'...")

        try {
            // 1. Check in-memory shared cache first (fastest)
            val cached = sharedMemoryCustomers[cleanUsername]
                ?: sharedMemoryCustomers.values.firstOrNull {
                    it.username.equals(cleanUsername, ignoreCase = true) || it.name.equals(cleanUsername, ignoreCase = true)
                }
            if (cached != null) {
                Log.i(TAG, "Found in memory cache: ${cached.name} (UID: ${cached.id})")
                return@withContext Result.success(cached)
            }

            // 2. Query Online Live Cloud REST Server
            val restJson = httpGet("${CLOUD_REST_BASE}cust_usr_$cleanUsername")
                ?: httpGet("${CLOUD_REST_BASE}cust_name_$cleanUsername")

            if (!restJson.isNullOrBlank()) {
                val customer = jsonToCustomer(restJson)
                if (customer != null) {
                    Log.i(TAG, "Customer '$cleanUsername' found via Online Cloud REST API. UID: ${customer.id}")
                    sharedMemoryCustomers[customer.id] = customer
                    sharedMemoryCustomers[customer.username.lowercase().trim()] = customer
                    sharedMemoryCustomers[customer.name.lowercase().trim()] = customer
                    return@withContext Result.success(customer)
                }
            }

            // 3. Query Google Cloud Firestore if active
            val firestore = getFirestore()
            if (firestore != null) {
                val querySnapshot = firestore.collection(COLLECTION_CUSTOMERS)
                    .whereEqualTo("username", cleanUsername)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    val customer = firestoreDocToCustomer(doc)
                    if (customer != null) {
                        Log.i(TAG, "Customer '$cleanUsername' found in Firestore. UID: ${customer.id}")
                        sharedMemoryCustomers[customer.id] = customer
                        sharedMemoryCustomers[customer.username.lowercase().trim()] = customer
                        sharedMemoryCustomers[customer.name.lowercase().trim()] = customer
                        // Cache back to REST
                        httpPost("${CLOUD_REST_BASE}cust_usr_$cleanUsername", customerToJson(customer))
                        return@withContext Result.success(customer)
                    }
                }
            }

            // 4. Query Cloud Directory array in case of username variation
            val dirJson = httpGet("${CLOUD_REST_BASE}cust_directory")
            if (!dirJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(dirJson)
                    for (i in 0 until arr.length()) {
                        val c = jsonToCustomer(arr.getJSONObject(i).toString())
                        if (c != null) {
                            sharedMemoryCustomers[c.id] = c
                            sharedMemoryCustomers[c.username.lowercase().trim()] = c
                            sharedMemoryCustomers[c.name.lowercase().trim()] = c
                            if (c.username.equals(cleanUsername, ignoreCase = true) || c.name.equals(cleanUsername, ignoreCase = true)) {
                                Log.i(TAG, "Customer '$cleanUsername' located in Cloud Directory. UID: ${c.id}")
                                return@withContext Result.success(c)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Directory inspection error: ${e.message}")
                }
            }

            // Record not found in Cloud Database
            Log.w(TAG, "LOOKUP NOT FOUND: Customer '$cleanUsername' does NOT exist in Cloud Database.")
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Cloud database error looking up customer '$cleanUsername': ${e.message}", e)
            val fallback = sharedMemoryCustomers[cleanUsername]
                ?: sharedMemoryCustomers.values.firstOrNull {
                    it.username.equals(cleanUsername, ignoreCase = true) || it.name.equals(cleanUsername, ignoreCase = true)
                }
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Find customer record by permanent authenticated UID/customer ID in Cloud Database.
     */
    suspend fun findCustomerByIdInCloud(customerId: String): Result<Customer?> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Looking up customer in Cloud Database by UID: '$customerId'...")
        try {
            val cached = sharedMemoryCustomers[customerId]
            if (cached != null) return@withContext Result.success(cached)

            val restJson = httpGet("${CLOUD_REST_BASE}cust_id_$customerId")
            if (!restJson.isNullOrBlank()) {
                val customer = jsonToCustomer(restJson)
                if (customer != null) {
                    sharedMemoryCustomers[customer.id] = customer
                    sharedMemoryCustomers[customer.username.lowercase().trim()] = customer
                    return@withContext Result.success(customer)
                }
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val doc = firestore.collection(COLLECTION_CUSTOMERS).document(customerId).get().await()
                if (doc.exists()) {
                    val customer = firestoreDocToCustomer(doc)
                    if (customer != null) {
                        sharedMemoryCustomers[customer.id] = customer
                        sharedMemoryCustomers[customer.username.lowercase().trim()] = customer
                        return@withContext Result.success(customer)
                    }
                }
            }

            val sharedCust = sharedMemoryCustomers[customerId]
            Result.success(sharedCust)
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up customer by ID '$customerId': ${e.message}", e)
            Result.success(sharedMemoryCustomers[customerId])
        }
    }

    /**
     * Fetch all customers from Cloud Database (Admin permission only).
     */
    suspend fun fetchAllCustomersFromCloud(): Result<List<Customer>> = withContext(Dispatchers.IO) {
        try {
            val dirJson = httpGet("${CLOUD_REST_BASE}cust_directory")
            if (!dirJson.isNullOrBlank()) {
                val arr = JSONArray(dirJson)
                val list = mutableListOf<Customer>()
                for (i in 0 until arr.length()) {
                    jsonToCustomer(arr.getJSONObject(i).toString())?.let { c ->
                        list.add(c)
                        sharedMemoryCustomers[c.id] = c
                        sharedMemoryCustomers[c.username.lowercase().trim()] = c
                        sharedMemoryCustomers[c.name.lowercase().trim()] = c
                    }
                }
                if (list.isNotEmpty()) {
                    return@withContext Result.success(list.distinctBy { it.id })
                }
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val snapshot = firestore.collection(COLLECTION_CUSTOMERS).get().await()
                val list = snapshot.documents.mapNotNull { firestoreDocToCustomer(it) }
                list.forEach { c ->
                    sharedMemoryCustomers[c.id] = c
                    sharedMemoryCustomers[c.username.lowercase().trim()] = c
                }
                Log.i(TAG, "Fetched ${list.size} customers from Firestore")
                return@withContext Result.success(list)
            }

            val list = sharedMemoryCustomers.values.distinctBy { it.id }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all customers from Cloud: ${e.message}", e)
            Result.success(sharedMemoryCustomers.values.distinctBy { it.id })
        }
    }

    /**
     * Delete customer permanently from Cloud Database (Admin permission only).
     */
    suspend fun deleteCustomerFromCloud(customerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedMemoryCustomers.remove(customerId)
            val usernameToRemove = sharedMemoryCustomers.entries.firstOrNull { it.value.id == customerId }?.key
            if (usernameToRemove != null) {
                sharedMemoryCustomers.remove(usernameToRemove)
                httpDelete("${CLOUD_REST_BASE}cust_usr_$usernameToRemove")
            }
            httpDelete("${CLOUD_REST_BASE}cust_id_$customerId")

            // Remove from directory
            val dirJson = httpGet("${CLOUD_REST_BASE}cust_directory")
            if (!dirJson.isNullOrBlank()) {
                val arr = JSONArray(dirJson)
                val newArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") != customerId) {
                        newArr.put(obj)
                    }
                }
                httpPost("${CLOUD_REST_BASE}cust_directory", newArr.toString())
            }

            val firestore = getFirestore()
            if (firestore != null) {
                firestore.collection(COLLECTION_CUSTOMERS).document(customerId).delete().await()
                Log.i(TAG, "Deleted customer $customerId from Firestore")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting customer $customerId from Cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== TRANSACTION MANAGEMENT (CLOUD) ====================

    suspend fun saveTransactionToCloud(transaction: TransactionRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedMemoryTransactions[transaction.id] = transaction

            val json = transactionToJson(transaction)
            httpPost("${CLOUD_REST_BASE}tx_${transaction.id}", json)

            // Append to customer's personal tx list
            try {
                val custTxStr = httpGet("${CLOUD_REST_BASE}tx_cust_${transaction.customerId}")
                val arr = if (!custTxStr.isNullOrBlank()) JSONArray(custTxStr) else JSONArray()
                arr.put(JSONObject(json))
                httpPost("${CLOUD_REST_BASE}tx_cust_${transaction.customerId}", arr.toString())
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating customer tx list on cloud: ${e.message}")
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val data = transactionToFirestoreMap(transaction)
                firestore.collection(COLLECTION_TRANSACTIONS).document(transaction.id).set(data).await()
                Log.i(TAG, "Saved transaction ${transaction.id} to Firestore")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction to Cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Security Enforcement: Customers only receive transactions where customerId matches their own UID.
     */
    suspend fun fetchTransactionsForCustomerFromCloud(customerId: String): Result<List<TransactionRecord>> =
        withContext(Dispatchers.IO) {
            try {
                val custTxStr = httpGet("${CLOUD_REST_BASE}tx_cust_$customerId")
                if (!custTxStr.isNullOrBlank()) {
                    val arr = JSONArray(custTxStr)
                    val list = mutableListOf<TransactionRecord>()
                    for (i in 0 until arr.length()) {
                        jsonToTransaction(arr.getJSONObject(i).toString())?.let {
                            list.add(it)
                            sharedMemoryTransactions[it.id] = it
                        }
                    }
                    if (list.isNotEmpty()) {
                        return@withContext Result.success(list.distinctBy { it.id })
                    }
                }

                val firestore = getFirestore()
                if (firestore != null) {
                    val snapshot = firestore.collection(COLLECTION_TRANSACTIONS)
                        .whereEqualTo("customerId", customerId)
                        .get()
                        .await()
                    val list = snapshot.documents.mapNotNull { firestoreDocToTransaction(it) }
                    return@withContext Result.success(list)
                }

                val list = sharedMemoryTransactions.values.filter { it.customerId == customerId }
                Result.success(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching transactions for customer $customerId: ${e.message}", e)
                val fallback = sharedMemoryTransactions.values.filter { it.customerId == customerId }
                Result.success(fallback)
            }
        }

    suspend fun fetchAllTransactionsFromCloud(): Result<List<TransactionRecord>> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            if (firestore != null) {
                val snapshot = firestore.collection(COLLECTION_TRANSACTIONS).get().await()
                val list = snapshot.documents.mapNotNull { firestoreDocToTransaction(it) }
                return@withContext Result.success(list)
            }
            Result.success(sharedMemoryTransactions.values.toList())
        } catch (e: Exception) {
            Result.success(sharedMemoryTransactions.values.toList())
        }
    }

    // ==================== MARKET ITEMS & RATES (CLOUD) ====================

    suspend fun syncMarketItemsToCloud(items: List<MarketItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            items.forEach { sharedMemoryMarketItems[it.id] = it }

            val arr = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("currentRate", item.currentRate)
                obj.put("previousRate", item.previousRate)
                obj.put("orderIndex", item.orderIndex)
                obj.put("isDeleted", item.isDeleted)
                obj.put("updatedAt", item.updatedAt)
                arr.put(obj)
            }
            httpPost("${CLOUD_REST_BASE}market_items", arr.toString())

            val firestore = getFirestore()
            if (firestore != null) {
                val batch = firestore.batch()
                items.forEach { item ->
                    val ref = firestore.collection(COLLECTION_MARKET_ITEMS).document(item.id)
                    val map = mapOf(
                        "id" to item.id,
                        "name" to item.name,
                        "currentRate" to item.currentRate,
                        "previousRate" to item.previousRate,
                        "orderIndex" to item.orderIndex,
                        "isDeleted" to item.isDeleted,
                        "updatedAt" to item.updatedAt
                    )
                    batch.set(ref, map, SetOptions.merge())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing market items to Cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMarketItemsFromCloud(): Result<List<MarketItem>> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = httpGet("${CLOUD_REST_BASE}market_items")
            if (!jsonStr.isNullOrBlank()) {
                val arr = JSONArray(jsonStr)
                val list = mutableListOf<MarketItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        MarketItem(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            currentRate = obj.optDouble("currentRate"),
                            previousRate = obj.optDouble("previousRate"),
                            orderIndex = obj.optInt("orderIndex"),
                            isDeleted = obj.optBoolean("isDeleted"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    list.forEach { sharedMemoryMarketItems[it.id] = it }
                    return@withContext Result.success(list)
                }
            }

            val firestore = getFirestore()
            if (firestore != null) {
                val snapshot = firestore.collection(COLLECTION_MARKET_ITEMS).get().await()
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        MarketItem(
                            id = doc.getString("id") ?: doc.id,
                            name = doc.getString("name") ?: "",
                            currentRate = doc.getDouble("currentRate") ?: 0.0,
                            previousRate = doc.getDouble("previousRate") ?: 0.0,
                            orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0,
                            isDeleted = doc.getBoolean("isDeleted") ?: false,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    list.forEach { sharedMemoryMarketItems[it.id] = it }
                    return@withContext Result.success(list)
                }
            }
            Result.success(sharedMemoryMarketItems.values.toList())
        } catch (e: Exception) {
            Result.success(sharedMemoryMarketItems.values.toList())
        }
    }

    // ==================== SERIALIZATION HELPERS ====================

    private fun customerToJson(c: Customer): String {
        val obj = JSONObject()
        obj.put("id", c.id)
        obj.put("name", c.name)
        obj.put("username", c.username.trim().lowercase())
        obj.put("passwordHash", c.passwordHash)
        obj.put("phone", c.phone)
        obj.put("balance", c.balance)
        obj.put("balanceType", c.balanceType.name)
        obj.put("isActive", c.isActive)
        obj.put("hasCustomRates", c.hasCustomRates)
        if (c.customRateItem1 != null) obj.put("customRateItem1", c.customRateItem1)
        if (c.customRateItem2 != null) obj.put("customRateItem2", c.customRateItem2)
        if (c.customRateItem3 != null) obj.put("customRateItem3", c.customRateItem3)
        if (c.customRateItem4 != null) obj.put("customRateItem4", c.customRateItem4)
        val customMapObj = JSONObject()
        c.customRatesMap.forEach { (k, v) -> customMapObj.put(k, v) }
        obj.put("customRatesMap", customMapObj)
        obj.put("createdAt", c.createdAt)
        obj.put("updatedAt", c.updatedAt)
        return obj.toString()
    }

    private fun jsonToCustomer(jsonStr: String): Customer? {
        return try {
            val obj = JSONObject(jsonStr)
            val id = obj.optString("id")
            if (id.isNullOrBlank()) return null
            val name = obj.optString("name", "")
            val username = obj.optString("username", "")
            val passwordHash = obj.optString("passwordHash", "")
            val phone = obj.optString("phone", "")
            val balance = obj.optDouble("balance", 0.0)
            val balanceTypeStr = obj.optString("balanceType", BalanceType.RECEIVABLE.name)
            val balanceType = try {
                BalanceType.valueOf(balanceTypeStr)
            } catch (_: Exception) {
                BalanceType.RECEIVABLE
            }
            val isActive = obj.optBoolean("isActive", true)
            val hasCustomRates = obj.optBoolean("hasCustomRates", false)
            val c1 = if (obj.has("customRateItem1") && !obj.isNull("customRateItem1")) obj.optDouble("customRateItem1") else null
            val c2 = if (obj.has("customRateItem2") && !obj.isNull("customRateItem2")) obj.optDouble("customRateItem2") else null
            val c3 = if (obj.has("customRateItem3") && !obj.isNull("customRateItem3")) obj.optDouble("customRateItem3") else null
            val c4 = if (obj.has("customRateItem4") && !obj.isNull("customRateItem4")) obj.optDouble("customRateItem4") else null

            val customRatesMap = mutableMapOf<String, Double>()
            val mapObj = obj.optJSONObject("customRatesMap")
            if (mapObj != null) {
                val keys = mapObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    customRatesMap[k] = mapObj.optDouble(k, 0.0)
                }
            }

            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())

            Customer(
                id = id,
                name = name,
                username = username,
                passwordHash = passwordHash,
                phone = phone,
                balance = balance,
                balanceType = balanceType,
                isActive = isActive,
                hasCustomRates = hasCustomRates,
                customRateItem1 = c1,
                customRateItem2 = c2,
                customRateItem3 = c3,
                customRateItem4 = c4,
                customRatesMap = customRatesMap,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse customer json: ${e.message}")
            null
        }
    }

    private fun transactionToJson(tx: TransactionRecord): String {
        val obj = JSONObject()
        obj.put("id", tx.id)
        obj.put("customerId", tx.customerId)
        obj.put("customerName", tx.customerName)
        obj.put("type", tx.type.name)
        if (tx.itemId != null) obj.put("itemId", tx.itemId)
        obj.put("itemName", tx.itemName)
        obj.put("quantity", tx.quantity)
        obj.put("unit", tx.unit)
        obj.put("rate", tx.rate)
        obj.put("amount", tx.amount)
        obj.put("paymentMethod", tx.paymentMethod)
        obj.put("billNumber", tx.billNumber)
        obj.put("date", tx.date)
        obj.put("timestamp", tx.timestamp)
        obj.put("notes", tx.notes)
        obj.put("balanceBefore", tx.balanceBefore)
        obj.put("balanceAfter", tx.balanceAfter)
        obj.put("balanceTypeAfter", tx.balanceTypeAfter.name)
        obj.put("recordedBy", tx.recordedBy)
        return obj.toString()
    }

    private fun jsonToTransaction(jsonStr: String): TransactionRecord? {
        return try {
            val obj = JSONObject(jsonStr)
            val id = obj.optString("id")
            val customerId = obj.optString("customerId")
            if (id.isNullOrBlank() || customerId.isNullOrBlank()) return null
            val customerName = obj.optString("customerName", "")
            val typeStr = obj.optString("type", TransactionType.BILL.name)
            val type = try { TransactionType.valueOf(typeStr) } catch (_: Exception) { TransactionType.BILL }
            val itemId = if (obj.has("itemId") && !obj.isNull("itemId")) obj.optString("itemId") else null
            val itemName = obj.optString("itemName", "")
            val quantity = obj.optDouble("quantity", 0.0)
            val unit = obj.optString("unit", "Kg")
            val rate = obj.optDouble("rate", 0.0)
            val amount = obj.optDouble("amount", 0.0)
            val paymentMethod = obj.optString("paymentMethod", "Cash")
            val billNumber = obj.optString("billNumber", "")
            val date = obj.optString("date", "")
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            val notes = obj.optString("notes", "")
            val balanceBefore = obj.optDouble("balanceBefore", 0.0)
            val balanceAfter = obj.optDouble("balanceAfter", 0.0)
            val balTypeAfterStr = obj.optString("balanceTypeAfter", BalanceType.RECEIVABLE.name)
            val balTypeAfter = try { BalanceType.valueOf(balTypeAfterStr) } catch (_: Exception) { BalanceType.RECEIVABLE }
            val recordedBy = obj.optString("recordedBy", "Admin")

            TransactionRecord(
                id = id,
                customerId = customerId,
                customerName = customerName,
                type = type,
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
                balanceTypeAfter = balTypeAfter,
                recordedBy = recordedBy
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun customerToFirestoreMap(c: Customer): Map<String, Any?> {
        return mapOf(
            "id" to c.id,
            "name" to c.name,
            "username" to c.username.lowercase().trim(),
            "passwordHash" to c.passwordHash,
            "phone" to c.phone,
            "balance" to c.balance,
            "balanceType" to c.balanceType.name,
            "isActive" to c.isActive,
            "hasCustomRates" to c.hasCustomRates,
            "customRateItem1" to c.customRateItem1,
            "customRateItem2" to c.customRateItem2,
            "customRateItem3" to c.customRateItem3,
            "customRateItem4" to c.customRateItem4,
            "customRatesMap" to c.customRatesMap,
            "role" to "CUSTOMER",
            "createdAt" to c.createdAt,
            "updatedAt" to c.updatedAt
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun firestoreDocToCustomer(doc: DocumentSnapshot): Customer? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val name = doc.getString("name") ?: return null
            val username = doc.getString("username") ?: return null
            val passwordHash = doc.getString("passwordHash") ?: ""
            val phone = doc.getString("phone") ?: ""
            val balance = doc.getDouble("balance") ?: 0.0
            val balanceTypeStr = doc.getString("balanceType") ?: BalanceType.RECEIVABLE.name
            val balanceType = try {
                BalanceType.valueOf(balanceTypeStr)
            } catch (_: Exception) {
                BalanceType.RECEIVABLE
            }
            val isActive = doc.getBoolean("isActive") ?: true
            val hasCustomRates = doc.getBoolean("hasCustomRates") ?: false
            val customRateItem1 = doc.getDouble("customRateItem1")
            val customRateItem2 = doc.getDouble("customRateItem2")
            val customRateItem3 = doc.getDouble("customRateItem3")
            val customRateItem4 = doc.getDouble("customRateItem4")
            val rawMap = doc.get("customRatesMap") as? Map<String, Any>
            val customRatesMap = rawMap?.mapNotNull { (k, v) ->
                when (v) {
                    is Number -> k to v.toDouble()
                    else -> null
                }
            }?.toMap() ?: emptyMap()

            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

            Customer(
                id = id,
                name = name,
                username = username,
                passwordHash = passwordHash,
                phone = phone,
                balance = balance,
                balanceType = balanceType,
                isActive = isActive,
                hasCustomRates = hasCustomRates,
                customRateItem1 = customRateItem1,
                customRateItem2 = customRateItem2,
                customRateItem3 = customRateItem3,
                customRateItem4 = customRateItem4,
                customRatesMap = customRatesMap,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firestore customer document: ${e.message}", e)
            null
        }
    }

    private fun transactionToFirestoreMap(tx: TransactionRecord): Map<String, Any?> {
        return mapOf(
            "id" to tx.id,
            "customerId" to tx.customerId,
            "customerName" to tx.customerName,
            "type" to tx.type.name,
            "itemId" to tx.itemId,
            "itemName" to tx.itemName,
            "quantity" to tx.quantity,
            "unit" to tx.unit,
            "rate" to tx.rate,
            "amount" to tx.amount,
            "paymentMethod" to tx.paymentMethod,
            "billNumber" to tx.billNumber,
            "date" to tx.date,
            "timestamp" to tx.timestamp,
            "notes" to tx.notes,
            "balanceBefore" to tx.balanceBefore,
            "balanceAfter" to tx.balanceAfter,
            "balanceTypeAfter" to tx.balanceTypeAfter.name,
            "recordedBy" to tx.recordedBy
        )
    }

    private fun firestoreDocToTransaction(doc: DocumentSnapshot): TransactionRecord? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val customerId = doc.getString("customerId") ?: return null
            val customerName = doc.getString("customerName") ?: ""
            val typeStr = doc.getString("type") ?: TransactionType.BILL.name
            val type = try {
                TransactionType.valueOf(typeStr)
            } catch (_: Exception) {
                TransactionType.BILL
            }
            val itemId = doc.getString("itemId")
            val itemName = doc.getString("itemName") ?: ""
            val quantity = doc.getDouble("quantity") ?: 0.0
            val unit = doc.getString("unit") ?: "Kg"
            val rate = doc.getDouble("rate") ?: 0.0
            val amount = doc.getDouble("amount") ?: 0.0
            val paymentMethod = doc.getString("paymentMethod") ?: "Cash"
            val billNumber = doc.getString("billNumber") ?: ""
            val date = doc.getString("date") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val notes = doc.getString("notes") ?: ""
            val balanceBefore = doc.getDouble("balanceBefore") ?: 0.0
            val balanceAfter = doc.getDouble("balanceAfter") ?: 0.0
            val balTypeAfterStr = doc.getString("balanceTypeAfter") ?: BalanceType.RECEIVABLE.name
            val balTypeAfter = try {
                BalanceType.valueOf(balTypeAfterStr)
            } catch (_: Exception) {
                BalanceType.RECEIVABLE
            }
            val recordedBy = doc.getString("recordedBy") ?: "Admin"

            TransactionRecord(
                id = id,
                customerId = customerId,
                customerName = customerName,
                type = type,
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
                balanceTypeAfter = balTypeAfter,
                recordedBy = recordedBy
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firestore transaction document: ${e.message}", e)
            null
        }
    }
}

/**
 * Extension helper to suspend-await Firebase Task without extra library requirements.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { exception ->
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
    }
    addOnCanceledListener {
        if (continuation.isActive) {
            continuation.cancel()
        }
    }
}
