package com.tenants.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tenants.app.data.local.TokenManager
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent
    data class PasswordChanged(val password: String) : AuthEvent
    data object SubmitLogin : AuthEvent
    data object Logout : AuthEvent
    data object ClearError : AuthEvent
}

/**
 * Manages Landlord login state and session persistence via TokenManager.
 */
class AuthViewModel(
    private val apiService: TenantsApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isLoggedIn = tokenManager.isLoggedIn())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> _uiState.update { it.copy(email = event.email, errorMessage = null) }
            is AuthEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password, errorMessage = null) }
            is AuthEvent.SubmitLogin -> login()
            is AuthEvent.Logout -> logout()
            is AuthEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email and password are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val tokens = response.body()!!.tokens
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, password = "") }
                } else {
                    val errorMsg = if (response.code() == 401) "Invalid email or password" else "Login failed (${response.code()})"
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Network error") }
            }
        }
    }

    private fun logout() {
        tokenManager.clearTokens()
        _uiState.update { AuthUiState(isLoggedIn = false) }
    }
}
