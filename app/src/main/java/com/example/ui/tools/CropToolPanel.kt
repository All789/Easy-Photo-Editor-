package com.example.ui.tools

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.model.CropRect
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CropToolPanel(
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    cropRect: CropRect,
    canvasDimensions: CanvasDimensions,
    canvasDpi: Int,
    selectedAspectRatio: Float?,
    maintainAspectRatio: Boolean,
    cropMatchCanvas: Boolean,
    isDirectPhotoMode: Boolean,
    onSelectAspectRatio: (Float?) -> Unit,
    onMatchCanvasCrop: () -> Unit,
    onToggleMaintainAspectRatio: (Boolean) -> Unit,
    onManualCropPixels: (Int, Int) -> Unit,
    onResetCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeSrcW = sourceWidthPx.coerceAtLeast(1)
    val safeSrcH = sourceHeightPx.coerceAtLeast(1)
    val liveSelW = (cropRect.width * safeSrcW).roundToInt().coerceIn(1, safeSrcW)
    val liveSelH = (cropRect.height * safeSrcH).roundToInt().coerceIn(1, safeSrcH)

    var widthText by remember { mutableStateOf(liveSelW.toString()) }
    var heightText by remember { mutableStateOf(liveSelH.toString()) }

    LaunchedEffect(liveSelW, liveSelH) {
        if (widthText != liveSelW.toString()) widthText = liveSelW.toString()
        if (heightText != liveSelH.toString()) heightText = liveSelH.toString()
    }

    val canvasAspect = canvasDimensions.width.toFloat().coerceAtLeast(1f) /
        canvasDimensions.height.toFloat().coerceAtLeast(1f)

    val presets: List<Pair<String, Float?>> = listOf(
        "Free" to null,
        "1:1" to 1f,
        "4:5" to (4f / 5f),
        "16:9" to (16f / 9f),
        "9:16" to (9f / 16f),
        "3:2" to (3f / 2f),
        "2:3" to (2f / 3f),
        "4:6 (Passport)" to (4f / 6f),
        "3:4" to (3f / 4f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Live canvas & selection dimensions readout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBackground, RoundedCornerShape(10.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Project Canvas",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "${canvasDimensions.width} × ${canvasDimensions.height} px • ${canvasDpi} DPI",
                    color = SecondaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Crop Selection",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = "$liveSelW × $liveSelH px",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Aspect Ratio Presets + Match Canvas chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isDirectPhotoMode) {
                FilterChip(
                    selected = cropMatchCanvas,
                    onClick = onMatchCanvasCrop,
                    leadingIcon = {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = "Match Canvas",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            "Match Canvas (${canvasDimensions.width}×${canvasDimensions.height})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryTeal.copy(alpha = 0.28f),
                        selectedLabelColor = TextPrimary,
                        containerColor = DarkSurfaceVariant,
                        labelColor = SecondaryTeal
                    ),
                    modifier = Modifier.testTag("crop_preset_match_canvas")
                )
            }

            presets.forEach { (label, ratio) ->
                val isSelected = !cropMatchCanvas && when {
                    ratio == null && selectedAspectRatio == null -> true
                    ratio != null && selectedAspectRatio != null -> abs(ratio - selectedAspectRatio) < 0.01f
                    else -> false
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectAspectRatio(ratio) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("crop_ratio_${label.lowercase()}")
                )
            }
        }

        // Maintain Aspect Ratio toggle + Reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleMaintainAspectRatio(!maintainAspectRatio) }
                    .padding(vertical = 2.dp)
                    .testTag("crop_lock_aspect_toggle")
            ) {
                Icon(
                    imageVector = if (maintainAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Maintain Aspect Ratio",
                    tint = if (maintainAspectRatio) SecondaryTeal else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Maintain Aspect Ratio",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = maintainAspectRatio,
                    onCheckedChange = onToggleMaintainAspectRatio,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SecondaryTeal
                    )
                )
            }

            OutlinedButton(
                onClick = onResetCrop,
                modifier = Modifier.testTag("crop_reset_btn")
            ) {
                Text("Reset", fontSize = 11.sp, color = TextSecondary)
            }
        }

        // Manual Crop Width & Height (px) entry + quick exact-canvas button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = widthText,
                onValueChange = { raw ->
                    widthText = raw.filter { it.isDigit() }
                    val typedW = widthText.toIntOrNull()
                    if (typedW != null && typedW > 0 && maintainAspectRatio) {
                        val activeRatio = selectedAspectRatio ?: (liveSelW.toFloat() / liveSelH.toFloat())
                        val nextH = (typedW / activeRatio).roundToInt().coerceIn(1, safeSrcH)
                        heightText = nextH.toString()
                    }
                },
                label = { Text("Crop W (px)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("crop_width_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )

            OutlinedTextField(
                value = heightText,
                onValueChange = { raw ->
                    heightText = raw.filter { it.isDigit() }
                    val typedH = heightText.toIntOrNull()
                    if (typedH != null && typedH > 0 && maintainAspectRatio) {
                        val activeRatio = selectedAspectRatio ?: (liveSelW.toFloat() / liveSelH.toFloat())
                        val nextW = (typedH * activeRatio).roundToInt().coerceIn(1, safeSrcW)
                        widthText = nextW.toString()
                    }
                },
                label = { Text("Crop H (px)", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("crop_height_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )

            Button(
                onClick = {
                    val w = (widthText.toIntOrNull() ?: liveSelW).coerceIn(1, safeSrcW)
                    val h = (heightText.toIntOrNull() ?: liveSelH).coerceIn(1, safeSrcH)
                    onManualCropPixels(w, h)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("crop_apply_dims_btn")
            ) {
                Text("Set px", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        if (!isDirectPhotoMode &&
            safeSrcW >= canvasDimensions.width &&
            safeSrcH >= canvasDimensions.height
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondaryTeal.copy(alpha = 0.14f))
                    .border(1.dp, SecondaryTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable {
                        onMatchCanvasCrop()
                        onManualCropPixels(canvasDimensions.width, canvasDimensions.height)
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .testTag("crop_exact_canvas_pixels_btn"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Set 1:1 Pixel Selection to ${canvasDimensions.width} × ${canvasDimensions.height} px (Top ✓ Apply to Finish)",
                    color = SecondaryTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
