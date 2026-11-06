package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHazardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val locations = remember { graph.nodes.map { it.name } }

    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var locationExpanded by remember { mutableStateOf(false) }
    var selectedHazardType by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    val hazardTypes = listOf(
        "🚧 Broken Ramp",
        "💧 Wet / Slippery Floor",
        "🏗️ Construction Blocking Path",
        "🪨 Uneven Surface",
        "🔆 Poor Lighting",
        "🚪 Inaccessible Entrance",
        "🛗 Elevator Out of Service",
        "⚠️ Other Hazard"
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC62828))
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Icon(Icons.Default.Report, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Report Accessibility Hazard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        if (submitted) {
            // Success state
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Report Submitted!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Thank you for helping improve campus accessibility. Your report has been recorded.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Back to Home", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Help keep LPU accessible for everyone. Report hazards so they can be fixed quickly.",
                            fontSize = 13.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                // Location picker
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📍 Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = locationExpanded,
                            onExpandedChange = { locationExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedLocation ?: "Select location on campus",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = locationExpanded,
                                onDismissRequest = { locationExpanded = false }
                            ) {
                                locations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc, fontSize = 13.sp) },
                                        onClick = {
                                            selectedLocation = loc
                                            locationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Hazard type selection
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ Hazard Type", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        hazardTypes.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { hazard ->
                                    val isSelected = selectedHazardType == hazard
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedHazardType = hazard },
                                        label = { Text(hazard, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFE3F2FD),
                                            selectedLabelColor = Color(0xFF1565C0)
                                        )
                                    )
                                }
                                // Fill empty slot if odd
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Description
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📝 Description (optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe the hazard in detail...", fontSize = 13.sp) },
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }

                // Submit button
                Button(
                    onClick = {
                        if (selectedLocation != null && selectedHazardType != null) {
                            submitted = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedLocation != null && selectedHazardType != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Report, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Hazard Report", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (selectedLocation == null || selectedHazardType == null) {
                    Text(
                        "Please select a location and hazard type to submit.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
