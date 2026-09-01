package com.belta.audio.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.belta.audio.core.domain.model.Track
import com.belta.audio.ui.screens.home.TrackListItem

enum class SearchFilterOption {
    ALL,
    HI_RES_ONLY,
    LOSSLESS,
    FLAC,
    MP3,
    FAVORITES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    allTracks: List<Track>,
    onTrackClick: (Track, List<Track>) -> Unit,
    onEditTagClick: (Track) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilterOption.ALL) }

    val filteredTracks = allTracks.filter { track ->
        val matchesQuery = query.isBlank() ||
                track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true) ||
                track.genre.contains(query, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            SearchFilterOption.ALL -> true
            SearchFilterOption.HI_RES_ONLY -> track.isHiRes
            SearchFilterOption.LOSSLESS -> track.isLossless
            SearchFilterOption.FLAC -> track.path.endsWith(".flac", ignoreCase = true)
            SearchFilterOption.MP3 -> track.path.endsWith(".mp3", ignoreCase = true)
            SearchFilterOption.FAVORITES -> track.isFavorite
        }

        matchesQuery && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search songs, artists, albums...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.ALL,
                        onClick = { selectedFilter = SearchFilterOption.ALL },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.HI_RES_ONLY,
                        onClick = { selectedFilter = SearchFilterOption.HI_RES_ONLY },
                        label = { Text("Hi-Res Audio") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.LOSSLESS,
                        onClick = { selectedFilter = SearchFilterOption.LOSSLESS },
                        label = { Text("Lossless") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.FLAC,
                        onClick = { selectedFilter = SearchFilterOption.FLAC },
                        label = { Text("FLAC") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.MP3,
                        onClick = { selectedFilter = SearchFilterOption.MP3 },
                        label = { Text("MP3") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == SearchFilterOption.FAVORITES,
                        onClick = { selectedFilter = SearchFilterOption.FAVORITES },
                        label = { Text("Favorites") }
                    )
                }
            }

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isNotBlank()) "No matching audio tracks found" else "Type a search term above",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            onClick = { onTrackClick(track, filteredTracks) },
                            onEditTagClick = { onEditTagClick(track) }
                        )
                    }
                }
            }
        }
    }
}
