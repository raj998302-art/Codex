# Car Jam Solver: Traffic Jam — Android Clone

A high-quality, fully native Android clone of the Play Store hit **"Car Jam Solver: Traffic Jam"**,
written in **Kotlin + Jetpack Compose**. No game engine, no binary assets — the glossy toy cars,
capsule passengers, themed stations and UI are all drawn procedurally with Compose Canvas, so the
APK stays small and crisp at any resolution.

![genre](https://img.shields.io/badge/genre-car%20puzzle-blue) ![minSdk](https://img.shields.io/badge/minSdk-24-green) ![kotlin](https://img.shields.io/badge/kotlin-2.0.21-purple)

## Gameplay (1:1 with the reference)

* A **queue of coloured passengers** waits at the station under a live "Queue" counter sign.
* Below a row of **dashed parking slots** sits the jam: cars packed in spirals, grids, diagonal
  stacks, discs and hearts — each with a **white arrow** showing its escape direction.
* **Tap a car** — if its arrow path is clear it slides out, flies along a curved road into a free
  slot and boarding begins. If it's blocked it just wobbles.
* Passengers board **in queue order** and only into a matching-colour car. A full car drives off
  and pays coins; an unfinished car keeps its slot hostage.
* Grey **"?" mystery cars** reveal their true colour the moment they become unblocked.
* **Win** when every passenger is seated. **Lose** when the slots fill up with cars nobody can
  board, or no car in the jam can move anymore.
* Levels are deterministic but **generated** (5 board styles × 5 station themes — sea, zoo,
  funfair, metro, desert) and every board is **proven solvable** by an elimination simulation
  before you play it.

## Project layout

```
app/src/main/java/com/codex/carjam/
├── MainActivity.kt
├── game/
│   ├── Models.kt          # palette, themes, level/car specs, design coordinate system
│   ├── Geometry.kt        # OBB corners, convex hull, SAT overlap, swept-path clearance
│   ├── LevelGenerator.kt  # layout styles + solvability proof + queue colouring
│   ├── GameEngine.kt      # tick loop: exiting, parking, boarding, departing, win/lose
│   ├── Prefs.kt           # coins / unlocked level / sound / vibration (SharedPreferences)
│   ├── SoundManager.kt    # zero-asset procedural sound effects (ToneGenerator)
│   └── render/Painters.kt # the toy-like car/passenger/station/effects painters
└── ui/
    ├── App.kt, HomeScreen.kt, GameScreen.kt, Dialogs.kt, UiBits.kt
```

## Building the APK with GitHub Actions (no local setup)

The workflow [.github/workflows/android-build.yml](.github/workflows/android-build.yml) runs on
every push and on demand:

1. Open the repo's **Actions** tab → pick the latest **"Android APK Build"** run
   (or use **Run workflow** to start one manually).
2. Wait for the green check, then download from **Artifacts**:
   * `car-jam-solver-debug-apk` — `app-debug.apk`, signed with the debug key, ready to install.
   * `car-jam-solver-release-apk` — release variant (also debug-signed so it installs anywhere;
     swap in a real keystore before publishing to Play).
3. Copy the APK to your phone and install it (allow "install from unknown sources").

### Local build (optional)

With JDK 17 + Android SDK installed:

```bash
gradle assembleDebug      # debug APK in app/build/outputs/apk/debug
gradle assembleRelease    # release APK in app/build/outputs/apk/release
```

## Tech notes

* Kotlin 2.0.21, AGP 8.5.2, Compose BOM 2024.09.03, minSdk 24 / targetSdk 34.
* Portrait-locked single activity; scene is letterboxed from a 1080×2280 design space.
* Blocking check uses exact **swept SAT** collision (convex hull of the slid body), identical in
  spirit to the original game's slide-out rule.
* Sounds are synthesised at runtime — the project contains **zero binary assets**.
