package tech.kshot.midnightprover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private data class Page(val icon: ImageVector, val title: String, val body: String)

/**
 * First-run walkthrough: what the app does, where the server runs, testnet
 * only, the server-setup steps (from server-setup/README.md), then connect.
 */
@Composable
fun OnboardingScreen(state: ProverUiState, vm: ProverViewModel, startPage: Int = 0) {
    var page by rememberSaveable { mutableIntStateOf(startPage) }
    val pages = listOf(
        Page(Icons.Filled.Star, Copy.ONBOARD_WELCOME_TITLE, Copy.ONBOARD_WELCOME),
        Page(Icons.Filled.Build, Copy.ONBOARD_WHERE_TITLE, Copy.ONBOARD_WHERE),
        Page(Icons.Filled.Lock, Copy.ONBOARD_TESTNET_TITLE, Copy.ONBOARD_TESTNET),
        Page(Icons.Filled.Build, Copy.ONBOARD_SETUP_TITLE, ""),
        Page(Icons.Filled.Refresh, Copy.ONBOARD_CONNECT_TITLE, "")
    )
    val last = pages.lastIndex
    val reduce = LocalReduceMotion.current
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Midnight Prover · ${page + 1} of ${pages.size}")
                if (page < last) LinkButton(
                    "Skip",
                    onClick = { page = last },
                    color = MidnightPalette.Starlight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MidnightPalette.Abyss.copy(alpha = 0.92f))
                        .testTag("onboarding-skip")
                )
            }
            PageDots(page, pages.size)
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (reduce) fadeIn(snap()) togetherWith fadeOut(snap())
                    else (slideInHorizontally { it / 6 } + fadeIn()) togetherWith fadeOut()
                },
                label = "onboarding",
                modifier = Modifier.weight(1f)
            ) { p ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("onboarding-page-$p"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val pg = pages[p]
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(MidnightPalette.BrandGradient),
                        contentAlignment = Alignment.Center
                    ) { Icon(pg.icon, contentDescription = null, tint = MidnightPalette.Starlight, modifier = Modifier.size(32.dp)) }
                    Text(pg.title, style = MaterialTheme.typography.headlineMedium, color = MidnightPalette.Starlight)
                    when (p) {
                        3 -> {
                            SetupGuide.steps.dropLast(1).forEachIndexed { i, step -> SetupStep(i + 1, step) }
                            Text(Copy.ONBOARD_DATA_NOTE, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
                            Text(Copy.ONBOARD_KEEP_RUNNING, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.WarningAmber)
                        }
                        4 -> {
                            Text(SetupGuide.steps.last().body, style = MaterialTheme.typography.bodyLarge, color = MidnightPalette.Moonlight)
                            ServerStatusCard(state, onCheck = vm::checkServer)
                            Text(SetupGuide.PHONE_STATUS, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.DimStar)
                        }
                        else -> Text(pg.body, style = MaterialTheme.typography.bodyLarge, color = MidnightPalette.Moonlight)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) GhostButton("Back", onClick = { page-- }, modifier = Modifier.weight(1f))
                if (page < last) {
                    GradientButton("Next", enabled = true, onClick = { page++ }, icon = Icons.Filled.ArrowForward,
                        modifier = Modifier.weight(1f).testTag("onboarding-next"))
                } else {
                    GradientButton("Start proving", enabled = true, onClick = vm::finishOnboarding, icon = Icons.Filled.Done,
                        modifier = Modifier.weight(1f).testTag("onboarding-finish"))
                }
            }
            if (page == last) {
                LinkButton(Copy.ONBOARD_OTHER, onClick = vm::finishOnboarding, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Page ${current + 1} of $count" }
    ) {
        repeat(count) { i ->
            Box(
                Modifier.height(6.dp).width(if (i == current) 28.dp else 10.dp).clip(CircleShape)
                    .background(if (i <= current) MidnightPalette.ProofGlow else MidnightPalette.Hairline)
            )
        }
    }
}
