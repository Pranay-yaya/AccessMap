package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.accessmap.app.routing.LpuCampusData

@Composable
fun BuildingInfoScreen(navController: NavHostController, buildingId: String) {
    val context = LocalContext.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val node = remember(buildingId) { graph.nodeById(buildingId) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text("Building Info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                .padding(16.dp),
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
                        .background(Color(0xFF1565C0))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(node.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Accessibility Score: ${node.accessibilityScore}/10",
                            color = Color(0xFFBBDEFB),
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

            // Description
            if (node.description.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ℹ️ About", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(node.description, fontSize = 14.sp, color = Color.DarkGray)
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
                    Text("Latitude:  ${node.latitude}", fontSize = 13.sp, color = Color.DarkGray)
                    Text("Longitude: ${node.longitude}", fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            // Show on Map button
            Button(
                onClick = { navController.navigate("map/${buildingId}/${buildingId}") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("📍 Show on Map", color = Color.White, fontWeight = FontWeight.Bold)
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
            Text(label, fontSize = 14.sp, color = Color.DarkGray)
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
