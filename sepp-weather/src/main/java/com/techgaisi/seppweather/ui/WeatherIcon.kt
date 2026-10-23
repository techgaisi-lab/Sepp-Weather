package com.techgaisi.seppweather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.techgaisi.seppweather.SeppWeatherColors
import com.techgaisi.seppweather.model.WeatherKind
import kotlin.math.cos
import kotlin.math.sin

/**
 * Wettersymbol, direkt gezeichnet: keine Bilddateien, gestochen scharf in
 * jeder Größe und automatisch in den Farben des Stils.
 * Für Screenreader wird es ausgeblendet – der Text daneben sagt dasselbe.
 */
@Composable
fun WeatherIcon(
    kind: WeatherKind,
    isDay: Boolean,
    size: Dp,
    colors: SeppWeatherColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val s = this.size.minDimension
        when (kind) {
            WeatherKind.CLEAR ->
                if (isDay) sun(Offset(s * .5f, s * .5f), s * .46f, colors.sun)
                else moon(Offset(s * .5f, s * .5f), s * .42f, colors.moon)

            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) sun(Offset(s * .36f, s * .36f), s * .3f, colors.sun)
                else moon(Offset(s * .36f, s * .34f), s * .28f, colors.moon)
                cloud(s * .22f, s * .40f, s * .72f, colors.cloud)
            }

            WeatherKind.CLOUDY -> {
                cloud(s * .30f, s * .18f, s * .62f, colors.darkCloud)
                cloud(s * .08f, s * .36f, s * .76f, colors.cloud)
            }

            WeatherKind.FOG -> {
                cloud(s * .14f, s * .10f, s * .72f, colors.cloud)
                fogLines(s, colors.darkCloud)
            }

            WeatherKind.DRIZZLE -> {
                cloud(s * .12f, s * .08f, s * .76f, colors.cloud)
                drops(s, colors.rain, count = 3, long = false)
            }

            WeatherKind.RAIN -> {
                cloud(s * .12f, s * .08f, s * .76f, colors.darkCloud)
                drops(s, colors.rain, count = 4, long = true)
            }

            WeatherKind.SHOWERS -> {
                if (isDay) sun(Offset(s * .70f, s * .24f), s * .2f, colors.sun)
                cloud(s * .08f, s * .12f, s * .74f, colors.cloud)
                drops(s, colors.rain, count = 3, long = true)
            }

            WeatherKind.FREEZING_RAIN -> {
                cloud(s * .12f, s * .08f, s * .76f, colors.darkCloud)
                drops(s, colors.rain, count = 2, long = true, offsetIndex = 0)
                flakes(s, colors.snow, positions = listOf(.42f, .78f))
            }

            WeatherKind.SNOW -> {
                cloud(s * .12f, s * .08f, s * .76f, colors.cloud)
                flakes(s, colors.snow, positions = listOf(.28f, .52f, .76f))
            }

            WeatherKind.THUNDERSTORM -> {
                cloud(s * .12f, s * .08f, s * .76f, colors.darkCloud)
                bolt(s, colors.lightning)
            }
        }
    }
}

private fun DrawScope.sun(center: Offset, radius: Float, color: Color) {
    drawCircle(color, radius * .55f, center)
    val stroke = radius * .13f
    for (i in 0 until 8) {
        val a = Math.toRadians(i * 45.0)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        drawLine(
            color = color,
            start = center + Offset(dx, dy) * (radius * .74f),
            end = center + Offset(dx, dy) * (radius * .98f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.moon(center: Offset, radius: Float, color: Color) {
    val full = Path().apply { addOval(Rect(center, radius)) }
    val cut = Path().apply {
        addOval(Rect(center + Offset(radius * .45f, -radius * .35f), radius * .82f))
    }
    drawPath(Path.combine(PathOperation.Difference, full, cut), color)
}

/** Wolke: [left]/[top] = linke obere Ecke, [width] = Breite (Höhe ≈ 0,56 × Breite) */
private fun DrawScope.cloud(left: Float, top: Float, width: Float, color: Color) {
    val h = width * .56f
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top + h * .46f),
        size = Size(width, h * .54f),
        cornerRadius = CornerRadius(h * .27f),
    )
    drawCircle(color, width * .2f, Offset(left + width * .3f, top + h * .56f))
    drawCircle(color, width * .27f, Offset(left + width * .6f, top + h * .42f))
}

private fun DrawScope.drops(s: Float, color: Color, count: Int, long: Boolean, offsetIndex: Int = 0) {
    val xs = when (count) {
        2 -> listOf(.30f, .60f)
        3 -> listOf(.30f, .50f, .70f)
        else -> listOf(.24f, .42f, .60f, .78f)
    }
    val length = if (long) s * .2f else s * .11f
    xs.forEachIndexed { i, x ->
        val y = s * (.62f + if ((i + offsetIndex) % 2 == 0) 0f else .07f)
        drawLine(
            color = color,
            start = Offset(s * x, y),
            end = Offset(s * x - length * .35f, y + length),
            strokeWidth = s * .055f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.flakes(s: Float, color: Color, positions: List<Float>) {
    positions.forEachIndexed { i, x ->
        val c = Offset(s * x, s * (.72f + if (i % 2 == 0) 0f else .1f))
        val r = s * .075f
        for (k in 0 until 3) {
            val a = Math.toRadians(k * 60.0 + 90)
            val d = Offset(cos(a).toFloat(), sin(a).toFloat()) * r
            drawLine(color, c - d, c + d, strokeWidth = s * .035f, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.bolt(s: Float, color: Color) {
    val p = Path().apply {
        moveTo(s * .52f, s * .52f)
        lineTo(s * .38f, s * .76f)
        lineTo(s * .50f, s * .76f)
        lineTo(s * .42f, s * .96f)
        lineTo(s * .66f, s * .68f)
        lineTo(s * .54f, s * .68f)
        lineTo(s * .62f, s * .52f)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.fogLines(s: Float, color: Color) {
    listOf(.66f to .14f, .78f to .22f, .90f to .14f).forEach { (y, inset) ->
        drawLine(
            color = color,
            start = Offset(s * inset, s * y),
            end = Offset(s * (1f - inset), s * y),
            strokeWidth = s * .06f,
            cap = StrokeCap.Round,
        )
    }
}
