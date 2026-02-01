package dev.privateeye.common.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central logger for PrivateEye application
 */
@Singleton
class Logger @Inject constructor() {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    fun debug(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }
    
    fun info(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }
    
    fun warning(tag: String, message: String) {
        addLog(LogLevel.WARNING, tag, message)
    }
    
    fun error(tag: String, message: String) {
        addLog(LogLevel.ERROR, tag, message)
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
    
    private fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        _logs.value = _logs.value + entry
        
        // Also log to Android Logcat
        when (level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message)
            LogLevel.INFO -> android.util.Log.i(tag, message)
            LogLevel.WARNING -> android.util.Log.w(tag, message)
            LogLevel.ERROR -> android.util.Log.e(tag, message)
        }
    }
}
