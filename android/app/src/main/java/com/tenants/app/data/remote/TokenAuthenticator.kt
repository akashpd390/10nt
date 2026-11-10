package com.tenants.app.data.remote

import com.google.gson.Gson
import com.tenants.app.BuildConfig
import com.tenants.app.data.local.SessionEvents
import com.tenants.app.data.local.TokenManager
import com.tenants.app.data.remote.model.TokensDto
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Automatically intercepts HTTP 401 Unauthorized responses.
 * Refreshes access token silently using the 7-day refresh token.
 * If the 7-day refresh token also expires, triggers forced logout and prompts the user.
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops if refresh endpoint itself returned 401
        if (response.request.url.encodedPath.contains("api/auth/refresh") ||
            response.request.url.encodedPath.contains("api/auth/login")
        ) {
            return null
        }

        // Check if retry count exceeded 2 attempts
        if (responseCount(response) >= 2) {
            return null
        }

        val currentRefreshToken = tokenManager.getRefreshToken()
        if (currentRefreshToken.isNullOrBlank()) {
            forceLogout("No session token found. Please log in.")
            return null
        }

        // Call /api/auth/refresh synchronously
        val refreshRequestBody = "{\"refreshToken\":\"$currentRefreshToken\"}"
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(BuildConfig.BASE_URL + "api/auth/refresh")
            .post(refreshRequestBody)
            .build()

        val client = OkHttpClient.Builder().build()
        try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful && refreshResponse.body != null) {
                val jsonString = refreshResponse.body!!.string()
                val parsed = gson.fromJson(jsonString, RefreshResponseDto::class.java)

                if (parsed?.tokens != null) {
                    tokenManager.saveTokens(parsed.tokens.accessToken, parsed.tokens.refreshToken)

                    // Retry original request with newly acquired access token
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${parsed.tokens.accessToken}")
                        .build()
                }
            } else {
                // 7-day refresh token has expired or is invalid
                forceLogout("Your 7-day session has expired. Please sign in again.")
                return null
            }
        } catch (e: Exception) {
            return null
        }

        return null
    }

    private fun forceLogout(reason: String) {
        tokenManager.clearTokens()
        SessionEvents.triggerForceLogout(reason)
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private data class RefreshResponseDto(val tokens: TokensDto?)
}
