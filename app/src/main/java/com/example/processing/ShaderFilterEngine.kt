package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.model.FilterSettings
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object ShaderFilterEngine {

    fun processBitmapWithFilters(source: Bitmap, filters: FilterSettings): Bitmap {
        val working = source.copy(Bitmap.Config.ARGB_8888, true)

        val hasColorMatrixChanges =
            filters.brightness != 0f ||
                filters.contrast != 1f ||
                filters.saturation != 1f ||
                filters.grayscale ||
                filters.sepia ||
                abs(filters.hueShift) > 0.5f ||
                abs(filters.temperature) > 0.5f

        var current = if (hasColorMatrixChanges) {
            val out = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val cm = buildColorMatrix(filters)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(working, 0f, 0f, paint)
            out
        } else {
            working
        }

        if (filters.blurRadius > 0.5f) {
            current = fastSeparableBlur(current, filters.blurRadius.roundToInt().coerceIn(1, 25))
        }

        if (filters.sharpen > 0.05f) {
            current = applyConvolutionSharpen(current, filters.sharpen.coerceIn(0f, 2f))
        }

        return current
    }

    fun buildColorMatrix(filters: FilterSettings): ColorMatrix {
        val cm = ColorMatrix()

        if (filters.grayscale) {
            cm.setSaturation(0f)
        } else if (filters.saturation != 1f) {
            cm.setSaturation(filters.saturation.coerceIn(0f, 3f))
        }

        if (filters.contrast != 1f || filters.brightness != 0f) {
            val c = filters.contrast.coerceIn(0.2f, 3f)
            val b = filters.brightness * 255f
            val translate = (-0.5f * c + 0.5f) * 255f + b
            val contrastMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, translate,
                    0f, c, 0f, 0f, translate,
                    0f, 0f, c, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(contrastMatrix)
        }

        if (abs(filters.temperature) > 0.5f) {
            val temp = (filters.temperature / 100f).coerceIn(-1f, 1f) * 40f
            val tempMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, temp,
                    0f, 1f, 0f, 0f, temp * 0.25f,
                    0f, 0f, 1f, 0f, -temp,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(tempMatrix)
        }

        if (abs(filters.hueShift) > 0.5f) {
            val hueRad = Math.toRadians(filters.hueShift.toDouble())
            val cosVal = cos(hueRad).toFloat()
            val sinVal = sin(hueRad).toFloat()
            val lumR = 0.213f
            val lumG = 0.715f
            val lumB = 0.072f
            val hueMatrix = ColorMatrix(
                floatArrayOf(
                    lumR + cosVal * (1f - lumR) + sinVal * (-lumR),
                    lumG + cosVal * (-lumG) + sinVal * (-lumG),
                    lumB + cosVal * (-lumB) + sinVal * (1f - lumB),
                    0f, 0f,
                    lumR + cosVal * (-lumR) + sinVal * 0.143f,
                    lumG + cosVal * (1f - lumG) + sinVal * 0.140f,
                    lumB + cosVal * (-lumB) + sinVal * (-0.283f),
                    0f, 0f,
                    lumR + cosVal * (-lumR) + sinVal * (-(1f - lumR)),
                    lumG + cosVal * (-lumG) + sinVal * lumG,
                    lumB + cosVal * (1f - lumB) + sinVal * lumB,
                    0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(hueMatrix)
        }

        if (filters.sepia) {
            val sepiaMatrix = ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(sepiaMatrix)
        }

        return cm
    }

    private fun applyConvolutionSharpen(source: Bitmap, strength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val total = width * height
        val srcPixels = IntArray(total)
        val dstPixels = IntArray(total)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val edge = -strength
        val center = 1f + 4f * strength

        for (y in 0 until height) {
            val yPrev = (y - 1).coerceAtLeast(0) * width
            val yCurr = y * width
            val yNext = (y + 1).coerceAtMost(height - 1) * width
            for (x in 0 until width) {
                val xPrev = (x - 1).coerceAtLeast(0)
                val xNext = (x + 1).coerceAtMost(width - 1)

                val pTop = srcPixels[yPrev + x]
                val pLeft = srcPixels[yCurr + xPrev]
                val pCenter = srcPixels[yCurr + x]
                val pRight = srcPixels[yCurr + xNext]
                val pBottom = srcPixels[yNext + x]

                val a = pCenter ushr 24
                val r = (((pCenter shr 16) and 0xFF) * center +
                    (((pTop shr 16) and 0xFF) + ((pLeft shr 16) and 0xFF) +
                        ((pRight shr 16) and 0xFF) + ((pBottom shr 16) and 0xFF)) * edge)
                    .roundToInt().coerceIn(0, 255)
                val g = (((pCenter shr 8) and 0xFF) * center +
                    (((pTop shr 8) and 0xFF) + ((pLeft shr 8) and 0xFF) +
                        ((pRight shr 8) and 0xFF) + ((pBottom shr 8) and 0xFF)) * edge)
                    .roundToInt().coerceIn(0, 255)
                val b = ((pCenter and 0xFF) * center +
                    ((pTop and 0xFF) + (pLeft and 0xFF) +
                        (pRight and 0xFF) + (pBottom and 0xFF)) * edge)
                    .roundToInt().coerceIn(0, 255)

                dstPixels[yCurr + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(dstPixels, 0, width, 0, 0, width, height)
        }
    }

    private fun fastSeparableBlur(source: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val width = source.width
        val height = source.height
        val total = width * height
        val inPixels = IntArray(total)
        val tempPixels = IntArray(total)
        val outPixels = IntArray(total)
        source.getPixels(inPixels, 0, width, 0, 0, width, height)

        val windowSize = r * 2 + 1
        val invWindow = 1f / windowSize

        for (y in 0 until height) {
            val rowStart = y * width
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0
            for (i in -r..r) {
                val p = inPixels[rowStart + i.coerceIn(0, width - 1)]
                aSum += p ushr 24
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (x in 0 until width) {
                tempPixels[rowStart + x] =
                    (((aSum * invWindow).toInt()) shl 24) or
                        (((rSum * invWindow).toInt()) shl 16) or
                        (((gSum * invWindow).toInt()) shl 8) or
                        ((bSum * invWindow).toInt())
                val pRemove = inPixels[rowStart + (x - r).coerceIn(0, width - 1)]
                val pAdd = inPixels[rowStart + (x + r + 1).coerceIn(0, width - 1)]
                aSum += (pAdd ushr 24) - (pRemove ushr 24)
                rSum += ((pAdd shr 16) and 0xFF) - ((pRemove shr 16) and 0xFF)
                gSum += ((pAdd shr 8) and 0xFF) - ((pRemove shr 8) and 0xFF)
                bSum += (pAdd and 0xFF) - (pRemove and 0xFF)
            }
        }

        for (x in 0 until width) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0
            for (i in -r..r) {
                val p = tempPixels[i.coerceIn(0, height - 1) * width + x]
                aSum += p ushr 24
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (y in 0 until height) {
                outPixels[y * width + x] =
                    (((aSum * invWindow).toInt()) shl 24) or
                        (((rSum * invWindow).toInt()) shl 16) or
                        (((gSum * invWindow).toInt()) shl 8) or
                        ((bSum * invWindow).toInt())
                val pRemove = tempPixels[(y - r).coerceIn(0, height - 1) * width + x]
                val pAdd = tempPixels[(y + r + 1).coerceIn(0, height - 1) * width + x]
                aSum += (pAdd ushr 24) - (pRemove ushr 24)
                rSum += ((pAdd shr 16) and 0xFF) - ((pRemove shr 16) and 0xFF)
                gSum += ((pAdd shr 8) and 0xFF) - ((pRemove shr 8) and 0xFF)
                bSum += (pAdd and 0xFF) - (pRemove and 0xFF)
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(outPixels, 0, width, 0, 0, width, height)
        }
    }
}
