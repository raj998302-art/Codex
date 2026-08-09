package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.codex.carjam.game.Net
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager
import com.codex.carjam.monetize.AdsManager
import com.codex.carjam.monetize.BillingManager
import com.codex.carjam.monetize.PlayGamesManager

private sealed interface Screen {
    data object Splash : Screen

    data object Home : Screen

    data class Game(val level: Int, val attempt: Int, val practice: Boolean) : Screen

    // v3.3 full-page experiences (reference banner screens)
    data object SkinShop : Screen

    data object Collection : Screen

    data object Leaderboard : Screen

    data object Profile : Screen
}

@Composable
fun CarJamApp() {
    val viewContext = LocalContext.current
    val context = LocalContext.current.applicationContext
    val prefs = remember { Prefs(context) }
    val sound = remember { SoundManager(prefs) }
    val billing = remember { BillingManager(context, prefs) }
    val ads = remember { AdsManager(context, prefs) }
    val pgs = remember { PlayGamesManager(context) }
    var online by remember { mutableStateOf(Net.isOnline(context)) }

    LaunchedEffect(Unit) {
        ads.initialize()
        pgs.initialize()
        SoundManager.active = sound
        sound.attach(context)
    }
    DisposableEffect(Unit) {
        onDispose {
            if (SoundManager.active === sound) SoundManager.active = null
            sound.release()
            billing.release()
        }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val splashProgress = remember { Animatable(0f) }
    var splashDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        splashProgress.animateTo(1f, tween(1600))
        splashDone = true
        if (Net.isOnline(context)) {
            online = true
            screen = Screen.Home
        }
    }

    when (val s = screen) {
        Screen.Splash -> SplashScreen(
            progress = splashProgress.value,
            showOfflineGate = splashDone && !online,
            onRetry = {
                val now = Net.isOnline(context)
                online = now
                if (now) screen = Screen.Home
            },
            onPlayOffline = {
                online = false
                screen = Screen.Home
            },
        )

        Screen.Home -> HomeScreen(
            prefs = prefs,
            sound = sound,
            ads = ads,
            billing = billing,
            pgs = pgs,
            online = online,
            onPlay = { lvl -> screen = Screen.Game(lvl, attempt = 0, practice = false) },
            onPractice = { screen = Screen.Game(prefs.maxLevel.intValue, attempt = 0, practice = true) },
            onRefreshNet = { online = Net.isOnline(context) },
            onOpenSkinShop = { screen = Screen.SkinShop },
            onOpenCollection = { screen = Screen.Collection },
            onOpenLeaderboard = { screen = Screen.Leaderboard },
            onOpenProfile = { screen = Screen.Profile },
        )

        Screen.SkinShop -> SkinShopScreen(
            prefs = prefs,
            ads = ads,
            onNav = { tab ->
                screen = when (tab) {
                    BannerTab.HOME -> Screen.Home
                    BannerTab.COLLECTION -> Screen.Collection
                    BannerTab.LEADERBOARD -> Screen.Leaderboard
                    else -> Screen.SkinShop
                }
            },
        )

        Screen.Collection -> CollectionScreen(
            prefs = prefs,
            onNav = { tab ->
                screen = when (tab) {
                    BannerTab.HOME -> Screen.Home
                    BannerTab.SKIN -> Screen.SkinShop
                    BannerTab.LEADERBOARD -> Screen.Leaderboard
                    else -> Screen.Collection
                }
            },
        )

        Screen.Leaderboard -> LeaderboardScreen(
            prefs = prefs,
            onNav = { tab ->
                screen = when (tab) {
                    BannerTab.HOME -> Screen.Home
                    BannerTab.COLLECTION -> Screen.Collection
                    BannerTab.SKIN -> Screen.SkinShop
                    else -> Screen.Leaderboard
                }
            },
            onOpenProfile = { screen = Screen.Profile },
        )

        Screen.Profile -> (viewContext as? Activity)?.let { act ->
            ProfileScreen(
                prefs = prefs,
                ads = ads,
                activity = act,
                onOpenShop = { screen = Screen.Home },
                onClose = { screen = Screen.Home },
            )
        }

        is Screen.Game -> GameScreen(
            level = s.level,
            attempt = s.attempt,
            practice = s.practice,
            prefs = prefs,
            sound = sound,
            ads = ads,
            billing = billing,
            onHome = { screen = Screen.Home },
            onNext = { screen = Screen.Game(s.level + 1, attempt = 0, practice = s.practice) },
            onRetry = { screen = Screen.Game(s.level, attempt = s.attempt + 1, practice = s.practice) },
        )
    }
}
