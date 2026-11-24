package com.example.model

import android.graphics.Bitmap
import java.util.UUID

enum class LayerType {
    IMAGE,
    TEXT
}

data class FilterSettings(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val grayscale: Boolean = false,
    val sepia: Boolean = false,
    val hueShift: Float = 0f,
    val temperature: Float = 0f,
    val blurRadius: Float = 0f,
    val sharpen: Float = 0f
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType = LayerType.IMAGE,
    val bitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 500f,
    val height: Float = 500f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val opacity: Float = 1f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val text: String = "",
    val textColor: Long = 0xFFFFFFFFL,
    val fontSize: Float = 48f,
    val fontFamily: String = "Default",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val textAlignment: String = "center",
    val letterSpacing: Float = 0f,
    val hasShadow: Boolean = false,
    val shadowColor: Long = 0x80000000L,
    val shadowRadius: Float = 4f,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 2f,
    val hasOutline: Boolean = false,
    val outlineColor: Long = 0xFF000000L,
    val outlineWidth: Float = 3f,
    val hasBackground: Boolean = false,
    val backgroundColor: Long = 0xAA000000L,
    val backgroundCornerRadius: Float = 12f,
    val textAlpha: Float = 1f,
    val filters: FilterSettings = FilterSettings()
)

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0.01f)

    val height: Float
        get() = (bottom - top).coerceAtLeast(0.01f)
}

enum class EditorTool {
    SELECT,
    HAND,
    CROP,
    TRANSFORM,
    FILTERS,
    BG_REMOVE,
    LAYERS,
    TEXT,
    STICKERS,
    DRAW,
    CANVAS_SETTINGS,
    EXPORT
}

data class DrawStroke(
    val points: List<Pair<Float, Float>>,
    val color: Long = 0xFF22C55EL,
    val strokeWidth: Float = 10f,
    val alpha: Float = 1f
)

data class CanvasDimensions(
    val width: Int = 1080,
    val height: Int = 1080,
    val isTransparent: Boolean = false,
    val backgroundColor: Long = 0xFFFFFFFFL
)

data class ProjectItem(
    val id: String,
    val name: String,
    val lastModified: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layerCount: Int,
    val thumbnailPath: String? = null
)

enum class CutoutSubTool(val label: String) {
    SELECT("Select"),
    OUTLINE("Outline"),
    BRUSH("Brush"),
    ERASE("Erase"),
    SHAPE("Shape")
}

enum class CutoutTarget(val label: String) {
    PERSON("Person"),
    FACE("Face"),
    CLOTHES("Clothes"),
    SKY("Sky"),
    HEAD("Head"),
    HAIR("Hair"),
    BACKGROUND("Background")
}

enum class CutoutShape(val label: String) {
    CIRCLE("Circle"),
    SQUARE("Square"),
    ROUNDED("Rounded"),
    OVAL("Oval"),
    TRIANGLE("Triangle"),
    STAR("Star"),
    HEART("Heart"),
    HEXAGON("Hexagon"),
    DIAMOND("Diamond")
}
