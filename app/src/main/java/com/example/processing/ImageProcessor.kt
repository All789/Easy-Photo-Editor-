package com.example.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.exifinterface.media.ExifInterface
import com.example.model.CanvasDimensions
import com.example.model.CropRect
import com.example.model.FilterSettings
import com.example.model.Layer
import com.example.model.LayerType
import com.example.util.FontManager
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object ImageProcessor {

    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return null

            var inSampleSize = 1
            while (origW / inSampleSize > maxDimension * 2 || origH / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val orientation = resolver.openInputStream(uri)?.use { stream ->
                runCatching {
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val oriented = applyExifOrientation(decoded, orientation)
            if (max(oriented.width, oriented.height) > maxDimension) {
                val scale = maxDimension.toFloat() / max(oriented.width, oriented.height)
                val targetW = (oriented.width * scale).roundToInt().coerceAtLeast(1)
                val targetH = (oriented.height * scale).roundToInt().coerceAtLeast(1)
                scaleBitmapHighQuality(oriented, targetW, targetH)
            } else {
                oriented.copy(Bitmap.Config.ARGB_8888, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Crops [source] strictly by [crop] normalized coordinates without stretching pixels.
     */
    fun cropBitmap(source: Bitmap, crop: CropRect): Bitmap {
        val w = source.width
        val h = source.height
        val x = (crop.left * w).roundToInt().coerceIn(0, (w - 1).coerceAtLeast(0))
        val y = (crop.top * h).roundToInt().coerceIn(0, (h - 1).coerceAtLeast(0))
        val cropW = ((crop.right - crop.left) * w).roundToInt().coerceIn(1, w - x)
        val cropH = ((crop.bottom - crop.top) * h).roundToInt().coerceIn(1, h - y)
        return Bitmap.createBitmap(source, x, y, cropW, cropH)
    }

    /**
     * Crops [source] by [crop] normalized coordinates, and optionally resizes to exact
     * [targetWidth] x [targetHeight] using progressive high-quality filtered scaling.
     */
    fun cropBitmapToSize(
        source: Bitmap,
        crop: CropRect,
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Bitmap {
        val cropped = cropBitmap(source, crop)
        if (targetWidth != null && targetHeight != null && targetWidth > 0 && targetHeight > 0) {
            if (cropped.width != targetWidth || cropped.height != targetHeight) {
                return scaleBitmapHighQuality(cropped, targetWidth, targetHeight)
            }
        }
        return cropped
    }

    /**
     * High-quality progressive bitmap scaling (avoids aliasing artifacts on large downscales).
     */
    fun scaleBitmapHighQuality(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val safeW = targetWidth.coerceAtLeast(1)
        val safeH = targetHeight.coerceAtLeast(1)
        if (source.width == safeW && source.height == safeH) return source

        var current = source
        // Progressive 50% downsampling steps preserve sharpness when shrinking large photos
        while (current.width / 2 >= safeW && current.height / 2 >= safeH) {
            val nextW = (current.width / 2).coerceAtLeast(safeW)
            val nextH = (current.height / 2).coerceAtLeast(safeH)
            val step = Bitmap.createScaledBitmap(current, nextW, nextH, true)
            if (current != source) current.recycle()
            current = step
        }
        if (current.width == safeW && current.height == safeH) return current
        val finalBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        canvas.drawBitmap(
            current,
            Rect(0, 0, current.width, current.height),
            RectF(0f, 0f, safeW.toFloat(), safeH.toFloat()),
            paint
        )
        if (current != source) current.recycle()
        return finalBitmap
    }

    fun createColorMatrix(filters: FilterSettings): ColorMatrix {
        return ShaderFilterEngine.buildColorMatrix(filters)
    }

    fun createComposeColorMatrix(filters: FilterSettings): FloatArray {
        return ShaderFilterEngine.buildColorMatrix(filters).array
    }

    fun applyPixelFilters(source: Bitmap, filters: FilterSettings): Bitmap {
        return ShaderFilterEngine.processBitmapWithFilters(source, filters)
    }

    fun fastBoxBlur(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val w = out.width
        val h = out.height
        val pixels = IntArray(w * h)
        out.getPixels(pixels, 0, w, 0, 0, w, h)
        boxBlurHorizontal(pixels, w, h, r)
        boxBlurVertical(pixels, w, h, r)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlurHorizontal(pixels: IntArray, w: Int, h: Int, r: Int) {
        val row = IntArray(w)
        val div = r * 2 + 1
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                var a = 0
                var rd = 0
                var g = 0
                var b = 0
                for (dx in -r..r) {
                    val sx = (x + dx).coerceIn(0, w - 1)
                    val c = pixels[offset + sx]
                    a += c ushr 24
                    rd += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                }
                row[x] = ((a / div) shl 24) or ((rd / div) shl 16) or ((g / div) shl 8) or (b / div)
            }
            System.arraycopy(row, 0, pixels, offset, w)
        }
    }

    private fun boxBlurVertical(pixels: IntArray, w: Int, h: Int, r: Int) {
        val col = IntArray(h)
        val div = r * 2 + 1
        for (x in 0 until w) {
            for (y in 0 until h) {
                var a = 0
                var rd = 0
                var g = 0
                var b = 0
                for (dy in -r..r) {
                    val sy = (y + dy).coerceIn(0, h - 1)
                    val c = pixels[sy * w + x]
                    a += c ushr 24
                    rd += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                }
                col[y] = ((a / div) shl 24) or ((rd / div) shl 16) or ((g / div) shl 8) or (b / div)
            }
            for (y in 0 until h) {
                pixels[y * w + x] = col[y]
            }
        }
    }

    fun applySharpen(src: Bitmap, strength: Float): Bitmap {
        return ShaderFilterEngine.processBitmapWithFilters(src, FilterSettings(sharpen = strength))
    }

    fun renderComposition(
        dimensions: CanvasDimensions,
        layers: List<Layer>,
        context: Context? = null
    ): Bitmap {
        val outWidth = dimensions.width.coerceAtLeast(1)
        val outHeight = dimensions.height.coerceAtLeast(1)
        val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // Direct photo mode uses backgroundColor == 0L; only fill solid background when explicitly set.
        if (!dimensions.isTransparent && dimensions.backgroundColor != 0L) {
            canvas.drawColor(dimensions.backgroundColor.toInt())
        } else {
            canvas.drawColor(Color.TRANSPARENT)
        }

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }

        layers.filter { it.isVisible }.forEach { layer ->
            canvas.save()
            val centerX = layer.x + layer.width / 2f
            val centerY = layer.y + layer.height / 2f
            canvas.translate(centerX, centerY)
            if (layer.rotation != 0f) {
                canvas.rotate(layer.rotation)
            }
            canvas.scale(layer.scaleX, layer.scaleY)
            canvas.translate(-layer.width / 2f, -layer.height / 2f)

            when (layer.type) {
                LayerType.IMAGE -> {
                    val bmp = layer.bitmap
                    if (bmp != null && !bmp.isRecycled) {
                        imagePaint.alpha = (layer.opacity * 255f).roundToInt().coerceIn(0, 255)
                        imagePaint.colorFilter = ColorMatrixColorFilter(createColorMatrix(layer.filters))
                        canvas.drawBitmap(
                            bmp,
                            Rect(0, 0, bmp.width, bmp.height),
                            RectF(0f, 0f, layer.width, layer.height),
                            imagePaint
                        )
                    }
                }
                LayerType.TEXT -> {
                    drawTextLayer(canvas, layer, context)
                }
            }
            canvas.restore()
        }

        return outBitmap
    }

    private fun drawTextLayer(canvas: Canvas, layer: Layer, context: Context?) {
        if (layer.text.isEmpty()) return
        val typeface = FontManager.getTypeface(context, layer.fontFamily, layer.isBold, layer.isItalic)
        val combinedAlpha = (layer.opacity * layer.textAlpha * 255f).roundToInt().coerceIn(0, 255)
        val align = when (layer.textAlignment.lowercase()) {
            "left" -> Layout.Alignment.ALIGN_NORMAL
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.textColor.toInt()
            alpha = combinedAlpha
            textSize = layer.fontSize.coerceAtLeast(8f)
            this.typeface = typeface
            letterSpacing = layer.letterSpacing / 20f
            if (layer.hasShadow) {
                setShadowLayer(
                    layer.shadowRadius.coerceAtLeast(0.5f),
                    layer.shadowOffsetX,
                    layer.shadowOffsetY,
                    layer.shadowColor.toInt()
                )
            }
        }

        if (layer.hasBackground) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.backgroundColor.toInt()
                alpha = ((layer.backgroundColor ushr 24) and 0xFF).toInt()
            }
            val rect = RectF(0f, 0f, layer.width, layer.height)
            canvas.drawRoundRect(
                rect,
                layer.backgroundCornerRadius,
                layer.backgroundCornerRadius,
                bgPaint
            )
        }

        val layoutWidth = layer.width.roundToInt().coerceAtLeast(16)
        val textLayout = StaticLayout.Builder.obtain(
            layer.text,
            0,
            layer.text.length,
            paint,
            layoutWidth
        )
            .setAlignment(align)
            .setIncludePad(false)
            .build()

        val dy = ((layer.height - textLayout.height) / 2f).coerceAtLeast(0f)
        canvas.save()
        canvas.translate(0f, dy)

        if (layer.hasOutline) {
            val strokePaint = TextPaint(paint).apply {
                style = Paint.Style.STROKE
                strokeWidth = layer.outlineWidth.coerceAtLeast(1f)
                color = layer.outlineColor.toInt()
                clearShadowLayer()
            }
            val outlineLayout = StaticLayout.Builder.obtain(
                layer.text,
                0,
                layer.text.length,
                strokePaint,
                layoutWidth
            )
                .setAlignment(align)
                .setIncludePad(false)
                .build()
            outlineLayout.draw(canvas)
        }

        textLayout.draw(canvas)
        canvas.restore()
    }
}
