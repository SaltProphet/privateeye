package dev.privateeye.common.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log entry for the PrivateEye console
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
) {
    fun formatTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatForConsole(): String {
        return "[${formatTimestamp()}] [${level.name}] [$tag] $message"
    }
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
