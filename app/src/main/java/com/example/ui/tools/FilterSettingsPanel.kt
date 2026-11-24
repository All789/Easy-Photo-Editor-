package com.example.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterSettings
import com.example.model.Layer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FilterSettingsPanel(
    selectedLayer: Layer?,
    onUpdateFilters: (FilterSettings) -> Unit,
    onApplyPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedLayer == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select an image layer to adjust filters", color = TextSecondary, fontSize = 13.sp)
        }
        return
    }

    val f = selectedLayer.filters
    val presets = listOf("Normal", "Vibrant", "B&W", "Vintage", "Dramatic", "Warm", "Cool")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Style Presets", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { onUpdateFilters(FilterSettings()) }) {
                Text("Reset All", fontSize = 11.sp, color = TextSecondary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val key = when (preset) {
                    "B&W" -> "bw"
                    else -> preset.lowercase()
                }
                FilterChip(
                    selected = false,
                    onClick = { onApplyPreset(key) },
                    label = { Text(preset, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("filter_preset_$key")
                )
            }
        }

        FilterSliderRow(
            label = "Brightness",
            value = f.brightness,
            range = -0.5f..0.5f,
            displayValue = "${(f.brightness * 100f).roundToInt()}",
            onValueChange = { onUpdateFilters(f.copy(brightness = it)) }
        )
        FilterSliderRow(
            label = "Contrast",
            value = f.contrast,
            range = 0.5f..2.0f,
            displayValue = String.format(Locale.US, "%.2fx", f.contrast),
            onValueChange = { onUpdateFilters(f.copy(contrast = it)) }
        )
        FilterSliderRow(
            label = "Saturation",
            value = f.saturation,
            range = 0f..2.5f,
            displayValue = String.format(Locale.US, "%.2fx", f.saturation),
            onValueChange = { onUpdateFilters(f.copy(saturation = it)) }
        )
        FilterSliderRow(
            label = "Temperature",
            value = f.temperature,
            range = -100f..100f,
            displayValue = "${f.temperature.roundToInt()}",
            onValueChange = { onUpdateFilters(f.copy(temperature = it)) }
        )
        FilterSliderRow(
            label = "Hue Shift",
            value = f.hueShift,
            range = -180f..180f,
            displayValue = "${f.hueShift.roundToInt()}°",
            onValueChange = { onUpdateFilters(f.copy(hueShift = it)) }
        )
        FilterSliderRow(
            label = "Blur",
            value = f.blurRadius,
            range = 0f..20f,
            displayValue = "${f.blurRadius.roundToInt()} px",
            onValueChange = { onUpdateFilters(f.copy(blurRadius = it)) }
        )
        FilterSliderRow(
            label = "Sharpen",
            value = f.sharpen,
            range = 0f..1.5f,
            displayValue = String.format(Locale.US, "%.1fx", f.sharpen),
            onValueChange = { onUpdateFilters(f.copy(sharpen = it)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grayscale", color = TextPrimary, fontSize = 12.sp)
                Switch(
                    checked = f.grayscale,
                    onCheckedChange = { onUpdateFilters(f.copy(grayscale = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryTeal)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sepia", color = TextPrimary, fontSize = 12.sp)
                Switch(
                    checked = f.sepia,
                    onCheckedChange = { onUpdateFilters(f.copy(sepia = it)) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryTeal)
                )
            }
        }
    }
}

@Composable
fun FilterSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextPrimary, fontSize = 12.sp)
            Text(displayValue, color = SecondaryTeal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
        )
    }
}
