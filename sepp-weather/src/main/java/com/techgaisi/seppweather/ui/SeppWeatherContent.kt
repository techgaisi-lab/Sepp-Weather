package com.techgaisi.seppweather.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

/**
 * Die eigentliche Wetteransicht (ohne Laden/Standort). Kann auch direkt mit
 * eigenen Daten benutzt werden – z. B. für Vorschauen oder Tests.
 *
 * Ab 720 dp Breite (Tablet quer) werden zwei Spalten nebeneinander gezeigt.
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
    val t = rememberSeppTypography(style)
    BoxWithConstraints(modifier.fillMaxSize().background(style.colors.background)) {
        val wide = maxWidth >= 720.dp
        val gap = Arrangement.spacedBy(style.spacing)
        val scroll = rememberScrollState()

        if (wide) {
            Row(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(style.contentPadding),
                horizontalArrangement = gap,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = gap) {
                    Header(data, style, t)
                    HeroCard(data, style, t)
                    if (config.showAdvice) AdviceCard(data, style, t)
                }
                Column(Modifier.weight(1f), verticalArrangement = gap) {
                    if (config.showDayParts && data.dayParts.isNotEmpty()) DayPartsSection(data.dayParts, style, t)
                    if (data.nextDays.isNotEmpty()) NextDaysSection(data, style, t)
                    Footer(data, style, t, config, refreshing, offline, onRefresh)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(style.contentPadding),
                verticalArrangement = gap,
            ) {
                Header(data, style, t)
                HeroCard(data, style, t)
                if (config.showAdvice) AdviceCard(data, style, t)
                if (config.showDayParts && data.dayParts.isNotEmpty()) DayPartsSection(data.dayParts, style, t)
                if (data.nextDays.isNotEmpty()) NextDaysSection(data, style, t)
                Footer(data, style, t, config, refreshing, offline, onRefresh)
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
private fun Header(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography) {
    val place = data.placeName ?: stringResource(R.string.sepp_weather_unknown_place)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = null,
            tint = style.colors.accent,
            modifier = Modifier.size(44.dp * style.textScale),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = place,
                style = t.title,
                color = style.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = todayHeadline(data.today.date), style = t.body, color = style.colors.secondaryText)
        }
    }
}

@Composable
private fun HeroCard(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography) {
    val cur = data.current
    val label = stringResource(cur.kind.labelRes(cur.isDay))
    val spoken = stringResource(R.string.sepp_weather_now_spoken, cur.temperature.rounded(), label)
    SeppCard(style, Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherIcon(cur.kind, cur.isDay, 150.dp * style.textScale, style.colors)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.clearAndSetSemantics { }) {
                Text(
                    text = stringResource(R.string.sepp_weather_degrees_short, cur.temperature.rounded()),
                    style = t.hero,
                    color = style.colors.text,
                    maxLines = 1,
                )
                Text(text = label, style = t.title, color = style.colors.text)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.sepp_weather_feels_like, cur.feelsLike.rounded()) +
                " · " + stringResource(windRes(cur.windSpeedKmh)),
            style = t.body,
            color = style.colors.secondaryText,
        )
    }
}

@Composable
private fun AdviceCard(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography) {
    val advice = adviceFor(data)
    SeppCard(style, background = style.colors.adviceBackground) {
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
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Spacer(
                    Modifier
                        .padding(top = 12.dp * style.textScale)
                        .size(14.dp * style.textScale)
                        .clip(CircleShape)
                        .background(style.colors.accent),
                )
                Spacer(Modifier.width(16.dp))
                Text(text = stringResource(res), style = t.body, color = style.colors.text)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, style: SeppWeatherStyle, t: SeppTypography) {
    Text(
        text = text,
        style = t.title,
        color = style.colors.text,
        modifier = Modifier
            .padding(start = 4.dp, top = 4.dp)
            .semantics { heading() },
    )
}

@Composable
private fun DayPartsSection(parts: List<DayPart>, style: SeppWeatherStyle, t: SeppTypography) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.sepp_weather_section_day), style, t)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            parts.forEach { part ->
                val name = stringResource(part.type.labelRes())
                val label = stringResource(part.kind.labelRes(part.isDay))
                val temp = part.temperature.rounded()
                SeppCard(
                    style = style,
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$name: $temp °, $label"
                        },
                    padding = 12.dp,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(name, style = t.label, color = style.colors.secondaryText, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    WeatherIcon(part.kind, part.isDay, 64.dp * style.textScale, style.colors)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.sepp_weather_degrees_short, temp),
                        style = t.tileTemperature,
                        color = style.colors.text,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun NextDaysSection(data: WeatherData, style: SeppWeatherStyle, t: SeppTypography) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.sepp_weather_section_days), style, t)
        SeppCard(style, padding = 0.dp) {
            data.nextDays.forEachIndexed { index, day ->
                if (index > 0) HorizontalDivider(color = style.colors.border, thickness = 1.dp)
                DayRow(day, data.today.date, style, t)
            }
        }
    }
}

@Composable
private fun DayRow(day: DailyForecast, today: java.time.LocalDate, style: SeppWeatherStyle, t: SeppTypography) {
    val name = dayNameFor(day.date, today)
    val label = stringResource(day.kind.labelRes(true))
    val max = day.maxTemperature.rounded()
    val min = day.minTemperature.rounded()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = style.contentPadding, vertical = 16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$name: $label, $min bis $max Grad"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = t.bodyStrong, color = style.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = t.label, color = style.colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WeatherIcon(day.kind, true, 60.dp * style.textScale, style.colors)
        Spacer(Modifier.width(16.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = style.colors.warm)) { append("$max°") }
                    withStyle(SpanStyle(color = style.colors.secondaryText)) { append(" / ") }
                    withStyle(SpanStyle(color = style.colors.cold)) { append("$min°") }
                },
                style = t.bodyStrong,
                maxLines = 1,
            )
            if (day.rainChance > 0) {
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
}

@Composable
private fun Footer(
    data: WeatherData,
    style: SeppWeatherStyle,
    t: SeppTypography,
    config: SeppWeatherConfig,
    refreshing: Boolean,
    offline: Boolean,
    onRefresh: (() -> Unit)?,
) {
    val time = clockTime(data.fetchedAtMillis)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (offline) {
            Text(
                text = stringResource(R.string.sepp_weather_offline_hint, time),
                style = t.body,
                color = style.colors.warm,
            )
        } else {
            Text(
                text = stringResource(R.string.sepp_weather_updated_at, time),
                style = t.label,
                color = style.colors.secondaryText,
                textAlign = TextAlign.Start,
            )
        }
        if (config.showRefreshButton && onRefresh != null) {
            BigButton(
                text = stringResource(R.string.sepp_weather_refresh),
                style = style,
                t = t,
                icon = Icons.Filled.Refresh,
                busy = refreshing,
                onClick = onRefresh,
            )
        }
    }
}

/** Großer, gut treffbarer Knopf (mindestens 72 dp hoch). */
@Composable
internal fun BigButton(
    text: String,
    style: SeppWeatherStyle,
    t: SeppTypography,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    busy: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier.fillMaxWidth().heightIn(min = 72.dp * style.textScale),
        shape = RoundedCornerShape(style.cornerRadius),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = style.colors.accent,
            contentColor = style.colors.onAccent,
            disabledContainerColor = style.colors.accent.copy(alpha = 0.7f),
            disabledContentColor = style.colors.onAccent,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(30.dp),
                color = style.colors.onAccent,
                strokeWidth = 4.dp,
            )
            Spacer(Modifier.width(14.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp * style.textScale))
            Spacer(Modifier.width(14.dp))
        }
        Text(text, style = t.button, textAlign = TextAlign.Center)
    }
}
