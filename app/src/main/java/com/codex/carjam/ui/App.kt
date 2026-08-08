package com.codex.carjam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.SoundManager

private sealed interface Screen {
    data object Home : Screen

    data class Game(val level: Int, val attempt: Int) : Screen
}

@Composable
fun CarJamApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context.applicationContext) }
    val sound = remember { SoundManager(prefs) }
    DisposableEffect(Unit) {
        onDispose { sound.release() }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            prefs = prefs,
            sound = sound,
            onPlay = { lvl -> screen = Screen.Game(lvl, attempt = 0) },
        )

        is Screen.Game -> GameScreen(
            level = s.level,
            attempt = s.attempt,
            prefs = prefs,
            sound = sound,
            onHome = { screen = Screen.Home },
            onNext = { screen = Screen.Game(s.level + 1, attempt = 0) },
            onRetry = { screen = Screen.Game(s.level, attempt = s.attempt + 1) },
        )
    }
}
