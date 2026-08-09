package com.codex.carjam.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.Referral
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.monetize.PlayGamesManager

private val Ink = Color(0xFF4A3826)
private val Sub = Color(0xFF8C6A3F)

/**
 * Player profile: editable name, 8-avatar DP picker, lifetime stats,
 * Google Play sign-in hook, referral centre and security status.
 */
@Composable
fun ProfileDialog(
    prefs: Prefs,
    pgs: PlayGamesManager,
    activity: Activity,
    onClose: () -> Unit,
) {
    var nameDraft by remember { mutableStateOf(prefs.playerName) }
    var refDraft by remember { mutableStateOf("") }
    var refMsg by remember { mutableStateOf<String?>(null) }

    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            OutlinedTextC(text = "PROFILE", size = 30.dp, fill = Color.White, outline = Color(0xFF7A4A12))
            SpacerH(12.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ---- identity
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White, CircleShape)
                        .border(4.dp, Color(0xFFFFB300), CircleShape)
                        .padding(4.dp),
                ) { AvatarIcon(prefs.avatarId.intValue, 92.dp) }

                SpacerH(10.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(Ink),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                SpacerH(6.dp)
                SquishyButton(
                    "SAVE NAME",
                    onClick = { prefs.setPlayerName(nameDraft) },
                    top = Color(0xFF6FB6FF),
                    bottom = Color(0xFF3B7FE0),
                    height = 38.dp,
                    textSize = 13.dp,
                )

                SpacerH(14.dp)
                BasicText("CHOOSE YOUR DP", style = TextStyle(color = Sub, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp))
                SpacerH(6.dp)
                for (row in listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6, 7))) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (id in row) {
                            val selected = prefs.avatarId.intValue == id
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) Color(0xFFFFF0C2) else Color.White, CircleShape)
                                    .border(if (selected) 4.dp else 2.dp, if (selected) Color(0xFFFFB300) else Color(0xFFDDCEB0), CircleShape)
                                    .clickable { prefs.setAvatar(id) }
                                    .padding(5.dp),
                            ) { AvatarIcon(id, 52.dp) }
                        }
                    }
                    SpacerH(8.dp)
                }

                // ---- stats
                SpacerH(4.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("LEVEL", "${prefs.maxLevel.intValue}", Modifier.weight(1f))
                    StatCell("WINS", "${prefs.wins.intValue}", Modifier.weight(1f))
                    StatCell("WIN %", "${prefs.winRate()}%", Modifier.weight(1f))
                    StatCell("PRACTICE", "${prefs.practiceWins.intValue}", Modifier.weight(1f))
                }

                SpacerH(12.dp)
                // ---- Google Play
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF7FF), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFF9FC3E8), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (pgs.signedIn.value) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GameIcon(GameIconKind.SHIELD, 18.dp)
                            SpacerW(6.dp)
                            BasicText("Google Play connected", style = TextStyle(color = Color(0xFF2FA84F), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
                        }
                        BasicText(
                            pgs.gamerName.value ?: "",
                            style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        )
                    } else {
                        BasicText("GOOGLE PLAY GAMES", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                        SpacerH(6.dp)
                        SquishyButton(
                            "CONNECT",
                            onClick = { pgs.signIn(activity) },
                            height = 40.dp,
                            textSize = 13.dp,
                            icon = { GameIcon(GameIconKind.PLAY_AD, 18.dp) },
                        )
                        SpacerH(4.dp)
                        BasicText(
                            "Cloud profile, achievements & real rankings (activates once registered in Play Console).",
                            style = TextStyle(color = Sub, fontSize = 10.5.sp, textAlign = TextAlign.Center),
                        )
                    }
                }

                SpacerH(12.dp)
                // ---- referral centre
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF6E0), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFFE3B36B), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon(GameIconKind.GIFT, 22.dp)
                        SpacerW(6.dp)
                        BasicText("REFER & EARN", style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                    }
                    SpacerH(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(
                            prefs.myReferralCode,
                            style = TextStyle(color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                        )
                    }
                    SpacerH(6.dp)
                    SquishyButton(
                        "SHARE CODE",
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🚗 Play CAR JAM SOLVER with me! Use my code ${prefs.myReferralCode} in Profile → Refer & Earn for FREE coins!",
                                )
                            }
                            activity.startActivity(Intent.createChooser(send, "Share your code"))
                        },
                        top = Color(0xFFFFB340),
                        bottom = Color(0xFFE07F00),
                        height = 40.dp,
                        textSize = 13.dp,
                    )
                    SpacerH(10.dp)
                    BasicText("Have a friend's code? +${Referral.WELCOME_BONUS} coins!", style = TextStyle(color = Sub, fontSize = 11.5.sp))
                    SpacerH(6.dp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = refDraft,
                            onValueChange = { refDraft = it.uppercase().take(9) },
                            singleLine = true,
                            textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp),
                            cursorBrush = SolidColor(Ink),
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFFDDCEB0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                        SpacerW(8.dp)
                        Box(
                            Modifier
                                .background(Color(0xFF58D76B), RoundedCornerShape(12.dp))
                                .border(2.dp, Color(0xFF28A745), RoundedCornerShape(12.dp))
                                .clickable {
                    refMsg = when (Referral.redeem(prefs, refDraft)) {
                        Referral.Result.SUCCESS -> "Welcome bonus! +${Referral.WELCOME_BONUS} coins added"
                        Referral.Result.OWN_CODE -> "That's your own code — share it with a friend!"
                        Referral.Result.ALREADY_USED -> "Already redeemed on this device."
                        Referral.Result.BAD_FORMAT -> "Format: CJ-XXXXXX"
                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            BasicText("GO", style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold))
                        }
                    }
                    refMsg?.let {
                        SpacerH(6.dp)
                        BasicText(it, style = TextStyle(color = Sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
                    }
                }

                SpacerH(12.dp)
                // ---- security status
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE9F9EC), RoundedCornerShape(14.dp))
                        .border(2.dp, Color(0xFF9BD8A5), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(GameIconKind.SHIELD, 20.dp)
                    SpacerW(8.dp)
                    BasicText(
                        "Vault armed • purchases verified by Google Play" +
                            if (prefs.tamperDetected.intValue > 0) " • shields up ×${prefs.tamperDetected.intValue}" else "",
                        style = TextStyle(color = Color(0xFF2FA84F), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            SpacerH(10.dp)
            SquishyButton("CLOSE", onClick = onClose, top = Color(0xFF9AA5B1), bottom = Color(0xFF6E7883), height = 46.dp, textSize = 15.dp)
            SpacerH(4.dp)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(2.dp, Color(0xFFDDCEB0), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(value, style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
        BasicText(label, style = TextStyle(color = Sub, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp))
    }
}
