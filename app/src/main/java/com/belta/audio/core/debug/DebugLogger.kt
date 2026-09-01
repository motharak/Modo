package com.belta.audio.core.debug

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

enum class LogCategory {
    AUDIO_ENGINE,
    MEDIA_STORE,
    DSP_EQUALIZER,
    DATABASE,
    CLOUD_SYNC,
    TAG_PARSER,
    SYSTEM
}

data class LogEntry(
    val id: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val category: LogCategory,
    val tag: String,
    val message: String,
    val details: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

data class AudioHardwareInfo(
    val deviceModel: String,
    val androidVersion: String,
    val nativeSampleRate: String,
    val nativeBufferSize: String,
    val hasLowLatencyAudio: Boolean,
    val hasProAudio: Boolean,
    val isPcmFloatSupported: Boolean
)

object DebugLogger {

    private const val MAX_LOGS = 1000
    private var nextId = 1L

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(
        level: LogLevel,
        category: LogCategory,
        tag: String,
        message: String,
        details: String? = null
    ) {
        val entry = LogEntry(
            id = nextId++,
            level = level,
            category = category,
            tag = tag,
            message = message,
            details = details
        )

        // Log to system Logcat
        val fullMessage = if (details != null) "$message | Details: $details" else message
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, fullMessage)
            LogLevel.DEBUG -> Log.d(tag, fullMessage)
            LogLevel.INFO -> Log.i(tag, fullMessage)
            LogLevel.WARN -> Log.w(tag, fullMessage)
            LogLevel.ERROR -> Log.e(tag, fullMessage)
        }

        // Append to in-app real-time stream
        _logs.update { current ->
            val updated = current + entry
            if (updated.size > MAX_LOGS) {
                updated.drop(updated.size - MAX_LOGS)
            } else {
                updated
            }
        }
    }

    fun d(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.DEBUG, category, tag, message, details)
    }

    fun i(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.INFO, category, tag, message, details)
    }

    fun w(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.WARN, category, tag, message, details)
    }

    fun e(category: LogCategory, tag: String, message: String, details: String? = null) {
        log(LogLevel.ERROR, category, tag, message, details)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun getAudioHardwareInfo(context: Context): AudioHardwareInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val sampleRate = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "Unknown"
        val bufferSize = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "Unknown"
        val pm = context.packageManager
        val hasLowLatency = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        val hasProAudio = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)

        return AudioHardwareInfo(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            nativeSampleRate = "$sampleRate Hz",
            nativeBufferSize = "$bufferSize frames",
            hasLowLatencyAudio = hasLowLatency,
            hasProAudio = hasProAudio,
            isPcmFloatSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        )
    }
}
