package com.techgaisi.seppweather.data

import android.content.Context
import androidx.core.content.edit
import com.techgaisi.seppweather.model.SeppLocation
import com.techgaisi.seppweather.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lädt Wetterdaten und merkt sie sich (im Speicher und auf dem Gerät).
 * Dadurch erscheint beim Öffnen sofort das letzte Wetter, während im
 * Hintergrund aktualisiert wird.
 */
internal object WeatherRepository {

    private const val PREFS = "sepp_weather_cache"
    private const val KEY_JSON = "json"
    private const val KEY_PLACE = "place"
    private const val KEY_TIME = "time"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"

    private val mutex = Mutex()

    @Volatile
    private var memory: WeatherData? = null

    @Volatile
    private var memoryLocation: SeppLocation? = null

    /** Zuletzt geladenes Wetter (falls vorhanden) – ohne Netzwerk. */
    suspend fun cached(context: Context, days: Int): WeatherData? {
        memory?.let { return it.limitDays(days) }
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_JSON, null) ?: return@withContext null
            runCatching {
                OpenMeteoClient.parse(
                    json = json,
                    placeName = prefs.getString(KEY_PLACE, null),
                    fetchedAtMillis = prefs.getLong(KEY_TIME, 0L),
                    days = 7,
                )
            }.getOrNull()?.also {
                memory = it
                memoryLocation = SeppLocation(
                    latitude = prefs.getFloat(KEY_LAT, 0f).toDouble(),
                    longitude = prefs.getFloat(KEY_LON, 0f).toDouble(),
                    name = it.placeName,
                )
            }?.limitDays(days)
        }
    }

    fun lastLocation(): SeppLocation? = memoryLocation

    fun isFresh(data: WeatherData?, maxAgeMs: Long): Boolean =
        data != null && System.currentTimeMillis() - data.fetchedAtMillis < maxAgeMs

    suspend fun load(context: Context, location: SeppLocation, days: Int): WeatherData =
        mutex.withLock {
            val placeName = location.name
                ?: memoryLocation?.takeIf { it.isNear(location) }?.name
                ?: DeviceLocation.placeName(context, location.latitude, location.longitude)

            val json = OpenMeteoClient.fetchRaw(location.latitude, location.longitude, 7)
            val now = System.currentTimeMillis()
            val data = OpenMeteoClient.parse(json, placeName, now, 7)

            memory = data
            memoryLocation = location.copy(name = placeName)
            withContext(Dispatchers.IO) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                    putString(KEY_JSON, json)
                    putString(KEY_PLACE, placeName)
                    putLong(KEY_TIME, now)
                    putFloat(KEY_LAT, location.latitude.toFloat())
                    putFloat(KEY_LON, location.longitude.toFloat())
                }
            }
            data.limitDays(days)
        }

    private fun WeatherData.limitDays(days: Int) = copy(nextDays = nextDays.take(days))

    /** Etwa 3 km Toleranz – dann ist es derselbe Ort. */
    private fun SeppLocation.isNear(other: SeppLocation): Boolean =
        kotlin.math.abs(latitude - other.latitude) < 0.03 &&
            kotlin.math.abs(longitude - other.longitude) < 0.04
}
