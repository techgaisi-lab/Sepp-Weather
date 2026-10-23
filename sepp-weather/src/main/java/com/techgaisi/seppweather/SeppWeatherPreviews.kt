package com.techgaisi.seppweather

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techgaisi.seppweather.model.WeatherKind
import com.techgaisi.seppweather.ui.SeppWeatherContent

// Vorschauen: In Android Studio diese Datei öffnen → „Split“ oder „Design“.

@Preview(name = "Hell", widthDp = 1280, heightDp = 800)
@Composable
private fun PreviewLight() {
    SeppWeatherContent(SeppWeatherSamples.sample(), style = SeppWeatherDefaults.light(), onRefresh = {})
}

@Preview(name = "Dunkel – Regen", widthDp = 1280, heightDp = 800)
@Composable
private fun PreviewDark() {
    SeppWeatherContent(
        SeppWeatherSamples.sample(WeatherKind.RAIN, 9.0),
        style = SeppWeatherDefaults.dark(),
        onRefresh = {},
    )
}

@Preview(name = "Hoher Kontrast – Schnee", widthDp = 1280, heightDp = 800)
@Composable
private fun PreviewContrast() {
    SeppWeatherContent(
        SeppWeatherSamples.sample(WeatherKind.SNOW, -2.0),
        style = SeppWeatherDefaults.highContrast(),
        onRefresh = {},
    )
}

@Preview(name = "Kleines Tablet quer", widthDp = 960, heightDp = 600)
@Composable
private fun PreviewSmall() {
    SeppWeatherContent(SeppWeatherSamples.sample(WeatherKind.CLEAR, 22.0), style = SeppWeatherDefaults.light(), onRefresh = {})
}

@Preview(name = "Kleine Karte", widthDp = 800)
@Composable
private fun PreviewCard() {
    SeppWeatherCardContent(SeppWeatherSamples.sample(), Modifier.padding(16.dp), SeppWeatherDefaults.light())
}
