package com.belta.audio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belta.audio.core.audio.FadeCurve
import com.belta.audio.core.domain.model.Playlist
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistRemixFlowSheet(
    playlist: Playlist,
    globalCrossfadeSeconds: Int,
    onDismiss: () -> Unit,
    onSaveFlowConfig: (crossfadeSeconds: Int?, isAutomixEnabled: Boolean, fadeCurve: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentDuration by remember(playlist) {
        mutableFloatStateOf((playlist.crossfadeSeconds ?: globalCrossfadeSeconds).toFloat())
    }
    var isAutomix by remember(playlist) {
        mutableStateOf(playlist.isAutomixEnabled)
    }
    var selectedCurve by remember(playlist) {
        mutableStateOf(
            try {
                FadeCurve.valueOf(playlist.fadeCurve)
            } catch (_: Exception) {
                FadeCurve.EQUAL_POWER
            }
        )
    }
    var isCustomOverride by remember(playlist) {
        mutableStateOf(playlist.crossfadeSeconds != null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131722),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Remix Flow & Crossfade",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (isCustomOverride) {
                    TextButton(
                        onClick = {
                            isCustomOverride = false
                            currentDuration = globalCrossfadeSeconds.toFloat()
                            selectedCurve = FadeCurve.EQUAL_POWER
                            isAutomix = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Interactive Transition Overlap Waveform Canvas
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E2433),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                CrossfadeWaveformPreview(
                    durationSeconds = currentDuration.toInt(),
                    curve = selectedCurve
                )
            }

            // Duration Slider
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossfade Overlap",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentDuration.toInt() == 0) "Gapless (Off)" else "${currentDuration.toInt()}s blend",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (currentDuration.toInt() == 0) Color(0xFF64748B) else Color(0xFF818CF8)
                    )
                }

                Slider(
                    value = currentDuration,
                    onValueChange = {
                        currentDuration = it
                        isCustomOverride = true
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF818CF8),
                        activeTrackColor = Color(0xFF6366F1),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text("4s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text("8s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text("12s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                }
            }

            // Spotify Automix Switch
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF1E2433),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isAutomix = !isAutomix
                            isCustomOverride = true
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Spotify Automix",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Auto-trims dead air and blends songs on the beat",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Switch(
                        checked = isAutomix,
                        onCheckedChange = {
                            isAutomix = it
                            isCustomOverride = true
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }

            // Transition Curve Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Acoustic Energy Curve",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FadeCurve.entries.forEach { curve ->
                        val isSelected = selectedCurve == curve
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCurve = curve
                                isCustomOverride = true
                            },
                            label = {
                                Text(
                                    text = when (curve) {
                                        FadeCurve.EQUAL_POWER -> "Equal Power"
                                        FadeCurve.EXPONENTIAL -> "Exponential"
                                        FadeCurve.LINEAR -> "Linear"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6366F1),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E2433),
                                labelColor = Color(0xFF94A3B8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = Color(0xFF818CF8),
                                borderColor = Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            // Save & Apply Button
            Button(
                onClick = {
                    val finalSec = if (isCustomOverride) currentDuration.toInt() else null
                    onSaveFlowConfig(finalSec, isAutomix, selectedCurve.name)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text(
                    text = "Apply to ${playlist.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun CrossfadeWaveformPreview(
    durationSeconds: Int,
    curve: FadeCurve,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        val w = size.width
        val h = size.height

        val outPath = Path()
        val inPath = Path()

        val steps = 60
        for (i in 0..steps) {
            val fraction = i.toFloat() / steps.toFloat()
            val x = fraction * w

            val outVol = when (curve) {
                FadeCurve.EQUAL_POWER -> cos(Math.PI * 0.5 * fraction).toFloat()
                FadeCurve.EXPONENTIAL -> (1f - fraction) * (1f - fraction)
                FadeCurve.LINEAR -> 1f - fraction
            }

            val inVol = when (curve) {
                FadeCurve.EQUAL_POWER -> sin(Math.PI * 0.5 * fraction).toFloat()
                FadeCurve.EXPONENTIAL -> fraction * fraction
                FadeCurve.LINEAR -> fraction
            }

            val yOut = h - (outVol * (h - 8.dp.toPx())) - 4.dp.toPx()
            val yIn = h - (inVol * (h - 8.dp.toPx())) - 4.dp.toPx()

            if (i == 0) {
                outPath.moveTo(x, yOut)
                inPath.moveTo(x, yIn)
            } else {
                outPath.lineTo(x, yOut)
                inPath.lineTo(x, yIn)
            }
        }

        // Draw Outgoing Track Line (Red/Orange gradient)
        drawPath(
            path = outPath,
            brush = Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB923C))),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Incoming Track Line (Indigo/Cyan gradient)
        drawPath(
            path = inPath,
            brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8))),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Midpoint energy marker
        val midX = w * 0.5f
        drawLine(
            color = Color(0xFF475569),
            start = Offset(midX, 0f),
            end = Offset(midX, h),
            strokeWidth = 1.dp.toPx()
        )
    }
}
