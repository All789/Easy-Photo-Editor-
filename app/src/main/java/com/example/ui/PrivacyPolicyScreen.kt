package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val DEVELOPER_NAME = "Ismail Nurul Islam"
private const val DEVELOPER_COUNTRY = "Bangladesh"
private const val DEVELOPER_EMAIL = "ranahossain82@gmail.com"
private const val DEVELOPER_WEBSITE = "https://ismailtgdeveloper.blogspot.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var senderName by remember { mutableStateOf("") }
    var messageSubject by remember { mutableStateOf("Easy Photo Editor Support") }
    var messageBody by remember { mutableStateOf("") }
    var formFeedback by remember { mutableStateOf<String?>(null) }

    fun openWebsite() {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_WEBSITE))
            context.startActivity(intent)
        }
    }

    fun openEmail(subject: String = "Easy Photo Editor Inquiry", body: String = "") {
        runCatching {
            val uri = Uri.parse(
                "mailto:$DEVELOPER_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
            )
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            context.startActivity(intent)
        }.onFailure {
            formFeedback = "Contact Email: $DEVELOPER_EMAIL"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy & Contact",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("privacy_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App & Compliance Header Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.privacy_policy_compliance_subtitle),
                        color = SecondaryTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} • Updated 2026",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Privacy Policy Sections
            PrivacyPolicySectionCard(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.privacy_policy_camera_title),
                body = "Camera access is requested only when you choose to capture a new photo inside the app. Captured images stay strictly on your device and are never transmitted or recorded in the background."
            )

            PrivacyPolicySectionCard(
                icon = Icons.Default.PhotoLibrary,
                title = stringResource(R.string.privacy_policy_storage_title),
                body = "Easy Photo Editor uses Android's privacy-friendly Photo Picker so you choose exactly which photos to edit without granting broad storage access. Exported photos and saved projects remain stored locally on your device."
            )

            PrivacyPolicySectionCard(
                icon = Icons.Default.VerifiedUser,
                title = stringResource(R.string.privacy_policy_on_device_title),
                body = "All editing features—including Crop, Transform, Filters, Cutout (Person, Face, Clothes, Sky, Head, Hair, Background), Brush/Erase, Stickers, and Text—run 100% locally on your device. Your personal photos are never uploaded to external servers."
            )

            PrivacyPolicySectionCard(
                icon = Icons.Default.Shield,
                title = stringResource(R.string.privacy_policy_no_tracking_title),
                body = "We do not sell, rent, or share your personal data. Easy Photo Editor contains zero hidden behavioral tracking and respects user privacy at every step."
            )

            // Developer Info Card (Pre-filled)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.testTag("developer_info_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SecondaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.privacy_policy_contact_title),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = BorderColor)

                    ContactDetailRow(
                        icon = Icons.Default.Person,
                        label = "Developer Name",
                        value = DEVELOPER_NAME
                    )
                    ContactDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Country",
                        value = DEVELOPER_COUNTRY
                    )
                    ContactDetailRow(
                        icon = Icons.Default.Email,
                        label = "Connect / Email",
                        value = DEVELOPER_EMAIL,
                        isLink = true,
                        onClick = { openEmail() }
                    )
                    ContactDetailRow(
                        icon = Icons.Default.Language,
                        label = "Website",
                        value = DEVELOPER_WEBSITE,
                        isLink = true,
                        onClick = { openWebsite() }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openEmail() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_email_developer")
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { openWebsite() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_website")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Website", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Interactive Contact Form Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.testTag("contact_form_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Contact Us / Feedback Form",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Send a message directly to $DEVELOPER_NAME ($DEVELOPER_EMAIL)",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("Your Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SecondaryTeal,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = messageSubject,
                        onValueChange = { messageSubject = it },
                        label = { Text("Subject", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_subject_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SecondaryTeal,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("Your Message", fontSize = 12.sp) },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_message_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = SecondaryTeal,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    if (formFeedback != null) {
                        Text(
                            text = formFeedback!!,
                            color = SecondaryTeal,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (messageBody.isBlank()) {
                                formFeedback = "Please write a brief message first."
                            } else {
                                val fullBody = buildString {
                                    if (senderName.isNotBlank()) append("From: $senderName\n")
                                    append("App: Easy Photo Editor v${BuildConfig.VERSION_NAME}\n\n")
                                    append(messageBody)
                                }
                                formFeedback = "Opening email app to $DEVELOPER_EMAIL"
                                openEmail(
                                    subject = messageSubject.ifBlank { "Easy Photo Editor Inquiry" },
                                    body = fullBody
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_send_contact_message")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Message", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_privacy_acknowledge")
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy_acknowledge),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PrivacyPolicySectionCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SecondaryTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = SecondaryTeal,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = body,
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ContactDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isLink) SecondaryTeal else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextSecondary, fontSize = 10.sp)
            Text(
                text = value,
                color = if (isLink) SecondaryTeal else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (isLink) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open",
                tint = SecondaryTeal,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
