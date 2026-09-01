package com.belta.audio.core.domain.smartengine

import com.belta.audio.core.domain.model.LogicalOperator
import com.belta.audio.core.domain.model.RuleField
import com.belta.audio.core.domain.model.RuleOperator
import com.belta.audio.core.domain.model.SmartFilterCondition
import com.belta.audio.core.domain.model.SmartPlaylistSortField
import com.belta.audio.core.domain.model.SmartRuleDefinition
import com.belta.audio.core.domain.model.SortDirection
import com.belta.audio.core.domain.model.Track
import java.util.concurrent.TimeUnit

object SmartPlaylistEngine {

    fun evaluate(allTracks: List<Track>, ruleDefinition: SmartRuleDefinition): List<Track> {
        val filtered = allTracks.filter { track ->
            evaluateTrack(track, ruleDefinition)
        }

        val sorted = when (ruleDefinition.sortField) {
            SmartPlaylistSortField.TITLE -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.title.lowercase() }
                } else {
                    filtered.sortedByDescending { it.title.lowercase() }
                }
            }
            SmartPlaylistSortField.ARTIST -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.artist.lowercase() }
                } else {
                    filtered.sortedByDescending { it.artist.lowercase() }
                }
            }
            SmartPlaylistSortField.ALBUM -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.album.lowercase() }
                } else {
                    filtered.sortedByDescending { it.album.lowercase() }
                }
            }
            SmartPlaylistSortField.PLAY_COUNT -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.playCount }
                } else {
                    filtered.sortedByDescending { it.playCount }
                }
            }
            SmartPlaylistSortField.DATE_ADDED -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.dateAdded }
                } else {
                    filtered.sortedByDescending { it.dateAdded }
                }
            }
            SmartPlaylistSortField.LAST_PLAYED -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.lastPlayedTimestamp }
                } else {
                    filtered.sortedByDescending { it.lastPlayedTimestamp }
                }
            }
            SmartPlaylistSortField.BITRATE -> {
                if (ruleDefinition.sortDirection == SortDirection.ASCENDING) {
                    filtered.sortedBy { it.bitrateKbps }
                } else {
                    filtered.sortedByDescending { it.bitrateKbps }
                }
            }
            SmartPlaylistSortField.RANDOM -> filtered.shuffled()
        }

        return if (ruleDefinition.maxLimit > 0) {
            sorted.take(ruleDefinition.maxLimit)
        } else {
            sorted
        }
    }

    private fun evaluateTrack(track: Track, ruleDefinition: SmartRuleDefinition): Boolean {
        if (ruleDefinition.conditions.isEmpty()) return true

        val results = ruleDefinition.conditions.map { condition ->
            evaluateCondition(track, condition)
        }

        return when (ruleDefinition.matchOperator) {
            LogicalOperator.AND -> results.all { it }
            LogicalOperator.OR -> results.any { it }
        }
    }

    private fun evaluateCondition(track: Track, condition: SmartFilterCondition): Boolean {
        val now = System.currentTimeMillis()

        return when (condition.field) {
            RuleField.TITLE -> matchString(track.title, condition.operator, condition.valueString)
            RuleField.ARTIST -> matchString(track.artist, condition.operator, condition.valueString)
            RuleField.ALBUM -> matchString(track.album, condition.operator, condition.valueString)
            RuleField.GENRE -> matchString(track.genre, condition.operator, condition.valueString)
            RuleField.YEAR -> matchNumber(track.year.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.BITRATE_KBPS -> matchNumber(track.bitrateKbps.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.SAMPLE_RATE_HZ -> matchNumber(track.sampleRateHz.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.BIT_DEPTH -> matchNumber(track.bitDepth.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.IS_HI_RES -> track.isHiRes == condition.valueBoolean
            RuleField.IS_LOSSLESS -> track.isLossless == condition.valueBoolean
            RuleField.PLAY_COUNT -> matchNumber(track.playCount.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.IS_FAVORITE -> track.isFavorite == condition.valueBoolean
            RuleField.RATING -> matchNumber(track.rating.toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.DURATION_SECONDS -> matchNumber((track.durationMs / 1000).toDouble(), condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            RuleField.LAST_PLAYED_DAYS_AGO -> {
                if (track.lastPlayedTimestamp == 0L) {
                    // Never played -> infinite days ago
                    condition.operator == RuleOperator.GREATER_THAN
                } else {
                    val daysAgo = TimeUnit.MILLISECONDS.toDays(now - track.lastPlayedTimestamp).toDouble()
                    matchNumber(daysAgo, condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
                }
            }
            RuleField.DATE_ADDED_DAYS_AGO -> {
                val dateAddedMs = if (track.dateAdded < 10000000000L) track.dateAdded * 1000 else track.dateAdded
                val daysAgo = TimeUnit.MILLISECONDS.toDays(now - dateAddedMs).toDouble()
                matchNumber(daysAgo, condition.operator, condition.valueNumber, condition.valueSecondaryNumber)
            }
        }
    }

    private fun matchString(source: String, operator: RuleOperator, target: String): Boolean {
        val s = source.lowercase()
        val t = target.lowercase()
        return when (operator) {
            RuleOperator.EQUALS -> s == t
            RuleOperator.NOT_EQUALS -> s != t
            RuleOperator.CONTAINS -> s.contains(t)
            RuleOperator.NOT_CONTAINS -> !s.contains(t)
            RuleOperator.STARTS_WITH -> s.startsWith(t)
            RuleOperator.ENDS_WITH -> s.endsWith(t)
            else -> false
        }
    }

    private fun matchNumber(source: Double, operator: RuleOperator, target: Double, secondaryTarget: Double): Boolean {
        return when (operator) {
            RuleOperator.EQUALS -> source == target
            RuleOperator.NOT_EQUALS -> source != target
            RuleOperator.GREATER_THAN -> source > target
            RuleOperator.LESS_THAN -> source < target
            RuleOperator.IN_RANGE -> source >= target && source <= secondaryTarget
            else -> false
        }
    }
}
