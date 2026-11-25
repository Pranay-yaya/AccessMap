package com.accessmap.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun SettingsAboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val voice = LocalVoiceGuidance.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Icon(
                Icons.Default.AccessibilityNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Accessibility & About",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Display & Accessibility Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("♿ Accessibility & Display", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))

                    SettingsSwitchRow(
                        icon = Icons.Default.RecordVoiceOver,
                        title = "Voice Guidance (Text-to-Speech)",
                        subtitle = "Speak turn-by-turn directions, re-routing, and arrival alerts",
                        checked = settings.voiceGuidance,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settings.toggleVoiceGuidance(enabled)
                            if (enabled) voice.speak("Voice guidance enabled.", true)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsSwitchRow(
                        icon = Icons.Default.FormatSize,
                        title = "Large Text Mode",
                        subtitle = "Increase text size across buttons, cards, and directions",
                        checked = settings.largeText,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settings.toggleLargeText(it)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsSwitchRow(
                        icon = Icons.Default.Contrast,
                        title = "High-Contrast Mode",
                        subtitle = "Stronger color contrast for low-vision readability",
                        checked = settings.highContrast,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settings.toggleHighContrast(it)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    SettingsSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Reduce glare during evening campus navigation",
                        checked = settings.darkTheme,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            settings.toggleDarkTheme(it)
                        }
                    )
                }
            }

            // Free Google Colab AI Model Training Guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧠 Train Custom Campus AI (Free Google Colab)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "You can train a custom LiteRT (TFLite) classifier on a free Google Colab GPU using your own LPU photos:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Collect photos in 4 folders: ramp/, stairs/, obstacle/, clear_path/\n" +
                            "2. In Colab, train MobileNetV3 / TFLite Model Maker and export accessmap_model.tflite\n" +
                            "3. Drop accessmap_model.tflite into app/src/main/assets/ — the AI Scanner picks it up automatically!",
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // About AccessMap LPU
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ℹ️ About AccessMap LPU", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AccessMap (CSE226) is an open-source, zero-API-key accessible navigation app built for Lovely Professional University. Powered by OSMDroid, Room DB, CameraX 1.4.2, LiteRT, and built-in Android TextToSpeech.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_SUBJECT, "AccessMap LPU Feedback")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Feedback / Campus Accessibility Update for AccessMap LPU:\n\nBuilding/Location:\nDetails:"
                                    )
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, "Send Feedback")) }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Feedback", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val repoIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Pranay-yaya/AccessMap")
                                )
                                runCatching { context.startActivity(repoIntent) }
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GitHub", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { contentDescription = "$title. $subtitle" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
        )
    }
}
