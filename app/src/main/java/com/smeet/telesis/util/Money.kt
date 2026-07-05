package com.smeet.telesis.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

object Money {
    private val india = Locale("en", "IN")

    fun parseToPaise(raw: String): Long? {
        val cleaned = raw.replace(",", "").trim()
        val value = cleaned.toBigDecimalOrNull() ?: return null
        return value.multiply(100.toBigDecimal()).toLong()
    }

    fun format(paise: Long, compact: Boolean = false): String {
        val rupees = paise / 100.0
        if (compact && paise.absoluteValue >= 10000000L) return "₹%.1fL".format(india, rupees / 100000.0)
        if (compact && paise.absoluteValue >= 100000L) return "₹%.1fK".format(india, rupees / 1000.0)
        return NumberFormat.getCurrencyInstance(india).apply {
            maximumFractionDigits = if (paise % 100L == 0L) 0 else 2
        }.format(rupees)
    }
}
