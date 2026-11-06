package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.routing.LpuCampusData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val allLocations = remember { graph.nodes }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromId by remember { mutableStateOf("") }
    var selectedToId by remember { mutableStateOf("") }

    val selectedFromName = allLocations.find { it.id == selectedFromId }?.name ?: ""
    val selectedToName = allLocations.find { it.id == selectedToId }?.name ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // App Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "♿ AccessMap LPU",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Barrier-free Smart Navigation for Lovely Professional University",
                    color = Color(0xFFBBDEFB),
                    fontSize = 13.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Route Planning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 Quick Accessible Routing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    Text("Start Location:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFromName.ifEmpty { "Select origin on campus" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.TripOrigin, null, tint = Color(0xFF1565C0)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            allLocations.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text(node.name, fontSize = 13.sp) },
                                    onClick = {
                                        selectedFromId = node.id
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Destination:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedToName.ifEmpty { "Select destination" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFFE53935)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            allLocations.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text(node.name, fontSize = 13.sp) },
                                    onClick = {
                                        selectedToId = node.id
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (selectedFromId.isNotEmpty() && selectedToId.isNotEmpty()) {
                                navController.navigate("navigate/$selectedFromId/$selectedToId")
                            } else {
                                navController.navigate("navigate")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculate Accessible Route", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Actions 2x2 Grid
            Text("Campus Tools", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Map,
                    label = "Campus Map",
                    subtitle = "All 47 Pins & Paths",
                    color = Color(0xFF1976D2),
                    onClick = { navController.navigate("map") }
                )
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsRun,
                    label = "Navigate",
                    subtitle = "Turn-by-turn path",
                    color = Color(0xFF2E7D32),
                    onClick = { navController.navigate("navigate") }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    label = "AI Scanner",
                    subtitle = "LiteRT Camera",
                    color = Color(0xFF7B1FA2),
                    onClick = { navController.navigate("scanner") }
                )
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Report,
                    label = "Report Hazard",
                    subtitle = "Crowdsource fixes",
                    color = Color(0xFFC62828),
                    onClick = { navController.navigate("report") }
                )
            }

            // Campus Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏫 LPU Surveyed Infrastructure", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(10.dp))
                    val accessibleCount = graph.buildings.count { it.hasRamp }
                    val elevatorCount = graph.buildings.count { it.hasElevator }
                    StatItemRow("🏛️ Total Surveyed Nodes", "${graph.nodes.size} locations")
                    StatItemRow("♿ Ramp Accessible Buildings", "$accessibleCount of ${graph.buildings.size}")
                    StatItemRow("🛗 Elevator Equipped Blocks", "$elevatorCount buildings")
                    StatItemRow("🗺️ Mapped Campus Pathways", "${graph.edges.size} edges")
                    StatItemRow("📍 Map Engine", "OSMDroid (OpenStreetMap)")
                }
            }

            // Helpful Accessibility Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Accessibility Notice", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                        Text(
                            "Architecture Block (B6) front entrance has 24 stairs. Use the rear ramp entrance or the west corridor from B8.",
                            fontSize = 12.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun HomeActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.DarkGray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
    }
}
