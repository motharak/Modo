package com.belta.audio.ui.screens.player.styles

enum class PlayerVisualTheme(val displayName: String, val subtitle: String) {
    MODERN_GLASS("Modern Glass", "Frosted glassmorphism & dynamic ambient glow"),
    RETRO_CASSETTE("Retro Cassette", "Vintage tape deck with spinning spools & VU meters"),
    CYBER_HUD("Future Cyber HUD", "Holographic telemetry, spectrum ring & neon meters"),
    VINYL_TURNTABLE("Vinyl Record", "Authentic vinyl microgrooves & tonearm tracking"),
    MINIMALIST_ZEN("Minimalist Zen", "Distraction-free typography & subtle gradient")
}

enum class ProgressBarStyle(val displayName: String) {
    LIQUID_CAPSULE("Liquid Capsule Pill"),
    DYNAMIC_WAVEFORM("Audio Waveform"),
    MAGNETIC_TAPE_RIBBON("Cassette Tape Ribbon"),
    NEON_LASER("Neon Laser Beam"),
    SEGMENTED_TICKS("Segmented LED Meter")
}
