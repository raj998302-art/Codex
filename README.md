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

### Security model (v2.4)

Defense in depth, end to end:

1. **Local wallet** — coins/gems/earnings/no-ads pass through `SecureVault`:
   SHA-256 checksums salted per install, mirror auto-restore and a tamper counter.
2. **Cloud save** — the whole wallet + progress (level, streaks, stats, avatar,
   name, referrals) syncs to MongoDB through `backend/`. The account credential
   is a **256-bit sync key** generated on first launch; the server stores only
   its SHA-256 hash, so even a full database dump can't leak it. No emails, no
   passwords, nothing to phish.
3. **Server-side economy clamps** — the backend applies the same absolute caps
   as the client PLUS per-hour growth budgets (coins ≤50k/h, gems ≤300/h,
   level ≤30/h…). A tampered client posting 999,999,999 coins gets shrunk to a
   plausible value, permanently.
4. **Purchases never sync** — no-ads is granted exclusively by Google Play's
   own restore flow (`restorePurchases`), so a forged cloud save cannot mint
   paid content. Real purchases restore on any device via the same Google
   account; everything else restores via the sync key.
5. **Leaderboard** — ratings are recomputed server-side from raw components.
6. **Transport** — HTTPS only; the MongoDB URI exists solely in Render env vars.

Remaining honest caveat: on a rooted device with a repacked client, stats can
still be inflated **within** the hourly budgets — meaningful wallet/value theft
(fake purchases, fake leaderboard toppers, massive coins) is blocked, not the
last few "level-ups per hour". Server-side play-pacing heuristics can tighten
that later without client changes.

### Moving to a new phone (account transfer)

SETTINGS → **MY SYNC CODE** shows the account key (copy it). On the new device:
SETTINGS → **RESTORE SAVE** → paste → the entire account (level, coins, gems,
streak, stats) adopts onto the device. Purchases additionally restore from
PLAY STORE (same Google account → SHOP → *Restore purchases*).

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

## v2.5 (quests + seasons)

| System | What changed |
| --- | --- |
| **Daily missions** | New QUESTS home chip: 3 seeded missions every day (wins / passengers seated / coins collected / new levels) with progress bars and claimable coin+gem rewards; practice mode never counts |
| **Achievements** | AWARDS tab: 12 lifetime milestones (first win → level 100, 50k coins earned, 500 passengers, 7-day streak) with one-time payouts and OWNED state |
| **Weekly season prizes (auto-credit)** | Backend now freezes a per-week rating board; the first sync after a season ends auto-credits your rank prize (up to 500 coins + 25 gems) with a SEASON PRIZE celebration dialog |
| **Daily-reward hardening** | Claims are idempotent — double-taps, re-fires and replays can never double-grant; badge state is fully reactive |
| **Live counters** | New lifetime passengers-seated counter feeds both missions and achievements (practice excluded) |

## v2.6 (mega content drop)

| System | What changed |
| --- | --- |
| **Frozen cars (ice blockers)** | From level 9 some jam cars arrive ice-locked: tap once (twice from level 25) to crack the frost shell before they can drive out. Frost slab, crack webs that spread per tap, wobble + crack FX; never on mystery cars, never on the opening escapees |
| **New rides in the jam** | Three new painter styles mixed into level traffic: STREET SPORT (racing stripes + spoiler), RESCUE VAN ambulance (lightbar + medical cross) and the SCHOOL BUS (amber beacons, chevron band, STOP badge) |
| **MY GARAGE** | Car collection on the profile: 8 unlockable rides (coins or gems), live-painted showroom strip, SELECT your star ride; unlocks cloud-sync instantly |
| **Background music** | A procedurally composed 24s seamless loop (pads, marimba melody, soft percussion, res/raw/bg_music.wav) that plays app-wide, pauses in background, with a Music toggle in SETTINGS |
| **Sound FX** | New ice-crack tone; music volume kept low (0.32) so taps stay crisp |

## v2.7 (boosters + new blockers)

| System | What changed |
| --- | --- |
| **Ice Hammer booster** | Arm the hammer from the bottom-right belt, tap a frozen car: every ice layer shatters in one blow. Armed taps never move cars and the hammer is only spent on a real smash. Free welcome gift: 2 hammers + 3 mixes |
| **Queue Mix booster** | Rebuilds the waiting line so the front passenger is immediately useful (a parked car hungry for that colour, else a colour that can move) — passenger counts never change, so the level stays exactly as solvable |
| **Chain-locked cars (L11+)** | Cars shackled in iron chains with a padlock: they cannot move until their key car leaves the arena. The key always comes earlier in the elimination order, so levels stay provably solvable; chains snap with a satisfying break FX |
| **Exit gates (L14+)** | One arena side barred by a hazard-striped gate with a live counter badge; lifts after K arena exits. The generator places it on a side whose first use IS the K-th exit — a gate can only delay, never softlock |
| **Booster shop** | BOOSTER BELT dialog (tap an empty booster or the shop): coin-priced ×1/×3 top-ups; counts clamped server-side (economy clamp) and synced with cloud save |
| **New sounds** | Hammer smash, queue mix, chain rattle and chain break tones; hammer armed state has a pulsing hint chip |

## v2.8 (theme maps + difficulty ramp)

| System | What changed |
| --- | --- |
| **6 new theme maps** | Total 11 boards: WINTER (falling snow, snow-capped pines, snowman), SUMMER BEACH (spinning sun, palm, umbrella, beach ball), FROZEN (aurora ribbons, huge ice crystals, drifting snow), LAVA (smoking volcano, lava pool, rising embers), JUNGLE (canopy leaves, vines, wildflowers, fireflies), NEON NIGHT (twinkling stars, moon, lit-window skyline) |
| **Maps unlock as you climb** | Themes join the rotation progressively — Beach L6, Winter L9, Jungle L13, Frozen L16, Lava L21, Neon Night L26; board layouts too (Diagonal L3, Spiral L5, Disc L8, Heart L13). Home screen backdrop mirrors the same unlock pool |
| **Theme-linked difficulty** | FROZEN boards pack 1.6x frozen cars ("ice bhare" levels), LAVA boards 1.45x chain-locks, NEON NIGHT hides an extra mystery car |
| **Difficulty ramps harder** | Frozen chance cap raised (0.22 → 0.30, steeper slope), double-tap ice 40% at L25+ and 55% at L40+, second chain-lock gets likelier past L40 — every ramp tweak stays inside the generator's provably-solvable rules |

## v2.4 (cloud save + hardened security)

| System | What changed |
| --- | --- |
| **Cloud save (MongoDB)** | Level, coins, gems, streaks, wins/losses, avatar, name, referral status — everything backs up to the database and syncs on app-open, every win, every purchase-adjacent action |
| **Sync key accounts** | 256-bit random key per install; server stores only its SHA-256 hash. SETTINGS → MY SYNC CODE / RESTORE SAVE = full account transfer to a new phone |
| **Server economy clamps** | Per-hour growth budgets on every wallet field — fake saves get shrunk, legit speed-runs pass |
| **Purchase isolation** | no-ads never travels through the cloud; Play Billing restore remains the only entitlement path (fake saves can't unlock paid items) |
| **Settings UI** | CLOUD SAVE status dot (synced/offline/syncing) + sync code dialogs with copy & restore flows |

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
