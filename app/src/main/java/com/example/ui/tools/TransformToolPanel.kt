package com.example.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasDimensions
import com.example.model.Layer
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TransformToolPanel(
    selectedLayer: Layer?,
    canvasDimensions: CanvasDimensions,
    maintainAspectRatio: Boolean = true,
    onToggleMaintainAspectRatio: ((Boolean) -> Unit)? = null,
    onUpdateScale: (String, Float, Float) -> Unit,
    onUpdateRotation: (String, Float) -> Unit,
    onUpdateTransform: (String, Float, Float, Float, Float, Float) -> Unit,
    onFlipH: (String, Boolean) -> Unit,
    onFlipV: (String, Boolean) -> Unit,
    canvasDpi: Int = 300,
    modifier: Modifier = Modifier
) {
    if (selectedLayer == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select a layer to resize or rotate",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    var localLockAspect by remember { mutableStateOf(maintainAspectRatio) }
    val effectiveLockAspect = if (onToggleMaintainAspectRatio != null) maintainAspectRatio else localLockAspect
    var useCmUnit by remember { mutableStateOf(false) }

    val safeDpi = canvasDpi.coerceAtLeast(72).toFloat()
    val wPx = selectedLayer.width.roundToInt().coerceAtLeast(1)
    val hPx = selectedLayer.height.roundToInt().coerceAtLeast(1)

    fun formatDim(px: Float): String {
        return if (useCmUnit) {
            String.format(Locale.US, "%.2f", (px / safeDpi) * 2.54f)
        } else {
            px.roundToInt().toString()
        }
    }

    fun parseDimToPx(text: String): Float? {
        val v = text.toFloatOrNull() ?: return null
        return if (useCmUnit) (v / 2.54f) * safeDpi else v
    }

    var widthInput by remember(selectedLayer.id, useCmUnit) { mutableStateOf(formatDim(selectedLayer.width)) }
    var heightInput by remember(selectedLayer.id, useCmUnit) { mutableStateOf(formatDim(selectedLayer.height)) }

    LaunchedEffect(wPx, hPx, useCmUnit) {
        widthInput = formatDim(selectedLayer.width)
        heightInput = formatDim(selectedLayer.height)
    }

    val canvasW = canvasDimensions.width.toFloat().coerceAtLeast(1f)
    val canvasH = canvasDimensions.height.toFloat().coerceAtLeast(1f)
    val srcW = (selectedLayer.bitmap?.width?.toFloat() ?: selectedLayer.width).coerceAtLeast(1f)
    val srcH = (selectedLayer.bitmap?.height?.toFloat() ?: selectedLayer.height).coerceAtLeast(1f)
    val maxSliderW = max(canvasW * 2f, wPx * 1.5f).coerceAtLeast(200f)
    val maxSliderH = max(canvasH * 2f, hPx * 1.5f).coerceAtLeast(200f)

    fun applyNewSize(newW: Float, newH: Float) {
        val safeW = newW.coerceIn(16f, 8000f)
        val safeH = newH.coerceIn(16f, 8000f)
        val cx = selectedLayer.x + selectedLayer.width / 2f
        val cy = selectedLayer.y + selectedLayer.height / 2f
        onUpdateTransform(
            selectedLayer.id,
            cx - safeW / 2f,
            cy - safeH / 2f,
            safeW,
            safeH,
            selectedLayer.rotation
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Canvas dimensions, Unit Switch (px / cm), and Maintain Aspect Ratio toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Canvas: ${canvasDimensions.width} × ${canvasDimensions.height} px",
                color = SecondaryTeal,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !useCmUnit,
                    onClick = { useCmUnit = false },
                    label = { Text("px", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
                FilterChip(
                    selected = useCmUnit,
                    onClick = { useCmUnit = true },
                    label = { Text("cm", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Aspect lock mode banner + toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .clickable {
                    val next = !effectiveLockAspect
                    if (onToggleMaintainAspectRatio != null) onToggleMaintainAspectRatio(next)
                    else localLockAspect = next
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("toggle_lock_aspect_ratio"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (effectiveLockAspect) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Ratio",
                    tint = if (effectiveLockAspect) SecondaryTeal else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (effectiveLockAspect) {
                        "Aspect Ratio Locked (Proportional)"
                    } else {
                        "Free Resizing (Stretch / Compress)"
                    },
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = if (effectiveLockAspect) "ON" else "OFF",
                color = if (effectiveLockAspect) SecondaryTeal else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Fit / Match Project Canvas Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val fitScale = min(canvasW / srcW, canvasH / srcH)
                    val targetW = srcW * fitScale
                    val targetH = srcH * fitScale
                    onUpdateTransform(
                        selectedLayer.id,
                        (canvasW - targetW) / 2f,
                        (canvasH - targetH) / 2f,
                        targetW,
                        targetH,
                        selectedLayer.rotation
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                modifier = Modifier.testTag("btn_fit_canvas")
            ) {
                Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Fit Canvas (No Stretch)", fontSize = 11.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = {
                    onUpdateTransform(
                        selectedLayer.id,
                        0f,
                        0f,
                        canvasW,
                        canvasH,
                        selectedLayer.rotation
                    )
                },
                modifier = Modifier.testTag("btn_match_canvas")
            ) {
                Text("Match ${canvasDimensions.width}×${canvasDimensions.height} px", fontSize = 11.sp, color = TextPrimary)
            }

            OutlinedButton(
                onClick = {
                    val coverScale = max(canvasW / srcW, canvasH / srcH)
                    val targetW = srcW * coverScale
                    val targetH = srcH * coverScale
                    onUpdateTransform(
                        selectedLayer.id,
                        (canvasW - targetW) / 2f,
                        (canvasH - targetH) / 2f,
                        targetW,
                        targetH,
                        selectedLayer.rotation
                    )
                },
                modifier = Modifier.testTag("btn_fill_canvas")
            ) {
                Text("Fill Canvas", fontSize = 11.sp, color = TextPrimary)
            }

            OutlinedButton(
                onClick = {
                    val newH = if (effectiveLockAspect) {
                        canvasW * (selectedLayer.height / selectedLayer.width.coerceAtLeast(1f))
                    } else selectedLayer.height
                    onUpdateTransform(
                        selectedLayer.id,
                        0f,
                        (canvasH - newH) / 2f,
                        canvasW,
                        newH,
                        selectedLayer.rotation
                    )
                },
                modifier = Modifier.testTag("btn_fit_width")
            ) {
                Text("Fit Width", fontSize = 11.sp, color = TextSecondary)
            }

            OutlinedButton(
                onClick = {
                    val newW = if (effectiveLockAspect) {
                        canvasH * (selectedLayer.width / selectedLayer.height.coerceAtLeast(1f))
                    } else selectedLayer.width
                    onUpdateTransform(
                        selectedLayer.id,
                        (canvasW - newW) / 2f,
                        0f,
                        newW,
                        canvasH,
                        selectedLayer.rotation
                    )
                },
                modifier = Modifier.testTag("btn_fit_height")
            ) {
                Text("Fit Height", fontSize = 11.sp, color = TextSecondary)
            }

            OutlinedButton(
                onClick = {
                    onUpdateTransform(
                        selectedLayer.id,
                        (canvasW - selectedLayer.width) / 2f,
                        (canvasH - selectedLayer.height) / 2f,
                        selectedLayer.width,
                        selectedLayer.height,
                        selectedLayer.rotation
                    )
                },
                modifier = Modifier.testTag("btn_center_layer")
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Center", fontSize = 11.sp, color = TextSecondary)
            }
        }

        // Manual Exact Width & Height Entry (px or cm)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unitSuffix = if (useCmUnit) "cm" else "px"
            OutlinedTextField(
                value = widthInput,
                onValueChange = { raw ->
                    widthInput = raw
                    val targetW = parseDimToPx(raw)
                    if (targetW != null && targetW >= 16f && effectiveLockAspect) {
                        val ratio = selectedLayer.width / selectedLayer.height.coerceAtLeast(1f)
                        heightInput = formatDim(targetW / ratio)
                    }
                },
                label = { Text("Width ($unitSuffix)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_layer_width"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )
            OutlinedTextField(
                value = heightInput,
                onValueChange = { raw ->
                    heightInput = raw
                    val targetH = parseDimToPx(raw)
                    if (targetH != null && targetH >= 16f && effectiveLockAspect) {
                        val ratio = selectedLayer.width / selectedLayer.height.coerceAtLeast(1f)
                        widthInput = formatDim(targetH * ratio)
                    }
                },
                label = { Text("Height ($unitSuffix)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_layer_height"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )
            Button(
                onClick = {
                    val targetW = parseDimToPx(widthInput) ?: selectedLayer.width
                    val targetH = parseDimToPx(heightInput) ?: selectedLayer.height
                    applyNewSize(targetW, targetH)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("btn_apply_exact_size")
            ) {
                Text("Apply", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        // Independent Width & Height Sliders + Nudge buttons
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Width: $wPx px", color = TextPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { applyNewSize((selectedLayer.width - 20f).coerceAtLeast(16f), selectedLayer.height) },
                        modifier = Modifier.testTag("btn_thinner")
                    ) { Text("-20", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { applyNewSize(selectedLayer.width + 20f, selectedLayer.height) },
                        modifier = Modifier.testTag("btn_wider")
                    ) { Text("+20", fontSize = 11.sp) }
                }
            }
            Slider(
                value = selectedLayer.width.coerceIn(16f, maxSliderW),
                onValueChange = { newW ->
                    val newH = if (effectiveLockAspect) {
                        newW / (selectedLayer.width / selectedLayer.height.coerceAtLeast(1f))
                    } else selectedLayer.height
                    applyNewSize(newW, newH)
                },
                valueRange = 16f..maxSliderW,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue),
                modifier = Modifier.testTag("slider_layer_width")
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Height: $hPx px", color = TextPrimary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { applyNewSize(selectedLayer.width, (selectedLayer.height - 20f).coerceAtLeast(16f)) },
                        modifier = Modifier.testTag("btn_shorter")
                    ) { Text("-20", fontSize = 11.sp) }
                    OutlinedButton(
                        onClick = { applyNewSize(selectedLayer.width, selectedLayer.height + 20f) },
                        modifier = Modifier.testTag("btn_taller")
                    ) { Text("+20", fontSize = 11.sp) }
                }
            }
            Slider(
                value = selectedLayer.height.coerceIn(16f, maxSliderH),
                onValueChange = { newH ->
                    val newW = if (effectiveLockAspect) {
                        newH * (selectedLayer.width / selectedLayer.height.coerceAtLeast(1f))
                    } else selectedLayer.width
                    applyNewSize(newW, newH)
                },
                valueRange = 16f..maxSliderH,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue),
                modifier = Modifier.testTag("slider_layer_height")
            )
        }

        // 360-degree Rotation + Flip Controls
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rotate (0° – 360°): ${selectedLayer.rotation.roundToInt()}°",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("badge_rotation_angle")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onUpdateRotation(selectedLayer.id, selectedLayer.rotation - 90f) }) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Rotate -90", modifier = Modifier.size(15.dp))
                    }
                    OutlinedButton(onClick = { onUpdateRotation(selectedLayer.id, 0f) }) {
                        Text("0°", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onUpdateRotation(selectedLayer.id, selectedLayer.rotation + 90f) }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Rotate +90", modifier = Modifier.size(15.dp))
                    }
                }
            }
            Slider(
                value = ((selectedLayer.rotation % 360f) + 360f) % 360f,
                onValueChange = { angle -> onUpdateRotation(selectedLayer.id, angle) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(thumbColor = SecondaryTeal, activeTrackColor = SecondaryTeal)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onFlipH(selectedLayer.id, selectedLayer.scaleX < 0f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Horizontal", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip Horizontal", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { onFlipV(selectedLayer.id, selectedLayer.scaleY < 0f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Flip, contentDescription = "Flip Vertical", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flip Vertical", fontSize = 11.sp)
                }
            }
        }
    }
}
