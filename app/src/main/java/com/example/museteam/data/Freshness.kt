package com.example.museteam.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Logic thời gian THUẦN: độ tươi của số liệu, % còn lại, đếm ngược reset.
 * Mốc "mất tín hiệu" = báo cáo cũ hơn 24 giờ.
 */
object Freshness {

    const val STALE_MILLIS: Long = 24L * 60 * 60 * 1000

    /** Parse ISO-8601 có offset ("2026-11-08T21:19:00+07:00" hoặc "...Z"). */
    fun parseIsoToMillis(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val t = text.trim()
        return try {
            OffsetDateTime.parse(t).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                Instant.parse(t).toEpochMilli()
            } catch (e2: Exception) {
                null
            }
        }
    }

    /** % quota còn lại từ % đã dùng — luôn kẹp trong 0..100. */
    fun remainingPct(usedPct: Int): Int = (100 - usedPct).coerceIn(0, 100)

    fun ageMillis(now: Long, updatedAt: Long?): Long? =
        updatedAt?.let { (now - it).coerceAtLeast(0L) }

    /** Số liệu quá 24h (hoặc không rõ mốc) coi như mất tín hiệu. */
    fun isStale(now: Long, updatedAt: Long?): Boolean {
        val age = ageMillis(now, updatedAt) ?: return true
        return age > STALE_MILLIS
    }

    /** "vừa xong" · "4 phút trước" · "2 giờ trước" · "3 ngày trước". */
    fun ageText(now: Long, updatedAt: Long?): String {
        val age = ageMillis(now, updatedAt) ?: return "chưa rõ"
        val minutes = age / 60_000L
        return when {
            minutes < 1 -> "vừa xong"
            minutes < 60 -> "$minutes phút trước"
            minutes < 60 * 24 -> "${minutes / 60} giờ trước"
            else -> "${minutes / (60 * 24)} ngày trước"
        }
    }

    /** "còn 5 giờ nữa reset" · "còn 2 ngày nữa reset" · "đã reset". */
    fun resetCountdownText(now: Long, resetAt: Long?): String {
        if (resetAt == null) return ""
        val diff = resetAt - now
        if (diff <= 0) return "đã reset"
        val minutes = diff / 60_000L
        return when {
            minutes < 1 -> "còn chưa đầy 1 phút nữa reset"
            minutes < 60 -> "còn $minutes phút nữa reset"
            minutes < 60 * 24 -> "còn ${minutes / 60} giờ nữa reset"
            else -> "còn ${minutes / (60 * 24)} ngày nữa reset"
        }
    }

    /** "08/11" */
    fun formatDayMonth(millis: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
        if (millis == null) return "—"
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return "%02d/%02d".format(dt.dayOfMonth, dt.monthValue)
    }

    /** "8 tháng 11" — đúng cách panel Mức sử dụng của Muse viết. */
    fun formatDayMonthLong(millis: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
        if (millis == null) return "—"
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return "${dt.dayOfMonth} tháng ${dt.monthValue}"
    }

    /** "19:34 · 07/11" */
    fun formatTime(millis: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
        if (millis == null) return ""
        val dt = Instant.ofEpochMilli(millis).atZone(zone)
        return "%02d:%02d · %02d/%02d".format(dt.hour, dt.minute, dt.dayOfMonth, dt.monthValue)
    }
}
