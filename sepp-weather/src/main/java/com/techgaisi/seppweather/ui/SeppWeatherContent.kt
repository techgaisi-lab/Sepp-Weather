package com.techgaisi.seppweather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.techgaisi.seppweather.R
import com.techgaisi.seppweather.SeppWeatherConfig
import com.techgaisi.seppweather.SeppWeatherDefaults
import com.techgaisi.seppweather.SeppWeatherStyle
import com.techgaisi.seppweather.model.DailyForecast
import com.techgaisi.seppweather.model.DayPart
import com.techgaisi.seppweather.model.WeatherData
import com.techgaisi.seppweather.model.WeatherKind
import java.time.LocalDate
import kotlin.math.min

/** Größe, für die das Layout entworfen ist (Tablet quer, z. B. 10–11 Zoll). */
private val DESIGN_WIDTH = 1280.dp
private val DESIGN_HEIGHT = 840.dp

/**
 * Die eigentliche Wetteransicht im Querformat (ohne Laden/Standort).
 * Kann auch direkt mit eigenen Daten benutzt werden – z. B. für Vorschauen.
 *
 * Aufbau – alles auf einen Blick, ohne Scrollen:
 * ```
 * ┌ Ort · Datum ───────────────────────────── Stand · [Aktualisieren] ┐
 * │ Jetzt: Symbol + Temperatur        │ Heute: Sätze / Ratschläge      │
 * │ Früh · Mittag · Abend · Nacht     │ Morgen · Tag 2 · Tag 3         │
 * └────────────────────────────────────────────────────────────────────┘
 * ```
 * Alles wird automatisch an die Bildschirmgröße angepasst: auf großen
 * Tablets wird es noch größer, auf kleinen etwas kleiner.
 */
@Composable
fun SeppWeatherContent(
    data: WeatherData,
    modifier: Modifier = Modifier,
    style: SeppWeatherStyle = SeppWeatherDefaults.style(),
    config: SeppWeatherConfig = SeppWeatherConfig(),
    refreshing: Boolean = false,
    offline: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(style.colors.background)) {
        val fit = if (config.fitToScreen) {
            min(maxWidth / DESIGN_WIDTH, maxHeight / DESIGN_HEIGHT).coerceIn(0.55f, 1.8f)
        } else {
            1f
        }
        val s = style.copy(
            textScale = style.textScale * fit,
            contentPadding = style.contentPadding * fit,
            spacing = style.spacing * fit,
        )
        val t = rememberSeppTypography(s)
        val gap = Arrangement.spacedBy(s.spacing)

        // Scrollen nur als Notlösung (z. B. sehr große Systemschrift)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(s.contentPadding),
            verticalArrangement = gap,
        ) {
            TopBar(data, s, t, config, refreshing, offline, onRefresh)

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = gap) {
                HeroCard(data, s, t, Modifier.weight(1f).fillMaxHeight())
                if (config.showAdvice) AdviceCard(data, s, t, Modifier.weight(1f).fillMaxHeight())
            }

            val showParts = config.showDayParts && data.dayParts.isNotEmpty()
            val showDays = data.nextDays.isNotEmpty()
            if (showParts || showDays) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                    if (showParts) {
                        TileGroup(stringResource(R.string.sepp_weather_section_day), s, t, Modifier.weight(1f)) {
                            data.dayParts.forEach { DayPartTile(it, s, t) }
                        }
                    }
                    if (showDays) {
                        TileGroup(stringResource(R.string.sepp_weather_section_days), s, t, Modifier.weight(1f)) {
                            data.nextDays.forEach { DayTile(it, data.today.date, s, t) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bausteine
// ---------------------------------------------------------------------------

@Composable
internal fun SeppCard(
    style: SeppWeatherStyle,
    modifier: Modifier = Modifier,
    background: Color = style.colors.surface,
    padding: Dp = style.contentPadding,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(style.cornerRadius)
    val bordered = if (style.borderWidth > 0.dp && style.colors.border != Color.Transparent) {
        Modifier.border(style.borderWidth, style.colors.border, shape)
    } else {
        Modifier
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(bordered)
            .padding(padding),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

@Composable
private fun TopBar(
    data: WeatherData,
    style: SeppWeatherStyle,
    t: SeppTypography,
    config: SeppWeatherConfig,
    refreshing: Boolean,
    offline: Boolean,
    onRefresh: (() -> Unit)?,
) {
    val place = data.placeName ?: stringResource(R.string.sepp_weather_unknown_place)
    val time = clockTime(data.fetchedAtMillis)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = style.colors.accent,
            modifier = Modifier.size(60.dp * style.textScale),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = place,
                style = t.title,
                color = style.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = todayHeadline(data.today.date), style = t.body, color = style.colors.secondaryText, maxLines = 1)
        }
        Spacer(Modifier.width(style.spacing))
        Text(
            text = if (offline) stringResource(R.string.sepp_weather_offline_hint, time)
            else stringResource(R.string.sepp_weather_updated_at, time),
            style = t.label,
            color = if (offline) style.colors.warm else style.colors.secondaryText,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(max = 420.dp * style.textScale),
        )
        if (config.showRefreshButton && onRefresh != null) {
            Spacer(Modifier.width(style.spacing))
            BigButton(
                text = stringResource(R.string.sepp_weather_refresh),
                style = style,
                t = t,
                icon = Icons.Filled.Refresh,
                busy = refreshing,
                onClick = onRefresh,
                fillWidth = false,
            )
        }
    }
}

@Composable
private fun HeroCard(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography, modifier: Modifier) {
    val cur = data.current
    val label = stringResource(cur.kind.labelRes(cur.isDay))
    val spoken = stringResource(R.string.sepp_weather_now_spoken, cur.temperature.rounded(), label)
    SeppCard(style, modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Column(Modifier.clearAndSetSemantics { }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(cur.kind, cur.isDay, 190.dp * style.textScale, style.colors)
                Spacer(Modifier.width(24.dp * style.textScale))
                Text(
                    text = stringResource(R.string.sepp_weather_degrees_short, cur.temperature.rounded()),
                    style = t.hero,
                    color = style.colors.text,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(8.dp * style.textScale))
            Text(text = label, style = t.title, color = style.colors.text)
            Text(
                text = stringResource(R.string.sepp_weather_feels_like, cur.feelsLike.rounded()) +
                    " · " + stringResource(windRes(cur.windSpeedKmh)),
                style = t.body,
                color = style.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun AdviceCard(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography, modifier: Modifier) {
    val advice = adviceFor(data, max = 2)
    SeppCard(style, modifier, background = style.colors.adviceBackground) {
        Text(
            text = stringResource(
                R.string.sepp_weather_today_range,
                data.today.minTemperature.rounded(),
                data.today.maxTemperature.rounded(),
            ),
            style = t.bodyStrong,
            color = style.colors.text,
        )
        advice.forEach { res ->
            Spacer(Modifier.height(16.dp * style.textScale))
            Row(verticalAlignment = Alignment.Top) {
                Spacer(
                    Modifier
                        .padding(top = 13.dp * style.textScale)
                        .size(18.dp * style.textScale)
                        .clip(CircleShape)
                        .background(style.colors.accent),
                )
                Spacer(Modifier.width(18.dp * style.textScale))
                Text(text = stringResource(res), style = t.body, color = style.colors.text)
            }
        }
    }
}

@Composable
private fun TileGroup(
    title: String,
    style: SeppWeatherStyle,
    t: SeppTypography,
    modifier: Modifier,
    tiles: @Composable RowScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp * style.textScale)) {
        Text(
            text = title,
            style = t.label.copy(fontWeight = t.title.fontWeight),
            color = style.colors.text,
            modifier = Modifier
                .padding(start = 4.dp)
                .semantics { heading() },
        )
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp * style.textScale),
            content = tiles,
        )
    }
}

@Composable
private fun RowScope.Tile(
    style: SeppWeatherStyle,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SeppCard(
        style = style,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics(mergeDescendants = true) { contentDescription = description },
        padding = 12.dp * style.textScale,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.clearAndSetSemantics { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp * style.textScale),
            content = content,
        )
    }
}

@Composable
private fun RowScope.DayPartTile(part: DayPart, style: SeppWeatherStyle, t: SeppTypography) {
    val name = stringResource(part.type.labelRes())
    val label = stringResource(part.kind.labelRes(part.isDay))
    val temp = part.temperature.rounded()
    Tile(style, "$name: $temp Grad, $label") {
        Text(name, style = t.label, color = style.colors.secondaryText, maxLines = 1)
        WeatherIcon(part.kind, part.isDay, 80.dp * style.textScale, style.colors)
        Text(
            text = stringResource(R.string.sepp_weather_degrees_short, temp),
            style = t.tileTemperature,
            color = style.colors.text,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.DayTile(day: DailyForecast, today: LocalDate, style: SeppWeatherStyle, t: SeppTypography) {
    val name = dayNameFor(day.date, today)
    val label = stringResource(day.kind.labelRes(true))
    val max = day.maxTemperature.rounded()
    val min = day.minTemperature.rounded()
    Tile(style, "$name: $label, $min bis $max Grad") {
        Text(name, style = t.label, color = style.colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        WeatherIcon(day.kind, true, 80.dp * style.textScale, style.colors)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$max°", style = t.tileTemperature, color = style.colors.warm, maxLines = 1)
            Spacer(Modifier.width(10.dp * style.textScale))
            Text("$min°", style = t.body, color = style.colors.cold, maxLines = 1)
        }
        if (day.rainChance >= 30) {
            Text(
                text = stringResource(
                    if (day.kind == WeatherKind.SNOW) R.string.sepp_weather_snow_chance
                    else R.string.sepp_weather_rain_chance,
                    day.rainChance,
                ),
                style = t.label,
                color = style.colors.secondaryText,
                maxLines = 1,
            )
        }
    }
}

/** Großer, gut treffbarer Knopf (mindestens 80 dp hoch). */
@Composable
internal fun BigButton(
    text: String,
    style: SeppWeatherStyle,
    t: SeppTypography,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    busy: Boolean = false,
    fillWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 80.dp * style.textScale),
        shape = RoundedCornerShape(style.cornerRadius),
        contentPadding = PaddingValues(horizontal = 32.dp * style.textScale, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.colors.accent,
            contentColor = style.colors.onAccent,
            disabledContainerColor = style.colors.accent.copy(alpha = 0.7f),
            disabledContentColor = style.colors.onAccent,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp * style.textScale),
                color = style.colors.onAccent,
                strokeWidth = 5.dp,
            )
            Spacer(Modifier.width(16.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp * style.textScale))
            Spacer(Modifier.width(16.dp))
        }
        Text(text, style = t.button, textAlign = TextAlign.Center, maxLines = 1)
    }
}
