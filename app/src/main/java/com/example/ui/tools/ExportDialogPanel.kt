package com.example.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasDimensions
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ExportDialogPanel(
    dimensions: CanvasDimensions,
    layerCount: Int,
    isSaving: Boolean,
    onSaveProject: () -> Unit,
    onExportImage: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var format by remember { mutableStateOf("PNG") }
    var quality by remember { mutableFloatStateOf(95f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Export & Save", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Resolution: ${dimensions.width} × ${dimensions.height} px  •  Layers: $layerCount  •  Transparency: ${if (dimensions.isTransparent) "Yes" else "No"}",
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
                onClick = { format = "PNG" },
                label = { Text("PNG") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                modifier = Modifier.testTag("export_format_png")
            )
            FilterChip(
                selected = format == "JPEG",
                onClick = { format = "JPEG" },
                label = { Text("JPEG") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                modifier = Modifier.testTag("export_format_jpeg")
            )
        }

        if (format == "JPEG") {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Quality", color = TextPrimary, fontSize = 12.sp)
                    Text("${quality.roundToInt()}%", color = SecondaryTeal, fontSize = 12.sp)
                }
                Slider(
                    value = quality,
                    onValueChange = { quality = it },
                    valueRange = 50f..100f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue),
                    modifier = Modifier.testTag("jpeg_quality_slider")
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSaveProject,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_project_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Saving...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Project", fontSize = 12.sp)
                }
            }
            Button(
                onClick = { onExportImage(format, quality.roundToInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_image_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export $format", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
