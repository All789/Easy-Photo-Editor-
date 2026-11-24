package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.CanvasDimensions
import com.example.model.FilterSettings
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.ProjectItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ProjectRepository(private val context: Context) {

    fun getProjectsDir(): File {
        return File(context.filesDir, "projects").apply { mkdirs() }
    }

    suspend fun saveProject(
        projectId: String,
        name: String,
        dimensions: CanvasDimensions,
        layers: List<Layer>,
        thumbnail: Bitmap?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(getProjectsDir(), projectId).apply { mkdirs() }
            val json = JSONObject().apply {
                put("id", projectId)
                put("name", name)
                put("lastModified", System.currentTimeMillis())
                put("canvasWidth", dimensions.width)
                put("canvasHeight", dimensions.height)
                put("isTransparent", dimensions.isTransparent)
                put("backgroundColor", dimensions.backgroundColor)

                val layersArray = JSONArray()
                layers.forEachIndexed { index, layer ->
                    val layerObj = JSONObject().apply {
                        put("id", layer.id)
                        put("name", layer.name)
                        put("type", layer.type.name)
                        put("x", layer.x.toDouble())
                        put("y", layer.y.toDouble())
                        put("width", layer.width.toDouble())
                        put("height", layer.height.toDouble())
                        put("rotation", layer.rotation.toDouble())
                        put("scaleX", layer.scaleX.toDouble())
                        put("scaleY", layer.scaleY.toDouble())
                        put("opacity", layer.opacity.toDouble())
                        put("isVisible", layer.isVisible)
                        put("isLocked", layer.isLocked)
                        put("text", layer.text)
                        put("textColor", layer.textColor)
                        put("fontSize", layer.fontSize.toDouble())
                        put("fontFamily", layer.fontFamily)
                        put("isBold", layer.isBold)
                        put("isItalic", layer.isItalic)
                        put("textAlignment", layer.textAlignment)
                        put("letterSpacing", layer.letterSpacing.toDouble())
                        put("hasShadow", layer.hasShadow)
                        put("shadowColor", layer.shadowColor)
                        put("shadowRadius", layer.shadowRadius.toDouble())
                        put("shadowOffsetX", layer.shadowOffsetX.toDouble())
                        put("shadowOffsetY", layer.shadowOffsetY.toDouble())
                        put("hasOutline", layer.hasOutline)
                        put("outlineColor", layer.outlineColor)
                        put("outlineWidth", layer.outlineWidth.toDouble())
                        put("hasBackground", layer.hasBackground)
                        put("backgroundColor", layer.backgroundColor)
                        put("backgroundCornerRadius", layer.backgroundCornerRadius.toDouble())
                        put("textAlpha", layer.textAlpha.toDouble())

                        val filterObj = JSONObject().apply {
                            put("brightness", layer.filters.brightness.toDouble())
                            put("contrast", layer.filters.contrast.toDouble())
                            put("saturation", layer.filters.saturation.toDouble())
                            put("grayscale", layer.filters.grayscale)
                            put("sepia", layer.filters.sepia)
                            put("hueShift", layer.filters.hueShift.toDouble())
                            put("temperature", layer.filters.temperature.toDouble())
                            put("blurRadius", layer.filters.blurRadius.toDouble())
                            put("sharpen", layer.filters.sharpen.toDouble())
                        }
                        put("filters", filterObj)
                    }

                    if (layer.type == LayerType.IMAGE && layer.bitmap != null) {
                        val bmpFile = File(dir, "layer_${index}.png")
                        FileOutputStream(bmpFile).use { fos ->
                            layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                        layerObj.put("bitmapFile", bmpFile.name)
                    }
                    if (layer.type == LayerType.IMAGE && layer.originalBitmap != null) {
                        val origFile = File(dir, "layer_orig_${index}.png")
                        FileOutputStream(origFile).use { fos ->
                            layer.originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                        layerObj.put("originalBitmapFile", origFile.name)
                    }
                    layersArray.put(layerObj)
                }
                put("layers", layersArray)
            }

            File(dir, "project.json").writeText(json.toString())

            if (thumbnail != null && !thumbnail.isRecycled) {
                val thumbFile = File(dir, "thumbnail.png")
                FileOutputStream(thumbFile).use { fos ->
                    thumbnail.compress(Bitmap.CompressFormat.PNG, 90, fos)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadProject(projectId: String): Pair<CanvasDimensions, List<Layer>>? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(getProjectsDir(), projectId)
                val jsonFile = File(dir, "project.json")
                if (!jsonFile.exists()) return@withContext null

                val root = JSONObject(jsonFile.readText())
                val dimensions = CanvasDimensions(
                    width = root.optInt("canvasWidth", 1080),
                    height = root.optInt("canvasHeight", 1080),
                    isTransparent = root.optBoolean("isTransparent", false),
                    backgroundColor = root.optLong("backgroundColor", 0xFFFFFFFFL)
                )

                val layersJson = root.optJSONArray("layers") ?: JSONArray()
                val layers = mutableListOf<Layer>()
                for (i in 0 until layersJson.length()) {
                    val obj = layersJson.getJSONObject(i)
                    val bitmapFile = obj.optString("bitmapFile", "")
                    val bmp = if (bitmapFile.isNotEmpty()) {
                        File(dir, bitmapFile).takeIf { it.exists() }?.let {
                            BitmapFactory.decodeFile(it.absolutePath)
                        }
                    } else null

                    val origFile = obj.optString("originalBitmapFile", "")
                    val origBmp = if (origFile.isNotEmpty()) {
                        File(dir, origFile).takeIf { it.exists() }?.let {
                            BitmapFactory.decodeFile(it.absolutePath)
                        }
                    } else bmp

                    val fObj = obj.optJSONObject("filters")
                    val filters = if (fObj != null) {
                        FilterSettings(
                            brightness = fObj.optDouble("brightness", 0.0).toFloat(),
                            contrast = fObj.optDouble("contrast", 1.0).toFloat(),
                            saturation = fObj.optDouble("saturation", 1.0).toFloat(),
                            grayscale = fObj.optBoolean("grayscale", false),
                            sepia = fObj.optBoolean("sepia", false),
                            hueShift = fObj.optDouble("hueShift", 0.0).toFloat(),
                            temperature = fObj.optDouble("temperature", 0.0).toFloat(),
                            blurRadius = fObj.optDouble("blurRadius", 0.0).toFloat(),
                            sharpen = fObj.optDouble("sharpen", 0.0).toFloat()
                        )
                    } else FilterSettings()

                    val typeName = obj.optString("type", LayerType.IMAGE.name)
                    val layerType = runCatching { LayerType.valueOf(typeName) }.getOrDefault(LayerType.IMAGE)

                    layers.add(
                        Layer(
                            id = obj.optString("id"),
                            name = obj.optString("name", "Layer ${i + 1}"),
                            type = layerType,
                            bitmap = bmp,
                            originalBitmap = origBmp,
                            x = obj.optDouble("x", 0.0).toFloat(),
                            y = obj.optDouble("y", 0.0).toFloat(),
                            width = obj.optDouble("width", 500.0).toFloat(),
                            height = obj.optDouble("height", 500.0).toFloat(),
                            rotation = obj.optDouble("rotation", 0.0).toFloat(),
                            scaleX = obj.optDouble("scaleX", 1.0).toFloat(),
                            scaleY = obj.optDouble("scaleY", 1.0).toFloat(),
                            opacity = obj.optDouble("opacity", 1.0).toFloat(),
                            isVisible = obj.optBoolean("isVisible", true),
                            isLocked = obj.optBoolean("isLocked", false),
                            text = obj.optString("text", ""),
                            textColor = obj.optLong("textColor", 0xFFFFFFFFL),
                            fontSize = obj.optDouble("fontSize", 48.0).toFloat(),
                            fontFamily = obj.optString("fontFamily", "Default"),
                            isBold = obj.optBoolean("isBold", false),
                            isItalic = obj.optBoolean("isItalic", false),
                            textAlignment = obj.optString("textAlignment", "center"),
                            letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat(),
                            hasShadow = obj.optBoolean("hasShadow", false),
                            shadowColor = obj.optLong("shadowColor", 0x80000000L),
                            shadowRadius = obj.optDouble("shadowRadius", 4.0).toFloat(),
                            shadowOffsetX = obj.optDouble("shadowOffsetX", 2.0).toFloat(),
                            shadowOffsetY = obj.optDouble("shadowOffsetY", 2.0).toFloat(),
                            hasOutline = obj.optBoolean("hasOutline", false),
                            outlineColor = obj.optLong("outlineColor", 0xFF000000L),
                            outlineWidth = obj.optDouble("outlineWidth", 3.0).toFloat(),
                            hasBackground = obj.optBoolean("hasBackground", false),
                            backgroundColor = obj.optLong("backgroundColor", 0xAA000000L),
                            backgroundCornerRadius = obj.optDouble("backgroundCornerRadius", 12.0).toFloat(),
                            textAlpha = obj.optDouble("textAlpha", 1.0).toFloat(),
                            filters = filters
                        )
                    )
                }
                dimensions to layers
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    suspend fun listProjects(): List<ProjectItem> = withContext(Dispatchers.IO) {
        val dirs = getProjectsDir().listFiles()?.filter { it.isDirectory } ?: return@withContext emptyList()
        dirs.mapNotNull { dir ->
            val jsonFile = File(dir, "project.json")
            if (!jsonFile.exists()) return@mapNotNull null
            try {
                val obj = JSONObject(jsonFile.readText())
                val thumbFile = File(dir, "thumbnail.png")
                ProjectItem(
                    id = obj.optString("id", dir.name),
                    name = obj.optString("name", "Untitled Project"),
                    lastModified = obj.optLong("lastModified", dir.lastModified()),
                    canvasWidth = obj.optInt("canvasWidth", 1080),
                    canvasHeight = obj.optInt("canvasHeight", 1080),
                    layerCount = obj.optJSONArray("layers")?.length() ?: 0,
                    thumbnailPath = thumbFile.takeIf { it.exists() }?.absolutePath
                )
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.lastModified }
    }

    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(getProjectsDir(), projectId).deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadDrawableAsBitmap(resId: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
