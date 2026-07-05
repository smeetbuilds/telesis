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
    suspend fun readInbox(limit: Int = 2000): List<DeviceSms> = withContext(Dispatchers.IO) {
        val sms = mutableListOf<DeviceSms>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val senderIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext() && sms.size < limit) {
                sms += DeviceSms(
                    id = cursor.getLong(idIndex),
                    sender = cursor.getString(senderIndex).orEmpty(),
                    body = cursor.getString(bodyIndex).orEmpty(),
                    date = cursor.getLong(dateIndex)
                )
            }
        }
        sms
    }
}
