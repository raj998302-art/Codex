package com.codex.carjam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.codex.carjam.game.SoundManager
import com.codex.carjam.ui.CarJamApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = false
        }
        setContent {
            CarJamApp()
        }
    }

    override fun onResume() {
        super.onResume()
        SoundManager.active?.applyMusicPref()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.active?.pauseMusic()
    }
}
