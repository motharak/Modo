package com.belta.audio.ui.screens.player

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.belta.audio.core.domain.model.Lyrics
import com.belta.audio.core.domain.model.PlaybackState
import com.belta.audio.core.domain.model.RepeatMode
import com.belta.audio.core.domain.model.ShuffleMode
import com.belta.audio.core.domain.model.Track
import com.belta.audio.ui.components.AudioSpecsBadge
import com.belta.audio.ui.components.CyberHudView
import com.belta.audio.ui.components.DefaultArtwork
import com.belta.audio.ui.components.RetroCassetteView
import com.belta.audio.ui.components.SyncedLyricsView
import com.belta.audio.ui.components.VinylRecordView
import com.belta.audio.ui.components.progressbar.CustomMusicProgressBar
import com.belta.audio.ui.screens.player.styles.PlayerVisualTheme
import com.belta.audio.ui.screens.player.styles.ProgressBarStyle

data class AlbumGroup(
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val tracks: List<Track>
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    lyrics: Lyrics,
    allTracks: List<Track> = emptyList(),
    onPlayTrackFromList: (Track, List<Track>) -> Unit = { _, _ -> },
    onCollapse: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onEqualizerClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("belta_prefs", Context.MODE_PRIVATE) }

    val savedThemeName = remember { sharedPreferences.getString("player_visual_theme", PlayerVisualTheme.MODERN_GLASS.name) }
    var playerTheme by remember {
        mutableStateOf(PlayerVisualTheme.entries.find { it.name == savedThemeName } ?: PlayerVisualTheme.MODERN_GLASS)
    }

    val savedProgressStyleName = remember { sharedPreferences.getString("player_progress_bar_style", ProgressBarStyle.DYNAMIC_WAVEFORM.name) }
    var progressBarStyle by remember {
        mutableStateOf(ProgressBarStyle.entries.find { it.name == savedProgressStyleName } ?: ProgressBarStyle.DYNAMIC_WAVEFORM)
    }

    var showStyleMenu by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var showAlbumSheet by remember { mutableStateOf(false) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var showAllAlbumsSheet by rememberSaveable { mutableStateOf(false) }
    var albumSearchQuery by remember { mutableStateOf("") }
    var isLockScreenActive by rememberSaveable { mutableStateOf(false) }

    val albumsList = remember(allTracks) {
        allTracks
            .filter { it.album.isNotBlank() }
            .groupBy { it.album.trim() }
            .map { (albumName, tracksInAlbum) ->
                AlbumGroup(
                    name = albumName,
                    artist = tracksInAlbum.firstOrNull()?.artist.orEmpty(),
                    artworkUri = tracksInAlbum.firstOrNull()?.artworkUri,
                    tracks = tracksInAlbum
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    if (isLockScreenActive) {
        BatterySaverLockScreen(
            playbackState = playbackState,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            onDismiss = { isLockScreenActive = false }
        )
        return
    }

    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val progressFraction = (playbackState.currentPositionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    val albumTracks = remember(allTracks, track.albumId, track.album) {
        allTracks.filter { it.albumId == track.albumId || it.album.equals(track.album, ignoreCase = true) }
            .sortedBy { if (it.trackNumber > 0) it.trackNumber else 1 }
    }

    val artistTracks = remember(allTracks, track.artistId, track.artist) {
        allTracks.filter { it.artistId == track.artistId || it.artist.equals(track.artist, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
    }

    val dismissScope = rememberCoroutineScope()
    var dragDismissOffsetY by remember { mutableFloatStateOf(0f) }
    val animDismissOffsetY = remember { Animatable(0f) }
    val currentOnCollapse by rememberUpdatedState(onCollapse)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse Player")
                    }
                },
                modifier = Modifier.pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var totalY = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break

                            if (change.changedToUp()) {
                                val velY = velocityTracker.calculateVelocity().y
                                val currentOffset = dragDismissOffsetY
                                dragDismissOffsetY = 0f
                                if (currentOffset > 90.dp.toPx() || velY > 600.dp.toPx()) {
                                    dismissScope.launch {
                                        animDismissOffsetY.snapTo(currentOffset)
                                        animDismissOffsetY.animateTo(1000f, tween(180, easing = FastOutSlowInEasing))
                                        currentOnCollapse()
                                        animDismissOffsetY.snapTo(0f)
                                    }
                                } else {
                                    dismissScope.launch {
                                        animDismissOffsetY.snapTo(currentOffset)
                                        animDismissOffsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 450f))
                                    }
                                }
                                break
                            }

                            val dragAmount = change.positionChange()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            totalY += dragAmount.y
                            dragDismissOffsetY = totalY.coerceAtLeast(0f)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isLockScreenActive = true }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Pocket / Battery Saver Lock"
                        )
                    }
                    IconButton(onClick = { showLyricsView = !showLyricsView }) {
                        Icon(
                            Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyricsView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showStyleMenu = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Change Visual Style")
                    }
                    DropdownMenu(
                        expanded = showStyleMenu,
                        onDismissRequest = { showStyleMenu = false }
                    ) {
                        Text(
                            text = "Visual Player Themes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        PlayerVisualTheme.entries.forEach { theme ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = theme.displayName,
                                        fontWeight = if (playerTheme == theme) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    playerTheme = theme
                                    sharedPreferences.edit().putString("player_visual_theme", theme.name).apply()
                                    showStyleMenu = false
                                }
                            )
                        }

                        androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Progress Bar Styles",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        ProgressBarStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = style.displayName,
                                        fontWeight = if (progressBarStyle == style) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    progressBarStyle = style
                                    sharedPreferences.edit().putString("player_progress_bar_style", style.name).apply()
                                    showStyleMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.offset { IntOffset(0, (animDismissOffsetY.value + dragDismissOffsetY).coerceAtLeast(0f).roundToInt()) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                var gestureDragX by remember { mutableFloatStateOf(0f) }
                var gestureDragY by remember { mutableFloatStateOf(0f) }
                val animOffsetX = remember { Animatable(0f) }
                val animOffsetY = remember { Animatable(0f) }
                val swipeScope = rememberCoroutineScope()
                var isSwipingTrack by remember { mutableStateOf(false) }

                // Always ensure artwork card is centered firmly at (0,0) whenever track changes unless actively swiping
                LaunchedEffect(track.id) {
                    if (!isSwipingTrack) {
                        gestureDragX = 0f
                        gestureDragY = 0f
                        animOffsetX.snapTo(0f)
                        animOffsetY.snapTo(0f)
                    }
                }

                // Capture latest states to guarantee pointerInput always accesses fresh values
                val currentTrackState by rememberUpdatedState(track)
                val currentAlbumsListState by rememberUpdatedState(albumsList)
                val currentOnNextClick by rememberUpdatedState(onNextClick)
                val currentOnPreviousClick by rememberUpdatedState(onPreviousClick)
                val currentOnPlayTrackFromList by rememberUpdatedState(onPlayTrackFromList)

                // Synchronous active album index tracker to allow rapid successive swipes through all albums
                var activeAlbumIndex by rememberSaveable { mutableIntStateOf(-1) }

                LaunchedEffect(track.id, track.album) {
                    val foundIdx = albumsList.indexOfFirst {
                        it.name.equals(track.album.trim(), ignoreCase = true) ||
                        it.tracks.any { t -> t.id == track.id }
                    }
                    if (foundIdx != -1) {
                        activeAlbumIndex = foundIdx
                    }
                }

                val skipToNextAlbum: () -> Unit = {
                    val list = currentAlbumsListState
                    if (list.isNotEmpty()) {
                        val curTrack = currentTrackState
                        val foundIdx = list.indexOfFirst {
                            it.name.equals(curTrack.album.trim(), ignoreCase = true) ||
                            it.tracks.any { t -> t.id == curTrack.id }
                        }
                        val baseIdx = if (activeAlbumIndex in list.indices) activeAlbumIndex else foundIdx
                        val nextIdx = if (baseIdx in 0 until list.size - 1) baseIdx + 1 else 0
                        activeAlbumIndex = nextIdx
                        val targetAlbum = list[nextIdx]
                        val firstTrack = targetAlbum.tracks.firstOrNull()
                        if (firstTrack != null) {
                            currentOnPlayTrackFromList(firstTrack, targetAlbum.tracks)
                            Toast.makeText(context, "Album (${nextIdx + 1}/${list.size}): ${targetAlbum.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val skipToPreviousAlbum: () -> Unit = {
                    val list = currentAlbumsListState
                    if (list.isNotEmpty()) {
                        val curTrack = currentTrackState
                        val foundIdx = list.indexOfFirst {
                            it.name.equals(curTrack.album.trim(), ignoreCase = true) ||
                            it.tracks.any { t -> t.id == curTrack.id }
                        }
                        val baseIdx = if (activeAlbumIndex in list.indices) activeAlbumIndex else foundIdx
                        val prevIdx = if (baseIdx > 0) baseIdx - 1 else list.size - 1
                        activeAlbumIndex = prevIdx
                        val targetAlbum = list[prevIdx]
                        val firstTrack = targetAlbum.tracks.firstOrNull()
                        if (firstTrack != null) {
                            currentOnPlayTrackFromList(firstTrack, targetAlbum.tracks)
                            Toast.makeText(context, "Album (${prevIdx + 1}/${list.size}): ${targetAlbum.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val skipToNextAlbumState by rememberUpdatedState(skipToNextAlbum)
                val skipToPreviousAlbumState by rememberUpdatedState(skipToPreviousAlbum)

                // Interactive Visual Artwork Center Area (Swipe left/right for songs, fast swipe up/down for albums)
                val density = androidx.compose.ui.platform.LocalDensity.current
                val minSwipeDistancePx = with(density) { 65.dp.toPx() }
                val minFlingVelocityPx = with(density) { 650.dp.toPx() }
                val hapticFeedback = LocalHapticFeedback.current

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .offset {
                            IntOffset(
                                (animOffsetX.value + gestureDragX).roundToInt(),
                                (animOffsetY.value + gestureDragY).roundToInt()
                            )
                        }
                        .pointerInput(Unit) {
                            val velocityTracker = VelocityTracker()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                velocityTracker.resetTracking()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                var totalX = 0f
                                var totalY = 0f
                                var lockedAxis: String? = null
                                var hasHapticFired = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (change.isConsumed) break

                                    if (change.changedToUp()) {
                                        val velocity = velocityTracker.calculateVelocity()
                                        val velX = velocity.x
                                        val velY = velocity.y
                                        val startX = gestureDragX
                                        val startY = gestureDragY
                                        gestureDragX = 0f
                                        gestureDragY = 0f

                                        val absX = abs(totalX)
                                        val absY = abs(totalY)
                                        val absVelX = abs(velX)
                                        val absVelY = abs(velY)

                                        val isHorizontalSwipe = (absX > minSwipeDistancePx || absVelX > minFlingVelocityPx) && absX > (absY * 1.35f)
                                        val isVerticalSwipe = (absY > minSwipeDistancePx || absVelY > minFlingVelocityPx) && absY > (absX * 1.35f)

                                        if (isHorizontalSwipe) {
                                            swipeScope.launch {
                                                isSwipingTrack = true
                                                animOffsetX.snapTo(startX)
                                                animOffsetY.snapTo(startY)
                                                launch { animOffsetY.animateTo(0f, tween(100)) }
                                                if (totalX < 0) {
                                                    // Swipe left: Next song
                                                    animOffsetX.animateTo(-600f, tween(140, easing = FastOutSlowInEasing))
                                                    currentOnNextClick()
                                                    animOffsetX.snapTo(600f)
                                                    animOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                                } else {
                                                    // Swipe right: Previous song
                                                    animOffsetX.animateTo(600f, tween(140, easing = FastOutSlowInEasing))
                                                    currentOnPreviousClick()
                                                    animOffsetX.snapTo(-600f)
                                                    animOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                                }
                                                isSwipingTrack = false
                                            }
                                        } else if (isVerticalSwipe) {
                                            swipeScope.launch {
                                                animOffsetX.snapTo(startX)
                                                animOffsetY.snapTo(startY)
                                                launch { animOffsetX.animateTo(0f, tween(100)) }
                                                if (totalY < -120.dp.toPx() && absVelY < minFlingVelocityPx) {
                                                    showAllAlbumsSheet = true
                                                    animOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                                } else if (totalY < 0) {
                                                    // Fast swipe UP: Next album
                                                    animOffsetY.animateTo(-500f, tween(140, easing = FastOutSlowInEasing))
                                                    skipToNextAlbumState()
                                                    animOffsetY.snapTo(500f)
                                                    animOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                                } else {
                                                    // Fast swipe DOWN: Previous album
                                                    animOffsetY.animateTo(500f, tween(140, easing = FastOutSlowInEasing))
                                                    skipToPreviousAlbumState()
                                                    animOffsetY.snapTo(-500f)
                                                    animOffsetY.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 420f))
                                                }
                                            }
                                        } else {
                                            swipeScope.launch {
                                                animOffsetX.snapTo(startX)
                                                animOffsetY.snapTo(startY)
                                                launch { animOffsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
                                                launch { animOffsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 500f)) }
                                            }
                                        }
                                        break
                                    }

                                    val dragAmount = change.positionChange()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    change.consume()

                                    totalX += dragAmount.x
                                    totalY += dragAmount.y

                                    val absX = abs(totalX)
                                    val absY = abs(totalY)

                                    if (lockedAxis == null) {
                                        if (absX > 14f && absX > absY * 1.35f) {
                                            lockedAxis = "X"
                                        } else if (absY > 14f && absY > absX * 1.35f) {
                                            lockedAxis = "Y"
                                        }
                                    }

                                    val thresholdPassed = (absX > minSwipeDistancePx && absX > absY * 1.35f) ||
                                                          (absY > minSwipeDistancePx && absY > absX * 1.35f)
                                    if (thresholdPassed && !hasHapticFired) {
                                        hasHapticFired = true
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else if (!thresholdPassed) {
                                        hasHapticFired = false
                                    }

                                    // Direct non-suspending state assignment: zero lag, 120fps smooth!
                                    if (lockedAxis == "X") {
                                        gestureDragX = totalX
                                        gestureDragY = 0f
                                    } else if (lockedAxis == "Y") {
                                        gestureDragY = totalY
                                        gestureDragX = 0f
                                    } else {
                                        gestureDragX = totalX * 0.7f
                                        gestureDragY = totalY * 0.7f
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (showLyricsView) {
                        SyncedLyricsView(
                            lyrics = lyrics,
                            currentPositionMs = playbackState.currentPositionMs,
                            onLineClick = onSeek,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Crossfade(targetState = playerTheme, label = "PlayerThemeAnimation") { theme ->
                            when (theme) {
                                PlayerVisualTheme.MODERN_GLASS -> {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .size(280.dp)
                                            .shadow(16.dp, RoundedCornerShape(24.dp)),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            DefaultArtwork(
                                                title = track.title,
                                                artist = track.artist,
                                                cornerRadius = 24.dp,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (!track.artworkUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = track.artworkUri,
                                                    contentDescription = track.album,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                                PlayerVisualTheme.RETRO_CASSETTE -> {
                                    RetroCassetteView(
                                        title = track.title,
                                        artist = track.artist,
                                        isPlaying = playbackState.isPlaying,
                                        progressFraction = progressFraction,
                                        size = 300.dp
                                    )
                                }
                                PlayerVisualTheme.CYBER_HUD -> {
                                    CyberHudView(
                                        specs = playbackState.currentSpecs,
                                        isPlaying = playbackState.isPlaying,
                                        progressFraction = progressFraction,
                                        hudSize = 280.dp
                                    )
                                }
                                PlayerVisualTheme.VINYL_TURNTABLE -> {
                                    VinylRecordView(
                                        artworkUri = track.artworkUri,
                                        isPlaying = playbackState.isPlaying,
                                        size = 280.dp
                                    )
                                }
                                PlayerVisualTheme.MINIMALIST_ZEN -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        DefaultArtwork(
                                            title = track.title,
                                            artist = track.artist,
                                            cornerRadius = 80.dp,
                                            modifier = Modifier.size(160.dp)
                                        )
                                    }
                                }
                                PlayerVisualTheme.CLASSIC_CLICK_WHEEL -> {
                                    ClassicClickWheelView(
                                        track = track,
                                        playbackState = playbackState,
                                        onSeek = onSeek,
                                        onPlayPauseClick = onPlayPauseClick,
                                        onNextClick = onNextClick,
                                        onPreviousClick = onPreviousClick,
                                        onMenuClick = {
                                            showLyricsView = !showLyricsView
                                            Toast.makeText(context, if (showLyricsView) "Lyrics view" else "Click wheel view", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Audio Technical Specs Badge
                AudioSpecsBadge(
                    specs = playbackState.currentSpecs,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Track Title & Artist (Smooth marquee so titles never get cut off)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Quick Chips: "Find Albums", "Album Tracks" & "Artist Tracks"
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        SuggestionChip(
                            onClick = { showAllAlbumsSheet = true },
                            icon = {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            label = {
                                Text(
                                    text = "Albums (${albumsList.size})",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        SuggestionChip(
                            onClick = { showAlbumSheet = true },
                            icon = {
                                Icon(
                                    Icons.Default.Album,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text(
                                    text = track.album.ifBlank { "Current Album" },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    item {
                        SuggestionChip(
                            onClick = { showArtistSheet = true },
                            icon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            label = {
                                Text(
                                    text = "Artist Songs",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Music Progress Bar (5 Styles)
                CustomMusicProgressBar(
                    currentPositionMs = playbackState.currentPositionMs,
                    durationMs = playbackState.durationMs,
                    style = progressBarStyle,
                    onSeek = onSeek
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Main Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode == ShuffleMode.ON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
                    }

                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
                    }

                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = when (playbackState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                RepeatMode.ALL -> Icons.Default.Repeat
                                RepeatMode.OFF -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Secondary Action Row (Favorite, Equalizer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onToggleFavorite(track.id, !track.isFavorite) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onEqualizerClick) {
                        Icon(
                            Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Album Tracks Modal Bottom Sheet
            if (showAlbumSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAlbumSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            if (track.artworkUri != null) {
                                AsyncImage(
                                    model = track.artworkUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                DefaultArtwork(
                                    title = track.title,
                                    artist = track.artist,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.album.ifBlank { "Unknown Album" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${albumTracks.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        androidx.compose.material3.Divider(modifier = Modifier.padding(bottom = 8.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(albumTracks) { index, albumTrack ->
                                val isCurrent = albumTrack.id == track.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPlayTrackFromList(albumTrack, albumTracks)
                                            showAlbumSheet = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (albumTrack.trackNumber > 0) "${albumTrack.trackNumber}" else "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.width(28.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = albumTrack.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = albumTrack.audioQualitySummary,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = albumTrack.durationFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isCurrent && playbackState.isPlaying) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Artist Tracks Modal Bottom Sheet
            if (showArtistSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showArtistSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.artist.ifBlank { "Unknown Artist" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${artistTracks.size} songs in library",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        androidx.compose.material3.Divider(modifier = Modifier.padding(bottom = 8.dp))

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(artistTracks) { _, artistTrack ->
                                val isCurrent = artistTrack.id == track.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPlayTrackFromList(artistTrack, artistTracks)
                                            showArtistSheet = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artistTrack.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${artistTrack.album} • ${artistTrack.audioQualitySummary}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = artistTrack.durationFormatted,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isCurrent && playbackState.isPlaying) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. All Albums Modal Bottom Sheet (Find Albums Quick-Picker)
            if (showAllAlbumsSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showAllAlbumsSheet = false
                        albumSearchQuery = ""
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ) {
                    val filteredAlbums = remember(albumsList, albumSearchQuery) {
                        if (albumSearchQuery.isBlank()) {
                            albumsList
                        } else {
                            albumsList.filter {
                                it.name.contains(albumSearchQuery, ignoreCase = true) ||
                                it.artist.contains(albumSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All Albums (${albumsList.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap any album to play • Fast flick up/down to skip",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = albumSearchQuery,
                            onValueChange = { albumSearchQuery = it },
                            placeholder = { Text("Search albums or artists...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAlbums, key = { it.name }) { album ->
                                val isCurrentAlbum = album.name.equals(track.album.trim(), ignoreCase = true)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val firstTrack = album.tracks.firstOrNull()
                                            if (firstTrack != null) {
                                                onPlayTrackFromList(firstTrack, album.tracks)
                                                Toast.makeText(context, "Playing: ${album.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            showAllAlbumsSheet = false
                                            albumSearchQuery = ""
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentAlbum)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (album.artworkUri != null) {
                                            AsyncImage(
                                                model = album.artworkUri,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            DefaultArtwork(
                                                title = album.name,
                                                artist = album.artist,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = album.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isCurrentAlbum) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isCurrentAlbum) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${album.artist} • ${album.tracks.size} tracks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isCurrentAlbum) {
                                            Text(
                                                text = "PLAYING",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassicClickWheelView(
    track: Track,
    playbackState: PlaybackState,
    onSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var wheelRotationAngle by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini Display Card (Classic LCD Screen vibe)
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(105.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini album thumbnail
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DefaultArtwork(
                        title = track.title,
                        artist = track.artist,
                        cornerRadius = 10.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!track.artworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = track.album,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val progress = (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tactile Rotary Click Wheel (200dp)
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1E2430))
                .pointerInput(Unit) {
                    var lastAngle: Float? = null
                    detectDragGestures(
                        onDragStart = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            if (dist > 35.dp.toPx()) {
                                lastAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            } else {
                                lastAngle = null
                            }
                        },
                        onDragEnd = { lastAngle = null },
                        onDragCancel = { lastAngle = null },
                        onDrag = { change, _ ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = change.position.x - centerX
                            val dy = change.position.y - centerY
                            val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val prev = lastAngle
                            if (prev != null) {
                                var delta = currentAngle - prev
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f

                                if (abs(delta) >= 6f) {
                                    wheelRotationAngle = (wheelRotationAngle + delta) % 360f
                                    val seekStepMs = (delta * 65).toLong()
                                    val newPos = (playbackState.currentPositionMs + seekStepMs).coerceIn(0L, playbackState.durationMs)
                                    onSeek(newPos)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastAngle = currentAngle
                                }
                            } else {
                                lastAngle = currentAngle
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Rotary tick indicator ring (Canvas)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 4.dp.toPx()
                val rad = Math.toRadians(wheelRotationAngle.toDouble())
                val markerX = center.x + (radius - 12.dp.toPx()) * kotlin.math.cos(rad).toFloat()
                val markerY = center.y + (radius - 12.dp.toPx()) * kotlin.math.sin(rad).toFloat()
                drawCircle(
                    color = Color(0xFF64748B).copy(alpha = 0.5f),
                    radius = 5.dp.toPx(),
                    center = Offset(markerX, markerY)
                )
            }

            // Top Sector: MENU
            Text(
                text = "MENU",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .clickable(onClick = onMenuClick)
            )

            // Left Sector: |<< (Previous)
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous Track",
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
                    .size(24.dp)
                    .clickable(onClick = onPreviousClick)
            )

            // Right Sector: >>| (Next)
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next Track",
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .size(24.dp)
                    .clickable(onClick = onNextClick)
            )

            // Bottom Sector: >|| (Play/Pause)
            Icon(
                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play / Pause",
                tint = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .size(24.dp)
                    .clickable(onClick = onPlayPauseClick)
            )

            // Center Button (68dp glossy circle)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .clickable(onClick = onPlayPauseClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playbackState.isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                    contentDescription = "Center Select",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
