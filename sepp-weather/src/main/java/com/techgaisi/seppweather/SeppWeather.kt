package com.techgaisi.seppweather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.techgaisi.seppweather.model.WeatherData
import com.techgaisi.seppweather.ui.BigButton
import com.techgaisi.seppweather.ui.ErrorScreen
import com.techgaisi.seppweather.ui.LoadingScreen
import com.techgaisi.seppweather.ui.PermissionScreen
import com.techgaisi.seppweather.ui.SeppCard
import com.techgaisi.seppweather.ui.SeppWeatherContent
import com.techgaisi.seppweather.ui.WeatherIcon
import com.techgaisi.seppweather.ui.adviceFor
import com.techgaisi.seppweather.ui.labelRes
import com.techgaisi.seppweather.ui.rememberSeppTypography
import com.techgaisi.seppweather.ui.rounded

/**
 * Großer Wetter-Bildschirm – fertig zum Einbauen:
 *
 * ```
 * SeppWeather()                                   // alles automatisch
 * SeppWeather(style = SeppWeatherDefaults.dark()) // dunkles Design
 * ```
 *
 * Kümmert sich selbst um Standortfreigabe, Laden, Zwischenspeicher und Fehler.
 */
@Composable
fun SeppWeather(
    modifier: Modifier = Modifier,
    style: SeppWeatherStyle = SeppWeatherDefaults.style(),
    config: SeppWeatherConfig = SeppWeatherConfig(),
    state: SeppWeatherState = rememberSeppWeatherState(config),
) {
    val requestPermission = rememberLocationPermissionRequest(state, config)
    WeatherLifecycle(state, config, requestPermission)

    when (val ui = state.uiState) {
        SeppWeatherUiState.Loading -> LoadingScreen(style, modifier)

        is SeppWeatherUiState.NeedsPermission -> PermissionScreen(
            style = style,
            blocked = ui.blocked,
            onRequest = requestPermission,
            onOpenSettings = state::openAppSettings,
            modifier = modifier,
        )

        is SeppWeatherUiState.Failed -> ErrorScreen(
            style = style,
            reason = ui.reason,
            onRetry = { state.refresh(force = true) },
            modifier = modifier,
        )

        is SeppWeatherUiState.Ready -> SeppWeatherContent(
            data = ui.data,
            modifier = modifier,
            style = style,
            config = config,
            refreshing = ui.refreshing,
            offline = ui.offline,
            onRefresh = { state.refresh(force = true) },
        )
    }
}

/**
 * Kleine Wetterkarte, z. B. für den Startbildschirm einer App.
 * [onClick] kann z. B. den großen Wetter-Bildschirm öffnen.
 */
@Composable
fun SeppWeatherCard(
    modifier: Modifier = Modifier,
    style: SeppWeatherStyle = SeppWeatherDefaults.style(),
    config: SeppWeatherConfig = SeppWeatherConfig(),
    state: SeppWeatherState = rememberSeppWeatherState(config),
    onClick: (() -> Unit)? = null,
) {
    val requestPermission = rememberLocationPermissionRequest(state, config)
    WeatherLifecycle(state, config, requestPermission)
    val t = rememberSeppTypography(style)

    when (val ui = state.uiState) {
        is SeppWeatherUiState.Ready -> SeppWeatherCardContent(ui.data, modifier, style, onClick)

        SeppWeatherUiState.Loading -> SeppCard(style, modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = style.colors.accent,
                    strokeWidth = 5.dp,
                )
                Spacer(Modifier.width(20.dp))
                Text(stringResource(R.string.sepp_weather_loading), style = t.body, color = style.colors.text)
            }
        }

        is SeppWeatherUiState.NeedsPermission -> SeppCard(style, modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.sepp_weather_permission_title), style = t.title, color = style.colors.text)
                Text(
                    text = stringResource(
                        if (ui.blocked) R.string.sepp_weather_permission_blocked_text
                        else R.string.sepp_weather_permission_text,
                    ),
                    style = t.body,
                    color = style.colors.secondaryText,
                )
                BigButton(
                    text = stringResource(
                        if (ui.blocked) R.string.sepp_weather_permission_settings_button
                        else R.string.sepp_weather_permission_button,
                    ),
                    style = style,
                    t = t,
                    icon = if (ui.blocked) Icons.Filled.Settings else Icons.Filled.LocationOn,
                    onClick = if (ui.blocked) state::openAppSettings else requestPermission,
                )
            }
        }

        is SeppWeatherUiState.Failed -> SeppCard(style, modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(
                        if (ui.reason == FailReason.NO_INTERNET) R.string.sepp_weather_error_internet_text
                        else R.string.sepp_weather_error_location_text,
                    ),
                    style = t.body,
                    color = style.colors.text,
                )
                BigButton(
                    text = stringResource(R.string.sepp_weather_retry),
                    style = style,
                    t = t,
                    icon = Icons.Filled.Refresh,
                    onClick = { state.refresh(force = true) },
                )
            }
        }
    }
}

/** Kleine Wetterkarte mit eigenen Daten (ohne Laden/Standort). */
@Composable
fun SeppWeatherCardContent(
    data: WeatherData,
    modifier: Modifier = Modifier,
    style: SeppWeatherStyle = SeppWeatherDefaults.style(),
    onClick: (() -> Unit)? = null,
) {
    val t = rememberSeppTypography(style)
    val cur = data.current
    val label = stringResource(cur.kind.labelRes(cur.isDay))
    val place = data.placeName ?: stringResource(R.string.sepp_weather_unknown_place)
    val advice = adviceFor(data, max = 1).firstOrNull()
    val spoken = stringResource(R.string.sepp_weather_now_spoken, cur.temperature.rounded(), label)

    SeppCard(
        style = style,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = "$place. $spoken" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(cur.kind, cur.isDay, 116.dp * style.textScale, style.colors)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = place,
                    style = t.label,
                    color = style.colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.sepp_weather_degrees_short, cur.temperature.rounded()),
                        style = t.cardTemperature,
                        color = style.colors.text,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = label,
                        style = t.title,
                        color = style.colors.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (advice != null) {
            Spacer(Modifier.size(12.dp))
            Text(stringResource(advice), style = t.body, color = style.colors.text)
        }
    }
}

@Composable
private fun WeatherLifecycle(
    state: SeppWeatherState,
    config: SeppWeatherConfig,
    requestPermission: () -> Unit,
) {
    LaunchedEffect(state) { state.start() }

    LifecycleResumeEffect(state) {
        state.onResume()
        onPauseOrDispose { }
    }

    val ui = state.uiState
    LaunchedEffect(ui) {
        if (ui is SeppWeatherUiState.NeedsPermission &&
            !ui.blocked &&
            config.askForPermissionAutomatically &&
            !state.autoAskedForPermission
        ) {
            requestPermission()
        }
    }
}
