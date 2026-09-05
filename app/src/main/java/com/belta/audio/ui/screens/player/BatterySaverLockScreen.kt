package com.belta.audio.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.belta.audio.core.domain.model.PlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class LockScreenStyle(val displayName: String) {
    RETRO_TAPE("Retro Tape"),
    DIGITAL_CLOCK("Minimal Clock"),
    VINYL("Vinyl Turntable"),
    CYBER_HUD("Cyber HUD")
}

@Composable
fun BatterySaverLockScreen(
    playbackState: PlaybackState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val track = playbackState.currentTrack
    val sharedPreferences = remember { context.getSharedPreferences("belta_prefs", Context.MODE_PRIVATE) }

    var selectedStyle by remember {
        val saved = sharedPreferences.getString("lock_screen_style", LockScreenStyle.RETRO_TAPE.name)
        mutableStateOf(LockScreenStyle.entries.find { it.name == saved } ?: LockScreenStyle.RETRO_TAPE)
    }
    var showUnlockHint by remember { mutableStateOf(false) }

    // Lock Screen Lifecycle: Force Landscape, Strict Immersive Sticky Full-Screen, and Dim Display
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val originalSystemUiVisibility = window?.decorView?.systemUiVisibility ?: 0

        // 1. Force landscape orientation for nightstand / desk cassette deck display
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 2. Hide system bars
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        // 3. Apply fullscreen flags and immersive sticky flags to decorView
        window?.let { w ->
            val params = w.attributes
            params.screenBrightness = 0.01f
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            w.attributes = params
            w.addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.let { w ->
                val params = w.attributes
                params.screenBrightness = originalBrightness
                w.attributes = params
                w.clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = originalSystemUiVisibility
            }
        }
    }

    // Intercept hardware/gesture Back: do not dismiss on edge back gesture, require double-tap
    BackHandler {
        showUnlockHint = true
    }

    // Strict 1 FPS update ticker for time, battery, and visual animation
    var currentEpochSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var batteryPercentage by remember { mutableIntStateOf(100) }

    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        while (isActive) {
            currentEpochSeconds = System.currentTimeMillis() / 1000
            batteryManager?.let {
                batteryPercentage = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
            }
            delay(1000L) // 1 FPS tick
        }
    }

    val timeString = remember(currentEpochSeconds) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(currentEpochSeconds * 1000))
    }
    val dateString = remember(currentEpochSeconds) {
        SimpleDateFormat("EEEE • dd MMMM", Locale.getDefault()).format(Date(currentEpochSeconds * 1000)).uppercase()
    }

    val totalMs = playbackState.durationMs.coerceAtLeast(1L)
    val progressFraction = (playbackState.currentPositionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Pure OLED AMOLED pitch black
            // Consume drag gestures: block status bar pull-down and enable horizontal swipe to change track
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                val minLockSwipePx = 120.dp.toPx()
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        val absX = abs(dragX)
                        val absY = abs(dragY)
                        // Require deliberate swipe distance (> 120dp) and 2:1 horizontal dominance to prevent false positives
                        if (absX > absY * 2.0f && absX > minLockSwipePx) {
                            if (dragX < 0) {
                                onNextClick() // Swipe Left: Next Track
                            } else {
                                onPreviousClick() // Swipe Right: Previous Track
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume() // Fully consume drag to block status bar and navigation gestures
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    }
                )
            }
            // Double-tap to unlock, single-tap for hint
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDismiss() },
                    onTap = { showUnlockHint = true }
                )
            }
    ) {
        val songMood = remember(track?.genre, track?.title, track?.artist) {
            detectSongMood(track?.genre, track?.title, track?.artist)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Pane: Visual Stage & Dedicated Real Cat Band (Guaranteed Zero Overlay)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Slot: Selected Style Visualizer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedStyle) {
                        LockScreenStyle.RETRO_TAPE -> {
                            RetroTapeLandscapeView(
                                currentEpochSeconds = currentEpochSeconds,
                                progressFraction = progressFraction,
                                isPlaying = playbackState.isPlaying,
                                trackTitle = track?.title ?: "BELTA CASSETTE",
                                artistName = track?.artist ?: "HIGH BIAS CHROME"
                            )
                        }
                        LockScreenStyle.DIGITAL_CLOCK -> {
                            DigitalClockLandscapeView(
                                timeString = timeString,
                                dateString = dateString,
                                currentPositionMs = playbackState.currentPositionMs,
                                durationMs = playbackState.durationMs,
                                progressFraction = progressFraction,
                                isHiRes = track?.isHiRes ?: false,
                                isLossless = track?.isLossless ?: false
                            )
                        }
                        LockScreenStyle.VINYL -> {
                            VinylLandscapeView(
                                currentEpochSeconds = currentEpochSeconds,
                                isPlaying = playbackState.isPlaying,
                                trackTitle = track?.title ?: "VINYL RECORD",
                                artistName = track?.artist ?: "33 1/3 RPM"
                            )
                        }
                        LockScreenStyle.CYBER_HUD -> {
                            CyberHudLandscapeView(
                                currentEpochSeconds = currentEpochSeconds,
                                timeString = timeString,
                                batteryPercentage = batteryPercentage,
                                isPlaying = playbackState.isPlaying,
                                track = track
                            )
                        }
                    }
                }

                // Dedicated Lower Stage: Real Cat Silhouette Band (Zero overlay on visualizer or controls!)
                RealCatBandStage(
                    trackId = track?.id ?: 0L,
                    currentEpochSeconds = currentEpochSeconds,
                    isPlaying = playbackState.isPlaying,
                    songMood = songMood,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Right Pane: Track Info, Controls, Battery Status, and Style Chips
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Style Selector Chips and Battery
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.50f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$batteryPercentage%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "1 FPS OLED Saver",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    // Style Switcher Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(0.60f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        LockScreenStyle.entries.forEach { style ->
                            val isSelected = selectedStyle == style
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedStyle = style
                                        sharedPreferences.edit().putString("lock_screen_style", style.name).apply()
                                    }
                            ) {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Middle: Track Title, Artist, and Format Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.70f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track?.artist ?: "Unknown Artist"} • ${track?.album ?: "Unknown Album"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 1 FPS Live Band Mood Indicator
                Text(
                    text = "${songMood.label} • 1 FPS BAND",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = songMood.badgeColor.copy(alpha = 0.75f),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Bottom: Pocket Controls, Swipe Hint, and Unlock Pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.alpha(0.45f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        IconButton(
                            onClick = onNextClick,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Swipe left/right to change song",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.35f)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onDismiss() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (showUnlockHint) "Tap again or double-tap to exit" else "Double-tap or tap to exit",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.45f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroTapeLandscapeView(
    currentEpochSeconds: Long,
    progressFraction: Float,
    isPlaying: Boolean,
    trackTitle: String,
    artistName: String,
    modifier: Modifier = Modifier
) {
    val angleDegrees = if (isPlaying) ((currentEpochSeconds % 60) * 6f) else 0f
    val counterStr = String.format(Locale.US, "%03d", (currentEpochSeconds % 1000))

    Box(
        modifier = modifier
            .width(330.dp)
            .height(185.dp)
            .alpha(0.45f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 1.5.dp.toPx()
            val w = size.width
            val h = size.height
            val corner = 14.dp.toPx()

            // Outer cassette outline
            drawRoundRect(
                color = Color.White,
                size = size,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = strokeW)
            )

            // 4 Corner Screws
            val screwR = 3.5.dp.toPx()
            val screwPad = 12.dp.toPx()
            val screwPoints = listOf(
                Offset(screwPad, screwPad),
                Offset(w - screwPad, screwPad),
                Offset(screwPad, h - screwPad),
                Offset(w - screwPad, h - screwPad)
            )
            for (p in screwPoints) {
                drawCircle(color = Color.White.copy(alpha = 0.6f), radius = screwR, center = p, style = Stroke(width = 1.dp.toPx()))
                drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(p.x - screwR * 0.7f, p.y), end = Offset(p.x + screwR * 0.7f, p.y), strokeWidth = 1.dp.toPx())
            }

            // Top Label Header line
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(screwPad * 2f, 26.dp.toPx()),
                end = Offset(w - screwPad * 2f, 26.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Center Clear Tape Window
            val winW = w * 0.74f
            val winH = h * 0.48f
            val winLeft = (w - winW) / 2f
            val winTop = (h - winH) / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(winLeft, winTop),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = strokeW)
            )

            // Tape Spool Centers
            val spoolRadius = 22.dp.toPx()
            val leftSpoolCenter = Offset(w * 0.31f, h * 0.5f)
            val rightSpoolCenter = Offset(w * 0.69f, h * 0.5f)

            // Dynamic tape pack thickness on left vs right reel based on progress
            val leftTapeRadius = spoolRadius + (18.dp.toPx() * (1f - progressFraction))
            val rightTapeRadius = spoolRadius + (18.dp.toPx() * progressFraction)

            // Tape packs
            drawCircle(color = Color.White.copy(alpha = 0.20f), radius = leftTapeRadius, center = leftSpoolCenter)
            drawCircle(color = Color.White.copy(alpha = 0.20f), radius = rightTapeRadius, center = rightSpoolCenter)

            // Hub rings
            drawCircle(color = Color.White, radius = spoolRadius, center = leftSpoolCenter, style = Stroke(width = strokeW))
            drawCircle(color = Color.White, radius = spoolRadius, center = rightSpoolCenter, style = Stroke(width = strokeW))

            // 6-tooth gear teeth step rotating at 1 FPS
            for (i in 0 until 6) {
                val rad = Math.toRadians((angleDegrees + i * 60.0)).toFloat()
                val rCos = cos(rad)
                val rSin = sin(rad)
                val p1 = Offset(leftSpoolCenter.x + (spoolRadius * 0.35f) * rCos, leftSpoolCenter.y + (spoolRadius * 0.35f) * rSin)
                val p2 = Offset(leftSpoolCenter.x + spoolRadius * rCos, leftSpoolCenter.y + spoolRadius * rSin)
                drawLine(color = Color.White, start = p1, end = p2, strokeWidth = strokeW, cap = StrokeCap.Round)

                val p3 = Offset(rightSpoolCenter.x + (spoolRadius * 0.35f) * rCos, rightSpoolCenter.y + (spoolRadius * 0.35f) * rSin)
                val p4 = Offset(rightSpoolCenter.x + spoolRadius * rCos, rightSpoolCenter.y + spoolRadius * rSin)
                drawLine(color = Color.White, start = p3, end = p4, strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            // Magnetic Tape Ribbon path across the bottom rollers
            val ribbonY = h * 0.72f
            drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = Offset(winLeft + 10.dp.toPx(), ribbonY),
                end = Offset(winLeft + winW - 10.dp.toPx(), ribbonY),
                strokeWidth = 2.dp.toPx()
            )

            // 5-segment LED dB meter indicator at bottom
            val ledBaseX = w * 0.5f - 24.dp.toPx()
            val ledY = h - 22.dp.toPx()
            val activeLeds = if (isPlaying) (1 + (currentEpochSeconds % 5).toInt()) else 0
            for (seg in 0 until 5) {
                val segX = ledBaseX + seg * 10.dp.toPx()
                val isLit = seg < activeLeds
                drawRect(
                    color = if (isLit) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(segX, ledY),
                    size = Size(7.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // Cassette Brand Header
        Text(
            text = "TYPE II • BELTA CHROME • 70µs",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            letterSpacing = 1.sp,
            color = Color.White.copy(alpha = 0.50f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )

        // Rolling Tape Counter Box
        Text(
            text = counterStr,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.70f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
        )
    }
}

@Composable
private fun DigitalClockLandscapeView(
    timeString: String,
    dateString: String,
    currentPositionMs: Long,
    durationMs: Long,
    progressFraction: Float,
    isHiRes: Boolean,
    isLossless: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(320.dp)
            .alpha(0.60f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Audio Quality Badge & Date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = dateString,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = Color.Gray,
                letterSpacing = 2.sp
            )
            if (isHiRes) {
                Text(
                    text = "HI-RES",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFB800),
                    modifier = Modifier
                        .background(Color(0xFFFFB800).copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            } else if (isLossless) {
                Text(
                    text = "LOSSLESS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF00C853),
                    modifier = Modifier
                        .background(Color(0xFF00C853).copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        // Giant Digital Clock
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 4.sp
            ),
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )

        // Slim Progress Bar Scrubber
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.65f))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val elapsedSec = (currentPositionMs / 1000).coerceAtLeast(0)
                val totalSec = (durationMs / 1000).coerceAtLeast(0)
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun VinylLandscapeView(
    currentEpochSeconds: Long,
    isPlaying: Boolean,
    trackTitle: String,
    artistName: String,
    modifier: Modifier = Modifier
) {
    val angleDegrees = if (isPlaying) ((currentEpochSeconds % 60) * 6f) else 0f

    Box(
        modifier = modifier
            .size(210.dp)
            .alpha(0.45f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.width / 2f
            val strokeW = 1.dp.toPx()

            // Vinyl Outer Rim
            drawCircle(color = Color.White, radius = maxR, center = center, style = Stroke(width = 1.5.dp.toPx()))

            // Concentric Micro-Groove Sheen Rings
            for (step in 1..5) {
                val r = maxR * (0.36f + step * 0.12f)
                drawCircle(color = Color.White.copy(alpha = 0.28f), radius = r, center = center, style = Stroke(width = strokeW))
            }

            // Center Record Label Ring
            val labelR = maxR * 0.36f
            drawCircle(color = Color.White.copy(alpha = 0.60f), radius = labelR, center = center, style = Stroke(width = strokeW))

            // Spindle Center Hole
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = center)

            // Rotating Light Beam Reflection
            val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
            val startP = Offset(center.x + maxR * 0.38f * cos(rad), center.y + maxR * 0.38f * sin(rad))
            val endP = Offset(center.x + maxR * 0.96f * cos(rad), center.y + maxR * 0.96f * sin(rad))
            drawLine(color = Color.White.copy(alpha = 0.55f), start = startP, end = endP, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)

            // Stylized Turntable Tonearm with Cartridge
            val armBase = Offset(size.width - 6.dp.toPx(), 8.dp.toPx())
            val armPivot = Offset(size.width - 24.dp.toPx(), 28.dp.toPx())
            val armNeedle = Offset(center.x + maxR * 0.72f, center.y - maxR * 0.40f)

            drawLine(color = Color.White.copy(alpha = 0.5f), start = armBase, end = armPivot, strokeWidth = 2.dp.toPx())
            drawLine(color = Color.White.copy(alpha = 0.5f), start = armPivot, end = armNeedle, strokeWidth = 1.5.dp.toPx())
            drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 4.dp.toPx(), center = armPivot)
            drawRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(armNeedle.x - 4.dp.toPx(), armNeedle.y - 2.dp.toPx()),
                size = Size(8.dp.toPx(), 4.dp.toPx())
            )
        }

        // Center Label Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "33 RPM",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun CyberHudLandscapeView(
    currentEpochSeconds: Long,
    timeString: String,
    batteryPercentage: Int,
    isPlaying: Boolean,
    track: com.belta.audio.core.domain.model.Track?,
    modifier: Modifier = Modifier
) {
    val angleDegrees = if (isPlaying) ((currentEpochSeconds % 60) * 6f) else 0f

    Box(
        modifier = modifier
            .size(210.dp)
            .alpha(0.50f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.width / 2f
            val strokeW = 1.dp.toPx()

            // Outer Radar Gauge Ring with Dashes
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = maxR,
                center = center,
                style = Stroke(width = strokeW)
            )

            // 12 Outer Radar Ticks
            for (i in 0 until 12) {
                val rad = Math.toRadians((i * 30.0)).toFloat()
                val p1 = Offset(center.x + (maxR - 8.dp.toPx()) * cos(rad), center.y + (maxR - 8.dp.toPx()) * sin(rad))
                val p2 = Offset(center.x + maxR * cos(rad), center.y + maxR * sin(rad))
                drawLine(color = Color.White.copy(alpha = 0.5f), start = p1, end = p2, strokeWidth = strokeW)
            }

            // Inner Orbit Ring with 1 FPS Radar Scan
            val innerR = maxR * 0.70f
            drawCircle(color = Color.White.copy(alpha = 0.25f), radius = innerR, center = center, style = Stroke(width = strokeW))

            val radScan = Math.toRadians(angleDegrees.toDouble()).toFloat()
            val scanEnd = Offset(center.x + innerR * cos(radScan), center.y + innerR * sin(radScan))
            drawLine(color = Color.White.copy(alpha = 0.70f), start = center, end = scanEnd, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }

        // Center HUD Telemetry
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "CYBER HUD",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
            Text(
                text = timeString,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Light),
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
            val bitrateStr = if ((track?.bitrateKbps ?: 0) > 0) "${track?.bitrateKbps} KBPS" else "PCM LOSSLESS"
            Text(
                text = bitrateStr,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontFamily = FontFamily.Monospace,
                color = Color.Gray
            )
        }
    }
}

enum class SongMood(val label: String, val badgeColor: Color) {
    PARTY_DANCE("DANCING CAT • PARTY", Color(0xFFE879F9)),
    ROCK_INTENSE("HEADBANG CAT • ROCK", Color(0xFFF97316)),
    CHILL_LOFI("CHILL CAT • LO-FI", Color(0xFF38BDF8)),
    MELANCHOLY("STARGAZER CAT • MOODY", Color(0xFFA78BFA)),
    GROOVE_BEAT("BOUNCING CAT • GROOVE", Color(0xFF34D399))
}

fun detectSongMood(genre: String?, title: String?, artist: String?): SongMood {
    val text = "${genre.orEmpty()} ${title.orEmpty()} ${artist.orEmpty()}".lowercase()
    return when {
        text.contains("rock") || text.contains("metal") || text.contains("punk") || text.contains("guitar") -> SongMood.ROCK_INTENSE
        text.contains("dance") || text.contains("edm") || text.contains("pop") || text.contains("house") || text.contains("techno") || text.contains("club") || text.contains("remix") -> SongMood.PARTY_DANCE
        text.contains("chill") || text.contains("ambient") || text.contains("lo-fi") || text.contains("lofi") || text.contains("acoustic") || text.contains("jazz") || text.contains("piano") -> SongMood.CHILL_LOFI
        text.contains("sad") || text.contains("blues") || text.contains("rain") || text.contains("slow") || text.contains("ballad") -> SongMood.MELANCHOLY
        else -> SongMood.GROOVE_BEAT
    }
}

enum class InstrumentType {
    GUITAR, DRUMS, KEYBOARD, TURNTABLE, SAXOPHONE
}

data class RealCatMusician(
    val instrument: InstrumentType,
    val xRatio: Float,
    val scale: Float,
    val isFacingRight: Boolean
)

@Composable
fun RealCatBandStage(
    trackId: Long,
    currentEpochSeconds: Long,
    isPlaying: Boolean,
    songMood: SongMood,
    modifier: Modifier = Modifier
) {
    val tick = (currentEpochSeconds % 2).toInt()

    // Deterministic random stage composition seeded by trackId
    val musicians = remember(trackId) {
        val rng = java.util.Random(trackId xor 0xCAFE_BABE)
        val allInstruments = InstrumentType.entries.toMutableList()
        allInstruments.shuffle(rng)
        val count = 2 + rng.nextInt(2) // 2 or 3 musicians
        val selected = allInstruments.take(count)

        val slots = if (count == 2) {
            listOf(0.28f + (rng.nextFloat() * 0.08f - 0.04f), 0.72f + (rng.nextFloat() * 0.08f - 0.04f))
        } else {
            listOf(0.18f + (rng.nextFloat() * 0.06f - 0.03f), 0.50f + (rng.nextFloat() * 0.06f - 0.03f), 0.82f + (rng.nextFloat() * 0.06f - 0.03f))
        }

        selected.mapIndexed { idx, instrument ->
            val slotX = slots[idx]
            RealCatMusician(
                instrument = instrument,
                xRatio = slotX,
                scale = 0.90f + rng.nextFloat() * 0.20f,
                isFacingRight = if (slotX < 0.45f) true else (if (slotX > 0.65f) false else rng.nextBoolean())
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val baseY = h - 6.dp.toPx()
        val accentColor = songMood.badgeColor
        val bandAlpha = if (isPlaying) 0.55f else 0.25f
        val fgColor = Color.White.copy(alpha = bandAlpha)

        // Stage Floor Ground Line
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(4.dp.toPx(), baseY),
            end = Offset(w - 4.dp.toPx(), baseY),
            strokeWidth = 1.dp.toPx()
        )

        // 2D Line-Art Stroke specifications
        fun getLineStroke(s: Float) = Stroke(width = 2.dp.toPx() * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun getThinStroke(s: Float) = Stroke(width = 1.4.dp.toPx() * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Helper: Draw 2D Line-Art Meme Cat Body, Ears, Whiskers, and Face
        fun drawMemeCatOutline(cx: Float, cy: Float, scale: Float) {
            val lStroke = getLineStroke(scale)
            val tStroke = getThinStroke(scale)

            // 1. Cat Head & Body Contour (Line Art Only - No Solid Fill)
            val body = Path().apply {
                moveTo(cx - 26.dp.toPx() * scale, cy)
                cubicTo(
                    cx - 28.dp.toPx() * scale, cy - 18.dp.toPx() * scale,
                    cx - 22.dp.toPx() * scale, cy - 35.dp.toPx() * scale,
                    cx - 14.dp.toPx() * scale, cy - 42.dp.toPx() * scale
                )
                // Left Ear
                lineTo(cx - 11.dp.toPx() * scale, cy - 53.dp.toPx() * scale)
                lineTo(cx + 0.dp.toPx() * scale, cy - 45.dp.toPx() * scale)
                // Top of Head
                lineTo(cx + 8.dp.toPx() * scale, cy - 45.dp.toPx() * scale)
                // Right Ear
                lineTo(cx + 19.dp.toPx() * scale, cy - 53.dp.toPx() * scale)
                lineTo(cx + 22.dp.toPx() * scale, cy - 42.dp.toPx() * scale)
                // Right Flank
                cubicTo(
                    cx + 30.dp.toPx() * scale, cy - 35.dp.toPx() * scale,
                    cx + 36.dp.toPx() * scale, cy - 18.dp.toPx() * scale,
                    cx + 34.dp.toPx() * scale, cy
                )
                close()
            }
            drawPath(body, fgColor, style = lStroke)

            // 2. Inner Ear Contour Lines (Pink/Accent line)
            drawLine(
                color = Color(0xFFF472B6).copy(alpha = 0.65f),
                start = Offset(cx - 9.dp.toPx() * scale, cy - 49.dp.toPx() * scale),
                end = Offset(cx - 2.dp.toPx() * scale, cy - 44.dp.toPx() * scale),
                strokeWidth = 1.4.dp.toPx() * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFF472B6).copy(alpha = 0.65f),
                start = Offset(cx + 17.dp.toPx() * scale, cy - 49.dp.toPx() * scale),
                end = Offset(cx + 10.dp.toPx() * scale, cy - 44.dp.toPx() * scale),
                strokeWidth = 1.4.dp.toPx() * scale,
                cap = StrokeCap.Round
            )

            // 3. Whiskers (Line Art)
            // Left whiskers
            drawLine(color = fgColor, start = Offset(cx - 22.dp.toPx() * scale, cy - 27.dp.toPx() * scale), end = Offset(cx - 33.dp.toPx() * scale, cy - 29.dp.toPx() * scale), strokeWidth = 1.2.dp.toPx() * scale, cap = StrokeCap.Round)
            drawLine(color = fgColor, start = Offset(cx - 22.dp.toPx() * scale, cy - 23.dp.toPx() * scale), end = Offset(cx - 32.dp.toPx() * scale, cy - 22.dp.toPx() * scale), strokeWidth = 1.2.dp.toPx() * scale, cap = StrokeCap.Round)
            // Right whiskers
            drawLine(color = fgColor, start = Offset(cx + 26.dp.toPx() * scale, cy - 27.dp.toPx() * scale), end = Offset(cx + 37.dp.toPx() * scale, cy - 29.dp.toPx() * scale), strokeWidth = 1.2.dp.toPx() * scale, cap = StrokeCap.Round)
            drawLine(color = fgColor, start = Offset(cx + 26.dp.toPx() * scale, cy - 23.dp.toPx() * scale), end = Offset(cx + 36.dp.toPx() * scale, cy - 22.dp.toPx() * scale), strokeWidth = 1.2.dp.toPx() * scale, cap = StrokeCap.Round)

            // 4. Face Expressions
            if (!isPlaying) {
                // Sleeping face: _ _ with small w mouth and z Z
                val leftEye = Path().apply {
                    moveTo(cx - 12.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                    quadraticBezierTo(cx - 7.dp.toPx() * scale, cy - 25.dp.toPx() * scale, cx - 2.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                }
                drawPath(leftEye, fgColor, style = lStroke)
                val rightEye = Path().apply {
                    moveTo(cx + 6.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                    quadraticBezierTo(cx + 11.dp.toPx() * scale, cy - 25.dp.toPx() * scale, cx + 16.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                }
                drawPath(rightEye, fgColor, style = lStroke)
                val mouth = Path().apply {
                    moveTo(cx - 1.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                    quadraticBezierTo(cx + 2.dp.toPx() * scale, cy - 20.dp.toPx() * scale, cx + 5.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                }
                drawPath(mouth, fgColor, style = tStroke)
            } else {
                when (songMood) {
                    SongMood.ROCK_INTENSE -> {
                        // >w< Rocking Meme Face
                        val leftEye = Path().apply {
                            moveTo(cx - 12.dp.toPx() * scale, cy - 31.dp.toPx() * scale)
                            lineTo(cx - 5.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                            lineTo(cx - 12.dp.toPx() * scale, cy - 25.dp.toPx() * scale)
                        }
                        drawPath(leftEye, fgColor, style = lStroke)
                        val rightEye = Path().apply {
                            moveTo(cx + 16.dp.toPx() * scale, cy - 31.dp.toPx() * scale)
                            lineTo(cx + 9.dp.toPx() * scale, cy - 28.dp.toPx() * scale)
                            lineTo(cx + 16.dp.toPx() * scale, cy - 25.dp.toPx() * scale)
                        }
                        drawPath(rightEye, fgColor, style = lStroke)
                        val mouth = Path().apply {
                            moveTo(cx - 2.dp.toPx() * scale, cy - 23.dp.toPx() * scale)
                            quadraticBezierTo(cx + 2.dp.toPx() * scale, cy - 19.dp.toPx() * scale, cx + 6.dp.toPx() * scale, cy - 23.dp.toPx() * scale)
                        }
                        drawPath(mouth, fgColor, style = tStroke)
                    }
                    SongMood.CHILL_LOFI -> {
                        // (•ㅅ•) Chill Meme Face
                        drawCircle(color = fgColor, radius = 2.dp.toPx() * scale, center = Offset(cx - 8.dp.toPx() * scale, cy - 28.dp.toPx() * scale))
                        drawCircle(color = fgColor, radius = 2.dp.toPx() * scale, center = Offset(cx + 12.dp.toPx() * scale, cy - 28.dp.toPx() * scale))
                        val mouth = Path().apply {
                            moveTo(cx - 3.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                            quadraticBezierTo(cx - 0.5.dp.toPx() * scale, cy - 24.dp.toPx() * scale, cx + 2.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                            quadraticBezierTo(cx + 4.5.dp.toPx() * scale, cy - 24.dp.toPx() * scale, cx + 7.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                        }
                        drawPath(mouth, fgColor, style = tStroke)
                    }
                    else -> {
                        // ^w^ Classic Happy Bongo Cat Face
                        val leftEye = Path().apply {
                            moveTo(cx - 12.dp.toPx() * scale, cy - 27.dp.toPx() * scale)
                            quadraticBezierTo(cx - 7.dp.toPx() * scale, cy - 32.dp.toPx() * scale, cx - 2.dp.toPx() * scale, cy - 27.dp.toPx() * scale)
                        }
                        drawPath(leftEye, fgColor, style = lStroke)
                        val rightEye = Path().apply {
                            moveTo(cx + 6.dp.toPx() * scale, cy - 27.dp.toPx() * scale)
                            quadraticBezierTo(cx + 11.dp.toPx() * scale, cy - 32.dp.toPx() * scale, cx + 16.dp.toPx() * scale, cy - 27.dp.toPx() * scale)
                        }
                        drawPath(rightEye, fgColor, style = lStroke)
                        // :3 Cute mouth
                        val mouth = Path().apply {
                            moveTo(cx - 3.dp.toPx() * scale, cy - 23.dp.toPx() * scale)
                            quadraticBezierTo(cx - 0.5.dp.toPx() * scale, cy - 20.dp.toPx() * scale, cx + 2.dp.toPx() * scale, cy - 22.dp.toPx() * scale)
                            quadraticBezierTo(cx + 4.5.dp.toPx() * scale, cy - 20.dp.toPx() * scale, cx + 7.dp.toPx() * scale, cy - 23.dp.toPx() * scale)
                        }
                        drawPath(mouth, fgColor, style = tStroke)
                    }
                }
            }
        }

        // Draw each randomized meme musician and their instrument (Line Art Only)
        musicians.forEach { musician ->
            val catX = w * musician.xRatio
            val catScale = musician.scale
            val lStroke = getLineStroke(catScale)
            val tStroke = getThinStroke(catScale)

            // 1. Draw Cat Line Art Body
            drawMemeCatOutline(catX, baseY, catScale)

            // 2. Draw Line Art Instrument & Paws
            when (musician.instrument) {
                InstrumentType.DRUMS -> {
                    // Twin Bongo Drums
                    val bongoY = baseY - 8.dp.toPx() * catScale
                    val drumStroke = Stroke(width = 1.4.dp.toPx() * catScale, cap = StrokeCap.Round, join = StrokeJoin.Round)

                    // Left Drum
                    val leftDrumTop = Offset(catX - 11.dp.toPx() * catScale, bongoY)
                    drawOval(color = accentColor, topLeft = Offset(leftDrumTop.x - 9.dp.toPx() * catScale, leftDrumTop.y - 3.dp.toPx() * catScale), size = Size(18.dp.toPx() * catScale, 6.dp.toPx() * catScale), style = drumStroke)
                    val leftDrumBody = Path().apply {
                        moveTo(leftDrumTop.x - 9.dp.toPx() * catScale, leftDrumTop.y)
                        lineTo(leftDrumTop.x - 6.dp.toPx() * catScale, baseY)
                        lineTo(leftDrumTop.x + 6.dp.toPx() * catScale, baseY)
                        lineTo(leftDrumTop.x + 9.dp.toPx() * catScale, leftDrumTop.y)
                    }
                    drawPath(leftDrumBody, accentColor, style = drumStroke)

                    // Right Drum
                    val rightDrumTop = Offset(catX + 15.dp.toPx() * catScale, bongoY)
                    drawOval(color = accentColor, topLeft = Offset(rightDrumTop.x - 9.dp.toPx() * catScale, rightDrumTop.y - 3.dp.toPx() * catScale), size = Size(18.dp.toPx() * catScale, 6.dp.toPx() * catScale), style = drumStroke)
                    val rightDrumBody = Path().apply {
                        moveTo(rightDrumTop.x - 9.dp.toPx() * catScale, rightDrumTop.y)
                        lineTo(rightDrumTop.x - 6.dp.toPx() * catScale, baseY)
                        lineTo(rightDrumTop.x + 6.dp.toPx() * catScale, baseY)
                        lineTo(rightDrumTop.x + 9.dp.toPx() * catScale, rightDrumTop.y)
                    }
                    drawPath(rightDrumBody, accentColor, style = drumStroke)

                    // Alternating Bongo Bean Paws (1 FPS)
                    val leftPawDown = isPlaying && (tick == 0)
                    val rightPawDown = isPlaying && (tick == 1)

                    // Left Paw
                    val leftPaw = Path().apply {
                        moveTo(catX - 19.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        if (leftPawDown) {
                            quadraticBezierTo(catX - 13.dp.toPx() * catScale, baseY - 6.dp.toPx() * catScale, catX - 11.dp.toPx() * catScale, baseY - 8.dp.toPx() * catScale)
                            quadraticBezierTo(catX - 8.dp.toPx() * catScale, baseY - 12.dp.toPx() * catScale, catX - 14.dp.toPx() * catScale, baseY - 18.dp.toPx() * catScale)
                        } else {
                            quadraticBezierTo(catX - 15.dp.toPx() * catScale, baseY - 26.dp.toPx() * catScale, catX - 10.dp.toPx() * catScale, baseY - 24.dp.toPx() * catScale)
                            quadraticBezierTo(catX - 6.dp.toPx() * catScale, baseY - 19.dp.toPx() * catScale, catX - 14.dp.toPx() * catScale, baseY - 17.dp.toPx() * catScale)
                        }
                    }
                    drawPath(leftPaw, fgColor, style = lStroke)

                    // Right Paw
                    val rightPaw = Path().apply {
                        moveTo(catX + 23.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        if (rightPawDown) {
                            quadraticBezierTo(catX + 17.dp.toPx() * catScale, baseY - 6.dp.toPx() * catScale, catX + 15.dp.toPx() * catScale, baseY - 8.dp.toPx() * catScale)
                            quadraticBezierTo(catX + 12.dp.toPx() * catScale, baseY - 12.dp.toPx() * catScale, catX + 17.dp.toPx() * catScale, baseY - 18.dp.toPx() * catScale)
                        } else {
                            quadraticBezierTo(catX + 19.dp.toPx() * catScale, baseY - 26.dp.toPx() * catScale, catX + 14.dp.toPx() * catScale, baseY - 24.dp.toPx() * catScale)
                            quadraticBezierTo(catX + 10.dp.toPx() * catScale, baseY - 19.dp.toPx() * catScale, catX + 17.dp.toPx() * catScale, baseY - 17.dp.toPx() * catScale)
                        }
                    }
                    drawPath(rightPaw, fgColor, style = lStroke)

                    if (isPlaying && tick == 1) {
                        // Floating note
                        drawCircle(color = Color(0xFFA78BFA), radius = 1.8.dp.toPx() * catScale, center = Offset(catX + 28.dp.toPx() * catScale, baseY - 32.dp.toPx() * catScale))
                        drawLine(color = Color(0xFFA78BFA), start = Offset(catX + 29.8.dp.toPx() * catScale, baseY - 32.dp.toPx() * catScale), end = Offset(catX + 29.8.dp.toPx() * catScale, baseY - 38.dp.toPx() * catScale), strokeWidth = 1.dp.toPx())
                    }
                }

                InstrumentType.GUITAR -> {
                    val gBodyX = catX - 4.dp.toPx() * catScale
                    val gBodyY = baseY - 10.dp.toPx() * catScale
                    val guitarColor = Color(0xFFFBBF24).copy(alpha = if (isPlaying) 0.85f else 0.40f)

                    // Contoured Guitar Body Line Art
                    val gBody = Path().apply {
                        moveTo(gBodyX - 12.dp.toPx() * catScale, gBodyY - 4.dp.toPx() * catScale)
                        cubicTo(gBodyX - 16.dp.toPx() * catScale, gBodyY - 8.dp.toPx() * catScale, gBodyX - 16.dp.toPx() * catScale, gBodyY + 8.dp.toPx() * catScale, gBodyX - 6.dp.toPx() * catScale, gBodyY + 8.dp.toPx() * catScale)
                        cubicTo(gBodyX + 4.dp.toPx() * catScale, gBodyY + 8.dp.toPx() * catScale, gBodyX + 8.dp.toPx() * catScale, gBodyY - 2.dp.toPx() * catScale, gBodyX + 2.dp.toPx() * catScale, gBodyY - 6.dp.toPx() * catScale)
                        cubicTo(gBodyX - 2.dp.toPx() * catScale, gBodyY - 8.dp.toPx() * catScale, gBodyX - 4.dp.toPx() * catScale, gBodyY - 2.dp.toPx() * catScale, gBodyX - 12.dp.toPx() * catScale, gBodyY - 4.dp.toPx() * catScale)
                        close()
                    }
                    drawPath(gBody, guitarColor, style = tStroke)

                    // Neck and Headstock
                    val neckEnd = Offset(gBodyX + 28.dp.toPx() * catScale, gBodyY - 22.dp.toPx() * catScale)
                    drawLine(color = guitarColor, start = Offset(gBodyX, gBodyY - 2.dp.toPx() * catScale), end = neckEnd, strokeWidth = 1.6.dp.toPx() * catScale)
                    drawCircle(color = guitarColor, radius = 2.dp.toPx() * catScale, center = neckEnd)

                    // Left Paw holding neck
                    val leftPaw = Path().apply {
                        moveTo(catX + 18.dp.toPx() * catScale, baseY - 24.dp.toPx() * catScale)
                        quadraticBezierTo(catX + 22.dp.toPx() * catScale, baseY - 20.dp.toPx() * catScale, catX + 16.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                    }
                    drawPath(leftPaw, fgColor, style = lStroke)

                    // Right Paw Strumming
                    val strumY = if (isPlaying && tick == 1) gBodyY + 2.dp.toPx() * catScale else gBodyY - 4.dp.toPx() * catScale
                    val rightPaw = Path().apply {
                        moveTo(catX - 18.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        quadraticBezierTo(catX - 10.dp.toPx() * catScale, strumY, catX - 4.dp.toPx() * catScale, strumY)
                        quadraticBezierTo(catX - 8.dp.toPx() * catScale, strumY + 6.dp.toPx() * catScale, catX - 16.dp.toPx() * catScale, baseY - 12.dp.toPx() * catScale)
                    }
                    drawPath(rightPaw, fgColor, style = lStroke)

                    if (isPlaying && tick == 1) {
                        drawCircle(color = Color(0xFFA78BFA), radius = 1.8.dp.toPx() * catScale, center = Offset(neckEnd.x + 4.dp.toPx(), neckEnd.y - 4.dp.toPx()))
                        drawLine(color = Color(0xFFA78BFA), start = Offset(neckEnd.x + 5.8.dp.toPx(), neckEnd.y - 4.dp.toPx()), end = Offset(neckEnd.x + 5.8.dp.toPx(), neckEnd.y - 10.dp.toPx()), strokeWidth = 1.2.dp.toPx())
                    }
                }

                InstrumentType.KEYBOARD -> {
                    val kY = baseY - 10.dp.toPx() * catScale
                    val kColor = Color(0xFFF472B6).copy(alpha = if (isPlaying) 0.85f else 0.40f)

                    // X-Stand outline lines
                    drawLine(color = kColor, start = Offset(catX - 20.dp.toPx() * catScale, baseY), end = Offset(catX + 20.dp.toPx() * catScale, kY), strokeWidth = 1.2.dp.toPx())
                    drawLine(color = kColor, start = Offset(catX + 20.dp.toPx() * catScale, baseY), end = Offset(catX - 20.dp.toPx() * catScale, kY), strokeWidth = 1.2.dp.toPx())

                    // Keyboard deck outline
                    drawRoundRect(
                        color = kColor,
                        topLeft = Offset(catX - 25.dp.toPx() * catScale, kY - 5.dp.toPx() * catScale),
                        size = Size(50.dp.toPx() * catScale, 9.dp.toPx() * catScale),
                        cornerRadius = CornerRadius(1.5.dp.toPx() * catScale),
                        style = tStroke
                    )
                    // White & Black Piano key lines
                    for (i in 0..6) {
                        val keyX = catX - 21.dp.toPx() * catScale + (i * 7.dp.toPx() * catScale)
                        drawLine(color = kColor, start = Offset(keyX, kY - 5.dp.toPx() * catScale), end = Offset(keyX, kY + 4.dp.toPx() * catScale), strokeWidth = 1.dp.toPx())
                    }

                    // Paws on keys shifting at 1 FPS
                    val pawShift = if (isPlaying && tick == 1) 3.dp.toPx() * catScale else -3.dp.toPx() * catScale
                    val leftPaw = Path().apply {
                        moveTo(catX - 18.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        quadraticBezierTo(catX - 14.dp.toPx() * catScale, kY - 6.dp.toPx() * catScale, catX - 8.dp.toPx() * catScale + pawShift, kY - 4.dp.toPx() * catScale)
                    }
                    drawPath(leftPaw, fgColor, style = lStroke)
                    val rightPaw = Path().apply {
                        moveTo(catX + 22.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        quadraticBezierTo(catX + 18.dp.toPx() * catScale, kY - 6.dp.toPx() * catScale, catX + 12.dp.toPx() * catScale - pawShift, kY - 4.dp.toPx() * catScale)
                    }
                    drawPath(rightPaw, fgColor, style = lStroke)

                    if (isPlaying && tick == 0) {
                        drawArc(color = accentColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(catX - 12.dp.toPx() * catScale, kY - 14.dp.toPx() * catScale), size = Size(24.dp.toPx() * catScale, 8.dp.toPx() * catScale), style = tStroke)
                    }
                }

                InstrumentType.TURNTABLE -> {
                    val tY = baseY - 8.dp.toPx() * catScale
                    val djColor = Color(0xFFA78BFA).copy(alpha = if (isPlaying) 0.85f else 0.40f)

                    // Cat DJ Headphones over ears
                    drawArc(
                        color = djColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(catX - 16.dp.toPx() * catScale, baseY - 58.dp.toPx() * catScale),
                        size = Size(32.dp.toPx() * catScale, 22.dp.toPx() * catScale),
                        style = Stroke(width = 2.4.dp.toPx() * catScale, cap = StrokeCap.Round)
                    )
                    drawCircle(color = djColor, radius = 3.5.dp.toPx() * catScale, center = Offset(catX - 16.dp.toPx() * catScale, baseY - 47.dp.toPx() * catScale), style = tStroke)
                    drawCircle(color = djColor, radius = 3.5.dp.toPx() * catScale, center = Offset(catX + 16.dp.toPx() * catScale, baseY - 47.dp.toPx() * catScale), style = tStroke)

                    // Turntable Console Outline
                    drawRoundRect(
                        color = djColor,
                        topLeft = Offset(catX - 20.dp.toPx() * catScale, tY),
                        size = Size(40.dp.toPx() * catScale, 12.dp.toPx() * catScale),
                        cornerRadius = CornerRadius(1.5.dp.toPx() * catScale),
                        style = tStroke
                    )
                    // Vinyl disc with grooves
                    val discCenter = Offset(catX - 4.dp.toPx() * catScale, tY + 6.dp.toPx() * catScale)
                    drawOval(color = djColor, topLeft = Offset(discCenter.x - 9.dp.toPx() * catScale, discCenter.y - 4.dp.toPx() * catScale), size = Size(18.dp.toPx() * catScale, 8.dp.toPx() * catScale), style = tStroke)
                    drawCircle(color = Color.White, radius = 1.5.dp.toPx() * catScale, center = discCenter)
                    // Tonearm line
                    drawLine(color = djColor, start = Offset(catX + 12.dp.toPx() * catScale, tY + 2.dp.toPx() * catScale), end = discCenter, strokeWidth = 1.dp.toPx())

                    // Scratching Paw
                    val scratchPawX = if (isPlaying && tick == 1) discCenter.x + 3.dp.toPx() * catScale else discCenter.x - 3.dp.toPx() * catScale
                    val leftPaw = Path().apply {
                        moveTo(catX - 18.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                        quadraticBezierTo(catX - 10.dp.toPx() * catScale, tY + 2.dp.toPx() * catScale, scratchPawX, discCenter.y)
                    }
                    drawPath(leftPaw, fgColor, style = lStroke)
                }

                InstrumentType.SAXOPHONE -> {
                    val saxColor = Color(0xFFFBBF24).copy(alpha = if (isPlaying) 0.85f else 0.40f)

                    // Curved brass saxophone tube from mouth
                    val saxPath = Path().apply {
                        moveTo(catX + 6.dp.toPx() * catScale, baseY - 24.dp.toPx() * catScale)
                        quadraticBezierTo(catX + 14.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale, catX + 16.dp.toPx() * catScale, baseY - 8.dp.toPx() * catScale)
                        quadraticBezierTo(catX + 20.dp.toPx() * catScale, baseY, catX + 24.dp.toPx() * catScale, baseY - 8.dp.toPx() * catScale)
                        lineTo(catX + 26.dp.toPx() * catScale, baseY - 16.dp.toPx() * catScale)
                    }
                    drawPath(saxPath, saxColor, style = tStroke)
                    // Flared bell
                    drawOval(color = saxColor, topLeft = Offset(catX + 23.dp.toPx() * catScale, baseY - 19.dp.toPx() * catScale), size = Size(6.dp.toPx() * catScale, 3.5.dp.toPx() * catScale), style = tStroke)

                    // Paws holding horn
                    val paws = Path().apply {
                        moveTo(catX + 2.dp.toPx() * catScale, baseY - 18.dp.toPx() * catScale)
                        lineTo(catX + 12.dp.toPx() * catScale, baseY - 14.dp.toPx() * catScale)
                    }
                    drawPath(paws, fgColor, style = lStroke)

                    if (isPlaying && tick == 1) {
                        drawCircle(color = saxColor, radius = 1.8.dp.toPx() * catScale, center = Offset(catX + 30.dp.toPx() * catScale, baseY - 22.dp.toPx() * catScale))
                        drawLine(color = saxColor, start = Offset(catX + 31.8.dp.toPx() * catScale, baseY - 22.dp.toPx() * catScale), end = Offset(catX + 31.8.dp.toPx() * catScale, baseY - 28.dp.toPx() * catScale), strokeWidth = 1.2.dp.toPx())
                    }
                }
            }
        }
    }
}
