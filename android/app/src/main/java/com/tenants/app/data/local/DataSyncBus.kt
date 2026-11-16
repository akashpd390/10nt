package com.tenants.app.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Topics for reactive cross-module data synchronization.
 */
enum class SyncTopic {
    ROOMS,
    TENANTS,
    DASHBOARD,
    ALL
}

/**
 * Lightweight, thread-safe Event Bus for reactive cross-screen data synchronization.
 * When data is mutated (created, updated, deleted) in one module (e.g. Tenants),
 * it emits an invalidation event so other ViewModels (e.g. Rooms and Dashboard)
 * automatically and silently refresh their states in the background.
 */
object DataSyncBus {
    private val _syncEvents = MutableSharedFlow<SyncTopic>(extraBufferCapacity = 16)
    val syncEvents: SharedFlow<SyncTopic> = _syncEvents.asSharedFlow()

    /**
     * Emits a data change topic to notify observing ViewModels.
     */
    fun emitChange(topic: SyncTopic) {
        _syncEvents.tryEmit(topic)
    }
}
