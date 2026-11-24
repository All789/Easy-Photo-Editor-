package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

enum class StickerCategory(val label: String) {
    ALL("All"),
    TICKS("Ticks & Badges"),
    ARROWS("Arrows & Callouts"),
    EMOJI("Emoji"),
    DECOR("Decor"),
    BANGLA("Bangla")
}

data class StickerSpec(
    val id: String,
    val label: String,
    val category: StickerCategory
)

object StickerGenerator {

    val allStickers: List<StickerSpec> = listOf(
        // Ticks & Badges
        StickerSpec("tick_circle", "Green ✓", StickerCategory.TICKS),
        StickerSpec("tick_gold", "Verified ✓", StickerCategory.TICKS),
        StickerSpec("cross_red", "Cross ✗", StickerCategory.TICKS),
        StickerSpec("badge_approved", "Approved", StickerCategory.TICKS),
        StickerSpec("badge_verified", "Verified", StickerCategory.TICKS),
        StickerSpec("badge_new", "NEW ★", StickerCategory.TICKS),
        StickerSpec("badge_special", "Special", StickerCategory.TICKS),
        StickerSpec("badge_100", "100%", StickerCategory.TICKS),

        // Arrows & Callouts
        StickerSpec("arrow_red_right", "Red Arrow", StickerCategory.ARROWS),
        StickerSpec("arrow_green_up", "Green Arrow", StickerCategory.ARROWS),
        StickerSpec("arrow_yellow_down", "Pointer", StickerCategory.ARROWS),
        StickerSpec("callout_hi", "HI!", StickerCategory.ARROWS),
        StickerSpec("callout_wow", "WOW!", StickerCategory.ARROWS),

        // Emoji & Reactions
        StickerSpec("emoji_heart_eyes", "😍", StickerCategory.EMOJI),
        StickerSpec("emoji_fire", "🔥", StickerCategory.EMOJI),
        StickerSpec("emoji_heart", "❤️", StickerCategory.EMOJI),
        StickerSpec("emoji_thumbs_up", "👍", StickerCategory.EMOJI),
        StickerSpec("emoji_party", "🎉", StickerCategory.EMOJI),
        StickerSpec("emoji_sparkles", "✨", StickerCategory.EMOJI),
        StickerSpec("emoji_cool", "😎", StickerCategory.EMOJI),
        StickerSpec("emoji_crown", "👑", StickerCategory.EMOJI),
        StickerSpec("emoji_star", "🌟", StickerCategory.EMOJI),
        StickerSpec("emoji_bouquet", "💐", StickerCategory.EMOJI),

        // Decor & Vector Icons
        StickerSpec("decor_star", "Gold Star", StickerCategory.DECOR),
        StickerSpec("decor_heart", "Red Heart", StickerCategory.DECOR),
        StickerSpec("decor_crown", "Royal Crown", StickerCategory.DECOR),
        StickerSpec("decor_sparkles", "Sparkles", StickerCategory.DECOR),
        StickerSpec("decor_crescent", "Moon & Star", StickerCategory.DECOR),
        StickerSpec("decor_ribbon", "Ribbon", StickerCategory.DECOR),

        // Bangla Stamps
        StickerSpec("bangla_dhonnobad", "ধন্যবাদ", StickerCategory.BANGLA),
        StickerSpec("bangla_shuvokamona", "শুভকামনা", StickerCategory.BANGLA),
        StickerSpec("bangla_darun", "✓ দারুণ!", StickerCategory.BANGLA),
        StickerSpec("bangla_valobasha", "♥ ভালোবাসা", StickerCategory.BANGLA)
    )

    private val previewCache = mutableMapOf<String, Bitmap>()

    fun getPreviewBitmap(id: String): Bitmap {
        return previewCache.getOrPut(id) { createStickerBitmap(id, 160) }
    }

    fun createStickerBitmap(id: String, sizePx: Int = 512): Bitmap {
        val s = sizePx.coerceIn(64, 1024)
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val f = s.toFloat()

        when (id) {
            "tick_circle" -> drawBadgeCheck(canvas, f, Color.parseColor("#22C55E"), Color.parseColor("#15803D"))
            "tick_gold" -> drawStarburstCheck(canvas, f)
            "cross_red" -> drawBadgeCross(canvas, f)
            "badge_approved" -> drawStampPill(canvas, f, "✓ APPROVED", Color.parseColor("#16A34A"), -10f)
            "badge_verified" -> drawStampPill(canvas, f, "✓ VERIFIED", Color.parseColor("#2563EB"), 0f)
            "badge_new" -> drawStampPill(canvas, f, "★ NEW", Color.parseColor("#EA580C"), -6f)
            "badge_special" -> drawStampPill(canvas, f, "SPECIAL", Color.parseColor("#DB2777"), 0f)
            "badge_100" -> drawSealText(canvas, f, "100%", Color.parseColor("#DC2626"), Color.parseColor("#FACC15"))

            "arrow_red_right" -> drawArrow(canvas, f, Color.parseColor("#EF4444"), 0f)
            "arrow_green_up" -> drawArrow(canvas, f, Color.parseColor("#22C55E"), -45f)
            "arrow_yellow_down" -> drawArrow(canvas, f, Color.parseColor("#FACC15"), 90f)
            "callout_hi" -> drawSpeechBubble(canvas, f, "HI!", Color.parseColor("#3B82F6"))
            "callout_wow" -> drawSpeechBubble(canvas, f, "WOW!", Color.parseColor("#EC4899"))

            "emoji_heart_eyes" -> drawEmoji(canvas, f, "😍")
            "emoji_fire" -> drawEmoji(canvas, f, "🔥")
            "emoji_heart" -> drawEmoji(canvas, f, "❤️")
            "emoji_thumbs_up" -> drawEmoji(canvas, f, "👍")
            "emoji_party" -> drawEmoji(canvas, f, "🎉")
            "emoji_sparkles" -> drawEmoji(canvas, f, "✨")
            "emoji_cool" -> drawEmoji(canvas, f, "😎")
            "emoji_crown" -> drawEmoji(canvas, f, "👑")
            "emoji_star" -> drawEmoji(canvas, f, "🌟")
            "emoji_bouquet" -> drawEmoji(canvas, f, "💐")

            "decor_star" -> drawGoldStar(canvas, f)
            "decor_heart" -> drawVectorHeart(canvas, f)
            "decor_crown" -> drawCrown(canvas, f)
            "decor_sparkles" -> drawSparkleCluster(canvas, f)
            "decor_crescent" -> drawCrescentMoon(canvas, f)
            "decor_ribbon" -> drawRibbonBanner(canvas, f)

            "bangla_dhonnobad" -> drawStampPill(canvas, f, "ধন্যবাদ", Color.parseColor("#0D9488"), 0f)
            "bangla_shuvokamona" -> drawStampPill(canvas, f, "শুভকামনা", Color.parseColor("#7C3AED"), 0f)
            "bangla_darun" -> drawStampPill(canvas, f, "✓ দারুণ!", Color.parseColor("#16A34A"), -6f)
            "bangla_valobasha" -> drawStampPill(canvas, f, "♥ ভালোবাসা", Color.parseColor("#E11D48"), 0f)
            else -> drawEmoji(canvas, f, "★")
        }
        return bmp
    }

    private fun drawBadgeCheck(canvas: Canvas, s: Float, c1: Int, c2: Int) {
        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.41f
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, c1, c2, Shader.TileMode.CLAMP)
            setShadowLayer(s * 0.04f, 0f, s * 0.02f, Color.argb(120, 0, 0, 0))
        }
        canvas.drawCircle(cx, cy, r, circlePaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.04f
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, r - s * 0.02f, borderPaint)

        val checkPath = Path().apply {
            moveTo(s * 0.29f, s * 0.51f)
            lineTo(s * 0.44f, s * 0.66f)
            lineTo(s * 0.72f, s * 0.35f)
        }
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.11f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun drawStarburstCheck(canvas: Canvas, s: Float) {
        val cx = s / 2f
        val cy = s / 2f
        val path = Path()
        val points = 16
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) s * 0.44f else s * 0.35f
            val angle = Math.toRadians((i * 180.0 / points) - 90.0)
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, Color.parseColor("#FACC15"), Color.parseColor("#EA580C"), Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, paint)

        val checkPath = Path().apply {
            moveTo(s * 0.33f, s * 0.52f)
            lineTo(s * 0.45f, s * 0.64f)
            lineTo(s * 0.69f, s * 0.38f)
        }
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.095f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun drawBadgeCross(canvas: Canvas, s: Float) {
        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.41f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, Color.parseColor("#EF4444"), Color.parseColor("#B91C1C"), Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx, cy, r, paint)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.1f
            strokeCap = Paint.Cap.ROUND
            color = Color.WHITE
        }
        canvas.drawLine(s * 0.34f, s * 0.34f, s * 0.66f, s * 0.66f, strokePaint)
        canvas.drawLine(s * 0.66f, s * 0.34f, s * 0.34f, s * 0.66f, strokePaint)
    }

    private fun drawStampPill(canvas: Canvas, s: Float, text: String, bgColor: Int, angleDeg: Float) {
        canvas.save()
        canvas.rotate(angleDeg, s / 2f, s / 2f)
        val rect = RectF(s * 0.08f, s * 0.32f, s * 0.92f, s * 0.68f)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            setShadowLayer(s * 0.03f, 0f, s * 0.015f, Color.argb(120, 0, 0, 0))
        }
        canvas.drawRoundRect(rect, s * 0.10f, s * 0.10f, bgPaint)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            color = Color.WHITE
        }
        canvas.drawRoundRect(
            RectF(rect.left + s * 0.02f, rect.top + s * 0.02f, rect.right - s * 0.02f, rect.bottom - s * 0.02f),
            s * 0.08f,
            s * 0.08f,
            border
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = if (text.length > 8) s * 0.13f else s * 0.16f
        }
        val textY = s / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, s / 2f, textY, textPaint)
        canvas.restore()
    }

    private fun drawSealText(canvas: Canvas, s: Float, label: String, bgColor: Int, borderColor: Int) {
        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.42f
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas.drawCircle(cx, cy, r, bg)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.045f
            color = borderColor
        }
        canvas.drawCircle(cx, cy, r * 0.86f, ring)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = s * 0.24f
        }
        val textY = cy - (tp.descent() + tp.ascent()) / 2f
        canvas.drawText(label, cx, textY, tp)
    }

    private fun drawArrow(canvas: Canvas, s: Float, color: Int, angleDeg: Float) {
        canvas.save()
        canvas.rotate(angleDeg, s / 2f, s / 2f)
        val path = Path().apply {
            moveTo(s * 0.12f, s * 0.40f)
            lineTo(s * 0.56f, s * 0.40f)
            lineTo(s * 0.56f, s * 0.22f)
            lineTo(s * 0.88f, s * 0.50f)
            lineTo(s * 0.56f, s * 0.78f)
            lineTo(s * 0.56f, s * 0.60f)
            lineTo(s * 0.12f, s * 0.60f)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            setShadowLayer(s * 0.03f, 0f, s * 0.02f, Color.argb(120, 0, 0, 0))
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.03f
            strokeJoin = Paint.Join.ROUND
            this.color = Color.WHITE
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
        canvas.restore()
    }

    private fun drawSpeechBubble(canvas: Canvas, s: Float, text: String, bgColor: Int) {
        val bubble = Path().apply {
            addRoundRect(RectF(s * 0.12f, s * 0.18f, s * 0.88f, s * 0.70f), s * 0.14f, s * 0.14f, Path.Direction.CW)
            moveTo(s * 0.30f, s * 0.69f)
            lineTo(s * 0.22f, s * 0.86f)
            lineTo(s * 0.46f, s * 0.69f)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            setShadowLayer(s * 0.03f, 0f, s * 0.015f, Color.argb(100, 0, 0, 0))
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.03f
            color = Color.WHITE
        }
        canvas.drawPath(bubble, fill)
        canvas.drawPath(bubble, border)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = s * 0.21f
        }
        val ty = s * 0.44f - (tp.descent() + tp.ascent()) / 2f
        canvas.drawText(text, s / 2f, ty, tp)
    }

    private fun drawEmoji(canvas: Canvas, s: Float, emoji: String) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = s * 0.72f
            textAlign = Paint.Align.CENTER
        }
        val y = s / 2f - (tp.descent() + tp.ascent()) / 2f
        canvas.drawText(emoji, s / 2f, y, tp)
    }

    private fun drawGoldStar(canvas: Canvas, s: Float) {
        val cx = s / 2f
        val cy = s / 2f
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) s * 0.43f else s * 0.18f
            val angle = Math.toRadians((i * 36.0) - 90.0)
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, Color.parseColor("#FDE047"), Color.parseColor("#F59E0B"), Shader.TileMode.CLAMP)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(path, p)
        canvas.drawPath(path, stroke)
    }

    private fun drawVectorHeart(canvas: Canvas, s: Float) {
        val path = Path().apply {
            moveTo(s * 0.50f, s * 0.84f)
            cubicTo(s * 0.10f, s * 0.58f, s * 0.08f, s * 0.22f, s * 0.31f, s * 0.20f)
            cubicTo(s * 0.42f, s * 0.19f, s * 0.48f, s * 0.27f, s * 0.50f, s * 0.34f)
            cubicTo(s * 0.52f, s * 0.27f, s * 0.58f, s * 0.19f, s * 0.69f, s * 0.20f)
            cubicTo(s * 0.92f, s * 0.22f, s * 0.90f, s * 0.58f, s * 0.50f, s * 0.84f)
            close()
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, Color.parseColor("#FB7185"), Color.parseColor("#E11D48"), Shader.TileMode.CLAMP)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            color = Color.WHITE
        }
        canvas.drawPath(path, p)
        canvas.drawPath(path, stroke)
    }

    private fun drawCrown(canvas: Canvas, s: Float) {
        val path = Path().apply {
            moveTo(s * 0.14f, s * 0.72f)
            lineTo(s * 0.10f, s * 0.32f)
            lineTo(s * 0.32f, s * 0.50f)
            lineTo(s * 0.50f, s * 0.22f)
            lineTo(s * 0.68f, s * 0.50f)
            lineTo(s * 0.90f, s * 0.32f)
            lineTo(s * 0.86f, s * 0.72f)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, Color.parseColor("#FDE047"), Color.parseColor("#D97706"), Shader.TileMode.CLAMP)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
    }

    private fun drawSparkleCluster(canvas: Canvas, s: Float) {
        drawFourPointStar(canvas, s * 0.45f, s * 0.45f, s * 0.28f, Color.parseColor("#FDE047"))
        drawFourPointStar(canvas, s * 0.74f, s * 0.30f, s * 0.14f, Color.parseColor("#38BDF8"))
        drawFourPointStar(canvas, s * 0.72f, s * 0.70f, s * 0.16f, Color.parseColor("#F472B6"))
    }

    private fun drawFourPointStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx, cy - r)
            quadTo(cx, cy, cx + r, cy)
            quadTo(cx, cy, cx, cy + r)
            quadTo(cx, cy, cx - r, cy)
            quadTo(cx, cy, cx, cy - r)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun drawCrescentMoon(canvas: Canvas, s: Float) {
        val moonPath = Path().apply {
            addCircle(s * 0.44f, s * 0.50f, s * 0.32f, Path.Direction.CW)
        }
        val cutPath = Path().apply {
            addCircle(s * 0.56f, s * 0.44f, s * 0.27f, Path.Direction.CW)
        }
        moonPath.op(cutPath, Path.Op.DIFFERENCE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, s, s, Color.parseColor("#FEF08A"), Color.parseColor("#F59E0B"), Shader.TileMode.CLAMP)
        }
        canvas.drawPath(moonPath, paint)
        drawFourPointStar(canvas, s * 0.72f, s * 0.34f, s * 0.12f, Color.parseColor("#FDE047"))
    }

    private fun drawRibbonBanner(canvas: Canvas, s: Float) {
        val rect = RectF(s * 0.10f, s * 0.35f, s * 0.90f, s * 0.65f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, s * 0.35f, s, s * 0.65f, Color.parseColor("#EC4899"), Color.parseColor("#9333EA"), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, p)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = s * 0.025f
            color = Color.WHITE
        }
        canvas.drawRoundRect(rect, s * 0.08f, s * 0.08f, stroke)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = s * 0.14f
        }
        canvas.drawText("★ BEST ★", s / 2f, s / 2f - (tp.descent() + tp.ascent()) / 2f, tp)
    }
}
