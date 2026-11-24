package com.example.ui.tools

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CutoutShape
import com.example.model.CutoutSubTool
import com.example.model.CutoutTarget
import com.example.model.Layer
import com.example.model.LayerType
import com.example.processing.BackgroundRemover
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

private val CutoutTealFill = Color(0xFF0E5A52)
private val CutoutTealBorder = Color(0xFF2DD4BF)
private val CutoutPillBg = Color(0xFF242426)

@Composable
fun BgRemoveToolPanel(
    selectedLayer: Layer?,
    allLayers: List<Layer>,
    isProcessing: Boolean,
    cutoutSubTool: CutoutSubTool,
    cutoutSelectedTarget: CutoutTarget?,
    cutoutBrushSize: Float,
    cutoutBrushHardness: Float,
    cutoutOutlineKeepInside: Boolean,
    cutoutShapeInvert: Boolean,
    cutoutSelectedShape: CutoutShape?,
    onSelectSubTool: (CutoutSubTool) -> Unit,
    onSelectSemanticTarget: (CutoutTarget, Float) -> Unit,
    onUpdateBrushSettings: (Float, Float) -> Unit,
    onToggleOutlineMode: (Boolean) -> Unit,
    onApplyShape: (CutoutShape, Boolean) -> Unit,
    onToggleShapeInvert: (Boolean) -> Unit,
    onSelectLayer: (String) -> Unit,
    onRemoveBackgroundAi: (Float) -> Unit,
    onRemoveBackgroundColor: (Int, Float, Float) -> Unit,
    onApplyBackdrop: (BackgroundRemover.BackdropType, Long, Long, Long, Float) -> Unit,
    onRestoreOriginal: () -> Unit,
    onDuplicateAsCutout: () -> Unit,
    onClosePanel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var smoothing by remember { mutableFloatStateOf(0.5f) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    var colorTolerance by remember { mutableFloatStateOf(0.22f) }
    var targetColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var bokehRadius by remember { mutableFloatStateOf(15f) }

    val imageLayers = allLayers.filter { it.type == LayerType.IMAGE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141416))
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Multi-layer selector + status/quick utilities
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Cutout",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (imageLayers.size > 1) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        imageLayers.forEach { layer ->
                            FilterChip(
                                selected = selectedLayer?.id == layer.id,
                                onClick = { onSelectLayer(layer.id) },
                                label = { Text(layer.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CutoutTealFill,
                                    selectedLabelColor = Color.White,
                                    containerColor = CutoutPillBg,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = CutoutTealBorder,
                        strokeWidth = 2.dp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CutoutPillBg)
                        .clickable { onRestoreOriginal() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_cutout_restore")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Restore", color = TextPrimary, fontSize = 10.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CutoutPillBg)
                        .clickable { onDuplicateAsCutout() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_cutout_duplicate")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Copy", color = TextPrimary, fontSize = 10.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (showAdvancedOptions) CutoutTealFill else CutoutPillBg)
                        .clickable { showAdvancedOptions = !showAdvancedOptions }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_cutout_backdrop_opts")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = "More Options", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("BG/Blur", color = TextPrimary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Contextual strip matching selected Cutout sub-tool (Select, Outline, Brush, Erase, Shape)
        when (cutoutSubTool) {
            CutoutSubTool.SELECT -> {
                // Horizontal pill strip: Person, Face, Clothes, Sky, Head, Hair, Background
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CutoutTarget.entries.forEach { target ->
                        val isSelected = cutoutSelectedTarget == target
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isSelected) CutoutTealFill else CutoutPillBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.2.dp, CutoutTealBorder, RoundedCornerShape(22.dp))
                                    } else Modifier
                                )
                                .clickable(enabled = !isProcessing) {
                                    onSelectSemanticTarget(target, smoothing)
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                                .testTag("cutout_target_${target.name}")
                        ) {
                            Text(
                                text = target.label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            CutoutSubTool.OUTLINE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Draw outline loop on photo",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = cutoutOutlineKeepInside,
                            onClick = { onToggleOutlineMode(true) },
                            label = { Text("Keep Inside", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CutoutTealFill,
                                selectedLabelColor = Color.White,
                                containerColor = CutoutPillBg,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("outline_keep_inside")
                        )
                        FilterChip(
                            selected = !cutoutOutlineKeepInside,
                            onClick = { onToggleOutlineMode(false) },
                            label = { Text("Cut Inside", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CutoutTealFill,
                                selectedLabelColor = Color.White,
                                containerColor = CutoutPillBg,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("outline_cut_inside")
                        )
                    }
                }
            }
            CutoutSubTool.BRUSH, CutoutSubTool.ERASE -> {
                val isErase = cutoutSubTool == CutoutSubTool.ERASE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (isErase) "Eraser Size" else "Brush Size",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                                Text("${cutoutBrushSize.roundToInt()} px", color = CutoutTealBorder, fontSize = 11.sp)
                            }
                            Slider(
                                value = cutoutBrushSize,
                                onValueChange = { onUpdateBrushSettings(it, cutoutBrushHardness) },
                                valueRange = 8f..140f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CutoutTealBorder,
                                    activeTrackColor = CutoutTealBorder
                                ),
                                modifier = Modifier.testTag("cutout_brush_size_slider")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hardness", color = TextPrimary, fontSize = 11.sp)
                                Text("${(cutoutBrushHardness * 100f).roundToInt()}%", color = CutoutTealBorder, fontSize = 11.sp)
                            }
                            Slider(
                                value = cutoutBrushHardness,
                                onValueChange = { onUpdateBrushSettings(cutoutBrushSize, it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CutoutTealBorder,
                                    activeTrackColor = CutoutTealBorder
                                ),
                                modifier = Modifier.testTag("cutout_brush_hardness_slider")
                            )
                        }
                    }
                }
            }
            CutoutSubTool.SHAPE -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = cutoutShapeInvert,
                        onClick = {
                            val nextInv = !cutoutShapeInvert
                            onToggleShapeInvert(nextInv)
                            cutoutSelectedShape?.let { onApplyShape(it, nextInv) }
                        },
                        label = { Text("Invert", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CutoutTealFill,
                            selectedLabelColor = Color.White,
                            containerColor = CutoutPillBg,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("cutout_shape_invert")
                    )
                    CutoutShape.entries.forEach { shape ->
                        val isSelected = cutoutSelectedShape == shape
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) CutoutTealFill else CutoutPillBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.2.dp, CutoutTealBorder, RoundedCornerShape(20.dp))
                                    } else Modifier
                                )
                                .clickable(enabled = !isProcessing) {
                                    onApplyShape(shape, cutoutShapeInvert)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("cutout_shape_${shape.name}")
                        ) {
                            Text(
                                text = shape.label,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Optional Backdrops, Edge Smoothing, Color Cutout & Bokeh Blur drawer
        if (showAdvancedOptions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .background(DarkBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edge Smoothing: ${(smoothing * 100).roundToInt()}%", color = TextPrimary, fontSize = 11.sp)
                    Slider(
                        value = smoothing,
                        onValueChange = { smoothing = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.width(150.dp),
                        colors = SliderDefaults.colors(thumbColor = CutoutTealBorder, activeTrackColor = CutoutTealBorder)
                    )
                }

                // Color Chroma Cutout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color Cutout", color = TextPrimary, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(
                            AndroidColor.WHITE,
                            AndroidColor.BLACK,
                            AndroidColor.GREEN,
                            AndroidColor.BLUE
                        ).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .border(
                                        width = if (targetColor == col) 2.dp else 1.dp,
                                        color = if (targetColor == col) CutoutTealBorder else BorderColor,
                                        shape = CircleShape
                                    )
                                    .clickable { targetColor = col }
                            )
                        }
                        OutlinedButton(onClick = { onRemoveBackgroundColor(targetColor, colorTolerance, 0.15f) }) {
                            Text("Cut", fontSize = 10.sp)
                        }
                    }
                }

                // Backdrops & Bokeh
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(onClick = { onApplyBackdrop(BackgroundRemover.BackdropType.TRANSPARENT, 0L, 0L, 0L, 0f) }) {
                        Text("Transparent", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { onApplyBackdrop(BackgroundRemover.BackdropType.SOLID_COLOR, 0xFFFFFFFFL, 0L, 0L, 0f) }) {
                        Text("White BG", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { onApplyBackdrop(BackgroundRemover.BackdropType.SOLID_COLOR, 0xFF1E40AFL, 0L, 0L, 0f) }) {
                        Text("Blue BG", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { onApplyBackdrop(BackgroundRemover.BackdropType.GRADIENT, 0L, 0xFF1E3A8AL, 0xFF38BDF8L, 0f) }) {
                        Text("Gradient", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = { onApplyBackdrop(BackgroundRemover.BackdropType.BLUR_ORIGINAL, 0L, 0L, 0L, bokehRadius) }) {
                        Text("Bokeh Blur", fontSize = 10.sp)
                    }
                }
            }
        }

        // Bottom 5 Cutout Mode Tabs matching screenshot: Select, Outline, Brush, Erase, Shape
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CutoutModeTab(
                icon = Icons.Default.CenterFocusStrong,
                label = "Select",
                isSelected = cutoutSubTool == CutoutSubTool.SELECT,
                onClick = { onSelectSubTool(CutoutSubTool.SELECT) },
                testTag = "cutout_tab_select"
            )
            CutoutModeTab(
                icon = Icons.Default.Gesture,
                label = "Outline",
                isSelected = cutoutSubTool == CutoutSubTool.OUTLINE,
                onClick = { onSelectSubTool(CutoutSubTool.OUTLINE) },
                testTag = "cutout_tab_outline"
            )
            CutoutModeTab(
                icon = Icons.Default.Brush,
                label = "Brush",
                isSelected = cutoutSubTool == CutoutSubTool.BRUSH,
                onClick = { onSelectSubTool(CutoutSubTool.BRUSH) },
                testTag = "cutout_tab_brush"
            )
            CutoutModeTab(
                icon = Icons.Default.AutoFixNormal,
                label = "Erase",
                isSelected = cutoutSubTool == CutoutSubTool.ERASE,
                onClick = { onSelectSubTool(CutoutSubTool.ERASE) },
                testTag = "cutout_tab_erase"
            )
            CutoutModeTab(
                icon = Icons.Default.ChangeHistory,
                label = "Shape",
                isSelected = cutoutSubTool == CutoutSubTool.SHAPE,
                onClick = { onSelectSubTool(CutoutSubTool.SHAPE) },
                testTag = "cutout_tab_shape"
            )
        }
    }
}

@Composable
private fun CutoutModeTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) CutoutTealFill else Color.Transparent)
                .then(
                    if (isSelected) {
                        Modifier.border(1.5.dp, CutoutTealBorder, RoundedCornerShape(10.dp))
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFA7F3D0) else Color(0xFFE5E5EA),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFFD1D1D6),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
