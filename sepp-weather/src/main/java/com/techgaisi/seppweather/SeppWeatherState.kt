package com.techgaisi.seppweather

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.techgaisi.seppweather.data.DeviceLocation
import com.techgaisi.seppweather.data.WeatherRepository
import com.techgaisi.seppweather.model.SeppLocation
import com.techgaisi.seppweather.model.WeatherData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Was gerade angezeigt wird. */
sealed interface SeppWeatherUiState {
    data object Loading : SeppWeatherUiState

    /** [blocked] = Freigabe dauerhaft abgelehnt → nur noch über die Einstellungen möglich */
    data class NeedsPermission(val blocked: Boolean) : SeppWeatherUiState

    /** [offline] = Aktualisieren hat nicht geklappt, es wird das zuletzt geladene Wetter gezeigt */
    data class Ready(
        val data: WeatherData,
        val refreshing: Boolean = false,
        val offline: Boolean = false,
    ) : SeppWeatherUiState

    data class Failed(val reason: FailReason) : SeppWeatherUiState
}

enum class FailReason { NO_INTERNET, NO_LOCATION }

/**
 * Steuert Laden, Standort und Zwischenspeicher. Wird normalerweise mit
 * [rememberSeppWeatherState] erzeugt. Mit [refresh] kann die App selbst neu laden.
 */
@Stable
class SeppWeatherState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val config: SeppWeatherConfig,
) {
    var uiState: SeppWeatherUiState by mutableStateOf(SeppWeatherUiState.Loading)
        private set

    internal var autoAskedForPermission = false
    private var permissionDenied = false
    private var job: Job? = null

    private val days get() = config.forecastDays.coerceIn(0, 7)

    private fun needsPermission(): Boolean {
        if (DeviceLocation.hasPermission(context)) return false
        if (config.fixedLocation == null) return true
        // Fester Ort nur als Ersatz: zuerst einmal höflich nach dem echten Standort fragen
        return config.fixedLocationOnlyAsFallback &&
            config.askForPermissionAutomatically &&
            !permissionDenied &&
            !autoAskedForPermission
    }

    /** Startet die Anzeige: zuerst sofort das gespeicherte Wetter, dann frische Daten. */
    internal fun start() {
        if (needsPermission()) {
            uiState = SeppWeatherUiState.NeedsPermission(blocked = false)
        } else {
            refresh(force = false)
        }
    }

    /** Wird aufgerufen, wenn die App wieder sichtbar wird. */
    internal fun onResume() {
        when (val s = uiState) {
            is SeppWeatherUiState.NeedsPermission ->
                if (DeviceLocation.hasPermission(context)) refresh(force = true)
            is SeppWeatherUiState.Ready ->
                if (!s.refreshing && !WeatherRepository.isFresh(s.data, config.maxAgeMs)) refresh(force = false)
            else -> Unit
        }
    }

    internal fun onPermissionResult(granted: Boolean, blocked: Boolean) {
        if (granted) {
            permissionDenied = false
            refresh(force = true)
        } else {
            permissionDenied = true
            if (config.fixedLocation != null) {
                refresh(force = true)
            } else {
                uiState = SeppWeatherUiState.NeedsPermission(blocked = blocked)
            }
        }
    }

    /**
     * Lädt das Wetter neu. Mit [force] = false wird nur geladen,
     * wenn die gespeicherten Daten älter als das eingestellte Intervall sind.
     */
    fun refresh(force: Boolean = true) {
        job?.cancel()
        job = scope.launch {
            val cached = (uiState as? SeppWeatherUiState.Ready)?.data
                ?: WeatherRepository.cached(context, days)

            if (!force && cached != null && WeatherRepository.isFresh(cached, config.maxAgeMs)) {
                uiState = SeppWeatherUiState.Ready(cached)
                return@launch
            }
            if (cached != null) uiState = SeppWeatherUiState.Ready(cached, refreshing = true)
            else uiState = SeppWeatherUiState.Loading

            val location = resolveLocation()
            if (location == null) {
                uiState = when {
                    !DeviceLocation.hasPermission(context) -> SeppWeatherUiState.NeedsPermission(blocked = false)
                    cached != null -> SeppWeatherUiState.Ready(cached)
                    else -> SeppWeatherUiState.Failed(FailReason.NO_LOCATION)
                }
                return@launch
            }

            uiState = try {
                SeppWeatherUiState.Ready(WeatherRepository.load(context, location, days))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (cached != null) SeppWeatherUiState.Ready(cached, offline = true)
                else SeppWeatherUiState.Failed(FailReason.NO_INTERNET)
            }
        }
    }

    private suspend fun resolveLocation(): SeppLocation? {
        val fixed = config.fixedLocation
        if (fixed != null && !config.fixedLocationOnlyAsFallback) return fixed
        if (DeviceLocation.hasPermission(context)) {
            DeviceLocation.current(context)?.let { return SeppLocation(it.latitude, it.longitude) }
            return fixed ?: WeatherRepository.lastLocation()
        }
        return fixed
    }

    internal fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

/** Erzeugt den Zustand und merkt ihn sich über Neuzeichnungen hinweg. */
@Composable
fun rememberSeppWeatherState(config: SeppWeatherConfig = SeppWeatherConfig()): SeppWeatherState {
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember(config) { SeppWeatherState(appContext, scope, config) }
}

/** Liefert eine Funktion, die den System-Dialog für die Standortfreigabe öffnet. */
@Composable
internal fun rememberLocationPermissionRequest(
    state: SeppWeatherState,
    config: SeppWeatherConfig,
): () -> Unit {
    val activity = LocalContext.current.findActivity()
    val permissions = if (config.requestPreciseLocation) {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        val blocked = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        state.onPermissionResult(granted, blocked)
    }
    return remember(launcher, permissions.size) {
        {
            state.autoAskedForPermission = true
            launcher.launch(permissions)
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
