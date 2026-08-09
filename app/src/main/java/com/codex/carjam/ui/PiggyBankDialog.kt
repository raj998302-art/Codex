package com.codex.carjam.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.CloudSave
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.monetize.BillingManager

private val PigInk = Color(0xFF154A2A)

/**
 * v3.0 Piggy Bank — every win drops 20 coins into the pig (cap 800).
 * When it is FULL the player can break it with a small one-time purchase
 * and all saved coins flood into their wallet (Google Play Billing only).
 */
@Composable
fun PiggyBankDialog(
    prefs: Prefs,
    billing: BillingManager,
    activity: Activity,
    onClose: () -> Unit,
) {
    val fill = prefs.piggy.intValue
    val frac = fill / Prefs.PIGGY_CAP.toFloat()
    val price = billing.priceFor(BillingManager.PRODUCT_PIGGY) ?: "₹210.00"

    DialogOverlay {
        PanelCard(Modifier.width(340.dp)) {
            DialogTitleText("PIGGY BANK")
            SpacerH(8.dp)
            GameIcon(GameIconKind.PIGGY, 84.dp)
            SpacerH(8.dp)
            // fill bar (mirrors the reference "500 ←→ 800" meter)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color.White, RoundedCornerShape(15.dp))
                    .border(3.dp, Color(0xFFBFE8C8), RoundedCornerShape(15.dp)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .height(30.dp)
                        .background(Color(0xFF58D76B), RoundedCornerShape(15.dp)),
                )
                BasicText(
                    "$fill / ${Prefs.PIGGY_CAP}",
                    style = TextStyle(color = PigInk, fontSize = 14.sp, fontWeight = FontWeight.Black),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            SpacerH(10.dp)
            BasicText(
                "Beat levels to collect coins; break the piggy bank when it is FULL!",
                style = TextStyle(
                    color = Color(0xFF8C6A3F),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(6.dp)
            BasicText(
                "+20 coins per win",
                style = TextStyle(color = Color(0xFF2FA84F), fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold),
            )
            SpacerH(14.dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SquishyButton(
                    if (prefs.piggyFull) "BREAK  $price" else "FILLING…  ${fill * 100 / Prefs.PIGGY_CAP}%",
                    onClick = {
                        if (prefs.piggyFull && billing.ready.value) {
                            billing.launchPurchase(activity, BillingManager.PRODUCT_PIGGY)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    top = if (prefs.piggyFull && billing.ready.value) Color(0xFF6FEE85) else Color(0xFFC7BBA6),
                    bottom = if (prefs.piggyFull && billing.ready.value) Color(0xFF1FA94F) else Color(0xFFA89B86),
                    height = 50.dp,
                    textSize = 15.dp,
                    icon = { GameIcon(GameIconKind.PIGGY, 22.dp) },
                )
            }
            SpacerH(6.dp)
            BasicText(
                if (billing.ready.value) {
                    if (prefs.piggyFull) "Full! Break it open to grab all ${Prefs.PIGGY_CAP} coins." else "Keep winning — $fill/${Prefs.PIGGY_CAP} saved."
                } else {
                    "Store connecting…"
                },
                style = TextStyle(color = Color(0xFF8C6A3F), fontSize = 11.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(10.dp)
            SquishyButton(
                "CLOSE",
                onClick = onClose,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 44.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}
