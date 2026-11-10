package com.tenants.app.presentation.auth

import com.tenants.app.data.local.TokenManager
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.LoginRequest
import com.tenants.app.data.remote.model.LoginResponse
import com.tenants.app.data.remote.model.TokensDto
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: TenantsApiService = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { tokenManager.isLoggedIn() } returns false
        viewModel = AuthViewModel(apiService, tokenManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoginSuccessSavesTokensAndSetsLoggedInTrue() = runTest {
        val email = "landlord@example.com"
        val password = "SecretPassword123!"

        coEvery { apiService.login(LoginRequest(email, password)) } returns Response.success(
            LoginResponse(
                message = "Login successful",
                tokens = TokensDto(accessToken = "access-token-xyz", refreshToken = "refresh-token-abc")
            )
        )

        viewModel.onEvent(AuthEvent.EmailChanged(email))
        viewModel.onEvent(AuthEvent.PasswordChanged(password))
        viewModel.onEvent(AuthEvent.SubmitLogin)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("", state.password) // Password cleared from state for security

        verify { tokenManager.saveTokens("access-token-xyz", "refresh-token-abc") }
    }

    @Test
    fun testLoginFailureSetsErrorMessage() = runTest {
        val email = "wrong@example.com"
        val password = "wrong"

        coEvery { apiService.login(LoginRequest(email, password)) } returns Response.error(
            401,
            "{}".toResponseBody()
        )

        viewModel.onEvent(AuthEvent.EmailChanged(email))
        viewModel.onEvent(AuthEvent.PasswordChanged(password))
        viewModel.onEvent(AuthEvent.SubmitLogin)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertEquals("Invalid email or password", state.errorMessage)

        verify(exactly = 0) { tokenManager.saveTokens(any(), any()) }
    }

    @Test
    fun testLogoutClearsTokens() {
        viewModel.onEvent(AuthEvent.Logout)
        assertFalse(viewModel.uiState.value.isLoggedIn)
        verify { tokenManager.clearTokens() }
    }
}
