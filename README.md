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


## v2.0 feature systems

| System | What it does |
| --- | --- |
| **Splash + offline gate** | Branded loading screen; if the device is offline a friendly **NO INTERNET** card offers RETRY / **PLAY OFFLINE** |
| **Profile** | Editable player name, **8 toon DP avatars** (cap, helmet, shades, headband, crown, headphones, mohawk, beanie), lifetime stats (level, wins, win%, practice) |
| **Google Play Games sign-in** | PGS v2 silent + interactive sign-in; shows gamer name. Activates fully once the game is registered with Play Games Services in Play Console |
| **Security (SecureVault)** | Coins / total earnings / No-Ads ownership are stored **checksum-signed** (SHA-256 `value|tag|installSalt`) with a mirrored backup — tampering with SharedPreferences triggers automatic restore from the clean mirror and counts a tamper flag. Grant sizes and balances are hard-capped |
| **Referral centre** | Deterministic `CJ-XXXXXX` code per install, system share sheet, **+150 coins** welcome bonus, one redemption per device, own-code & duplicate guards |
| **Practice mode** | Free play entry from home: no coins granted or lost, no level unlocks, no ads — pure learning, with its own practice-win stat |
| **Offline mode** | `Net` connectivity check drives an OFFLINE pill, banner/shop awareness; 100% of gameplay works without internet |
| **AAA icon** | Generated glossy toy-car icon with adaptive, round and monochrome variants |

### Honest security note

Client hardening raises the bar, but **real cheat-proofing requires a backend**:
Play Billing already verifies purchase tokens with Google servers, so purchases
cannot be faked through the app itself. For unhackable leaderboards / cross-device
referral crediting, add a small server (e.g. Firebase) that validates receipts and
scores — the code is structured so `Prefs`/`Leaderboard` can be swapped to remote
sources without touching the UI.

### Going live with real money

1. **Play Billing** — create products in Play Console matching `BillingManager.PRODUCT_IDS`
   (`remove_ads` ₹99, `coins_120/400/1000/2500`). Prices fall back to documented defaults
   until products exist.
2. **AdMob** — replace the Google TEST ids in `AdsManager` (`BANNER_ID`,
   `INTERSTITIAL_ID`, `REWARDED_ID`) and the `APPLICATION_ID` meta-data in
   `AndroidManifest.xml` with your own units; buy `remove_ads` to hide all ads.
3. **Play Games Services** — link the app in Play Console → Play Games Services →
   configuration; sign-in then upgrades the local profile automatically.
4. Replace the debug signing config in `app/build.gradle.kts` with your keystore.

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

The workflow YAML ships in this repo twice: [.github/workflows/android-build.yml](.github/workflows/android-build.yml)
and a plain-text mirror at [ci/android-build.yml.txt](ci/android-build.yml.txt).
(The mirror exists because some GitHub tokens are not allowed to push files into
`.github/workflows/` — if the real workflow file is missing on your branch, add it once by hand:
**Actions → New workflow → set up a workflow yourself** → paste the contents of
`ci/android-build.yml.txt` → **Commit changes**. That's it — everything builds automatically after that.)

The workflow runs on every push and on demand:

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

## Monetization & live-ops

The game ships with a full money-making stack, wired with **Google TEST credentials** so it is
safe to run anywhere. Swap in your real credentials before publishing:

| Feature | Where to configure |
| --- | --- |
| **No-Ads pack (₹99)** | Play Console product id `remove_ads` (non-consumable). Thumbnail: `app/src/main/res/drawable-nodpi/pack_no_ads.png` |
| **Coin packs ₹29–₹299** | Play Console products `coins_120`, `coins_400`, `coins_1000`, `coins_2500` (consumables) |
| **AdMob banner / interstitial / rewarded** | Replace test ids in `AndroidManifest.xml` (`APPLICATION_ID`) and `monetize/AdsManager.kt` |
| **Rewarded revives & free coins** | Works out of the box (`REVIVE ▶ AD` on game over, `FREE +50 COINS` in shop) |
| **Daily events** | `game/Events.kt` — Coin Rush ×2, Mystery Mayhem, Slot Sale, … rotates by weekday |
| **Weekly leaderboard** | `game/Leaderboard.kt` — offline 50-player board; swap `weeklyBoard()` for Play Games Services later |
| **Daily rewards** | `game/DailyRewards.kt` — 7-day streak calendar, day-7 mega prize |

Billing and ads degrade gracefully: without Play services or products created, buttons simply
show their default INR price and purchases/ads are inert — nothing crashes.

## Tech notes

* Kotlin 2.0.21, AGP 8.5.2, Compose BOM 2024.09.03, minSdk 24 / targetSdk 34.
* Portrait-locked single activity; scene is letterboxed from a 1080×2280 design space.
* Blocking check uses exact **swept SAT** collision (convex hull of the slid body), identical in
  spirit to the original game's slide-out rule.
* All vehicle/NPC art is rendered procedurally (premium glossy cars with glass, hubcaps,
  mirrors, taxi/bus variants; pod passengers with arms, feet and hair caps) — plus one
  AI-designed pack thumbnail. Everything else stays asset-free.
