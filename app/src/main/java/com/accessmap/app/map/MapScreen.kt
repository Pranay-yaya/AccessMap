package com.accessmap.app.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.accessmap.app.routing.LPU_CAMPUS_NODES
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// LPU main campus, Phagwara, Punjab
val LPU_CENTER = LatLng(31.2551, 75.7033)

@Composable
fun MapScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LPU_CENTER, 16f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        LPU_CAMPUS_NODES.forEach { node ->
            Marker(
                state = MarkerState(position = LatLng(node.latitude, node.longitude)),
                title = node.name,
                snippet = node.id
            )
        }
    }
}
