package com.smeet.telesis.sms

import android.content.ContentResolver
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceSms(
    val id: Long,
    val sender: String,
    val body: String,
    val date: Long
)

class SmsReader(private val contentResolver: ContentResolver) {
    suspend fun readInbox(limit: Int = DEFAULT_LIMIT, sinceMillis: Long? = null): List<DeviceSms> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(MIN_LIMIT, MAX_LIMIT)
        val sms = mutableListOf<DeviceSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = sinceMillis?.let { "${Telephony.Sms.DATE} >= ?" }
        val selectionArgs = sinceMillis?.let { arrayOf(it.toString()) }
        contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val senderIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext() && sms.size < safeLimit) {
                runCatching {
                    DeviceSms(
                        id = cursor.getLong(idIndex),
                        sender = cursor.getString(senderIndex).orEmpty().take(80),
                        body = cursor.getString(bodyIndex).orEmpty().take(MAX_BODY_CHARS),
                        date = cursor.getLong(dateIndex)
                    )
                }.getOrNull()?.let { row ->
                    if (row.body.isNotBlank()) sms += row
                }
            }
        }
        sms
    }

    companion object {
        private const val MIN_LIMIT = 100
        private const val DEFAULT_LIMIT = 2000
        private const val MAX_LIMIT = 5000
        private const val MAX_BODY_CHARS = 1_200
    }
}
