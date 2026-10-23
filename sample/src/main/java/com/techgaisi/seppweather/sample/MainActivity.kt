package com.techgaisi.seppweather.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.techgaisi.seppweather.SeppWeather
import com.techgaisi.seppweather.SeppWeatherCard
import com.techgaisi.seppweather.SeppWeatherConfig
import com.techgaisi.seppweather.SeppWeatherDefaults
import com.techgaisi.seppweather.rememberSeppWeatherState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Demo()
                }
            }
        }
    }
}

private val variants = listOf("Automatisch", "Hell", "Dunkel", "Kontrast", "App-Farben", "Eigener Stil", "Kleine Karte")

@Composable
private fun Demo() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val config = SeppWeatherConfig(forecastDays = 3)
    // Ein gemeinsamer Zustand: Wechsel der Variante lädt nicht neu
    val state = rememberSeppWeatherState(config)

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            variants.forEachIndexed { i, name ->
                FilterChip(selected = selected == i, onClick = { selected = i }, label = { Text(name) })
            }
        }

        val style = when (selected) {
            1 -> SeppWeatherDefaults.light()
            2 -> SeppWeatherDefaults.dark()
            3 -> SeppWeatherDefaults.highContrast()
            4 -> SeppWeatherDefaults.fromAppTheme()
            5 -> customStyle()
            else -> SeppWeatherDefaults.style()
        }

        if (selected == 6) {
            SeppWeatherCard(Modifier.padding(16.dp), config = config, state = state)
        } else {
            SeppWeather(style = style, config = config, state = state)
        }
    }
}

/** Beispiel: Farben, Schrift und Größe an die eigene App anpassen. */
@Composable
private fun customStyle() = SeppWeatherDefaults.light(fontFamily = FontFamily.Serif, textScale = 1.1f).let {
    it.copy(
        cornerRadius = 12.dp,
        colors = it.colors.copy(
            background = Color(0xFFEAF4EC),
            accent = Color(0xFF2E7D32),
            adviceBackground = Color(0xFFD7EDDA),
            border = Color(0xFFB9D8BE),
        ),
    )
}
