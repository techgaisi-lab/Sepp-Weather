package com.techgaisi.seppweather

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Alle Farben von Sepp-Weather. Mit `copy(...)` kann jede Farbe einzeln geändert werden.
 */
@Immutable
data class SeppWeatherColors(
    /** Hintergrund des ganzen Wetter-Bildschirms */
    val background: Color,
    /** Hintergrund der Karten */
    val surface: Color,
    /** Haupttext */
    val text: Color,
    /** Nebentext (Datum, Hinweise) – trotzdem gut lesbar */
    val secondaryText: Color,
    /** Knöpfe und Hervorhebungen */
    val accent: Color,
    /** Text auf Knöpfen */
    val onAccent: Color,
    /** Rahmen der Karten (Color.Transparent = kein Rahmen) */
    val border: Color,
    /** Hintergrund der Ratschlag-Karte */
    val adviceBackground: Color,
    // Farben der Wetter-Symbole
    val sun: Color,
    val moon: Color,
    val cloud: Color,
    val darkCloud: Color,
    val rain: Color,
    val snow: Color,
    val lightning: Color,
    // Temperatur-Farben (Höchst- und Tiefstwert)
    val warm: Color,
    val cold: Color,
)

/**
 * Das komplette Aussehen. Wird [fontFamily] auf null gelassen, übernimmt
 * Sepp-Weather automatisch die Schriftart der App (MaterialTheme.typography).
 */
@Immutable
data class SeppWeatherStyle(
    val colors: SeppWeatherColors,
    /** null = Schrift der App verwenden */
    val fontFamily: FontFamily? = null,
    /** Faktor für alle Schriftgrößen. 1.0 = Standard (bereits groß), 1.2 = 20 % größer */
    val textScale: Float = 1f,
    /** Rundung der Ecken */
    val cornerRadius: Dp = 28.dp,
    /** Innenabstand am Rand */
    val contentPadding: Dp = 24.dp,
    /** Abstand zwischen den Karten */
    val spacing: Dp = 20.dp,
    /** Dicke des Kartenrahmens */
    val borderWidth: Dp = 0.dp,
)

object SeppWeatherDefaults {

    /** Warmes, freundliches Hell-Design */
    val LightColors = SeppWeatherColors(
        background = Color(0xFFF6F1E7),
        surface = Color(0xFFFFFFFF),
        text = Color(0xFF1B1B1F),
        secondaryText = Color(0xFF4A4A52),
        accent = Color(0xFF1F5FAD),
        onAccent = Color(0xFFFFFFFF),
        border = Color(0xFFE4DDCF),
        adviceBackground = Color(0xFFFFF4D6),
        sun = Color(0xFFF5A623),
        moon = Color(0xFFF2C94C),
        cloud = Color(0xFFB7C3D0),
        darkCloud = Color(0xFF6B7785),
        rain = Color(0xFF2F80ED),
        snow = Color(0xFF7FB3E6),
        lightning = Color(0xFFF2B705),
        warm = Color(0xFFC0392B),
        cold = Color(0xFF1F5FAD),
    )

    /** Dunkles Design – angenehm am Abend */
    val DarkColors = SeppWeatherColors(
        background = Color(0xFF121418),
        surface = Color(0xFF1E2229),
        text = Color(0xFFF4F4F6),
        secondaryText = Color(0xFFC9CCD3),
        accent = Color(0xFF8AB8FF),
        onAccent = Color(0xFF0B1B33),
        border = Color(0xFF2C313A),
        adviceBackground = Color(0xFF2B2A20),
        sun = Color(0xFFFFC043),
        moon = Color(0xFFF7DC7A),
        cloud = Color(0xFFD5DCE4),
        darkCloud = Color(0xFF8A96A6),
        rain = Color(0xFF6FB1FF),
        snow = Color(0xFFDDEBFA),
        lightning = Color(0xFFFFD43B),
        warm = Color(0xFFFF8A7A),
        cold = Color(0xFF8AB8FF),
    )

    /** Maximaler Kontrast – für Menschen mit eingeschränktem Sehvermögen */
    val HighContrastColors = SeppWeatherColors(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        text = Color(0xFFFFFFFF),
        secondaryText = Color(0xFFFFFFFF),
        accent = Color(0xFFFFE600),
        onAccent = Color(0xFF000000),
        border = Color(0xFFFFFFFF),
        adviceBackground = Color(0xFF000000),
        sun = Color(0xFFFFE600),
        moon = Color(0xFFFFE600),
        cloud = Color(0xFFFFFFFF),
        darkCloud = Color(0xFFBDBDBD),
        rain = Color(0xFF00D5FF),
        snow = Color(0xFFFFFFFF),
        lightning = Color(0xFFFFE600),
        warm = Color(0xFFFF6E6E),
        cold = Color(0xFF00D5FF),
    )

    fun light(fontFamily: FontFamily? = null, textScale: Float = 1f) =
        SeppWeatherStyle(colors = LightColors, fontFamily = fontFamily, textScale = textScale, borderWidth = 1.dp)

    fun dark(fontFamily: FontFamily? = null, textScale: Float = 1f) =
        SeppWeatherStyle(colors = DarkColors, fontFamily = fontFamily, textScale = textScale, borderWidth = 1.dp)

    fun highContrast(fontFamily: FontFamily? = null, textScale: Float = 1f) =
        SeppWeatherStyle(colors = HighContrastColors, fontFamily = fontFamily, textScale = textScale, borderWidth = 3.dp)

    /** Standard: folgt automatisch dem Hell-/Dunkelmodus des Geräts. */
    @Composable
    fun style(darkTheme: Boolean = isSystemInDarkTheme()): SeppWeatherStyle =
        if (darkTheme) dark() else light()

    /**
     * Übernimmt die Farben aus dem MaterialTheme der App, damit das Wetter
     * genau zum Rest der App passt.
     */
    @Composable
    fun fromAppTheme(): SeppWeatherStyle {
        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.background.luminance() < 0.5f
        val base = if (isDark) DarkColors else LightColors
        return SeppWeatherStyle(
            colors = base.copy(
                background = scheme.background,
                surface = scheme.surface,
                text = scheme.onSurface,
                secondaryText = scheme.onSurfaceVariant,
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                border = scheme.outlineVariant,
                adviceBackground = scheme.secondaryContainer,
            ),
            borderWidth = 1.dp,
        )
    }
}
