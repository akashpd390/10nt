package com.tenants.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for session lifecycle events (e.g. forced logout due to 7-day token expiration).
 */
object SessionEvents {
    private val _forceLogoutEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val forceLogoutEvent: SharedFlow<String> = _forceLogoutEvent.asSharedFlow()

    fun triggerForceLogout(reason: String = "Your 7-day session has expired. Please sign in again.") {
        _forceLogoutEvent.tryEmit(reason)
    }
}
