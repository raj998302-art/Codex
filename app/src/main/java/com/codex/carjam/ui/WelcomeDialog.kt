package com.codex.carjam.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.R
import com.codex.carjam.game.render.GameIconKind

/**
 * First-run onboarding: hero showroom art + "continue with Google Play Games".
 * connect → Play Games sign-in sheet (syncs progress, rankings, gamer name).
 * guest   → local profile only, they can still connect later from PROFILE.
 */
@Composable
fun WelcomeDialog(onContinuePlay: () -> Unit, onGuest: () -> Unit) {
    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            // hero showroom card (3D hero car on a showroom-lit gradient)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFF4FAFF), Color(0xFFCBE4FB))),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.hero_car),
                    contentDescription = "Your new ride",
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(168.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            SpacerH(16.dp)
            OutlinedTextC("WELCOME RACER!", 24.dp, outline = Color(0xFF7A4A00))
            SpacerH(8.dp)
            BasicText(
                "Connect Google Play Games to save your progress, chase the rankings and pick the jam up on any phone.",
                style = TextStyle(
                    color = Color(0xFF8C6A3F),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(16.dp)
            SquishyButton(
                "CONTINUE WITH GOOGLE PLAY GAMES",
                onClick = onContinuePlay,
                top = Color(0xFF6FB6FF),
                bottom = Color(0xFF2E6BD6),
                textSize = 13.dp,
                height = 50.dp,
                icon = { GameIcon(GameIconKind.GAMEPAD, 24.dp) },
            )
            SpacerH(10.dp)
            BasicText(
                text = "Play as guest",
                style = TextStyle(
                    color = Color(0xFF8C6A3F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textDecoration = TextDecoration.Underline,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onGuest() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            SpacerH(2.dp)
        }
    }
}
