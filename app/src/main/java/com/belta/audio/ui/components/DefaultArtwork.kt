package com.belta.audio.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.belta.audio.R
import kotlin.math.absoluteValue

@Composable
fun DefaultArtwork(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val paletteIndex = remember(title, artist) {
        (title.hashCode() + artist.hashCode()).absoluteValue % GradientPalettes.size
    }
    val gradientColors = GradientPalettes[paletteIndex]

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val minDim = if (maxWidth < maxHeight) maxWidth else maxHeight
        val logoSize = (minDim * 0.72f).coerceAtLeast(24.dp)

        // Concentric acoustic soundwave rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            for (i in 1..4) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f * i),
                    radius = (maxR / 4f) * i,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Center Branded Moon & Waveform Emblem
        Box(
            modifier = Modifier
                .size(logoSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private val GradientPalettes = listOf(
    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)), // Slate Night
    listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA)), // Deep Indigo
    listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF15803D)), // Emerald Green
    listOf(Color(0xFF581C87), Color(0xFF6B21A8), Color(0xFF7E22CE)), // Cyber Violet
    listOf(Color(0xFF701A75), Color(0xFF86198F), Color(0xFFA21CAF)), // Fuchsia Glow
    listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF2563EB)), // Cobalt Blue
    listOf(Color(0xFF7C2D12), Color(0xFF9A3412), Color(0xFFC2410C))  // Rust Copper
)
