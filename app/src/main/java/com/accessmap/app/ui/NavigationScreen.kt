package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.database.AppDatabase
import com.accessmap.app.repository.AccessMapRepository
import com.accessmap.app.routing.AccessibilityRouter
import com.accessmap.app.routing.LpuCampusData
import com.accessmap.app.routing.RouteResult
import com.accessmap.app.routing.haversineDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    navController: NavHostController,
    initialFromId: String = "",
    initialToId: String = ""
) {
    val context = LocalContext.current
    val settings = LocalAppSettings.current
    val voice = LocalVoiceGuidance.current
    val haptic = LocalHapticFeedback.current

    val graph = remember { LpuCampusData.buildGraph(context) }
    val router = remember { AccessibilityRouter(graph) }
    val repo = remember { AccessMapRepository(AppDatabase.getInstance(context).hazardDao(), graph) }
    val activeHazards by repo.activeHazards().collectAsState(initial = emptyList())
    val allNodes = remember { graph.nodes }

    // Map active Room hazards to their nearest campus node so Dijkstra avoids blocked paths
    val blockedNodeIds = remember(activeHazards) {
        activeHazards.mapNotNull { h ->
            if (h.nodeId.isNotEmpty()) h.nodeId
            else graph.nodes.minByOrNull { n ->
                haversineDistance(h.latitude, h.longitude, n.latitude, n.longitude)
            }?.id
        }.toSet()
    }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromId by remember { mutableStateOf(initialFromId) }
    var selectedToId by remember { mutableStateOf(initialToId) }
    var accessibleOnly by remember { mutableStateOf(true) }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    var noRouteFound by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }

    val selectedFromName = allNodes.find { it.id == selectedFromId }?.name ?: "Select start location"
    val selectedToName = allNodes.find { it.id == selectedToId }?.name ?: "Select destination"

    fun computeRoute() {
        hasSearched = true
        noRouteFound = false
        routeResult = null
        if (selectedFromId.isEmpty() || selectedToId.isEmpty() || selectedFromId == selectedToId) return
        val result = if (accessibleOnly) {
            router.findAccessibleRoute(selectedFromId, selectedToId, blockedNodeIds)
        } else {
            router.findNormalRoute(selectedFromId, selectedToId, blockedNodeIds)
        }
        if (result == null) {
            noRouteFound = true
            if (settings.voiceGuidance) {
                voice.speak("No step-free route found. You can toggle off step-free only to see the shortest path.", true)
            }
        } else {
            routeResult = result
            val destName = graph.nodeById(selectedToId)?.name ?: selectedToId
            val mins = (result.totalDistanceMeters / 75.0).toInt().coerceAtLeast(1)
            if (settings.voiceGuidance) {
                voice.speak(
                    "Route ready to $destName. Distance ${result.totalDistanceMeters.toInt()} meters, about $mins minutes.",
                    true
                )
            }
        }
    }

    LaunchedEffect(initialFromId, initialToId, blockedNodeIds) {
        if (initialFromId.isNotEmpty() && initialToId.isNotEmpty() && initialFromId != initialToId) {
            computeRoute()
        }
    }

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
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Icon(Icons.Default.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text(
                "Accessible Route Planner",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Find Path at LPU", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))

                        // FROM Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("From:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
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
                                value = selectedFromName,
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
                                allNodes.forEach { node ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(node.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(
                                                    if (node.hasRamp) "♿ Ramp available" else "⚠️ Stairs without ramp",
                                                    fontSize = 11.sp,
                                                    color = if (node.hasRamp) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        },
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

                        // Swap Button
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val temp = selectedFromId
                                    selectedFromId = selectedToId
                                    selectedToId = temp
                                },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.SwapVert, "Swap points", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // TO Selector
                        Text("To:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        ExposedDropdownMenuBox(
                            expanded = toExpanded,
                            onExpandedChange = { toExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedToName,
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
                                allNodes.forEach { node ->
                                    val star = if (settings.isFavorite(node.id)) "⭐ " else ""
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("$star${node.name}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(
                                                    if (node.hasRamp) "♿ Ramp available" else "⚠️ Stairs without ramp",
                                                    fontSize = 11.sp,
                                                    color = if (node.hasRamp) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedToId = node.id
                                            toExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Step-free vs Shortest Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = accessibleOnly,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    accessibleOnly = it
                                    if (selectedFromId.isNotEmpty() && selectedToId.isNotEmpty()) computeRoute()
                                },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (accessibleOnly) "Step-free only (wheelchair safe)" else "Shortest route (may include stairs)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (accessibleOnly) "Avoids stairs, steep slopes & active hazards" else "Minimizes total walking distance",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                computeRoute()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = selectedFromId.isNotEmpty() && selectedToId.isNotEmpty() && selectedFromId != selectedToId,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsRun, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (accessibleOnly) "Find Step-Free Route" else "Find Shortest Route",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // No route found warning
            if (hasSearched && noRouteFound) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("No Step-Free Route Found", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text(
                                    "Stairs or obstacles block the accessible path. Switch to Shortest route to preview the standard path.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D4037)
                                )
                                Spacer(Modifier.height(6.dp))
                                TextButton(onClick = {
                                    accessibleOnly = false
                                    computeRoute()
                                }) {
                                    Text("Show shortest path anyway →", color = Color(0xFF1565C0))
                                }
                            }
                        }
                    }
                }
            }

            // Results Section
            routeResult?.let { result ->
                val distText = if (result.totalDistanceMeters >= 1000)
                    "%.1f km".format(result.totalDistanceMeters / 1000.0)
                else
                    "${result.totalDistanceMeters.toInt()} meters"
                val walkMin = (result.totalDistanceMeters / 75.0).toInt().coerceAtLeast(1)

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (accessibleOnly) "Step-Free Path Computed" else "Shortest Path Computed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val stepsText = result.path.mapIndexedNotNull { idx, id ->
                                            val name = graph.nodeById(id)?.name ?: id
                                            if (idx == 0) "Start at $name"
                                            else if (idx == result.path.lastIndex) "Arrive at $name"
                                            else "Continue past $name"
                                        }.joinToString(". ")
                                        voice.speak(stepsText, true)
                                    },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Speak turn-by-turn directions")
                                }
                            }
                            if (result.avoidedHazards) {
                                Text("🛡️ Hazard-aware: detoured around reported campus hazards", fontSize = 12.sp, color = Color(0xFF1565C0))
                            }
                            if (!accessibleOnly) {
                                Text("⚠️ Includes stairs or uneven segments", fontSize = 12.sp, color = Color(0xFFE65100))
                            }
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                RouteStatBadge("📍", distText, "Distance")
                                RouteStatBadge("⏱️", "~$walkMin min", "Est. Time")
                                RouteStatBadge("🏛️", "${result.path.size}", "Waypoints")
                            }

                            Spacer(Modifier.height(12.dp))

                            if (result.path.size >= 2) {
                                val startNode = graph.nodeById(result.path[0])
                                val nextNode = graph.nodeById(result.path[1])
                                if (startNode != null && nextNode != null) {
                                    val bearing = remember(startNode.id, nextNode.id) {
                                        calculateBearingDegrees(
                                            startNode.latitude, startNode.longitude,
                                            nextNode.latitude, nextNode.longitude
                                        )
                                    }
                                    RouteCompassCard(
                                        targetBearingDegrees = bearing,
                                        nextStopName = nextNode.name,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.navigate("map/$selectedFromId/$selectedToId")
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Map, null)
                                Spacer(Modifier.width(8.dp))
                                Text("View & Track Live on Map", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Turn-by-Turn Directions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                itemsIndexed(result.path) { index, nodeId ->
                    val node = graph.nodeById(nodeId)
                    val edge = result.edges.getOrNull(index)
                    val isLast = index == result.path.lastIndex

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        when {
                                            index == 0 -> Color(0xFF2E7D32)
                                            isLast -> Color(0xFFC62828)
                                            else -> Color(0xFF1565C0)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(40.dp)
                                        .background(Color(0xFF90CAF9))
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Card(
                            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = node?.name ?: nodeId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (edge != null) {
                                    val segDist = "${edge.distance.toInt()}m"
                                    val surf = edge.surface.name.lowercase().replaceFirstChar { it.uppercase() }
                                    Text(
                                        text = "Walk $segDist → ($surf surface${if (edge.hasStairs) ", ⚠️ Stairs" else ", ♿ Flat/Ramp"})",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                } else if (isLast) {
                                    Text("🎉 Destination Reached", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RouteStatBadge(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}
