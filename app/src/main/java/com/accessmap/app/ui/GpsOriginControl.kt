package com.accessmap.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accessmap.app.routing.CampusGraph
import com.accessmap.app.routing.CampusLocationHelper
import com.accessmap.app.routing.GpsOriginResult

/**
 * Shared "Use My GPS" pill button that snaps the user's live GPS fix
 * to the nearest surveyed LPU node and reports status feedback.
 */
@Composable
fun UseMyGpsChip(
    graph: CampusGraph,
    onNodeResolved: (nodeId: String, statusMessage: String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val context = LocalContext.current

    fun resolveNearest() {
        when (val res = CampusLocationHelper.nearestCampusNodeFromGps(context, graph)) {
            is GpsOriginResult.Success -> onNodeResolved(
                res.node.id,
                "📍 Nearest surveyed point: ${res.node.name} (${res.distanceMeters.toInt()} m away)"
            )
            is GpsOriginResult.OutsideCampus -> {
                val km = "%.1f".format(res.distanceMeters / 1000.0)
                onStatusMessage("⚠️ GPS is $km km from LPU (on emulator, set Extended Controls → Location to 31.2525, 75.7038).")
            }
            GpsOriginResult.LocationUnavailable -> {
                onStatusMessage("⚠️ Waiting for GPS signal — test outdoors or set mock location in Extended Controls.")
            }
            GpsOriginResult.NoPermission -> {
                onStatusMessage("⚠️ Please allow Location permission to use your current GPS position.")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            resolveNearest()
        } else {
            onStatusMessage("⚠️ Location permission denied. Pick a start building manually.")
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFE3F2FD),
        modifier = Modifier.clickable {
            if (CampusLocationHelper.hasLocationPermission(context)) {
                resolveNearest()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Use Current GPS",
                tint = Color(0xFF1565C0),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Use My GPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0)
            )
        }
    }
}
