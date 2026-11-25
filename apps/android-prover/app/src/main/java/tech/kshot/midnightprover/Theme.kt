package tech.kshot.midnightprover

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Midnight design system.
 *
 * Deep-space navy surfaces, an indigo-to-violet brand gradient, and a
 * single cyan "proof glow" reserved for live status and success. Dark
 * only: a prover that runs at midnight should look like it.
 */
object MidnightPalette {
    val Abyss = Color(0xFF05070F)        // app background, deepest layer
    val DeepSpace = Color(0xFF0A0F24)    // screen background
    val Nebula = Color(0xFF101736)       // card surface
    val NebulaHigh = Color(0xFF182050)   // raised surface / variant
    val Starlight = Color(0xFFE9ECFF)    // primary text
    val Moonlight = Color(0xFFA7B0DC)    // secondary text
    val DimStar = Color(0xFF6B76A8)      // tertiary text
    val Indigo = Color(0xFF4D6BFE)       // brand gradient start
    val Violet = Color(0xFF8B5CF6)       // brand gradient end
    val ProofGlow = Color(0xFF3EE6FF)    // live status / success glow
    val WarningAmber = Color(0xFFFFC24D)
    val ErrorRose = Color(0xFFFF6B81)
    val Hairline = Color(0xFF232C5E)     // borders on navy

    val BrandGradient = Brush.linearGradient(listOf(Indigo, Violet))
    val GlowGradient = Brush.linearGradient(listOf(ProofGlow, Indigo))
}

private val MidnightScheme = darkColorScheme(
    primary = MidnightPalette.Violet,
    onPrimary = Color.White,
    secondary = MidnightPalette.ProofGlow,
    background = MidnightPalette.DeepSpace,
    onBackground = MidnightPalette.Starlight,
    surface = MidnightPalette.Nebula,
    onSurface = MidnightPalette.Starlight,
    surfaceVariant = MidnightPalette.NebulaHigh,
    onSurfaceVariant = MidnightPalette.Moonlight,
    outline = MidnightPalette.Hairline,
    error = MidnightPalette.ErrorRose
)

@Composable
fun MidnightProverTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MidnightScheme, content = content)
}
