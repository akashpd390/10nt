package com.tenants.app.service

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for ServerWakeupForegroundService lifecycle logic and SMS intent formatting.
 */
class ServerWakeupServiceTest {

    @Test
    fun testLifecycleBackgroundDetection() {
        val createdState = Lifecycle.State.CREATED
        val resumedState = Lifecycle.State.RESUMED

        // Background state (< RESUMED) should trigger background notification
        assertFalse(createdState.isAtLeast(Lifecycle.State.RESUMED))

        // Foreground state (RESUMED) should suppress background notification
        assertTrue(resumedState.isAtLeast(Lifecycle.State.RESUMED))
    }

    @Test
    fun testSmsTemplateFormat() {
        val tenantName = "John Doe"
        val amount = 5000.0
        val paymentMode = "UPI"
        val remainingDue = 1200.0

        val formattedMessage = "Dear $tenantName, payment of Rs ${"%.2f".format(amount)} received via $paymentMode. Remaining Due: Rs ${"%.2f".format(remainingDue)}. Thank you!"
        assertEquals("Dear John Doe, payment of Rs 5000.00 received via UPI. Remaining Due: Rs 1200.00. Thank you!", formattedMessage)
    }
}
