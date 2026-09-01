package com.belta.audio.ui.components.progressbar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belta.audio.ui.screens.player.styles.ProgressBarStyle
import java.util.Locale

@Composable
fun CustomMusicProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    style: ProgressBarStyle,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = (currentPositionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val displayProgress = if (isDragging) dragProgress else actualProgress

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .pointerInput(totalMs) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((newProgress * totalMs).toLong())
                    }
                }
                .pointerInput(totalMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragProgress * totalMs).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when (style) {
                ProgressBarStyle.DYNAMIC_WAVEFORM -> {
                    WaveformProgressBar(progress = displayProgress)
                }
                ProgressBarStyle.MAGNETIC_TAPE_RIBBON -> {
                    TapeRibbonProgressBar(progress = displayProgress)
                }
                ProgressBarStyle.NEON_LASER -> {
                    NeonLaserProgressBar(progress = displayProgress)
                }
                ProgressBarStyle.LIQUID_CAPSULE -> {
                    LiquidCapsuleProgressBar(progress = displayProgress)
                }
                ProgressBarStyle.SEGMENTED_TICKS -> {
                    SegmentedTicksProgressBar(progress = displayProgress)
                }
            }
        }

        // Timestamps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayMs = if (isDragging) (dragProgress * totalMs).toLong() else currentPositionMs
            Text(
                text = formatDuration(displayMs),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WaveformProgressBar(progress: Float) {
    val barCount = 50
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val totalWidth = size.width
        val barWidth = (totalWidth / barCount) * 0.65f
        val gap = (totalWidth / barCount) * 0.35f

        for (i in 0 until barCount) {
            val frac = i.toFloat() / barCount.toFloat()
            val isPlayed = frac <= progress
            val x = i * (barWidth + gap)
            // Simulated acoustic peaks
            val normalizedHeight = 0.2f + 0.8f * kotlin.math.abs(kotlin.math.sin(i * 0.35f)).toFloat()
            val barH = size.height * normalizedHeight
            val y = (size.height - barH) / 2f

            drawRoundRect(
                color = if (isPlayed) Color(0xFF6750A4) else Color(0xFF6750A4).copy(alpha = 0.25f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun TapeRibbonProgressBar(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val y = size.height / 2f
        // Magnetic tape ribbon background
        drawRoundRect(
            color = Color(0xFF3E2723),
            topLeft = Offset(0f, y - 4f),
            size = Size(size.width, 8f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Wound tape fill
        drawRoundRect(
            color = Color(0xFFD97706),
            topLeft = Offset(0f, y - 4f),
            size = Size(size.width * progress, 8f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Magnetic tape head scrubber knob
        val headX = size.width * progress
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = 8f,
            center = Offset(headX, y)
        )
        drawCircle(
            color = Color(0xFF1F2937),
            radius = 3f,
            center = Offset(headX, y)
        )
    }
}

@Composable
fun NeonLaserProgressBar(progress: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val y = size.height / 2f
        // Laser track
        drawLine(
            color = Color(0xFF00F0FF).copy(alpha = 0.2f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 4f
        )
        // Glowing laser line
        val headX = size.width * progress
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFF007F), Color(0xFF00F0FF)),
                startX = 0f,
                endX = headX.coerceAtLeast(1f)
            ),
            start = Offset(0f, y),
            end = Offset(headX, y),
            strokeWidth = 6f
        )
        // Laser point particle
        drawCircle(
            color = Color(0xFF00F0FF),
            radius = 7f,
            center = Offset(headX, y)
        )
    }
}

@Composable
fun LiquidCapsuleProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
    }
}

@Composable
fun SegmentedTicksProgressBar(progress: Float) {
    val totalTicks = 32
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalTicks) {
            val frac = i.toFloat() / totalTicks.toFloat()
            val isFilled = frac <= progress
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(if (i % 4 == 0) 1.0f else 0.6f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isFilled) {
                            if (i > totalTicks * 0.8f) Color(0xFFF85149) else Color(0xFF3FB950)
                        } else {
                            Color(0xFF30363D)
                        }
                    )
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
