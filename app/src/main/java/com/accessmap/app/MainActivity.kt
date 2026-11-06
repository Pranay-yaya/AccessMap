package com.accessmap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.accessmap.app.map.MapScreen
import com.accessmap.app.ui.*
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid configuration with application context and local preferences
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Prompt for location permissions on launch for live GPS marker & proximity
                    var locationPermissionGranted by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val locationLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        locationPermissionGranted =
                            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    }

                    LaunchedEffect(Unit) {
                        if (!locationPermissionGranted) {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController)
                        }

                        composable("map") {
                            MapScreen(navController, "", "")
                        }

                        composable(
                            "map/{fromId}/{toId}",
                            arguments = listOf(
                                navArgument("fromId") { type = NavType.StringType },
                                navArgument("toId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val fromId = backStackEntry.arguments?.getString("fromId") ?: ""
                            val toId = backStackEntry.arguments?.getString("toId") ?: ""
                            MapScreen(navController, fromId, toId)
                        }

                        composable(
                            "building/{buildingId}",
                            arguments = listOf(
                                navArgument("buildingId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val buildingId = backStackEntry.arguments?.getString("buildingId") ?: ""
                            BuildingInfoScreen(navController, buildingId)
                        }

                        composable("report") {
                            ReportHazardScreen(navController)
                        }

                        composable("scanner") {
                            ScannerScreen(navController)
                        }

                        composable("navigate") {
                            NavigationScreen(navController)
                        }

                        composable(
                            "navigate/{fromId}/{toId}",
                            arguments = listOf(
                                navArgument("fromId") { type = NavType.StringType; defaultValue = "" },
                                navArgument("toId") { type = NavType.StringType; defaultValue = "" }
                            )
                        ) { backStackEntry ->
                            val fromId = backStackEntry.arguments?.getString("fromId") ?: ""
                            val toId = backStackEntry.arguments?.getString("toId") ?: ""
                            NavigationScreen(navController, fromId, toId)
                        }
                    }
                }
            }
        }
    }
}
