package com.accessmap.app.map

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.accessmap.app.routing.CampusNode
import com.accessmap.app.routing.LpuCampusData
import com.accessmap.app.routing.RouteResult
import com.accessmap.app.routing.AccessibilityRouter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.compose.ui.platform.LocalContext

// LPU main campus center (Central Library / Administration area)
val LPU_CENTER = GeoPoint(31.2525, 75.7038)

@Composable
fun MapScreen(navController: NavHostController, fromId: String, toId: String) {
    val context = LocalContext.current
    val graph = remember { LpuCampusData.buildGraph(context) }
    val router = remember { AccessibilityRouter(graph) }

    val routeResult = remember(fromId, toId) {
        if (fromId.isNotEmpty() && toId.isNotEmpty()) {
            router.findAccessibleRoute(fromId, toId)
                ?: router.findNormalRoute(fromId, toId)
        } else null
    }

    var showRouteInfo by remember { mutableStateOf(routeResult != null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    controller.setCenter(LPU_CENTER)

                    // Add all campus node markers
                    graph.nodes.forEach { node ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(node.latitude, node.longitude)
                        marker.title = node.name
                        marker.snippet = buildSnippet(node)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        overlays.add(marker)
                    }

                    // Draw route polyline if available
                    routeResult?.let { result ->
                        val routePoints = result.path.mapNotNull { nodeId ->
                            graph.nodeById(nodeId)?.let { GeoPoint(it.latitude, it.longitude) }
                        }
                        if (routePoints.size >= 2) {
                            val polyline = Polyline()
                            polyline.setPoints(routePoints)
                            polyline.outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
                            polyline.outlinePaint.strokeWidth = 14f
                            polyline.outlinePaint.strokeCap = Paint.Cap.ROUND
                            overlays.add(0, polyline)

                            // Center map on route midpoint
                            val midLat = routePoints.map { it.latitude }.average()
                            val midLon = routePoints.map { it.longitude }.average()
                            controller.setCenter(GeoPoint(midLat, midLon))
                        }
                    }
                    invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDD1565C0))
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text(
                text = if (fromId.isEmpty()) "Campus Map" else "Accessible Route",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Route info panel at bottom
        if (routeResult != null && showRouteInfo) {
            RouteInfoPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                result = routeResult,
                graph = graph,
                fromId = fromId,
                toId = toId,
                onDismiss = { showRouteInfo = false },
                onBuildingInfo = { nodeId -> navController.navigate("building/$nodeId") }
            )
        }
    }
}

fun buildSnippet(node: CampusNode): String {
    if (!node.isBuilding) return node.description.ifEmpty { "Campus path/junction" }
    return buildString {
        if (node.hasRamp) append("✅ Ramp available | ") else append("❌ No ramp | ")
        if (node.hasElevator) append("🛗 Elevator | ")
        append("🪜 ${node.stairCount} stairs | ")
        append("⭐ Score: ${node.accessibilityScore}/10")
    }
}

@Composable
fun RouteInfoPanel(
    modifier: Modifier,
    result: RouteResult,
    graph: com.accessmap.app.routing.CampusGraph,
    fromId: String,
    toId: String,
    onDismiss: () -> Unit,
    onBuildingInfo: (String) -> Unit
) {
    val fromNode = graph.nodeById(fromId)
    val toNode = graph.nodeById(toId)
    val distanceText = if (result.totalDistanceMeters >= 1000)
        "%.1f km".format(result.totalDistanceMeters / 1000)
    else
        "${result.totalDistanceMeters.toInt()} m"
    val walkMinutes = (result.totalDistanceMeters / 80).toInt().coerceAtLeast(1) // ~80m/min walk

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Route Found ✅", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1565C0))
                    Text(
                        "${fromNode?.name ?: fromId} → ${toNode?.name ?: toId}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                TextButton(onClick = onDismiss) { Text("✕") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("📍 $distanceText")
                InfoChip("🚶 ~$walkMinutes min")
                InfoChip("${result.path.size} stops")
            }
            Spacer(Modifier.height(8.dp))
            Text("Stops:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            result.path.forEachIndexed { i, nodeId ->
                val node = graph.nodeById(nodeId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${i + 1}.", modifier = Modifier.width(24.dp), fontSize = 12.sp, color = Color.Gray)
                    Text(node?.name ?: nodeId, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    if (node?.isBuilding == true) {
                        IconButton(
                            onClick = { onBuildingInfo(nodeId) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1565C0)
        )
    }
}
