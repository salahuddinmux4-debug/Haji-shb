package com.example.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MujahidRepository
import com.example.model.AdminUser
import com.example.model.Customer
import com.example.util.SessionManager
import com.example.util.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object CheckingSession : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val role: UserRole, val customer: Customer? = null, val admin: AdminUser? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MujahidRepository(application)
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(UserRole.CUSTOMER) // Default to Customer or Admin
    val selectedTab: StateFlow<UserRole> = _selectedTab.asStateFlow()

    init {
        checkExistingSession()
    }

    fun setTab(role: UserRole) {
        _selectedTab.value = role
        _uiState.value = AuthUiState.Idle
    }

    fun checkExistingSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.CheckingSession
            if (sessionManager.isLoggedIn() && sessionManager.isAutoLoginEnabled()) {
                val role = sessionManager.getRole()
                val userId = sessionManager.getUserId()
                if (role == UserRole.CUSTOMER && userId != null) {
                    val customer = repository.getCustomerById(userId)
                    if (customer != null && customer.isActive) {
                        _uiState.value = AuthUiState.Authenticated(UserRole.CUSTOMER, customer = customer)
                        return@launch
                    } else {
                        // Inactive or deleted -> clear session
                        sessionManager.clearSession()
                        _uiState.value = AuthUiState.Idle
                        return@launch
                    }
                } else if (role == UserRole.ADMIN) {
                    _uiState.value = AuthUiState.Authenticated(UserRole.ADMIN, admin = AdminUser("admin", "Admin", "admin", ""))
                    return@launch
                }
            }
            _uiState.value = AuthUiState.Idle
        }
    }

    fun loginCustomer(username: String, plainPass: String) {
        if (username.isBlank() || plainPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter both username and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.authenticateCustomer(username, plainPass)
            result.onSuccess { customer ->
                sessionManager.saveCustomerSession(customer.id, customer.name, customer.username)
                _uiState.value = AuthUiState.Authenticated(UserRole.CUSTOMER, customer = customer)
            }.onFailure { ex ->
                _uiState.value = AuthUiState.Error(ex.message ?: "Authentication failed")
            }
        }
    }

    fun loginAdmin(username: String, plainPass: String) {
        if (username.isBlank() || plainPass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter Admin credentials")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val admin = repository.authenticateAdmin(username, plainPass)
            if (admin != null) {
                sessionManager.saveAdminSession(admin.id, admin.name, admin.username)
                _uiState.value = AuthUiState.Authenticated(UserRole.ADMIN, admin = admin)
            } else {
                _uiState.value = AuthUiState.Error("Invalid admin credentials. Please try again.")
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _uiState.value = AuthUiState.Idle
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
