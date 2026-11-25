package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.database.AppDatabase
import com.accessmap.app.repository.AccessMapRepository
import com.accessmap.app.routing.LpuCampusData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val haptic = LocalHapticFeedback.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val allLocations = remember { graph.nodes }
    val repo = remember { AccessMapRepository(AppDatabase.getInstance(context).hazardDao(), graph) }
    val activeHazards by repo.activeHazards().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromId by remember { mutableStateOf("") }
    var selectedToId by remember { mutableStateOf("") }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }

    val selectedFromName = allLocations.find { it.id == selectedFromId }?.name ?: ""
    val selectedToName = allLocations.find { it.id == selectedToId }?.name ?: ""

    val searchResults = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else allLocations.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.id.contains(q, ignoreCase = true) ||
                it.description.contains(q, ignoreCase = true)
        }.take(6)
    }

    val favoriteNodes = remember(settings.favoriteIds) {
        allLocations.filter { it.id in settings.favoriteIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "♿ AccessMap LPU",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Barrier-free Smart Navigation for LPU",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate("settings")
                },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    Icons.Default.AccessibilityNew,
                    contentDescription = "Accessibility Settings and About",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Building Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search 47 LPU blocks, CSE, Library, Gate...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search buildings") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Search Results", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                        searchResults.forEach { node ->
                            val isFav = settings.isFavorite(node.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedToId = node.id
                                        searchQuery = ""
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(node.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        if (node.hasRamp) "♿ Ramp · Score ${node.accessibilityScore}/10" else "⚠️ ${node.stairCount} stairs · Score ${node.accessibilityScore}/10",
                                        fontSize = 11.sp,
                                        color = if (node.hasRamp) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
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
                                        contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                                        tint = if (isFav) Color(0xFFFFB300) else Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { navController.navigate("building/${node.id}") },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Building Details", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            // Favorite Destinations Quick Chips
            if (favoriteNodes.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⭐ Favorite Destinations", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        favoriteNodes.forEach { fav ->
                            AssistChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedToId = fav.id
                                },
                                label = { Text(fav.id, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }

            // Quick Route Planning Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 Quick Accessible Routing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Start Location:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        UseMyGpsChip(
                            graph = graph,
                            onNodeResolved = { nodeId, msg ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFromId = nodeId
                                gpsStatusMessage = msg
                            },
                            onStatusMessage = { msg -> gpsStatusMessage = msg }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedFromName.ifEmpty { "Select origin on campus" },
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.TripOrigin, null, tint = MaterialTheme.colorScheme.primary) },
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
                                        gpsStatusMessage = null
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    gpsStatusMessage?.let { status ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            color = if (status.startsWith("📍")) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
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
                                    text = {
                                        val star = if (settings.isFavorite(node.id)) "⭐ " else ""
                                        Text("$star${node.name}", fontSize = 13.sp)
                                    },
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
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (selectedFromId.isNotEmpty() && selectedToId.isNotEmpty()) {
                                navController.navigate("navigate/$selectedFromId/$selectedToId")
                            } else {
                                navController.navigate("navigate")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Navigation, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculate Accessible Route", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Actions 2x2 Grid
            Text("Campus Tools", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Map,
                    label = "Campus Map",
                    subtitle = "Satellite & Offline",
                    color = Color(0xFF1976D2),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("map")
                    }
                )
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    label = "Navigate",
                    subtitle = "Voice & Re-route",
                    color = Color(0xFF2E7D32),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("navigate")
                    }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    label = "AI Scanner",
                    subtitle = "LiteRT Camera",
                    color = Color(0xFF7B1FA2),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("scanner")
                    }
                )
                HomeActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Report,
                    label = "Report Hazard",
                    subtitle = "${activeHazards.size} active alerts",
                    color = Color(0xFFC62828),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("report")
                    }
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
                    StatItemRow("⚠️ Active Community Hazards", "${activeHazards.size} reported")
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
        modifier = modifier.heightIn(min = 104.dp),
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
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
