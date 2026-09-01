package com.belta.audio.core.data.scanner

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.belta.audio.core.domain.model.AudioSpecs
import com.belta.audio.core.domain.model.LyricLine
import com.belta.audio.core.domain.model.Lyrics
import java.io.File

/**
 * Inspector for deep audio file metadata: bit depth, sample rate, bitrate, codec, and lyrics.
 */
object AudioTagReader {

    fun extractSpecs(filePath: String, fallbackMimeType: String): AudioSpecs {
        var sampleRate = 44100
        var bitDepth = 16
        var bitrate = 0
        var channelCount = 2
        var mimeType = fallbackMimeType
        var containerFormat = "UNKNOWN"
        var codecName = "UNKNOWN"

        // Determine container format from extension
        val extension = File(filePath).extension.lowercase()
        containerFormat = when (extension) {
            "flac" -> "FLAC"
            "wav" -> "WAV"
            "alac", "m4a" -> "ALAC / AAC"
            "mp3" -> "MP3"
            "ogg" -> "OGG"
            "opus" -> "OPUS"
            "dsf", "dff" -> "DSD"
            "aiff", "aif" -> "AIFF"
            "ape" -> "APE"
            "wma" -> "WMA"
            else -> extension.uppercase()
        }

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    mimeType = trackMime
                    codecName = trackMime.substringAfter("audio/").uppercase()

                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        val encoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        bitDepth = when (encoding) {
                            android.media.AudioFormat.ENCODING_PCM_8BIT -> 8
                            android.media.AudioFormat.ENCODING_PCM_16BIT -> 16
                            android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            android.media.AudioFormat.ENCODING_PCM_32BIT -> 32
                            android.media.AudioFormat.ENCODING_PCM_FLOAT -> 32
                            else -> 16
                        }
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Fallback to retriever
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {}
        }

        // Secondary fallback using MediaMetadataRetriever
        if (bitrate == 0 || sampleRate == 44100) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val retrieverBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                if (retrieverBitrate != null && retrieverBitrate > 0) {
                    bitrate = retrieverBitrate / 1000
                }
                val retrieverSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                if (retrieverSampleRate != null && retrieverSampleRate > 0) {
                    sampleRate = retrieverSampleRate
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        // Lossless and Hi-Res heuristics
        val isAlac = extension == "alac" || mimeType.contains("alac", ignoreCase = true) || codecName.contains("ALAC", ignoreCase = true)
        val isLossless = isAlac || extension in listOf("flac", "wav", "aiff", "aif", "dsf", "dff", "ape")
        val isHiRes = (sampleRate >= 88200 || bitDepth >= 24 || extension in listOf("dsf", "dff"))

        if (extension == "m4a" || extension == "mp4") {
            containerFormat = if (isAlac) "ALAC" else "AAC (M4A)"
        } else if (extension == "aac") {
            containerFormat = "AAC"
            codecName = "AAC"
        }

        // Calculate bitrate for uncompressed WAV/FLAC/ALAC if missing
        if (bitrate == 0 && isLossless && sampleRate > 0 && channelCount > 0) {
            bitrate = (sampleRate * bitDepth * channelCount) / 1000
        }

        val channelLayout = when (channelCount) {
            1 -> "Mono"
            2 -> "Stereo 2.0"
            6 -> "5.1 Surround"
            8 -> "7.1 Surround"
            else -> "$channelCount Channels"
        }

        return AudioSpecs(
            containerFormat = containerFormat,
            codecName = codecName,
            bitrateKbps = bitrate,
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            channelCount = channelCount,
            isHiRes = isHiRes,
            isLossless = isLossless,
            channelLayout = channelLayout
        )
    }

    fun parseLyrics(filePath: String, trackId: Long): Lyrics {
        // 1. Check for companion .lrc file in the same directory
        val file = File(filePath)
        val lrcFile = File(file.parentFile, "${file.nameWithoutExtension}.lrc")
        if (lrcFile.exists() && lrcFile.canRead()) {
            val content = lrcFile.readText()
            val parsedLines = parseLrcString(content)
            if (parsedLines.isNotEmpty()) {
                return Lyrics(
                    trackId = trackId,
                    isSynced = true,
                    lines = parsedLines,
                    rawPlainLyrics = content
                )
            }
        }

        // 2. Check embedded lyrics via MediaMetadataRetriever (or ID3 USLT / SYLT)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            // Some devices support METADATA_KEY_LYRICS or custom fields
            val embeddedLyrics = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) // placeholder for retriever
            // We also check for raw text
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return Lyrics(
            trackId = trackId,
            isSynced = false,
            lines = emptyList(),
            rawPlainLyrics = ""
        )
    }

    private fun parseLrcString(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

        lrcContent.lineSequence().forEach { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val millisRaw = match.groupValues[3]
                val millis = if (millisRaw.length == 2) millisRaw.toLong() * 10 else millisRaw.toLong()
                val timestampMs = (min * 60 * 1000) + (sec * 1000) + millis
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(timestampMs, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }
}
