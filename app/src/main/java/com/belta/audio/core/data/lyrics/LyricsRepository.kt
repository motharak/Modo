package com.belta.audio.core.data.lyrics

import android.content.Context
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import com.belta.audio.core.domain.model.LyricLine
import com.belta.audio.core.domain.model.Lyrics
import com.belta.audio.core.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface LyricsRepository {
    suspend fun getLyrics(track: Track): Lyrics
}

class LyricsRepositoryImpl(
    private val context: Context
) : LyricsRepository {

    private val lyricsCacheDir by lazy {
        File(context.cacheDir, "lyrics_cache").apply { if (!exists()) mkdirs() }
    }

    override suspend fun getLyrics(track: Track): Lyrics = withContext(Dispatchers.IO) {
        // 1. Search in local track directory
        val localLyrics = searchLocalFolderLyrics(track)
        if (localLyrics != null && (localLyrics.isSynced || localLyrics.rawPlainLyrics.isNotBlank())) {
            DebugLogger.i(
                LogCategory.TAG_PARSER,
                "LYRICS",
                "Found local LRC file for: ${track.title}"
            )
            return@withContext localLyrics
        }

        // 2. Search local cached LRC from previous downloads
        val cachedLyrics = searchCachedLyrics(track)
        if (cachedLyrics != null) {
            return@withContext cachedLyrics
        }

        // 3. Fetch from free, open LRCLIB API without API key
        val onlineLyrics = fetchFromLrclibApi(track)
        if (onlineLyrics != null) {
            cacheLyricsLocally(track, onlineLyrics)
            return@withContext onlineLyrics
        }

        Lyrics(trackId = track.id, isSynced = false, rawPlainLyrics = "No lyrics found for this track.")
    }

    private fun searchLocalFolderLyrics(track: Track): Lyrics? {
        val audioFile = File(track.path)
        val parentDir = audioFile.parentFile ?: return null

        val baseName = audioFile.nameWithoutExtension
        val candidateFiles = listOf(
            File(parentDir, "$baseName.lrc"),
            File(parentDir, "${track.title}.lrc"),
            File(parentDir, "$baseName.txt"),
            File(parentDir, "${track.title}.txt")
        )

        for (candidate in candidateFiles) {
            if (candidate.exists() && candidate.canRead()) {
                val text = candidate.readText()
                val parsed = parseLrcText(track.id, text)
                if (parsed.isSynced || parsed.rawPlainLyrics.isNotBlank()) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun searchCachedLyrics(track: Track): Lyrics? {
        val cacheKey = getCacheFilename(track)
        val cacheFile = File(lyricsCacheDir, cacheKey)
        if (cacheFile.exists() && cacheFile.canRead()) {
            val text = cacheFile.readText()
            return parseLrcText(track.id, text)
        }
        return null
    }

    private fun cacheLyricsLocally(track: Track, lyrics: Lyrics) {
        try {
            val cacheKey = getCacheFilename(track)
            val cacheFile = File(lyricsCacheDir, cacheKey)
            val content = if (lyrics.isSynced) {
                lyrics.lines.joinToString("\n") { line ->
                    val min = (line.timestampMs / 60000).toInt()
                    val sec = ((line.timestampMs % 60000) / 1000).toInt()
                    val ms = ((line.timestampMs % 1000) / 10).toInt()
                    String.format("[%02d:%02d.%02d]%s", min, sec, ms, line.text)
                }
            } else {
                lyrics.rawPlainLyrics
            }
            cacheFile.writeText(content)
        } catch (_: Exception) {}
    }

    private fun getCacheFilename(track: Track): String {
        val cleanArtist = track.artist.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val cleanTitle = track.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${cleanArtist}_${cleanTitle}.lrc"
    }

    /**
     * Queries https://lrclib.net/api/get for synced karaoke lyrics without API keys.
     */
    private fun fetchFromLrclibApi(track: Track): Lyrics? {
        try {
            val artistEncoded = URLEncoder.encode(track.artist, "UTF-8")
            val titleEncoded = URLEncoder.encode(track.title, "UTF-8")
            val albumEncoded = URLEncoder.encode(track.album, "UTF-8")
            val durationSeconds = (track.durationMs / 1000).toInt()

            val urlString = "https://lrclib.net/api/get?artist_name=$artistEncoded&track_name=$titleEncoded&album_name=$albumEncoded&duration=$durationSeconds"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BeltaAudioPlayer/1.0 ( support@belta.audio )")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val syncedLyrics = json.optString("syncedLyrics", "")
                val plainLyrics = json.optString("plainLyrics", "")

                if (syncedLyrics.isNotBlank()) {
                    DebugLogger.i(
                        LogCategory.TAG_PARSER,
                        "LRCLIB",
                        "Fetched synced lyrics for: ${track.title}"
                    )
                    return parseLrcText(track.id, syncedLyrics)
                } else if (plainLyrics.isNotBlank()) {
                    DebugLogger.i(
                        LogCategory.TAG_PARSER,
                        "LRCLIB",
                        "Fetched plain lyrics for: ${track.title}"
                    )
                    return Lyrics(trackId = track.id, isSynced = false, rawPlainLyrics = plainLyrics)
                }
            }
        } catch (e: Exception) {
            DebugLogger.w(
                LogCategory.TAG_PARSER,
                "LRCLIB",
                "Online lyrics fetch skipped: ${e.localizedMessage}"
            )
        }
        return null
    }

    private fun parseLrcText(trackId: Long, text: String): Lyrics {
        val lines = mutableListOf<LyricLine>()
        val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

        for (line in text.lines()) {
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) (msStr.toLongOrNull() ?: 0L) * 10 else (msStr.toLongOrNull() ?: 0L)
                val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
                val lyricText = match.groupValues[4].trim()
                lines.add(LyricLine(timestampMs = totalMs, text = lyricText))
            }
        }

        return if (lines.isNotEmpty()) {
            Lyrics(trackId = trackId, isSynced = true, lines = lines.sortedBy { it.timestampMs })
        } else {
            Lyrics(trackId = trackId, isSynced = false, rawPlainLyrics = text)
        }
    }
}
