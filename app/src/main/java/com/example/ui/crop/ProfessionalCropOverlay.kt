package com.example.ui.crop

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasDimensions
import com.example.model.CropRect
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProfessionalCropOverlay(
    bitmap: Bitmap,
    cropRect: CropRect,
    aspectRatio: Float?,
    maintainAspectRatio: Boolean = false,
    canvasDimensions: CanvasDimensions? = null,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCrop by rememberUpdatedState(cropRect)
    val currentAspect by rememberUpdatedState(aspectRatio)
    val currentMaintainAspect by rememberUpdatedState(maintainAspectRatio)
    val currentOnCropChange by rememberUpdatedState(onCropChange)
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current

    val srcW = bitmap.width.coerceAtLeast(1)
    val srcH = bitmap.height.coerceAtLeast(1)
    val selPxW = (cropRect.width * srcW).roundToInt().coerceIn(1, srcW)
    val selPxH = (cropRect.height * srcH).roundToInt().coerceIn(1, srcH)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121214)),
        contentAlignment = Alignment.Center
    ) {
        val availW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val availH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val imgAspect = srcW.toFloat() / srcH.toFloat()
        val pad = with(density) { 24.dp.toPx() }
        val drawMaxW = (availW - pad * 2f).coerceAtLeast(60f)
        val drawMaxH = (availH - pad * 2f).coerceAtLeast(60f)

        val displayScale = min(drawMaxW / srcW.toFloat(), drawMaxH / srcH.toFloat())
        val imgW = srcW * displayScale
        val imgH = srcH * displayScale
        val imgLeft = (availW - imgW) / 2f
        val imgTop = (availH - imgH) / 2f

        val cornerHitRadius = with(density) { 28.dp.toPx() }
        val edgeHitThreshold = with(density) { 22.dp.toPx() }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Crop Target",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bitmap) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()

                            val initialCrop = currentCrop
                            val startL = imgLeft + initialCrop.left * imgW
                            val startT = imgTop + initialCrop.top * imgH
                            val startR = imgLeft + initialCrop.right * imgW
                            val startB = imgTop + initialCrop.bottom * imgH

                            val p = down.position
                            val activeHandle = when {
                                abs(p.x - startL) <= cornerHitRadius && abs(p.y - startT) <= cornerHitRadius -> CropHandle.TOP_LEFT
                                abs(p.x - startR) <= cornerHitRadius && abs(p.y - startT) <= cornerHitRadius -> CropHandle.TOP_RIGHT
                                abs(p.x - startL) <= cornerHitRadius && abs(p.y - startB) <= cornerHitRadius -> CropHandle.BOTTOM_LEFT
                                abs(p.x - startR) <= cornerHitRadius && abs(p.y - startB) <= cornerHitRadius -> CropHandle.BOTTOM_RIGHT
                                abs(p.x - startL) <= edgeHitThreshold && p.y in (startT - edgeHitThreshold)..(startB + edgeHitThreshold) -> CropHandle.LEFT
                                abs(p.x - startR) <= edgeHitThreshold && p.y in (startT - edgeHitThreshold)..(startB + edgeHitThreshold) -> CropHandle.RIGHT
                                abs(p.y - startT) <= edgeHitThreshold && p.x in (startL - edgeHitThreshold)..(startR + edgeHitThreshold) -> CropHandle.TOP
                                abs(p.y - startB) <= edgeHitThreshold && p.x in (startL - edgeHitThreshold)..(startR + edgeHitThreshold) -> CropHandle.BOTTOM
                                p.x in startL..startR && p.y in startT..startB -> CropHandle.BODY
                                else -> CropHandle.NONE
                            }

                            if (activeHandle == CropHandle.NONE) return@awaitEachGesture

                            val lockedPixelRatio: Float? = currentAspect ?: if (currentMaintainAspect) {
                                val initPxW = initialCrop.width * srcW
                                val initPxH = initialCrop.height * srcH
                                if (initPxH > 0f) initPxW / initPxH else 1f
                            } else null

                            // Normalized coordinate ratio = (dNormW / dNormH)
                            val lockedNormRatio = lockedPixelRatio?.let { it / imgAspect }
                            val minNormSize = 0.04f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()

                                val totalDx = (change.position.x - down.position.x) / imgW
                                val totalDy = (change.position.y - down.position.y) / imgH

                                var l = initialCrop.left
                                var t = initialCrop.top
                                var r = initialCrop.right
                                var b = initialCrop.bottom

                                if (activeHandle == CropHandle.BODY) {
                                    val w = r - l
                                    val h = b - t
                                    l = (initialCrop.left + totalDx).coerceIn(0f, 1f - w)
                                    t = (initialCrop.top + totalDy).coerceIn(0f, 1f - h)
                                    r = l + w
                                    b = t + h
                                } else {
                                    when (activeHandle) {
                                        CropHandle.LEFT -> l = (initialCrop.left + totalDx).coerceIn(0f, r - minNormSize)
                                        CropHandle.RIGHT -> r = (initialCrop.right + totalDx).coerceIn(l + minNormSize, 1f)
                                        CropHandle.TOP -> t = (initialCrop.top + totalDy).coerceIn(0f, b - minNormSize)
                                        CropHandle.BOTTOM -> b = (initialCrop.bottom + totalDy).coerceIn(t + minNormSize, 1f)
                                        CropHandle.TOP_LEFT -> {
                                            l = (initialCrop.left + totalDx).coerceIn(0f, r - minNormSize)
                                            t = (initialCrop.top + totalDy).coerceIn(0f, b - minNormSize)
                                        }
                                        CropHandle.TOP_RIGHT -> {
                                            r = (initialCrop.right + totalDx).coerceIn(l + minNormSize, 1f)
                                            t = (initialCrop.top + totalDy).coerceIn(0f, b - minNormSize)
                                        }
                                        CropHandle.BOTTOM_LEFT -> {
                                            l = (initialCrop.left + totalDx).coerceIn(0f, r - minNormSize)
                                            b = (initialCrop.bottom + totalDy).coerceIn(t + minNormSize, 1f)
                                        }
                                        CropHandle.BOTTOM_RIGHT -> {
                                            r = (initialCrop.right + totalDx).coerceIn(l + minNormSize, 1f)
                                            b = (initialCrop.bottom + totalDy).coerceIn(t + minNormSize, 1f)
                                        }
                                        else -> Unit
                                    }

                                    // Apply smooth aspect ratio lock if active without sudden jumps
                                    if (lockedNormRatio != null && lockedNormRatio > 0f) {
                                        val isVerticalEdge = activeHandle == CropHandle.TOP || activeHandle == CropHandle.BOTTOM
                                        if (isVerticalEdge) {
                                            val desiredH = (b - t).coerceAtLeast(minNormSize)
                                            var desiredW = desiredH * lockedNormRatio
                                            val cx = (initialCrop.left + initialCrop.right) / 2f
                                            if (desiredW > 1f) {
                                                desiredW = 1f
                                                val cappedH = desiredW / lockedNormRatio
                                                if (activeHandle == CropHandle.TOP) t = b - cappedH else b = t + cappedH
                                            }
                                            var newL = cx - desiredW / 2f
                                            var newR = cx + desiredW / 2f
                                            if (newL < 0f) {
                                                newL = 0f
                                                newR = desiredW.coerceAtMost(1f)
                                            } else if (newR > 1f) {
                                                newR = 1f
                                                newL = (1f - desiredW).coerceAtLeast(0f)
                                            }
                                            l = newL
                                            r = newR
                                        } else {
                                            // Width-driven anchor for LEFT, RIGHT, and all 4 CORNERS
                                            var desiredW = (r - l).coerceAtLeast(minNormSize)
                                            var desiredH = desiredW / lockedNormRatio

                                            when (activeHandle) {
                                                CropHandle.LEFT, CropHandle.RIGHT -> {
                                                    val cy = (initialCrop.top + initialCrop.bottom) / 2f
                                                    if (desiredH > 1f) {
                                                        desiredH = 1f
                                                        desiredW = desiredH * lockedNormRatio
                                                        if (activeHandle == CropHandle.LEFT) l = r - desiredW else r = l + desiredW
                                                    }
                                                    var newT = cy - desiredH / 2f
                                                    var newB = cy + desiredH / 2f
                                                    if (newT < 0f) {
                                                        newT = 0f
                                                        newB = desiredH.coerceAtMost(1f)
                                                    } else if (newB > 1f) {
                                                        newB = 1f
                                                        newT = (1f - desiredH).coerceAtLeast(0f)
                                                    }
                                                    t = newT
                                                    b = newB
                                                }
                                                CropHandle.TOP_LEFT -> {
                                                    val maxH = initialCrop.bottom
                                                    if (desiredH > maxH) {
                                                        desiredH = maxH
                                                        desiredW = desiredH * lockedNormRatio
                                                        l = (initialCrop.right - desiredW).coerceAtLeast(0f)
                                                    }
                                                    t = (initialCrop.bottom - desiredH).coerceAtLeast(0f)
                                                }
                                                CropHandle.TOP_RIGHT -> {
                                                    val maxH = initialCrop.bottom
                                                    if (desiredH > maxH) {
                                                        desiredH = maxH
                                                        desiredW = desiredH * lockedNormRatio
                                                        r = (initialCrop.left + desiredW).coerceAtMost(1f)
                                                    }
                                                    t = (initialCrop.bottom - desiredH).coerceAtLeast(0f)
                                                }
                                                CropHandle.BOTTOM_LEFT -> {
                                                    val maxH = 1f - initialCrop.top
                                                    if (desiredH > maxH) {
                                                        desiredH = maxH
                                                        desiredW = desiredH * lockedNormRatio
                                                        l = (initialCrop.right - desiredW).coerceAtLeast(0f)
                                                    }
                                                    b = (initialCrop.top + desiredH).coerceAtMost(1f)
                                                }
                                                CropHandle.BOTTOM_RIGHT -> {
                                                    val maxH = 1f - initialCrop.top
                                                    if (desiredH > maxH) {
                                                        desiredH = maxH
                                                        desiredW = desiredH * lockedNormRatio
                                                        r = (initialCrop.left + desiredW).coerceAtMost(1f)
                                                    }
                                                    b = (initialCrop.top + desiredH).coerceAtMost(1f)
                                                }
                                                else -> Unit
                                            }
                                        }
                                    }
                                }

                                currentOnCropChange(
                                    CropRect(
                                        left = l.coerceIn(0f, 1f - minNormSize),
                                        top = t.coerceIn(0f, 1f - minNormSize),
                                        right = r.coerceIn(l + minNormSize, 1f),
                                        bottom = b.coerceIn(t + minNormSize, 1f)
                                    )
                                )
                            }
                        }
                    }
            ) {
                val cL = imgLeft + cropRect.left * imgW
                val cT = imgTop + cropRect.top * imgH
                val cR = imgLeft + cropRect.right * imgW
                val cB = imgTop + cropRect.bottom * imgH
                val cW = cR - cL
                val cH = cB - cT

                val maskColor = Color.Black.copy(alpha = 0.62f)
                // Top mask
                drawRect(maskColor, Offset(imgLeft, imgTop), Size(imgW, (cT - imgTop).coerceAtLeast(0f)))
                // Bottom mask
                drawRect(maskColor, Offset(imgLeft, cB), Size(imgW, (imgTop + imgH - cB).coerceAtLeast(0f)))
                // Left mask
                drawRect(maskColor, Offset(imgLeft, cT), Size((cL - imgLeft).coerceAtLeast(0f), cH))
                // Right mask
                drawRect(maskColor, Offset(cR, cT), Size((imgLeft + imgW - cR).coerceAtLeast(0f), cH))

                // Crisp border
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cL, cT),
                    size = Size(cW, cH),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )

                // Rule of thirds grid
                val gridColor = Color.White.copy(alpha = 0.35f)
                val gridStroke = 1.dp.toPx()
                drawLine(gridColor, Offset(cL + cW / 3f, cT), Offset(cL + cW / 3f, cB), gridStroke)
                drawLine(gridColor, Offset(cL + 2f * cW / 3f, cT), Offset(cL + 2f * cW / 3f, cB), gridStroke)
                drawLine(gridColor, Offset(cL, cT + cH / 3f), Offset(cR, cT + cH / 3f), gridStroke)
                drawLine(gridColor, Offset(cL, cT + 2f * cH / 3f), Offset(cR, cT + 2f * cH / 3f), gridStroke)

                // Bold corner brackets & edge grips
                val bracketLen = min(22.dp.toPx(), min(cW, cH) * 0.35f)
                val bracketStroke = 3.5.dp.toPx()
                val handleColor = PrimaryBlue
                // TL
                drawLine(handleColor, Offset(cL, cT), Offset(cL + bracketLen, cT), bracketStroke, StrokeCap.Round)
                drawLine(handleColor, Offset(cL, cT), Offset(cL, cT + bracketLen), bracketStroke, StrokeCap.Round)
                // TR
                drawLine(handleColor, Offset(cR, cT), Offset(cR - bracketLen, cT), bracketStroke, StrokeCap.Round)
                drawLine(handleColor, Offset(cR, cT), Offset(cR, cT + bracketLen), bracketStroke, StrokeCap.Round)
                // BL
                drawLine(handleColor, Offset(cL, cB), Offset(cL + bracketLen, cB), bracketStroke, StrokeCap.Round)
                drawLine(handleColor, Offset(cL, cB), Offset(cL, cB - bracketLen), bracketStroke, StrokeCap.Round)
                // BR
                drawLine(handleColor, Offset(cR, cB), Offset(cR - bracketLen, cB), bracketStroke, StrokeCap.Round)
                drawLine(handleColor, Offset(cR, cB), Offset(cR, cB - bracketLen), bracketStroke, StrokeCap.Round)

                // Edge center bars
                val edgeBarLen = min(24.dp.toPx(), min(cW, cH) * 0.35f)
                drawLine(Color.White, Offset(cL + cW / 2f - edgeBarLen / 2f, cT), Offset(cL + cW / 2f + edgeBarLen / 2f, cT), 3.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cL + cW / 2f - edgeBarLen / 2f, cB), Offset(cL + cW / 2f + edgeBarLen / 2f, cB), 3.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cL, cT + cH / 2f - edgeBarLen / 2f), Offset(cL, cT + cH / 2f + edgeBarLen / 2f), 3.dp.toPx(), StrokeCap.Round)
                drawLine(Color.White, Offset(cR, cT + cH / 2f - edgeBarLen / 2f), Offset(cR, cT + cH / 2f + edgeBarLen / 2f), 3.dp.toPx(), StrokeCap.Round)
            }

            // Live pixel size & target canvas badge during crop (no black ruler or 91.4mm label!)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Crop: ${selPxW} × ${selPxH} px",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (canvasDimensions != null) {
                    Text(
                        text = "Canvas: ${canvasDimensions.width} × ${canvasDimensions.height} px",
                        color = SecondaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
