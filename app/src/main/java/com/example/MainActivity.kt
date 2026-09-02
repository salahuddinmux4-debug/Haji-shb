package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.admin.AdminHomeScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.AuthUiState
import com.example.ui.auth.AuthViewModel
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerViewModel
import com.example.ui.theme.MujahidAccountsTheme
import com.example.util.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MujahidAccountsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MujahidAccountsApp()
                }
            }
        }
    }
}

@Composable
fun MujahidAccountsApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    Crossfade(targetState = uiState, label = "AppNavCrossfade") { state ->
        when (state) {
            is AuthUiState.Authenticated -> {
                when (state.role) {
                    UserRole.ADMIN -> {
                        val adminViewModel: AdminViewModel = viewModel()
                        AdminHomeScreen(
                            adminViewModel = adminViewModel,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                    UserRole.CUSTOMER -> {
                        val customerViewModel: CustomerViewModel = viewModel()
                        CustomerHomeScreen(
                            customerViewModel = customerViewModel,
                            onLogout = { authViewModel.logout() }
                        )
                    }
                    UserRole.NONE -> {
                        AuthScreen(authViewModel = authViewModel)
                    }
                }
            }
            else -> {
                AuthScreen(authViewModel = authViewModel)
            }
        }
    }
}
