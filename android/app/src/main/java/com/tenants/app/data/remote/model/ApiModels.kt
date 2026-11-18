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

data class CreateRoomRequest(
    @SerializedName("roomNo") val roomNo: String,
    @SerializedName("floor") val floor: Int,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("description") val description: String? = null
)

data class UpdateRoomRequest(
    @SerializedName("floor") val floor: Int,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("description") val description: String? = null
)

// Tenants DTOs
data class LedgerEntryDto(
    @SerializedName("id") val id: String,
    @SerializedName("tenantId") val tenantId: String,
    @SerializedName("type") val type: String, // BILL_CHARGE, PAYMENT_RECEIVED, MANUAL_ADJUSTMENT
    @SerializedName("amount") val amount: String,
    @SerializedName("runningBalance") val runningBalance: String,
    @SerializedName("description") val description: String,
    @SerializedName("referenceId") val referenceId: String? = null,
    @SerializedName("createdAt") val createdAt: String
)

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
    @SerializedName("room") val room: RoomDto?,
    @SerializedName("adhaarCardUrl") val adhaarCardUrl: String? = null,
    @SerializedName("profilePicUrl") val profilePicUrl: String? = null,
    @SerializedName("joinedDate") val joinedDate: String? = null,
    @SerializedName("ledgerEntries") val ledgerEntries: List<LedgerEntryDto>? = null
)

data class TenantsListResponse(
    @SerializedName("tenants") val tenants: List<TenantDto>
)

data class TenantDetailResponse(
    @SerializedName("tenant") val tenant: TenantDto
)

data class UploadResponse(
    @SerializedName("message") val message: String,
    @SerializedName("url") val url: String
)

data class CreateTenantRequest(
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("familyMembersCount") val familyMembersCount: Int,
    @SerializedName("roomId") val roomId: String,
    @SerializedName("allottedPrice") val allottedPrice: Double,
    @SerializedName("electricityRatePerUnit") val electricityRatePerUnit: Double,
    @SerializedName("initialMeterReading") val initialMeterReading: Double,
    @SerializedName("initialAdvanceOrBalance") val initialAdvanceOrBalance: Double,
    @SerializedName("adhaarCardUrl") val adhaarCardUrl: String? = null,
    @SerializedName("profilePicUrl") val profilePicUrl: String? = null
)

data class UpdateTenantRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("familyMembersCount") val familyMembersCount: Int? = null,
    @SerializedName("allottedPrice") val allottedPrice: Double? = null,
    @SerializedName("electricityRatePerUnit") val electricityRatePerUnit: Double? = null,
    @SerializedName("adhaarCardUrl") val adhaarCardUrl: String? = null,
    @SerializedName("profilePicUrl") val profilePicUrl: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null
)

/**
 * Parses JSON error responses returned by the backend (e.g. {"error": "..."})
 * Fallback to HTTP status code message if parsing fails.
 */
fun parseApiError(response: retrofit2.Response<*>): String {
    try {
        val errorString = response.errorBody()?.string()
        if (!errorString.isNullOrBlank()) {
            val json = com.google.gson.JsonParser.parseString(errorString).asJsonObject
            if (json.has("error") && !json.get("error").isJsonNull) {
                return json.get("error").asString
            }
            if (json.has("message") && !json.get("message").isJsonNull) {
                return json.get("message").asString
            }
        }
    } catch (_: Exception) {}
    return "Request failed with status ${response.code()}"
}

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

data class PaymentRecordDto(
    @SerializedName("id") val id: String,
    @SerializedName("tenantId") val tenantId: String,
    @SerializedName("amountPaid") val amountPaid: String,
    @SerializedName("paymentMode") val paymentMode: String,
    @SerializedName("paymentDate") val paymentDate: String,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("balanceBefore") val balanceBefore: String? = null,
    @SerializedName("balanceAfter") val balanceAfter: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("tenant") val tenant: TenantDto? = null
)

data class PaymentsListResponse(
    @SerializedName("payments") val payments: List<PaymentRecordDto>
)
