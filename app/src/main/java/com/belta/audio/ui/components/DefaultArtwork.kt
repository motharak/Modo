package com.belta.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

private val GradientPalettes = listOf(
    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)), // Slate Night
    listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA)), // Deep Indigo
    listOf(Color(0xFF14532D), Color(0xFF166534), Color(0xFF15803D)), // Emerald Green
    listOf(Color(0xFF581C87), Color(0xFF6B21A8), Color(0xFF7E22CE)), // Cyber Violet
    listOf(Color(0xFF701A75), Color(0xFF86198F), Color(0xFFA21CAF)), // Fuchsia Glow
    listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF2563EB)), // Cobalt Blue
    listOf(Color(0xFF7C2D12), Color(0xFF9A3412), Color(0xFFC2410C))  // Rust Copper
)

private val PrecomputedBrushes = GradientPalettes.map { colors ->
    Brush.linearGradient(
        colors = colors,
        start = Offset(0f, 0f),
        end = Offset(200f, 200f)
    )
}

@Composable
fun DefaultArtwork(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val paletteIndex = remember(title, artist) {
        (title.hashCode() + artist.hashCode()).absoluteValue % PrecomputedBrushes.size
    }
    val brush = PrecomputedBrushes[paletteIndex]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(24.dp)
        )
    }
}
