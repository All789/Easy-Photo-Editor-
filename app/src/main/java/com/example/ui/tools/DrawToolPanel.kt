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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun DrawToolPanel(
    drawColor: Long,
    strokeWidth: Float,
    drawAlpha: Float,
    strokeCount: Int,
    onColorChange: (Long) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onUndoStroke: () -> Unit,
    onClearStrokes: () -> Unit,
    onAddTickMark: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        0xFF22C55EL, // Green (Tick default)
        0xFFFFFFFFL, // White
        0xFF000000L, // Black
        0xFFEF4444L, // Red
        0xFF3B82F6L, // Blue
        0xFFFACC15L, // Yellow
        0xFFA855F7L, // Purple
        0xFF14B8A6L  // Teal
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Draw / Write on Photo (Tap ✓ Done to finish)",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$strokeCount strokes",
                color = SecondaryTeal,
                fontSize = 11.sp
            )
        }

        // Pen Color Swatches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            palette.forEach { col ->
                val selected = drawColor == col
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(col))
                        .border(
                            width = if (selected) 2.5.dp else 1.dp,
                            color = if (selected) PrimaryBlue else BorderColor,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(col) }
                        .testTag("draw_color_${col}")
                )
            }
        }

        // Pen Style Presets (Pen vs Highlighter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = drawAlpha >= 0.9f,
                onClick = { onAlphaChange(1f) },
                label = { Text("Pen (Solid)", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                )
            )
            FilterChip(
                selected = drawAlpha < 0.9f,
                onClick = { onAlphaChange(0.45f) },
                label = { Text("Highlighter", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SecondaryTeal,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onUndoStroke,
                enabled = strokeCount > 0,
                modifier = Modifier.testTag("draw_undo_stroke_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Stroke", modifier = Modifier.size(15.dp))
            }
            OutlinedButton(
                onClick = onClearStrokes,
                enabled = strokeCount > 0,
                modifier = Modifier.testTag("draw_clear_btn")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(15.dp))
            }
        }

        // Brush Size Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pen Size", color = TextPrimary, fontSize = 12.sp)
                Text("${strokeWidth.roundToInt()} px", color = SecondaryTeal, fontSize = 12.sp)
            }
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 2f..48f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue),
                modifier = Modifier.testTag("draw_size_slider")
            )
        }

        // Quick 1-Tap Tick Mark Stamp Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddTickMark("✓", drawColor) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_add_tick_stamp")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add ✓ Tick Mark", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onAddTickMark("✔", drawColor) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_add_bold_tick_stamp")
            ) {
                Text("✔ Bold", fontSize = 12.sp, color = TextPrimary)
            }
            OutlinedButton(
                onClick = { onAddTickMark("☑", drawColor) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_add_box_tick_stamp")
            ) {
                Text("☑ Box", fontSize = 12.sp, color = TextPrimary)
            }
        }
    }
}
