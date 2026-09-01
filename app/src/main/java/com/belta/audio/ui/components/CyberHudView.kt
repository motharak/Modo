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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.belta.audio.core.domain.model.AudioSpecs

@Composable
fun CyberHudView(
    specs: AudioSpecs,
    isPlaying: Boolean,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    hudSize: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CyberHudSpin")
    val hudRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HudRing"
    )

    val currentHudRotation = if (isPlaying) hudRotation else 0f

    Box(
        modifier = modifier
            .size(hudSize)
            .shadow(20.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF070B12))
            .border(2.dp, Color(0xFF00F0FF).copy(alpha = 0.6f), CircleShape)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating Outer HUD Telemetry Ticks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(currentHudRotation)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4f

            // Neon cyan outer ring
            drawCircle(
                color = Color(0xFF00F0FF).copy(alpha = 0.3f),
                radius = radius,
                style = Stroke(width = 2f)
            )

            // Segmented compass ticks
            for (i in 0 until 36) {
                val angle = (i * 10f) * (Math.PI / 180f)
                val len = if (i % 9 == 0) 10f else 5f
                val startX = center.x + (radius - len) * kotlin.math.cos(angle).toFloat()
                val startY = center.y + (radius - len) * kotlin.math.sin(angle).toFloat()
                val endX = center.x + radius * kotlin.math.cos(angle).toFloat()
                val endY = center.y + radius * kotlin.math.sin(angle).toFloat()

                drawLine(
                    color = if (i % 9 == 0) Color(0xFF00F0FF) else Color(0xFF00F0FF).copy(alpha = 0.4f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }

        // Inner Progress Arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFF00F0FF), Color(0xFFFF007F), Color(0xFF00F0FF))
                ),
                startAngle = -90f,
                sweepAngle = 360f * progressFraction,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = arcSize,
                style = Stroke(width = stroke)
            )
        }

        // Center Cyber Telemetry Data
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "AUDIO HUD v2.4",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF00F0FF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = specs.containerFormat.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = "${specs.bitDepthFormatted} / ${specs.sampleRateKHzFormatted}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFFF007F)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = specs.bitrateFormatted,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF00E676)
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Animated Real-Time Spectrum Mini Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.height(18.dp)
            ) {
                for (b in 1..7) {
                    val barHeight = if (isPlaying) (8 + ((b * 5 + (progressFraction * 100).toInt()) % 11)).dp else 4.dp
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (b % 2 == 0) Color(0xFF00F0FF) else Color(0xFFFF007F))
                    )
                }
            }
        }
    }
}
