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
    suspend fun createRoom(@Body room: Map<String, Any>): Response<ResponseBody>

    @PUT("api/rooms/{id}")
    suspend fun updateRoom(@Path("id") id: String, @Body room: Map<String, Any>): Response<ResponseBody>

    @DELETE("api/rooms/{id}")
    suspend fun deleteRoom(@Path("id") id: String): Response<ResponseBody>

    @GET("api/tenants")
    suspend fun getTenants(): Response<TenantsListResponse>

    @POST("api/tenants")
    suspend fun createTenant(@Body tenant: Map<String, Any>): Response<ResponseBody>

    @PUT("api/tenants/{id}")
    suspend fun updateTenant(@Path("id") id: String, @Body tenant: Map<String, Any>): Response<ResponseBody>

    @DELETE("api/tenants/{id}")
    suspend fun deleteTenant(@Path("id") id: String): Response<ResponseBody>

    @POST("api/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>

    @Streaming
    @GET("api/export/tenants/{id}/pdf")
    suspend fun downloadTenantPdf(@Path("id") id: String): Response<ResponseBody>
}
