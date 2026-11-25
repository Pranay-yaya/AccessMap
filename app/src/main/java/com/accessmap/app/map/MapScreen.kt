package com.accessmap.app.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.accessmap.app.R
import com.accessmap.app.database.AppDatabase
import com.accessmap.app.database.HazardEntity
import com.accessmap.app.repository.AccessMapRepository
import com.accessmap.app.routing.AccessibilityRouter
import com.accessmap.app.routing.CampusLocationHelper
import com.accessmap.app.routing.CampusNode
import com.accessmap.app.routing.GpsOriginResult
import com.accessmap.app.routing.LPU_CAMPUS_NODES
import com.accessmap.app.routing.LpuCampusData
import com.accessmap.app.routing.RouteResult
import com.accessmap.app.routing.haversineDistance
import com.accessmap.app.ui.LocalAppSettings
import com.accessmap.app.ui.LocalVoiceGuidance
import com.accessmap.app.ui.RouteCompassCard
import com.accessmap.app.ui.StartingRouteOverlay
import com.accessmap.app.ui.bearingToFullDirection
import com.accessmap.app.ui.calculateBearingDegrees
import com.accessmap.app.ui.createWalkingCharacterDrawable
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(navController: NavHostController, fromId: String, toId: String) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings = LocalAppSettings.current
    val voice = LocalVoiceGuidance.current

    val graph = remember { LpuCampusData.buildGraph(context) }
    val router = remember { AccessibilityRouter(graph) }
    val repository = remember {
        AccessMapRepository(AppDatabase.getInstance(context).hazardDao(), graph)
    }
    val activeHazards by repository.activeHazards().collectAsState(initial = emptyList())
    val blockedNodeIds = remember(activeHazards) {
        activeHazards.mapNotNull { it.nodeId.ifBlank { null } }.toSet()
    }

    var activeFromId by remember(fromId) { mutableStateOf(fromId) }
    var stepFreeOnly by remember { mutableStateOf(true) }

    val routeResult = remember(activeFromId, toId, stepFreeOnly, blockedNodeIds) {
        if (activeFromId.isNotEmpty() && toId.isNotEmpty()) {
            if (stepFreeOnly) {
                router.findAccessibleRoute(activeFromId, toId, blockedNodeIds)
                    ?: router.findNormalRoute(activeFromId, toId, blockedNodeIds)
            } else {
                router.findNormalRoute(activeFromId, toId, blockedNodeIds)
            }
        } else null
    }

    var isSatelliteView by remember { mutableStateOf(true) }
    var followUser by remember { mutableStateOf(false) }
    var isDownloadingTiles by remember { mutableStateOf(false) }
    var hasArrived by remember(toId) { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(CampusLocationHelper.hasLocationPermission(context))
    }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlayRef by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var locationNotice by remember { mutableStateOf<String?>(null) }
    var showRouteInfo by remember(routeResult) { mutableStateOf(routeResult != null) }
    var selectedNode by remember { mutableStateOf<CampusNode?>(null) }

    val dynamicOverlays = remember { mutableListOf<Overlay>() }
    var lastOverlayKey by remember { mutableStateOf<String?>(null) }
    var lastZoomedRouteKey by remember { mutableStateOf<String?>(null) }
    var showStartingOverlay by remember(fromId, toId) {
        mutableStateOf(fromId.isNotEmpty() && toId.isNotEmpty() && settings.animationsEnabled)
    }
    var walkerMarkerRef by remember { mutableStateOf<Marker?>(null) }

    LaunchedEffect(fromId, toId, settings.animationsEnabled) {
        if (fromId.isNotEmpty() && toId.isNotEmpty() && settings.animationsEnabled) {
            showStartingOverlay = true
            delay(2000L)
            showStartingOverlay = false
        } else {
            showStartingOverlay = false
        }
    }

    // Animate cartoon walker marker along the active route on the map
    LaunchedEffect(routeResult, settings.animationsEnabled, showStartingOverlay, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val points = routeResult?.path?.mapNotNull { id ->
            graph.nodeById(id)?.let { GeoPoint(it.latitude, it.longitude) }
        } ?: emptyList()

        if (!settings.animationsEnabled || points.size < 2 || showStartingOverlay) {
            walkerMarkerRef?.let {
                mv.overlays.remove(it)
                mv.postInvalidate()
            }
            walkerMarkerRef = null
            return@LaunchedEffect
        }

        val walker = walkerMarkerRef ?: Marker(mv).apply {
            icon = createWalkingCharacterDrawable(context)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Route Preview"
            snippet = "Walking along accessible route"
        }.also {
            walkerMarkerRef = it
            mv.overlays.add(it)
        }

        try {
            while (isActive) {
                for (i in 0 until points.size - 1) {
                    val startPt = points[i]
                    val endPt = points[i + 1]
                    val steps = 36
                    for (step in 0..steps) {
                        if (!isActive) break
                        val t = step / steps.toDouble()
                        val lat = startPt.latitude + (endPt.latitude - startPt.latitude) * t
                        val lon = startPt.longitude + (endPt.longitude - startPt.longitude) * t
                        walker.position = GeoPoint(lat, lon)
                        mv.postInvalidate()
                        delay(40L)
                    }
                }
                delay(800L)
            }
        } finally {
            mv.overlays.remove(walker)
            mv.postInvalidate()
            if (walkerMarkerRef == walker) walkerMarkerRef = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            myLocationOverlayRef?.enableMyLocation()
            myLocationOverlayRef?.enableFollowLocation()
            followUser = true
            locationNotice = "📡 Live GPS enabled — waiting for satellite lock..."
        } else {
            locationNotice = "⚠️ Location permission denied. Tap here to open App Settings and allow Location."
        }
    }

    // Off-route re-routing & arrival alert when navigating to a destination
    val latestRoute by rememberUpdatedState(routeResult)
    val latestFromId by rememberUpdatedState(activeFromId)
    DisposableEffect(toId, hasLocationPermission) {
        if (toId.isEmpty() || !hasLocationPermission) {
            return@DisposableEffect onDispose {}
        }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@DisposableEffect onDispose {}
        var lastRerouteAt = 0L

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val destNode = graph.nodeById(toId) ?: return
                val distToDest = haversineDistance(
                    loc.latitude, loc.longitude,
                    destNode.latitude, destNode.longitude
                )
                if (distToDest <= 25.0 && !hasArrived) {
                    hasArrived = true
                    locationNotice = "🎉 You have arrived at ${destNode.name}!"
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    voice.speak("You have arrived at ${destNode.name}", settings.voiceGuidance)
                    return
                }

                val currentRoute = latestRoute ?: return
                val minPathDist = currentRoute.path.mapNotNull { graph.nodeById(it) }
                    .minOfOrNull { node ->
                        haversineDistance(loc.latitude, loc.longitude, node.latitude, node.longitude)
                    } ?: return

                val now = System.currentTimeMillis()
                if (minPathDist > 60.0 && now - lastRerouteAt > 8000L) {
                    val (nearest, distToNearest) = graph.nearestNodeWithDistance(
                        loc.latitude, loc.longitude
                    ) ?: return
                    if (distToNearest <= CampusLocationHelper.MAX_CAMPUS_RADIUS_METERS &&
                        nearest.id != latestFromId
                    ) {
                        lastRerouteAt = now
                        activeFromId = nearest.id
                        locationNotice = "🔄 Off-route! Re-routing from ${nearest.name}..."
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        voice.speak(
                            "Off route. Re-routing from ${nearest.name}",
                            settings.voiceGuidance
                        )
                    }
                }
            }

            @Deprecated("Deprecated in Android API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000L, 5f, listener)
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000L, 5f, listener)
            }
        } catch (_: Exception) {
        }

        onDispose {
            try {
                lm.removeUpdates(listener)
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapViewRef, myLocationOverlayRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapViewRef?.onResume()
                    if (CampusLocationHelper.hasLocationPermission(context)) {
                        myLocationOverlayRef?.enableMyLocation()
                        if (followUser) myLocationOverlayRef?.enableFollowLocation()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    myLocationOverlayRef?.disableFollowLocation()
                    myLocationOverlayRef?.disableMyLocation()
                    mapViewRef?.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocationOverlayRef?.disableMyLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewRef = this
                    setTileSource(if (isSatelliteView) GoogleSatelliteHybrid else TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.5)
                    controller.setCenter(LPU_CENTER)

                    val buildingIconNormal = ContextCompat.getDrawable(ctx, R.drawable.ic_map_building)
                    val buildingIconWarning = ContextCompat.getDrawable(ctx, R.drawable.ic_map_building_warning)

                    // Add all 47 campus node markers
                    graph.nodes.forEach { node ->
                        val marker = Marker(this)
                        marker.position = GeoPoint(node.latitude, node.longitude)
                        marker.title = node.name
                        marker.snippet = buildSnippet(node)

                        if (node.hasRamp) {
                            buildingIconNormal?.let { marker.icon = it }
                        } else {
                            buildingIconWarning?.let { marker.icon = it }
                        }

                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.setOnMarkerClickListener { _, _ ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedNode = node
                            showRouteInfo = false
                            true
                        }
                        overlays.add(marker)
                    }

                    // Live "You Are Here" blue dot GPS overlay (no API key needed)
                    val myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                        if (hasLocationPermission) {
                            enableMyLocation()
                        }
                    }
                    myLocationOverlayRef = myLocationOverlay
                    overlays.add(myLocationOverlay)

                    invalidate()
                }
            },
            update = { mapView ->
                val targetSource = if (isSatelliteView) GoogleSatelliteHybrid else TileSourceFactory.MAPNIK
                if (mapView.tileProvider.tileSource.name() != targetSource.name()) {
                    mapView.setTileSource(targetSource)
                }

                val routeKey = routeResult?.let {
                    "${it.path.joinToString(",")}:$stepFreeOnly:${it.avoidedHazards}"
                } ?: "none"
                val hazardKey = activeHazards.joinToString(";") {
                    "${it.id}:${it.confirmations}:${it.nodeId}"
                }
                val combinedKey = "$routeKey|$hazardKey"

                if (lastOverlayKey != combinedKey) {
                    lastOverlayKey = combinedKey
                    mapView.overlays.removeAll(dynamicOverlays)
                    dynamicOverlays.clear()

                    val myLocOverlay = myLocationOverlayRef
                    if (myLocOverlay != null) {
                        mapView.overlays.remove(myLocOverlay)
                    }

                    // Render active hazard & AI-confirmed markers
                    val hazardIcon = ContextCompat.getDrawable(context, R.drawable.ic_map_building_warning)
                    activeHazards.forEach { hazard: HazardEntity ->
                        val hm = Marker(mapView).apply {
                            position = GeoPoint(hazard.latitude, hazard.longitude)
                            title = "⚠️ ${hazard.type.name.replace('_', ' ')}"
                            snippet = "${hazard.description} (Confirmed ${hazard.confirmations}x)"
                            hazardIcon?.let { icon = it }
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        dynamicOverlays.add(hm)
                        mapView.overlays.add(hm)
                    }

                    // Draw route polyline & pins
                    routeResult?.let { result ->
                        val routePoints = result.path.mapNotNull { nodeId ->
                            graph.nodeById(nodeId)?.let { GeoPoint(it.latitude, it.longitude) }
                        }
                        if (routePoints.size >= 2) {
                            val outerBorder = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#00231C")
                                outlinePaint.strokeWidth = 22f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            dynamicOverlays.add(outerBorder)
                            mapView.overlays.add(outerBorder)

                            val isAccessible = result.edges.isEmpty() || result.edges.all { it.wheelchairAccessible }

                            val midGlow = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor(
                                    if (isAccessible) "#00E5FF" else "#FFB300"
                                )
                                outlinePaint.strokeWidth = 16f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            dynamicOverlays.add(midGlow)
                            mapView.overlays.add(midGlow)

                            val coreLine = Polyline().apply {
                                setPoints(routePoints)
                                outlinePaint.color = android.graphics.Color.parseColor(
                                    if (isAccessible) "#00E676" else "#FF3D00"
                                )
                                outlinePaint.strokeWidth = 10f
                                outlinePaint.strokeCap = Paint.Cap.ROUND
                                outlinePaint.strokeJoin = Paint.Join.ROUND
                            }
                            dynamicOverlays.add(coreLine)
                            mapView.overlays.add(coreLine)

                            CampusLocationHelper.getBestLastKnownLocation(context)?.let { gpsLoc ->
                                val distToStart = haversineDistance(
                                    gpsLoc.latitude, gpsLoc.longitude,
                                    routePoints.first().latitude, routePoints.first().longitude
                                )
                                if (distToStart in 8.0..CampusLocationHelper.MAX_CAMPUS_RADIUS_METERS) {
                                    val walkInLine = Polyline().apply {
                                        setPoints(listOf(GeoPoint(gpsLoc.latitude, gpsLoc.longitude), routePoints.first()))
                                        outlinePaint.color = android.graphics.Color.parseColor("#2979FF")
                                        outlinePaint.strokeWidth = 10f
                                        outlinePaint.strokeCap = Paint.Cap.ROUND
                                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 16f), 0f)
                                    }
                                    dynamicOverlays.add(walkInLine)
                                    mapView.overlays.add(walkInLine)
                                }
                            }

                            for (i in 1 until routePoints.size - 1) {
                                val wpMarker = Marker(mapView).apply {
                                    position = routePoints[i]
                                    ContextCompat.getDrawable(context, R.drawable.ic_route_waypoint)?.let { icon = it }
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    val wpNode = graph.nodeById(result.path[i])
                                    title = wpNode?.name ?: "Step $i"
                                    snippet = "Campus Turn • Accessible Walkway"
                                }
                                dynamicOverlays.add(wpMarker)
                                mapView.overlays.add(wpMarker)
                            }

                            val startMarker = Marker(mapView).apply {
                                position = routePoints.first()
                                ContextCompat.getDrawable(context, R.drawable.ic_route_start)?.let { icon = it }
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                val startNode = graph.nodeById(result.path.first())
                                title = "🟢 START: ${startNode?.name ?: "Origin"}"
                                snippet = startNode?.description ?: "Starting Point"
                            }
                            dynamicOverlays.add(startMarker)
                            mapView.overlays.add(startMarker)

                            val destMarker = Marker(mapView).apply {
                                position = routePoints.last()
                                ContextCompat.getDrawable(context, R.drawable.ic_route_end)?.let { icon = it }
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                val destNode = graph.nodeById(result.path.last())
                                title = "🏁 DESTINATION: ${destNode?.name ?: "Target"}"
                                snippet = destNode?.description ?: "Destination"
                            }
                            dynamicOverlays.add(destMarker)
                            mapView.overlays.add(destMarker)

                            if (lastZoomedRouteKey != routeKey) {
                                lastZoomedRouteKey = routeKey
                                mapView.post {
                                    try {
                                        val bBox = BoundingBox.fromGeoPoints(routePoints)
                                        mapView.zoomToBoundingBox(bBox.increaseByScale(1.35f), true, 130)
                                    } catch (_: Exception) {
                                        val midLat = routePoints.map { it.latitude }.average()
                                        val midLon = routePoints.map { it.longitude }.average()
                                        mapView.controller.setCenter(GeoPoint(midLat, midLon))
                                    }
                                }
                            }
                        }
                    }

                    if (myLocOverlay != null) {
                        mapView.overlays.add(myLocOverlay)
                    }
                    walkerMarkerRef?.let {
                        mapView.overlays.remove(it)
                        mapView.overlays.add(it)
                    }
                    mapView.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar + optional GPS / offline / arrival banner
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEE0A192F))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (activeFromId.isEmpty()) "LPU Campus Map" else "Accessible Campus Route",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = buildString {
                            append(if (isSatelliteView) "🛰️ Satellite Hybrid" else "🗺️ Street Map")
                            if (activeHazards.isNotEmpty()) append(" • ⚠️ ${activeHazards.size} hazards")
                        },
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
                        .heightIn(min = 38.dp)
                        .semantics { contentDescription = "Toggle satellite or street map" }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSatelliteView = !isSatelliteView
                        }
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

            locationNotice?.let { msg ->
                Surface(
                    color = Color(0xEE102A43),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable {
                            if (msg.contains("App Settings")) {
                                CampusLocationHelper.openAppPermissionSettings(context)
                            }
                            locationNotice = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = Color(0xFF9FB3C8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Live Compass + Route Direction Card when routing is active
            routeResult?.takeIf { it.path.size >= 2 }?.let { result ->
                val fromNode = graph.nodeById(result.path[0])
                val nextNode = graph.nodeById(result.path[1])
                if (fromNode != null && nextNode != null) {
                    val bearing = remember(fromNode.id, nextNode.id) {
                        calculateBearingDegrees(
                            fromNode.latitude, fromNode.longitude,
                            nextNode.latitude, nextNode.longitude
                        )
                    }
                    RouteCompassCard(
                        targetBearingDegrees = bearing,
                        nextStopName = nextNode.name,
                        modifier = Modifier.padding(start = 12.dp, end = 68.dp, top = 6.dp)
                    )
                }
            }
        }

        // Right-side Map Controls: My Location, Center LPU, Offline Campus Tiles
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 74.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!CampusLocationHelper.hasLocationPermission(context)) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@SmallFloatingActionButton
                    }

                    val overlay = myLocationOverlayRef
                    overlay?.enableMyLocation()
                    val currentGeo = overlay?.myLocation
                        ?: CampusLocationHelper.getBestLastKnownLocation(context)?.let {
                            GeoPoint(it.latitude, it.longitude)
                        }

                    if (currentGeo != null) {
                        val nextFollow = !followUser
                        followUser = nextFollow
                        if (nextFollow) {
                            overlay?.enableFollowLocation()
                            mapViewRef?.controller?.animateTo(currentGeo)
                            mapViewRef?.controller?.setZoom(18.5)
                            val distFromCampus = haversineDistance(
                                currentGeo.latitude, currentGeo.longitude,
                                LPU_CENTER.latitude, LPU_CENTER.longitude
                            )
                            locationNotice = if (distFromCampus > CampusLocationHelper.MAX_CAMPUS_RADIUS_METERS) {
                                "📍 GPS is ${(distFromCampus / 1000).toInt()} km away. On emulator: Extended Controls → Location → 31.2525, 75.7038"
                            } else {
                                "📍 Following your live GPS position (tap again to unlock pan)"
                            }
                        } else {
                            overlay?.disableFollowLocation()
                            locationNotice = null
                        }
                    } else {
                        followUser = true
                        overlay?.enableFollowLocation()
                        locationNotice = "📡 Waiting for GPS lock... (On emulator: Extended Controls → Location)"
                    }
                },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                containerColor = if (followUser) Color(0xFF00E676) else Color.White,
                contentColor = if (followUser) Color(0xFF003300) else Color(0xFF1565C0)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Live Location")
            }

            SmallFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    followUser = false
                    myLocationOverlayRef?.disableFollowLocation()
                    mapViewRef?.controller?.animateTo(LPU_CENTER)
                    mapViewRef?.controller?.setZoom(17.5)
                    locationNotice = null
                },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1565C0)
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center on LPU Campus")
            }

            // Offline Campus Map Tiles Cache Manager FAB
            SmallFloatingActionButton(
                onClick = {
                    val mv = mapViewRef
                    if (mv != null && !isDownloadingTiles) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isDownloadingTiles = true
                        locationNotice = "⬇️ Caching LPU Campus map tiles (Zoom 15–18) for offline use..."
                        val cacheManager = CacheManager(mv)
                        val campusBounds = BoundingBox(31.264, 75.714, 31.244, 75.695)
                        cacheManager.downloadAreaAsyncNoUI(
                            context,
                            campusBounds,
                            15,
                            18,
                            object : CacheManager.CacheManagerCallback {
                                override fun onTaskComplete() {
                                    isDownloadingTiles = false
                                    locationNotice = "✅ Offline campus map tiles cached (Zoom 15–18)!"
                                    voice.speak("Offline campus map tiles downloaded.", settings.voiceGuidance)
                                }

                                override fun onTaskFailed(errors: Int) {
                                    isDownloadingTiles = false
                                    locationNotice = if (errors > 0) {
                                        "⚠️ Offline cache finished with $errors tile errors (check connection)."
                                    } else {
                                        "✅ Offline campus map tiles cached!"
                                    }
                                }

                                override fun updateProgress(
                                    progress: Int,
                                    currentZoomLevel: Int,
                                    zoomMin: Int,
                                    zoomMax: Int
                                ) {
                                    locationNotice = "⬇️ Caching offline tiles: zoom $currentZoomLevel ($progress tiles)..."
                                }

                                override fun downloadStarted() {}
                                override fun setPossibleTilesInArea(total: Int) {}
                            }
                        )
                    }
                },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                containerColor = if (isDownloadingTiles) Color(0xFFFFD54F) else Color.White,
                contentColor = Color(0xFF1565C0)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download Offline Campus Map Tiles")
            }
        }

        // Route info panel at bottom
        if (routeResult != null && showRouteInfo && selectedNode == null) {
            RouteInfoPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                result = routeResult,
                graph = graph,
                fromId = activeFromId,
                toId = toId,
                stepFreeOnly = stepFreeOnly,
                onToggleStepFree = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    stepFreeOnly = !stepFreeOnly
                },
                onSpeakRoute = {
                    val fromName = graph.nodeById(activeFromId)?.name ?: activeFromId
                    val toName = graph.nodeById(toId)?.name ?: toId
                    val dist = routeResult.totalDistanceMeters.toInt()
                    val mins = (dist / 80).coerceAtLeast(1)
                    val mode = if (stepFreeOnly) "step-free" else "shortest"
                    val dirPhrase = if (routeResult.path.size >= 2) {
                        val s = graph.nodeById(routeResult.path[0])
                        val n = graph.nodeById(routeResult.path[1])
                        if (s != null && n != null) {
                            val b = calculateBearingDegrees(s.latitude, s.longitude, n.latitude, n.longitude)
                            " Head ${bearingToFullDirection(b)} toward ${n.name}."
                        } else ""
                    } else ""
                    voice.speak(
                        "Showing $mode route from $fromName to $toName, $dist meters, about $mins minutes.$dirPhrase",
                        true
                    )
                },
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
                val isFav = settings.isFavorite(node.id)
                BuildingPictureCard(
                    node = node,
                    isFavorite = isFav,
                    onToggleFavorite = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settings.toggleFavorite(node.id)
                    },
                    onSpeakSummary = {
                        val rampText = if (node.hasRamp) "Ramp available" else "No ramp"
                        val elevText = if (node.hasElevator) "Elevator available" else "${node.stairCount} stairs"
                        voice.speak(
                            "${node.name}. $rampText. $elevText. Accessibility score ${node.accessibilityScore} out of 10.",
                            true
                        )
                    },
                    onDismiss = { selectedNode = null },
                    onNavigateTo = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val gpsOrigin = CampusLocationHelper.nearestCampusNodeFromGps(
                            context, graph, myLocationOverlayRef?.lastFix
                        )
                        val origin = when {
                            gpsOrigin is GpsOriginResult.Success && gpsOrigin.node.id != node.id ->
                                gpsOrigin.node.id
                            node.id != "MAIN_GATE" ->
                                LPU_CAMPUS_NODES.firstOrNull { it.id == "MAIN_GATE" }?.id ?: "MAIN_GATE"
                            else -> "B1"
                        }
                        navController.navigate("navigate/$origin/${node.id}")
                    },
                    onFullDetails = {
                        navController.navigate("building/${node.id}")
                    }
                )
            }
        }

        // 2-second "Starting your route..." walking cartoon overlay (tap to skip)
        AnimatedVisibility(
            visible = showStartingOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val destName = graph.nodeById(toId)?.name ?: toId
            StartingRouteOverlay(
                destinationName = destName,
                onDismiss = { showStartingOverlay = false }
            )
        }
    }
}

/**
 * Big Picture & Detailed Card for each building when clicked.
 */
@Composable
fun BuildingPictureCard(
    node: CampusNode,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onSpeakSummary: () -> Unit,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                if (imageResId != null) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = node.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
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
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
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

                // Top Right: Favorite + Speak + Close
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onSpeakSummary,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Read building info aloud",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Score: ${node.accessibilityScore}/10",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateTo,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Route Here", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onFullDetails,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
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
    stepFreeOnly: Boolean,
    onToggleStepFree: () -> Unit,
    onSpeakRoute: () -> Unit,
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
            .padding(12.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (stepFreeOnly) "Step-Free Route ✅" else "Shortest Route ⚡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${fromNode?.name ?: fromId} → ${toNode?.name ?: toId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (result.avoidedHazards) {
                        Text(
                            "🛡️ Routing around active campus hazards",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onSpeakRoute) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Speak route summary",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onDismiss) { Text("✕") }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoChip("📍 $distanceText")
                InfoChip("🚶 ~$walkMinutes min")
                InfoChip("${result.path.size} stops")
                FilterChip(
                    selected = stepFreeOnly,
                    onClick = onToggleStepFree,
                    label = {
                        Text(if (stepFreeOnly) "♿ Step-free" else "⚡ Shortest", fontSize = 11.sp)
                    }
                )
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
                    Text(
                        "${i + 1}.",
                        modifier = Modifier.width(24.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        node?.name ?: nodeId,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (node?.isBuilding == true) {
                        IconButton(
                            onClick = { onBuildingInfo(nodeId) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "View building info",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
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
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
