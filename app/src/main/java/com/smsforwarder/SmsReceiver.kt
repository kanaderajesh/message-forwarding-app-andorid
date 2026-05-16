package com.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = PrefsManager(context)
        if (!prefs.isForwardingEnabled) return

        val forwardTo = prefs.forwardToNumber
        val keywords = prefs.getKeywords()
        if (forwardTo.isBlank() || keywords.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in messages) {
            val body = message.messageBody ?: continue
            val sender = message.originatingAddress ?: "Unknown"
            val bodyLower = body.lowercase()

            val matched = keywords.firstOrNull { bodyLower.contains(it) } ?: continue

            Log.d(TAG, "Keyword '$matched' matched in message from $sender — forwarding to $forwardTo")
            forwardSms(context, forwardTo, sender, body)
        }
    }

    private fun forwardSms(context: Context, to: String, from: String, body: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val text = "Fwd from $from:\n$body"
            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(to, null, parts, null, null)
            Log.d(TAG, "Forwarded SMS to $to")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS: ${e.message}")
        }
    }
}
