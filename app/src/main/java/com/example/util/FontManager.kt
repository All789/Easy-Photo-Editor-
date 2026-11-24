package com.example.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

data class FontItem(
    val id: String,
    val displayName: String,
    val isCustom: Boolean = false,
    val filePath: String? = null
)

object FontManager {
    private val customTypefaceCache = mutableMapOf<String, Typeface>()

    fun getDefaultBuiltInFonts(): List<FontItem> = listOf(
        FontItem(id = "Default", displayName = "Sans Serif (Default)"),
        FontItem(id = "Serif", displayName = "Classic Serif"),
        FontItem(id = "Monospace", displayName = "Monospace"),
        FontItem(id = "Noto Sans Bengali", displayName = "Noto Sans Bengali (বাংলা)"),
        FontItem(id = "Anek Bangla", displayName = "Anek Bangla (আধুনিক বাংলা)"),
        FontItem(id = "Galada", displayName = "Galada (শৈল্পিক বাংলা)")
    )

    fun getCustomFonts(context: Context): List<FontItem> {
        val fontsDir = File(context.filesDir, "custom_fonts")
        if (!fontsDir.exists() || !fontsDir.isDirectory) return emptyList()
        val files = fontsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".ttf", ignoreCase = true) ||
                file.name.endsWith(".otf", ignoreCase = true))
        } ?: return emptyList()

        return files.sortedBy { it.name.lowercase() }.map { file ->
            val displayName = file.nameWithoutExtension
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifEmpty { file.name }
            FontItem(
                id = "custom:${file.absolutePath}",
                displayName = displayName,
                isCustom = true,
                filePath = file.absolutePath
            )
        }
    }

    fun getAllFonts(context: Context): List<FontItem> {
        return getDefaultBuiltInFonts() + getCustomFonts(context)
    }

    fun importCustomFont(context: Context, uri: Uri): FontItem? {
        return try {
            val resolver = context.contentResolver
            var originalName = "custom_font_${System.currentTimeMillis()}.ttf"
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) {
                            originalName = name
                        }
                    }
                }
            }
            val safeName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalName = if (safeName.endsWith(".ttf", true) || safeName.endsWith(".otf", true)) {
                safeName
            } else {
                "$safeName.ttf"
            }

            val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val destFile = File(fontsDir, finalName)
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val tf = Typeface.createFromFile(destFile)
            val id = "custom:${destFile.absolutePath}"
            customTypefaceCache[id] = tf

            val displayName = destFile.nameWithoutExtension
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()
                .ifEmpty { destFile.name }

            FontItem(
                id = id,
                displayName = displayName,
                isCustom = true,
                filePath = destFile.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTypeface(
        context: Context?,
        fontFamilyId: String,
        isBold: Boolean = false,
        isItalic: Boolean = false
    ): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        val base = when {
            fontFamilyId.startsWith("custom:") -> {
                val path = fontFamilyId.removePrefix("custom:")
                customTypefaceCache[fontFamilyId] ?: run {
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            Typeface.createFromFile(file).also {
                                customTypefaceCache[fontFamilyId] = it
                            }
                        } catch (_: Exception) {
                            Typeface.DEFAULT
                        }
                    } else {
                        Typeface.DEFAULT
                    }
                }
            }
            fontFamilyId == "Serif" -> Typeface.SERIF
            fontFamilyId == "Monospace" -> Typeface.MONOSPACE
            fontFamilyId == "Noto Sans Bengali" && context != null ->
                runCatching { ResourcesCompat.getFont(context, R.font.noto_sans_bengali) }.getOrNull()
                    ?: Typeface.DEFAULT
            fontFamilyId == "Anek Bangla" && context != null ->
                runCatching { ResourcesCompat.getFont(context, R.font.anek_bangla) }.getOrNull()
                    ?: Typeface.DEFAULT
            fontFamilyId == "Galada" && context != null ->
                runCatching { ResourcesCompat.getFont(context, R.font.galada) }.getOrNull()
                    ?: Typeface.DEFAULT
            else -> Typeface.SANS_SERIF
        }

        return Typeface.create(base, style)
    }
}
