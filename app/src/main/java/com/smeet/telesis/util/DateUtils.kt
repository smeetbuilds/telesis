package com.smeet.telesis.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val shortDate = DateTimeFormatter.ofPattern("dd MMM")
    private val fullDate = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")
    private val inputDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun startOfCurrentMonth(): Long = YearMonth.now().atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOfCurrentMonth(): Long = YearMonth.now().atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    fun startOfToday(): Long = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOfToday(): Long = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    fun formatShort(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(shortDate)
    fun formatFull(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(fullDate)
    fun formatInputDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().format(inputDate)
    fun parseInputDate(value: String): Long? = runCatching {
        LocalDate.parse(value.trim(), inputDate).atStartOfDay(zone).toInstant().toEpochMilli()
    }.getOrNull()
    fun now(): Long = Instant.now().toEpochMilli()
}
