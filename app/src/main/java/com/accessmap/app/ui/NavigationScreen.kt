package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.routing.AccessibilityRouter
import com.accessmap.app.routing.LpuCampusData
import com.accessmap.app.routing.RouteResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    navController: NavHostController,
    initialFromId: String = "",
    initialToId: String = ""
) {
    val context = LocalContext.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val router = remember { AccessibilityRouter(graph) }
    val allNodes = remember { graph.nodes }

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var selectedFromId by remember { mutableStateOf(initialFromId) }
    var selectedToId by remember { mutableStateOf(initialToId) }
    var accessibleOnly by remember { mutableStateOf(true) }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    var noRouteFound by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val selectedFromName = allNodes.find { it.id == selectedFromId }?.name ?: "Select start location"
    val selectedToName = allNodes.find { it.id == selectedToId }?.name ?: "Select destination"

    fun computeRoute() {
        hasSearched = true
        noRouteFound = false
        routeResult = null
        if (selectedFromId.isEmpty() || selectedToId.isEmpty() || selectedFromId == selectedToId) return
        val result = if (accessibleOnly) {
            router.findAccessibleRoute(selectedFromId, selectedToId)
        } else {
            router.findNormalRoute(selectedFromId, selectedToId)
        }
        if (result == null) {
            noRouteFound = true
        } else {
            routeResult = result
        }
    }

    LaunchedEffect(initialFromId, initialToId) {
        if (initialFromId.isNotEmpty() && initialToId.isNotEmpty() && initialFromId != initialToId) {
            computeRoute()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1565C0))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Accessible Route Planner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
                        Text("From:", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        ExposedDropdownMenuBox(
                            expanded = fromExpanded,
                            onExpandedChange = { fromExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedFromName,
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
                                            fromExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Swap Button
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            IconButton(onClick = {
                                val temp = selectedFromId
                                selectedFromId = selectedToId
                                selectedToId = temp
                            }) {
                                Icon(Icons.Default.SwapVert, "Swap points", tint = Color(0xFF1565C0))
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
                                            selectedToId = node.id
                                            toExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Accessible Only Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = accessibleOnly,
                                onCheckedChange = { accessibleOnly = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1565C0))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Wheelchair accessible route only", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Stairs are completely bypassed; avoids steep slopes", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { computeRoute() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedFromId.isNotEmpty() && selectedToId.isNotEmpty() && selectedFromId != selectedToId,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DirectionsRun, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Find Accessible Route", color = Color.White, fontWeight = FontWeight.Bold)
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
                                Text("No Accessible Route Found", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Text(
                                    "Stairs or obstacles block the accessible path. Toggle off 'Wheelchair accessible only' to see the standard path.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D4037)
                                )
                                Spacer(Modifier.height(6.dp))
                                TextButton(onClick = {
                                    accessibleOnly = false
                                    computeRoute()
                                }) {
                                    Text("Show path with stairs anyway →", color = Color(0xFF1565C0))
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
                                Text("Accessible Path Computed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
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

                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    navController.navigate("map/$selectedFromId/$selectedToId")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Map, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("View on Campus Map", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Turn-by-Turn Directions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray)
                }

                itemsIndexed(result.path) { index, nodeId ->
                    val node = graph.nodeById(nodeId)
                    val edge = result.edges.getOrNull(index)
                    val isLast = index == result.path.lastIndex

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isLast) Color(0xFFE53935) else Color(0xFF1565C0),
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(34.dp)
                                        .background(Color(0xFFBBDEFB))
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            onClick = {
                                if (node?.isBuilding == true) {
                                    navController.navigate("building/$nodeId")
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (node?.isBuilding == true) Icons.Default.Business else Icons.Default.Place,
                                        null,
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(node?.name ?: nodeId, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (node?.isBuilding == true) {
                                        Text(if (node.hasRamp) "♿" else "🪜", fontSize = 14.sp)
                                    }
                                }
                                if (edge != null) {
                                    Text(
                                        "Walk ${edge.distance.toInt()}m · ${edge.surface.name.lowercase()}" +
                                                if (edge.hasStairs) " (⚠️ has stairs)" else " (barrier-free)",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 2.dp, start = 22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun RouteStatBadge(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 22.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0))
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

