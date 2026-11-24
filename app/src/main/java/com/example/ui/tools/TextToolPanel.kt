package com.example.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasDimensions
import com.example.model.Layer
import com.example.model.LayerType
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FontManager
import kotlin.math.roundToInt

@Composable
fun TextToolPanel(
    selectedLayer: Layer?,
    canvasDimensions: CanvasDimensions,
    onAddTextLayer: (String, Long, Float, String) -> Unit,
    onUpdateText: (String, Long, Float, String, Boolean, Boolean, String) -> Unit,
    onUpdatePosition: (Float, Float) -> Unit,
    onUpdateSpacing: (Float, Float) -> Unit,
    onAddPhotoBottomBanner: ((String) -> Unit)? = null,
    onAddTickMark: ((String, Long) -> Unit)? = null,
    onUpdateTextEffects: ((Boolean, Boolean, Boolean, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textLayer = selectedLayer?.takeIf { it.type == LayerType.TEXT }

    var inputText by remember(textLayer?.id) { mutableStateOf(textLayer?.text ?: "Your Text Here") }
    var textColor by remember(textLayer?.id) { mutableLongStateOf(textLayer?.textColor ?: 0xFFFFFFFFL) }
    var fontSize by remember(textLayer?.id) { mutableFloatStateOf(textLayer?.fontSize ?: 56f) }
    var fontFamily by remember(textLayer?.id) { mutableStateOf(textLayer?.fontFamily ?: "Default") }
    var isBold by remember(textLayer?.id) { mutableStateOf(textLayer?.isBold ?: false) }
    var isItalic by remember(textLayer?.id) { mutableStateOf(textLayer?.isItalic ?: false) }
    var alignment by remember(textLayer?.id) { mutableStateOf(textLayer?.textAlignment ?: "center") }
    var letterSpacing by remember(textLayer?.id) { mutableFloatStateOf(textLayer?.letterSpacing ?: 0f) }
    var opacity by remember(textLayer?.id) { mutableFloatStateOf(textLayer?.opacity ?: 1f) }
    var hasShadow by remember(textLayer?.id) { mutableStateOf(textLayer?.hasShadow ?: false) }
    var hasOutline by remember(textLayer?.id) { mutableStateOf(textLayer?.hasOutline ?: false) }
    var hasBackground by remember(textLayer?.id) { mutableStateOf(textLayer?.hasBackground ?: false) }
    var bgColor by remember(textLayer?.id) { mutableLongStateOf(textLayer?.backgroundColor ?: 0xAA000000L) }

    var fonts by remember { mutableStateOf(FontManager.getAllFonts(context)) }
    var fontMessage by remember { mutableStateOf<String?>(null) }

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val imported = FontManager.importCustomFont(context, uri)
            if (imported != null) {
                fonts = FontManager.getAllFonts(context)
                fontFamily = imported.id
                fontMessage = "Added custom font: ${imported.displayName}"
                if (textLayer != null) {
                    onUpdateText(inputText, textColor, fontSize, fontFamily, isBold, isItalic, alignment)
                }
            }
        }
    }

    val palette = listOf(
        0xFFFFFFFFL, 0xFF000000L, 0xFF22C55EL, 0xFFEF4444L,
        0xFF3B82F6L, 0xFFF59E0BL, 0xFFA855F7L, 0xFFEC4899L
    )
    val symbols = listOf("✓", "✔", "☑", "✗", "★", "❤", "→", "←", "↑", "↓", "↗", "✦")
    val writingTemplates = listOf(
        "Signature" to "— Signature",
        "Approved ✓" to "✓ Approved",
        "Bangla" to "আমার সোনার বাংলা",
        "Clear" to ""
    )

    fun syncIfEditing() {
        if (textLayer != null) {
            onUpdateText(inputText, textColor, fontSize, fontFamily, isBold, isItalic, alignment)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick 1-tap Tick Mark + Writing Templates row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onAddTickMark != null) {
                Button(
                    onClick = { onAddTickMark("✓", 0xFF22C55EL) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.testTag("text_quick_tick_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("✓ Tick Mark", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            writingTemplates.forEach { (label, content) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        inputText = content
                        syncIfEditing()
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                syncIfEditing()
            },
            label = { Text("Write Text (Bangla / English / Tick / Symbols)", fontSize = 11.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_text_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = BorderColor
            )
        )

        // Tick marks, Arrows & Symbols quick insert
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            symbols.forEach { sym ->
                FilterChip(
                    selected = false,
                    onClick = {
                        inputText += sym
                        syncIfEditing()
                    },
                    label = { Text(sym, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurfaceVariant,
                        labelColor = if (sym == "✓" || sym == "✔") AccentGreen else TextPrimary
                    )
                )
            }
        }

        // Color swatches + Style controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                palette.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                if (textColor == c) 2.5.dp else 1.dp,
                                if (textColor == c) SecondaryTeal else BorderColor,
                                CircleShape
                            )
                            .clickable {
                                textColor = c
                                syncIfEditing()
                            }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { isBold = !isBold; syncIfEditing() }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = if (isBold) SecondaryTeal else TextSecondary)
                }
                IconButton(onClick = { isItalic = !isItalic; syncIfEditing() }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = if (isItalic) SecondaryTeal else TextSecondary)
                }
                IconButton(onClick = { alignment = "left"; syncIfEditing() }) {
                    Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Align Left", tint = if (alignment == "left") SecondaryTeal else TextSecondary)
                }
                IconButton(onClick = { alignment = "center"; syncIfEditing() }) {
                    Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = if (alignment == "center") SecondaryTeal else TextSecondary)
                }
                IconButton(onClick = { alignment = "right"; syncIfEditing() }) {
                    Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = "Align Right", tint = if (alignment == "right") SecondaryTeal else TextSecondary)
                }
            }
        }

        // Fonts + Custom Font Import
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    fontPicker.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
                }
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Font", fontSize = 11.sp)
            }
            fonts.forEach { f ->
                FilterChip(
                    selected = fontFamily == f.id,
                    onClick = {
                        fontFamily = f.id
                        syncIfEditing()
                    },
                    label = { Text(f.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        if (fontMessage != null) {
            Text(fontMessage!!, color = SecondaryTeal, fontSize = 11.sp)
        }

        // Size slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Font Size", color = TextPrimary, fontSize = 12.sp)
                Text("${fontSize.roundToInt()} sp", color = SecondaryTeal, fontSize = 12.sp)
            }
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    syncIfEditing()
                },
                valueRange = 14f..220f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )
        }

        // Letter Spacing & Opacity sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Spacing: ${letterSpacing.roundToInt()}", color = TextSecondary, fontSize = 11.sp)
                Slider(
                    value = letterSpacing,
                    onValueChange = {
                        letterSpacing = it
                        onUpdateSpacing(letterSpacing, opacity)
                    },
                    valueRange = 0f..15f
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Opacity: ${(opacity * 100f).roundToInt()}%", color = TextSecondary, fontSize = 11.sp)
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        onUpdateSpacing(letterSpacing, opacity)
                    },
                    valueRange = 0.1f..1f
                )
            }
        }

        // Writing Effects Toggles (Shadow, Outline, Optional Tag Box)
        if (onUpdateTextEffects != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shadow", color = TextPrimary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = hasShadow,
                        onCheckedChange = {
                            hasShadow = it
                            onUpdateTextEffects(hasShadow, hasOutline, hasBackground, bgColor)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryTeal)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Outline", color = TextPrimary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = hasOutline,
                        onCheckedChange = {
                            hasOutline = it
                            onUpdateTextEffects(hasShadow, hasOutline, hasBackground, bgColor)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SecondaryTeal)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Label Box", color = TextPrimary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = hasBackground,
                        onCheckedChange = {
                            hasBackground = it
                            onUpdateTextEffects(hasShadow, hasOutline, hasBackground, bgColor)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onAddTextLayer(inputText, textColor, fontSize, fontFamily) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (textLayer == null) "Add Text Layer" else "Add New Copy",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (onAddPhotoBottomBanner != null) {
                OutlinedButton(
                    onClick = { onAddPhotoBottomBanner(inputText) },
                    modifier = Modifier.testTag("btn_photo_bottom_name")
                ) {
                    Text("Name Strip", fontSize = 11.sp)
                }
            }
        }
    }
}
