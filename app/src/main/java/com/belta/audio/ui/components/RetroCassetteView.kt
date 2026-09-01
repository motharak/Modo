package com.belta.audio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RetroCassetteView(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 290.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CassetteSpool")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpoolRotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f
    val tapeProgress = progressFraction.coerceIn(0.05f, 0.95f)

    // Cassette Shell Container
    Box(
        modifier = modifier
            .width(size)
            .height(size * 0.65f)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2C2D30), Color(0xFF1B1C1E), Color(0xFF141416))
                )
            )
            .border(2.dp, Color(0xFF4A4B50), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cassette Top Vintage Label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFD97706))
                        )
                    )
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SIDE A  •  $title - $artist",
                        color = Color(0xFF1F1F1F),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "TYPE II CrO2",
                        color = Color(0xFF1F1F1F),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // Cassette Center Window with Spools & Tape
            Box(
                modifier = Modifier
                    .width(size * 0.72f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1012))
                    .border(1.dp, Color(0xFF38393F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Spool (shrinks as song plays)
                    CassetteSpool(
                        rotation = currentRotation,
                        tapeThicknessFraction = 1.0f - tapeProgress
                    )

                    // Center Tape Counter & VU Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%03d", (progressFraction * 999).toInt()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB800)
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF222428))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF3FB950), Color(0xFFD29922), Color(0xFFF85149))
                                        )
                                    )
                            )
                        }
                    }

                    // Right Spool (grows as song plays)
                    CassetteSpool(
                        rotation = currentRotation,
                        tapeThicknessFraction = tapeProgress
                    )
                }
            }

            // Cassette Bottom Magnetic Tape Head Window & Screws
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrewDot()
                Text(
                    text = "HI-FI STEREO CASSETTE DECK",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                    color = Color(0xFF6E7178),
                    letterSpacing = 1.sp
                )
                ScrewDot()
            }
        }
    }
}

@Composable
fun CassetteSpool(rotation: Float, tapeThicknessFraction: Float) {
    Box(
        modifier = Modifier
            .size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Brown Magnetic Tape Pack
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxTapeRadius = (size.minDimension / 2f)
            val currentTapeR = 12f + (maxTapeRadius - 12f) * tapeThicknessFraction.coerceIn(0.1f, 1.0f)

            // Wound tape ring
            drawCircle(
                color = Color(0xFF3E2723),
                radius = currentTapeR,
                center = center
            )
            // Outer white cog ring
            drawCircle(
                color = Color(0xFFE5E7EB),
                radius = 12f,
                center = center
            )
            // Center axle hole
            drawCircle(
                color = Color(0xFF111827),
                radius = 6f,
                center = center
            )
        }

        // Rotating Spool Cog Teeth
        Box(
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (Math.PI / 180f)
                    val x = center.x + 8f * kotlin.math.cos(angle).toFloat()
                    val y = center.y + 8f * kotlin.math.sin(angle).toFloat()
                    drawCircle(
                        color = Color(0xFF111827),
                        radius = 2.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun ScrewDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF4A4B50))
            .border(1.dp, Color(0xFF6E7178), CircleShape)
    )
}
