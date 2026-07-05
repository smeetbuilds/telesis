package com.smeet.telesis.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.smeet.telesis.TelesisApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TransactionSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as? TelesisApp ?: return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pending = goAsync()
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
        val timestamp = messages.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(SMS_RECEIVER_TIMEOUT_MS) {
                    app.repository.importSingleSms(sender = sender, body = body, date = timestamp)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Incoming SMS import failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TelesisSmsReceiver"
        private const val SMS_RECEIVER_TIMEOUT_MS = 9_000L
    }
}
