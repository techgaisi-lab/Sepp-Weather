package com.techgaisi.seppweather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techgaisi.seppweather.FailReason
import com.techgaisi.seppweather.R
import com.techgaisi.seppweather.SeppWeatherStyle

/** Gemeinsames Grundgerüst: großes Symbol, Überschrift, Satz, großer Knopf. */
@Composable
internal fun MessageScreen(
    style: SeppWeatherStyle,
    icon: ImageVector?,
    title: String,
    text: String,
    buttonText: String?,
    buttonIcon: ImageVector?,
    onButton: (() -> Unit)?,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
) {
    val t = rememberSeppTypography(style)
    Box(
        modifier
            .fillMaxSize()
            .background(style.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(style.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SeppCard(
                style = style,
                modifier = Modifier.widthIn(max = 920.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    if (showProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(110.dp),
                            color = style.colors.accent,
                            strokeWidth = 9.dp,
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = style.colors.accent,
                            modifier = Modifier.size(130.dp * style.textScale),
                        )
                    }
                    Text(
                        text = title,
                        style = t.title,
                        color = style.colors.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (text.isNotEmpty()) {
                        Text(
                            text = text,
                            style = t.body,
                            color = style.colors.secondaryText,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (buttonText != null && onButton != null) {
                        BigButton(buttonText, style, t, onButton, icon = buttonIcon)
                    }
                }
            }
        }
    }
}

@Composable
internal fun LoadingScreen(style: SeppWeatherStyle, modifier: Modifier = Modifier) {
    MessageScreen(
        style = style,
        icon = null,
        title = stringResource(R.string.sepp_weather_loading),
        text = "",
        buttonText = null,
        buttonIcon = null,
        onButton = null,
        modifier = modifier,
        showProgress = true,
    )
}

@Composable
internal fun PermissionScreen(
    style: SeppWeatherStyle,
    blocked: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageScreen(
        style = style,
        icon = Icons.Filled.LocationOn,
        title = stringResource(R.string.sepp_weather_permission_title),
        text = stringResource(
            if (blocked) R.string.sepp_weather_permission_blocked_text
            else R.string.sepp_weather_permission_text,
        ),
        buttonText = stringResource(
            if (blocked) R.string.sepp_weather_permission_settings_button
            else R.string.sepp_weather_permission_button,
        ),
        buttonIcon = if (blocked) Icons.Filled.Settings else Icons.Filled.LocationOn,
        onButton = if (blocked) onOpenSettings else onRequest,
        modifier = modifier,
    )
}

@Composable
internal fun ErrorScreen(
    style: SeppWeatherStyle,
    reason: FailReason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noInternet = reason == FailReason.NO_INTERNET
    MessageScreen(
        style = style,
        icon = if (noInternet) Icons.Filled.Warning else Icons.Filled.LocationOn,
        title = stringResource(
            if (noInternet) R.string.sepp_weather_error_internet_title
            else R.string.sepp_weather_error_location_title,
        ),
        text = stringResource(
            if (noInternet) R.string.sepp_weather_error_internet_text
            else R.string.sepp_weather_error_location_text,
        ),
        buttonText = stringResource(R.string.sepp_weather_retry),
        buttonIcon = Icons.Filled.Refresh,
        onButton = onRetry,
        modifier = modifier,
    )
}
