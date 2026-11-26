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
 * Midnight design system — monochrome (CLAUDE-AP-8, Kyle's direction
 * 2026-11-26: black and white, the Midnight token mark, professional).
 *
 * True black surfaces, white primary actions and text, and a grayscale
 * ramp for everything else. No hue anywhere: state is carried by icon,
 * label and lightness, never by colour. Dark only.
 *
 * Contrast invariants are unchanged and enforced by PaletteContrastTest:
 * every text colour meets WCAG AA (4.5:1) on the card surface [Nebula]
 * and on [DeepSpace]; the primary button is white with black text
 * (16:1+), replacing the old indigo/violet gradient with white text.
 */
object MidnightPalette {
    val Abyss = Color(0xFF000000)        // app background, deepest layer
    val DeepSpace = Color(0xFF000000)    // screen background
    val Nebula = Color(0xFF101010)       // card surface
    val NebulaHigh = Color(0xFF1C1C1C)   // raised surface / variant
    val Starlight = Color(0xFFFFFFFF)    // primary text
    val Moonlight = Color(0xFFC9C9C9)    // secondary text
    val DimStar = Color(0xFFA3A3A3)      // tertiary text (AA on Nebula)
    val Indigo = Color(0xFFEDEDED)       // accent ramp (was brand indigo)
    val Violet = Color(0xFFCFCFCF)       // accent ramp (was brand violet)
    val VioletText = Color(0xFFFFFFFF)   // accent text = white
    val ButtonStart = Color(0xFFFFFFFF)  // primary button: white
    val ButtonEnd = Color(0xFFE8E8E8)    // primary button gradient foot
    val ProofGlow = Color(0xFFFFFFFF)    // live status / success = white
    val WarningAmber = Color(0xFFD6D6D6) // warning = light gray + icon/label
    val ErrorRose = Color(0xFFF5F5F5)    // error = near-white + icon/label
    val Hairline = Color(0xFF262626)     // decorative card borders
    val FieldBorder = Color(0xFF8A8A8A)  // input borders, >= 3:1 (WCAG 1.4.11)

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
    onPrimary = Color.Black,
    primaryContainer = MidnightPalette.ButtonEnd,
    onPrimaryContainer = Color.Black,
    secondary = MidnightPalette.ProofGlow,
    onSecondary = Color.Black,
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
    onError = Color.Black
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
