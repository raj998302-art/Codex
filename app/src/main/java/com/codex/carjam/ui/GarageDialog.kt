package com.codex.carjam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.carjam.game.CarColor
import com.codex.carjam.game.Garage
import com.codex.carjam.game.Prefs
import com.codex.carjam.game.render.GameIconKind
import com.codex.carjam.game.render.Painters

private val GDark = Color(0xFF4A3826)
private val GMuted = Color(0xFF8C6A3F)

/** Live top-down preview of a ride, drawn by the very painter the game uses. */
@Composable
private fun RidePreview(ride: Garage.Ride, heightDp: androidx.compose.ui.unit.Dp) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(heightDp),
    ) {
        val bodyLen = ride.type.len
        val scale = (size.height * 0.86f) / bodyLen
        with(Painters) {
            drawCar(
                size.width / 2f,
                size.height / 2f,
                0f,
                ride.type,
                CarColor.BLUE,
                scale = scale,
                variant = ride.variant,
            )
        }
    }
}

/** The car collection: showroom cards with unlock/select, painted live. */
@Composable
fun GarageDialog(prefs: Prefs, onChanged: () -> Unit, onClose: () -> Unit) {
    val selected = Garage.selected(prefs)
    DialogOverlay {
        PanelCard(Modifier.width(360.dp)) {
            DialogTitleText("MY GARAGE")
            SpacerH(6.dp)
            BasicText(
                "${prefs.ownedRides.value.size} / ${Garage.rides.size} rides unlocked — your pick stars on the profile.",
                style = TextStyle(color = GMuted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(10.dp)
            // featured showroom strip
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFD9EAFB))),
                        RoundedCornerShape(18.dp),
                    )
                    .border(2.dp, Color(0xFF9DC7F2), RoundedCornerShape(18.dp))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RidePreview(selected, 150.dp)
                BasicText(selected.title, style = TextStyle(color = GDark, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp))
                BasicText(selected.blurb, style = TextStyle(color = GMuted, fontSize = 11.5.sp), modifier = Modifier.padding(top = 2.dp))
            }
            SpacerH(10.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                for (ride in Garage.rides) {
                    if (ride.id == selected.id) continue
                    RideRow(prefs = prefs, ride = ride, onChanged = onChanged)
                    SpacerH(8.dp)
                }
            }
            SpacerH(8.dp)
            SquishyButton(
                "CLOSE",
                onClick = onClose,
                top = Color(0xFF9AA5B1),
                bottom = Color(0xFF6E7883),
                height = 46.dp,
                textSize = 15.dp,
            )
            SpacerH(4.dp)
        }
    }
}

@Composable
private fun RideRow(prefs: Prefs, ride: Garage.Ride, onChanged: () -> Unit) {
    val owned = Garage.owned(prefs, ride)
    val afford = Garage.canAfford(prefs, ride)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, if (owned) Color(0xFF3DDC5F) else Color(0xFFE3B36B), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(64.dp)
                .height(76.dp)
                .background(Color(0xFFF0F7FF), RoundedCornerShape(12.dp)),
        ) {
            RidePreview(ride, 76.dp)
        }
        SpacerW(8.dp)
        Column(Modifier.weight(1f)) {
            BasicText(ride.title, style = TextStyle(color = GDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold))
            BasicText(ride.blurb, style = TextStyle(color = GMuted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold))
            SpacerH(4.dp)
            if (!owned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ride.priceCoins > 0) {
                        CoinIcon(16.dp)
                        SpacerW(4.dp)
                        BasicText("${ride.priceCoins}", style = TextStyle(color = GDark, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                    }
                    if (ride.priceGems > 0) {
                        if (ride.priceCoins > 0) SpacerW(8.dp)
                        GameIcon(GameIconKind.GEM, 15.dp)
                        SpacerW(4.dp)
                        BasicText("${ride.priceGems}", style = TextStyle(color = Color(0xFF0E7BC0), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }
        SpacerW(6.dp)
        when {
            owned -> SquishyButton(
                "SELECT",
                onClick = { prefs.selectRide(ride.id); onChanged() },
                modifier = Modifier.width(84.dp),
                top = Color(0xFF6FEE85),
                bottom = Color(0xFF1FA94F),
                height = 34.dp,
                textSize = 12.dp,
            )

            else -> SquishyButton(
                if (afford) "UNLOCK" else "LOCKED",
                onClick = {
                    if (Garage.unlock(prefs, ride)) {
                        prefs.selectRide(ride.id)
                        onChanged()
                    }
                },
                modifier = Modifier.width(84.dp),
                top = if (afford) Color(0xFF6FB6FF) else Color(0xFFC7BBA6),
                bottom = if (afford) Color(0xFF3B7FE0) else Color(0xFFA89B86),
                height = 34.dp,
                textSize = 12.dp,
            )
        }
    }
}
