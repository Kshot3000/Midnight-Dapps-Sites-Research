package tech.kshot.midnightprover

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/**
 * A deterministic starfield with a crescent moon, drawn once and kept
 * behind every screen. Stars twinkle by re-drawing alpha on a slow
 * infinite transition; positions never change between frames.
 */
@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        val rnd = Random(20261124)
        List(110) {
            Triple(
                rnd.nextFloat(),
                rnd.nextFloat(),
                0.6f + rnd.nextFloat() * 1.9f // radius dp-ish
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "twinkle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(7000), RepeatMode.Restart),
        label = "phase"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MidnightPalette.Abyss, MidnightPalette.DeepSpace, Color(0xFF0D1440))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEachIndexed { i, (fx, fy, r) ->
                val tw = 0.45f + 0.4f * sin(phase + i * 1.7f)
                drawCircle(
                    color = Color.White.copy(alpha = tw.coerceIn(0.08f, 0.9f)),
                    radius = r,
                    center = Offset(fx * size.width, fy * size.height)
                )
            }
            // Crescent moon, top-right: a lit disc minus an offset shadow disc.
            val moonR = size.minDimension * 0.055f
            val cx = size.width * 0.86f
            val cy = size.height * 0.10f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFF4F7FF), Color(0xFFC9D4FF)),
                    center = Offset(cx, cy),
                    radius = moonR
                ),
                radius = moonR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = MidnightPalette.Abyss,
                radius = moonR * 0.88f,
                center = Offset(cx + moonR * 0.42f, cy - moonR * 0.18f)
            )
        }
    }
}

/** Pulsing status orb: cyan when live, amber when busy, dim when unknown, rose on failure. */
@Composable
fun StatusOrb(core: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(modifier = modifier.size(46.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(core.copy(alpha = 0.45f * pulse), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(14.dp)
                .padding(0.dp)
                .clip(CircleShape)
                .background(core)
                .align(androidx.compose.ui.Alignment.Center)
        )
    }
}

/** Primary call-to-action in the brand gradient. */
@Composable
fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = MidnightPalette.DimStar
        ),
        modifier = modifier
            .heightIn(min = 52.dp) // Bob P2 / Joe P2-8: 52–56dp primary action
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) MidnightPalette.BrandGradient
                else Brush.linearGradient(
                    listOf(MidnightPalette.NebulaHigh, MidnightPalette.NebulaHigh)
                )
            )
    ) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
    }
}

/** Small rounded label chip. */
@Composable
fun MidnightChip(text: String, accent: Color = MidnightPalette.ProofGlow, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(50))
            .background(MidnightPalette.NebulaHigh.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = accent, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
    }
}
