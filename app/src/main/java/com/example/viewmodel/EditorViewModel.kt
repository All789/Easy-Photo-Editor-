package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.model.CanvasDimensions
import com.example.model.CropRect
import com.example.model.EditorTool
import com.example.model.FilterSettings
import com.example.model.Layer
import com.example.model.LayerType
import com.example.processing.BackgroundRemover
import com.example.processing.ImageProcessor
import com.example.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<HistorySnapshot>()
    private val redoStack = mutableListOf<HistorySnapshot>()
    private val maxHistory = 30

    init {
        refreshRecentProjects()
    }

    fun updateStateDirectly(transform: (EditorUiState) -> EditorUiState) {
        _uiState.update(transform)
    }

    fun resetUndoHistory() {
        undoStack.clear()
        redoStack.clear()
        _uiState.update { it.copy(canUndo = false, canRedo = false) }
    }

    fun pushUndoSnapshot() {
        val current = _uiState.value
        undoStack.add(
            HistorySnapshot(
                layers = current.layers,
                selectedLayerId = current.selectedLayerId,
                canvasDimensions = current.canvasDimensions,
                canvasDpi = current.canvasDpi
            )
        )
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value
        redoStack.add(
            HistorySnapshot(
                layers = current.layers,
                selectedLayerId = current.selectedLayerId,
                canvasDimensions = current.canvasDimensions,
                canvasDpi = current.canvasDpi
            )
        )
        val snapshot = undoStack.removeAt(undoStack.lastIndex)
        _uiState.update { state ->
            state.copy(
                layers = snapshot.layers,
                selectedLayerId = snapshot.selectedLayerId,
                canvasDimensions = snapshot.canvasDimensions,
                canvasDpi = snapshot.canvasDpi,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusMessage = "Undone"
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value
        undoStack.add(
            HistorySnapshot(
                layers = current.layers,
                selectedLayerId = current.selectedLayerId,
                canvasDimensions = current.canvasDimensions,
                canvasDpi = current.canvasDpi
            )
        )
        val snapshot = redoStack.removeAt(redoStack.lastIndex)
        _uiState.update { state ->
            state.copy(
                layers = snapshot.layers,
                selectedLayerId = snapshot.selectedLayerId,
                canvasDimensions = snapshot.canvasDimensions,
                canvasDpi = snapshot.canvasDpi,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                statusMessage = "Redone"
            )
        }
    }

    fun refreshRecentProjects() {
        viewModelScope.launch {
            val items = repository.listProjects()
            _uiState.update { it.copy(recentProjects = items) }
        }
    }

    fun createNewProject(
        width: Int,
        height: Int,
        isTransparent: Boolean,
        bgColor: Long,
        name: String = "Untitled Project",
        dpi: Int = 72
    ) {
        resetUndoHistory()
        val safeW = width.coerceIn(16, 8000)
        val safeH = height.coerceIn(16, 8000)
        val safeDpi = dpi.coerceIn(72, 1200)
        _uiState.update { state ->
            state.copy(
                projectId = "proj_${System.currentTimeMillis()}",
                projectName = name.ifBlank { "Untitled Project" },
                canvasDimensions = CanvasDimensions(
                    width = safeW,
                    height = safeH,
                    isTransparent = isTransparent,
                    backgroundColor = if (isTransparent) 0L else bgColor
                ),
                canvasDpi = safeDpi,
                layers = emptyList(),
                selectedLayerId = null,
                activeTool = EditorTool.SELECT,
                cropRect = CropRect(),
                cropAspectRatio = null,
                cropMaintainAspectRatio = false,
                cropMatchCanvas = false,
                zoom = 1f,
                panX = 0f,
                panY = 0f,
                canUndo = false,
                canRedo = false,
                hasActiveProject = true,
                statusMessage = "Created canvas ${safeW} × ${safeH} px @ ${safeDpi} DPI"
            )
        }
    }

    fun loadSamplePreset(presetKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val resId = when (presetKey) {
                "portrait" -> R.drawable.sample_portrait
                else -> R.drawable.sample_landscape
            }
            val bitmap = repository.loadDrawableAsBitmap(resId)
            if (bitmap != null) {
                val label = if (presetKey == "portrait") "Sample Portrait" else "Sample Landscape"
                openWithPhotoDirectly(bitmap, label)
            } else {
                _uiState.update { it.copy(errorMessage = "Could not load sample image") }
            }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    fun loadExistingProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val loaded = repository.loadProject(projectId)
            if (loaded != null) {
                resetUndoHistory()
                val (dims, layers) = loaded
                val projName = _uiState.value.recentProjects
                    .find { it.id == projectId }?.name ?: "Untitled Project"
                _uiState.update { state ->
                    state.copy(
                        projectId = projectId,
                        projectName = projName,
                        canvasDimensions = dims,
                        layers = layers,
                        selectedLayerId = layers.lastOrNull()?.id,
                        activeTool = EditorTool.SELECT,
                        zoom = 1f,
                        panX = 0f,
                        panY = 0f,
                        hasActiveProject = true,
                        isProcessing = false,
                        statusMessage = "Loaded \"$projName\""
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isProcessing = false, errorMessage = "Failed to load project")
                }
            }
        }
    }

    fun deleteRecentProject(projectId: String) {
        viewModelScope.launch {
            val ok = repository.deleteProject(projectId)
            if (ok) {
                refreshRecentProjects()
                _uiState.update { it.copy(statusMessage = "Project deleted") }
            }
        }
    }

    fun addImageLayer(bitmap: Bitmap, name: String = "Image Layer") {
        importPhotoToCanvas(bitmap, name, fillCanvas = false)
    }

    fun addTextLayer(
        text: String,
        color: Long = 0xFFFFFFFFL,
        fontSize: Float = 64f,
        fontFamily: String = "Default"
    ) {
        if (text.isBlank()) return
        pushUndoSnapshot()
        val canvasW = _uiState.value.canvasDimensions.width.toFloat().coerceAtLeast(100f)
        val canvasH = _uiState.value.canvasDimensions.height.toFloat().coerceAtLeast(100f)
        val defaultW = (canvasW * 0.72f).coerceIn(180f, canvasW * 0.95f)
        val defaultH = (fontSize * 2.2f).coerceIn(72f, canvasH * 0.5f)
        val newLayer = Layer(
            id = UUID.randomUUID().toString(),
            name = text.take(18),
            type = LayerType.TEXT,
            x = (canvasW - defaultW) / 2f,
            y = (canvasH - defaultH) / 2f,
            width = defaultW,
            height = defaultH,
            rotation = 0f,
            scaleX = 1f,
            scaleY = 1f,
            opacity = 1f,
            text = text,
            textColor = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            hasBackground = false
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + newLayer,
                selectedLayerId = newLayer.id,
                hasActiveProject = true
            )
        }
    }

    fun selectLayer(layerId: String?) {
        _uiState.update { it.copy(selectedLayerId = layerId) }
    }

    fun toggleLayerVisibility(layerId: String) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
                }
            )
        }
    }

    fun toggleLayerLock(layerId: String) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
                }
            )
        }
    }

    fun duplicateLayer(layerId: String) {
        val layer = _uiState.value.layers.find { it.id == layerId } ?: return
        pushUndoSnapshot()
        val copy = layer.copy(
            id = UUID.randomUUID().toString(),
            name = "${layer.name} Copy",
            x = layer.x + 24f,
            y = layer.y + 24f,
            bitmap = layer.bitmap?.copy(layer.bitmap.config ?: Bitmap.Config.ARGB_8888, true),
            originalBitmap = layer.originalBitmap?.copy(
                layer.originalBitmap.config ?: Bitmap.Config.ARGB_8888,
                true
            )
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + copy,
                selectedLayerId = copy.id,
                statusMessage = "Duplicated ${layer.name}"
            )
        }
    }

    fun deleteLayer(layerId: String) {
        pushUndoSnapshot()
        _uiState.update { state ->
            val remaining = state.layers.filterNot { it.id == layerId }
            val nextSelected = if (state.selectedLayerId == layerId) {
                remaining.lastOrNull()?.id
            } else state.selectedLayerId
            state.copy(layers = remaining, selectedLayerId = nextSelected)
        }
    }

    fun moveLayerUp(layerId: String) {
        val current = _uiState.value.layers.toMutableList()
        val idx = current.indexOfFirst { it.id == layerId }
        if (idx in 0 until current.lastIndex) {
            pushUndoSnapshot()
            val item = current.removeAt(idx)
            current.add(idx + 1, item)
            _uiState.update { it.copy(layers = current) }
        }
    }

    fun moveLayerDown(layerId: String) {
        val current = _uiState.value.layers.toMutableList()
        val idx = current.indexOfFirst { it.id == layerId }
        if (idx > 0) {
            pushUndoSnapshot()
            val item = current.removeAt(idx)
            current.add(idx - 1, item)
            _uiState.update { it.copy(layers = current) }
        }
    }

    fun updateLayerOpacity(layerId: String, opacity: Float) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
                }
            )
        }
    }

    fun renameLayer(layerId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(name = trimmed) else it
                }
            )
        }
    }

    fun updateLayerTransform(
        layerId: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float
    ) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && !it.isLocked) {
                        it.copy(
                            x = x,
                            y = y,
                            width = width.coerceAtLeast(16f),
                            height = height.coerceAtLeast(16f),
                            rotation = ((rotation % 360f) + 360f) % 360f
                        )
                    } else it
                }
            )
        }
    }

    fun updateLayerPosition(layerId: String, x: Float, y: Float) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && !it.isLocked) it.copy(x = x, y = y) else it
                }
            )
        }
    }

    fun updateLayerRotation(layerId: String, rotation: Float) {
        val normalized = ((rotation % 360f) + 360f) % 360f
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && !it.isLocked) it.copy(rotation = normalized) else it
                }
            )
        }
    }

    fun updateLayerScale(layerId: String, scaleX: Float, scaleY: Float) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && !it.isLocked) {
                        it.copy(scaleX = scaleX, scaleY = scaleY)
                    } else it
                }
            )
        }
    }

    fun updateTextLayer(
        layerId: String,
        text: String,
        color: Long,
        fontSize: Float,
        fontFamily: String,
        opacity: Float = 1f
    ) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && it.type == LayerType.TEXT) {
                        it.copy(
                            text = text,
                            name = text.take(18).ifBlank { it.name },
                            textColor = color,
                            fontSize = fontSize.coerceIn(10f, 300f),
                            fontFamily = fontFamily,
                            opacity = opacity.coerceIn(0f, 1f)
                        )
                    } else it
                }
            )
        }
    }

    fun updateTextProperties(
        layerId: String,
        text: String,
        color: Long,
        fontSize: Float,
        fontFamily: String,
        isBold: Boolean,
        isItalic: Boolean,
        alignment: String
    ) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && it.type == LayerType.TEXT) {
                        it.copy(
                            text = text,
                            name = text.take(18).ifBlank { it.name },
                            textColor = color,
                            fontSize = fontSize.coerceIn(10f, 300f),
                            fontFamily = fontFamily,
                            isBold = isBold,
                            isItalic = isItalic,
                            textAlignment = alignment
                        )
                    } else it
                }
            )
        }
    }

    fun updateTextEffects(
        layerId: String,
        hasShadow: Boolean,
        shadowColor: Long,
        shadowRadius: Float,
        shadowOffsetX: Float,
        shadowOffsetY: Float,
        hasOutline: Boolean,
        outlineColor: Long,
        outlineWidth: Float,
        hasBackground: Boolean,
        backgroundColor: Long,
        backgroundCornerRadius: Float,
        textAlpha: Float
    ) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && it.type == LayerType.TEXT) {
                        it.copy(
                            hasShadow = hasShadow,
                            shadowColor = shadowColor,
                            shadowRadius = shadowRadius,
                            shadowOffsetX = shadowOffsetX,
                            shadowOffsetY = shadowOffsetY,
                            hasOutline = hasOutline,
                            outlineColor = outlineColor,
                            outlineWidth = outlineWidth,
                            hasBackground = hasBackground,
                            backgroundColor = backgroundColor,
                            backgroundCornerRadius = backgroundCornerRadius,
                            textAlpha = textAlpha.coerceIn(0f, 1f)
                        )
                    } else it
                }
            )
        }
    }

    fun updateTextSpacing(layerId: String, letterSpacing: Float, opacity: Float) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId && it.type == LayerType.TEXT) {
                        it.copy(
                            letterSpacing = letterSpacing,
                            opacity = opacity.coerceIn(0f, 1f)
                        )
                    } else it
                }
            )
        }
    }

    fun applyFilterSettings(layerId: String, filters: FilterSettings) {
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(filters = filters) else it
                }
            )
        }
    }

    fun applyFilterPreset(layerId: String, presetName: String) {
        pushUndoSnapshot()
        val preset = when (presetName.lowercase()) {
            "vibrant" -> FilterSettings(contrast = 1.18f, saturation = 1.45f, sharpen = 0.25f)
            "bw", "grayscale" -> FilterSettings(contrast = 1.15f, saturation = 0f, grayscale = true)
            "sepia", "vintage" -> FilterSettings(brightness = 0.04f, contrast = 1.08f, saturation = 0.8f, sepia = true, temperature = 15f)
            "dramatic" -> FilterSettings(brightness = -0.05f, contrast = 1.38f, saturation = 1.1f, sharpen = 0.35f)
            "warm" -> FilterSettings(saturation = 1.12f, temperature = 35f)
            "cool" -> FilterSettings(saturation = 1.05f, temperature = -35f)
            else -> FilterSettings()
        }
        applyFilterSettings(layerId, preset)
    }

    fun setActiveTool(tool: EditorTool) {
        if (tool == EditorTool.CROP) {
            // Prepare crop rect safely if none selected
            val current = _uiState.value
            val imageLayer = current.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
                ?: current.layers.lastOrNull { it.type == LayerType.IMAGE }
            _uiState.update { state ->
                state.copy(
                    activeTool = tool,
                    selectedLayerId = imageLayer?.id ?: state.selectedLayerId,
                    cropRect = CropRect()
                )
            }
        } else {
            _uiState.update { it.copy(activeTool = tool) }
        }
    }

    fun updateCropRect(crop: CropRect) {
        val left = crop.left.coerceIn(0f, 0.95f)
        val top = crop.top.coerceIn(0f, 0.95f)
        val right = crop.right.coerceIn(left + 0.03f, 1f)
        val bottom = crop.bottom.coerceIn(top + 0.03f, 1f)
        _uiState.update { it.copy(cropRect = CropRect(left, top, right, bottom)) }
    }

    fun setCropMaintainAspectRatio(locked: Boolean) {
        _uiState.update { state ->
            val currentAspect = if (locked) {
                state.cropAspectRatio ?: run {
                    val bmp = targetCropLayer()?.bitmap
                    if (bmp != null && bmp.height > 0) {
                        val selW = state.cropRect.width * bmp.width
                        val selH = state.cropRect.height * bmp.height
                        if (selH > 0f) selW / selH else 1f
                    } else 1f
                }
            } else {
                null
            }
            state.copy(
                cropMaintainAspectRatio = locked,
                cropAspectRatio = currentAspect,
                cropMatchCanvas = if (!locked) false else state.cropMatchCanvas
            )
        }
    }

    fun setCropAspectRatio(ratio: Float?, matchCanvas: Boolean = false) {
        val targetLayer = targetCropLayer()
        val bmpW = targetLayer?.bitmap?.width?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val bmpH = targetLayer?.bitmap?.height?.toFloat()?.coerceAtLeast(1f) ?: 1f
        val imgAspect = bmpW / bmpH

        if (ratio == null) {
            _uiState.update {
                it.copy(
                    cropAspectRatio = null,
                    cropMaintainAspectRatio = false,
                    cropMatchCanvas = false,
                    cropRect = CropRect(0f, 0f, 1f, 1f)
                )
            }
            return
        }

        // Convert desired output ratio (wPx/hPx) into normalized [0..1] crop rect ratio
        val normRatio = ratio / imgAspect
        val (rectW, rectH) = if (normRatio >= 1f) {
            1f to (1f / normRatio).coerceIn(0.05f, 1f)
        } else {
            normRatio.coerceIn(0.05f, 1f) to 1f
        }
        val left = ((1f - rectW) / 2f).coerceIn(0f, 1f - rectW)
        val top = ((1f - rectH) / 2f).coerceIn(0f, 1f - rectH)

        _uiState.update { state ->
            state.copy(
                cropAspectRatio = ratio,
                cropMaintainAspectRatio = true,
                cropMatchCanvas = matchCanvas,
                cropRect = CropRect(left, top, left + rectW, top + rectH)
            )
        }
    }

    fun setCropToProjectCanvas() {
        val dims = _uiState.value.canvasDimensions
        val ratio = dims.width.toFloat().coerceAtLeast(1f) / dims.height.toFloat().coerceAtLeast(1f)
        setCropAspectRatio(ratio, matchCanvas = true)
    }

    fun setCropManualPixelDimensions(targetWidthPx: Int, targetHeightPx: Int) {
        val layer = targetCropLayer() ?: return
        val bmp = layer.bitmap ?: return
        val srcW = bmp.width.coerceAtLeast(1)
        val srcH = bmp.height.coerceAtLeast(1)

        val clampedW = targetWidthPx.coerceIn(1, srcW)
        val clampedH = targetHeightPx.coerceIn(1, srcH)

        val normW = (clampedW.toFloat() / srcW).coerceIn(0.02f, 1f)
        val normH = (clampedH.toFloat() / srcH).coerceIn(0.02f, 1f)

        val cur = _uiState.value.cropRect
        val centerX = (cur.left + cur.right) / 2f
        val centerY = (cur.top + cur.bottom) / 2f

        val left = (centerX - normW / 2f).coerceIn(0f, 1f - normW)
        val top = (centerY - normH / 2f).coerceIn(0f, 1f - normH)

        val newRatio = if (_uiState.value.cropMaintainAspectRatio) {
            clampedW.toFloat() / clampedH.toFloat()
        } else _uiState.value.cropAspectRatio

        _uiState.update {
            it.copy(
                cropRect = CropRect(left, top, left + normW, top + normH),
                cropAspectRatio = newRatio
            )
        }
    }

    private fun targetCropLayer(): Layer? {
        val state = _uiState.value
        return state.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
            ?: state.layers.lastOrNull { it.type == LayerType.IMAGE }
    }

    /**
     * Applies the current crop selection cleanly without stretching pixels.
     * - When [isDirectPhotoMode] is true (gallery single-photo mode), adapts canvas dimensions to cropped size.
     * - When [isDirectPhotoMode] is false (custom project canvas e.g. 512x512 @ 300 DPI), preserves canvasDimensions
     *   and fits the cropped image proportionally (or fills canvas cleanly when aspect ratios match) with full quality.
     */
    fun applyCrop(
        isDirectPhotoMode: Boolean = false,
        fitToProjectCanvas: Boolean = true
    ) {
        val state = _uiState.value
        val target = targetCropLayer() ?: return
        val sourceBmp = target.bitmap ?: return
        val crop = state.cropRect

        val cropped = ImageProcessor.cropBitmap(sourceBmp, crop)
        val origCropped = target.originalBitmap?.let { ImageProcessor.cropBitmap(it, crop) }
            ?: cropped.copy(Bitmap.Config.ARGB_8888, true)

        pushUndoSnapshot()

        val croppedW = cropped.width.toFloat().coerceAtLeast(1f)
        val croppedH = cropped.height.toFloat().coerceAtLeast(1f)
        val croppedAspect = croppedW / croppedH

        if (isDirectPhotoMode) {
            val newCanvas = state.canvasDimensions.copy(
                width = cropped.width,
                height = cropped.height
            )
            _uiState.update { cur ->
                cur.copy(
                    canvasDimensions = newCanvas,
                    layers = cur.layers.map { l ->
                        if (l.id == target.id) {
                            l.copy(
                                bitmap = cropped,
                                originalBitmap = origCropped,
                                x = 0f,
                                y = 0f,
                                width = croppedW,
                                height = croppedH
                            )
                        } else l
                    },
                    activeTool = EditorTool.SELECT,
                    cropRect = CropRect(),
                    cropAspectRatio = null,
                    cropMaintainAspectRatio = false,
                    cropMatchCanvas = false,
                    statusMessage = "Cropped to ${cropped.width} × ${cropped.height} px"
                )
            }
        } else {
            // Custom project canvas (e.g., 512x512 @ 300 DPI): NEVER overwrite canvasDimensions!
            val canvasW = state.canvasDimensions.width.toFloat().coerceAtLeast(1f)
            val canvasH = state.canvasDimensions.height.toFloat().coerceAtLeast(1f)
            val canvasAspect = canvasW / canvasH
            val matchesCanvasAspect = abs(croppedAspect - canvasAspect) <= 0.025f || state.cropMatchCanvas

            val (newX, newY, newW, newH) = if (matchesCanvasAspect && fitToProjectCanvas) {
                // Proportional fill of exact project canvas dimensions without stretching
                Quad(0f, 0f, canvasW, canvasH)
            } else if (fitToProjectCanvas) {
                val scale = min(canvasW / croppedW, canvasH / croppedH)
                val w = croppedW * scale
                val h = croppedH * scale
                Quad((canvasW - w) / 2f, (canvasH - h) / 2f, w, h)
            } else {
                val prevCenterX = target.x + target.width / 2f
                val prevCenterY = target.y + target.height / 2f
                val scale = min(target.width / croppedW, target.height / croppedH)
                val w = croppedW * scale
                val h = croppedH * scale
                Quad(prevCenterX - w / 2f, prevCenterY - h / 2f, w, h)
            }

            _uiState.update { cur ->
                cur.copy(
                    layers = cur.layers.map { l ->
                        if (l.id == target.id) {
                            l.copy(
                                bitmap = cropped,
                                originalBitmap = origCropped,
                                x = newX,
                                y = newY,
                                width = newW,
                                height = newH
                            )
                        } else l
                    },
                    activeTool = EditorTool.SELECT,
                    cropRect = CropRect(),
                    cropAspectRatio = null,
                    cropMaintainAspectRatio = false,
                    cropMatchCanvas = false,
                    statusMessage = "Cropped (${cropped.width}×${cropped.height} px) in ${state.canvasDimensions.width}×${state.canvasDimensions.height} px project"
                )
            }
        }
    }

    private data class Quad(val x: Float, val y: Float, val w: Float, val h: Float)

    fun removeBackgroundAi(smoothing: Float = 0.5f) {
        val target = _uiState.value.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
            ?: _uiState.value.layers.lastOrNull { it.type == LayerType.IMAGE }
            ?: return
        val source = target.originalBitmap ?: target.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBgRemove = true, statusMessage = "Removing background...") }
            val result = BackgroundRemover.removeBackgroundAi(source, smoothing)
            pushUndoSnapshot()
            _uiState.update { state ->
                state.copy(
                    isProcessingBgRemove = false,
                    layers = state.layers.map {
                        if (it.id == target.id) it.copy(bitmap = result) else it
                    },
                    statusMessage = "Background removed"
                )
            }
        }
    }

    fun removeBackgroundColor(targetColor: Int, tolerance: Float, feather: Float) {
        val target = _uiState.value.selectedLayer?.takeIf { it.type == LayerType.IMAGE } ?: return
        val source = target.originalBitmap ?: target.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBgRemove = true) }
            val result = withContext(Dispatchers.Default) {
                BackgroundRemover.removeBackgroundByColor(source, targetColor, tolerance, feather)
            }
            pushUndoSnapshot()
            _uiState.update { state ->
                state.copy(
                    isProcessingBgRemove = false,
                    layers = state.layers.map {
                        if (it.id == target.id) it.copy(bitmap = result) else it
                    },
                    statusMessage = "Color background removed"
                )
            }
        }
    }

    fun applyBackdrop(
        backdropType: BackgroundRemover.BackdropType,
        solidColor: Long = 0xFFFFFFFFL,
        gradientStart: Long = 0xFF1E3A8AL,
        gradientEnd: Long = 0xFF38BDF8L,
        blurRadius: Float = 15f
    ) {
        val target = _uiState.value.selectedLayer?.takeIf { it.type == LayerType.IMAGE } ?: return
        val cutout = target.bitmap ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBgRemove = true) }
            val composited = withContext(Dispatchers.Default) {
                BackgroundRemover.applyBackdrop(
                    cutoutBitmap = cutout,
                    originalBitmap = target.originalBitmap,
                    backdropType = backdropType,
                    solidColor = solidColor,
                    gradientStart = gradientStart,
                    gradientEnd = gradientEnd,
                    blurRadius = blurRadius
                )
            }
            pushUndoSnapshot()
            _uiState.update { state ->
                state.copy(
                    isProcessingBgRemove = false,
                    layers = state.layers.map {
                        if (it.id == target.id) it.copy(bitmap = composited) else it
                    },
                    statusMessage = "Backdrop applied"
                )
            }
        }
    }

    fun restoreOriginalLayerBitmap(layerId: String) {
        val target = _uiState.value.layers.find { it.id == layerId } ?: return
        val orig = target.originalBitmap ?: return
        pushUndoSnapshot()
        val restored = orig.copy(orig.config ?: Bitmap.Config.ARGB_8888, true)
        _uiState.update { state ->
            state.copy(
                layers = state.layers.map {
                    if (it.id == layerId) it.copy(bitmap = restored) else it
                },
                statusMessage = "Restored original image"
            )
        }
    }

    fun duplicateLayerAsCutout(layerId: String) {
        val target = _uiState.value.layers.find { it.id == layerId } ?: return
        val bmp = target.bitmap ?: return
        pushUndoSnapshot()
        val copy = target.copy(
            id = UUID.randomUUID().toString(),
            name = "${target.name} Cutout",
            x = target.x + 20f,
            y = target.y + 20f,
            bitmap = bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true),
            originalBitmap = bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true)
        )
        _uiState.update { state ->
            state.copy(
                layers = state.layers + copy,
                selectedLayerId = copy.id,
                statusMessage = "Duplicated cutout layer"
            )
        }
    }

    fun updateCanvasSettings(
        width: Int,
        height: Int,
        isTransparent: Boolean,
        bgColor: Long
    ) {
        pushUndoSnapshot()
        _uiState.update { state ->
            state.copy(
                canvasDimensions = CanvasDimensions(
                    width = width.coerceIn(16, 8000),
                    height = height.coerceIn(16, 8000),
                    isTransparent = isTransparent,
                    backgroundColor = if (isTransparent) 0L else bgColor
                ),
                statusMessage = "Canvas updated (${width}×${height} px)"
            )
        }
    }

    fun updateZoomAndPan(zoom: Float, panX: Float, panY: Float) {
        _uiState.update {
            it.copy(
                zoom = zoom.coerceIn(0.25f, 8f),
                panX = panX,
                panY = panY
            )
        }
    }

    fun resetView() {
        _uiState.update { it.copy(zoom = 1f, panX = 0f, panY = 0f) }
    }

    fun saveProject() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val thumb = withContext(Dispatchers.Default) {
                val rendered = ImageProcessor.renderComposition(
                    state.canvasDimensions,
                    state.layers,
                    getApplication()
                )
                val maxDim = max(rendered.width, rendered.height).coerceAtLeast(1)
                val scale = (300f / maxDim).coerceAtMost(1f)
                ImageProcessor.scaleBitmapHighQuality(
                    rendered,
                    (rendered.width * scale).roundToInt().coerceAtLeast(1),
                    (rendered.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
            val saved = repository.saveProject(
                projectId = state.projectId,
                name = state.projectName,
                dimensions = state.canvasDimensions,
                layers = state.layers,
                thumbnail = thumb
            )
            val list = repository.listProjects()
            _uiState.update {
                it.copy(
                    isSaving = false,
                    recentProjects = list,
                    statusMessage = if (saved) "Project saved" else "Save failed"
                )
            }
        }
    }

    fun exportFinalImage(format: String = "PNG", quality: Int = 95) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val bitmap = withContext(Dispatchers.Default) {
                ImageProcessor.renderComposition(
                    state.canvasDimensions,
                    state.layers,
                    getApplication()
                )
            }
            _uiState.update { it.copy(isProcessing = false, exportedBitmap = bitmap) }
        }
    }

    fun clearExportedBitmap() {
        _uiState.update { it.copy(exportedBitmap = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, statusMessage = null) }
    }
}
