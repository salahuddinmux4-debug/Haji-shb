package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.cloud.CloudDatabaseService
import com.example.model.BalanceType
import com.example.model.Customer
import com.example.util.SecurityUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiDeviceCustomerAuthTest {

    private lateinit var cloudDatabase: CloudDatabaseService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cloudDatabase = CloudDatabaseService(context)
    }

    @Test
    fun testMultiDeviceCustomerAuthentication_DeviceAcreates_DeviceBauthenticates() = runBlocking {
        // --- DEVICE A: Admin creates Customer A ---
        val customerUid = "cust_perm_${System.currentTimeMillis()}_1234"
        val customerUsername = "tariq_traders"
        val plainPassword = "securePassword123"
        val passwordHash = SecurityUtils.hashPassword(plainPassword)

        val newCustomer = Customer(
            id = customerUid,
            name = "Tariq Traders",
            username = customerUsername,
            passwordHash = passwordHash,
            phone = "03001234567",
            balance = 15000.0,
            balanceType = BalanceType.RECEIVABLE,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Device A saves customer to the shared Cloud Database
        val saveResult = cloudDatabase.saveCustomerToCloud(newCustomer)
        assertTrue("Customer should be saved successfully to Cloud Database", saveResult.isSuccess)

        // --- DEVICE B: Customer attempts to log in on a completely different mobile ---
        // (Device B has no local storage of Device A; queries Cloud Database directly)
        val cloudLookupResult = cloudDatabase.findCustomerByUsernameInCloud("tariq_traders")
        assertTrue("Device B must find customer in shared Cloud Database", cloudLookupResult.isSuccess)

        val foundCustomer = cloudLookupResult.getOrNull()
        assertNotNull("Customer record must exist in Cloud Database", foundCustomer)
        assertEquals("UID must match permanent UID", customerUid, foundCustomer?.id)
        assertEquals("Name must match", "Tariq Traders", foundCustomer?.name)
        assertEquals("Balance must match", 15000.0, foundCustomer?.balance ?: 0.0, 0.001)

        // Password verification on Device B
        val isPasswordCorrect = SecurityUtils.verifyPassword(plainPassword, foundCustomer!!.passwordHash)
        assertTrue("Password verification must succeed with correct credentials", isPasswordCorrect)

        val isWrongPasswordCorrect = SecurityUtils.verifyPassword("wrongpass", foundCustomer.passwordHash)
        assertFalse("Password verification must fail with wrong credentials", isWrongPasswordCorrect)
    }

    @Test
    fun testCloudCustomerLookup_NonExistentCustomer_ReturnsNull() = runBlocking {
        val result = cloudDatabase.findCustomerByUsernameInCloud("unknown_user_99999")
        assertTrue(result.isSuccess)
        assertNull("Non-existent customer must return null without crashing", result.getOrNull())
    }

    @Test
    fun testCloudCustomerDeactivation_BlocksAccess() = runBlocking {
        val customerUid = "cust_perm_deactivated_${System.currentTimeMillis()}"
        val customer = Customer(
            id = customerUid,
            name = "Deactivated Customer",
            username = "deactivated_user",
            passwordHash = SecurityUtils.hashPassword("pass123"),
            phone = "03009999999",
            balance = 0.0,
            balanceType = BalanceType.RECEIVABLE,
            isActive = false, // Deactivated by Admin
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        cloudDatabase.saveCustomerToCloud(customer)

        val lookup = cloudDatabase.findCustomerByUsernameInCloud("deactivated_user").getOrNull()
        assertNotNull(lookup)
        assertFalse("Account must be recognized as inactive across devices", lookup!!.isActive)
    }

    @Test
    fun testCustomerAuthentication() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = com.example.data.repository.MujahidRepository(context)

        val testUsername = "active_trader_${System.currentTimeMillis()}"
        val testPassword = "traderPass123"
        val addResult = repo.addCustomer(
            name = "Active Trader",
            username = testUsername,
            plainPass = testPassword,
            phone = "03001234567",
            balance = 10000.0,
            balanceType = BalanceType.RECEIVABLE
        )
        assertTrue("Customer creation must succeed", addResult.isSuccess)

        val result = repo.authenticateCustomer(testUsername, testPassword)
        assertTrue("Customer should authenticate successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val customer = result.getOrNull()
        assertNotNull(customer)
        assertEquals(testUsername, customer?.username?.lowercase()?.trim())
    }
}
