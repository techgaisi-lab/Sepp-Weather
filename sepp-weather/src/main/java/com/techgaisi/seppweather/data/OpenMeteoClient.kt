package com.techgaisi.seppweather.data

import com.techgaisi.seppweather.model.CurrentWeather
import com.techgaisi.seppweather.model.DailyForecast
import com.techgaisi.seppweather.model.DayPart
import com.techgaisi.seppweather.model.DayPartType
import com.techgaisi.seppweather.model.WeatherData
import com.techgaisi.seppweather.model.WeatherKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale

/**
 * Holt Wetterdaten von Open-Meteo (kostenlos, ohne API-Schlüssel).
 * Bewusst ohne zusätzliche Bibliotheken: HttpURLConnection + org.json sind
 * in jedem Android enthalten – klein und schnell.
 */
internal object OpenMeteoClient {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"
    private const val TIMEOUT_MS = 10_000

    suspend fun fetchRaw(latitude: Double, longitude: Double, days: Int): String =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append(BASE_URL)
                append("?latitude=").append(String.format(Locale.US, "%.4f", latitude))
                append("&longitude=").append(String.format(Locale.US, "%.4f", longitude))
                append("&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day")
                append("&hourly=temperature_2m,weather_code,precipitation_probability,is_day")
                append("&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                append("&timezone=auto")
                append("&forecast_days=").append((days + 1).coerceIn(2, 8))
            }
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("Open-Meteo HTTP $code")
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }

    /** Wandelt die Open-Meteo-Antwort in [WeatherData] um. */
    fun parse(json: String, placeName: String?, fetchedAtMillis: Long, days: Int): WeatherData {
        val root = JSONObject(json)

        val cur = root.getJSONObject("current")
        val current = CurrentWeather(
            temperature = cur.optDouble("temperature_2m", 0.0),
            feelsLike = cur.optDouble("apparent_temperature", cur.optDouble("temperature_2m", 0.0)),
            humidity = cur.optInt("relative_humidity_2m", 0),
            windSpeedKmh = cur.optDouble("wind_speed_10m", 0.0),
            kind = WeatherKind.fromWmoCode(cur.optInt("weather_code", 3)),
            isDay = cur.optInt("is_day", 1) == 1,
        )

        val daily = root.getJSONObject("daily")
        val dTimes = daily.getJSONArray("time")
        val dCodes = daily.getJSONArray("weather_code")
        val dMax = daily.getJSONArray("temperature_2m_max")
        val dMin = daily.getJSONArray("temperature_2m_min")
        val dRain = daily.optJSONArray("precipitation_probability_max")

        val allDays = (0 until dTimes.length()).map { i ->
            DailyForecast(
                date = LocalDate.parse(dTimes.getString(i)),
                kind = WeatherKind.fromWmoCode(dCodes.optInt(i, 3)),
                maxTemperature = dMax.optDouble(i, 0.0),
                minTemperature = dMin.optDouble(i, 0.0),
                rainChance = dRain.intAt(i),
            )
        }

        val hourly = root.getJSONObject("hourly")
        val hTimes = hourly.getJSONArray("time")
        val hTemp = hourly.getJSONArray("temperature_2m")
        val hCode = hourly.getJSONArray("weather_code")
        val hRain = hourly.optJSONArray("precipitation_probability")
        val hDay = hourly.optJSONArray("is_day")

        val today = allDays.first()
        val index = HashMap<String, Int>(hTimes.length())
        for (i in 0 until hTimes.length()) index[hTimes.getString(i)] = i

        val dayParts = DayPartType.entries.mapNotNull { type ->
            val key = "${today.date}T%02d:00".format(Locale.US, type.hour)
            val i = index[key] ?: return@mapNotNull null
            DayPart(
                type = type,
                temperature = hTemp.optDouble(i, 0.0),
                kind = WeatherKind.fromWmoCode(hCode.optInt(i, 3)),
                isDay = hDay?.optInt(i, 1) != 0,
                rainChance = hRain.intAt(i),
            )
        }

        return WeatherData(
            placeName = placeName,
            current = current,
            today = today,
            dayParts = dayParts,
            nextDays = allDays.drop(1).take(days),
            fetchedAtMillis = fetchedAtMillis,
        )
    }

    private fun JSONArray?.intAt(i: Int): Int {
        if (this == null || isNull(i)) return 0
        return optDouble(i, 0.0).toInt()
    }
}
