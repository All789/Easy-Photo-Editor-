package com.example.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.model.LayerType
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun LayersPanel(
    layers: List<Layer>,
    selectedLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveLayerUp: (String) -> Unit,
    onMoveLayerDown: (String) -> Unit,
    onUpdateOpacity: (String, Float) -> Unit,
    onRenameLayer: (String, String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddTextClick: () -> Unit,
    onFitToCanvasClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selected = layers.find { it.id == selectedLayerId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddPhotoClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Photo", fontSize = 12.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = onAddTextClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Text", fontSize = 12.sp, color = TextPrimary)
            }
        }

        if (selected != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Layer Opacity", color = TextPrimary, fontSize = 11.sp)
                    Text("${(selected.opacity * 100f).roundToInt()}%", color = SecondaryTeal, fontSize = 11.sp)
                }
                Slider(
                    value = selected.opacity,
                    onValueChange = { onUpdateOpacity(selected.id, it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(layers.asReversed(), key = { it.id }) { layer ->
                val isSelected = layer.id == selectedLayerId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryBlue.copy(alpha = 0.22f) else DarkSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) PrimaryBlue else BorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectLayer(layer.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("layer_row_${layer.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            tint = if (layer.isVisible) SecondaryTeal else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = layer.name,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (layer.type == LayerType.TEXT) {
                                "Text • ${layer.width.roundToInt()}×${layer.height.roundToInt()}"
                            } else {
                                "Image • ${layer.width.roundToInt()}×${layer.height.roundToInt()}"
                            },
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    IconButton(onClick = { onToggleLock(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Layer",
                            tint = if (layer.isLocked) Color(0xFFF59E0B) else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(onClick = { onMoveLayerUp(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onMoveLayerDown(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDuplicateLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = { onDeleteLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}
