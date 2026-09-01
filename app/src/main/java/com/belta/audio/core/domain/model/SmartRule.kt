package com.belta.audio.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RuleField {
    TITLE,
    ARTIST,
    ALBUM,
    GENRE,
    YEAR,
    BITRATE_KBPS,
    SAMPLE_RATE_HZ,
    BIT_DEPTH,
    IS_HI_RES,
    IS_LOSSLESS,
    PLAY_COUNT,
    LAST_PLAYED_DAYS_AGO,
    DATE_ADDED_DAYS_AGO,
    IS_FAVORITE,
    RATING,
    DURATION_SECONDS
}

@Serializable
enum class RuleOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    LESS_THAN,
    IN_RANGE
}

@Serializable
enum class LogicalOperator {
    AND,
    OR
}

@Serializable
enum class SmartPlaylistSortField {
    TITLE,
    ARTIST,
    ALBUM,
    PLAY_COUNT,
    DATE_ADDED,
    LAST_PLAYED,
    BITRATE,
    RANDOM
}

@Serializable
enum class SortDirection {
    ASCENDING,
    DESCENDING
}

@Serializable
data class SmartFilterCondition(
    val field: RuleField,
    val operator: RuleOperator,
    val valueString: String = "",
    val valueNumber: Double = 0.0,
    val valueSecondaryNumber: Double = 0.0,
    val valueBoolean: Boolean = false
)

@Serializable
data class SmartRuleDefinition(
    val matchOperator: LogicalOperator = LogicalOperator.AND,
    val conditions: List<SmartFilterCondition> = emptyList(),
    val maxLimit: Int = 100,
    val sortField: SmartPlaylistSortField = SmartPlaylistSortField.PLAY_COUNT,
    val sortDirection: SortDirection = SortDirection.DESCENDING
)

enum class SmartPlaylistPreset(
    val title: String,
    val description: String,
    val definition: SmartRuleDefinition
) {
    MOST_PLAYED(
        title = "Top Most Played",
        description = "Your most frequently played tracks",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.PLAY_COUNT,
                    operator = RuleOperator.GREATER_THAN,
                    valueNumber = 0.0
                )
            ),
            maxLimit = 50,
            sortField = SmartPlaylistSortField.PLAY_COUNT,
            sortDirection = SortDirection.DESCENDING
        )
    ),
    RECENTLY_ADDED(
        title = "Recently Added",
        description = "Tracks added to device in the last 30 days",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.DATE_ADDED_DAYS_AGO,
                    operator = RuleOperator.LESS_THAN,
                    valueNumber = 30.0
                )
            ),
            maxLimit = 50,
            sortField = SmartPlaylistSortField.DATE_ADDED,
            sortDirection = SortDirection.DESCENDING
        )
    ),
    RECENTLY_PLAYED(
        title = "Recently Played",
        description = "Tracks you listened to recently",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.LAST_PLAYED_DAYS_AGO,
                    operator = RuleOperator.LESS_THAN,
                    valueNumber = 14.0
                )
            ),
            maxLimit = 50,
            sortField = SmartPlaylistSortField.LAST_PLAYED,
            sortDirection = SortDirection.DESCENDING
        )
    ),
    FAVORITES(
        title = "Favorites",
        description = "Tracks marked as favorite",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.IS_FAVORITE,
                    operator = RuleOperator.EQUALS,
                    valueBoolean = true
                )
            ),
            maxLimit = 500,
            sortField = SmartPlaylistSortField.DATE_ADDED,
            sortDirection = SortDirection.DESCENDING
        )
    ),
    HI_RES_MASTERS(
        title = "Hi-Res Master Collection",
        description = "Audiophile lossless and high-resolution tracks (24-bit/96kHz+)",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.OR,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.IS_HI_RES,
                    operator = RuleOperator.EQUALS,
                    valueBoolean = true
                ),
                SmartFilterCondition(
                    field = RuleField.IS_LOSSLESS,
                    operator = RuleOperator.EQUALS,
                    valueBoolean = true
                )
            ),
            maxLimit = 200,
            sortField = SmartPlaylistSortField.BITRATE,
            sortDirection = SortDirection.DESCENDING
        )
    ),
    FORGOTTEN_GEMS(
        title = "Forgotten Gems",
        description = "High play count tracks not played in the past 60 days",
        definition = SmartRuleDefinition(
            matchOperator = LogicalOperator.AND,
            conditions = listOf(
                SmartFilterCondition(
                    field = RuleField.PLAY_COUNT,
                    operator = RuleOperator.GREATER_THAN,
                    valueNumber = 5.0
                ),
                SmartFilterCondition(
                    field = RuleField.LAST_PLAYED_DAYS_AGO,
                    operator = RuleOperator.GREATER_THAN,
                    valueNumber = 60.0
                )
            ),
            maxLimit = 50,
            sortField = SmartPlaylistSortField.PLAY_COUNT,
            sortDirection = SortDirection.DESCENDING
        )
    )
}
