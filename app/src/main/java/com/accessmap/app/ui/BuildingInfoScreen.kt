package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.routing.LpuCampusData

@Composable
fun BuildingInfoScreen(navController: NavHostController, buildingId: String) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val voice = LocalVoiceGuidance.current
    val haptic = LocalHapticFeedback.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val node = remember(buildingId) { graph.nodeById(buildingId) }
    val isFav = node?.let { settings.isFavorite(it.id) } ?: false

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(
                "Building Info",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            if (node != null) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val rampText = if (node.hasRamp) "Wheelchair ramp available." else "No wheelchair ramp."
                        val elevText = if (node.hasElevator) "Elevator available." else "${node.stairCount} stairs."
                        voice.speak("${node.name}. Score ${node.accessibilityScore} out of 10. $rampText $elevText ${node.description}", true)
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, "Read building info aloud", tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        settings.toggleFavorite(node.id)
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFav) "Remove from favorites" else "Save to favorites",
                        tint = if (isFav) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        if (node == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Building not found")
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name / Score card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(node.name, color = MaterialTheme.colorScheme.onPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Accessibility Score: ${node.accessibilityScore}/10",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { node.accessibilityScore / 10f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = when {
                                node.accessibilityScore >= 8 -> Color(0xFF4CAF50)
                                node.accessibilityScore >= 5 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            },
                            trackColor = Color(0xFF0D47A1)
                        )
                    }
                }
            }

            // Survey notes / Description
            if (node.description.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ℹ️ Survey Notes & About", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(node.description, fontSize = 14.sp)
                    }
                }
            }

            // Accessibility details
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("♿ Accessibility Details", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    AccessibilityRow(
                        emoji = if (node.hasRamp) "✅" else "❌",
                        label = "Wheelchair Ramp",
                        value = if (node.hasRamp) "Available" else "Not Available",
                        valueColor = if (node.hasRamp) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AccessibilityRow(
                        emoji = if (node.hasElevator) "✅" else "❌",
                        label = "Elevator",
                        value = if (node.hasElevator) "Available" else "Not Available",
                        valueColor = if (node.hasElevator) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    AccessibilityRow(
                        emoji = "🪜",
                        label = "Number of Stairs",
                        value = if (node.stairCount == 0) "Ground Floor (No Stairs)" else "${node.stairCount} stairs",
                        valueColor = when {
                            node.stairCount == 0 -> Color(0xFF2E7D32)
                            node.stairCount < 10 -> Color(0xFF1565C0)
                            else -> Color(0xFFC62828)
                        }
                    )
                }
            }

            // Coordinates
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Latitude:  ${node.latitude}", fontSize = 13.sp)
                    Text("Longitude: ${node.longitude}", fontSize = 13.sp)
                }
            }

            // Show on Map button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("map/${buildingId}/${buildingId}")
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("📍 Show on Map", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AccessibilityRow(emoji: String, label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
