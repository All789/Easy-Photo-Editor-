package com.example.ui.tools

import androidx.compose.foundation.Image
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processing.StickerCategory
import com.example.processing.StickerGenerator
import com.example.processing.StickerSpec
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StickersToolPanel(
    onAddSticker: (StickerSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(StickerCategory.ALL) }
    val stickers = remember(selectedCategory) {
        if (selectedCategory == StickerCategory.ALL) {
            StickerGenerator.allStickers
        } else {
            StickerGenerator.allStickers.filter { it.category == selectedCategory }
        }
    }

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
                text = "Tap any Sticker to add • Tap ✓ Done when finished",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${stickers.size} items",
                color = SecondaryTeal,
                fontSize = 11.sp
            )
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StickerCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("sticker_cat_${cat.name}")
                )
            }
        }

        // Scrollable Sticker Strip (2 rows for fast browsing)
        val firstRow = remember(stickers) { stickers.filterIndexed { index, _ -> index % 2 == 0 } }
        val secondRow = remember(stickers) { stickers.filterIndexed { index, _ -> index % 2 == 1 } }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StickerGridRow(items = firstRow, onAddSticker = onAddSticker)
            if (secondRow.isNotEmpty()) {
                StickerGridRow(items = secondRow, onAddSticker = onAddSticker)
            }
        }
    }
}

@Composable
private fun StickerGridRow(
    items: List<StickerSpec>,
    onAddSticker: (StickerSpec) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { spec ->
            val preview = remember(spec.id) {
                StickerGenerator.getPreviewBitmap(spec.id).asImageBitmap()
            }
            Column(
                modifier = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkBackground)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .clickable { onAddSticker(spec) }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .testTag("sticker_item_${spec.id}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = preview,
                    contentDescription = spec.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = spec.label,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
