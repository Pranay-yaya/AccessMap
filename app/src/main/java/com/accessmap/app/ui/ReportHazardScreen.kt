package com.accessmap.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.accessmap.app.database.AppDatabase
import com.accessmap.app.database.HazardEntity
import com.accessmap.app.database.HazardType
import com.accessmap.app.repository.AccessMapRepository
import com.accessmap.app.routing.CampusNode
import com.accessmap.app.routing.LpuCampusData
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHazardScreen(navController: NavHostController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val graph = remember { LpuCampusData.buildGraph(context) }
    val repo = remember { AccessMapRepository(AppDatabase.getInstance(context).hazardDao(), graph) }
    val activeHazards by repo.activeHazards().collectAsState(initial = emptyList())

    var selectedNode by remember { mutableStateOf<CampusNode?>(null) }
    var locationExpanded by remember { mutableStateOf(false) }
    var selectedHazardLabel by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var gpsStatus by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val hazardOptions = listOf(
        "🚧 Construction Blocking Path" to HazardType.CONSTRUCTION,
        "🚪 Locked / Inaccessible Gate" to HazardType.LOCKED_GATE,
        "🛗 Elevator Out of Service" to HazardType.BROKEN_ELEVATOR,
        "🛑 Blocked Ramp / Walkway" to HazardType.BLOCKED_PATH,
        "🪨 Uneven / Broken Pavement" to HazardType.UNEVEN_PAVEMENT
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC62828))
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Icon(Icons.Default.Report, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Community Hazard Reports", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }

        if (submitted) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Hazard Saved to Campus Map!", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Saved in Room DB. Accessible routing will now avoid this hazard and warn nearby students.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { submitted = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Active Reports & Submit Another", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFE65100), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reports are saved locally in Room, shown on the map, avoided by routing, and expire after 7 days unless confirmed.",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                // Location picker + GPS
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍 Location", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            UseMyGpsChip(
                                graph = graph,
                                onNodeResolved = { nodeId, msg ->
                                    selectedNode = graph.nodeById(nodeId)
                                    gpsStatus = msg
                                },
                                onStatusMessage = { gpsStatus = it }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = locationExpanded,
                            onExpandedChange = { locationExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedNode?.name ?: "Select location on campus",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = locationExpanded,
                                onDismissRequest = { locationExpanded = false }
                            ) {
                                graph.nodes.forEach { node ->
                                    DropdownMenuItem(
                                        text = { Text(node.name, fontSize = 13.sp) },
                                        onClick = {
                                            selectedNode = node
                                            locationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        gpsStatus?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }

                // Hazard type selection
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ Hazard Type", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        hazardOptions.forEach { (label, _) ->
                            val isSelected = selectedHazardLabel == label
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedHazardLabel = label
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            )
                        }
                    }
                }

                // Attach Photo + Description
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📷 Photo & Description", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (capturedBitmap == null) "Attach Camera Photo" else "Retake Photo")
                        }

                        capturedBitmap?.let { bmp ->
                            Spacer(Modifier.height(10.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Captured hazard photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Describe the hazard in detail...", fontSize = 13.sp) },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // Submit button
                Button(
                    onClick = {
                        val node = selectedNode ?: return@Button
                        val typePair = hazardOptions.find { it.first == selectedHazardLabel } ?: return@Button
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            val now = System.currentTimeMillis()
                            var savedPhotoPath = ""
                            capturedBitmap?.let { bmp ->
                                val dir = File(context.filesDir, "hazards").apply { mkdirs() }
                                val file = File(dir, "hazard_$now.jpg")
                                FileOutputStream(file).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                }
                                savedPhotoPath = file.absolutePath
                            }
                            repo.reportHazard(
                                HazardEntity(
                                    latitude = node.latitude,
                                    longitude = node.longitude,
                                    type = typePair.second,
                                    description = description.ifBlank { "${typePair.first} near ${node.name}" },
                                    timestamp = now,
                                    nodeId = node.id,
                                    photoPath = savedPhotoPath,
                                    confirmations = 1,
                                    lastConfirmedAt = now
                                )
                            )
                            selectedLocationReset@ run {
                                description = ""
                                capturedBitmap = null
                                submitted = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = selectedNode != null && selectedHazardLabel != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Report, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Hazard Report", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Live Active Community Hazards List
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Active Campus Hazards (${activeHazards.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (activeHazards.isEmpty()) {
                    Text(
                        "No active hazards reported. Campus pathways are clear!",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                } else {
                    activeHazards.forEach { hazard ->
                        val nodeName = graph.nodeById(hazard.nodeId)?.name ?: "Campus Location"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚠️ ${hazard.type.name.replace('_', ' ')}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC62828))
                                    Text("👍 ${hazard.confirmations}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(nodeName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(hazard.description, fontSize = 12.sp, color = Color.Gray)

                                if (hazard.photoPath.isNotEmpty() && File(hazard.photoPath).exists()) {
                                    val thumb = remember(hazard.photoPath) { BitmapFactory.decodeFile(hazard.photoPath) }
                                    thumb?.let {
                                        Spacer(Modifier.height(6.dp))
                                        Image(
                                            bitmap = it.asImageBitmap(),
                                            contentDescription = "Hazard photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            scope.launch { repo.confirmStillThere(hazard.id) }
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("Still There (+1)", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch { repo.resolveHazard(hazard.id) }
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("Resolved ✅", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
