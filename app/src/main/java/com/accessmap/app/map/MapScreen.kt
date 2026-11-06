package com.accessmap.app.map

import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.accessmap.app.R
import com.accessmap.app.routing.AccessibilityRouter
import com.accessmap.app.routing.CampusNode
import com.accessmap.app.routing.LPU_CAMPUS_NODES
import com.accessmap.app.routing.LpuCampusData
import com.accessmap.app.routing.RouteResult
import androidx.compose.foundation.clickable
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// LPU main campus center (Central Library / Administration area)
val LPU_CENTER = GeoPoint(31.2525, 75.7038)

// Google Maps Satellite Hybrid (Photographic aerial view + building/road labels)
val GoogleSatelliteHybrid = object : OnlineTileSourceBase(
    "GoogleHybrid",
    0, 20, 256, ".jpg",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=y",
        "https://mt1.google.com/vt/lyrs=y",
        "https://mt2.google.com/vt/lyrs=y",
        "https://mt3.google.com/vt/lyrs=y"
    ),
    "© Google Maps Satellite"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
                "&hl=en&x=" + MapTileIndex.getX(pMapTileIndex) +
                "&y=" + MapTileIndex.getY(pMapTileIndex) +
                "&z=" + MapTileIndex.getZoom(pMapTileIndex)
    }
}

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

    var isSatelliteView by remember { mutableStateOf(true) }
    var showRouteInfo by remember { mutableStateOf(routeResult != null) }
    var selectedNode by remember { mutableStateOf<CampusNode?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(if (isSatelliteView) GoogleSatelliteHybrid else TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    controller.setCenter(LPU_CENTER)

                    val buildingIconNormal = ContextCompat.getDrawable(ctx, R.drawable.ic_map_building)
                    val buildingIconWarning = ContextCompat.getDrawable(ctx, R.drawable.ic_map_building_warning)

                    // Add all campus node markers with custom building icons
                    graph.nodes.forEach { node ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(node.latitude, node.longitude)
                        marker.title = node.name
                        marker.snippet = buildSnippet(node)

                        // Set custom building icon instead of generic finger/pin
                        if (node.hasRamp) {
                            buildingIconNormal?.let { marker.icon = it }
                        } else {
                            buildingIconWarning?.let { marker.icon = it }
                        }

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                        // Clicking icon opens the rich Building Detail bottom sheet with Big Picture
                        marker.setOnMarkerClickListener { _, _ ->
                            selectedNode = node
                            showRouteInfo = false
                            true
                        }

                        overlays.add(marker)
                    }

                    // Draw colorful high-contrast route polyline and waypoint pins if available
                    routeResult?.let { result ->
                        val routePoints = result.path.mapNotNull { nodeId ->
                            graph.nodeById(nodeId)?.let { GeoPoint(it.latitude, it.longitude) }
                        }
                        if (routePoints.size >= 2) {
                            // Layer 1: Dark outer border / shadow glow for crisp visibility on satellite
                            val outerBorder = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#00231C")
                                outlinePaint.strokeWidth = 22f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            overlays.add(outerBorder)

                            val isAccessible = result.edges.isEmpty() || result.edges.all { it.wheelchairAccessible }

                            // Layer 2: Neon Cyan / Amber middle accent glow
                            val midGlow = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor(
                                    if (isAccessible) "#00E5FF" else "#FFB300"
                                )
                                outlinePaint.strokeWidth = 16f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            overlays.add(midGlow)

                            // Layer 3: Core Neon Path (Glowing Accessible Emerald Green or Warning Orange)
                            val coreLine = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor(
                                    if (isAccessible) "#00E676" else "#FF3D00"
                                )
                                outlinePaint.strokeWidth = 10f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            overlays.add(coreLine)

                            // Intermediate turn waypoint dots along the route
                            for (i in 1 until routePoints.size - 1) {
                                val wpMarker = Marker(this)
                                wpMarker.position = routePoints[i]
                                ContextCompat.getDrawable(ctx, R.drawable.ic_route_waypoint)?.let {
                                    wpMarker.icon = it
                                }
                                wpMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                val wpNode = graph.nodeById(result.path[i])
                                wpMarker.title = wpNode?.name ?: "Step $i"
                                wpMarker.snippet = "Campus Turn • Accessible Walkway"
                                overlays.add(wpMarker)
                            }

                            // Distinct Green START Marker
                            val startMarker = Marker(this)
                            startMarker.position = routePoints.first()
                            ContextCompat.getDrawable(ctx, R.drawable.ic_route_start)?.let {
                                startMarker.icon = it
                            }
                            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            val startNode = graph.nodeById(result.path.first())
                            startMarker.title = "🟢 START: ${startNode?.name ?: "Origin"}"
                            startMarker.snippet = startNode?.description ?: "Starting Point"
                            overlays.add(startMarker)

                            // Distinct Red DESTINATION Marker
                            val destMarker = Marker(this)
                            destMarker.position = routePoints.last()
                            ContextCompat.getDrawable(ctx, R.drawable.ic_route_end)?.let {
                                destMarker.icon = it
                            }
                            destMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            val destNode = graph.nodeById(result.path.last())
                            destMarker.title = "🏁 DESTINATION: ${destNode?.name ?: "Target"}"
                            destMarker.snippet = destNode?.description ?: "Destination"
                            overlays.add(destMarker)

                            // Smoothly zoom and frame the entire route with padding
                            post {
                                try {
                                    val bBox = BoundingBox.fromGeoPoints(routePoints)
                                    zoomToBoundingBox(bBox.increaseByScale(1.35f), true, 130)
                                } catch (e: Exception) {
                                    val midLat = routePoints.map { it.latitude }.average()
                                    val midLon = routePoints.map { it.longitude }.average()
                                    controller.setCenter(GeoPoint(midLat, midLon))
                                }
                            }
                        }
                    }
                    invalidate()
                }
            },
            update = { mapView ->
                val targetSource = if (isSatelliteView) GoogleSatelliteHybrid else TileSourceFactory.MAPNIK
                if (mapView.tileProvider.tileSource.name() != targetSource.name()) {
                    mapView.setTileSource(targetSource)
                    mapView.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar with Satellite Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xEE0A192F))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (fromId.isEmpty()) "LPU Campus Map" else "Accessible Campus Route",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = if (isSatelliteView) "🛰️ Satellite View (Google Hybrid)" else "🗺️ Vector Street Map",
                    color = if (isSatelliteView) Color(0xFF00E676) else Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Satellite vs Street Toggle Switch
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSatelliteView) Color(0xFF00E676) else Color.White.copy(alpha = 0.22f),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { isSatelliteView = !isSatelliteView }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSatelliteView) "🛰️ Satellite" else "🗺️ Street",
                        color = if (isSatelliteView) Color(0xFF003300) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Route info panel at bottom
        if (routeResult != null && showRouteInfo && selectedNode == null) {
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

        // Big Picture & Full Details Card when building marker is tapped
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = fadeIn() + slideInVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedNode?.let { node ->
                BuildingPictureCard(
                    node = node,
                    onDismiss = { selectedNode = null },
                    onNavigateTo = {
                        val origin = LPU_CAMPUS_NODES.firstOrNull { it.id == "MAIN_GATE" }?.id ?: "MAIN_GATE"
                        navController.navigate("navigate/$origin/${node.id}")
                    },
                    onFullDetails = {
                        navController.navigate("building/${node.id}")
                    }
                )
            }
        }
    }
}

/**
 * Big Picture & Detailed Card for each building when clicked.
 */
@Composable
fun BuildingPictureCard(
    node: CampusNode,
    onDismiss: () -> Unit,
    onNavigateTo: () -> Unit,
    onFullDetails: () -> Unit
) {
    val context = LocalContext.current
    val imageResId = remember(node.id) {
        val cleanId = node.id.lowercase().replace("-", "_")
        val numPart = cleanId.removePrefix("b")
        val candidates = listOf(
            "pic_$cleanId",
            cleanId,
            "b$numPart",
            "pic_b$numPart",
            "block_$numPart",
            "block$numPart",
            "pic_block_$numPart"
        )
        candidates.firstNotNullOfOrNull { name ->
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) id else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Big Picture Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (imageResId != null) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = node.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Modern Styled Building Graphic Banner (Placeholder until user adds photo)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0D47A1),
                                        Color(0xFF1565C0),
                                        Color(0xFF1E88E5)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = node.id,
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "📷 Photo Spot Ready · LPU Campus",
                                color = Color(0xFFBBDEFB),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Close Button Top Right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(34.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Accessibility Badge Top Left
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (node.hasRamp) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (node.hasRamp) "♿ Barrier-Free" else "⚠️ Steps Only",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Building Details Body
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = node.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE3F2FD)
                    ) {
                        Text(
                            text = "Score: ${node.accessibilityScore}/10",
                            color = Color(0xFF1565C0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Accessibility Feature Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailChip(
                        icon = if (node.hasRamp) "✅" else "❌",
                        label = if (node.hasRamp) "Ramp Available" else "No Ramp",
                        bgColor = if (node.hasRamp) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                    DetailChip(
                        icon = if (node.hasElevator) "🛗" else "🪜",
                        label = if (node.hasElevator) "Elevator" else "${node.stairCount} Stairs",
                        bgColor = Color(0xFFF5F5F5)
                    )
                }

                if (node.description.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = node.description,
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A),
                        lineHeight = 17.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateTo,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Route Here", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onFullDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Full Profile", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailChip(icon: String, label: String, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(icon, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF37474F))
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
    val walkMinutes = (result.totalDistanceMeters / 80).toInt().coerceAtLeast(1)

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
                                contentDescription = null,
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
