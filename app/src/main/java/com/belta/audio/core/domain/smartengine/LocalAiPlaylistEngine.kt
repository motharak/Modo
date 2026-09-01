package com.belta.audio.core.domain.smartengine

import android.content.Context
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory
import com.belta.audio.core.domain.model.LogicalOperator
import com.belta.audio.core.domain.model.RuleField
import com.belta.audio.core.domain.model.RuleOperator
import com.belta.audio.core.domain.model.SmartFilterCondition
import com.belta.audio.core.domain.model.SmartPlaylistSortField
import com.belta.audio.core.domain.model.SmartRuleDefinition
import com.belta.audio.core.domain.model.SortDirection
import com.belta.audio.core.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class GeneratedAiPlaylist(
    val name: String,
    val description: String,
    val matchedTracks: List<Track>,
    val confidencePercent: Int,
    val reasoningTags: List<String>,
    val ruleDefinition: SmartRuleDefinition
)

data class QuickPromptIdea(
    val title: String,
    val prompt: String,
    val category: String
)

object LocalAiPlaylistEngine {

    val QUICK_PROMPT_IDEAS = listOf(
        QuickPromptIdea("Late Night Drive", "late night synthwave electronic and highway drive", "Driving"),
        QuickPromptIdea("Rainy Day Lo-Fi", "chill lofi rain coffee calm relax peaceful", "Mood"),
        QuickPromptIdea("Hi-Res Audiophile", "high resolution flac alac lossless high bitrate 24bit audiophile", "Quality"),
        QuickPromptIdea("Gym Workout Beast", "high energy fast heavy rock metal bass pump workout", "Activity"),
        QuickPromptIdea("Nostalgic 90s & 2000s", "classic 90s and 2000s vintage rock pop hits", "Decades"),
        QuickPromptIdea("Japanese City Pop", "japanese city pop anime 80s funk groove retro", "International"),
        QuickPromptIdea("Deep Focus & Code", "ambient electronic instrumental focus study minimal", "Productivity"),
        QuickPromptIdea("Acoustic Sunset", "warm acoustic guitar folk singer songwriter calm", "Vibe"),
        QuickPromptIdea("Summer Beach Pop", "upbeat happy summer dance pop positive euphoric", "Party"),
        QuickPromptIdea("Dark Cyberpunk", "dark industrial electronic cyberpunk techno intense", "Cyber")
    )

    private val MOOD_KEYWORDS = mapOf(
        "chill" to listOf("chill", "relax", "calm", "peaceful", "ambient", "soft", "gentle", "slow", "quiet", "sleep", "coffee", "breeze"),
        "energetic" to listOf("energy", "energetic", "pump", "workout", "gym", "hype", "fast", "heavy", "power", "beast", "hard", "intense"),
        "melancholic" to listOf("sad", "melancholy", "melancholic", "cry", "rain", "heartbreak", "slow", "lonely", "dark", "sorrow", "grief", "pain"),
        "driving" to listOf("drive", "driving", "night", "highway", "car", "cruise", "road", "speed", "trip", "drift", "city lights"),
        "happy" to listOf("happy", "positive", "summer", "sun", "fun", "joy", "upbeat", "dance", "party", "good", "sunshine", "smile"),
        "focus" to listOf("focus", "study", "code", "coding", "work", "instrumental", "minimal", "deep", "space", "reading", "thinking"),
        "romantic" to listOf("love", "romantic", "romance", "heart", "passion", "lover", "sweet", "kiss", "forever", "date"),
        "cyberpunk" to listOf("cyberpunk", "synthwave", "retrowave", "neon", "future", "dystopia", "industrial", "glitch")
    )

    private val GENRE_KEYWORDS = mapOf(
        "rock" to listOf("rock", "metal", "punk", "alternative", "grunge", "hard rock", "indie rock", "guitar solo"),
        "electronic" to listOf("electronic", "electro", "edm", "synthwave", "techno", "house", "trance", "dance", "cyberpunk", "synth"),
        "lo-fi" to listOf("lofi", "lo-fi", "chillhop", "beat", "beats", "jazzhop", "vinyl crackle"),
        "pop" to listOf("pop", "dance pop", "synthpop", "chart", "hits"),
        "japanese" to listOf("japanese", "j-pop", "jpop", "city pop", "anime", "j-rock", "japan", "tatsuro", "anison"),
        "k-pop" to listOf("kpop", "k-pop", "korean", "idol"),
        "jazz" to listOf("jazz", "blues", "soul", "funk", "smooth jazz", "swing", "saxophone", "trumpet"),
        "classical" to listOf("classical", "orchestra", "piano", "symphony", "cinematic", "soundtrack", "ost", "violin"),
        "hip-hop" to listOf("hip hop", "hiphop", "rap", "trap", "r&b", "urban", "flow"),
        "acoustic" to listOf("acoustic", "folk", "unplugged", "guitar", "singer", "songwriter", "country")
    )

    private val QUALITY_KEYWORDS = listOf("hires", "hi-res", "lossless", "alac", "flac", "audiophile", "24bit", "24-bit", "high bitrate", "hd", "96khz", "192khz")

    /**
     * Enhanced AI playlist generator with multi-field semantic scoring, lyrics matching, and open online metadata knowledge.
     */
    suspend fun generateSmart(
        prompt: String,
        allTracks: List<Track>,
        context: Context? = null
    ): GeneratedAiPlaylist = withContext(Dispatchers.IO) {
        val cleanPrompt = prompt.trim().lowercase(Locale.ROOT)
        if (cleanPrompt.isEmpty() || allTracks.isEmpty()) {
            return@withContext GeneratedAiPlaylist(
                name = "My Smart Mix",
                description = "All library favorites",
                matchedTracks = allTracks.take(30),
                confidencePercent = 100,
                reasoningTags = listOf("All Tracks", "Dynamic Shuffle"),
                ruleDefinition = SmartRuleDefinition(
                    matchOperator = LogicalOperator.OR,
                    sortField = SmartPlaylistSortField.RANDOM,
                    sortDirection = SortDirection.DESCENDING,
                    maxLimit = 50
                )
            )
        }

        val promptTokens = cleanPrompt.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 1 }

        // 1. Detect Intent, Moods, Genres, Eras, Quality
        val detectedMoods = mutableListOf<String>()
        MOOD_KEYWORDS.forEach { (mood, words) ->
            if (words.any { cleanPrompt.contains(it) }) {
                detectedMoods.add(mood)
            }
        }

        val detectedGenres = mutableListOf<String>()
        GENRE_KEYWORDS.forEach { (genre, words) ->
            if (words.any { cleanPrompt.contains(it) }) {
                detectedGenres.add(genre)
            }
        }

        val isHighQualityRequested = QUALITY_KEYWORDS.any { cleanPrompt.contains(it) }

        // Era extraction
        val detectedDecades = mutableListOf<Int>()
        if (cleanPrompt.contains("70s") || cleanPrompt.contains("1970")) detectedDecades.add(1970)
        if (cleanPrompt.contains("80s") || cleanPrompt.contains("1980")) detectedDecades.add(1980)
        if (cleanPrompt.contains("90s") || cleanPrompt.contains("1990")) detectedDecades.add(1990)
        if (cleanPrompt.contains("2000s") || cleanPrompt.contains("2000")) detectedDecades.add(2000)
        if (cleanPrompt.contains("2010s") || cleanPrompt.contains("2010")) detectedDecades.add(2010)
        if (cleanPrompt.contains("2020s") || cleanPrompt.contains("modern") || cleanPrompt.contains("recent")) detectedDecades.add(2020)

        // 2. Open Online Metadata Association (Keyless MusicBrainz / Open Knowledge lookup)
        val onlineRelatedKeywords = mutableSetOf<String>()
        try {
            if (promptTokens.isNotEmpty() && (detectedGenres.isEmpty() || detectedMoods.isEmpty())) {
                val queryTag = promptTokens.first()
                val url = URL("https://musicbrainz.org/ws/2/tag/?query=${URLEncoder.encode(queryTag, "UTF-8")}&fmt=json&limit=3")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2000
                    readTimeout = 2000
                    setRequestProperty("User-Agent", "BeltaAudioPlayer/1.0 (info@belta.audio)")
                }
                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val tags = json.optJSONArray("tags")
                    if (tags != null) {
                        for (i in 0 until minOf(tags.length(), 5)) {
                            val name = tags.getJSONObject(i).optString("name").lowercase(Locale.ROOT)
                            if (name.isNotBlank()) onlineRelatedKeywords.add(name)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Score Every Track in Library (with tag, lyrics, path, spec matching)
        val lyricsCacheDir = context?.let { File(it.cacheDir, "lyrics_cache") }

        val scoredTracks = allTracks.map { track ->
            var score = 0
            val titleLower = track.title.lowercase(Locale.ROOT)
            val artistLower = track.artist.lowercase(Locale.ROOT)
            val albumLower = track.album.lowercase(Locale.ROOT)
            val genreLower = (track.genre ?: "").lowercase(Locale.ROOT)
            val pathLower = track.path.lowercase(Locale.ROOT)

            // Direct Token Match (+35 for title/artist, +25 for genre, +20 for album/path)
            for (token in promptTokens) {
                if (titleLower.contains(token)) score += 35
                if (artistLower.contains(token)) score += 35
                if (genreLower.contains(token)) score += 25
                if (albumLower.contains(token)) score += 20
                if (pathLower.contains(token)) score += 15
            }

            // Genre & Subgenre Semantic Match (+30)
            for (genre in detectedGenres) {
                val keywords = GENRE_KEYWORDS[genre] ?: emptyList()
                if (keywords.any { genreLower.contains(it) || pathLower.contains(it) || titleLower.contains(it) }) {
                    score += 30
                }
            }

            // Mood & Vibe Match (+25)
            for (mood in detectedMoods) {
                val keywords = MOOD_KEYWORDS[mood] ?: emptyList()
                if (keywords.any { genreLower.contains(it) || titleLower.contains(it) || albumLower.contains(it) || pathLower.contains(it) }) {
                    score += 25
                }
            }

            // Online Knowledge / Tag Match (+20)
            for (kw in onlineRelatedKeywords) {
                if (titleLower.contains(kw) || artistLower.contains(kw) || genreLower.contains(kw)) {
                    score += 20
                }
            }

            // Lyrics Semantic Search in local lyrics cache (+25)
            if (lyricsCacheDir != null && lyricsCacheDir.exists()) {
                val lrcFile = File(lyricsCacheDir, "${track.artist}_${track.title}.lrc".replace(Regex("[^a-zA-Z0-9._-]"), "_"))
                if (lrcFile.exists()) {
                    try {
                        val lyricsText = lrcFile.readText().lowercase(Locale.ROOT)
                        for (token in promptTokens) {
                            if (lyricsText.contains(token)) score += 25
                        }
                        for (mood in detectedMoods) {
                            val words = MOOD_KEYWORDS[mood] ?: emptyList()
                            if (words.any { lyricsText.contains(it) }) score += 15
                        }
                    } catch (_: Exception) {}
                }
            }

            // Hi-Res Audio / Quality Match (+35)
            if (isHighQualityRequested) {
                if (track.mimeType.contains("flac", ignoreCase = true) ||
                    track.mimeType.contains("alac", ignoreCase = true) ||
                    track.path.endsWith(".flac", ignoreCase = true) ||
                    track.path.endsWith(".alac", ignoreCase = true) ||
                    (track.bitDepth != null && track.bitDepth >= 24) ||
                    track.bitrateKbps >= 320
                ) {
                    score += 35
                } else {
                    score -= 10
                }
            }

            // Era / Decade Match (+25)
            if (detectedDecades.isNotEmpty() && track.year != null && track.year > 0) {
                if (detectedDecades.any { decade -> track.year in decade..(decade + 9) }) {
                    score += 25
                }
            }

            // Engagement Bonus
            if (track.playCount > 0) {
                score += minOf(track.playCount * 2, 10)
            }
            if (track.isFavorite) {
                score += 5
            }

            Pair(track, score)
        }

        // 4. Filter and Sort
        val relevantMatches = scoredTracks
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        val finalTracks = if (relevantMatches.size >= 5) {
            relevantMatches.take(50)
        } else {
            val combined = (relevantMatches + allTracks.shuffled()).distinctBy { it.id }
            combined.take(30)
        }

        // 5. Reasoning Tags
        val reasoningTags = mutableListOf<String>()
        if (detectedMoods.isNotEmpty()) {
            reasoningTags.addAll(detectedMoods.map { it.replaceFirstChar { char -> char.uppercase() } + " Mood" })
        }
        if (detectedGenres.isNotEmpty()) {
            reasoningTags.addAll(detectedGenres.map { it.replaceFirstChar { char -> char.uppercase() } })
        }
        if (isHighQualityRequested) {
            reasoningTags.add("Hi-Res Lossless Audio")
        }
        if (detectedDecades.isNotEmpty()) {
            reasoningTags.addAll(detectedDecades.map { "${it}s Era" })
        }
        if (onlineRelatedKeywords.isNotEmpty()) {
            reasoningTags.add("Open Metadata Match")
        }
        if (reasoningTags.isEmpty()) {
            reasoningTags.add("Dynamic Semantic Match")
            reasoningTags.add("${finalTracks.size} Tracks")
        }

        val capitalizedPrompt = prompt.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        val playlistName = "$capitalizedPrompt Mix"
        val playlistDesc = "AI Generated for '$prompt' • ${reasoningTags.joinToString(" • ")}"

        val conditions = mutableListOf<SmartFilterCondition>()
        for (token in promptTokens.take(3)) {
            conditions.add(SmartFilterCondition(RuleField.TITLE, RuleOperator.CONTAINS, token))
            conditions.add(SmartFilterCondition(RuleField.ARTIST, RuleOperator.CONTAINS, token))
            conditions.add(SmartFilterCondition(RuleField.GENRE, RuleOperator.CONTAINS, token))
        }

        val ruleDefinition = SmartRuleDefinition(
            matchOperator = LogicalOperator.OR,
            conditions = conditions,
            sortField = if (isHighQualityRequested) SmartPlaylistSortField.BITRATE else SmartPlaylistSortField.RANDOM,
            sortDirection = SortDirection.DESCENDING,
            maxLimit = 50
        )

        val confidence = minOf(98, maxOf(75, 82 + relevantMatches.size * 2))

        DebugLogger.i(
            LogCategory.DATABASE,
            "LOCAL_AI",
            "Generated smart playlist '$playlistName' with ${finalTracks.size} tracks (${confidence}% confidence)"
        )

        GeneratedAiPlaylist(
            name = playlistName,
            description = playlistDesc,
            matchedTracks = finalTracks,
            confidencePercent = confidence,
            reasoningTags = reasoningTags,
            ruleDefinition = ruleDefinition
        )
    }

    // Synchronous fallback wrapper
    fun generate(prompt: String, allTracks: List<Track>): GeneratedAiPlaylist {
        return kotlinx.coroutines.runBlocking {
            generateSmart(prompt, allTracks)
        }
    }
}
