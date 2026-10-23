package com.techgaisi.seppweather

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

/**
 * Für Apps mit klassischen XML-Layouts (ohne Compose):
 *
 * ```xml
 * <com.techgaisi.seppweather.SeppWeatherView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     app:seppVariant="auto" />
 * ```
 *
 * Die Schriftart wird automatisch aus dem App-Theme gelesen
 * (`android:fontFamily` bzw. `fontFamily`), außer `app:seppFontFamily` ist gesetzt.
 */
class SeppWeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    /** Eigener Stil. null = Variante aus XML bzw. automatisch hell/dunkel. */
    var style: SeppWeatherStyle? by mutableStateOf<SeppWeatherStyle?>(null)

    var config: SeppWeatherConfig by mutableStateOf(SeppWeatherConfig())

    /** true = kleine Karte statt ganzer Bildschirm */
    var compact: Boolean by mutableStateOf(false)

    /** Wird beim Tippen auf die kleine Karte aufgerufen */
    var onCardClick: (() -> Unit)? by mutableStateOf<(() -> Unit)?>(null)

    private var variant = 0
    private var textScale = 1f
    private var accent: Color? = null
    private var fontFamily: FontFamily? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SeppWeatherView, defStyleAttr, 0)
        try {
            variant = a.getInt(R.styleable.SeppWeatherView_seppVariant, 0)
            compact = a.getBoolean(R.styleable.SeppWeatherView_seppCompact, false)
            textScale = a.getFloat(R.styleable.SeppWeatherView_seppTextScale, 1f)
            if (a.hasValue(R.styleable.SeppWeatherView_seppAccentColor)) {
                accent = Color(a.getColor(R.styleable.SeppWeatherView_seppAccentColor, 0))
            }
            val fontRes = a.getResourceId(R.styleable.SeppWeatherView_seppFontFamily, 0)
            fontFamily = if (fontRes != 0) fontFromRes(context, fontRes) else themeFont(context)
        } finally {
            a.recycle()
        }
    }

    @Composable
    override fun Content() {
        val base = style ?: when (variant) {
            1 -> SeppWeatherDefaults.light()
            2 -> SeppWeatherDefaults.dark()
            3 -> SeppWeatherDefaults.highContrast()
            else -> SeppWeatherDefaults.style()
        }.let { s ->
            s.copy(
                textScale = s.textScale * textScale,
                colors = accent?.let { s.colors.copy(accent = it) } ?: s.colors,
            )
        }
        val finalStyle = if (base.fontFamily == null && fontFamily != null) base.copy(fontFamily = fontFamily) else base

        if (compact) {
            SeppWeatherCard(style = finalStyle, config = config, onClick = onCardClick)
        } else {
            SeppWeather(style = finalStyle, config = config)
        }
    }

    private companion object {
        fun fontFromRes(context: Context, res: Int): FontFamily? =
            runCatching { ResourcesCompat.getFont(context, res) }.getOrNull()?.toFamily()

        /** Liest die Schrift aus dem Theme der App (android:fontFamily oder AppCompat fontFamily). */
        fun themeFont(context: Context): FontFamily? {
            val attrIds = buildList {
                add(android.R.attr.fontFamily)
                val appCompat = context.resources.getIdentifier("fontFamily", "attr", context.packageName)
                if (appCompat != 0) add(appCompat)
            }
            for (attr in attrIds) {
                val tv = TypedValue()
                if (!context.theme.resolveAttribute(attr, tv, true)) continue
                if (tv.resourceId != 0) {
                    fontFromRes(context, tv.resourceId)?.let { return it }
                }
                val name = tv.string?.toString()
                if (!name.isNullOrBlank()) {
                    return AndroidTypeface.create(name, AndroidTypeface.NORMAL).toFamily()
                }
            }
            return null
        }

        fun AndroidTypeface.toFamily(): FontFamily = FontFamily(ComposeTypeface(this))
    }
}
