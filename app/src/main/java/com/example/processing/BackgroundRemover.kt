package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

object BackgroundRemover {

    enum class BackdropType {
        TRANSPARENT,
        SOLID_COLOR,
        GRADIENT,
        BLUR_ORIGINAL
    }

    suspend fun removeBackgroundAi(
        bitmap: Bitmap,
        smoothing: Float = 0.5f
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val mask = Tasks.await(segmenter.process(inputImage), 15L, TimeUnit.SECONDS)

            val w = bitmap.width
            val h = bitmap.height
            val maskW = mask.width
            val maskH = mask.height
            val buffer = mask.buffer
            buffer.rewind()
            val maskFloats = FloatArray(maskW * maskH)
            buffer.asFloatBuffer().get(maskFloats)

            val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

            val lowerThreshold = (0.25f - smoothing * 0.15f).coerceIn(0.05f, 0.4f)
            val upperThreshold = (smoothing * 0.15f + 0.75f).coerceIn(0.6f, 0.95f)
            val rampRange = maxOf(0.01f, upperThreshold - lowerThreshold)

            for (y in 0 until h) {
                val maskY = ((y.toFloat() / h) * maskH).toInt().coerceIn(0, maskH - 1)
                val rowOffset = y * w
                val maskRowOffset = maskY * maskW
                for (x in 0 until w) {
                    val maskX = ((x.toFloat() / w) * maskW).toInt().coerceIn(0, maskW - 1)
                    val confidence = maskFloats[maskRowOffset + maskX]
                    val alphaFactor = when {
                        confidence >= upperThreshold -> 1f
                        confidence > lowerThreshold -> ((confidence - lowerThreshold) / rampRange).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    val origColor = pixels[rowOffset + x]
                    val origAlpha = (origColor ushr 24) and 0xFF
                    val newAlpha = (origAlpha * alphaFactor).toInt().coerceIn(0, 255)
                    pixels[rowOffset + x] = (newAlpha shl 24) or (origColor and 0x00FFFFFF)
                }
            }
            outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
            segmenter.close()
            outBitmap
        } catch (_: Throwable) {
            removeBackgroundAlgorithmic(bitmap, 0.25f)
        }
    }

    fun removeBackgroundAlgorithmic(
        bitmap: Bitmap,
        tolerance: Float = 0.25f
    ): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val topLeft = bitmap.getPixel(0, 0)
        val topRight = bitmap.getPixel((w - 1).coerceAtLeast(0), 0)
        val bottomLeft = bitmap.getPixel(0, (h - 1).coerceAtLeast(0))
        val avgR = (Color.red(topLeft) + Color.red(topRight) + Color.red(bottomLeft)) / 3
        val avgG = (Color.green(topLeft) + Color.green(topRight) + Color.green(bottomLeft)) / 3
        val avgB = (Color.blue(topLeft) + Color.blue(topRight) + Color.blue(bottomLeft)) / 3
        val estimatedBg = Color.rgb(avgR, avgG, avgB)
        return removeBackgroundByColor(bitmap, estimatedBg, tolerance, 0.2f)
    }

    fun removeBackgroundByColor(
        bitmap: Bitmap,
        targetColor: Int,
        tolerance: Float = 0.2f,
        feather: Float = 0.15f
    ): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val targetR = Color.red(targetColor)
        val targetG = Color.green(targetColor)
        val targetB = Color.blue(targetColor)
        val maxDist = 441.67f
        val hardThreshold = tolerance * maxDist
        val softThreshold = (tolerance + feather) * maxDist
        val softSpan = maxOf(1f, softThreshold - hardThreshold)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val origAlpha = (c ushr 24) and 0xFF

            val dr = (r - targetR).toFloat()
            val dg = (g - targetG).toFloat()
            val db = (b - targetB).toFloat()
            val dist = sqrt(dr * dr + dg * dg + db * db)

            val newAlpha = when {
                dist <= hardThreshold -> 0
                dist <= softThreshold -> {
                    val ratio = ((dist - hardThreshold) / softSpan).coerceIn(0f, 1f)
                    (origAlpha * ratio).toInt().coerceIn(0, 255)
                }
                else -> origAlpha
            }
            pixels[i] = (newAlpha shl 24) or (c and 0x00FFFFFF)
        }
        outBitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return outBitmap
    }

    fun applyBackdrop(
        cutoutBitmap: Bitmap,
        originalBitmap: Bitmap?,
        backdropType: BackdropType,
        solidColor: Long = 0xFFFFFFFFL,
        gradientStart: Long = 0xFF1E3A8AL,
        gradientEnd: Long = 0xFF38BDF8L,
        blurRadius: Float = 15f
    ): Bitmap {
        val w = cutoutBitmap.width
        val h = cutoutBitmap.height
        val outBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (backdropType) {
            BackdropType.TRANSPARENT -> Unit
            BackdropType.SOLID_COLOR -> {
                canvas.drawColor(solidColor.toInt())
            }
            BackdropType.GRADIENT -> {
                paint.shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    gradientStart.toInt(),
                    gradientEnd.toInt(),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
                paint.shader = null
            }
            BackdropType.BLUR_ORIGINAL -> {
                val base = originalBitmap?.let {
                    if (it.width == w && it.height == h) it
                    else ImageProcessor.scaleBitmapHighQuality(it, w, h)
                } ?: cutoutBitmap
                val blurred = ImageProcessor.fastBoxBlur(base, blurRadius.toInt().coerceIn(1, 25))
                canvas.drawBitmap(blurred, 0f, 0f, paint)
            }
        }
        canvas.drawBitmap(cutoutBitmap, 0f, 0f, paint)
        return outBitmap
    }
}
