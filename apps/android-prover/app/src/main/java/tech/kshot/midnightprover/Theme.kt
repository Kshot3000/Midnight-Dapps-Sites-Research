package tech.kshot.midnightprover

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

/**
 * Midnight design system.
 *
 * Deep-space navy surfaces, an indigo-to-violet brand gradient, and a
 * single cyan "proof glow" reserved for live status and success. Dark
 * only: a prover that runs at midnight should look like it.
 *
 * GROK-15 contrast pass: every text colour below meets WCAG AA (4.5:1) on
 * the card surface [Nebula]; `PaletteContrastTest` enforces it. DimStar and
 * the button gradient were darkened/lightened from v0.3.1 (3.99:1 and
 * 4.2:1 before).
 */
object MidnightPalette {
    val Abyss = Color(0xFF05070F)        // app background, deepest layer
    val DeepSpace = Color(0xFF0A0F24)    // screen background
    val Nebula = Color(0xFF101736)       // card surface
    val NebulaHigh = Color(0xFF182050)   // raised surface / variant
    val Starlight = Color(0xFFE9ECFF)    // primary text
    val Moonlight = Color(0xFFA7B0DC)    // secondary text
    val DimStar = Color(0xFF8590C4)      // tertiary text (AA on Nebula)
    val Indigo = Color(0xFF4D6BFE)       // brand accent (decoration)
    val Violet = Color(0xFF8B5CF6)       // brand accent (decoration, borders)
    val VioletText = Color(0xFFA78BFA)   // violet for text (AA on Nebula)
    val ButtonStart = Color(0xFF3D5AF1)  // gradient button, white text ≥ 5:1
    val ButtonEnd = Color(0xFF6D4AE0)
    val ProofGlow = Color(0xFF3EE6FF)    // live status / success glow
    val WarningAmber = Color(0xFFFFC24D)
    val ErrorRose = Color(0xFFFF6B81)
    val Hairline = Color(0xFF232C5E)     // decorative card borders on navy
    val FieldBorder = Color(0xFF5A6393)  // input borders, ≥ 3:1 (WCAG 1.4.11; Bob P1-14)

    val BrandGradient = Brush.linearGradient(listOf(ButtonStart, ButtonEnd))
    val GlowGradient = Brush.linearGradient(listOf(ProofGlow, Indigo))
}

/** WCAG 2.x contrast ratio. Pure + tested. */
object Contrast {
    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    fun luminance(c: Color): Double = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    fun ratio(a: Color, b: Color): Double {
        val la = luminance(a); val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }
}

private val MidnightScheme = darkColorScheme(
    primary = MidnightPalette.VioletText,
    onPrimary = Color(0xFF12082E),
    primaryContainer = MidnightPalette.ButtonEnd,
    onPrimaryContainer = Color.White,
    secondary = MidnightPalette.ProofGlow,
    onSecondary = Color(0xFF00222A),
    tertiary = MidnightPalette.WarningAmber,
    background = MidnightPalette.DeepSpace,
    onBackground = MidnightPalette.Starlight,
    surface = MidnightPalette.Nebula,
    onSurface = MidnightPalette.Starlight,
    surfaceVariant = MidnightPalette.NebulaHigh,
    onSurfaceVariant = MidnightPalette.Moonlight,
    surfaceContainer = MidnightPalette.Nebula,
    surfaceContainerHigh = MidnightPalette.NebulaHigh,
    outline = MidnightPalette.FieldBorder,
    outlineVariant = MidnightPalette.Hairline,
    error = MidnightPalette.ErrorRose,
    onError = Color(0xFF2B0710)
)

private val base = Typography()
private val MidnightTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
)

/** Monospace style for hex, addresses and URLs. */
val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 18.sp)

/** True when animations should be reduced (system "remove animations" or the in-app switch). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun MidnightProverTheme(reduceMotion: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = MidnightScheme,
            typography = MidnightTypography,
            shapes = Shapes(
                small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp)
            ),
            content = content
        )
    }
}
