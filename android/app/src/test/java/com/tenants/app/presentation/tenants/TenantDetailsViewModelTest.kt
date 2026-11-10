package com.tenants.app.presentation.tenants

import android.content.Context
import com.tenants.app.data.remote.TenantsApiService
import com.tenants.app.data.remote.model.RoomDto
import com.tenants.app.data.remote.model.TenantDto
import com.tenants.app.data.remote.model.TenantsListResponse
import com.tenants.app.utils.PdfDownloadHelper
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TenantDetailsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: TenantsApiService = mockk()
    private val pdfHelper: PdfDownloadHelper = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private lateinit var viewModel: TenantDetailsViewModel

    private val mockTenant = TenantDto(
        id = "tenant-101",
        name = "Vikram Singh",
        phone = "9876543210",
        familyMembersCount = 3,
        roomId = "room-1",
        allottedPrice = "6000.00",
        electricityRatePerUnit = "8.00",
        lastMeterReading = "250.00",
        currentBalance = "-1500.00",
        isActive = true,
        room = RoomDto("room-1", "201", "2BHK", 2, "6000.00", true)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { apiService.getTenants() } returns Response.success(
            TenantsListResponse(listOf(mockTenant))
        )
        viewModel = TenantDetailsViewModel(apiService, pdfHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadTenantDetailsSuccess() = runTest {
        viewModel.onEvent(TenantDetailsEvent.LoadDetails("tenant-101"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.tenant)
        assertEquals("Vikram Singh", state.tenant?.name)
        assertEquals("-1500.00", state.tenant?.currentBalance)
        assertEquals("201", state.tenant?.room?.roomNo)
    }

    @Test
    fun testDownloadAndSharePdfInvokesHelper() = runTest {
        viewModel.onEvent(TenantDetailsEvent.LoadDetails("tenant-101"))
        testDispatcher.scheduler.advanceUntilIdle()

        val fakePdfBytes = "%PDF-1.4 Mock Bytes".toByteArray()
        coEvery { apiService.downloadTenantPdf("tenant-101") } returns Response.success(
            fakePdfBytes.toResponseBody("application/pdf".toMediaType())
        )

        viewModel.onEvent(TenantDetailsEvent.DownloadAndSharePdf(context))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isPdfDownloading)
        assertEquals("PDF Invoice ready to view or share", state.successMessage)

        verify { pdfHelper.saveAndSharePdf(context, any(), "Invoice_Vikram_Singh.pdf") }
    }
}
