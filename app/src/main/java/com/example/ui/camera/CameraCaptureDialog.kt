package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CameraCaptureViewModel

@Composable
fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraCaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .fillMaxHeight(0.76f)
            ) {
                if (!hasCameraPermission) {
                    CameraPermissionRequestView(
                        onGrantClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onCancelClick = onDismiss
                    )
                } else if (uiState.capturedBitmap != null) {
                    val captured = uiState.capturedBitmap!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Photo Preview",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("camera_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Image(
                            bitmap = captured.asImageBitmap(),
                            contentDescription = "Captured photo preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.retakePhoto() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_retake_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retake", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onPhotoCaptured(captured) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("camera_use_photo_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Photo", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    val previewView = remember {
                        PreviewView(context).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    }

                    DisposableEffect(uiState.lensFacing, uiState.flashMode, lifecycleOwner) {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        val executor = ContextCompat.getMainExecutor(context)
                        cameraProviderFuture.addListener({
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val capture = ImageCapture.Builder()
                                    .setFlashMode(uiState.flashMode)
                                    .build()
                                imageCapture = capture

                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(uiState.lensFacing)
                                    .build()
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                            }.onFailure {
                                viewModel.setError("Camera unavailable: ${it.message}")
                            }
                        }, executor)

                        onDispose {
                            runCatching {
                                ProcessCameraProvider.getInstance(context).get().unbindAll()
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Compact Top Camera Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("camera_close_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { viewModel.cycleFlashMode() },
                                    modifier = Modifier.testTag("camera_flash_toggle_button")
                                ) {
                                    val flashIcon = when (uiState.flashMode) {
                                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                        else -> Icons.Default.FlashOff
                                    }
                                    Icon(flashIcon, contentDescription = "Flash mode", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { viewModel.toggleGrid() },
                                    modifier = Modifier.testTag("camera_grid_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                        contentDescription = "Toggle Grid",
                                        tint = if (uiState.showGrid) SecondaryTeal else Color.White
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.toggleLensFacing() },
                                    modifier = Modifier.testTag("camera_switch_lens_button")
                                ) {
                                    Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Lens", tint = Color.White)
                                }
                            }
                        }

                        // Compact Camera Viewfinder Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                        ) {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("camera_preview_view")
                            )
                            if (uiState.showGrid) {
                                RuleOfThirdsGrid(modifier = Modifier.fillMaxSize())
                            }
                        }

                        // Compact Bottom Shutter Bar (safely elevated inside centered card above system navigation bar)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .padding(vertical = 12.dp, horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.lensFacing == CameraSelector.LENS_FACING_BACK) "Back" else "Front",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .border(3.5.dp, Color.White, CircleShape)
                                    .padding(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable(enabled = !uiState.isCapturing) {
                                        val capture = imageCapture ?: return@clickable
                                        viewModel.setCapturing(true)
                                        capture.takePicture(
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val buffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                    val rot = image.imageInfo.rotationDegrees
                                                    image.close()
                                                    if (raw != null) {
                                                        val processed = viewModel.processCapturedBitmap(
                                                            raw,
                                                            isFrontCamera = uiState.lensFacing == CameraSelector.LENS_FACING_FRONT,
                                                            rotationDegrees = rot
                                                        )
                                                        viewModel.onPhotoCaptured(processed)
                                                    } else {
                                                        viewModel.setError("Failed to decode photo")
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    viewModel.setError("Capture failed: ${exception.message}")
                                                }
                                            }
                                        )
                                    }
                                    .testTag("camera_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Take Photo",
                                    tint = DarkBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleLensFacing() },
                                modifier = Modifier.testTag("camera_switch_lens_bottom_button")
                            ) {
                                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Lens", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleOfThirdsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.dp.toPx()
        val c = Color.White.copy(alpha = 0.35f)
        drawLine(c, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
        drawLine(c, Offset(2f * size.width / 3f, 0f), Offset(2f * size.width / 3f, size.height), stroke)
        drawLine(c, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), stroke)
        drawLine(c, Offset(0f, 2f * size.height / 3f), Offset(size.width, 2f * size.height / 3f), stroke)
    }
}

@Composable
fun CameraPermissionRequestView(
    onGrantClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            "Camera Permission Required",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Allow camera access to take a photo directly inside Easy Photo Editor.",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.testTag("cancel_camera_permission_button")
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("grant_camera_permission_button")
            ) {
                Text("Allow Camera", color = Color.White)
            }
        }
    }
}
