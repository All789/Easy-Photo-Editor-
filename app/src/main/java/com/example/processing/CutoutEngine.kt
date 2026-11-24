package com.example.processing

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import com.example.model.CutoutShape
import com.example.model.CutoutTarget
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object CutoutEngine {

    private data class PersonMaskData(
        val floats: FloatArray,
        val maskW: Int,
        val maskH: Int,
        val minYFrac: Float,
        val maxYFrac: Float,
        val minXFrac: Float,
        val maxXFrac: Float,
        val centerXFrac: Float
    )

    private fun computePersonMask(bitmap: Bitmap): PersonMaskData? {
        return try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val mask = Tasks.await(segmenter.process(inputImage), 15L, TimeUnit.SECONDS)
            val maskW = mask.width
            val maskH = mask.height
            val buffer = mask.buffer
            buffer.rewind()
            val floats = FloatArray(maskW * maskH)
            buffer.asFloatBuffer().get(floats)
            segmenter.close()

            var minX = maskW
            var maxX = 0
            var minY = maskH
            var maxY = 0
            var sumX = 0L
            var count = 0L

            for (y in 0 until maskH) {
                val row = y * maskW
                for (x in 0 until maskW) {
                    if (floats[row + x] > 0.45f) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                        sumX += x
                        count++
                    }
                }
            }

            if (count == 0L) {
                PersonMaskData(floats, maskW, maskH, 0.08f, 0.92f, 0.20f, 0.80f, 0.50f)
            } else {
                PersonMaskData(
                    floats = floats,
                    maskW = maskW,
                    maskH = maskH,
                    minYFrac = minY.toFloat() / maskH,
                    maxYFrac = maxY.toFloat() / maskH,
                    minXFrac = minX.toFloat() / maskW,
                    maxXFrac = maxX.toFloat() / maskW,
                    centerXFrac = (sumX.toFloat() / count) / maskW
                )
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        val cb = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b
        val cr = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        return y > 45f && cb in 77f..127f && cr in 133f..176f
    }

    private fun isSkyColor(r: Int, g: Int, b: Int): Boolean {
        val lum = 0.299f * r + 0.587f * g + 0.114f * b
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val sat = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC
        val blueSky = b >= r + 10 && b >= g - 12 && lum > 75f
        val brightCloudySky = lum > 168f && sat < 0.24f
        val sunsetSky = r > b + 20 && lum > 125f && sat < 0.65f
        return blueSky || brightCloudySky || sunsetSky
    }

    suspend fun extractSemanticCutout(
        source: Bitmap,
        target: CutoutTarget,
        smoothing: Float = 0.5f
    ): Bitmap = withContext(Dispatchers.Default) {
        if (target == CutoutTarget.PERSON) {
            return@withContext BackgroundRemover.removeBackgroundAi(source, smoothing)
        }

        val w = source.width
        val h = source.height
        val maskData = computePersonMask(source)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val lowerThreshold = (0.25f - smoothing * 0.15f).coerceIn(0.05f, 0.4f)
        val upperThreshold = (smoothing * 0.15f + 0.75f).coerceIn(0.6f, 0.95f)
        val rampRange = maxOf(0.01f, upperThreshold - lowerThreshold)

        val minY = maskData?.minYFrac ?: 0.08f
        val maxY = maskData?.maxYFrac ?: 0.92f
        val personH = (maxY - minY).coerceAtLeast(0.15f)
        val centerX = maskData?.centerXFrac ?: 0.5f

        for (y in 0 until h) {
            val yFrac = y.toFloat() / h
            val rowOffset = y * w
            val maskY = if (maskData != null) {
                (yFrac * maskData.maskH).toInt().coerceIn(0, maskData.maskH - 1)
            } else 0
            val maskRowOffset = maskY * (maskData?.maskW ?: 1)

            for (x in 0 until w) {
                val xFrac = x.toFloat() / w
                val origColor = pixels[rowOffset + x]
                val origAlpha = (origColor ushr 24) and 0xFF
                val r = (origColor shr 16) and 0xFF
                val g = (origColor shr 8) and 0xFF
                val b = origColor and 0xFF

                val fgConf = if (maskData != null) {
                    val maskX = (xFrac * maskData.maskW).toInt().coerceIn(0, maskData.maskW - 1)
                    maskData.floats[maskRowOffset + maskX]
                } else {
                    // Fallback radial prior
                    val dx = (xFrac - 0.5f) / 0.35f
                    val dy = (yFrac - 0.5f) / 0.42f
                    if (dx * dx + dy * dy <= 1f) 0.9f else 0.1f
                }

                val fgAlpha = when {
                    fgConf >= upperThreshold -> 1f
                    fgConf > lowerThreshold -> ((fgConf - lowerThreshold) / rampRange).coerceIn(0f, 1f)
                    else -> 0f
                }

                val relY = ((yFrac - minY) / personH).coerceIn(-0.5f, 1.5f)

                val factor: Float = when (target) {
                    CutoutTarget.PERSON -> fgAlpha
                    CutoutTarget.BACKGROUND -> 1f - fgAlpha
                    CutoutTarget.HEAD -> {
                        // Head occupies roughly top 0%..38% of person bounding box
                        val verticalFade = when {
                            relY < 0f -> 0f
                            relY <= 0.34f -> 1f
                            relY <= 0.42f -> (1f - (relY - 0.34f) / 0.08f).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        fgAlpha * verticalFade
                    }
                    CutoutTarget.FACE -> {
                        // Inner face portrait zone (skin + central head oval)
                        val verticalWeight = when {
                            relY < 0.06f -> 0f
                            relY <= 0.33f -> 1f
                            relY <= 0.39f -> (1f - (relY - 0.33f) / 0.06f).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val ovalDist = sqrt(
                            ((xFrac - centerX) / 0.14f) * ((xFrac - centerX) / 0.14f) +
                                ((relY - 0.21f) / 0.14f) * ((relY - 0.21f) / 0.14f)
                        )
                        val ovalWeight = when {
                            ovalDist <= 0.85f -> 1f
                            ovalDist <= 1.25f -> (1f - (ovalDist - 0.85f) / 0.40f).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val skinBoost = if (isSkinTone(r, g, b)) 1f else ovalWeight * 0.55f
                        (fgAlpha * verticalWeight * max(ovalWeight, skinBoost * verticalWeight)).coerceIn(0f, 1f)
                    }
                    CutoutTarget.HAIR -> {
                        // Upper head excluding core face skin
                        val headZone = when {
                            relY < -0.02f -> 0f
                            relY <= 0.33f -> 1f
                            relY <= 0.39f -> (1f - (relY - 0.33f) / 0.06f).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        val faceOval = sqrt(
                            ((xFrac - centerX) / 0.11f) * ((xFrac - centerX) / 0.11f) +
                                ((relY - 0.22f) / 0.11f) * ((relY - 0.22f) / 0.11f)
                        )
                        val notFaceSkin = if (isSkinTone(r, g, b) && faceOval < 1.15f) 0.05f else 1f
                        (fgAlpha * headZone * notFaceSkin).coerceIn(0f, 1f)
                    }
                    CutoutTarget.CLOTHES -> {
                        // Torso & outfit below neck line
                        val torsoWeight = when {
                            relY < 0.31f -> 0f
                            relY <= 0.38f -> ((relY - 0.31f) / 0.07f).coerceIn(0f, 1f)
                            else -> 1f
                        }
                        fgAlpha * torsoWeight
                    }
                    CutoutTarget.SKY -> {
                        // Upper sky pixels outside foreground person mask
                        val bgFactor = 1f - fgAlpha
                        val upperPrior = when {
                            yFrac <= 0.60f -> 1f
                            yFrac <= 0.80f -> (1f - (yFrac - 0.60f) / 0.20f).coerceIn(0f, 1f)
                            else -> 0f
                        }
                        if (bgFactor > 0.15f && upperPrior > 0f && isSkyColor(r, g, b)) {
                            (bgFactor * upperPrior).coerceIn(0f, 1f)
                        } else 0f
                    }
                }

                val newAlpha = (origAlpha * factor).toInt().coerceIn(0, 255)
                pixels[rowOffset + x] = (newAlpha shl 24) or (origColor and 0x00FFFFFF)
            }
        }

        out.setPixels(pixels, 0, w, 0, 0, w, h)
        out
    }

    fun applyOutlineCutout(
        originalSource: Bitmap,
        currentBitmap: Bitmap,
        normalizedPolygon: List<Pair<Float, Float>>,
        keepInside: Boolean,
        featherPx: Float = 4f
    ): Bitmap {
        if (normalizedPolygon.size < 3) return currentBitmap
        val w = currentBitmap.width
        val h = currentBitmap.height
        val path = Path()
        val (firstX, firstY) = normalizedPolygon[0]
        path.moveTo(firstX.coerceIn(0f, 1f) * w, firstY.coerceIn(0f, 1f) * h)
        for (i in 1 until normalizedPolygon.size) {
            val (px, py) = normalizedPolygon[i]
            path.lineTo(px.coerceIn(0f, 1f) * w, py.coerceIn(0f, 1f) * h)
        }
        path.close()

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        if (keepInside) {
            // Keep ONLY pixels inside outline from crisp originalSource (or currentBitmap scaled)
            val base = if (originalSource.width == w && originalSource.height == h) {
                originalSource
            } else {
                ImageProcessor.scaleBitmapHighQuality(originalSource, w, h)
            }
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                if (featherPx > 0.5f) {
                    maskFilter = BlurMaskFilter(featherPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawPath(path, maskPaint)
            val srcInPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(base, 0f, 0f, srcInPaint)
        } else {
            // Erase inside drawn outline from currentBitmap
            canvas.drawBitmap(currentBitmap, 0f, 0f, null)
            val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                if (featherPx > 0.5f) {
                    maskFilter = BlurMaskFilter(featherPx, BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawPath(path, clearPaint)
        }
        return out
    }

    fun applyBrushOrEraseStroke(
        originalSource: Bitmap,
        currentBitmap: Bitmap,
        normalizedPoints: List<Pair<Float, Float>>,
        brushSizePx: Float,
        hardness: Float,
        isErase: Boolean
    ): Bitmap {
        if (normalizedPoints.isEmpty()) return currentBitmap
        val w = currentBitmap.width
        val h = currentBitmap.height
        val out = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val safeStroke = brushSizePx.coerceIn(4f, min(w, h).toFloat().coerceAtLeast(16f))
        val blurRadius = ((1f - hardness.coerceIn(0f, 1f)) * safeStroke * 0.45f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = safeStroke
            if (blurRadius > 0.5f) {
                maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            }
            if (isErase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                val alignedOrig = if (originalSource.width == w && originalSource.height == h) {
                    originalSource
                } else {
                    ImageProcessor.scaleBitmapHighQuality(originalSource, w, h)
                }
                shader = BitmapShader(alignedOrig, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
        }

        if (normalizedPoints.size == 1) {
            val (nx, ny) = normalizedPoints[0]
            val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(nx * w, ny * h, safeStroke / 2f, fillPaint)
        } else {
            val path = Path()
            val (sx, sy) = normalizedPoints[0]
            path.moveTo(sx * w, sy * h)
            for (i in 1 until normalizedPoints.size) {
                val (nx, ny) = normalizedPoints[i]
                path.lineTo(nx * w, ny * h)
            }
            canvas.drawPath(path, paint)
        }
        return out
    }

    fun applyShapeCutout(
        originalSource: Bitmap,
        shape: CutoutShape,
        invert: Boolean
    ): Bitmap {
        val w = originalSource.width
        val h = originalSource.height
        val fW = w.toFloat()
        val fH = h.toFloat()
        val minDim = min(fW, fH)
        val cx = fW / 2f
        val cy = fH / 2f
        val r = minDim * 0.44f

        val path = Path()
        when (shape) {
            CutoutShape.CIRCLE -> {
                path.addCircle(cx, cy, r, Path.Direction.CW)
            }
            CutoutShape.SQUARE -> {
                path.addRect(cx - r, cy - r, cx + r, cy + r, Path.Direction.CW)
            }
            CutoutShape.ROUNDED -> {
                path.addRoundRect(
                    RectF(cx - r, cy - r, cx + r, cy + r),
                    r * 0.28f,
                    r * 0.28f,
                    Path.Direction.CW
                )
            }
            CutoutShape.OVAL -> {
                path.addOval(
                    RectF(fW * 0.08f, fH * 0.08f, fW * 0.92f, fH * 0.92f),
                    Path.Direction.CW
                )
            }
            CutoutShape.TRIANGLE -> {
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r * 0.95f, cy + r * 0.85f)
                path.lineTo(cx - r * 0.95f, cy + r * 0.85f)
                path.close()
            }
            CutoutShape.STAR -> {
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) r else r * 0.42f
                    val angle = Math.toRadians((i * 36.0) - 90.0)
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            CutoutShape.HEART -> {
                val left = cx - r
                val top = cy - r * 0.92f
                val size = r * 2f
                path.moveTo(cx, top + size * 0.88f)
                path.cubicTo(
                    left + size * 0.05f, top + size * 0.60f,
                    left + size * 0.02f, top + size * 0.16f,
                    left + size * 0.30f, top + size * 0.14f
                )
                path.cubicTo(
                    left + size * 0.42f, top + size * 0.13f,
                    cx - size * 0.02f, top + size * 0.24f,
                    cx, top + size * 0.34f
                )
                path.cubicTo(
                    cx + size * 0.02f, top + size * 0.24f,
                    left + size * 0.58f, top + size * 0.13f,
                    left + size * 0.70f, top + size * 0.14f
                )
                path.cubicTo(
                    left + size * 0.98f, top + size * 0.16f,
                    left + size * 0.95f, top + size * 0.60f,
                    cx, top + size * 0.88f
                )
                path.close()
            }
            CutoutShape.HEXAGON -> {
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60.0) - 90.0)
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
            }
            CutoutShape.DIAMOND -> {
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r * 0.82f, cy)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - r * 0.82f, cy)
                path.close()
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (!invert) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, maskPaint)
            val srcIn = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(originalSource, 0f, 0f, srcIn)
        } else {
            canvas.drawBitmap(originalSource, 0f, 0f, null)
            val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            canvas.drawPath(path, clearPaint)
        }
        return out
    }
}
