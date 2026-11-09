package com.tenants.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
// Uncomment for Direct SMS mode:
// import android.telephony.SmsManager

/**
 * Interface defining SMS sender capabilities.
 */
interface SmsSender {
    fun sendSms(context: Context, phoneNumber: String, message: String)
}

/**
 * Default Interactive SMS Sender.
 * Uses Android ACTION_SENDTO intent so the user can verify the text in their native SMS app.
 * Does NOT require android.permission.SEND_SMS.
 */
class InteractiveIntentSmsSender : SmsSender {
    override fun sendSms(context: Context, phoneNumber: String, message: String) {
        val uri = Uri.parse("smsto:$phoneNumber")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Direct SIM SMS Sender (Commented out / Alternative Mode).
 * Sends SMS directly via the device SIM card using SmsManager.
 * REQUIRES: android.permission.SEND_SMS in AndroidManifest.xml.
 */
class DirectSimSmsSender : SmsSender {
    override fun sendSms(context: Context, phoneNumber: String, message: String) {
        /*
        val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        */
        // Fallback to intent if called without direct permission
        InteractiveIntentSmsSender().sendSms(context, phoneNumber, message)
    }
}
