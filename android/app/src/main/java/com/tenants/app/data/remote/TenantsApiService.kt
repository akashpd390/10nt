package com.tenants.app.data.remote

import com.tenants.app.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TenantsApiService {

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/dashboard/overview")
    suspend fun getDashboardOverview(): Response<DashboardResponse>

    @GET("api/rooms")
    suspend fun getRooms(): Response<RoomsListResponse>

    @POST("api/rooms")
    suspend fun createRoom(@Body request: CreateRoomRequest): Response<ResponseBody>

    @PUT("api/rooms/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body request: UpdateRoomRequest): Response<ResponseBody>

    @DELETE("api/rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String): Response<ResponseBody>

    @GET("api/tenants")
    suspend fun getTenants(): Response<TenantsListResponse>

    @GET("api/tenants/{id}")
    suspend fun getTenantById(@Path("id") id: String): Response<TenantDetailResponse>

    @POST("api/tenants")
    suspend fun createTenant(@Body request: CreateTenantRequest): Response<ResponseBody>

    @PUT("api/tenants/{id}")
    suspend fun updateTenant(@Path("id") id: String, @Body request: UpdateTenantRequest): Response<ResponseBody>

    @DELETE("api/tenants/{id}")
    suspend fun deleteTenant(@Path("id") id: String): Response<ResponseBody>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: okhttp3.MultipartBody.Part): Response<UploadResponse>

    @POST("api/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @GET("api/payments")
    suspend fun getPayments(): Response<PaymentsListResponse>

    @Streaming
    @GET("api/export/tenants/{id}/pdf")
    suspend fun downloadTenantPdf(@Path("id") id: String): Response<ResponseBody>

    @POST("api/billing/generate-rent-roll")
    suspend fun generateRentRoll(@Body request: GenerateRentRollRequest): Response<GenerateRentRollResponse>

    @GET("api/billing/summary")
    suspend fun getMonthlyBillingSummary(@Query("month") month: String? = null): Response<MonthlyBillingSummaryResponse>

    @POST("api/billing/update-readings")
    suspend fun updateMeterReadings(@Body request: BatchMeterUpdateRequest): Response<ResponseBody>

    @Streaming
    @GET("api/export/bills/csv")
    suspend fun downloadMonthlyBillsCsv(@Query("month") month: String? = null): Response<ResponseBody>
}
