package com.tenants.app.data.remote.model

import com.google.gson.annotations.SerializedName

// Auth DTOs
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class TokensDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class LoginResponse(
    @SerializedName("message") val message: String,
    @SerializedName("tokens") val tokens: TokensDto
)

// Dashboard DTOs
data class OccupancyDto(
    @SerializedName("totalRooms") val totalRooms: Int,
    @SerializedName("occupiedRooms") val occupiedRooms: Int,
    @SerializedName("availableRooms") val availableRooms: Int,
    @SerializedName("occupancyPercentage") val occupancyPercentage: Double
)

data class TenantsSummaryDto(
    @SerializedName("totalActiveTenants") val totalActiveTenants: Int,
    @SerializedName("totalPeopleCount") val totalPeopleCount: Int
)

data class FinancesDto(
    @SerializedName("totalPendingBalance") val totalPendingBalance: Double,
    @SerializedName("totalAdvanceBalance") val totalAdvanceBalance: Double
)

data class UnpaidTenantDto(
    @SerializedName("tenantId") val tenantId: String,
    @SerializedName("tenantName") val tenantName: String,
    @SerializedName("roomNo") val roomNo: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("dueAmount") val dueAmount: Double,
    @SerializedName("profilePicUrl") val profilePicUrl: String?
)

data class RecentTxDto(
    @SerializedName("id") val id: String,
    @SerializedName("tenantName") val tenantName: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("mode") val mode: String,
    @SerializedName("date") val date: String,
    @SerializedName("balanceAfter") val balanceAfter: Double
)

data class DashboardResponse(
    @SerializedName("occupancy") val occupancy: OccupancyDto,
    @SerializedName("tenantsSummary") val tenantsSummary: TenantsSummaryDto,
    @SerializedName("finances") val finances: FinancesDto,
    @SerializedName("unpaidTenants") val unpaidTenants: List<UnpaidTenantDto>,
    @SerializedName("recentTransactions") val recentTransactions: List<RecentTxDto>
)

// Rooms DTOs
data class RoomDto(
    @SerializedName("id") val id: String,
    @SerializedName("roomNo") val roomNo: String,
    @SerializedName("description") val description: String?,
    @SerializedName("floor") val floor: Int,
    @SerializedName("basePrice") val basePrice: String,
    @SerializedName("isOccupied") val isOccupied: Boolean
)

data class RoomsListResponse(
    @SerializedName("rooms") val rooms: List<RoomDto>
)

// Tenants DTOs
data class TenantDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("familyMembersCount") val familyMembersCount: Int,
    @SerializedName("roomId") val roomId: String?,
    @SerializedName("allottedPrice") val allottedPrice: String,
    @SerializedName("electricityRatePerUnit") val electricityRatePerUnit: String,
    @SerializedName("lastMeterReading") val lastMeterReading: String,
    @SerializedName("currentBalance") val currentBalance: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("room") val room: RoomDto?
)

data class TenantsListResponse(
    @SerializedName("tenants") val tenants: List<TenantDto>
)

// Payments DTOs
data class CreatePaymentRequest(
    @SerializedName("tenantId") val tenantId: String,
    @SerializedName("amountPaid") val amountPaid: Double,
    @SerializedName("paymentMode") val paymentMode: String,
    @SerializedName("currentElectricityReading") val currentElectricityReading: Double?,
    @SerializedName("notes") val notes: String?
)

data class PaymentSummaryDto(
    @SerializedName("tenantName") val tenantName: String,
    @SerializedName("tenantPhone") val tenantPhone: String,
    @SerializedName("amountPaid") val amountPaid: Double,
    @SerializedName("paymentMode") val paymentMode: String,
    @SerializedName("newBalance") val newBalance: Double,
    @SerializedName("status") val status: String,
    @SerializedName("smsTemplate") val smsTemplate: String
)

data class CreatePaymentResponse(
    @SerializedName("message") val message: String,
    @SerializedName("summary") val summary: PaymentSummaryDto
)
