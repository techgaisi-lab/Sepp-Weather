package com.techgaisi.seppweather.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.techgaisi.seppweather.R
import com.techgaisi.seppweather.model.DayPartType
import com.techgaisi.seppweather.model.WeatherData
import com.techgaisi.seppweather.model.WeatherKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

internal fun Double.rounded(): Int = roundToInt()

@StringRes
internal fun WeatherKind.labelRes(isDay: Boolean): Int = when (this) {
    WeatherKind.CLEAR -> if (isDay) R.string.sepp_weather_clear_day else R.string.sepp_weather_clear_night
    WeatherKind.PARTLY_CLOUDY -> R.string.sepp_weather_partly_cloudy
    WeatherKind.CLOUDY -> R.string.sepp_weather_cloudy
    WeatherKind.FOG -> R.string.sepp_weather_fog
    WeatherKind.DRIZZLE -> R.string.sepp_weather_drizzle
    WeatherKind.RAIN -> R.string.sepp_weather_rain
    WeatherKind.SHOWERS -> R.string.sepp_weather_showers
    WeatherKind.FREEZING_RAIN -> R.string.sepp_weather_freezing_rain
    WeatherKind.SNOW -> R.string.sepp_weather_snow
    WeatherKind.THUNDERSTORM -> R.string.sepp_weather_thunderstorm
}

@StringRes
internal fun windRes(kmh: Double): Int = when {
    kmh < 8 -> R.string.sepp_weather_wind_calm
    kmh < 30 -> R.string.sepp_weather_wind_light
    kmh < 62 -> R.string.sepp_weather_wind_strong
    else -> R.string.sepp_weather_wind_storm
}

@StringRes
internal fun DayPartType.labelRes(): Int = when (this) {
    DayPartType.MORNING -> R.string.sepp_weather_part_morning
    DayPartType.NOON -> R.string.sepp_weather_part_noon
    DayPartType.EVENING -> R.string.sepp_weather_part_evening
    DayPartType.NIGHT -> R.string.sepp_weather_part_night
}

/**
 * Wählt höchstens [max] einfache Ratschläge aus – das Wichtigste zuerst.
 */
internal fun adviceFor(data: WeatherData, max: Int = 3): List<Int> {
    val today = data.today
    val cur = data.current
    val kinds = buildSet {
        add(today.kind)
        add(cur.kind)
        data.dayParts.forEach { add(it.kind) }
    }
    val result = mutableListOf<Int>()

    if (WeatherKind.THUNDERSTORM in kinds) result += R.string.sepp_weather_advice_storm
    if (WeatherKind.FREEZING_RAIN in kinds ||
        (today.minTemperature <= 1.0 && kinds.any { it.isWet })
    ) {
        result += R.string.sepp_weather_advice_ice
    }
    when {
        WeatherKind.SNOW in kinds -> result += R.string.sepp_weather_advice_snow
        kinds.any { it.isWet } || today.rainChance >= 50 -> result += R.string.sepp_weather_advice_rain
        today.rainChance < 30 -> result += R.string.sepp_weather_advice_dry
    }
    if (WeatherKind.FOG in kinds && cur.kind == WeatherKind.FOG) result += R.string.sepp_weather_advice_fog

    when {
        today.maxTemperature >= 28 -> result += R.string.sepp_weather_advice_hot
        cur.feelsLike <= 4 -> result += R.string.sepp_weather_advice_cold
        cur.feelsLike <= 13 -> result += R.string.sepp_weather_advice_cool
    }
    if (cur.windSpeedKmh >= 40) result += R.string.sepp_weather_advice_wind

    if (result.size == 1 && result[0] == R.string.sepp_weather_advice_dry &&
        today.maxTemperature in 15.0..27.9 && cur.isDay
    ) {
        result += R.string.sepp_weather_advice_nice
    }
    return result.distinct().take(max)
}

@Composable
internal fun dayNameFor(date: LocalDate, today: LocalDate): String {
    if (date == today.plusDays(1)) return stringResource(R.string.sepp_weather_tomorrow)
    val locale = Locale.getDefault()
    return remember(date, locale) {
        DateTimeFormatter.ofPattern("EEEE", locale).format(date)
            .replaceFirstChar { it.titlecase(locale) }
    }
}

@Composable
internal fun todayHeadline(today: LocalDate): String {
    val pattern = stringResource(R.string.sepp_weather_date_pattern)
    val locale = Locale.getDefault()
    return remember(today, pattern, locale) {
        DateTimeFormatter.ofPattern(pattern, locale).format(today)
            .replaceFirstChar { it.titlecase(locale) }
    }
}

internal fun clockTime(millis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
