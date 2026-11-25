package com.accessmap.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Calculates initial compass bearing in degrees [0, 360) from (lat1, lon1) to (lat2, lon2).
 */
fun calculateBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val deltaLambda = Math.toRadians(lon2 - lon1)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    val theta = atan2(y, x)
    return (((Math.toDegrees(theta) + 360.0) % 360.0)).toFloat()
}

/**
 * Converts degrees [0, 360) into a 8-point cardinal direction label.
 */
fun bearingToCardinal(degrees: Float): String {
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = (((degrees + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
    return dirs[index]
}

fun bearingToFullDirection(degrees: Float): String = when (bearingToCardinal(degrees)) {
    "N" -> "North"
    "NE" -> "Northeast"
    "E" -> "East"
    "SE" -> "Southeast"
    "S" -> "South"
    "SW" -> "Southwest"
    "W" -> "West"
    "NW" -> "Northwest"
    else -> "North"
}

/**
 * Compact live compass widget using Android's built-in SensorManager (free, offline, no API key).
 * Shows both live phone heading (if magnetometer/rotation sensor exists) and target route direction.
 */
@Composable
fun RouteCompassCard(
    targetBearingDegrees: Float,
    nextStopName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var deviceAzimuth by remember { mutableStateOf<Float?>(null) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMag = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val rotMatrix = FloatArray(9)
                        val orientation = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        deviceAzimuth = deg
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasMag = true
                    }
                }
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR && hasGravity && hasMag) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val deg = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                        deviceAzimuth = deg
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelSensor != null && magSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val currentHeading = deviceAzimuth ?: 0f
    // Relative angle to turn/head toward the route target
    val relativeTargetAngle = ((targetBearingDegrees - currentHeading + 360f) % 360f)
    val animatedNeedleAngle by animateFloatAsState(
        targetValue = relativeTargetAngle,
        animationSpec = tween(durationMillis = 250),
        label = "compass_needle"
    )
    val animatedNorthAngle by animateFloatAsState(
        targetValue = (-currentHeading + 360f) % 360f,
        animationSpec = tween(durationMillis = 250),
        label = "compass_north"
    )

    val targetCardinal = bearingToCardinal(targetBearingDegrees)
    val targetFull = bearingToFullDirection(targetBearingDegrees)
    val facingText = deviceAzimuth?.let {
        "Facing ${bearingToCardinal(it)} (${it.toInt()}°)"
    } ?: "Compass ready"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE0A192F),
        shadowElevation = 6.dp,
        modifier = modifier.semantics {
            contentDescription = "Compass: Head $targetFull, ${targetBearingDegrees.toInt()} degrees toward $nextStopName. $facingText"
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live rotating compass dial
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f - 2f

                    // Outer ring
                    drawCircle(
                        color = Color(0xFF1E3A5F),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF64B5F6),
                        radius = radius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )

                    // North indicator dot (red)
                    rotate(degrees = animatedNorthAngle, pivot = center) {
                        drawCircle(
                            color = Color(0xFFFF5252),
                            radius = 4f,
                            center = Offset(center.x, center.y - radius + 6f)
                        )
                    }

                    // Target route direction arrow (bright emerald green)
                    rotate(degrees = animatedNeedleAngle, pivot = center) {
                        val arrowPath = Path().apply {
                            moveTo(center.x, center.y - radius + 7f)
                            lineTo(center.x - 7f, center.y + 7f)
                            lineTo(center.x, center.y + 2f)
                            lineTo(center.x + 7f, center.y + 7f)
                            close()
                        }
                        drawPath(arrowPath, color = Color(0xFF00E676))
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = "🧭 Head $targetFull ($targetCardinal ${targetBearingDegrees.toInt()}°)",
                    color = Color(0xFF00E676),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Toward $nextStopName • $facingText",
                    color = Color(0xFFBBDEFB),
                    fontSize = 11.sp
                )
            }
        }
    }
}
