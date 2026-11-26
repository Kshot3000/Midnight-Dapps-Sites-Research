package tech.kshot.midnightprover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

fun healthColor(kind: HealthKind): Color = when (kind) {
    HealthKind.ONLINE -> MidnightPalette.ProofGlow
    HealthKind.CHECKING -> MidnightPalette.VioletText
    HealthKind.NOT_READY, HealthKind.WRONG_VERSION -> MidnightPalette.WarningAmber
    HealthKind.OFFLINE, HealthKind.ERROR -> MidnightPalette.ErrorRose
    HealthKind.UNKNOWN -> MidnightPalette.DimStar
}

/** One-line explanation under the headline for each health state. */
fun healthDetail(h: ServerHealth, url: String): String = when (h.kind) {
    HealthKind.UNKNOWN -> Copy.STATUS_NOT_CHECKED
    HealthKind.CHECKING -> if (h.attempt <= 1) url else "No answer yet — retrying with backoff."
    HealthKind.ONLINE -> url
    HealthKind.NOT_READY -> if (h.status?.readyStatus == "busy") Copy.STATUS_BUSY else Copy.STATUS_NOT_READY
    HealthKind.WRONG_VERSION -> Copy.statusVersionMismatch(h.status?.version ?: "?")
    HealthKind.OFFLINE -> Copy.statusUnreachable(url)
    HealthKind.ERROR -> h.status?.failure?.message ?: ""
}

/** Server status hero. [compact] = the summary pill used on the Prove tab. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServerStatusCard(state: ProverUiState, onCheck: () -> Unit, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val h = state.health
    val color = healthColor(h.kind)
    MidnightCard(
        modifier = modifier
            .testTag("server-card")
            .then(if (onOpen != null) Modifier.clickable(onClickLabel = "Open server details", onClick = onOpen) else Modifier),
        borderColor = color.copy(alpha = 0.45f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Server status: ${h.headline}. ${healthDetail(h, state.settings.baseUrl)}"
                liveRegion = LiveRegionMode.Polite
            }
        ) {
            StatusOrb(color = color, pulsing = h.kind == HealthKind.ONLINE || h.kind == HealthKind.CHECKING)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (h.kind == HealthKind.CHECKING && h.attempt <= 1) Copy.STATUS_CHECKING else h.headline,
                    style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight
                )
                Text(healthDetail(h, state.settings.baseUrl), style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
            }
        }
        val s = h.status
        if (s != null && s.reachable && s.failure == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MidnightChip("v${s.version ?: "?"}", if (s.versionMatches) MidnightPalette.ProofGlow else MidnightPalette.WarningAmber)
                MidnightChip(Copy.chipProofs(Copy.formatProofVersions(s.proofVersions)), MidnightPalette.VioletText)
                s.jobCapacity?.let { MidnightChip(Copy.chipJobs(it), MidnightPalette.Moonlight) }
                MidnightChip("Queue ${s.jobsProcessing + s.jobsPending}", if (s.queueFull) MidnightPalette.WarningAmber else MidnightPalette.Moonlight)
                s.latencyMs?.let { MidnightChip("$it ms", MidnightPalette.Moonlight) }
                if (state.offPhone) MidnightChip("REMOTE", MidnightPalette.WarningAmber)
            }
        }
        GhostButton(
            text = if (h.kind == HealthKind.UNKNOWN) "Check server" else "Check again",
            onClick = onCheck,
            enabled = h.kind != HealthKind.CHECKING,
            icon = Icons.Filled.Refresh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("check-server")
        )
    }
}
