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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.ui.setup.PageSetupDefaults
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CanvasSettingsPanel(
    dimensions: CanvasDimensions,
    onUpdateCanvas: (Int, Int, Boolean, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthStr by remember(dimensions.width) { mutableStateOf(dimensions.width.toString()) }
    var heightStr by remember(dimensions.height) { mutableStateOf(dimensions.height.toString()) }
    var isTransparent by remember(dimensions.isTransparent) { mutableStateOf(dimensions.isTransparent) }
    var bgColor by remember(dimensions.backgroundColor) {
        mutableLongStateOf(if (dimensions.backgroundColor == 0L) 0xFFFFFFFFL else dimensions.backgroundColor)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Canvas Dimensions & Background", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = widthStr,
                onValueChange = { widthStr = it.filter { ch -> ch.isDigit() } },
                label = { Text("Width (px)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("canvas_settings_width"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )
            OutlinedTextField(
                value = heightStr,
                onValueChange = { heightStr = it.filter { ch -> ch.isDigit() } },
                label = { Text("Height (px)", fontSize = 11.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("canvas_settings_height"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PageSetupDefaults.colors.forEach { opt ->
                val selected = (opt.isTransparent && isTransparent) ||
                    (!opt.isTransparent && !isTransparent && opt.colorLong == bgColor)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            isTransparent = opt.isTransparent
                            if (!opt.isTransparent) bgColor = opt.colorLong
                        }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                            Icon(Icons.Default.GridOn, contentDescription = "Transparent", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        } else if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (opt.colorLong == 0xFF000000L) Color.White else PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(opt.label, color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Button(
            onClick = {
                val w = (widthStr.toIntOrNull() ?: dimensions.width).coerceIn(16, 8000)
                val h = (heightStr.toIntOrNull() ?: dimensions.height).coerceIn(16, 8000)
                onUpdateCanvas(w, h, isTransparent, bgColor)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("apply_canvas_settings_btn")
        ) {
            Text("Apply Canvas Settings", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
