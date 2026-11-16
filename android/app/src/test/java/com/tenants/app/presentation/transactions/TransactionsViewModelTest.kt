package com.tenants.app.presentation.transactions

import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.PaymentRecordDto
import com.tenants.app.data.remote.model.PaymentsListResponse
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: TenantsApiService = mockk()
    private lateinit var viewModel: TransactionsViewModel

    private val mockPayments = listOf(
        PaymentRecordDto(
            id = "tx-1",
            tenantId = "tenant-1",
            amountPaid = "5500.00",
            paymentMode = "UPI",
            paymentDate = "2026-11-13T16:09:12.358Z",
            notes = "Manual rent payment",
            balanceBefore = "0.00",
            balanceAfter = "5500.00",
            tenant = TenantDto(
                id = "tenant-1",
                name = "Rahul Sharma",
                phone = "9876543210",
                familyMembersCount = 2,
                roomId = "room-1",
                allottedPrice = "5500.00",
                electricityRatePerUnit = "8.00",
                lastMeterReading = "220.00",
                currentBalance = "5500.00",
                isActive = true,
                room = RoomDto("room-1", "101", "1BHK", 1, "5500.00", true)
            )
        ),
        PaymentRecordDto(
            id = "tx-2",
            tenantId = "tenant-2",
            amountPaid = "4000.00",
            paymentMode = "CASH",
            paymentDate = "2026-11-10T12:01:37.418Z",
            notes = "Cash advance",
            balanceBefore = "-4000.00",
            balanceAfter = "0.00",
            tenant = TenantDto(
                id = "tenant-2",
                name = "Pooja Verma",
                phone = "9876543211",
                familyMembersCount = 1,
                roomId = "room-2",
                allottedPrice = "4000.00",
                electricityRatePerUnit = "8.00",
                lastMeterReading = "120.00",
                currentBalance = "0.00",
                isActive = true,
                room = RoomDto("room-2", "102", "Single", 1, "4000.00", true)
            )
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { apiService.getPayments() } returns Response.success(PaymentsListResponse(mockPayments))
        viewModel = TransactionsViewModel(apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load fetches payments and updates state`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(2, state.allTransactions.size)
        assertEquals(2, state.filteredTransactions.size)
    }

    @Test
    fun `filtering by payment mode filters transactions properly`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.FilterModeSelected("UPI"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("UPI", viewModel.uiState.value.filteredTransactions.first().paymentMode)

        viewModel.onEvent(TransactionsEvent.FilterModeSelected("CASH"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("CASH", viewModel.uiState.value.filteredTransactions.first().paymentMode)

        viewModel.onEvent(TransactionsEvent.FilterModeSelected("ALL"))
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
    }

    @Test
    fun `search by tenant name filters correctly`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SearchQueryChanged("Rahul"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("Rahul Sharma", viewModel.uiState.value.filteredTransactions.first().tenant?.name)

        viewModel.onEvent(TransactionsEvent.ClearSearch)
        assertEquals(2, viewModel.uiState.value.filteredTransactions.size)
    }

    @Test
    fun `search by room number filters correctly`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(TransactionsEvent.SearchQueryChanged("102"))
        assertEquals(1, viewModel.uiState.value.filteredTransactions.size)
        assertEquals("Pooja Verma", viewModel.uiState.value.filteredTransactions.first().tenant?.name)
    }
}
