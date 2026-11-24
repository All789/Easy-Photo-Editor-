package com.example.viewmodel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.lifecycle.viewModelScope
import com.example.model.CanvasDimensions
import com.example.model.CutoutShape
import com.example.model.CutoutSubTool
import com.example.model.CutoutTarget
import com.example.model.DrawStroke
import com.example.model.EditorTool
import com.example.model.Layer
import com.example.model.LayerType
import com.example.processing.CutoutEngine
import com.example.processing.ImageProcessor
import com.example.processing.StickerGenerator
import com.example.processing.StickerSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

fun EditorViewModel.openWithPhotoDirectly(bitmap: Bitmap, name: String = "Edited Photo") {
    val safeBitmap = if (bitmap.config == Bitmap.Config.ARGB_8888) {
        bitmap
    } else {
        bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: bitmap
    }
    setupDirectPhotoCanvas(safeBitmap, name)
}

fun EditorViewModel.setupDirectPhotoCanvas(bitmap: Bitmap, name: String) {
    resetUndoHistory()
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    val hasTransparency = bitmap.hasAlpha()
    // Direct gallery editing MUST NOT inject an opaque white canvas behind the photo
    val directCanvas = CanvasDimensions(
        width = w,
        height = h,
        isTransparent = hasTransparency,
        backgroundColor = 0L
    )
    val photoLayer = Layer(
        id = UUID.randomUUID().toString(),
        name = name,
        type = LayerType.IMAGE,
        bitmap = bitmap,
        originalBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
        x = 0f,
        y = 0f,
        width = w.toFloat(),
        height = h.toFloat(),
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f
    )
    updateStateDirectly { state ->
        state.copy(
            projectId = "proj_${System.currentTimeMillis()}",
            projectName = name,
            canvasDimensions = directCanvas,
            canvasDpi = 72,
            layers = listOf(photoLayer),
            selectedLayerId = photoLayer.id,
            activeTool = EditorTool.SELECT,
            zoom = 1f,
            panX = 0f,
            panY = 0f,
            canUndo = false,
            canRedo = false,
            statusMessage = "Loaded photo (${w} × ${h} px)",
            hasActiveProject = true
        )
    }
}

fun EditorViewModel.createCutoutProjectFromPhoto(bitmap: Bitmap, name: String = "Cutout Studio") {
    resetUndoHistory()
    val w = bitmap.width.coerceAtLeast(1)
    val h = bitmap.height.coerceAtLeast(1)
    val canvas = CanvasDimensions(
        width = w,
        height = h,
        isTransparent = true,
        backgroundColor = 0L
    )
    val layer = Layer(
        id = UUID.randomUUID().toString(),
        name = name,
        type = LayerType.IMAGE,
        bitmap = bitmap,
        originalBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
        x = 0f,
        y = 0f,
        width = w.toFloat(),
        height = h.toFloat(),
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f
    )
    updateStateDirectly { state ->
        state.copy(
            projectId = "proj_${System.currentTimeMillis()}",
            projectName = name,
            canvasDimensions = canvas,
            canvasDpi = 72,
            layers = listOf(layer),
            selectedLayerId = layer.id,
            activeTool = EditorTool.BG_REMOVE,
            zoom = 1f,
            panX = 0f,
            panY = 0f,
            canUndo = false,
            canRedo = false,
            statusMessage = "Ready for AI background removal",
            hasActiveProject = true
        )
    }
    removeBackgroundAi(0.5f)
}

fun EditorViewModel.importPhotoToCanvas(
    bitmap: Bitmap,
    name: String = "Photo Layer",
    fillCanvas: Boolean = false
) {
    val current = uiState.value
    val canvasW = current.canvasDimensions.width.toFloat().coerceAtLeast(1f)
    val canvasH = current.canvasDimensions.height.toFloat().coerceAtLeast(1f)
    val bmpW = bitmap.width.toFloat().coerceAtLeast(1f)
    val bmpH = bitmap.height.toFloat().coerceAtLeast(1f)

    // Fit cleanly inside canvas while preserving original photo aspect ratio (no stretching)
    val scale = if (fillCanvas) {
        maxOf(canvasW / bmpW, canvasH / bmpH)
    } else {
        min(canvasW / bmpW, canvasH / bmpH).coerceAtMost(1f)
    }
    val displayW = bmpW * scale
    val displayH = bmpH * scale
    val posX = (canvasW - displayW) / 2f
    val posY = (canvasH - displayH) / 2f

    pushUndoSnapshot()
    val newLayer = Layer(
        id = UUID.randomUUID().toString(),
        name = name,
        type = LayerType.IMAGE,
        bitmap = bitmap,
        originalBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
        x = posX,
        y = posY,
        width = displayW,
        height = displayH,
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f
    )
    updateStateDirectly { state ->
        state.copy(
            layers = state.layers + newLayer,
            selectedLayerId = newLayer.id,
            activeTool = EditorTool.SELECT,
            statusMessage = "Imported $name (${bitmap.width}×${bitmap.height} px)",
            hasActiveProject = true
        )
    }
}

fun EditorViewModel.resizeLayerToMatchCanvas(
    layerId: String,
    stretchToFill: Boolean = false,
    coverCanvas: Boolean = false
) {
    val current = uiState.value
    val layer = current.layers.find { it.id == layerId } ?: return
    val canvasW = current.canvasDimensions.width.toFloat().coerceAtLeast(1f)
    val canvasH = current.canvasDimensions.height.toFloat().coerceAtLeast(1f)

    val srcW = (layer.bitmap?.width?.toFloat() ?: layer.width).coerceAtLeast(1f)
    val srcH = (layer.bitmap?.height?.toFloat() ?: layer.height).coerceAtLeast(1f)

    val (targetW, targetH) = when {
        stretchToFill -> canvasW to canvasH
        coverCanvas -> {
            val scale = maxOf(canvasW / srcW, canvasH / srcH)
            (srcW * scale) to (srcH * scale)
        }
        else -> {
            val scale = minOf(canvasW / srcW, canvasH / srcH)
            (srcW * scale) to (srcH * scale)
        }
    }
    val targetX = (canvasW - targetW) / 2f
    val targetY = (canvasH - targetH) / 2f

    pushUndoSnapshot()
    updateLayerTransform(
        layerId = layerId,
        x = targetX,
        y = targetY,
        width = targetW,
        height = targetH,
        rotation = layer.rotation
    )
    val label = when {
        stretchToFill -> "Matched canvas (${canvasW.toInt()}×${canvasH.toInt()} px)"
        coverCanvas -> "Filled canvas proportionally"
        else -> "Fitted inside canvas (${targetW.toInt()}×${targetH.toInt()} px)"
    }
    updateStateDirectly { it.copy(statusMessage = label) }
}

fun EditorViewModel.cropCanvasToLayer(layerId: String) {
    val current = uiState.value
    val layer = current.layers.find { it.id == layerId } ?: return
    val newW = layer.width.toInt().coerceAtLeast(1)
    val newH = layer.height.toInt().coerceAtLeast(1)
    val offsetX = layer.x
    val offsetY = layer.y
    pushUndoSnapshot()
    updateStateDirectly { state ->
        state.copy(
            canvasDimensions = state.canvasDimensions.copy(width = newW, height = newH),
            layers = state.layers.map { l ->
                l.copy(x = l.x - offsetX, y = l.y - offsetY)
            },
            statusMessage = "Canvas matched to ${newW}×${newH} px"
        )
    }
}

fun EditorViewModel.updateLayerExactDimensions(
    layerId: String,
    targetWidthPx: Float,
    targetHeightPx: Float,
    anchorCenter: Boolean = true
) {
    val current = uiState.value
    val layer = current.layers.find { it.id == layerId } ?: return
    val safeW = targetWidthPx.coerceAtLeast(16f)
    val safeH = targetHeightPx.coerceAtLeast(16f)
    val newX = if (anchorCenter) layer.x + (layer.width - safeW) / 2f else layer.x
    val newY = if (anchorCenter) layer.y + (layer.height - safeH) / 2f else layer.y
    updateLayerTransform(
        layerId = layerId,
        x = newX,
        y = newY,
        width = safeW,
        height = safeH,
        rotation = layer.rotation
    )
}

fun EditorViewModel.addPhotoBottomBannerWithName(text: String) {
    val current = uiState.value
    val imageLayer = current.layers.lastOrNull { it.type == LayerType.IMAGE } ?: return
    val srcBitmap = imageLayer.bitmap ?: return
    val bannerHeight = (srcBitmap.height * 0.16f).toInt().coerceAtLeast(60)
    val newW = srcBitmap.width
    val newH = srcBitmap.height + bannerHeight

    val combined = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(combined)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
    val bgPaint = Paint().apply { color = android.graphics.Color.WHITE }
    canvas.drawRect(0f, srcBitmap.height.toFloat(), newW.toFloat(), newH.toFloat(), bgPaint)

    pushUndoSnapshot()
    updateStateDirectly { state ->
        val updatedLayers = state.layers.map {
            if (it.id == imageLayer.id) {
                it.copy(
                    bitmap = combined,
                    height = it.height + (it.height * 0.16f)
                )
            } else it
        }
        state.copy(
            layers = updatedLayers,
            canvasDimensions = state.canvasDimensions.copy(
                height = if (state.layers.size == 1) newH else state.canvasDimensions.height
            )
        )
    }
    if (text.isNotBlank()) {
        val textY = imageLayer.y + imageLayer.height
        addTextLayer(
            text = text,
            color = 0xFF111827L,
            fontSize = (bannerHeight * 0.45f).coerceIn(22f, 72f),
            fontFamily = "Default"
        )
        val added = uiState.value.selectedLayer
        if (added != null) {
            updateLayerTransform(
                added.id,
                x = imageLayer.x + (imageLayer.width - added.width) / 2f,
                y = textY,
                width = added.width,
                height = (imageLayer.height * 0.16f).coerceAtLeast(48f),
                rotation = 0f
            )
        }
    }
}

fun EditorViewModel.updateDrawSettings(
    color: Long? = null,
    strokeWidth: Float? = null,
    alpha: Float? = null
) {
    updateStateDirectly { state ->
        state.copy(
            drawColor = color ?: state.drawColor,
            drawStrokeWidth = strokeWidth?.coerceIn(2f, 64f) ?: state.drawStrokeWidth,
            drawAlpha = alpha?.coerceIn(0.15f, 1f) ?: state.drawAlpha
        )
    }
}

fun EditorViewModel.addTickMarkStampLayer(
    symbol: String = "✓",
    color: Long = 0xFF22C55EL,
    fontSize: Float = 110f
) {
    val current = uiState.value
    val canvasW = current.canvasDimensions.width.toFloat().coerceAtLeast(100f)
    val canvasH = current.canvasDimensions.height.toFloat().coerceAtLeast(100f)
    val boxSize = (min(canvasW, canvasH) * 0.24f).coerceIn(96f, 360f)
    val scaledFont = (boxSize * 0.65f).coerceIn(36f, fontSize.coerceAtLeast(140f))

    pushUndoSnapshot()
    val tickLayer = Layer(
        id = UUID.randomUUID().toString(),
        name = "Tick $symbol",
        type = LayerType.TEXT,
        x = (canvasW - boxSize) / 2f,
        y = (canvasH - boxSize) / 2f,
        width = boxSize,
        height = boxSize,
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f,
        text = symbol,
        textColor = color,
        fontSize = scaledFont,
        fontFamily = "Default",
        isBold = true,
        hasShadow = true,
        shadowColor = 0x66000000L,
        shadowRadius = 4f,
        hasBackground = false
    )
    updateStateDirectly { state ->
        state.copy(
            layers = state.layers + tickLayer,
            selectedLayerId = tickLayer.id,
            activeTool = EditorTool.SELECT,
            statusMessage = "Added tick mark $symbol",
            hasActiveProject = true
        )
    }
}

fun EditorViewModel.commitDrawStrokesToLayer(strokes: List<DrawStroke>) {
    if (strokes.isEmpty()) {
        updateStateDirectly { it.copy(activeTool = EditorTool.SELECT) }
        return
    }
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    var maxPad = 12f
    for (s in strokes) {
        if (s.strokeWidth > maxPad) maxPad = s.strokeWidth
        for ((px, py) in s.points) {
            if (px < minX) minX = px
            if (py < minY) minY = py
            if (px > maxX) maxX = px
            if (py > maxY) maxY = py
        }
    }
    if (minX == Float.MAX_VALUE) {
        updateStateDirectly { it.copy(activeTool = EditorTool.SELECT) }
        return
    }

    val pad = (maxPad * 1.25f).coerceAtLeast(16f)
    val originX = minX - pad
    val originY = minY - pad
    val bmpW = ((maxX - minX) + pad * 2f).roundToInt().coerceIn(24, 4096)
    val bmpH = ((maxY - minY) + pad * 2f).roundToInt().coerceIn(24, 4096)

    val outBmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outBmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    for (stroke in strokes) {
        if (stroke.points.isEmpty()) continue
        paint.color = stroke.color.toInt()
        paint.alpha = (stroke.alpha * 255f).roundToInt().coerceIn(0, 255)
        paint.strokeWidth = stroke.strokeWidth
        if (stroke.points.size == 1) {
            val (px, py) = stroke.points[0]
            val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(px - originX, py - originY, stroke.strokeWidth / 2f, dotPaint)
        } else {
            val path = Path()
            val (startX, startY) = stroke.points[0]
            path.moveTo(startX - originX, startY - originY)
            for (i in 1 until stroke.points.size) {
                val (px, py) = stroke.points[i]
                path.lineTo(px - originX, py - originY)
            }
            canvas.drawPath(path, paint)
        }
    }

    pushUndoSnapshot()
    val drawLayer = Layer(
        id = UUID.randomUUID().toString(),
        name = "Writing Layer",
        type = LayerType.IMAGE,
        bitmap = outBmp,
        originalBitmap = outBmp.copy(Bitmap.Config.ARGB_8888, true),
        x = originX,
        y = originY,
        width = bmpW.toFloat(),
        height = bmpH.toFloat(),
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f
    )
    updateStateDirectly { state ->
        state.copy(
            layers = state.layers + drawLayer,
            selectedLayerId = drawLayer.id,
            activeTool = EditorTool.SELECT,
            statusMessage = "Writing applied ✓",
            hasActiveProject = true
        )
    }
}

fun EditorViewModel.addStickerLayer(spec: StickerSpec) {
    val current = uiState.value
    val canvasW = current.canvasDimensions.width.toFloat().coerceAtLeast(100f)
    val canvasH = current.canvasDimensions.height.toFloat().coerceAtLeast(100f)
    val stickerSize = (min(canvasW, canvasH) * 0.30f).coerceIn(96f, 420f)
    val bmp = StickerGenerator.createStickerBitmap(spec.id, 512)

    pushUndoSnapshot()
    val stickerLayer = Layer(
        id = UUID.randomUUID().toString(),
        name = "Sticker: ${spec.label}",
        type = LayerType.IMAGE,
        bitmap = bmp,
        originalBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true),
        x = (canvasW - stickerSize) / 2f,
        y = (canvasH - stickerSize) / 2f,
        width = stickerSize,
        height = stickerSize,
        rotation = 0f,
        scaleX = 1f,
        scaleY = 1f,
        opacity = 1f
    )
    updateStateDirectly { state ->
        state.copy(
            layers = state.layers + stickerLayer,
            selectedLayerId = stickerLayer.id,
            statusMessage = "Added sticker: ${spec.label}",
            hasActiveProject = true
        )
    }
}

fun EditorViewModel.updateCutoutSettings(
    subTool: CutoutSubTool? = null,
    brushSize: Float? = null,
    brushHardness: Float? = null,
    outlineKeepInside: Boolean? = null,
    shapeInvert: Boolean? = null
) {
    updateStateDirectly { state ->
        state.copy(
            cutoutSubTool = subTool ?: state.cutoutSubTool,
            cutoutBrushSize = brushSize?.coerceIn(8f, 140f) ?: state.cutoutBrushSize,
            cutoutBrushHardness = brushHardness?.coerceIn(0f, 1f) ?: state.cutoutBrushHardness,
            cutoutOutlineKeepInside = outlineKeepInside ?: state.cutoutOutlineKeepInside,
            cutoutShapeInvert = shapeInvert ?: state.cutoutShapeInvert
        )
    }
}

private fun EditorViewModel.resolveTargetCutoutLayer(): Layer? {
    val state = uiState.value
    return state.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
        ?: state.layers.lastOrNull { it.type == LayerType.IMAGE }
}

fun EditorViewModel.applySemanticCutout(
    target: CutoutTarget,
    smoothing: Float = 0.5f
) {
    val targetLayer = resolveTargetCutoutLayer() ?: return
    val source = targetLayer.originalBitmap ?: targetLayer.bitmap ?: return
    viewModelScope.launch {
        updateStateDirectly {
            it.copy(
                isProcessingBgRemove = true,
                cutoutSelectedTarget = target,
                statusMessage = "Cutting out ${target.label}..."
            )
        }
        val result = CutoutEngine.extractSemanticCutout(source, target, smoothing)
        pushUndoSnapshot()
        updateStateDirectly { state ->
            state.copy(
                isProcessingBgRemove = false,
                layers = state.layers.map {
                    if (it.id == targetLayer.id) it.copy(bitmap = result) else it
                },
                statusMessage = "${target.label} cutout applied ✓"
            )
        }
    }
}

fun EditorViewModel.applyOutlineCutout(
    normalizedPolygon: List<Pair<Float, Float>>,
    keepInside: Boolean
) {
    if (normalizedPolygon.size < 3) return
    val targetLayer = resolveTargetCutoutLayer() ?: return
    val currentBmp = targetLayer.bitmap ?: return
    val origBmp = targetLayer.originalBitmap ?: currentBmp
    viewModelScope.launch {
        updateStateDirectly { it.copy(isProcessingBgRemove = true) }
        val result = withContext(Dispatchers.Default) {
            CutoutEngine.applyOutlineCutout(
                originalSource = origBmp,
                currentBitmap = currentBmp,
                normalizedPolygon = normalizedPolygon,
                keepInside = keepInside,
                featherPx = 3f
            )
        }
        pushUndoSnapshot()
        updateStateDirectly { state ->
            state.copy(
                isProcessingBgRemove = false,
                layers = state.layers.map {
                    if (it.id == targetLayer.id) it.copy(bitmap = result) else it
                },
                statusMessage = if (keepInside) "Outline cutout applied ✓" else "Outline area erased ✓"
            )
        }
    }
}

fun EditorViewModel.applyCutoutBrushOrErase(
    normalizedPoints: List<Pair<Float, Float>>,
    brushSizePx: Float,
    hardness: Float,
    isErase: Boolean
) {
    if (normalizedPoints.isEmpty()) return
    val targetLayer = resolveTargetCutoutLayer() ?: return
    val currentBmp = targetLayer.bitmap ?: return
    val origBmp = targetLayer.originalBitmap ?: currentBmp
    viewModelScope.launch {
        // Scale canvas brush size into bitmap pixels proportionally
        val scaleToBmp = currentBmp.width.toFloat() / targetLayer.width.coerceAtLeast(1f)
        val bmpBrushPx = (brushSizePx * scaleToBmp).coerceIn(6f, currentBmp.width * 0.4f)
        val result = withContext(Dispatchers.Default) {
            CutoutEngine.applyBrushOrEraseStroke(
                originalSource = origBmp,
                currentBitmap = currentBmp,
                normalizedPoints = normalizedPoints,
                brushSizePx = bmpBrushPx,
                hardness = hardness,
                isErase = isErase
            )
        }
        pushUndoSnapshot()
        updateStateDirectly { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == targetLayer.id) it.copy(bitmap = result) else it
                }
            )
        }
    }
}

fun EditorViewModel.applyShapeCutout(
    shape: CutoutShape,
    invert: Boolean
) {
    val targetLayer = resolveTargetCutoutLayer() ?: return
    val origBmp = targetLayer.originalBitmap ?: targetLayer.bitmap ?: return
    viewModelScope.launch {
        updateStateDirectly { it.copy(isProcessingBgRemove = true, cutoutSelectedShape = shape) }
        val result = withContext(Dispatchers.Default) {
            CutoutEngine.applyShapeCutout(origBmp, shape, invert)
        }
        pushUndoSnapshot()
        updateStateDirectly { state ->
            state.copy(
                isProcessingBgRemove = false,
                layers = state.layers.map {
                    if (it.id == targetLayer.id) it.copy(bitmap = result) else it
                },
                statusMessage = "${shape.label} cutout applied ✓"
            )
        }
    }
}
