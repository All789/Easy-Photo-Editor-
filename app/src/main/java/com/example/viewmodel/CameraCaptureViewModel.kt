package com.example.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CameraCaptureUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val showGrid: Boolean = true,
    val capturedBitmap: Bitmap? = null,
    val isCapturing: Boolean = false,
    val errorMessage: String? = null
)

class CameraCaptureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraCaptureUiState())
    val uiState: StateFlow<CameraCaptureUiState> = _uiState.asStateFlow()

    fun toggleLensFacing() {
        _uiState.update { state ->
            val next = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(lensFacing = next)
        }
    }

    fun cycleFlashMode() {
        _uiState.update { state ->
            val next = when (state.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            state.copy(flashMode = next)
        }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun setCapturing(capturing: Boolean) {
        _uiState.update { it.copy(isCapturing = capturing, errorMessage = null) }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _uiState.update { it.copy(capturedBitmap = bitmap, isCapturing = false, errorMessage = null) }
    }

    fun retakePhoto() {
        _uiState.update { it.copy(capturedBitmap = null, isCapturing = false, errorMessage = null) }
    }

    fun setError(message: String?) {
        _uiState.update { it.copy(errorMessage = message, isCapturing = false) }
    }

    fun processCapturedBitmap(raw: Bitmap, isFrontCamera: Boolean, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        if (isFrontCamera) {
            matrix.postScale(-1f, 1f)
        }
        return if (rotationDegrees != 0 || isFrontCamera) {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        } else {
            raw
        }
    }
}
