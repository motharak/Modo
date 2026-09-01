package com.belta.audio.core.domain.smartengine

import com.belta.audio.core.data.api.MusicBrainzOpenService
import com.belta.audio.core.domain.model.Track

enum class SmartVibeSection {
    FEELING_MOOD,
    DRIVE_COMMUTE,
    DECADES_CLASSIC,
    COUNTRY_WORLD
}

data class SmartVibeMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: SmartVibeSection,
    val filterPredicate: (Track) -> Boolean
)

object MoodCategoryClassifier {

    val VIBE_MIXES = listOf(
        // FEELING & MOOD
        SmartVibeMix(
            id = "chill_ambient",
            title = "Chill & Relax",
            subtitle = "Lo-Fi, Acoustic, Downtempo and Calm Melodies",
            section = SmartVibeSection.FEELING_MOOD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("chill") || g.contains("ambient") || g.contains("acoustic") ||
                        g.contains("lo-fi") || g.contains("downtempo") || g.contains("jazz") ||
                        g.contains("classical") || t.contains("chill") || t.contains("relax") ||
                        t.contains("acoustic") || t.contains("piano")
            }
        ),
        SmartVibeMix(
            id = "energy_workout",
            title = "Energy & Workout",
            subtitle = "High Tempo Beats, Electronic, Rock & Cardio Rhythm",
            section = SmartVibeSection.FEELING_MOOD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("rock") || g.contains("metal") || g.contains("dance") ||
                        g.contains("electronic") || g.contains("edm") || g.contains("techno") ||
                        g.contains("house") || g.contains("workout") || t.contains("remix") ||
                        t.contains("power") || t.contains("energy")
            }
        ),
        SmartVibeMix(
            id = "melancholy_soft",
            title = "Melancholy & Emotional",
            subtitle = "Sad Ballads, Soul, Blues & Rain Acoustics",
            section = SmartVibeSection.FEELING_MOOD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("blues") || g.contains("soul") || g.contains("ballad") ||
                        g.contains("sad") || t.contains("sad") || t.contains("tears") ||
                        t.contains("lonely") || t.contains("goodbye") || t.contains("heart")
            }
        ),
        SmartVibeMix(
            id = "happy_uplifting",
            title = "Happy & Uplifting",
            subtitle = "Feel-Good Pop, Funk, Disco & Vibrant Grooves",
            section = SmartVibeSection.FEELING_MOOD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("pop") || g.contains("funk") || g.contains("disco") ||
                        g.contains("reggae") || t.contains("happy") || t.contains("sun") ||
                        t.contains("love") || t.contains("smile") || t.contains("summer")
            }
        ),

        // DRIVE & COMMUTE
        SmartVibeMix(
            id = "late_night_drive",
            title = "Late Night Drive",
            subtitle = "Midnight Synthwave, Deep Bass & Neon Road Ambience",
            section = SmartVibeSection.DRIVE_COMMUTE,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("synth") || g.contains("wave") || g.contains("electronic") ||
                        g.contains("ambient") || t.contains("night") || t.contains("drive") ||
                        t.contains("midnight") || t.contains("neon") || t.contains("city")
            }
        ),
        SmartVibeMix(
            id = "highway_roadtrip",
            title = "Highway Road Trip",
            subtitle = "Classic Driving Anthems, Upbeat Indie & Cruising Beats",
            section = SmartVibeSection.DRIVE_COMMUTE,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("rock") || g.contains("pop") || g.contains("country") ||
                        g.contains("indie") || t.contains("road") || t.contains("highway") ||
                        t.contains("trip") || t.contains("run") || t.contains("ride")
            }
        ),

        // DECADES & VINTAGE CLASSIC OLD SONGS
        SmartVibeMix(
            id = "golden_60s_70s",
            title = "Golden 60s & 70s Oldies",
            subtitle = "Vintage Vinyl Classics, Classic Rock & Motown Soul",
            section = SmartVibeSection.DECADES_CLASSIC,
            filterPredicate = { track ->
                val y = track.year ?: 0
                val g = (track.genre ?: "").lowercase()
                (y in 1950..1979) || g.contains("oldies") || g.contains("motown") || g.contains("classic rock")
            }
        ),
        SmartVibeMix(
            id = "retro_80s",
            title = "80s Synth & Retro Wave",
            subtitle = "Synthesizers, New Wave, Glam Rock & 80s Icons",
            section = SmartVibeSection.DECADES_CLASSIC,
            filterPredicate = { track ->
                val y = track.year ?: 0
                val g = (track.genre ?: "").lowercase()
                (y in 1980..1989) || g.contains("80s") || g.contains("new wave") || g.contains("synthpop")
            }
        ),
        SmartVibeMix(
            id = "alternative_90s",
            title = "90s Grunge & Alternative",
            subtitle = "90s Golden Era, Grunge, Britpop & Eurodance",
            section = SmartVibeSection.DECADES_CLASSIC,
            filterPredicate = { track ->
                val y = track.year ?: 0
                val g = (track.genre ?: "").lowercase()
                (y in 1990..1999) || g.contains("90s") || g.contains("grunge") || g.contains("britpop")
            }
        ),
        SmartVibeMix(
            id = "nostalgia_2000s",
            title = "2000s Y2K Nostalgia",
            subtitle = "Early 2000s Pop Punk, R&B, Hip-Hop & Indie Rock",
            section = SmartVibeSection.DECADES_CLASSIC,
            filterPredicate = { track ->
                val y = track.year ?: 0
                val g = (track.genre ?: "").lowercase()
                (y in 2000..2009) || g.contains("2000s") || g.contains("nu metal") || g.contains("pop punk")
            }
        ),

        // COUNTRY & REGIONAL
        SmartVibeMix(
            id = "country_americana",
            title = "Country & Americana",
            subtitle = "Acoustic Guitars, Country Ballads & Folk Harmonies",
            section = SmartVibeSection.COUNTRY_WORLD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                g.contains("country") || g.contains("folk") || g.contains("bluegrass") ||
                        g.contains("americana") || t.contains("country")
            }
        ),
        SmartVibeMix(
            id = "asian_pop_soundtracks",
            title = "Asian J-Pop, K-Pop & OST",
            subtitle = "Anime Soundtracks, City Pop, J-Pop & Asian Melodies",
            section = SmartVibeSection.COUNTRY_WORLD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                val t = track.title.lowercase()
                val a = track.artist.lowercase()
                g.contains("j-pop") || g.contains("k-pop") || g.contains("anime") ||
                        g.contains("city pop") || g.contains("ost") || g.contains("soundtrack") ||
                        a.contains("soundtrack") || t.contains("ost") || t.contains("theme")
            }
        ),
        SmartVibeMix(
            id = "latin_world_grooves",
            title = "Latin & World Grooves",
            subtitle = "Bossa Nova, Reggaeton, Flamenco & Tropical Rhythm",
            section = SmartVibeSection.COUNTRY_WORLD,
            filterPredicate = { track ->
                val g = (track.genre ?: "").lowercase()
                g.contains("latin") || g.contains("bossa") || g.contains("salsa") ||
                        g.contains("flamenco") || g.contains("tango") || g.contains("tropical")
            }
        )
    )

    fun getTracksForMix(allTracks: List<Track>, mix: SmartVibeMix): List<Track> {
        val directMatches = allTracks.filter { mix.filterPredicate(it) }
        if (directMatches.isNotEmpty()) {
            return directMatches
        }
        // Fallback distributed sample if tags are sparse
        return when (mix.section) {
            SmartVibeSection.FEELING_MOOD -> allTracks.shuffled().take(30)
            SmartVibeSection.DRIVE_COMMUTE -> allTracks.filter { it.durationMs in 180000..360000 }.shuffled().take(30)
            SmartVibeSection.DECADES_CLASSIC -> allTracks.sortedBy { it.year ?: 2000 }.take(30)
            SmartVibeSection.COUNTRY_WORLD -> allTracks.take(30)
        }
    }
}
