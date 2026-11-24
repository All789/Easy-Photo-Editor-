package com.example.ui.setup

import kotlin.math.roundToInt

enum class SizeUnit(val label: String, val shortLabel: String) {
    PIXELS("Pixels (px)", "px"),
    CENTIMETERS("Centimeters (cm)", "cm"),
    MILLIMETERS("Millimeters (mm)", "mm"),
    INCHES("Inches (in)", "in")
}

data class PagePreset(
    val id: String,
    val title: String,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int = 300,
    val subtitle: String
)

data class ColorOption(
    val label: String,
    val colorLong: Long,
    val isTransparent: Boolean = false
)

object PageSetupDefaults {
    val presets = listOf(
        PagePreset("custom_512", "512 × 512", 512, 512, 300, "512×512 px @ 300 DPI"),
        PagePreset("square_1080", "Square (IG)", 1080, 1080, 72, "1080×1080 px"),
        PagePreset("passport_bd", "Passport (4×5 cm)", 472, 591, 300, "40×50 mm @ 300 DPI"),
        PagePreset("story_hd", "Story / Reel", 1080, 1920, 72, "1080×1920 px"),
        PagePreset("youtube_thumb", "YouTube 16:9", 1920, 1080, 72, "1920×1080 px"),
        PagePreset("a4_print", "A4 Print", 2480, 3508, 300, "21×29.7 cm @ 300 DPI")
    )

    val colors = listOf(
        ColorOption("White", 0xFFFFFFFFL, false),
        ColorOption("Black", 0xFF000000L, false),
        ColorOption("Transparent", 0L, true),
        ColorOption("Blue", 0xFF1E40AFL, false),
        ColorOption("Light Gray", 0xFFE5E7EBL, false),
        ColorOption("Cream", 0xFFFEF3C7L, false)
    )

    fun unitValueToPixels(value: Float, unit: SizeUnit, dpi: Int): Int {
        val safeDpi = dpi.coerceAtLeast(72)
        val px = when (unit) {
            SizeUnit.PIXELS -> value
            SizeUnit.INCHES -> value * safeDpi
            SizeUnit.CENTIMETERS -> (value / 2.54f) * safeDpi
            SizeUnit.MILLIMETERS -> (value / 25.4f) * safeDpi
        }
        return px.roundToInt().coerceIn(16, 8000)
    }

    fun pixelsToUnitValue(px: Int, unit: SizeUnit, dpi: Int): Float {
        val safeDpi = dpi.coerceAtLeast(72)
        return when (unit) {
            SizeUnit.PIXELS -> px.toFloat()
            SizeUnit.INCHES -> px.toFloat() / safeDpi
            SizeUnit.CENTIMETERS -> (px.toFloat() / safeDpi) * 2.54f
            SizeUnit.MILLIMETERS -> (px.toFloat() / safeDpi) * 25.4f
        }
    }
}
