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
3. **Play Games Services (Google Play sign-in)** — full setup:
   1. Play Console → your app → **Grow → Play Games Services → Setup & management**.
   2. Create a Play Games Services project (it links/creates a Google Cloud project).
   3. In the linked Cloud project, enable the **Google Play Games Services API** and
      create an **OAuth 2.0 Android client** with your package name
      (`com.codex.carjam` or `com.codex.carjam.debug`) + the **SHA-1** of your signing
      key (find it under Play Console → Release → Setup → **App integrity**).
   4. Back in Play Console, save & **publish** the Play Games Services configuration,
      and add your Gmail to **Testers** while it is in testing.
   5. Copy the 12-digit **project number** from the configuration page into
      `app/src/main/res/values/games_services.xml` (`games_app_id`).
   The game then shows the official account picker at first launch, silent-signs
   every session, and syncs the gamer name into PROFILE. Until then it no-ops
   gracefully — nothing breaks.
4. **Payments = Google Play Billing only.** All packs route through
   `BillingManager` — the Play-policy-compliant path for digital goods.
5. Replace the debug signing config in `app/build.gradle.kts` with your keystore.

## Real-time leaderboard backend (MongoDB Atlas + Render)

The leaderboard is a live, no-demo-data global board backed by MongoDB. The app
**never** talks to Mongo directly — an APK can be decompiled, so the connection
string would leak and anyone could wipe your database. Instead a tiny Node
service (in `backend/`) holds the credentials and exposes two HTTPS endpoints
(`POST /api/score`, `GET /api/leaderboard`). Ratings are re-computed and clamped
server-side from the raw components (level/coins/streak), so a tampered client
cannot post a fake score.

Deploy once (≈5 minutes, free):

1. **MongoDB Atlas** — your cluster already exists. In Atlas → **Network Access**,
   allow `0.0.0.0/0` (Render free instances use dynamic outbound IPs). Keep the DB
   user password private — if it was ever shared anywhere, rotate it in Atlas →
   **Database Access** first.
2. **Render** — render.com → sign in with GitHub → **New → Blueprint** → pick this
   repo (it reads `render.yaml` and sets everything up). When it asks, paste your
   MongoDB connection string as `MONGODB_URI` (`mongodb+srv://…/carjam`). One minute
   later the service is live at `https://<service-name>.onrender.com` (check
   `/healthz` → `{ "ok": true }`).
3. **Wire the app** — paste that URL into `LeaderboardApi.BASE_URL`
   (`app/src/main/java/com/codex/carjam/game/LeaderboardApi.kt`), commit, push —
   CI ships a new APK and the RANK screen goes live worldwide.

Notes: free Render instances sleep when idle — the first leaderboard sync after a
quiet hour takes ~20 s while it wakes; every sync after that is instant. When the
service or network is unreachable the dialog shows an honest OFFLINE card with the
player's own saved stats (still zero demo bots).


## v2.1 polish (AAA feedback pass)

| System | What changed |
| --- | --- |
| **Zero-emoji UI** | 20 hand-drawn vector icons in `game/render/Icons.kt` (cart, gift, trophy, gems, medals, locks…) replace every emoji in the interface |
| **Event banners** | Designed `EventBannerCard` with radial-burst art on home, in the events dialog, and as a once-per-day live-ops popup |
| **💎 Gems currency** | Second premium currency (vault-secured): shop packs `gems_80/250/700`, daily-reward day 3/6/7 grants, gem-revive (25 gems) next to the ad revive |
| **Shop pack art** | AI-generated banners for coin packs and gem packs (like the ₹99 No-Ads pack) |
| **Packed boards** | Difficulty ramp rebuilt: 17 → 60+ cars, 9-col grids, mixed-angle dense diagonals, 7 late-game slots — the jam now looks full |

## v2.2 (creator drop)

| System | What changed |
| --- | --- |
| **AI key art pipeline** | Concept renders generated for the brand: full-bleed cinematic `splash_art` behind a redesigned splash (shimmer progress bar, studio line) and a 3D `hero_car` showroom asset (background-removed) in the welcome screen + level-complete celebration |
| **Car models v3** | Painter rebuilt to match the renders: layered contact shadow, five-spoke alloy wheels, pearlescent 4-stop paint, panel seams, glasshouse with sky reflections, LED headlights/taillights, grille + license plate, mirrors, door handles, taxi check band, police lightbar, bus roof vents |
| **NPC models v3** | The gummy citizens gained happy faces (eyes with catch-lights, smile, blush), hands and a head sheen — straight from the key art |
| **Home HUD fit fix** | Identity chip is flexible (ellipsizes), coin/gem pills get a compact mode — the top bar can no longer overflow on any screen width |
| **First-run onboarding** | New `WelcomeDialog`: hero art + "Continue with Google Play Games" (opens the official account picker) or Play-as-guest; daily-event popup waits until onboarding is done |
| **Play Games plumbing** | `games_app_id` string resource + manifest meta-data wired — paste your PGS project number and sign-in goes live |
| **Razorpay module** | `RazorpayManager` (Checkout 1.6.41, pinned core) with UPI/cards checkout, pack mirror + reward crediting; disabled by default for Play-Store policy compliance |
| **Polish** | Sonar pulse ring on PLAY, level-complete showroom strip, v2.2/versionCode 3 |

## v2.3 (MongoDB real-time leaderboard)

| System | What changed |
| --- | --- |
| **Real-time leaderboard** | Demo bots deleted. The RANK screen is now a live global board: avatar + level + server-verified rating per player, medals 1-3, pinned "YOU" row, LIVE/OFFLINE badge, auto-refresh every 20 s while open |
| **Secure sync** | Score is pushed on every win + app open; the server recomputes the rating from components (same formula as `Prefs.rating()`) with hard clamps — fake scores can't exist server-side |
| **Backend in-repo** | `backend/` = Node + Express + Mongoose service, one-click `render.yaml` Blueprint; the MongoDB URI lives only in Render env vars, never in git or the APK |
| **Payments simplified** | Razorpay experiment removed — everything goes through Google Play Billing (policy-safe) |
| **Assets** | Splash art re-encoded at 1296-wide q90, hero car re-exported at 1200px transparent — retina-crisp, bigger APK allowed |

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
