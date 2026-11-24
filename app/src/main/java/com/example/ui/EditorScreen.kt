package com.example.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DrawStroke
import com.example.model.EditorTool
import com.example.model.LayerType
import com.example.processing.ImageProcessor
import com.example.ui.canvas.EditorCanvas
import com.example.ui.crop.ProfessionalCropOverlay
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.tools.BgRemoveToolPanel
import com.example.ui.tools.CanvasSettingsPanel
import com.example.ui.tools.CropToolPanel
import com.example.ui.tools.DrawToolPanel
import com.example.ui.tools.ExportDialogPanel
import com.example.ui.tools.FilterSettingsPanel
import com.example.ui.tools.LayersPanel
import com.example.ui.tools.StickersToolPanel
import com.example.ui.tools.TextToolPanel
import com.example.ui.tools.ToolsPanel
import com.example.ui.tools.TransformToolPanel
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.addPhotoBottomBannerWithName
import com.example.viewmodel.addStickerLayer
import com.example.viewmodel.addTickMarkStampLayer
import com.example.viewmodel.applyCropAndAdaptCanvas
import com.example.viewmodel.applyCutoutBrushOrErase
import com.example.viewmodel.applyOutlineCutout
import com.example.viewmodel.applySemanticCutout
import com.example.viewmodel.applyShapeCutout
import com.example.viewmodel.commitDrawStrokesToLayer
import com.example.viewmodel.importPhotoToCanvas
import com.example.viewmodel.saveToPhoneGallery
import com.example.viewmodel.sharePhoto
import com.example.viewmodel.updateCutoutSettings
import com.example.viewmodel.updateDrawSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    initialHasCreatedPage: Boolean,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasCreatedPage by remember(uiState.projectId, initialHasCreatedPage) {
        mutableStateOf(initialHasCreatedPage)
    }
    var isHandMode by remember { mutableStateOf(false) }
    var transformMaintainAspectRatio by remember { mutableStateOf(true) }
    var lastExportFormat by remember { mutableStateOf("PNG") }
    var lastExportQuality by remember { mutableStateOf(95) }
    var activeDrawStrokes by remember { mutableStateOf<List<DrawStroke>>(emptyList()) }
    var showSaveQuestionDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember(uiState.projectId) { mutableStateOf(false) }

    LaunchedEffect(uiState.canUndo) {
        if (uiState.canUndo) {
            hasUnsavedChanges = true
        }
    }

    fun triggerSaveQuestionFlow(fmt: String = lastExportFormat, qual: Int = lastExportQuality) {
        lastExportFormat = fmt
        lastExportQuality = qual
        showSaveQuestionDialog = true
        viewModel.exportFinalImage(fmt, qual)
    }

    fun handleBackAction() {
        when {
            uiState.activeTool == EditorTool.DRAW -> {
                activeDrawStrokes = emptyList()
                viewModel.setActiveTool(EditorTool.SELECT)
            }
            uiState.activeTool != EditorTool.SELECT -> {
                viewModel.setActiveTool(EditorTool.SELECT)
            }
            hasUnsavedChanges && uiState.layers.isNotEmpty() -> {
                showExitConfirmDialog = true
            }
            else -> {
                onNavigateHome()
            }
        }
    }

    val addPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ImageProcessor.decodeSampledBitmap(context, uri, maxDimension = 2048)
                }
                if (bmp != null) {
                    hasUnsavedChanges = true
                    viewModel.importPhotoToCanvas(bmp, "Imported Photo", fillCanvas = false)
                }
            }
        }
    }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    BackHandler {
        handleBackAction()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                navigationIcon = {
                    IconButton(
                        onClick = { handleBackAction() },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Column {
                        Text(
                            text = uiState.projectName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${uiState.canvasDimensions.width} × ${uiState.canvasDimensions.height} px • ${uiState.canvasDpi} DPI",
                            color = SecondaryTeal,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    when (uiState.activeTool) {
                        EditorTool.CROP -> {
                            IconButton(
                                onClick = { viewModel.setActiveTool(EditorTool.SELECT) },
                                modifier = Modifier.testTag("cancel_crop_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Crop", tint = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    hasUnsavedChanges = true
                                    viewModel.applyCropAndAdaptCanvas(
                                        isDirectPhotoMode = !hasCreatedPage,
                                        fitToProjectCanvas = true
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("apply_crop_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EditorTool.DRAW -> {
                            IconButton(
                                onClick = {
                                    activeDrawStrokes = emptyList()
                                    viewModel.setActiveTool(EditorTool.SELECT)
                                },
                                modifier = Modifier.testTag("cancel_draw_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Drawing", tint = TextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (activeDrawStrokes.isNotEmpty()) {
                                        hasUnsavedChanges = true
                                    }
                                    viewModel.commitDrawStrokesToLayer(activeDrawStrokes)
                                    activeDrawStrokes = emptyList()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("apply_draw_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        EditorTool.SELECT, EditorTool.HAND -> {
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = uiState.canUndo,
                                modifier = Modifier.testTag("undo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (uiState.canUndo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = uiState.canRedo,
                                modifier = Modifier.testTag("redo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (uiState.canRedo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { isHandMode = !isHandMode },
                                modifier = Modifier.testTag("hand_mode_button")
                            ) {
                                Icon(
                                    Icons.Default.PanTool,
                                    contentDescription = "Pan Canvas",
                                    tint = if (isHandMode) SecondaryTeal else TextSecondary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.resetView() },
                                modifier = Modifier.testTag("reset_zoom_button")
                            ) {
                                Icon(Icons.Default.ZoomOutMap, contentDescription = "Reset View", tint = TextSecondary)
                            }
                            Button(
                                onClick = { triggerSaveQuestionFlow() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("top_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Save Photo", tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            // Any active editing tool shows Undo/Redo + unmistakable green ✓ Done button
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = uiState.canUndo,
                                modifier = Modifier.testTag("undo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (uiState.canUndo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = uiState.canRedo,
                                modifier = Modifier.testTag("redo_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (uiState.canRedo) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.setActiveTool(EditorTool.SELECT)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("tool_done_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .navigationBarsPadding()
            ) {
                val showDrawer = uiState.activeTool != EditorTool.SELECT && uiState.activeTool != EditorTool.HAND
                AnimatedVisibility(visible = showDrawer) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .heightIn(max = 310.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (uiState.activeTool) {
                            EditorTool.CROP -> {
                                val cropTarget = uiState.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
                                    ?: uiState.layers.lastOrNull { it.type == LayerType.IMAGE }
                                val bmp = cropTarget?.bitmap
                                if (bmp != null) {
                                    CropToolPanel(
                                        sourceWidthPx = bmp.width,
                                        sourceHeightPx = bmp.height,
                                        cropRect = uiState.cropRect,
                                        canvasDimensions = uiState.canvasDimensions,
                                        canvasDpi = uiState.canvasDpi,
                                        selectedAspectRatio = uiState.cropAspectRatio,
                                        maintainAspectRatio = uiState.cropMaintainAspectRatio,
                                        cropMatchCanvas = uiState.cropMatchCanvas,
                                        isDirectPhotoMode = !hasCreatedPage,
                                        onSelectAspectRatio = { ratio ->
                                            viewModel.setCropAspectRatio(ratio, matchCanvas = false)
                                        },
                                        onMatchCanvasCrop = {
                                            viewModel.setCropToProjectCanvas()
                                        },
                                        onToggleMaintainAspectRatio = { locked ->
                                            viewModel.setCropMaintainAspectRatio(locked)
                                        },
                                        onManualCropPixels = { w, h ->
                                            viewModel.setCropManualPixelDimensions(w, h)
                                        },
                                        onResetCrop = {
                                            viewModel.setCropAspectRatio(null, matchCanvas = false)
                                        }
                                    )
                                } else {
                                    Text(
                                        "Import an image layer first to crop",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                            EditorTool.TRANSFORM -> {
                                TransformToolPanel(
                                    selectedLayer = uiState.selectedLayer,
                                    canvasDimensions = uiState.canvasDimensions,
                                    maintainAspectRatio = transformMaintainAspectRatio,
                                    onToggleMaintainAspectRatio = { transformMaintainAspectRatio = it },
                                    onUpdateScale = { id, sx, sy -> viewModel.updateLayerScale(id, sx, sy) },
                                    onUpdateRotation = { id, rot -> viewModel.updateLayerRotation(id, rot) },
                                    onUpdateTransform = { id, x, y, w, h, rot ->
                                        viewModel.updateLayerTransform(id, x, y, w, h, rot)
                                    },
                                    onFlipH = { id, currentlyFlipped ->
                                        val layer = uiState.layers.find { it.id == id } ?: return@TransformToolPanel
                                        val nextSX = if (currentlyFlipped) kotlin.math.abs(layer.scaleX) else -kotlin.math.abs(layer.scaleX)
                                        viewModel.updateLayerScale(id, nextSX, layer.scaleY)
                                    },
                                    onFlipV = { id, currentlyFlipped ->
                                        val layer = uiState.layers.find { it.id == id } ?: return@TransformToolPanel
                                        val nextSY = if (currentlyFlipped) kotlin.math.abs(layer.scaleY) else -kotlin.math.abs(layer.scaleY)
                                        viewModel.updateLayerScale(id, layer.scaleX, nextSY)
                                    },
                                    canvasDpi = uiState.canvasDpi
                                )
                            }
                            EditorTool.FILTERS -> {
                                FilterSettingsPanel(
                                    selectedLayer = uiState.selectedLayer,
                                    onUpdateFilters = { filters ->
                                        uiState.selectedLayerId?.let { viewModel.applyFilterSettings(it, filters) }
                                    },
                                    onApplyPreset = { preset ->
                                        uiState.selectedLayerId?.let { viewModel.applyFilterPreset(it, preset) }
                                    }
                                )
                            }
                            EditorTool.BG_REMOVE -> {
                                BgRemoveToolPanel(
                                    selectedLayer = uiState.selectedLayer,
                                    allLayers = uiState.layers,
                                    isProcessing = uiState.isProcessingBgRemove,
                                    cutoutSubTool = uiState.cutoutSubTool,
                                    cutoutSelectedTarget = uiState.cutoutSelectedTarget,
                                    cutoutBrushSize = uiState.cutoutBrushSize,
                                    cutoutBrushHardness = uiState.cutoutBrushHardness,
                                    cutoutOutlineKeepInside = uiState.cutoutOutlineKeepInside,
                                    cutoutShapeInvert = uiState.cutoutShapeInvert,
                                    cutoutSelectedShape = uiState.cutoutSelectedShape,
                                    onSelectSubTool = { subTool ->
                                        viewModel.updateCutoutSettings(subTool = subTool)
                                    },
                                    onSelectSemanticTarget = { target, smooth ->
                                        hasUnsavedChanges = true
                                        viewModel.applySemanticCutout(target, smooth)
                                    },
                                    onUpdateBrushSettings = { size, hard ->
                                        viewModel.updateCutoutSettings(brushSize = size, brushHardness = hard)
                                    },
                                    onToggleOutlineMode = { keepInside ->
                                        viewModel.updateCutoutSettings(outlineKeepInside = keepInside)
                                    },
                                    onApplyShape = { shape, invert ->
                                        hasUnsavedChanges = true
                                        viewModel.applyShapeCutout(shape, invert)
                                    },
                                    onToggleShapeInvert = { invert ->
                                        viewModel.updateCutoutSettings(shapeInvert = invert)
                                    },
                                    onSelectLayer = { viewModel.selectLayer(it) },
                                    onRemoveBackgroundAi = { viewModel.removeBackgroundAi(it) },
                                    onRemoveBackgroundColor = { color, tol, feather ->
                                        hasUnsavedChanges = true
                                        viewModel.removeBackgroundColor(color, tol, feather)
                                    },
                                    onApplyBackdrop = { type, solid, gradS, gradE, blur ->
                                        hasUnsavedChanges = true
                                        viewModel.applyBackdrop(type, solid, gradS, gradE, blur)
                                    },
                                    onRestoreOriginal = {
                                        val targetId = uiState.selectedLayerId
                                            ?: uiState.layers.lastOrNull { it.type == LayerType.IMAGE }?.id
                                        targetId?.let { viewModel.restoreOriginalLayerBitmap(it) }
                                    },
                                    onDuplicateAsCutout = {
                                        val targetId = uiState.selectedLayerId
                                            ?: uiState.layers.lastOrNull { it.type == LayerType.IMAGE }?.id
                                        targetId?.let {
                                            hasUnsavedChanges = true
                                            viewModel.duplicateLayerAsCutout(it)
                                        }
                                    },
                                    onClosePanel = { viewModel.setActiveTool(EditorTool.SELECT) }
                                )
                            }
                            EditorTool.LAYERS -> {
                                LayersPanel(
                                    layers = uiState.layers,
                                    selectedLayerId = uiState.selectedLayerId,
                                    onSelectLayer = { viewModel.selectLayer(it) },
                                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                                    onToggleLock = { viewModel.toggleLayerLock(it) },
                                    onDuplicateLayer = { viewModel.duplicateLayer(it) },
                                    onDeleteLayer = { viewModel.deleteLayer(it) },
                                    onMoveLayerUp = { viewModel.moveLayerUp(it) },
                                    onMoveLayerDown = { viewModel.moveLayerDown(it) },
                                    onUpdateOpacity = { id, op -> viewModel.updateLayerOpacity(id, op) },
                                    onRenameLayer = { id, n -> viewModel.renameLayer(id, n) },
                                    onAddPhotoClick = {
                                        addPhotoLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onAddTextClick = { viewModel.setActiveTool(EditorTool.TEXT) }
                                )
                            }
                            EditorTool.TEXT -> {
                                TextToolPanel(
                                    selectedLayer = uiState.selectedLayer,
                                    canvasDimensions = uiState.canvasDimensions,
                                    onAddTextLayer = { text, color, size, font ->
                                        viewModel.addTextLayer(text, color, size, font)
                                    },
                                    onUpdateText = { text, color, size, font, bold, italic, align ->
                                        uiState.selectedLayerId?.let { id ->
                                            viewModel.updateTextProperties(
                                                id, text, color, size, font, bold, italic, align
                                            )
                                        }
                                    },
                                    onUpdatePosition = { x, y ->
                                        uiState.selectedLayerId?.let { viewModel.updateLayerPosition(it, x, y) }
                                    },
                                    onUpdateSpacing = { spacing, opacity ->
                                        uiState.selectedLayerId?.let {
                                            viewModel.updateTextSpacing(it, spacing, opacity)
                                        }
                                    },
                                    onAddPhotoBottomBanner = { nameText ->
                                        viewModel.addPhotoBottomBannerWithName(nameText)
                                    },
                                    onAddTickMark = { symbol, color ->
                                        viewModel.addTickMarkStampLayer(symbol, color)
                                    },
                                    onUpdateTextEffects = { hasShadow, hasOutline, hasBg, bgColor ->
                                        val layer = uiState.selectedLayer
                                        if (layer != null && layer.type == LayerType.TEXT) {
                                            viewModel.updateTextEffects(
                                                layerId = layer.id,
                                                hasShadow = hasShadow,
                                                shadowColor = layer.shadowColor,
                                                shadowRadius = layer.shadowRadius,
                                                shadowOffsetX = layer.shadowOffsetX,
                                                shadowOffsetY = layer.shadowOffsetY,
                                                hasOutline = hasOutline,
                                                outlineColor = layer.outlineColor,
                                                outlineWidth = layer.outlineWidth,
                                                hasBackground = hasBg,
                                                backgroundColor = bgColor,
                                                backgroundCornerRadius = layer.backgroundCornerRadius,
                                                textAlpha = layer.textAlpha
                                            )
                                        }
                                    }
                                )
                            }
                            EditorTool.DRAW -> {
                                DrawToolPanel(
                                    drawColor = uiState.drawColor,
                                    strokeWidth = uiState.drawStrokeWidth,
                                    drawAlpha = uiState.drawAlpha,
                                    strokeCount = activeDrawStrokes.size,
                                    onColorChange = { viewModel.updateDrawSettings(color = it) },
                                    onStrokeWidthChange = { viewModel.updateDrawSettings(strokeWidth = it) },
                                    onAlphaChange = { viewModel.updateDrawSettings(alpha = it) },
                                    onUndoStroke = {
                                        if (activeDrawStrokes.isNotEmpty()) {
                                            activeDrawStrokes = activeDrawStrokes.dropLast(1)
                                        }
                                    },
                                    onClearStrokes = { activeDrawStrokes = emptyList() },
                                    onAddTickMark = { symbol, color ->
                                        viewModel.addTickMarkStampLayer(symbol, color)
                                    }
                                )
                            }
                            EditorTool.STICKERS -> {
                                StickersToolPanel(
                                    onAddSticker = { spec ->
                                        hasUnsavedChanges = true
                                        viewModel.addStickerLayer(spec)
                                    }
                                )
                            }
                            EditorTool.CANVAS_SETTINGS -> {
                                CanvasSettingsPanel(
                                    dimensions = uiState.canvasDimensions,
                                    onUpdateCanvas = { w, h, trans, bg ->
                                        hasCreatedPage = true
                                        hasUnsavedChanges = true
                                        viewModel.updateCanvasSettings(w, h, trans, bg)
                                    }
                                )
                            }
                            EditorTool.EXPORT -> {
                                ExportDialogPanel(
                                    dimensions = uiState.canvasDimensions,
                                    layerCount = uiState.layers.size,
                                    isSaving = uiState.isSaving,
                                    onSaveProject = {
                                        hasUnsavedChanges = false
                                        viewModel.saveProject()
                                    },
                                    onExportImage = { fmt, qual ->
                                        triggerSaveQuestionFlow(fmt, qual)
                                    }
                                )
                            }
                            else -> Unit
                        }
                    }
                }

                ToolsPanel(
                    activeTool = uiState.activeTool,
                    onSelectTool = { tool ->
                        if (tool == EditorTool.EXPORT) {
                            viewModel.setActiveTool(tool)
                        } else if (tool == uiState.activeTool && tool != EditorTool.SELECT) {
                            viewModel.setActiveTool(EditorTool.SELECT)
                        } else {
                            viewModel.setActiveTool(tool)
                        }
                    },
                    onAddPhotoClick = {
                        addPhotoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.activeTool == EditorTool.CROP) {
                val cropTarget = uiState.selectedLayer?.takeIf { it.type == LayerType.IMAGE }
                    ?: uiState.layers.lastOrNull { it.type == LayerType.IMAGE }
                val bmp = cropTarget?.bitmap
                if (bmp != null) {
                    ProfessionalCropOverlay(
                        bitmap = bmp,
                        cropRect = uiState.cropRect,
                        aspectRatio = uiState.cropAspectRatio,
                        maintainAspectRatio = uiState.cropMaintainAspectRatio,
                        canvasDimensions = uiState.canvasDimensions,
                        onCropChange = { viewModel.updateCropRect(it) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Import a photo to use Crop tool", color = TextSecondary)
                    }
                }
            } else {
                EditorCanvas(
                    dimensions = uiState.canvasDimensions,
                    layers = uiState.layers,
                    selectedLayerId = uiState.selectedLayerId,
                    activeTool = uiState.activeTool,
                    zoom = uiState.zoom,
                    panX = uiState.panX,
                    panY = uiState.panY,
                    isHandMode = isHandMode || uiState.activeTool == EditorTool.HAND,
                    maintainAspectRatio = transformMaintainAspectRatio,
                    onSelectLayer = { viewModel.selectLayer(it) },
                    onUpdateLayerTransform = { id, x, y, w, h, rot ->
                        hasUnsavedChanges = true
                        viewModel.updateLayerTransform(id, x, y, w, h, rot)
                    },
                    onUpdateLayerRotation = { id, rot ->
                        hasUnsavedChanges = true
                        viewModel.updateLayerRotation(id, rot)
                    },
                    onUpdatePan = { z, px, py -> viewModel.updateZoomAndPan(z, px, py) },
                    onCanvasDoubleTapText = { layerId ->
                        viewModel.selectLayer(layerId)
                        viewModel.setActiveTool(EditorTool.TEXT)
                    },
                    drawStrokes = activeDrawStrokes,
                    drawColor = uiState.drawColor,
                    drawStrokeWidth = uiState.drawStrokeWidth,
                    drawAlpha = uiState.drawAlpha,
                    onAddDrawStroke = { stroke ->
                        hasUnsavedChanges = true
                        activeDrawStrokes = activeDrawStrokes + stroke
                    },
                    cutoutSubTool = uiState.cutoutSubTool,
                    cutoutBrushSize = uiState.cutoutBrushSize,
                    cutoutOutlineKeepInside = uiState.cutoutOutlineKeepInside,
                    onCutoutOutlineComplete = { normPoly ->
                        hasUnsavedChanges = true
                        viewModel.applyOutlineCutout(normPoly, uiState.cutoutOutlineKeepInside)
                    },
                    onCutoutBrushComplete = { normPts, isErase ->
                        hasUnsavedChanges = true
                        viewModel.applyCutoutBrushOrErase(
                            normalizedPoints = normPts,
                            brushSizePx = uiState.cutoutBrushSize,
                            hardness = uiState.cutoutBrushHardness,
                            isErase = isErase
                        )
                    }
                )
            }
        }
    }

    if (showSaveQuestionDialog || uiState.exportedBitmap != null) {
        SaveQuestionDialog(
            bitmap = uiState.exportedBitmap,
            widthPx = uiState.canvasDimensions.width,
            heightPx = uiState.canvasDimensions.height,
            dpi = uiState.canvasDpi,
            format = lastExportFormat,
            quality = lastExportQuality,
            onFormatChange = { lastExportFormat = it },
            onQualityChange = { lastExportQuality = it },
            onDismiss = {
                showSaveQuestionDialog = false
                viewModel.clearExportedBitmap()
            },
            onSaveGallery = {
                scope.launch {
                    saveToPhoneGallery(context, viewModel, lastExportFormat, lastExportQuality)
                    hasUnsavedChanges = false
                    showSaveQuestionDialog = false
                    viewModel.clearExportedBitmap()
                }
            },
            onSaveProject = {
                viewModel.saveProject()
                hasUnsavedChanges = false
                showSaveQuestionDialog = false
                viewModel.clearExportedBitmap()
            },
            onShare = {
                scope.launch {
                    sharePhoto(context, viewModel, lastExportFormat, lastExportQuality)
                }
            }
        )
    }

    if (showExitConfirmDialog) {
        ExitSaveQuestionDialog(
            onDismiss = { showExitConfirmDialog = false },
            onSavePhoto = {
                showExitConfirmDialog = false
                triggerSaveQuestionFlow()
            },
            onDiscardAndExit = {
                showExitConfirmDialog = false
                onNavigateHome()
            }
        )
    }
}

@Composable
private fun SaveQuestionDialog(
    bitmap: Bitmap?,
    widthPx: Int,
    heightPx: Int,
    dpi: Int,
    format: String,
    quality: Int,
    onFormatChange: (String) -> Unit,
    onQualityChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSaveGallery: () -> Unit,
    onSaveProject: () -> Unit,
    onShare: () -> Unit
) {
    var qualitySlider by remember(quality) { mutableFloatStateOf(quality.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Save Photo?",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ফটোটি গ্যালারিতে সেভ করতে চান?",
                            color = SecondaryTeal,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Save Photo Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        CircularProgressIndicator(color = SecondaryTeal, modifier = Modifier.size(32.dp))
                    }
                }

                Text(
                    text = "Resolution: $widthPx × $heightPx px  •  $dpi DPI",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Format:", color = TextPrimary, fontSize = 12.sp)
                    FilterChip(
                        selected = format == "PNG",
                        onClick = { onFormatChange("PNG") },
                        label = { Text("PNG (HD)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                    FilterChip(
                        selected = format == "JPEG",
                        onClick = { onFormatChange("JPEG") },
                        label = { Text("JPEG", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }

                if (format == "JPEG") {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Quality", color = TextSecondary, fontSize = 11.sp)
                            Text("${qualitySlider.roundToInt()}%", color = SecondaryTeal, fontSize = 11.sp)
                        }
                        Slider(
                            value = qualitySlider,
                            onValueChange = {
                                qualitySlider = it
                                onQualityChange(it.roundToInt())
                            },
                            valueRange = 50f..100f,
                            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                        )
                    }
                }

                Button(
                    onClick = onSaveGallery,
                    enabled = bitmap != null,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_save_gallery_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yes, Save to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveProject,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_save_project_btn")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Draft", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onShare,
                        enabled = bitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("cancel_save_dialog_btn")
                ) {
                    Text("Keep Editing", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExitSaveQuestionDialog(
    onDismiss: () -> Unit,
    onSavePhoto: () -> Unit,
    onDiscardAndExit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Save before closing?",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "বের হওয়ার আগে ফটোটি কি সেভ করতে চান?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDiscardAndExit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("exit_without_saving_btn")
                    ) {
                        Text("Exit", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onSavePhoto,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("exit_save_photo_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
