package com.techgaisi.seppweather

import com.techgaisi.seppweather.model.CurrentWeather
import com.techgaisi.seppweather.model.DailyForecast
import com.techgaisi.seppweather.model.DayPart
import com.techgaisi.seppweather.model.DayPartType
import com.techgaisi.seppweather.model.WeatherData
import com.techgaisi.seppweather.model.WeatherKind
import java.time.LocalDate

/** Beispieldaten für Vorschauen in Android Studio und für Tests. */
object SeppWeatherSamples {

    fun sample(
        kind: WeatherKind = WeatherKind.PARTLY_CLOUDY,
        temperature: Double = 14.0,
        isDay: Boolean = true,
        place: String = "Zwettl",
    ): WeatherData {
        val today = LocalDate.now()
        return WeatherData(
            placeName = place,
            current = CurrentWeather(
                temperature = temperature,
                feelsLike = temperature - 2,
                humidity = 64,
                windSpeedKmh = 12.0,
                kind = kind,
                isDay = isDay,
            ),
            today = DailyForecast(today, kind, temperature + 2, temperature - 8, if (kind.isWet) 80 else 10),
            dayParts = listOf(
                DayPart(DayPartType.MORNING, temperature - 6, WeatherKind.FOG, true, 0),
                DayPart(DayPartType.NOON, temperature + 1, kind, true, 10),
                DayPart(DayPartType.EVENING, temperature - 3, WeatherKind.CLOUDY, false, 20),
                DayPart(DayPartType.NIGHT, temperature - 8, WeatherKind.CLEAR, false, 0),
            ),
            nextDays = listOf(
                DailyForecast(today.plusDays(1), WeatherKind.CLEAR, 16.0, 7.0, 0),
                DailyForecast(today.plusDays(2), WeatherKind.RAIN, 11.0, 6.0, 85),
                DailyForecast(today.plusDays(3), WeatherKind.CLOUDY, 12.0, 4.0, 20),
            ),
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }
}
