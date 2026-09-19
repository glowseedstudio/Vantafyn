package dev.vantafyn.feature.home

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SocialDateFormatter {

    fun parseIsoTimestamp(isoString: String?): Date? {
        if (isoString.isNullOrBlank()) return null
        val trimmed = isoString.trim()
        if (trimmed.equals("Now", ignoreCase = true) || trimmed.equals("Active now", ignoreCase = true)) {
            return Date()
        }

        // Try epoch millis
        trimmed.toLongOrNull()?.let { millis ->
            if (millis > 0L) return Date(millis)
        }

        // Try Instant parse
        try {
            return Date(java.time.Instant.parse(trimmed).toEpochMilli())
        } catch (_: Exception) {}

        // Format patterns
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )

        val clean = trimmed.substringBefore("+").substringBefore("Z")
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsed = format.parse(clean)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Formats "last seen" or "last active" timestamps respecting the user's phone date/time settings.
     */
    fun formatSocialLastSeen(context: Context, lastSeenStr: String?): String {
        if (lastSeenStr.isNullOrBlank()) return "Offline"
        val trimmed = lastSeenStr.trim()
        if (trimmed.equals("Now", ignoreCase = true) || trimmed.equals("Active now", ignoreCase = true)) {
            return "Active now"
        }
        if (trimmed.equals("Recently", ignoreCase = true)) {
            return "Active recently"
        }

        val date = parseIsoTimestamp(trimmed) ?: return "Offline"
        val now = System.currentTimeMillis()
        val diffMs = now - date.time

        if (diffMs < 60_000L) {
            return "Active just now"
        }
        if (diffMs < 3_600_000L) {
            val mins = (diffMs / 60_000L).coerceAtLeast(1)
            return "Active ${mins}m ago"
        }

        val nowCal = Calendar.getInstance()
        val dateCal = Calendar.getInstance().apply { time = date }
        val timeFormat = DateFormat.getTimeFormat(context)

        val isSameYear = nowCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR)
        val isToday = isSameYear && nowCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) {
            return "Active today at ${timeFormat.format(date)}"
        }

        val isYesterday = isSameYear && (nowCal.get(Calendar.DAY_OF_YEAR) - dateCal.get(Calendar.DAY_OF_YEAR) == 1)
        if (isYesterday) {
            return "Active yesterday at ${timeFormat.format(date)}"
        }

        val daysDiff = (diffMs / (24 * 3_600_000L))
        if (daysDiff < 7) {
            val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            return "Active ${dayNameFormat.format(date)} at ${timeFormat.format(date)}"
        }

        val systemDateFormat = DateFormat.getDateFormat(context)
        return "Last seen ${systemDateFormat.format(date)}"
    }

    /**
     * Formats conversation list timestamps (e.g. today's time, "Yesterday", weekday, or phone date).
     */
    fun formatConversationDate(context: Context, isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        val date = parseIsoTimestamp(isoString) ?: return ""

        val nowCal = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }

        val isSameYear = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
        val isToday = isSameYear && nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) {
            return DateFormat.getTimeFormat(context).format(date)
        }

        val isYesterday = isSameYear && (nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1)
        if (isYesterday) {
            return "Yesterday"
        }

        val diffMs = nowCal.timeInMillis - msgCal.timeInMillis
        val daysDiff = diffMs / (24 * 3_600_000L)
        if (daysDiff < 7) {
            return SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        }

        return DateFormat.getDateFormat(context).format(date)
    }

    /**
     * Formats individual chat message time (e.g. 2:30 PM / 14:30 according to user phone setting).
     */
    fun formatMessageTime(context: Context, isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        val date = parseIsoTimestamp(isoString) ?: return ""
        return DateFormat.getTimeFormat(context).format(date)
    }
}
