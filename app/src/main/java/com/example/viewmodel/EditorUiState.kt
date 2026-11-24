package com.example.viewmodel

import android.graphics.Bitmap
import com.example.model.CanvasDimensions
import com.example.model.CropRect
import com.example.model.CutoutShape
import com.example.model.CutoutSubTool
import com.example.model.CutoutTarget
import com.example.model.EditorTool
import com.example.model.Layer
import com.example.model.ProjectItem

data class EditorUiState(
    val projectId: String = "proj_default",
    val projectName: String = "Untitled Project",
    val canvasDimensions: CanvasDimensions = CanvasDimensions(),
    val canvasDpi: Int = 72,
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeTool: EditorTool = EditorTool.SELECT,
    val cropRect: CropRect = CropRect(),
    val cropAspectRatio: Float? = null,
    val cropMaintainAspectRatio: Boolean = false,
    val cropMatchCanvas: Boolean = false,
    val drawColor: Long = 0xFF22C55EL,
    val drawStrokeWidth: Float = 10f,
    val drawAlpha: Float = 1f,
    val cutoutSubTool: CutoutSubTool = CutoutSubTool.SELECT,
    val cutoutSelectedTarget: CutoutTarget? = null,
    val cutoutBrushSize: Float = 42f,
    val cutoutBrushHardness: Float = 0.75f,
    val cutoutOutlineKeepInside: Boolean = true,
    val cutoutShapeInvert: Boolean = false,
    val cutoutSelectedShape: CutoutShape? = null,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isProcessing: Boolean = false,
    val isProcessingBgRemove: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val exportedBitmap: Bitmap? = null,
    val recentProjects: List<ProjectItem> = emptyList(),
    val hasActiveProject: Boolean = false
) {
    val selectedLayer: Layer?
        get() = layers.find { it.id == selectedLayerId }
}
