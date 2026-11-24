package com.example.viewmodel

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.processing.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Applies active crop selection.
 * Preserves project canvas dimensions when editing in custom project mode (isDirectPhotoMode = false),
 * or adapts canvas dimensions to the cropped photo when editing a gallery photo directly (isDirectPhotoMode = true).
 */
fun EditorViewModel.applyCropAndAdaptCanvas(
    isDirectPhotoMode: Boolean,
    fitToProjectCanvas: Boolean = true
) {
    applyCrop(isDirectPhotoMode = isDirectPhotoMode, fitToProjectCanvas = fitToProjectCanvas)
}

suspend fun saveToPhoneGallery(
    context: Context,
    viewModel: EditorViewModel,
    format: String,
    quality: Int
): Boolean = withContext(Dispatchers.IO) {
    try {
        val state = viewModel.uiState.value
        val bitmap = ImageProcessor.renderComposition(state.canvasDimensions, state.layers, context)
        val isPng = format.equals("PNG", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val mime = if (isPng) "image/png" else "image/jpeg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val fileName = "EasyPhotoEditor_${System.currentTimeMillis()}.$ext"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EasyPhotoEditor"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false

        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(compressFormat, quality.coerceIn(10, 100), out)
        } ?: return@withContext false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        viewModel.updateStateDirectly { it.copy(statusMessage = "Saved $fileName to Gallery") }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        viewModel.updateStateDirectly { it.copy(errorMessage = "Save failed: ${e.message}") }
        false
    }
}

suspend fun sharePhoto(
    context: Context,
    viewModel: EditorViewModel,
    format: String,
    quality: Int
) = withContext(Dispatchers.IO) {
    try {
        val state = viewModel.uiState.value
        val bitmap = ImageProcessor.renderComposition(state.canvasDimensions, state.layers, context)
        val isPng = format.equals("PNG", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val mime = if (isPng) "image/png" else "image/jpeg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(shareDir, "EasyPhotoEditor_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { out ->
            bitmap.compress(compressFormat, quality.coerceIn(10, 100), out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(Intent.createChooser(intent, "Share Edited Image"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        viewModel.updateStateDirectly { it.copy(errorMessage = "Share failed: ${e.message}") }
    }
}
