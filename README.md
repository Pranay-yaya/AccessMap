# AccessMap (P12) — Starter Scaffold

This is a Phase 1 skeleton matching the architecture in your notes: a running
map screen, the accessibility-weighted routing engine, and the Room layer for
crowdsourced hazards. ML (TFLite) and offline sync are intentionally left for
Phase 2–4 so you can get something demoable first.

## How to use this

1. In Android Studio: **File > New > New Project > Empty Activity (Compose)**,
   package `com.accessmap.app`, min SDK 26.
2. Replace the generated `app/build.gradle.kts`, `settings.gradle.kts`, root
   `build.gradle.kts`, and `AndroidManifest.xml` with the versions here.
3. Copy the `.kt` files into the matching packages under
   `app/src/main/java/com/accessmap/app/...` (create the `ui`, `map`,
   `routing`, `database`, `repository` folders if Android Studio didn't).
4. Copy `local.properties.example` to `local.properties` (this file is
   already git-ignored by the default Android template) and paste in your
   real Maps API key and your local `sdk.dir`.
5. Sync Gradle, run on a device/emulator with Google Play services, grant
   location permission when prompted — you should see a Google Map centered
   on LPU's campus with one sample marker.

## What's here vs. what's next

**Included (Phase 1 + groundwork for 2–4):**
- `MainActivity.kt` / `ui/HomeScreen.kt` — nav graph, landing screen
- `map/MapScreen.kt` — Google Maps Compose centered on LPU
- `routing/CampusGraph.kt`, `routing/AccessibilityRouter.kt` — the accessible
  pathfinding engine (Dijkstra with a cost function that blocks stairs and
  penalizes steep/uneven paths). Currently takes an in-memory graph — you'll
  populate `nodes`/`edges` by surveying real paths between LPU blocks.
- `database/HazardDatabase.kt` — Room entity/DAO/DB for hazard reports
- `repository/AccessMapRepository.kt` — glue layer tying the above together

**Not yet built (next phases):**
- Actual LPU node/edge data (you'll need to walk/measure real paths, or
  approximate from satellite imagery to start)
- `NavigationScreen` / `ReportHazardScreen` UI (the report-a-hazard flow that
  writes into `HazardDao`)
- `ScannerScreen` + CameraX + TFLite pipeline for AI hazard detection
- Offline tile caching strategy

Happy to build any of these next — the routing engine and hazard database are
the two pieces worth getting solid early, since the UI and ML layers both
depend on them.
