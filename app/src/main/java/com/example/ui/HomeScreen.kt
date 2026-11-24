package com.example.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.model.ProjectItem
import com.example.processing.ImageProcessor
import com.example.ui.camera.CameraCaptureDialog
import com.example.ui.setup.PageSetupDialog
import com.example.ui.theme.AccentRed
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.createCutoutProjectFromPhoto
import com.example.viewmodel.openWithPhotoDirectly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onOpenEditor: (hasCreatedPage: Boolean) -> Unit,
    onOpenPrivacyPolicy: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var pendingBgRemoveMode by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    ImageProcessor.decodeSampledBitmap(context, uri, maxDimension = 2048)
                }
                if (bmp != null) {
                    if (pendingBgRemoveMode) {
                        pendingBgRemoveMode = false
                        viewModel.createCutoutProjectFromPhoto(bmp, "Cutout Photo")
                    } else {
                        viewModel.openWithPhotoDirectly(bmp, "Imported Photo")
                    }
                    onOpenEditor(false)
                } else {
                    pendingBgRemoveMode = false
                    localError = "Failed to load selected image"
                }
            }
        } else {
            pendingBgRemoveMode = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderSection()
            }

            if (localError != null) {
                item {
                    Text(localError!!, color = AccentRed, fontSize = 12.sp)
                }
            }

            item {
                PrimaryActionsSection(
                    onNewProject = { showNewProjectDialog = true },
                    onOpenImage = {
                        pendingBgRemoveMode = false
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onTakePhoto = { showCameraDialog = true },
                    onRemoveBg = {
                        pendingBgRemoveMode = true
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            item {
                SampleTemplatesSection(
                    onSelectPreset = { key ->
                        viewModel.loadSamplePreset(key)
                        onOpenEditor(false)
                    }
                )
            }

            if (uiState.recentProjects.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Projects",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(uiState.recentProjects, key = { it.id }) { proj ->
                    RecentProjectCard(
                        project = proj,
                        onOpen = {
                            viewModel.loadExistingProject(proj.id)
                            onOpenEditor(true)
                        },
                        onDelete = { viewModel.deleteRecentProject(proj.id) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Easy Photo Editor v${BuildConfig.VERSION_NAME}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.testTag("app_version_label")
                        )
                        Text(
                            text = "Ismail Nurul Islam • Bangladesh",
                            color = TextSecondary.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenPrivacyPolicy,
                        modifier = Modifier.testTag("privacy_policy_button")
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy & Contact", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        PageSetupDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreatePage = { w, h, transparent, bgColor, name ->
                showNewProjectDialog = false
                viewModel.createNewProject(w, h, transparent, bgColor, name, dpi = 300)
                onOpenEditor(true)
            },
            onCreatePageWithUnits = { w, h, transparent, bgColor, name, dpi ->
                showNewProjectDialog = false
                viewModel.createNewProject(w, h, transparent, bgColor, name, dpi = dpi)
                onOpenEditor(true)
            }
        )
    }

    if (showCameraDialog) {
        CameraCaptureDialog(
            onDismiss = { showCameraDialog = false },
            onPhotoCaptured = { bitmap ->
                showCameraDialog = false
                viewModel.openWithPhotoDirectly(bitmap, "Camera Photo")
                onOpenEditor(false)
            }
        )
    }
}

@Composable
fun HeaderSection() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(PrimaryBlue.copy(alpha = 0.22f), SecondaryTeal.copy(alpha = 0.14f))
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Easy Photo Editor Logo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Easy Photo Editor",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(SecondaryTeal.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = SecondaryTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Precision Crop • Custom DPI Canvas • Layers • AI Cutout",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PrimaryActionsSection(
    onNewProject: () -> Unit,
    onOpenImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveBg: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeActionCard(
                title = "New Page / Project",
                subtitle = "Custom px/cm & 300 DPI",
                icon = Icons.Default.AddBox,
                accentColor = PrimaryBlue,
                onClick = onNewProject,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_new_project_card")
            )
            HomeActionCard(
                title = "Open Photo",
                subtitle = "Edit directly from gallery",
                icon = Icons.Default.PhotoLibrary,
                accentColor = SecondaryTeal,
                onClick = onOpenImage,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_open_image_card")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeActionCard(
                title = "Camera Capture",
                subtitle = "Shoot & edit instantly",
                icon = Icons.Default.CameraAlt,
                accentColor = Color(0xFF8B5CF6),
                onClick = onTakePhoto,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_camera_card")
            )
            HomeActionCard(
                title = "AI Remove BG",
                subtitle = "Instant transparent cutout",
                icon = Icons.Default.AutoFixHigh,
                accentColor = Color(0xFFF59E0B),
                onClick = onRemoveBg,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_bg_remover_card")
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
fun SampleTemplatesSection(
    onSelectPreset: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Try Sample Photos (Instant 1-Click Test)",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSelectPreset("landscape") },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sample_landscape_button")
                ) {
                    Text("Landscape Sample", fontSize = 12.sp, color = TextPrimary)
                }
                Button(
                    onClick = { onSelectPreset("portrait") },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sample_portrait_button")
                ) {
                    Text("Portrait Sample", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun RecentProjectCard(
    project: ProjectItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val thumb = remember(project.thumbnailPath) {
        project.thumbnailPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    val dateStr = remember(project.lastModified) {
        SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(project.lastModified))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .testTag("recent_project_${project.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = "Project Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${project.canvasWidth} × ${project.canvasHeight} px • ${project.layerCount} layers",
                    color = SecondaryTeal,
                    fontSize = 11.sp
                )
                Text(dateStr, color = TextSecondary, fontSize = 10.sp)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_recent_project_${project.id}")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Project", tint = AccentRed)
            }
        }
    }
}
