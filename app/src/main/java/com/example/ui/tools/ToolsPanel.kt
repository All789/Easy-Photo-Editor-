package com.example.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EditorTool
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ToolsPanel(
    activeTool: EditorTool,
    onSelectTool: (EditorTool) -> Unit,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhotoshopTab(
                icon = Icons.Default.TouchApp,
                label = "Adjust",
                isSelected = activeTool == EditorTool.SELECT,
                onClick = { onSelectTool(EditorTool.SELECT) },
                testTag = "tab_adjust"
            )
            PhotoshopTab(
                icon = Icons.Default.Crop,
                label = "Crop",
                isSelected = activeTool == EditorTool.CROP,
                onClick = { onSelectTool(EditorTool.CROP) },
                testTag = "tab_crop"
            )
            PhotoshopTab(
                icon = Icons.Default.OpenWith,
                label = "Transform",
                isSelected = activeTool == EditorTool.TRANSFORM,
                onClick = { onSelectTool(EditorTool.TRANSFORM) },
                testTag = "tab_transform"
            )
            PhotoshopTab(
                icon = Icons.Default.FilterVintage,
                label = "Filters",
                isSelected = activeTool == EditorTool.FILTERS,
                onClick = { onSelectTool(EditorTool.FILTERS) },
                testTag = "tab_filters"
            )
            PhotoshopTab(
                icon = Icons.Default.AutoFixHigh,
                label = "Cutout",
                isSelected = activeTool == EditorTool.BG_REMOVE,
                onClick = { onSelectTool(EditorTool.BG_REMOVE) },
                testTag = "tab_bg_remove"
            )
            PhotoshopTab(
                icon = Icons.Default.TextFields,
                label = "Text",
                isSelected = activeTool == EditorTool.TEXT,
                onClick = { onSelectTool(EditorTool.TEXT) },
                testTag = "tab_text"
            )
            PhotoshopTab(
                icon = Icons.Default.Draw,
                label = "Write",
                isSelected = activeTool == EditorTool.DRAW,
                onClick = { onSelectTool(EditorTool.DRAW) },
                testTag = "tab_write"
            )
            PhotoshopTab(
                icon = Icons.Default.EmojiEmotions,
                label = "Stickers",
                isSelected = activeTool == EditorTool.STICKERS,
                onClick = { onSelectTool(EditorTool.STICKERS) },
                testTag = "tab_stickers"
            )
            PhotoshopTab(
                icon = Icons.Default.Layers,
                label = "Layers",
                isSelected = activeTool == EditorTool.LAYERS,
                onClick = { onSelectTool(EditorTool.LAYERS) },
                testTag = "tab_layers"
            )
            PhotoshopTab(
                icon = Icons.Default.AddPhotoAlternate,
                label = "Add Photo",
                isSelected = false,
                onClick = onAddPhotoClick,
                testTag = "tab_add_photo"
            )
            PhotoshopTab(
                icon = Icons.Default.AspectRatio,
                label = "Canvas",
                isSelected = activeTool == EditorTool.CANVAS_SETTINGS,
                onClick = { onSelectTool(EditorTool.CANVAS_SETTINGS) },
                testTag = "tab_canvas"
            )
            PhotoshopTab(
                icon = Icons.Default.Download,
                label = "Export",
                isSelected = activeTool == EditorTool.EXPORT,
                onClick = { onSelectTool(EditorTool.EXPORT) },
                testTag = "tab_export"
            )
        }
    }
}

@Composable
fun PhotoshopTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bgColor = if (isSelected) PrimaryBlue.copy(alpha = 0.22f) else Color.Transparent
    val tint = if (isSelected) SecondaryTeal else TextSecondary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
