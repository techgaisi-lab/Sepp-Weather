package com.techgaisi.seppweather.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.techgaisi.seppweather.SeppWeatherStyle

/**
 * Große, gut lesbare Schriftgrößen. Die Schriftart kommt – wenn im Stil
 * nichts anderes gesetzt ist – automatisch aus dem MaterialTheme der App.
 * Die Größen sind in „sp“, d. h. die Schriftgröße-Einstellung des Tablets
 * wird zusätzlich berücksichtigt.
 */
@Immutable
internal class SeppTypography(
    val hero: TextStyle,
    val cardTemperature: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val tileTemperature: TextStyle,
    val button: TextStyle,
)

@Composable
internal fun rememberSeppTypography(style: SeppWeatherStyle): SeppTypography {
    val appFamily = MaterialTheme.typography.bodyLarge.fontFamily
    val family: FontFamily? = style.fontFamily ?: appFamily
    val scale = style.textScale.coerceIn(0.6f, 2.5f)
    return remember(family, scale) {
        fun ts(size: Int, weight: FontWeight, lineFactor: Float = 1.25f): TextStyle {
            val fs: TextUnit = (size * scale).sp
            return TextStyle(
                fontFamily = family,
                fontSize = fs,
                fontWeight = weight,
                lineHeight = (size * scale * lineFactor).sp,
            )
        }
        SeppTypography(
            hero = ts(160, FontWeight.Bold, 1.0f),
            cardTemperature = ts(110, FontWeight.Bold, 1.0f),
            title = ts(46, FontWeight.SemiBold, 1.15f),
            body = ts(32, FontWeight.Normal, 1.3f),
            bodyStrong = ts(36, FontWeight.SemiBold, 1.25f),
            label = ts(30, FontWeight.Medium, 1.2f),
            tileTemperature = ts(48, FontWeight.Bold, 1.1f),
            button = ts(34, FontWeight.SemiBold, 1.2f),
        )
    }
}
