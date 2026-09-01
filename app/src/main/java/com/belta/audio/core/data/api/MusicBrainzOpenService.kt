package com.belta.audio.core.data.api

import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ArtistMetadata(
    val country: String? = null,
    val tags: List<String> = emptyList(),
    val beginYear: String? = null
)

object MusicBrainzOpenService {

    private val cache = mutableMapOf<String, ArtistMetadata>()

    /**
     * Queries the free, open MusicBrainz API without an API key.
     * Endpoint: https://musicbrainz.org/ws/2/artist/?query=artist:{name}&fmt=json
     */
    suspend fun getArtistMetadata(artistName: String): ArtistMetadata = withContext(Dispatchers.IO) {
        val normalized = artistName.trim().lowercase()
        cache[normalized]?.let { return@withContext it }

        if (normalized.isEmpty() || normalized == "unknown" || normalized == "unknown artist") {
            return@withContext ArtistMetadata()
        }

        try {
            val encodedQuery = URLEncoder.encode(artistName.trim(), "UTF-8")
            val urlString = "https://musicbrainz.org/ws/2/artist/?query=artist:$encodedQuery&fmt=json&limit=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BeltaAudioPlayer/1.0 ( support@belta.audio )")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val artistsArray = json.optJSONArray("artists")
                if (artistsArray != null && artistsArray.length() > 0) {
                    val firstArtist = artistsArray.getJSONObject(0)
                    val country = firstArtist.optString("country", null)
                    val lifeSpan = firstArtist.optJSONObject("life-span")
                    val begin = lifeSpan?.optString("begin", null)?.take(4)

                    val tagsList = mutableListOf<String>()
                    val tagsArray = firstArtist.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            val tagObj = tagsArray.getJSONObject(i)
                            val tagName = tagObj.optString("name", "")
                            if (tagName.isNotBlank()) {
                                tagsList.add(tagName.lowercase())
                            }
                        }
                    }

                    val meta = ArtistMetadata(country = country, tags = tagsList, beginYear = begin)
                    cache[normalized] = meta
                    DebugLogger.d(
                        LogCategory.CLOUD_SYNC,
                        "MUSICBRAINZ",
                        "Fetched metadata for $artistName: Country=${country ?: "N/A"}, Tags=${tagsList.take(3)}"
                    )
                    return@withContext meta
                }
            }
        } catch (e: Exception) {
            DebugLogger.w(
                LogCategory.CLOUD_SYNC,
                "MUSICBRAINZ",
                "Open API query skipped for $artistName: ${e.localizedMessage}"
            )
        }

        ArtistMetadata()
    }
}
