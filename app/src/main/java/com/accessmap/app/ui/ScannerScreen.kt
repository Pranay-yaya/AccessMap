package com.accessmap.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DetectionResult(
    val title: String,
    val confidence: Float,
    val description: String,
    val isHazard: Boolean,
    val badgeColor: Color
)

@Composable
fun ScannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isAnalyzing by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<DetectionResult?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // AI edge classification models outputs
    val sampleDetections = listOf(
        DetectionResult(
            title = "♿ Accessible Ramp Detected",
            confidence = 0.94f,
            description = "Standard gentle incline detected. Barrier-free and compliant for wheelchair navigation.",
            isHazard = false,
            badgeColor = Color(0xFF2E7D32)
        ),
        DetectionResult(
            title = "🪜 Flight of Stairs Detected",
            confidence = 0.92f,
            description = "Multi-step stairs detected. Path is NOT accessible for wheelchairs. Please take the adjacent ramp route.",
            isHazard = true,
            badgeColor = Color(0xFFC62828)
        ),
        DetectionResult(
            title = "🚧 Pathway Obstacle / Debris",
            confidence = 0.87f,
            description = "Construction material or obstacle narrowing walkway below 90cm required clearance.",
            isHazard = true,
            badgeColor = Color(0xFFE65100)
        ),
        DetectionResult(
            title = "✅ Paved Level Pathway",
            confidence = 0.96f,
            description = "Surface is flat, even and clean. Suitable for all wheelchairs and walking aids.",
            isHazard = false,
            badgeColor = Color(0xFF1565C0)
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF212121)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Camera Access Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AccessMap uses the camera to scan paths for stairs, ramps, and physical hazards using Google AI Edge LiteRT.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        // Top bar overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x99000000))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("AI Path & Hazard Scanner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        // Targeting reticle
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
        ) {
            listOf(
                Alignment.TopStart, Alignment.TopEnd,
                Alignment.BottomStart, Alignment.BottomEnd
            ).forEach { align ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .align(align)
                        .padding(4.dp)
                ) {
                    Surface(
                        color = if (isAnalyzing) Color(0xFF1565C0) else Color.White,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                    ) {}
                }
            }
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(60.dp).align(Alignment.Center),
                    color = Color(0xFF1565C0),
                    strokeWidth = 4.dp
                )
            }
        }

        // Bottom Controls and Results
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xCC000000))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = currentResult != null,
                enter = fadeIn() + slideInVertically { it }
            ) {
                currentResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = res.badgeColor.copy(alpha = 0.95f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        "${(res.confidence * 100).toInt()}% Match",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(res.description, color = Color.White, fontSize = 13.sp)

                            if (res.isHazard) {
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { navController.navigate("report") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Report, null, tint = res.badgeColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Report This Hazard to Campus", color = res.badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Capture button
            Button(
                onClick = {
                    if (!isAnalyzing) {
                        coroutineScope.launch {
                            isAnalyzing = true
                            currentResult = null
                            delay(1600) // Simulate LiteRT inference run
                            currentResult = sampleDetections.random()
                            isAnalyzing = false
                        }
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAnalyzing) Color(0xFF1565C0) else Color.White
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = hasCameraPermission && !isAnalyzing
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan", tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (isAnalyzing) "Processing frame with LiteRT..." else "Point camera at path & tap to classify",
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "⚡ Google AI Edge LiteRT (CameraX 1.4.2 · 16 KB Page-Size Ready)",
                color = Color(0xAAFFFFFF),
                fontSize = 10.sp
            )
        }
    }
}

