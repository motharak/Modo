# Modo

Modo is an advanced, audiophile-grade offline music player for Android, crafted with Jetpack Compose, AndroidX Media3 (ExoPlayer), and native FFmpeg audio decoders. It merges high-fidelity lossless acoustic performance with tactile retro-modern visual aesthetics, gesture navigation, and an AMOLED battery-saver stage.

---

## Features

### 1. Audiophile Audio Engine
- **Native Decoders**: Bundles native FFmpeg libraries to deliver bit-perfect playback for lossless and high-resolution formats including ALAC, FLAC, WAV, AAC, OGG, and MP3.
- **Hardware DSP Equalizer**: 10-band parametric equalizer engine with bass boost, 3D spatial virtualizer, and auxiliary room reverb presets (Small Room, Medium Hall, Large Stage).
- **Audio Normalization**: Integrated ReplayGain volume normalization to prevent loudness jumps across varying master tracks.
- **Dual-Player Gapless & Crossfade**: Smooth DJ-style crossfade transitions and true gapless playback support.
- **Auto-Play Discovery**: Smart background track recommendation fallback based on genre and artist similarity when playlists finish.

### 2. Five Visual Player Themes
Modo features 5 real-time switchable visual themes:
- **Modern Glass**: Deep glassmorphism artwork card with dynamic ambient shadows and album art palette matching.
- **Retro Cassette**: Authentic vintage compact cassette with animated dual rotating hubs synchronized to playback progress and tape deck styling.
- **Vinyl Turntable**: High-precision spinning vinyl record with micro-grooves, dynamic tonearm tracking, and vinyl center labels.
- **Cyber HUD**: Futuristic heads-up display showcasing real-time audio technical specs (bitrate, sample rate, bit depth, channel configuration).
- **Classic Click Wheel**: Nostalgic tactile rotary wheel interface with rotary touch scrubbing, haptic feedback ticks, and dedicated physical-style navigation buttons.

### 3. Five Custom Progress Bar Scrubbers
- **Dynamic Waveform**: Acoustic waveform visualization with proportional sound wave peaks.
- **Magnetic Tape Ribbon**: Mechanical tape ribbon channel with custom tape-head knob.
- **Neon Laser Beam**: High-contrast cyberpunk laser beam with particle head glow.
- **Liquid Capsule**: Rounded glowing pill capsule with dual-tone gradient fill.
- **Segmented VU Ticks**: Studio rack VU-meter segmented tick marks with warning color bars.
- **Zero-Latency Gesture Tracking**: Single-pass gesture tracking with instant tap seeking, 1:1 smooth drag, and zero position snap-back.

### 4. High-Performance Gesture Navigation
- **Horizontal Swipe**: Smooth slide transition between consecutive songs with locked vertical alignment.
- **Fast Vertical Swipe**: Velocity-aware flick gesture to switch between entire albums across the library.
- **Quick Library Explorer**: Slow upward pull-up gesture directly triggers the full searchable albums modal sheet.
- **Strict Axis-Locking**: Prevents accidental diagonal drift so player artwork always lands perfectly centered at (0, 0).

### 5. AMOLED Battery Saver Lockscreen
- **Ultra-Low Power Mode**: Pure OLED pitch-black background with system navigation and status bars hidden.
- **Mood-Synced 2D Line-Art Cat Band**: Dedicated 1 FPS minimalist vector line-art cat musicians performing instruments (drums, guitar, piano, bongo) tailored to the genre and mood of the current track.
- **Lockscreen Gestures**: Horizontal swipe to change tracks, double-tap to unlock, single-tap for status hints.

### 6. Fast Library Scanner & Metadata Management
- **Instant Rescan**: Batch database indexing with intelligent file modification and size caching to rescan hundreds of tracks in sub-second time.
- **Metadata Tag Editor**: On-device tag editing for title, artist, album, genre, year, and track numbering.
- **Smart Playlists**: Automated dynamic playlists including Favorites, Most Played, Recently Added, Recently Played, and Lossless / Hi-Res audio collections.
- **Synced Lyrics**: LRC format synchronized scrolling lyrics with interactive click-to-seek support.

---

## Architecture & Tech Stack

- **UI Layer**: 100% Jetpack Compose with Material 3 design tokens, customizable themes, and edge-to-edge support.
- **Audio Pipeline**: AndroidX Media3 Session & ExoPlayer with custom RenderersFactory binding to native FFmpeg C libraries.
- **Local Persistence**: Room Database (SQLite) with reactive Kotlin Flow queries for real-time UI updates.
- **Image Pipeline**: Coil Compose with disk and memory caching for high-resolution album artwork.
- **Concurrency**: Kotlin Coroutines with structured Dispatchers.IO, Dispatchers.Default, and Dispatchers.Main lifecycles.

---

## Project Structure

```
app/src/main/java/com/belta/audio/
├── BeltaAudioApp.kt                  # Application class & global dependency initialization
├── core/
│   ├── audio/                       # ExoPlayer service, Equalizer, Crossfade, MediaSession
│   ├── data/                        # Room database, DAO, entities, MediaStore scanner, lyrics
│   ├── debug/                       # Logging utilities and local diagnostics
│   ├── domain/model/                # Track, Album, Artist, Playlist, AudioSpecs domain models
│   └── theme/                       # Color schemes, typography, dynamic palette
└── ui/
    ├── MainActivity.kt              # Single activity host with navigation and overlays
    ├── components/                  # Mini player, artwork, custom progress bars, widgets
    └── screens/
        ├── home/                    # Tracks, Albums, Artists, Playlists, Folders tabs
        ├── player/                  # Full-screen player, themes, battery saver lockscreen
        ├── settings/                # Audio DSP preferences, crossfade, library settings
        └── smartplaylist/           # Smart playlist generator and rule filtering
```

---

## Building & Installation

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK Platform 34
- JDK 17 (recommended)

### Command Line Build

1. Clone the repository:
```bash
git clone https://github.com/motharak/Modo.git
cd Modo
```

2. Run unit tests:
```bash
./gradlew testDebugUnitTest
```

3. Build the debug APK:
```bash
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

4. Install onto connected Android device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions

- `READ_MEDIA_AUDIO`: Required on Android 13+ to query and play local music files.
- `READ_EXTERNAL_STORAGE`: Required on Android 12 and below for music library access.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for continuous background playback.
- `WAKE_LOCK`: Keeps audio pipeline active during screen-off and battery saver lockscreen modes.

---

## License

This project is licensed under the Apache License 2.0.