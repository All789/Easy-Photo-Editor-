package com.example.ui.setup

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun PageSetupDialog(
    onDismiss: () -> Unit,
    onCreatePage: (width: Int, height: Int, isTransparent: Boolean, bgColor: Long, name: String) -> Unit,
    onCreatePageWithUnits: ((width: Int, height: Int, isTransparent: Boolean, bgColor: Long, name: String, dpi: Int) -> Unit)? = null
) {
    var projectName by remember { mutableStateOf("Untitled Project") }
    var selectedUnit by remember { mutableStateOf(SizeUnit.PIXELS) }
    var dpi by remember { mutableIntStateOf(300) }
    var widthInput by remember { mutableStateOf("1080") }
    var heightInput by remember { mutableStateOf("1080") }
    var selectedColor by remember { mutableStateOf(PageSetupDefaults.colors.first()) }
    var selectedPresetId by remember { mutableStateOf("square_1080") }

    val widthVal = widthInput.toFloatOrNull()?.coerceAtLeast(1f) ?: 1080f
    val heightVal = heightInput.toFloatOrNull()?.coerceAtLeast(1f) ?: 1080f
    val widthPx = PageSetupDefaults.unitValueToPixels(widthVal, selectedUnit, dpi)
    val heightPx = PageSetupDefaults.unitValueToPixels(heightVal, selectedUnit, dpi)

    fun formatForUnit(px: Int, unit: SizeUnit, targetDpi: Int): String {
        val converted = PageSetupDefaults.pixelsToUnitValue(px, unit, targetDpi)
        return if (unit == SizeUnit.PIXELS) {
            px.toString()
        } else {
            String.format(Locale.US, "%.2f", converted)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Project Setup",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_page_setup_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name", color = TextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Text("Quick Presets", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PageSetupDefaults.presets.forEach { preset ->
                        val isSelected = selectedPresetId == preset.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPresetId = preset.id
                                dpi = preset.dpi
                                widthInput = formatForUnit(preset.widthPx, selectedUnit, preset.dpi)
                                heightInput = formatForUnit(preset.heightPx, selectedUnit, preset.dpi)
                            },
                            label = {
                                Column {
                                    Text(preset.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(preset.subtitle, fontSize = 10.sp, color = TextSecondary)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("preset_${preset.id}")
                        )
                    }
                }

                Text("Unit", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SizeUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = {
                                val oldPxW = widthPx
                                val oldPxH = heightPx
                                selectedUnit = unit
                                widthInput = formatForUnit(oldPxW, unit, dpi)
                                heightInput = formatForUnit(oldPxH, unit, dpi)
                            },
                            label = { Text(unit.shortLabel.uppercase(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("unit_${unit.shortLabel}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = {
                            widthInput = it
                            selectedPresetId = "custom"
                        },
                        label = { Text("Width (${selectedUnit.shortLabel})", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("canvas_width_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = {
                            heightInput = it
                            selectedPresetId = "custom"
                        },
                        label = { Text("Height (${selectedUnit.shortLabel})", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("canvas_height_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                }

                Text("Resolution (DPI)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(72, 150, 300).forEach { dpiOpt ->
                        FilterChip(
                            selected = dpi == dpiOpt,
                            onClick = { dpi = dpiOpt },
                            label = { Text("$dpiOpt DPI", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SecondaryTeal,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("dpi_${dpiOpt}")
                        )
                    }
                }

                Text(
                    text = "Output: $widthPx × $heightPx px • $dpi DPI",
                    color = SecondaryTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text("Background", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PageSetupDefaults.colors.forEach { opt ->
                        val selected = selectedColor.label == opt.label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedColor = opt }
                                .padding(4.dp)
                                .testTag("bg_color_${opt.label.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (opt.isTransparent) DarkBackground else Color(opt.colorLong))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) PrimaryBlue else BorderColor,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (opt.isTransparent) {
                                    Icon(
                                        Icons.Default.GridOn,
                                        contentDescription = "Transparent",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (opt.colorLong == 0xFF000000L) Color.White else PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(opt.label, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_page_setup_btn")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            if (onCreatePageWithUnits != null) {
                                onCreatePageWithUnits(
                                    widthPx,
                                    heightPx,
                                    selectedColor.isTransparent,
                                    selectedColor.colorLong,
                                    projectName.ifBlank { "Untitled Project" },
                                    dpi
                                )
                            } else {
                                onCreatePage(
                                    widthPx,
                                    heightPx,
                                    selectedColor.isTransparent,
                                    selectedColor.colorLong,
                                    projectName.ifBlank { "Untitled Project" }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_page_btn")
                    ) {
                        Text("Create Canvas", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
