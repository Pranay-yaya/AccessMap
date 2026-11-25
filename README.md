# AccessMap LPU

**AccessMap LPU** is an open-source, accessible campus navigation Android app built for **Lovely Professional University (LPU)** (CSE226).  
Every feature runs on **free, open-source, or built-in Android tools** — **no Google Maps API key, subscription, or billing account required**.

---

## Implemented Features

### 1. Map & Navigation
- **Interactive Campus Map (`osmdroid` 6.1.18)**: Displays all **47 surveyed LPU buildings and junctions** with a toggle between **Satellite Hybrid** and **Vector Street** map views.
- **Live GPS ("You Are Here")**: Built-in `MyLocationNewOverlay` blue dot using Android's `LocationManager` + **"Use My GPS"** snap to the nearest surveyed campus node.
- **Step-Free vs. Shortest Routing**: Dijkstra-based routing engine (`AccessibilityRouter`) supporting **Step-Free Only** and **Shortest** modes with distance, estimated walking time, and colored multi-layer route polylines.
- **Live Routing Compass & Direction**: `RouteCompassCard` (via Android `SensorManager`) shows live phone heading and points toward the next stop with cardinal directions (e.g., *Head Northeast (NE 48°)*) on both the route planner and map screen.
- **Turn-by-Turn, Off-Route Re-Routing & Arrival Alert**: Live `LocationListener` detects arrival (`<= 25m`) and off-route drift (`> 60m`) to automatically re-route from the nearest campus node.
- **Building Search & Favorites**: Instant local search over all 47 nodes and `SharedPreferences`-persisted favorite destinations (`⭐`).
- **Building Detail Cards**: Displays ramp availability, elevator/stair count, accessibility score, survey notes, favorite toggle, and read-aloud summary.
- **Offline Campus Map Tiles**: One-tap download button on the map using `osmdroid`'s `CacheManager` for offline campus tiles (zoom levels `15–18`).

### 2. Accessibility
- **Voice Guidance**: Built-in Android `TextToSpeech` (`VoiceGuidanceHelper`) for route summaries, turn-by-turn steps, building profiles, AI detections, and arrival/re-route alerts.
- **Display Modes**: Persisted toggles in **Settings & About** for **Dark Theme**, **High-Contrast Mode**, and **Large Text Mode (`1.22x`)**.
- **Screen Reader & Motor Ergonomics**: TalkBack `contentDescription` labels, `48dp` minimum touch targets, and haptic feedback on key actions.
- **Animation Control**: Settings toggle (**Route Animations**) to enable or disable route overlays and map character animations.

### 3. Hazards & Community
- **Room Database Persistence (`AppDatabase` v2)**: Report hazards (construction, locked gate, broken elevator, blocked path, uneven pavement) with GPS/node location.
- **Photo Attachments**: Attach camera photos saved locally under `filesDir/hazards/`.
- **Hazard-Aware Routing**: Automatically avoids active hazard-blocked nodes when computing routes.
- **Community Verification & Expiry**: Live active hazards feed with photo thumbnails, **"Still There (+1)"** confirmations, **"Resolved ✅"** actions, and **7-day automatic expiry**.

### 4. AI Camera Scanner
- **CameraX 1.4.2 + LiteRT 1.0.1**: Classifies `ramp`, `stairs`, `obstacle`, and `clear_path` (supports dropping a custom `accessmap_model.tflite` into `app/src/main/assets/`).
- **Confirm & Pin to Map**: **"Confirm & Pin to Map"** saves confirmed detections (`AI_CONFIRMED_STAIRS` / `AI_CONFIRMED_OBSTACLE`) to Room so they appear as markers on the map.
- **Free Google Colab Training Guide**: Built-in instructions in Settings/About for training a custom `.tflite` classifier using campus photos.

### 5. Animations & App Polish
- **"Starting your route..." Cartoon Overlay**: 2-second pure-Compose `Canvas` walking character animation when routing begins (tap anywhere to skip, screen-reader labeled `"Route starting"`).
- **Animated Map Walker**: Cartoon walker marker smoothly gliding along the active route polyline from start to destination.
- **Onboarding & Info**: Welcome landing screen, first-launch onboarding tips popup, animated `NavHost` transitions, About page, GitHub link, and `mailto:` feedback button.

---

## Tech Stack & Project Structure

- **Language & UI**: Kotlin, Jetpack Compose (Material 3), Navigation Compose (`minSdk 26`, `compileSdk 36`, `targetSdk 36`)
- **Maps**: `org.osmdroid:osmdroid-android:6.1.18`
- **Database**: `androidx.room` `2.6.1` (KSP)
- **Camera & AI**: `androidx.camera:*:1.4.2`, `com.google.ai.edge.litert:litert:1.0.1`

```text
app/src/main/
├── assets/
│   └── edges.csv                       # LPU campus walkway connections
└── java/com/accessmap/app/
    ├── MainActivity.kt                 # Theme, TTS, animated NavHost
    ├── database/HazardDatabase.kt      # Room entity, DAO, 7-day expiry
    ├── repository/AccessMapRepository.kt
    ├── routing/
    │   ├── AccessibilityRouter.kt      # Hazard-aware Dijkstra router
    │   ├── CampusData.kt               # 47 surveyed LPU nodes
    │   ├── CampusGraph.kt              # Graph loader & nearest-node lookup
    │   ├── CampusLocationHelper.kt     # Built-in LocationManager GPS helper
    │   └── DistanceUtils.kt            # Haversine distance
    ├── map/MapScreen.kt                # OSMDroid map, route overlays, compass, offline tiles
    └── ui/
        ├── AppAccessibilityState.kt    # Settings state, themes, TTS helper
        ├── BuildingInfoScreen.kt       # Detailed building accessibility profile
        ├── CompassWidget.kt            # Live SensorManager compass & bearing card
        ├── GpsOriginControl.kt         # "Use My GPS" origin chip
        ├── HomeScreen.kt               # Search, favorites, hazard badge, quick actions
        ├── NavigationScreen.kt         # Route planner, step-free toggle, turn-by-turn
        ├── ReportHazardScreen.kt       # Hazard reporting, photos, confirmations
        ├── RouteWalkingAnimation.kt    # Compose walking cartoon & map walker marker
        ├── ScannerScreen.kt            # CameraX + LiteRT scanner & map pinning
        ├── SettingsAboutScreen.kt      # Accessibility toggles, Colab guide, feedback
        └── WelcomeScreen.kt            # Landing page & onboarding tips dialog
```

---

## Building & Running

1. Open the project in **Android Studio** (or build from CLI using **JDK 21**).
2. Connect an Android device or start an emulator (API 26+).
3. Build the debug APK:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
4. Grant **Location** and **Camera** permissions when prompted to test live GPS routing, compass heading, and the AI scanner.
