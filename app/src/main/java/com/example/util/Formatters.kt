package com.example.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor

object Formatters {
    private val dhakaZone: ZoneId = ZoneId.of("Asia/Dhaka")
    private val bdDateFmt = DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.US)
    private val bdDateTimeFmt = DateTimeFormatter.ofPattern("dd-MMMM-yyyy hh:mm:ss a", Locale.US)
    private val isoDateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val isoMonthFmt = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    private val numFmt = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }

    fun formatTaka(amount: Double): String = "TK ${numFmt.format(amount)}"

    fun formatNumber(amount: Double): String = numFmt.format(amount)

    fun todayDhakaIso(): String = ZonedDateTime.now(dhakaZone).format(isoDateFmt)

    fun currentMonthDhakaIso(): String = ZonedDateTime.now(dhakaZone).format(isoMonthFmt)

    fun currentYearDhaka(): String = ZonedDateTime.now(dhakaZone).year.toString()

    fun nowDhakaDateTime(): String = ZonedDateTime.now(dhakaZone).format(bdDateTimeFmt)

    fun formatDateBD(raw: String?): String {
        if (raw.isNullOrBlank()) return "N/A"
        return parseZonedDateTime(raw)?.format(bdDateFmt)
            ?: parseLocalDate(raw)?.format(bdDateFmt)
            ?: raw
    }

    fun formatDateTimeBD(raw: String?): String {
        if (raw.isNullOrBlank()) return "N/A"
        return parseZonedDateTime(raw)?.format(bdDateTimeFmt)
            ?: parseLocalDate(raw)?.format(bdDateFmt)
            ?: raw
    }

    fun extractYearMonth(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        parseZonedDateTime(raw)?.let { return it.format(isoMonthFmt) }
        parseLocalDate(raw)?.let { return it.format(isoMonthFmt) }
        return if (raw.length >= 7) raw.substring(0, 7) else ""
    }

    fun extractYear(raw: String?): String {
        val ym = extractYearMonth(raw)
        return if (ym.length >= 4) ym.substring(0, 4) else ""
    }

    fun isPastDate(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val date = parseZonedDateTime(raw)?.toLocalDate() ?: parseLocalDate(raw) ?: return false
        return date.isBefore(ZonedDateTime.now(dhakaZone).toLocalDate())
    }

    fun isDueThisMonth(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val date = parseZonedDateTime(raw)?.toLocalDate() ?: parseLocalDate(raw) ?: return false
        val now = ZonedDateTime.now(dhakaZone).toLocalDate()
        return date.month == now.month && date.year == now.year
    }

    fun isDueWithinDays(raw: String?, days: Long): Boolean {
        if (raw.isNullOrBlank()) return false
        val date = parseZonedDateTime(raw)?.toLocalDate() ?: parseLocalDate(raw) ?: return false
        val now = ZonedDateTime.now(dhakaZone).toLocalDate()
        return !date.isBefore(now) && !date.isAfter(now.plusDays(days))
    }

    private fun parseZonedDateTime(raw: String): ZonedDateTime? {
        return runCatching {
            Instant.parse(raw).atZone(dhakaZone)
        }.getOrNull() ?: runCatching {
            ZonedDateTime.parse(raw).withZoneSameInstant(dhakaZone)
        }.getOrNull() ?: runCatching {
            java.time.LocalDateTime.parse(raw.replace(" ", "T")).atZone(dhakaZone)
        }.getOrNull()
    }

    private fun parseLocalDate(raw: String): LocalDate? {
        return runCatching {
            LocalDate.parse(raw.take(10), isoDateFmt)
        }.getOrNull()
    }

    fun numberToWords(amount: Double): String {
        val n = floor(amount).toLong()
        if (n <= 0L) return "Zero"

        val ones = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convert(value: Long): String {
            return when {
                value < 20L -> ones[value.toInt()]
                value < 100L -> tens[(value / 10).toInt()] + if (value % 10 != 0L) " ${ones[(value % 10).toInt()]}" else ""
                value < 1_000L -> "${ones[(value / 100).toInt()]} Hundred" + if (value % 100 != 0L) " ${convert(value % 100)}" else ""
                value < 100_000L -> "${convert(value / 1_000L)} Thousand" + if (value % 1_000L != 0L) " ${convert(value % 1_000L)}" else ""
                value < 10_000_000L -> "${convert(value / 100_000L)} Lakh" + if (value % 100_000L != 0L) " ${convert(value % 100_000L)}" else ""
                else -> "${convert(value / 10_000_000L)} Crore" + if (value % 10_000_000L != 0L) " ${convert(value % 10_000_000L)}" else ""
            }
        }

        return convert(n)
    }
}
