package com.accessmap.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 2-second "Starting your route..." overlay with a pure-Compose walking cartoon character.
 * Tap anywhere to skip immediately; screen-reader labeled "Route starting".
 */
@Composable
fun StartingRouteOverlay(
    destinationName: String,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "walking_cartoon")
    val stridePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stride"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0A192F))
            .semantics { contentDescription = "Route starting" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1E88E5))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WalkingCartoonCharacter(
                    stridePhase = stridePhase,
                    modifier = Modifier.size(120.dp)
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Starting your route...",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (destinationName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Heading to $destinationName",
                        color = Color(0xFFBBDEFB),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Tap anywhere to skip",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WalkingCartoonCharacter(
    stridePhase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val swing = sin(stridePhase)
        val bob = abs(swing) * size.height * 0.035f

        val groundY = size.height * 0.88f
        // Ground shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(cx - size.width * 0.22f, groundY - 4f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.44f, 10f)
        )

        val headRadius = size.minDimension * 0.13f
        val headY = size.height * 0.22f - bob
        val neckY = headY + headRadius + 4f
        val hipY = size.height * 0.56f - bob

        val legSwingX = size.width * 0.16f * swing
        val armSwingX = size.width * 0.14f * swing

        // Back leg
        drawLine(
            color = Color(0xFF90CAF9),
            start = Offset(cx, hipY),
            end = Offset(cx - legSwingX, groundY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        // Back arm
        drawLine(
            color = Color(0xFFFFCC80),
            start = Offset(cx, neckY + 6f),
            end = Offset(cx + armSwingX, hipY - 2f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Torso (vibrant emerald jacket)
        drawLine(
            color = Color(0xFF00E676),
            start = Offset(cx, neckY),
            end = Offset(cx, hipY),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )

        // Front leg
        drawLine(
            color = Color.White,
            start = Offset(cx, hipY),
            end = Offset(cx + legSwingX, groundY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        // Front arm
        drawLine(
            color = Color(0xFFFFE0B2),
            start = Offset(cx, neckY + 6f),
            end = Offset(cx - armSwingX, hipY - 2f),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        // Head
        drawCircle(
            color = Color(0xFFFFE0B2),
            radius = headRadius,
            center = Offset(cx, headY)
        )
        // Cap / visor
        drawArc(
            color = Color(0xFFFFD54F),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - headRadius, headY - headRadius),
            size = androidx.compose.ui.geometry.Size(headRadius * 2f, headRadius * 1.4f)
        )
        // Eye
        drawCircle(
            color = Color(0xFF0A192F),
            radius = 2.8f,
            center = Offset(cx + headRadius * 0.35f, headY - 1f)
        )
    }
}

/**
 * Creates a small cartoon walker badge drawable for the animated route marker on OSMDroid MapView.
 */
fun createWalkingCharacterDrawable(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (36 * density).toInt().coerceAtLeast(64)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Outer white border
    val borderPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = AndroidPaint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.46f, borderPaint)

    // Blue badge fill
    val bgPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1565C0")
        style = AndroidPaint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx * 0.40f, bgPaint)

    // Cartoon walker figure inside badge
    val figurePaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#00E676")
        strokeWidth = sizePx * 0.08f
        strokeCap = AndroidPaint.Cap.ROUND
    }
    val headPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#FFD54F")
        style = AndroidPaint.Style.FILL
    }
    // Head
    canvas.drawCircle(cx, sizePx * 0.28f, sizePx * 0.09f, headPaint)
    // Torso
    canvas.drawLine(cx, sizePx * 0.38f, cx, sizePx * 0.60f, figurePaint)
    // Arms
    canvas.drawLine(cx - sizePx * 0.14f, sizePx * 0.48f, cx + sizePx * 0.14f, sizePx * 0.42f, figurePaint)
    // Legs
    canvas.drawLine(cx, sizePx * 0.60f, cx - sizePx * 0.12f, sizePx * 0.78f, figurePaint)
    canvas.drawLine(cx, sizePx * 0.60f, cx + sizePx * 0.13f, sizePx * 0.76f, figurePaint)

    return BitmapDrawable(context.resources, bitmap)
}
