package tech.kshot.midnightprover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * A deterministic starfield with a crescent moon behind every screen.
 * Stars twinkle on a slow transition — frozen when reduced motion is on.
 */
@Composable
fun StarfieldBackground(modifier: Modifier = Modifier, animate: Boolean = true) {
    val reduce = LocalReduceMotion.current || !animate
    val stars = remember {
        val rnd = Random(20261124)
        List(110) { Triple(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.9f) }
    }
    val phase = if (reduce) 1.2f else {
        val transition = rememberInfiniteTransition(label = "twinkle")
        val p by transition.animateFloat(
            0f, 6.28318f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "phase"
        )
        p
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidnightPalette.Abyss, MidnightPalette.DeepSpace, Color(0xFF0D1440))))
            .clearAndSetSemantics { }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEachIndexed { i, (fx, fy, r) ->
                val tw = 0.45f + 0.4f * sin(phase + i * 1.7f)
                drawCircle(Color.White.copy(alpha = tw.coerceIn(0.08f, 0.9f)), r, Offset(fx * size.width, fy * size.height))
            }
            // Wide (unfolded) windows: smaller moon tucked into the corner so it stays clear of the second pane.
            val wide = size.width > size.height
            val moonR = size.minDimension * (if (wide) 0.035f else 0.055f)
            val cx = size.width * (if (wide) 0.955f else 0.88f)
            val cy = size.height * (if (wide) 0.035f else 0.07f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFFF4F7FF), Color(0xFFC9D4FF)), center = Offset(cx, cy), radius = moonR),
                radius = moonR, center = Offset(cx, cy)
            )
            drawCircle(MidnightPalette.Abyss, moonR * 0.88f, Offset(cx + moonR * 0.42f, cy - moonR * 0.18f))
        }
    }
}

/** Status orb: cyan when live, amber when degraded, rose on failure. Pulses unless reduced motion. */
@Composable
fun StatusOrb(color: Color, pulsing: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val pulse = if (reduce || !pulsing) 1f else {
        val t = rememberInfiniteTransition(label = "orb")
        val p by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
        p
    }
    Box(modifier = modifier.size(44.dp).clearAndSetSemantics { }) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * pulse), Color.Transparent)))
        )
        Box(Modifier.size(14.dp).clip(CircleShape).background(color).align(Alignment.Center))
    }
}

/** Indeterminate proving ring with the elapsed time in the middle. Static arc when reduced motion. */
@Composable
fun ProvingRing(elapsedLabel: String, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val angle = if (reduce) 0f else {
        val t = rememberInfiniteTransition(label = "ring")
        val a by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "angle")
        a
    }
    Box(modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(MidnightPalette.Hairline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                Brush.sweepGradient(listOf(MidnightPalette.Violet, MidnightPalette.ProofGlow, MidnightPalette.Violet)),
                angle, if (reduce) 300f else 110f, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(elapsedLabel, style = MaterialTheme.typography.headlineSmall, color = MidnightPalette.Starlight)
            Text("elapsed", style = MaterialTheme.typography.labelMedium, color = MidnightPalette.Moonlight)
        }
    }
}

/** Re-composes every [periodMs] while [running]; returns now in millis. */
@Composable
fun rememberTicker(running: Boolean, periodMs: Long = 250): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(running) {
        while (running) { now = System.currentTimeMillis(); delay(periodMs) }
        now = System.currentTimeMillis()
    }
    return now
}

/** Primary call-to-action in the brand gradient. At least 52dp tall. */
@Composable
fun GradientButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, disabledContainerColor = Color.Transparent,
            contentColor = Color.White, disabledContentColor = MidnightPalette.DimStar
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) MidnightPalette.BrandGradient
                else Brush.linearGradient(listOf(MidnightPalette.NebulaHigh, MidnightPalette.NebulaHigh))
            )
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary outlined action, 48dp minimum. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Color = MidnightPalette.Starlight
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) MidnightPalette.Hairline.copy(alpha = 1f) else MidnightPalette.Hairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent, disabledContentColor = MidnightPalette.DimStar),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, color = MidnightPalette.VioletText, style = MaterialTheme.typography.labelLarge)
    }
}

/** Translucent navy card with a hairline border. */
@Composable
fun MidnightCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MidnightPalette.Hairline,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MidnightPalette.Nebula.copy(alpha = 0.9f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/** Small rounded label chip. */
@Composable
fun MidnightChip(text: String, accent: Color = MidnightPalette.ProofGlow, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(50))
            .background(MidnightPalette.NebulaHigh.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = accent, style = MaterialTheme.typography.labelMedium)
    }
}

/** Uppercase overline for sections, announced as a heading. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MidnightPalette.ProofGlow,
        modifier = modifier.semantics { heading() }
    )
}

/** Screen title block. */
@Composable
fun ScreenHeader(overline: String, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(overline)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
    }
}

/** Label/value pair row. */
@Composable
fun FactRow(label: String, value: String, valueColor: Color = MidnightPalette.Starlight, mono: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight, modifier = Modifier.weight(0.42f))
        Text(
            value,
            style = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.58f)
        )
    }
}

enum class Tone(val color: Color) {
    INFO(MidnightPalette.ProofGlow), WARN(MidnightPalette.WarningAmber), ERROR(MidnightPalette.ErrorRose), NEUTRAL(MidnightPalette.Moonlight)
}

/** Empty / error / info state with an optional action. Announced as one unit. */
@Composable
fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String,
    tone: Tone = Tone.NEUTRAL,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    MidnightCard(modifier = modifier, borderColor = if (tone == Tone.NEUTRAL) MidnightPalette.Hairline else tone.color.copy(alpha = 0.5f)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title. $message" }
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(tone.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = tone.color, modifier = Modifier.size(22.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
            }
        }
        if (action != null) action()
    }
}

/** Material "content_copy" glyph, drawn here so the app doesn't pull in material-icons-extended. */
val CopyIcon: androidx.compose.ui.graphics.vector.ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder(
        name = "ContentCopy", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).addPath(
        pathData = androidx.compose.ui.graphics.vector.addPathNodes(
            "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 2,-2V7C21,5.9 20.1,5 19,5zM19,21H8V7h11V21z"
        ),
        fill = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Black),
    ).build()
}
