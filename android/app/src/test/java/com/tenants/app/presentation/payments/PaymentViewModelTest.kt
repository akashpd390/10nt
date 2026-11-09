package com.tenants.app.presentation.payments

import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.data.remote.model.TenantsListResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: TenantsApiService = mockk()
    private lateinit var viewModel: PaymentViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { apiService.getTenants() } returns Response.success(
            TenantsListResponse(
                listOf(
                    TenantDto(
                        id = "tenant-1",
                        name = "Rahul Sharma",
                        phone = "9876543210",
                        familyMembersCount = 2,
                        roomId = "room-1",
                        allottedPrice = "5000.00",
                        electricityRatePerUnit = "8.00",
                        lastMeterReading = "120.00",
                        currentBalance = "-5000.00",
                        isActive = true,
                        room = RoomDto("room-1", "101", "1BHK", 1, "5000.00", true)
                    )
                )
            )
        )
        viewModel = PaymentViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTenantSelectionAutoPopulatesAmountAndMeterReading() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val tenant = viewModel.uiState.value.tenants.first()
        viewModel.onEvent(PaymentEvent.TenantSelected(tenant))

        val state = viewModel.uiState.value
        assertEquals(tenant, state.selectedTenant)
        assertEquals("5000.00", state.amountPaid)
        assertEquals("120.00", state.currentElectricityReading)
    }

    @Test
    fun testSearchFiltering() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PaymentEvent.SearchQueryChanged("Rahul"))
        assertEquals(1, viewModel.uiState.value.filteredTenants.size)

        viewModel.onEvent(PaymentEvent.SearchQueryChanged("NonExistent"))
        assertEquals(0, viewModel.uiState.value.filteredTenants.size)
    }
}
