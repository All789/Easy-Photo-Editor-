package com.example.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.CanvasDimensions
import com.example.model.Layer

@Composable
fun DirectTextCanvasPanel(
    selectedLayer: Layer?,
    canvasDimensions: CanvasDimensions,
    onAddNewText: (String) -> Unit,
    onAddTextLayer: (String, Long, Long, Float, String, Float) -> Unit,
    onUpdateTextColorSize: (Long, Int, Int, Float, String) -> Unit,
    onUpdateText: (String, Long, Float, String, Boolean, Boolean, String) -> Unit,
    onUpdateTextEffects: (Boolean, Long, Float, Float, Float, Boolean, Long, Float, Boolean, Long, Float, Float) -> Unit,
    onUpdateSpacing: (Float, Float) -> Unit,
    onToggleBoldItalic: (Boolean, Boolean) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onClosePanel: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextToolPanel(
        selectedLayer = selectedLayer,
        canvasDimensions = canvasDimensions,
        onAddTextLayer = { text, color, size, font ->
            onAddTextLayer(text, color, color, size, font, 1f)
        },
        onUpdateText = onUpdateText,
        onUpdatePosition = { _, _ -> },
        onUpdateSpacing = onUpdateSpacing,
        modifier = modifier
    )
}
