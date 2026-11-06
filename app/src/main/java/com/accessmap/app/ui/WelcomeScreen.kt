package com.accessmap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun WelcomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Greeting Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D47A1), // Deep LPU Blue
                                Color(0xFF1976D2),
                                Color(0xFF2196F3)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.height(12.dp))

                    // App Emblem Badge
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(92.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("♿", fontSize = 38.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "AccessMap LPU",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = " Lovely Professional University ",
                            color = Color(0xFF212121),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Welcome to a barrier-free campus. Designed to empower students, staff, and visitors with independent, wheelchair-friendly navigation.",
                        color = Color(0xFFE3F2FD),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }

            // Core Accessibility Capabilities
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Key Campus Features",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF263238)
                )

                WelcomeFeatureCard(
                    icon = Icons.Default.DirectionsRun,
                    title = "Barrier-Free Routing",
                    description = "Dijkstra routing engine strictly avoids stairs and steep slopes to guarantee fully accessible walking paths.",
                    iconColor = Color(0xFF1565C0),
                    bgColor = Color(0xFFE3F2FD)
                )

                WelcomeFeatureCard(
                    icon = Icons.Default.Apartment,
                    title = "47 Surveyed Campus Blocks",
                    description = "Exact GPS locations, entrance ramp availability, elevator access, and stair counts for every academic block.",
                    iconColor = Color(0xFF2E7D32),
                    bgColor = Color(0xFFE8F5E9)
                )

                WelcomeFeatureCard(
                    icon = Icons.Default.CameraAlt,
                    title = "AI Edge Hazard Scanner",
                    description = "On-device computer vision detects ramps, stairs, and obstacles in real-time through your phone's camera.",
                    iconColor = Color(0xFF7B1FA2),
                    bgColor = Color(0xFFF3E5F5)
                )

                WelcomeFeatureCard(
                    icon = Icons.Default.ReportProblem,
                    title = "Crowdsourced Hazard Alerts",
                    description = "Report broken lifts, wet corridors, or construction barriers to help other students navigate safely.",
                    iconColor = Color(0xFFC62828),
                    bgColor = Color(0xFFFFEBEE)
                )
            }
        }

        // Bottom Fixed Action Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 12.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { navController.navigate("home") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Start Routing & Explore",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { navController.navigate("map") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1565C0))
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF1565C0))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Open Campus Map Directly",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconColor: Color,
    bgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF263238))
                Spacer(Modifier.height(3.dp))
                Text(description, fontSize = 12.sp, color = Color(0xFF546E7A), lineHeight = 17.sp)
            }
        }
    }
}
