package com.belta.audio

import com.belta.audio.core.domain.model.LogicalOperator
import com.belta.audio.core.domain.model.RuleField
import com.belta.audio.core.domain.model.RuleOperator
import com.belta.audio.core.domain.model.SmartFilterCondition
import com.belta.audio.core.domain.model.SmartPlaylistPreset
import com.belta.audio.core.domain.model.SmartPlaylistSortField
import com.belta.audio.core.domain.model.SmartRuleDefinition
import com.belta.audio.core.domain.model.SortDirection
import com.belta.audio.core.domain.model.Track
import com.belta.audio.core.domain.smartengine.SmartPlaylistEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartPlaylistEngineTest {

    private val sampleTracks = listOf(
        Track(
            id = 1,
            title = "Track A",
            artist = "Artist One",
            album = "Album Alpha",
            durationMs = 200000,
            path = "/storage/emulated/0/Music/trackA.flac",
            size = 50000000,
            mimeType = "audio/flac",
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            bitrateKbps = 5800,
            sampleRateHz = 192000,
            bitDepth = 24,
            isHiRes = true,
            isLossless = true,
            playCount = 45,
            isFavorite = true
        ),
        Track(
            id = 2,
            title = "Track B",
            artist = "Artist Two",
            album = "Album Beta",
            durationMs = 180000,
            path = "/storage/emulated/0/Music/trackB.mp3",
            size = 8000000,
            mimeType = "audio/mp3",
            dateAdded = (System.currentTimeMillis() / 1000) - (86400 * 40), // 40 days ago
            dateModified = System.currentTimeMillis() / 1000,
            bitrateKbps = 320,
            sampleRateHz = 44100,
            bitDepth = 16,
            isHiRes = false,
            isLossless = false,
            playCount = 10,
            isFavorite = false
        ),
        Track(
            id = 3,
            title = "Track C",
            artist = "Artist One",
            album = "Album Alpha",
            durationMs = 240000,
            path = "/storage/emulated/0/Music/trackC.flac",
            size = 30000000,
            mimeType = "audio/flac",
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            bitrateKbps = 1411,
            sampleRateHz = 44100,
            bitDepth = 16,
            isHiRes = false,
            isLossless = true,
            playCount = 80,
            isFavorite = true
        )
    )

    @Test
    fun testMostPlayedPreset() {
        val results = SmartPlaylistEngine.evaluate(sampleTracks, SmartPlaylistPreset.MOST_PLAYED.definition)
        assertEquals(3, results.size)
        assertEquals("Track C", results[0].title) // 80 plays
        assertEquals("Track A", results[1].title) // 45 plays
        assertEquals("Track B", results[2].title) // 10 plays
    }

    @Test
    fun testHiResMastersPreset() {
        val results = SmartPlaylistEngine.evaluate(sampleTracks, SmartPlaylistPreset.HI_RES_MASTERS.definition)
        assertEquals(2, results.size) // Track A (Hi-Res FLAC) and Track C (Lossless FLAC)
        assertTrue(results.any { it.title == "Track A" })
        assertTrue(results.any { it.title == "Track C" })
    }

    @Test
    fun testFavoritesPreset() {
        val results = SmartPlaylistEngine.evaluate(sampleTracks, SmartPlaylistPreset.FAVORITES.definition)
        assertEquals(2, results.size)
        assertTrue(results.all { it.isFavorite })
    }

    @Test
    fun testCustomRuleArtistAndBitrate() {
        val customRule = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.ARTIST,
                    operator = RuleOperator.EQUALS,
                    valueString = "Artist One"
                ),
                SmartFilterCondition(
                    field = RuleField.BITRATE_KBPS,
                    operator = RuleOperator.GREATER_THAN,
                    valueNumber = 2000.0
                )
            ),
            sortField = SmartPlaylistSortField.TITLE,
            sortDirection = SortDirection.ASCENDING
        )

        val results = SmartPlaylistEngine.evaluate(sampleTracks, customRule)
        assertEquals(1, results.size)
        assertEquals("Track A", results[0].title)
    }
}
