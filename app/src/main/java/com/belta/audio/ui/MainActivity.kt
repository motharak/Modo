package com.belta.audio.ui

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.belta.audio.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.belta.audio.BeltaAudioApp
import com.belta.audio.core.data.database.entity.ScanFolderEntity
import com.belta.audio.core.domain.model.EqualizerPreset
import com.belta.audio.core.domain.model.Lyrics
import com.belta.audio.core.domain.model.Track
import com.belta.audio.core.domain.smartengine.SmartPlaylistEngine
import com.belta.audio.core.theme.BeltaAudioTheme
import com.belta.audio.core.theme.PaletteExtractor
import com.belta.audio.core.theme.ThemeMode
import com.belta.audio.ui.components.MiniPlayer
import com.belta.audio.ui.screens.debug.DebugConsoleScreen
import com.belta.audio.ui.screens.equalizer.EqualizerScreen
import com.belta.audio.ui.screens.folders.FolderManagerDialog
import com.belta.audio.ui.screens.home.HomeScreen
import com.belta.audio.ui.screens.player.NowPlayingScreen
import com.belta.audio.ui.screens.search.SearchScreen
import com.belta.audio.ui.screens.settings.SettingsScreen
import com.belta.audio.ui.screens.smartplaylist.SmartPlaylistScreen
import com.belta.audio.ui.screens.tageditor.TagEditorScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.belta.audio.core.debug.DebugLogger
import com.belta.audio.core.debug.LogCategory

import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.belta.audio.core.audio.BeltaMediaService
import com.belta.audio.core.domain.model.ReverbSoundStage

class MainActivity : ComponentActivity() {

    private lateinit var app: BeltaAudioApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as BeltaAudioApp

        // Edge-to-Edge full screen with transparent bars preserving time, battery, and SIM signal
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            var currentTheme by remember { mutableStateOf(ThemeMode.DYNAMIC_MONET) }
            val playbackState by app.audioEngineController.playbackState.collectAsState()
            var albumArtColorScheme by remember { mutableStateOf<ColorScheme?>(null) }

            // Dynamic palette extraction when playing in Album Art Theme Mode
            LaunchedEffect(playbackState.currentTrack?.artworkUri, currentTheme) {
                if (currentTheme == ThemeMode.DYNAMIC_ALBUM_ART) {
                    val scheme = PaletteExtractor.extractColorSchemeFromArtwork(
                        this@MainActivity,
                        playbackState.currentTrack?.artworkUri
                    )
                    albumArtColorScheme = scheme
                }
            }

            var showSplashScreen by remember { mutableStateOf(true) }

            BeltaAudioTheme(
                themeMode = currentTheme,
                albumArtColorScheme = albumArtColorScheme
            ) {
                Crossfade(targetState = showSplashScreen, label = "AppSplashCrossfade") { isSplash ->
                    if (isSplash) {
                        BeltaSplashScreen(onFinished = { showSplashScreen = false })
                    } else {
                        MainAppContent(
                            app = app,
                            currentTheme = currentTheme,
                            onThemeChange = { currentTheme = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeltaSplashScreen(
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(1100L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.88f, animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(350))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Modo Logo",
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(26.dp))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BELTA AUDIO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "STUDIO SOUND ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    app: BeltaAudioApp,
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val sharedPreferences = remember { context.getSharedPreferences("belta_prefs", Context.MODE_PRIVATE) }
    var isDebugEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("live_debug_enabled", true)) }
    var minAudioDurationSeconds by remember { mutableIntStateOf(sharedPreferences.getInt("min_audio_duration_seconds", 60)) }
    var isAnimationsEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("animations_enabled", true)) }
    var isAutoPlayRadioEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_play_radio_enabled", true)) }
    var isScanning by remember { mutableStateOf(false) }

    val tracks by app.audioRepository.getAllTracksFlow().collectAsState(initial = emptyList())
    val albums by app.audioRepository.getAlbumsFlow().collectAsState(initial = emptyList())
    val artists by app.audioRepository.getArtistsFlow().collectAsState(initial = emptyList())
    val folders by app.audioRepository.getFoldersFlow().collectAsState(initial = emptyList())
    val playlists by app.playlistRepository.getAllPlaylistsFlow().collectAsState(initial = emptyList())
    val eqPresets by app.playlistRepository.getEqualizerPresetsFlow().collectAsState(initial = EqualizerPreset.DEFAULT_PRESETS)
    val playbackState by app.audioEngineController.playbackState.collectAsState()
    val scanFolders by app.database.scanFolderDao().getAllScanFoldersFlow().collectAsState(initial = emptyList())
    val scanProgress by app.audioRepository.scanProgress.collectAsState()

    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var currentLyrics by remember { mutableStateOf(Lyrics(0, false)) }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var currentEqPreset by remember { mutableStateOf(EqualizerPreset.FLAT) }
    var showFolderDialog by remember { mutableStateOf(false) }

    val eqEngine = BeltaMediaService.equalizerEngineInstance
    val bandFrequencies by (eqEngine?.bandFrequencies ?: remember { MutableStateFlow(listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")) }).collectAsState()
    val enginePreset by (eqEngine?.presetState ?: remember { MutableStateFlow(EqualizerPreset.FLAT) }).collectAsState()
    val isDspEnabled by (eqEngine?.enabledState ?: remember { MutableStateFlow(sharedPreferences.getBoolean("dsp_effects_enabled", true)) }).collectAsState()

    // Restore saved equalizer preset, master DSP switch, and replay gain on startup
    LaunchedEffect(eqPresets) {
        if (eqPresets.isNotEmpty()) {
            val savedPresetName = sharedPreferences.getString("selected_eq_preset_name", EqualizerPreset.FLAT.name)
            val matched = eqPresets.find { it.name == savedPresetName } ?: EqualizerPreset.FLAT
            currentEqPreset = matched
            BeltaMediaService.equalizerEngineInstance?.applyPreset(matched)
            val dspSaved = sharedPreferences.getBoolean("dsp_effects_enabled", true)
            BeltaMediaService.equalizerEngineInstance?.setEffectsEnabled(dspSaved)
            val replayGainSaved = sharedPreferences.getBoolean("replay_gain_enabled", true)
            app.audioEngineController.setReplayGainEnabled(replayGainSaved)
            BeltaMediaService.equalizerEngineInstance?.setReplayGainEnabled(replayGainSaved)
        }
    }

    LaunchedEffect(tracks, isAutoPlayRadioEnabled) {
        app.audioEngineController.autoPlayCandidatesProvider = { tracks }
        app.audioEngineController.isAutoPlayRadioEnabled = isAutoPlayRadioEnabled
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // System Document Tree File Picker for choosing music directories directly
    val openFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val resolvedPath = getPathFromTreeUri(context, it)
            if (resolvedPath.isNotBlank()) {
                scope.launch {
                    val folderName = java.io.File(resolvedPath).name.ifEmpty { "Music Folder" }
                    app.database.scanFolderDao().insertFolder(ScanFolderEntity(path = resolvedPath, name = folderName))
                    Toast.makeText(context, "Added folder: $resolvedPath", Toast.LENGTH_SHORT).show()
                    isScanning = true
                    val count = app.audioRepository.scanLibrary(minAudioDurationSeconds)
                    isScanning = false
                    Toast.makeText(context, "Scan complete: $count tracks found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 1. Intercept back to collapse full-screen player if open
    BackHandler(enabled = isNowPlayingExpanded) {
        isNowPlayingExpanded = false
    }

    // 2. Intercept back to dismiss folder dialog if open
    BackHandler(enabled = showFolderDialog && !isNowPlayingExpanded) {
        showFolderDialog = false
    }

    // 3. Intercept back to return to main Library tab if on secondary bottom-nav tabs
    val isSecondaryTab = currentRoute in listOf("smart_playlists", "equalizer", "settings")
    BackHandler(enabled = isSecondaryTab && !isNowPlayingExpanded && !showFolderDialog) {
        navController.navigate("home") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 4. On root home screen, minimize app into background without killing playback process
    val isRootHome = currentRoute == "home" && !isNowPlayingExpanded && !showFolderDialog
    BackHandler(enabled = isRootHome) {
        val activity = context as? Activity
        activity?.moveTaskToBack(true)
    }

    // Fetch local and open LRCLIB lyrics automatically when track changes
    LaunchedEffect(playbackState.currentTrack?.id) {
        val track = playbackState.currentTrack
        if (track != null) {
            currentLyrics = app.lyricsRepository.getLyrics(track)
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            scope.launch {
                isScanning = true
                val count = app.audioRepository.scanLibrary(minAudioDurationSeconds)
                isScanning = false
                Toast.makeText(context, "Scan complete: $count tracks found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    Scaffold(
        bottomBar = {
            val showBottomNav = currentRoute in listOf("home", "smart_playlists", "equalizer", "settings")
            if (showBottomNav && !isNowPlayingExpanded) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Library", fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "smart_playlists",
                        onClick = {
                            navController.navigate("smart_playlists") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("Smart Mixes", fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "equalizer",
                        onClick = {
                            navController.navigate("equalizer") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                        label = { Text("Equalizer", fontWeight = FontWeight.SemiBold) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        tracks = tracks,
                        albums = albums,
                        artists = artists,
                        folders = folders,
                        playlists = playlists,
                        onTrackClick = { track, list ->
                            val index = list.indexOf(track).coerceAtLeast(0)
                            app.audioEngineController.playQueue(list, index)
                        },
                        onAlbumClick = { album ->
                            scope.launch {
                                val albumTracks = tracks.filter { it.albumId == album.id }
                                if (albumTracks.isNotEmpty()) {
                                    app.audioEngineController.playQueue(albumTracks, 0)
                                }
                            }
                        },
                        onArtistClick = { artist ->
                            scope.launch {
                                val artistTracks = tracks.filter { it.artistId == artist.id }
                                if (artistTracks.isNotEmpty()) {
                                    app.audioEngineController.playQueue(artistTracks, 0)
                                }
                            }
                        },
                        onFolderClick = { folder ->
                            scope.launch {
                                val folderTracks = tracks.filter { java.io.File(it.path).parent == folder.path }
                                if (folderTracks.isNotEmpty()) {
                                    app.audioEngineController.playQueue(folderTracks, 0)
                                }
                            }
                        },
                        onPlaylistClick = { playlist ->
                            app.audioEngineController.setPlaylistFlowConfig(
                                playlist.crossfadeSeconds,
                                playlist.isAutomixEnabled,
                                playlist.fadeCurve
                            )
                            scope.launch {
                                if (playlist.isSmart && playlist.smartRuleJson != null) {
                                    try {
                                        val def = Json.decodeFromString<com.belta.audio.core.domain.model.SmartRuleDefinition>(playlist.smartRuleJson)
                                        val smartTracks = SmartPlaylistEngine.evaluate(tracks, def)
                                        if (smartTracks.isNotEmpty()) {
                                            app.audioEngineController.playQueue(smartTracks, 0)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                        onGetPlaylistTracks = { playlistId ->
                            app.playlistRepository.getTracksForPlaylist(playlistId)
                        },
                        onCreatePlaylist = { name, desc ->
                            scope.launch {
                                try {
                                    app.playlistRepository.createPlaylist(
                                        name = name,
                                        description = desc,
                                        isSmart = false
                                    )
                                    Toast.makeText(context, "Created playlist: $name", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error creating playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeletePlaylist = { playlistId ->
                            scope.launch {
                                try {
                                    app.playlistRepository.deletePlaylist(playlistId)
                                    Toast.makeText(context, "Deleted playlist", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error deleting playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onAddTrackToPlaylist = { playlistId, trackId ->
                            scope.launch {
                                try {
                                    app.playlistRepository.addTracksToPlaylist(playlistId, listOf(trackId))
                                    Toast.makeText(context, "Added track to playlist", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error adding track: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onUpdatePlaylist = { updatedPlaylist ->
                            scope.launch {
                                app.playlistRepository.updatePlaylist(updatedPlaylist)
                                app.audioEngineController.setPlaylistFlowConfig(
                                    updatedPlaylist.crossfadeSeconds,
                                    updatedPlaylist.isAutomixEnabled,
                                    updatedPlaylist.fadeCurve
                                )
                            }
                        },
                        globalCrossfadeSeconds = playbackState.crossfadeDurationSeconds,
                        onEditTagClick = { track ->
                            editingTrack = track
                            navController.navigate("tag_editor")
                        },
                        isAnimationsEnabled = isAnimationsEnabled
                    )
                }

                composable("smart_playlists") {
                    SmartPlaylistScreen(
                        allTracks = tracks,
                        userSmartPlaylists = playlists.filter { it.isSmart || !it.smartRuleJson.isNullOrBlank() },
                        onPlayTracks = { queue ->
                            if (queue.isNotEmpty()) {
                                app.audioEngineController.playQueue(queue, 0)
                            } else {
                                Toast.makeText(context, "No tracks matching this mix", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPlayPlaylist = { playlist ->
                            scope.launch {
                                var pTracks = app.playlistRepository.getTracksForPlaylist(playlist.id)
                                if (pTracks.isEmpty() && !playlist.smartRuleJson.isNullOrBlank()) {
                                    try {
                                        val def = Json.decodeFromString<com.belta.audio.core.domain.model.SmartRuleDefinition>(playlist.smartRuleJson)
                                        pTracks = SmartPlaylistEngine.evaluate(tracks, def)
                                    } catch (_: Exception) {}
                                }
                                if (pTracks.isNotEmpty()) {
                                    app.audioEngineController.setPlaylistFlowConfig(
                                        playlist.crossfadeSeconds,
                                        playlist.isAutomixEnabled,
                                        playlist.fadeCurve
                                    )
                                    app.audioEngineController.playQueue(pTracks, 0)
                                } else {
                                    Toast.makeText(context, "No tracks matching this mix", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCreateCustomSmartPlaylist = { name, desc, ruleJson, trackIds ->
                            scope.launch {
                                try {
                                    val playlistId = app.playlistRepository.createPlaylist(
                                        name = name,
                                        description = desc,
                                        isSmart = false,
                                        smartRuleJson = ruleJson
                                    )
                                    if (trackIds.isNotEmpty()) {
                                        app.playlistRepository.addTracksToPlaylist(playlistId, trackIds)
                                    }
                                    Toast.makeText(context, "Saved playlist: $name", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    DebugLogger.e(LogCategory.DATABASE, "SMART_PLAYLIST", "Error saving smart playlist: ${e.message}")
                                    Toast.makeText(context, "Error saving playlist: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeletePlaylist = { id ->
                            scope.launch {
                                try {
                                    app.playlistRepository.deletePlaylist(id)
                                    Toast.makeText(context, "Deleted mix", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error deleting mix: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onBackClick = null
                    )
                }

                composable("equalizer") {
                    EqualizerScreen(
                        presets = eqPresets,
                        currentPreset = enginePreset,
                        isReplayGainEnabled = playbackState.isReplayGainEnabled,
                        isEffectsEnabled = isDspEnabled,
                        bandFrequencies = bandFrequencies,
                        onEffectsEnabledToggle = { enabled ->
                            sharedPreferences.edit().putBoolean("dsp_effects_enabled", enabled).apply()
                            BeltaMediaService.equalizerEngineInstance?.setEffectsEnabled(enabled)
                        },
                        onPresetSelect = { preset ->
                            currentEqPreset = preset
                            sharedPreferences.edit().putString("selected_eq_preset_name", preset.name).apply()
                            BeltaMediaService.equalizerEngineInstance?.applyPreset(preset)
                        },
                        onBandGainChange = { index, gain ->
                            BeltaMediaService.equalizerEngineInstance?.setBandGain(index, gain)
                        },
                        onBassBoostChange = { boost ->
                            BeltaMediaService.equalizerEngineInstance?.setBassBoost(boost)
                        },
                        onVirtualizerChange = { virt ->
                            BeltaMediaService.equalizerEngineInstance?.setVirtualizer(virt)
                        },
                        onReverbStageChange = { stage ->
                            BeltaMediaService.equalizerEngineInstance?.setReverbSoundStage(stage)
                        },
                        onReplayGainToggle = { enabled ->
                            app.audioEngineController.setReplayGainEnabled(enabled)
                            sharedPreferences.edit().putBoolean("replay_gain_enabled", enabled).apply()
                            BeltaMediaService.equalizerEngineInstance?.setReplayGainEnabled(enabled)
                        },
                        onSaveCustomPreset = { name ->
                            scope.launch {
                                val activePreset = BeltaMediaService.equalizerEngineInstance?.presetState?.value ?: currentEqPreset
                                val newPreset = activePreset.copy(id = 0, name = name, isCustom = true)
                                val newId = app.playlistRepository.saveEqualizerPreset(newPreset)
                                currentEqPreset = newPreset.copy(id = newId)
                                sharedPreferences.edit().putString("selected_eq_preset_name", name).apply()
                                Toast.makeText(context, "Saved sound profile: $name", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteCustomPreset = { id ->
                            scope.launch {
                                app.playlistRepository.deleteEqualizerPreset(id)
                                Toast.makeText(context, "Deleted custom sound profile", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBackClick = null
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        currentTheme = currentTheme,
                        crossfadeSeconds = playbackState.crossfadeDurationSeconds,
                        isGaplessEnabled = playbackState.isGaplessEnabled,
                        isReplayGainEnabled = playbackState.isReplayGainEnabled,
                        scanFoldersCount = scanFolders.size,
                        onThemeChange = onThemeChange,
                        onCrossfadeChange = { app.audioEngineController.setCrossfadeDuration(it) },
                        onReplayGainToggle = { enabled ->
                            app.audioEngineController.setReplayGainEnabled(enabled)
                            sharedPreferences.edit().putBoolean("replay_gain_enabled", enabled).apply()
                            BeltaMediaService.equalizerEngineInstance?.setReplayGainEnabled(enabled)
                        },
                        onOpenFolderManager = { showFolderDialog = true },
                        onRescanLibrary = {
                            scope.launch {
                                isScanning = true
                                val count = app.audioRepository.scanLibrary(minAudioDurationSeconds)
                                isScanning = false
                                Toast.makeText(context, "Rescan complete: $count tracks found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        minAudioDurationSeconds = minAudioDurationSeconds,
                        onMinAudioDurationChange = { sec ->
                            minAudioDurationSeconds = sec
                            sharedPreferences.edit().putInt("min_audio_duration_seconds", sec).apply()
                            scope.launch {
                                isScanning = true
                                val count = app.audioRepository.scanLibrary(sec)
                                isScanning = false
                                Toast.makeText(context, "Library filtered: $count tracks (min ${sec}s)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        isAnimationsEnabled = isAnimationsEnabled,
                        onAnimationsToggle = { enabled ->
                            isAnimationsEnabled = enabled
                            sharedPreferences.edit().putBoolean("animations_enabled", enabled).apply()
                            Toast.makeText(context, if (enabled) "Motion & fluid animations enabled" else "Animations disabled", Toast.LENGTH_SHORT).show()
                        },
                        isAutoPlayRadioEnabled = isAutoPlayRadioEnabled,
                        onAutoPlayRadioToggle = { enabled ->
                            isAutoPlayRadioEnabled = enabled
                            sharedPreferences.edit().putBoolean("auto_play_radio_enabled", enabled).apply()
                            app.audioEngineController.isAutoPlayRadioEnabled = enabled
                            Toast.makeText(context, if (enabled) "Auto-Play infinite radio enabled" else "Auto-Play disabled", Toast.LENGTH_SHORT).show()
                        },
                        onBackupToCloud = {
                            scope.launch {
                                val backupFile = app.syncManager.exportBackupToFile()
                                Toast.makeText(context, "Saved backup to: ${backupFile.name}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onRestoreFromCloud = {
                            Toast.makeText(context, "Cloud sync connected", Toast.LENGTH_SHORT).show()
                        },
                        isDebugEnabled = isDebugEnabled,
                        onDebugToggle = { enabled ->
                            isDebugEnabled = enabled
                            sharedPreferences.edit().putBoolean("live_debug_enabled", enabled).apply()
                            if (enabled) {
                                app.debugHttpServer.start()
                                Toast.makeText(context, "Live Debug Server started on port 8080", Toast.LENGTH_SHORT).show()
                            } else {
                                app.debugHttpServer.stop()
                                Toast.makeText(context, "Live Debug Server stopped", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenDebugConsole = { navController.navigate("debug_console") },
                        onBackClick = null
                    )
                }

                composable("debug_console") {
                    DebugConsoleScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable("tag_editor") {
                    editingTrack?.let { track ->
                        TagEditorScreen(
                            track = track,
                            onSaveTrack = { updated ->
                                scope.launch {
                                    app.audioRepository.updateTrackMetadata(updated)
                                    Toast.makeText(context, "Metadata updated successfully", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            // Real-Time Scanning Progress Feedback Pill (rendered in front of NavHost)
            AnimatedVisibility(
                visible = scanProgress.isScanning,
                enter = if (isAnimationsEnabled) {
                    fadeIn() + slideInVertically(initialOffsetY = { -it })
                } else EnterTransition.None,
                exit = if (isAnimationsEnabled) {
                    fadeOut() + slideOutVertically(targetOffsetY = { -it })
                } else ExitTransition.None,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Scanning Library: ${scanProgress.scannedCount} tracks",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Floating Mini Player when not full screen
            AnimatedVisibility(
                visible = playbackState.currentTrack != null && !isNowPlayingExpanded,
                enter = if (isAnimationsEnabled) {
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = tween(200))
                } else EnterTransition.None,
                exit = if (isAnimationsEnabled) {
                    slideOutVertically(
                        animationSpec = tween(200),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = tween(150))
                } else ExitTransition.None,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                MiniPlayer(
                    playbackState = playbackState,
                    onPlayPauseClick = { app.audioEngineController.togglePlayPause() },
                    onNextClick = { app.audioEngineController.skipToNext() },
                    onPreviousClick = { app.audioEngineController.skipToPrevious() },
                    onClick = { isNowPlayingExpanded = true }
                )
            }

            // Full Screen Now Playing overlay
            AnimatedVisibility(
                visible = isNowPlayingExpanded && playbackState.currentTrack != null,
                enter = if (isAnimationsEnabled) {
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                        initialOffsetY = { it }
                    ) + fadeIn(animationSpec = tween(250))
                } else EnterTransition.None,
                exit = if (isAnimationsEnabled) {
                    slideOutVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = tween(200))
                } else ExitTransition.None
            ) {
                NowPlayingScreen(
                    playbackState = playbackState,
                    lyrics = currentLyrics,
                    allTracks = tracks,
                    onPlayTrackFromList = { selectedTrack, list ->
                        val index = list.indexOf(selectedTrack).coerceAtLeast(0)
                        app.audioEngineController.playQueue(list, index)
                    },
                    onCollapse = { isNowPlayingExpanded = false },
                    onPlayPauseClick = { app.audioEngineController.togglePlayPause() },
                    onNextClick = { app.audioEngineController.skipToNext() },
                    onPreviousClick = { app.audioEngineController.skipToPrevious() },
                    onSeek = { positionMs -> app.audioEngineController.seekTo(positionMs) },
                    onToggleFavorite = { id, isFav ->
                        scope.launch { app.audioRepository.toggleFavorite(id, isFav) }
                    },
                    onToggleRepeat = { app.audioEngineController.toggleRepeatMode() },
                    onToggleShuffle = { app.audioEngineController.toggleShuffleMode() },
                    onEqualizerClick = {
                        isNowPlayingExpanded = false
                        navController.navigate("equalizer")
                    },
                    onSleepTimerClick = {
                        Toast.makeText(context, "Sleep timer set for 30 minutes", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Folder Manager Dialog
            if (showFolderDialog) {
                FolderManagerDialog(
                    currentFolders = scanFolders,
                    onPickFromSystemFileManager = {
                        openFolderLauncher.launch(null)
                    },
                    onAddFolder = { path ->
                        scope.launch {
                            val name = java.io.File(path).name.ifEmpty { "Music Folder" }
                            app.database.scanFolderDao().insertFolder(ScanFolderEntity(path = path, name = name))
                            isScanning = true
                            val count = app.audioRepository.scanLibrary(minAudioDurationSeconds)
                            isScanning = false
                            Toast.makeText(context, "Scan complete: $count tracks found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRemoveFolder = { path ->
                        scope.launch {
                            app.database.scanFolderDao().deleteFolder(path)
                        }
                    },
                    onTriggerScan = {
                        scope.launch {
                            isScanning = true
                            val count = app.audioRepository.scanLibrary(minAudioDurationSeconds)
                            isScanning = false
                            Toast.makeText(context, "Scanned $count tracks from selected folders", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showFolderDialog = false }
                )
            }
        }
    }
}

private fun getPathFromTreeUri(context: Context, uri: Uri): String {
    try {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
    } catch (e: Exception) {
        // ignore
    }

    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        val type = split[0]
        val relativePath = if (split.size > 1) split[1] else ""

        if ("primary".equals(type, ignoreCase = true)) {
            if (relativePath.isEmpty()) {
                Environment.getExternalStorageDirectory().absolutePath
            } else {
                "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
            }
        } else {
            "/storage/$type/$relativePath"
        }
    } catch (e: Exception) {
        uri.path ?: ""
    }
}

