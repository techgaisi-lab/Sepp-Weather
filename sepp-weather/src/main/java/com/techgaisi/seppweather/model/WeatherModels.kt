package com.techgaisi.seppweather.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/** Vereinfachte Wetterlage – genug für klare, einfache Darstellung. */
enum class WeatherKind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SHOWERS,
    FREEZING_RAIN,
    SNOW,
    THUNDERSTORM;

    val isWet: Boolean
        get() = this == DRIZZLE || this == RAIN || this == SHOWERS ||
            this == FREEZING_RAIN || this == SNOW || this == THUNDERSTORM

    companion object {
        /** Übersetzt den WMO-Wettercode (wie ihn Open-Meteo liefert). */
        fun fromWmoCode(code: Int): WeatherKind = when (code) {
            0, 1 -> CLEAR
            2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            56, 57, 66, 67 -> FREEZING_RAIN
            61, 63, 65 -> RAIN
            80, 81, 82 -> SHOWERS
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}

/** Ein geografischer Ort. [name] ist optional und wird sonst automatisch ermittelt. */
@Immutable
data class SeppLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)

@Immutable
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeedKmh: Double,
    val kind: WeatherKind,
    val isDay: Boolean,
)

enum class DayPartType(val hour: Int) {
    MORNING(8),
    NOON(12),
    EVENING(18),
    NIGHT(22),
}

@Immutable
data class DayPart(
    val type: DayPartType,
    val temperature: Double,
    val kind: WeatherKind,
    val isDay: Boolean,
    val rainChance: Int,
)

@Immutable
data class DailyForecast(
    val date: LocalDate,
    val kind: WeatherKind,
    val maxTemperature: Double,
    val minTemperature: Double,
    val rainChance: Int,
)

/** Alles, was die Oberfläche anzeigt. Kann auch selbst befüllt werden (z. B. für Vorschauen). */
@Immutable
data class WeatherData(
    val placeName: String?,
    val current: CurrentWeather,
    val today: DailyForecast,
    val dayParts: List<DayPart>,
    val nextDays: List<DailyForecast>,
    val fetchedAtMillis: Long,
)
