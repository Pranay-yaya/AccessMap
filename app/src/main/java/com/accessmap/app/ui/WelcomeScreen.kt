package com.accessmap.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun WelcomeScreen(navController: NavHostController) {
    val settings = LocalAppSettings.current
    val haptic = LocalHapticFeedback.current
    var showOnboardingPopup by remember { mutableStateOf(!settings.onboardingSeen) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    if (showOnboardingPopup) {
        AlertDialog(
            onDismissRequest = {
                settings.markOnboardingSeen()
                showOnboardingPopup = false
            },
            icon = { Text("♿", fontSize = 32.sp) },
            title = { Text("Welcome to AccessMap LPU!", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick tips to get around campus:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("• Tap \"Use My GPS\" to start routing from the nearest surveyed building.", fontSize = 13.sp)
                    Text("• Switch between Step-free and Shortest routes anytime.", fontSize = 13.sp)
                    Text("• Tap any building pin for ramp, elevator & photo details.", fontSize = 13.sp)
                    Text("• Use Accessibility Settings for Voice Guidance, Large Text & High Contrast.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settings.markOnboardingSeen()
                        showOnboardingPopup = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Got It, Let's Go!")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Greeting Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF1976D2),
                                Color(0xFF2196F3)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                // Top quick-access buttons (Tips & Accessibility Settings)
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { showOnboardingPopup = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Onboarding Tips", tint = Color.White)
                    }
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = "Accessibility and About", tint = Color.White)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                ) {
                    // App Emblem Badge
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("♿", fontSize = 36.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "AccessMap LPU",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = " Lovely Professional University ",
                            color = Color(0xFF212121),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Welcome to a barrier-free campus. Designed to empower students, staff, and visitors with independent, wheelchair-friendly navigation.",
                        color = Color(0xFFE3F2FD),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Animated Core Accessibility Capabilities
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 6 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Key Campus Features",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    WelcomeFeatureCard(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = "Barrier-Free & Hazard-Aware Routing",
                        description = "Dijkstra routing avoids stairs, steep slopes, and active community hazard reports, with voice guidance.",
                        iconColor = Color(0xFF1565C0),
                        bgColor = Color(0xFFE3F2FD)
                    )

                    WelcomeFeatureCard(
                        icon = Icons.Default.Apartment,
                        title = "47 Surveyed Campus Blocks",
                        description = "Exact GPS locations, search & favorites, offline campus tiles, entrance ramps, elevators, and stair counts.",
                        iconColor = Color(0xFF2E7D32),
                        bgColor = Color(0xFFE8F5E9)
                    )

                    WelcomeFeatureCard(
                        icon = Icons.Default.CameraAlt,
                        title = "AI Edge Hazard Scanner",
                        description = "CameraX + LiteRT classifies ramps, stairs, and obstacles on-device and pins confirmed detections to the map.",
                        iconColor = Color(0xFF7B1FA2),
                        bgColor = Color(0xFFF3E5F5)
                    )

                    WelcomeFeatureCard(
                        icon = Icons.Default.ReportProblem,
                        title = "Community Hazard Reports & Photos",
                        description = "Attach camera photos, confirm 'Still There' or 'Resolved', and auto-expire old alerts.",
                        iconColor = Color(0xFFC62828),
                        bgColor = Color(0xFFFFEBEE)
                    )
                }
            }
        }

        // Bottom Fixed Action Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate("home")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Start Routing & Explore",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("map")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Open Campus Map Directly",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    bgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
