package com.techgaisi.seppweather

import androidx.compose.runtime.Immutable
import com.techgaisi.seppweather.model.SeppLocation

/**
 * Was angezeigt wird und wie sich das Wetter verhält.
 */
@Immutable
data class SeppWeatherConfig(
    /** Wie viele Folgetage angezeigt werden (0–7) */
    val forecastDays: Int = 3,
    /** „Früh / Mittag / Abend / Nacht“ anzeigen */
    val showDayParts: Boolean = true,
    /** Ratschläge in ganzen Sätzen anzeigen („Nehmen Sie einen Schirm mit.“) */
    val showAdvice: Boolean = true,
    /** Großen „Aktualisieren“-Knopf anzeigen */
    val showRefreshButton: Boolean = true,
    /** Nach wie vielen Minuten automatisch neu geladen wird */
    val refreshIntervalMinutes: Int = 30,
    /** Beim ersten Anzeigen automatisch nach dem Standort fragen */
    val askForPermissionAutomatically: Boolean = true,
    /** Genauen Standort anfragen (für Wetter reicht ungefähr – das ist auch datenschutzfreundlicher) */
    val requestPreciseLocation: Boolean = false,
    /**
     * Fester Ort statt Gerätestandort (z. B. für Tablets ohne Standort)
     * oder als Ersatz, wenn die Freigabe verweigert wird.
     */
    val fixedLocation: SeppLocation? = null,
    /** Fester Ort wird nur genutzt, wenn kein Standort verfügbar ist */
    val fixedLocationOnlyAsFallback: Boolean = true,
) {
    internal val maxAgeMs: Long get() = refreshIntervalMinutes.coerceAtLeast(5) * 60_000L
}
