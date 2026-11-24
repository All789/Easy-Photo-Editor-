package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CanvasDimensions
import com.example.model.CutoutSubTool
import com.example.model.DrawStroke
import com.example.model.EditorTool
import com.example.model.Layer
import com.example.model.LayerType
import com.example.processing.ImageProcessor
import com.example.ui.theme.CheckerDark
import com.example.ui.theme.CheckerLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.util.FontManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EditorCanvas(
    dimensions: CanvasDimensions,
    layers: List<Layer>,
    selectedLayerId: String?,
    activeTool: EditorTool,
    zoom: Float,
    panX: Float,
    panY: Float,
    isHandMode: Boolean,
    maintainAspectRatio: Boolean = true,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayerTransform: (String, Float, Float, Float, Float, Float) -> Unit,
    onUpdateLayerRotation: (String, Float) -> Unit,
    onUpdatePan: (Float, Float, Float) -> Unit,
    onCanvasDoubleTapText: (String) -> Unit,
    drawStrokes: List<DrawStroke> = emptyList(),
    drawColor: Long = 0xFF22C55EL,
    drawStrokeWidth: Float = 10f,
    drawAlpha: Float = 1f,
    onAddDrawStroke: ((DrawStroke) -> Unit)? = null,
    cutoutSubTool: CutoutSubTool = CutoutSubTool.SELECT,
    cutoutBrushSize: Float = 42f,
    cutoutOutlineKeepInside: Boolean = true,
    onCutoutOutlineComplete: ((List<Pair<Float, Float>>) -> Unit)? = null,
    onCutoutBrushComplete: ((List<Pair<Float, Float>>, Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLayers by rememberUpdatedState(layers)
    val currentSelectedId by rememberUpdatedState(selectedLayerId)
    val currentTool by rememberUpdatedState(activeTool)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPanX by rememberUpdatedState(panX)
    val currentPanY by rememberUpdatedState(panY)
    val currentIsHandMode by rememberUpdatedState(isHandMode)
    val currentMaintainAspect by rememberUpdatedState(maintainAspectRatio)
    val currentOnSelectLayer by rememberUpdatedState(onSelectLayer)
    val currentOnTransform by rememberUpdatedState(onUpdateLayerTransform)
    val currentOnRotate by rememberUpdatedState(onUpdateLayerRotation)
    val currentOnUpdatePan by rememberUpdatedState(onUpdatePan)
    val currentOnDoubleTapText by rememberUpdatedState(onCanvasDoubleTapText)
    val currentDrawColor by rememberUpdatedState(drawColor)
    val currentDrawWidth by rememberUpdatedState(drawStrokeWidth)
    val currentDrawAlpha by rememberUpdatedState(drawAlpha)
    val currentOnAddDrawStroke by rememberUpdatedState(onAddDrawStroke)
    val currentCutoutSubTool by rememberUpdatedState(cutoutSubTool)
    val currentCutoutBrushSize by rememberUpdatedState(cutoutBrushSize)
    val currentCutoutOutlineKeepInside by rememberUpdatedState(cutoutOutlineKeepInside)
    val currentOnCutoutOutlineComplete by rememberUpdatedState(onCutoutOutlineComplete)
    val currentOnCutoutBrushComplete by rememberUpdatedState(onCutoutBrushComplete)

    var showSnapCenterX by remember { mutableStateOf(false) }
    var showSnapCenterY by remember { mutableStateOf(false) }
    var liveAngleText by remember { mutableStateOf<String?>(null) }
    var liveSizeText by remember { mutableStateOf<String?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapLayerId by remember { mutableStateOf<String?>(null) }
    var activeStrokePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var activeCutoutStrokePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    val density = LocalDensity.current
    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF161618))
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val containerW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val containerH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val canvasW = dimensions.width.toFloat().coerceAtLeast(1f)
        val canvasH = dimensions.height.toFloat().coerceAtLeast(1f)

        val margin = with(density) { 20.dp.toPx() }
        val baseFitScale = min(
            (containerW - margin * 2f).coerceAtLeast(40f) / canvasW,
            (containerH - margin * 2f).coerceAtLeast(40f) / canvasH
        )
        val effectiveScale = baseFitScale * currentZoom

        fun canvasToScreen(pt: Offset, z: Float = currentZoom, px: Float = currentPanX, py: Float = currentPanY): Offset {
            val s = baseFitScale * z
            val originX = (containerW - canvasW * s) / 2f + px
            val originY = (containerH - canvasH * s) / 2f + py
            return Offset(originX + pt.x * s, originY + pt.y * s)
        }

        fun screenToCanvas(pt: Offset, z: Float = currentZoom, px: Float = currentPanX, py: Float = currentPanY): Offset {
            val s = (baseFitScale * z).coerceAtLeast(0.001f)
            val originX = (containerW - canvasW * s) / 2f + px
            val originY = (containerH - canvasH * s) / 2f + py
            return Offset((pt.x - originX) / s, (pt.y - originY) / s)
        }

        fun rotatedOffset(dx: Float, dy: Float, angleDeg: Float): Offset {
            val rad = Math.toRadians(angleDeg.toDouble())
            val c = cos(rad).toFloat()
            val s = sin(rad).toFloat()
            return Offset(dx * c - dy * s, dx * s + dy * c)
        }

        fun unrotatedOffset(dx: Float, dy: Float, angleDeg: Float): Offset {
            return rotatedOffset(dx, dy, -angleDeg)
        }

        fun hitTestLayer(canvasPt: Offset): Layer? {
            return currentLayers.asReversed().firstOrNull { layer ->
                if (!layer.isVisible) return@firstOrNull false
                val cx = layer.x + layer.width / 2f
                val cy = layer.y + layer.height / 2f
                val local = unrotatedOffset(canvasPt.x - cx, canvasPt.y - cy, layer.rotation)
                abs(local.x) <= layer.width / 2f && abs(local.y) <= layer.height / 2f
            }
        }

        fun hitTestSelectedHandles(screenPt: Offset, selected: Layer): CanvasHandle {
            if (selected.isLocked || !selected.isVisible) return CanvasHandle.NONE
            val s = effectiveScale
            val cx = selected.x + selected.width / 2f
            val cy = selected.y + selected.height / 2f
            val centerScreen = canvasToScreen(Offset(cx, cy))
            val localScreen = unrotatedOffset(
                screenPt.x - centerScreen.x,
                screenPt.y - centerScreen.y,
                selected.rotation
            )

            val halfW = (selected.width * s) / 2f
            val halfH = (selected.height * s) / 2f
            val hitTol = with(density) { 24.dp.toPx() }
            val rotateStem = with(density) { 30.dp.toPx() }

            // Top rotation handle
            if (abs(localScreen.x) <= hitTol && abs(localScreen.y - (-halfH - rotateStem)) <= hitTol) {
                return CanvasHandle.ROTATE
            }
            // Corners first
            if (abs(localScreen.x - (-halfW)) <= hitTol && abs(localScreen.y - (-halfH)) <= hitTol) {
                return CanvasHandle.TOP_LEFT
            }
            if (abs(localScreen.x - halfW) <= hitTol && abs(localScreen.y - (-halfH)) <= hitTol) {
                return CanvasHandle.TOP_RIGHT
            }
            if (abs(localScreen.x - (-halfW)) <= hitTol && abs(localScreen.y - halfH) <= hitTol) {
                return CanvasHandle.BOTTOM_LEFT
            }
            if (abs(localScreen.x - halfW) <= hitTol && abs(localScreen.y - halfH) <= hitTol) {
                return CanvasHandle.BOTTOM_RIGHT
            }
            // Edges
            if (abs(localScreen.x - (-halfW)) <= hitTol && abs(localScreen.y) <= halfH) {
                return CanvasHandle.LEFT
            }
            if (abs(localScreen.x - halfW) <= hitTol && abs(localScreen.y) <= halfH) {
                return CanvasHandle.RIGHT
            }
            if (abs(localScreen.y - (-halfH)) <= hitTol && abs(localScreen.x) <= halfW) {
                return CanvasHandle.TOP
            }
            if (abs(localScreen.y - halfH) <= hitTol && abs(localScreen.x) <= halfW) {
                return CanvasHandle.BOTTOM
            }
            if (abs(localScreen.x) <= halfW && abs(localScreen.y) <= halfH) {
                return CanvasHandle.BODY
            }
            return CanvasHandle.NONE
        }

        fun canvasToLayerNormalized(canvasPt: Offset, layer: Layer): Pair<Float, Float> {
            val cx = layer.x + layer.width / 2f
            val cy = layer.y + layer.height / 2f
            val local = unrotatedOffset(canvasPt.x - cx, canvasPt.y - cy, layer.rotation)
            val sx = if (layer.scaleX < 0f) -1f else 1f
            val sy = if (layer.scaleY < 0f) -1f else 1f
            val u = ((local.x * sx) / layer.width.coerceAtLeast(1f)) + 0.5f
            val v = ((local.y * sy) / layer.height.coerceAtLeast(1f)) + 0.5f
            return u.coerceIn(0f, 1f) to v.coerceIn(0f, 1f)
        }

        // Single gesture handler over entire canvas workspace
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dimensions, activeTool, cutoutSubTool) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPt = down.position
                        var moved = false

                        val isCutoutInteractive = currentTool == EditorTool.BG_REMOVE &&
                            !currentIsHandMode &&
                            (currentCutoutSubTool == CutoutSubTool.OUTLINE ||
                                currentCutoutSubTool == CutoutSubTool.BRUSH ||
                                currentCutoutSubTool == CutoutSubTool.ERASE)

                        // Interactive Cutout Outline / Brush / Erase gestures
                        if (isCutoutInteractive) {
                            val targetLayer = currentLayers.find { it.id == currentSelectedId && it.type == LayerType.IMAGE }
                                ?: currentLayers.lastOrNull { it.type == LayerType.IMAGE }
                            if (targetLayer != null) {
                                down.consume()
                                val firstCanvasPt = screenToCanvas(startPt)
                                val canvasPts = mutableListOf(firstCanvasPt.x to firstCanvasPt.y)
                                val normPts = mutableListOf(canvasToLayerNormalized(firstCanvasPt, targetLayer))
                                activeCutoutStrokePoints = canvasPts.toList()
                                var wasMultiTouch = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    if (activePointers.size >= 2) {
                                        wasMultiTouch = true
                                        activeCutoutStrokePoints = emptyList()
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val nextZoom = (currentZoom * zoomChange).coerceIn(0.25f, 8f)
                                        val maxPanX = containerW * 0.85f
                                        val maxPanY = containerH * 0.85f
                                        currentOnUpdatePan(
                                            nextZoom,
                                            (currentPanX + panChange.x).coerceIn(-maxPanX, maxPanX),
                                            (currentPanY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        if (!wasMultiTouch && normPts.isNotEmpty()) {
                                            when (currentCutoutSubTool) {
                                                CutoutSubTool.OUTLINE -> {
                                                    if (normPts.size >= 3) {
                                                        currentOnCutoutOutlineComplete?.invoke(normPts.toList())
                                                    }
                                                }
                                                CutoutSubTool.BRUSH -> {
                                                    currentOnCutoutBrushComplete?.invoke(normPts.toList(), false)
                                                }
                                                CutoutSubTool.ERASE -> {
                                                    currentOnCutoutBrushComplete?.invoke(normPts.toList(), true)
                                                }
                                                else -> Unit
                                            }
                                        }
                                        activeCutoutStrokePoints = emptyList()
                                        break
                                    }
                                    change.consume()
                                    if (!wasMultiTouch) {
                                        val cPt = screenToCanvas(change.position)
                                        canvasPts.add(cPt.x to cPt.y)
                                        normPts.add(canvasToLayerNormalized(cPt, targetLayer))
                                        activeCutoutStrokePoints = canvasPts.toList()
                                    }
                                }
                                return@awaitEachGesture
                            }
                        }

                        // Freehand writing / drawing mode when EditorTool.DRAW is active
                        if (currentTool == EditorTool.DRAW && !currentIsHandMode) {
                            down.consume()
                            val firstCanvasPt = screenToCanvas(startPt)
                            val pts = mutableListOf(firstCanvasPt.x to firstCanvasPt.y)
                            activeStrokePoints = pts.toList()
                            var wasMultiTouch = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }
                                if (activePointers.size >= 2) {
                                    wasMultiTouch = true
                                    activeStrokePoints = emptyList()
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val nextZoom = (currentZoom * zoomChange).coerceIn(0.25f, 8f)
                                    val maxPanX = containerW * 0.85f
                                    val maxPanY = containerH * 0.85f
                                    currentOnUpdatePan(
                                        nextZoom,
                                        (currentPanX + panChange.x).coerceIn(-maxPanX, maxPanX),
                                        (currentPanY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                    event.changes.forEach { it.consume() }
                                    continue
                                }
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    if (!wasMultiTouch && pts.isNotEmpty()) {
                                        currentOnAddDrawStroke?.invoke(
                                            DrawStroke(
                                                points = pts.toList(),
                                                color = currentDrawColor,
                                                strokeWidth = currentDrawWidth,
                                                alpha = currentDrawAlpha
                                            )
                                        )
                                    }
                                    activeStrokePoints = emptyList()
                                    break
                                }
                                change.consume()
                                if (!wasMultiTouch) {
                                    val cPt = screenToCanvas(change.position)
                                    pts.add(cPt.x to cPt.y)
                                    activeStrokePoints = pts.toList()
                                }
                            }
                            return@awaitEachGesture
                        }

                        val selected = currentLayers.find { it.id == currentSelectedId }
                        val initialHandle = if (!currentIsHandMode && selected != null) {
                            hitTestSelectedHandles(startPt, selected)
                        } else CanvasHandle.NONE

                        val tappedLayer = if (!currentIsHandMode && initialHandle == CanvasHandle.NONE) {
                            hitTestLayer(screenToCanvas(startPt))
                        } else null

                        val targetLayer = when {
                            initialHandle != CanvasHandle.NONE -> selected
                            tappedLayer != null -> tappedLayer
                            else -> null
                        }

                        var activeHandle = when {
                            currentIsHandMode -> CanvasHandle.NONE
                            initialHandle != CanvasHandle.NONE -> initialHandle
                            tappedLayer != null -> CanvasHandle.BODY
                            else -> CanvasHandle.NONE
                        }

                        if (tappedLayer != null && tappedLayer.id != currentSelectedId) {
                            currentOnSelectLayer(tappedLayer.id)
                        }

                        val initialSnapshot = targetLayer
                        var lastPt = startPt

                        while (true) {
                            val event = awaitPointerEvent()
                            val activePointers = event.changes.filter { it.pressed }

                            // Pinch / pan zoom gesture when 2 fingers are down
                            if (activePointers.size >= 2) {
                                moved = true
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val nextZoom = (currentZoom * zoomChange).coerceIn(0.25f, 8f)
                                val maxPanX = containerW * 0.85f
                                val maxPanY = containerH * 0.85f
                                val nextPanX = (currentPanX + panChange.x).coerceIn(-maxPanX, maxPanX)
                                val nextPanY = (currentPanY + panChange.y).coerceIn(-maxPanY, maxPanY)
                                currentOnUpdatePan(nextZoom, nextPanX, nextPanY)
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                showSnapCenterX = false
                                showSnapCenterY = false
                                liveAngleText = null
                                liveSizeText = null

                                if (!moved && !currentIsHandMode) {
                                    // Pure tap handling — never clears/disappears underlying page!
                                    val hit = hitTestLayer(screenToCanvas(change.position))
                                    if (hit != null) {
                                        currentOnSelectLayer(hit.id)
                                        val now = System.currentTimeMillis()
                                        if (hit.type == LayerType.TEXT &&
                                            lastTapLayerId == hit.id &&
                                            now - lastTapTime < 350L
                                        ) {
                                            currentOnDoubleTapText(hit.id)
                                        }
                                        lastTapTime = now
                                        lastTapLayerId = hit.id
                                    }
                                    // Note: intentionally keep canvas and current layers intact on background tap
                                }
                                break
                            }

                            val curPt = change.position
                            val dragDelta = curPt - lastPt
                            if (!moved && hypot(curPt.x - startPt.x, curPt.y - startPt.y) > 4f) {
                                moved = true
                            }

                            if (moved) {
                                change.consume()
                                if (currentIsHandMode || activeHandle == CanvasHandle.NONE) {
                                    val maxPanX = containerW * 0.85f
                                    val maxPanY = containerH * 0.85f
                                    currentOnUpdatePan(
                                        currentZoom,
                                        (currentPanX + dragDelta.x).coerceIn(-maxPanX, maxPanX),
                                        (currentPanY + dragDelta.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                } else if (initialSnapshot != null && !initialSnapshot.isLocked) {
                                    val totalCanvasDx = (curPt.x - startPt.x) / effectiveScale
                                    val totalCanvasDy = (curPt.y - startPt.y) / effectiveScale
                                    val localDelta = unrotatedOffset(totalCanvasDx, totalCanvasDy, initialSnapshot.rotation)

                                    when (activeHandle) {
                                        CanvasHandle.BODY -> {
                                            var newX = initialSnapshot.x + totalCanvasDx
                                            var newY = initialSnapshot.y + totalCanvasDy
                                            val snapThresh = 10f / effectiveScale
                                            val layerCenterX = newX + initialSnapshot.width / 2f
                                            val layerCenterY = newY + initialSnapshot.height / 2f
                                            val canvasCenterX = canvasW / 2f
                                            val canvasCenterY = canvasH / 2f
                                            showSnapCenterX = abs(layerCenterX - canvasCenterX) < snapThresh
                                            showSnapCenterY = abs(layerCenterY - canvasCenterY) < snapThresh
                                            if (showSnapCenterX) newX = canvasCenterX - initialSnapshot.width / 2f
                                            if (showSnapCenterY) newY = canvasCenterY - initialSnapshot.height / 2f
                                            currentOnTransform(
                                                initialSnapshot.id,
                                                newX,
                                                newY,
                                                initialSnapshot.width,
                                                initialSnapshot.height,
                                                initialSnapshot.rotation
                                            )
                                        }
                                        CanvasHandle.ROTATE -> {
                                            val cx = initialSnapshot.x + initialSnapshot.width / 2f
                                            val cy = initialSnapshot.y + initialSnapshot.height / 2f
                                            val centerScreen = canvasToScreen(Offset(cx, cy))
                                            val rad = atan2(
                                                curPt.y - centerScreen.y,
                                                curPt.x - centerScreen.x
                                            )
                                            val rawDeg = (((Math.toDegrees(rad.toDouble()) + 90.0).toFloat() % 360f) + 360f) % 360f
                                            val snapDeg = ((rawDeg / 45f).roundToInt() * 45f % 360f + 360f) % 360f
                                            val finalDeg = if (abs(rawDeg - snapDeg) < 3.5f) snapDeg else rawDeg
                                            liveAngleText = "${finalDeg.roundToInt()}°"
                                            currentOnRotate(initialSnapshot.id, finalDeg)
                                        }
                                        CanvasHandle.LEFT,
                                        CanvasHandle.RIGHT,
                                        CanvasHandle.TOP,
                                        CanvasHandle.BOTTOM -> {
                                            // Edge handles always resize Width or Height independently (thinner/wider/taller/shorter)
                                            var w = initialSnapshot.width
                                            var h = initialSnapshot.height
                                            var cx = initialSnapshot.x + w / 2f
                                            var cy = initialSnapshot.y + h / 2f
                                            when (activeHandle) {
                                                CanvasHandle.RIGHT -> {
                                                    val newW = (w + localDelta.x).coerceAtLeast(24f)
                                                    val shift = rotatedOffset((newW - w) / 2f, 0f, initialSnapshot.rotation)
                                                    w = newW
                                                    cx += shift.x
                                                    cy += shift.y
                                                }
                                                CanvasHandle.LEFT -> {
                                                    val newW = (w - localDelta.x).coerceAtLeast(24f)
                                                    val shift = rotatedOffset(-(newW - w) / 2f, 0f, initialSnapshot.rotation)
                                                    w = newW
                                                    cx += shift.x
                                                    cy += shift.y
                                                }
                                                CanvasHandle.BOTTOM -> {
                                                    val newH = (h + localDelta.y).coerceAtLeast(24f)
                                                    val shift = rotatedOffset(0f, (newH - h) / 2f, initialSnapshot.rotation)
                                                    h = newH
                                                    cx += shift.x
                                                    cy += shift.y
                                                }
                                                CanvasHandle.TOP -> {
                                                    val newH = (h - localDelta.y).coerceAtLeast(24f)
                                                    val shift = rotatedOffset(0f, -(newH - h) / 2f, initialSnapshot.rotation)
                                                    h = newH
                                                    cx += shift.x
                                                    cy += shift.y
                                                }
                                                else -> Unit
                                            }
                                            liveSizeText = "${w.roundToInt()} × ${h.roundToInt()} px"
                                            currentOnTransform(
                                                initialSnapshot.id,
                                                cx - w / 2f,
                                                cy - h / 2f,
                                                w,
                                                h,
                                                initialSnapshot.rotation
                                            )
                                        }
                                        CanvasHandle.TOP_LEFT,
                                        CanvasHandle.TOP_RIGHT,
                                        CanvasHandle.BOTTOM_LEFT,
                                        CanvasHandle.BOTTOM_RIGHT -> {
                                            val signX = if (activeHandle == CanvasHandle.TOP_RIGHT || activeHandle == CanvasHandle.BOTTOM_RIGHT) 1f else -1f
                                            val signY = if (activeHandle == CanvasHandle.BOTTOM_LEFT || activeHandle == CanvasHandle.BOTTOM_RIGHT) 1f else -1f

                                            var newW = (initialSnapshot.width + signX * localDelta.x).coerceAtLeast(24f)
                                            var newH = (initialSnapshot.height + signY * localDelta.y).coerceAtLeast(24f)

                                            if (currentMaintainAspect) {
                                                val initRatio = (initialSnapshot.width / initialSnapshot.height.coerceAtLeast(1f))
                                                val scaleW = newW / initialSnapshot.width.coerceAtLeast(1f)
                                                val scaleH = newH / initialSnapshot.height.coerceAtLeast(1f)
                                                val uniScale = if (abs(scaleW - 1f) >= abs(scaleH - 1f)) scaleW else scaleH
                                                newW = (initialSnapshot.width * uniScale).coerceAtLeast(24f)
                                                newH = (newW / initRatio).coerceAtLeast(24f)
                                            }

                                            val localShiftX = signX * (newW - initialSnapshot.width) / 2f
                                            val localShiftY = signY * (newH - initialSnapshot.height) / 2f
                                            val shift = rotatedOffset(localShiftX, localShiftY, initialSnapshot.rotation)
                                            val cx = initialSnapshot.x + initialSnapshot.width / 2f + shift.x
                                            val cy = initialSnapshot.y + initialSnapshot.height / 2f + shift.y

                                            liveSizeText = "${newW.roundToInt()} × ${newH.roundToInt()} px"
                                            currentOnTransform(
                                                initialSnapshot.id,
                                                cx - newW / 2f,
                                                cy - newH / 2f,
                                                newW,
                                                newH,
                                                initialSnapshot.rotation
                                            )
                                        }
                                        else -> Unit
                                    }
                                }
                            }
                            lastPt = curPt
                        }
                    }
                }
        ) {
            val originX = (containerW - canvasW * effectiveScale) / 2f + currentPanX
            val originY = (containerH - canvasH * effectiveScale) / 2f + currentPanY

            // Canvas backdrop (Checkerboard ONLY when transparent; opaque BG ONLY when backgroundColor != 0L)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topLeft = Offset(originX, originY)
                val rectSize = Size(canvasW * effectiveScale, canvasH * effectiveScale)

                if (dimensions.isTransparent) {
                    val tilePx = 12.dp.toPx()
                    val cols = ((rectSize.width / tilePx).toInt() + 1).coerceAtMost(200)
                    val rows = ((rectSize.height / tilePx).toInt() + 1).coerceAtMost(200)
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val x = topLeft.x + c * tilePx
                            val y = topLeft.y + r * tilePx
                            val w = (topLeft.x + rectSize.width - x).coerceAtMost(tilePx)
                            val h = (topLeft.y + rectSize.height - y).coerceAtMost(tilePx)
                            if (w > 0f && h > 0f) {
                                drawRect(
                                    color = if ((r + c) % 2 == 0) CheckerLight else CheckerDark,
                                    topLeft = Offset(x, y),
                                    size = Size(w, h)
                                )
                            }
                        }
                    }
                } else if (dimensions.backgroundColor != 0L) {
                    drawRect(
                        color = Color(dimensions.backgroundColor),
                        topLeft = topLeft,
                        size = rectSize
                    )
                }
            }

            // Render visible layers clipped strictly within project canvas area
            Box(
                modifier = Modifier
                    .offset { IntOffset(originX.roundToInt(), originY.roundToInt()) }
                    .size(
                        width = with(density) { (canvasW * effectiveScale).toDp() },
                        height = with(density) { (canvasH * effectiveScale).toDp() }
                    )
                    .clipToBounds()
            ) {
                currentLayers.filter { it.isVisible }.forEach { layer ->
                    val layerWidthDp = with(density) { (layer.width * effectiveScale).toDp() }
                    val layerHeightDp = with(density) { (layer.height * effectiveScale).toDp() }
                    val layerOffsetX = (layer.x * effectiveScale).roundToInt()
                    val layerOffsetY = (layer.y * effectiveScale).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(layerOffsetX, layerOffsetY) }
                            .size(layerWidthDp, layerHeightDp)
                            .graphicsLayer {
                                rotationZ = layer.rotation
                                scaleX = layer.scaleX
                                scaleY = layer.scaleY
                                alpha = layer.opacity
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (layer.type) {
                            LayerType.IMAGE -> {
                                val bmp = layer.bitmap
                                if (bmp != null && !bmp.isRecycled) {
                                    val imgBmp = remember(bmp) { bmp.asImageBitmap() }
                                    val cmArray = remember(layer.filters) {
                                        ImageProcessor.createComposeColorMatrix(layer.filters)
                                    }
                                    Image(
                                        bitmap = imgBmp,
                                        contentDescription = layer.name,
                                        contentScale = ContentScale.FillBounds,
                                        colorFilter = ColorFilter.colorMatrix(ColorMatrix(cmArray)),
                                        filterQuality = FilterQuality.High,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            LayerType.TEXT -> {
                                // Box-free text rendering unless layer.hasBackground == true
                                val textModifier = Modifier
                                    .fillMaxSize()
                                    .let { m ->
                                        if (layer.hasBackground) {
                                            m.background(
                                                Color(layer.backgroundColor),
                                                RoundedCornerShape(layer.backgroundCornerRadius.dp)
                                            )
                                        } else m
                                    }
                                    .alpha(layer.textAlpha)

                                Box(modifier = textModifier, contentAlignment = Alignment.Center) {
                                    AndroidView(
                                        factory = { ctx ->
                                            android.widget.TextView(ctx).apply {
                                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                            }
                                        },
                                        update = { tv ->
                                            tv.text = layer.text
                                            tv.setTextColor(layer.textColor.toInt())
                                            tv.setTextSize(
                                                android.util.TypedValue.COMPLEX_UNIT_PX,
                                                (layer.fontSize * effectiveScale).coerceAtLeast(8f)
                                            )
                                            tv.typeface = FontManager.getTypeface(
                                                context,
                                                layer.fontFamily,
                                                layer.isBold,
                                                layer.isItalic
                                            )
                                            tv.letterSpacing = layer.letterSpacing / 20f
                                            tv.gravity = when (layer.textAlignment.lowercase()) {
                                                "left" -> android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                                                "right" -> android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                                                else -> android.view.Gravity.CENTER
                                            }
                                            if (layer.hasShadow) {
                                                tv.setShadowLayer(
                                                    layer.shadowRadius.coerceAtLeast(0.5f),
                                                    layer.shadowOffsetX,
                                                    layer.shadowOffsetY,
                                                    layer.shadowColor.toInt()
                                                )
                                            } else {
                                                tv.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Canvas border & selection gizmo + snap guides
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rectTopLeft = Offset(originX, originY)
                val rectSize = Size(canvasW * effectiveScale, canvasH * effectiveScale)

                // Project boundary outline
                drawRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = rectTopLeft,
                    size = rectSize,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Center alignment guides
                val guideDash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                if (showSnapCenterX) {
                    val cxScreen = originX + (canvasW * effectiveScale) / 2f
                    drawLine(
                        color = SecondaryTeal,
                        start = Offset(cxScreen, originY),
                        end = Offset(cxScreen, originY + canvasH * effectiveScale),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = guideDash
                    )
                }
                if (showSnapCenterY) {
                    val cyScreen = originY + (canvasH * effectiveScale) / 2f
                    drawLine(
                        color = SecondaryTeal,
                        start = Offset(originX, cyScreen),
                        end = Offset(originX + canvasW * effectiveScale, cyScreen),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = guideDash
                    )
                }

                // Live freehand drawing preview strokes
                val allPreviewStrokes = if (activeStrokePoints.isNotEmpty()) {
                    drawStrokes + DrawStroke(
                        points = activeStrokePoints,
                        color = currentDrawColor,
                        strokeWidth = currentDrawWidth,
                        alpha = currentDrawAlpha
                    )
                } else drawStrokes

                allPreviewStrokes.forEach { stroke ->
                    if (stroke.points.isNotEmpty()) {
                        val strokeColor = Color(stroke.color).copy(alpha = stroke.alpha)
                        val pxWidth = (stroke.strokeWidth * effectiveScale).coerceAtLeast(1.5f)
                        if (stroke.points.size == 1) {
                            val (cx, cy) = stroke.points[0]
                            drawCircle(
                                color = strokeColor,
                                radius = pxWidth / 2f,
                                center = Offset(originX + cx * effectiveScale, originY + cy * effectiveScale)
                            )
                        } else {
                            val path = Path().apply {
                                val (sx, sy) = stroke.points[0]
                                moveTo(originX + sx * effectiveScale, originY + sy * effectiveScale)
                                for (i in 1 until stroke.points.size) {
                                    val (px, py) = stroke.points[i]
                                    lineTo(originX + px * effectiveScale, originY + py * effectiveScale)
                                }
                            }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(
                                    width = pxWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                // Live Cutout Outline / Brush / Erase preview overlay
                if (activeCutoutStrokePoints.isNotEmpty()) {
                    if (currentCutoutSubTool == CutoutSubTool.OUTLINE) {
                        val lassoColor = if (currentCutoutOutlineKeepInside) Color(0xFF2DD4BF) else Color(0xFFEF4444)
                        val lassoPath = Path().apply {
                            val (sx, sy) = activeCutoutStrokePoints[0]
                            moveTo(originX + sx * effectiveScale, originY + sy * effectiveScale)
                            for (i in 1 until activeCutoutStrokePoints.size) {
                                val (px, py) = activeCutoutStrokePoints[i]
                                lineTo(originX + px * effectiveScale, originY + py * effectiveScale)
                            }
                        }
                        drawPath(
                            path = lassoPath,
                            color = lassoColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )
                        if (activeCutoutStrokePoints.size >= 2) {
                            val (sx, sy) = activeCutoutStrokePoints.first()
                            val (ex, ey) = activeCutoutStrokePoints.last()
                            drawLine(
                                color = lassoColor.copy(alpha = 0.5f),
                                start = Offset(originX + ex * effectiveScale, originY + ey * effectiveScale),
                                end = Offset(originX + sx * effectiveScale, originY + sy * effectiveScale),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        }
                    } else if (currentCutoutSubTool == CutoutSubTool.BRUSH || currentCutoutSubTool == CutoutSubTool.ERASE) {
                        val isErase = currentCutoutSubTool == CutoutSubTool.ERASE
                        val previewCol = if (isErase) Color(0xFFEF4444).copy(alpha = 0.55f) else Color(0xFF22C55E).copy(alpha = 0.55f)
                        val pxBrush = (currentCutoutBrushSize * effectiveScale).coerceAtLeast(6f)
                        val brushPath = Path().apply {
                            val (sx, sy) = activeCutoutStrokePoints[0]
                            moveTo(originX + sx * effectiveScale, originY + sy * effectiveScale)
                            for (i in 1 until activeCutoutStrokePoints.size) {
                                val (px, py) = activeCutoutStrokePoints[i]
                                lineTo(originX + px * effectiveScale, originY + py * effectiveScale)
                            }
                        }
                        drawPath(
                            path = brushPath,
                            color = previewCol,
                            style = Stroke(
                                width = pxBrush,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        val (lx, ly) = activeCutoutStrokePoints.last()
                        drawCircle(
                            color = Color.White,
                            radius = pxBrush / 2f,
                            center = Offset(originX + lx * effectiveScale, originY + ly * effectiveScale),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Selected layer bounding box, 8 resize handles, and 360-deg rotation handle
                val selected = currentLayers.find { it.id == currentSelectedId }
                val hideHandlesForCutout = currentTool == EditorTool.BG_REMOVE &&
                    (currentCutoutSubTool == CutoutSubTool.OUTLINE ||
                        currentCutoutSubTool == CutoutSubTool.BRUSH ||
                        currentCutoutSubTool == CutoutSubTool.ERASE)
                if (selected != null && selected.isVisible && currentTool != EditorTool.CROP && currentTool != EditorTool.DRAW && !hideHandlesForCutout) {
                    val cx = originX + (selected.x + selected.width / 2f) * effectiveScale
                    val cy = originY + (selected.y + selected.height / 2f) * effectiveScale
                    val halfW = (selected.width * effectiveScale) / 2f
                    val halfH = (selected.height * effectiveScale) / 2f
                    val rot = selected.rotation

                    fun toWorld(lx: Float, ly: Float): Offset {
                        val o = rotatedOffset(lx, ly, rot)
                        return Offset(cx + o.x, cy + o.y)
                    }

                    val tl = toWorld(-halfW, -halfH)
                    val tr = toWorld(halfW, -halfH)
                    val br = toWorld(halfW, halfH)
                    val bl = toWorld(-halfW, halfH)
                    val tMid = toWorld(0f, -halfH)
                    val bMid = toWorld(0f, halfH)
                    val lMid = toWorld(-halfW, 0f)
                    val rMid = toWorld(halfW, 0f)
                    val rotHandle = toWorld(0f, -halfH - 30.dp.toPx())

                    val outlineStroke = 1.8.dp.toPx()
                    drawLine(PrimaryBlue, tl, tr, outlineStroke)
                    drawLine(PrimaryBlue, tr, br, outlineStroke)
                    drawLine(PrimaryBlue, br, bl, outlineStroke)
                    drawLine(PrimaryBlue, bl, tl, outlineStroke)

                    if (!selected.isLocked) {
                        // Rotation stem & knob
                        drawLine(SecondaryTeal, tMid, rotHandle, 1.5.dp.toPx())
                        drawCircle(SecondaryTeal, radius = 8.dp.toPx(), center = rotHandle)
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = rotHandle)

                        // Corner & edge resize handles
                        val handleRadius = 5.5.dp.toPx()
                        listOf(tl, tr, br, bl).forEach { pt ->
                            drawCircle(PrimaryBlue, radius = handleRadius + 1.5.dp.toPx(), center = pt)
                            drawCircle(Color.White, radius = handleRadius, center = pt)
                        }
                        listOf(tMid, bMid, lMid, rMid).forEach { pt ->
                            drawCircle(SecondaryTeal, radius = 4.5.dp.toPx(), center = pt)
                            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pt)
                        }
                    }
                }
            }

            // Live rotation/size feedback badge during drag
            val badgeText = liveAngleText ?: liveSizeText
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(10.dp))
                        .border(1.dp, SecondaryTeal.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
