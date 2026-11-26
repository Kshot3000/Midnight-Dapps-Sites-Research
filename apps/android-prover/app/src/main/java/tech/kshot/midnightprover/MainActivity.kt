package tech.kshot.midnightprover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Midnight Prover — an Android client for Midnight's official proof server
 * 9.0.2 (Route 3, CLAUDE-AP-1 verdict: the arm64 server runs in a Linux
 * environment on this phone, reached at 127.0.0.1:6300).
 *
 * It makes NO on-device performance claims: proving on phone hardware has
 * not been measured yet; first measurements come from a supervised run on
 * the developer's own phone. Testnet only.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MidnightProverTheme {
                ProverApp()
            }
        }
    }
}

enum class Tab(val label: String, val glyph: String) {
    PROVE("Prove", "◆"),
    ACTIVITY("Activity", "☰"),
    SERVER("Server", "◉")
}

private val fieldColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MidnightPalette.Violet,
    unfocusedBorderColor = MidnightPalette.FieldBorder,
    disabledBorderColor = MidnightPalette.Hairline,
    focusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f),
    unfocusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f),
    disabledContainerColor = MidnightPalette.Nebula.copy(alpha = 0.6f),
    disabledTextColor = MidnightPalette.Moonlight
)

@Composable
fun ProverApp(vm: ProverViewModel = viewModel(), initialTab: Tab = Tab.PROVE) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground()
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = MidnightPalette.Nebula.copy(alpha = 0.92f)) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Text(t.glyph) },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("tab-${t.name}"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MidnightPalette.ProofGlow,
                                selectedTextColor = MidnightPalette.ProofGlow,
                                indicatorColor = MidnightPalette.NebulaHigh,
                                unselectedIconColor = MidnightPalette.Moonlight,
                                unselectedTextColor = MidnightPalette.Moonlight
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (tab) {
                    Tab.PROVE -> ProveScreen(state, vm, snackbar)
                    Tab.ACTIVITY -> ActivityScreen(state, vm)
                    Tab.SERVER -> ServerScreen(state, vm, snackbar)
                }
            }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) { content() }
}

@Composable
private fun TestnetChip() {
    MidnightChip(Copy.TESTNET_CHIP, MidnightPalette.WarningAmber, Modifier.testTag("testnet-chip"))
}

@Composable
private fun Header(state: ProverUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("MIDNIGHT", style = MaterialTheme.typography.labelLarge, color = MidnightPalette.ProofGlow)
        Text(
            "Prover",
            style = MaterialTheme.typography.headlineLarge,
            color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            if (state.offPhone) Copy.headerSubtitleRemote(hostOf(state.baseUrl)) else Copy.HEADER_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MidnightPalette.Moonlight
        )
        TestnetChip()
    }
}

private fun hostOf(url: String): String = runCatching { java.net.URI(url).host }.getOrNull() ?: url

private fun healthColor(kind: HealthKind): Color = when (kind) {
    HealthKind.ONLINE -> MidnightPalette.ProofGlow
    HealthKind.BUSY, HealthKind.NOT_READY, HealthKind.WRONG_VERSION -> MidnightPalette.WarningAmber
    HealthKind.OFFLINE, HealthKind.ERROR -> MidnightPalette.ErrorRose
    HealthKind.UNKNOWN, HealthKind.CHECKING -> MidnightPalette.DimStar
}

private fun healthDetail(state: ProverUiState): String? {
    val s = state.health.status
    return when (state.health.kind) {
        HealthKind.UNKNOWN -> Copy.STATUS_NOT_CHECKED
        HealthKind.BUSY -> Copy.STATUS_BUSY + (s?.let { " (${it.jobsProcessing} running, ${it.jobsPending} waiting)" } ?: "")
        HealthKind.NOT_READY -> Copy.STATUS_NOT_READY
        HealthKind.WRONG_VERSION -> Copy.statusVersionMismatch(s?.version ?: "?")
        HealthKind.OFFLINE -> Copy.statusUnreachable(state.baseUrl) + (s?.failure?.message?.let { " $it" } ?: "")
        HealthKind.ERROR -> s?.failure?.message
        else -> null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerStatusCard(state: ProverUiState, vm: ProverViewModel) {
    val kind = state.health.kind
    val color = healthColor(kind)
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidnightPalette.Hairline, RoundedCornerShape(20.dp))
            .testTag("server-card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusOrb(color)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    state.health.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightPalette.Starlight,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("server-headline")
                )
                Text(state.baseUrl, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
                val s = state.health.status
                if (s != null && s.reachable && s.failure == null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MidnightChip("v${s.version ?: "?"}")
                        MidnightChip(Copy.chipProofs(Copy.formatProofVersions(s.proofVersions)), MidnightPalette.VioletText)
                        s.jobCapacity?.let { MidnightChip(Copy.chipJobs(it), MidnightPalette.Moonlight) }
                        if (state.offPhone) MidnightChip("REMOTE", MidnightPalette.WarningAmber)
                    }
                }
                healthDetail(state)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (kind == HealthKind.UNKNOWN) MidnightPalette.Moonlight else color)
                }
            }
        }
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            GradientButton(
                text = Copy.CHECK_SERVER,
                enabled = kind != HealthKind.CHECKING && !state.isProving,
                onClick = vm::checkServer,
                modifier = Modifier.fillMaxWidth().testTag("check-server")
            )
        }
    }
}

@Composable
private fun ProveScreen(state: ProverUiState, vm: ProverViewModel, snackbar: SnackbarHostState) {
    ScreenColumn {
        Header(state)
        ServerStatusCard(state, vm)
        if (state.offPhone) WarningCard(Copy.OFF_PHONE_WARNING)

        Text(
            "New proof job",
            style = MaterialTheme.typography.titleMedium,
            color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() }
        )
        OutlinedTextField(
            value = if (state.isProving) state.jobPayloadHex else state.txHex,
            onValueChange = vm::onTxHexChange,
            enabled = !state.isProving,
            label = { Text("Proving payload (hex)") },
            placeholder = { Text("Paste the proving payload a wallet or SDK would send the proof server…") },
            isError = state.inputError.isNotEmpty(),
            supportingText = if (state.inputError.isNotEmpty()) ({ Text(state.inputError) }) else null,
            minLines = 4,
            maxLines = 8,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth().testTag("payload-field")
        )
        GradientButton(
            text = Copy.PROVE_BUTTON,
            enabled = !state.isProving,
            onClick = vm::prove,
            modifier = Modifier.fillMaxWidth().testTag("prove")
        )

        if (state.warning.isNotEmpty()) {
            Text(state.warning, color = MidnightPalette.WarningAmber, style = MaterialTheme.typography.bodyMedium)
        }
        when (state.jobState) {
            JobState.PROVING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("proving")
                ) {
                    CircularProgressIndicator(color = MidnightPalette.ProofGlow)
                    Text(
                        Copy.progress(JobHistoryFormat.formatDuration(state.elapsedSeconds)),
                        color = MidnightPalette.Moonlight,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                GradientButton(
                    text = Copy.CANCEL,
                    enabled = true,
                    onClick = vm::cancelProve,
                    modifier = Modifier.fillMaxWidth().testTag("cancel")
                )
            }
            JobState.DONE -> ProofResultCard(state, vm, snackbar)
            JobState.FAILED, JobState.CANCELLED -> state.failure?.let { f ->
                ResultCard(
                    title = if (state.jobState == JobState.CANCELLED) f.title else f.title,
                    body = Copy.failureMessage(f, PROVE_TIMEOUT_MINUTES) +
                        (if (f.kind == "Unreachable" || f.kind == "Timeout") "\n\n${f.message}" else ""),
                    accent = if (state.jobState == JobState.CANCELLED) MidnightPalette.WarningAmber else MidnightPalette.ErrorRose,
                    onDismiss = vm::dismissResult
                )
            }
            else -> {}
        }
        HonestyNote()
    }
}

@Composable
private fun WarningCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MidnightPalette.WarningAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .background(MidnightPalette.Nebula.copy(alpha = 0.9f))
            .padding(12.dp)
    ) {
        Text(text, color = MidnightPalette.WarningAmber, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ResultCard(title: String, body: String, accent: Color, onDismiss: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("failure-card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = accent, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("failure-title"))
            if (body.isNotEmpty()) Text(body, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
            if (onDismiss != null) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Dismiss", color = MidnightPalette.VioletText)
                }
            }
        }
    }
}

@Composable
private fun ProofResultCard(state: ProverUiState, vm: ProverViewModel, snackbar: SnackbarHostState) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidnightPalette.ProofGlow.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("result-card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                Copy.doneTitle(JobHistoryFormat.formatDuration(state.elapsedSeconds)),
                color = MidnightPalette.ProofGlow,
                style = MaterialTheme.typography.titleMedium
            )
            Text(Copy.DONE_CAPTION, color = MidnightPalette.DimStar, style = MaterialTheme.typography.bodySmall)
            Text(state.proofSummary, color = MidnightPalette.Starlight, style = MaterialTheme.typography.bodyMedium)
            Text(Copy.DONE_NOTE, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MidnightPalette.Abyss.copy(alpha = 0.6f))
                    .padding(10.dp)
            ) {
                Text(
                    "0x" + state.proofHex.take(240) + if (state.proofHex.length > 240) "…" else "",
                    color = MidnightPalette.DimStar,
                    style = MonoStyle
                )
            }
            GradientButton(
                text = Copy.COPY_PROOF,
                enabled = true,
                onClick = {
                    clipboard.setText(AnnotatedString(state.proofHex))
                    scope.launch { snackbar.showSnackbar("${Copy.PROOF_COPIED} (${state.proofHex.length / 2} bytes)") }
                },
                modifier = Modifier.fillMaxWidth().testTag("copy-proof")
            )
            TextButton(onClick = vm::dismissResult, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Start a new job", color = MidnightPalette.VioletText)
            }
        }
    }
}

@Composable
private fun HonestyNote() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightPalette.NebulaHigh.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Copy.HONESTY_NOTE, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
            Text(Copy.TESTNET_NOTE, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActivityScreen(state: ProverUiState, vm: ProverViewModel) {
    ScreenColumn {
        Text("Activity", style = MaterialTheme.typography.headlineMedium, color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() })
        if (state.history.isEmpty()) {
            Text(Copy.ACTIVITY_EMPTY, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("activity-empty"))
        } else {
            Text(Copy.ACTIVITY_TITLE_NOTE, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
        }
        val fmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
        state.history.forEach { job ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MidnightChip(
                            job.outcome,
                            when { job.success -> MidnightPalette.ProofGlow; job.cancelled -> MidnightPalette.WarningAmber; else -> MidnightPalette.ErrorRose }
                        )
                        Text(
                            "${fmt.format(Date(job.finishedAtMillis))} · ${JobHistoryFormat.formatDuration(job.durationSeconds)}",
                            color = MidnightPalette.Moonlight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (job.circuit.isNotEmpty()) Text(job.circuit, color = MidnightPalette.Starlight, style = MaterialTheme.typography.bodyMedium)
                    Text(job.txPreview, color = MidnightPalette.Moonlight, style = MonoStyle)
                    Text(job.detail, color = MidnightPalette.DimStar, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (state.history.isNotEmpty()) {
            TextButton(onClick = vm::clearHistory, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Clear history", color = MidnightPalette.VioletText)
            }
        }
    }
}

@Composable
private fun ServerScreen(state: ProverUiState, vm: ProverViewModel, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    ScreenColumn {
        Text("Server", style = MaterialTheme.typography.headlineMedium, color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() })
        Text(Copy.SERVER_INTRO, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodyMedium)
        ServerStatusCard(state, vm)
        OutlinedTextField(
            value = state.urlDraft,
            onValueChange = vm::onUrlDraftChange,
            enabled = !state.isProving,
            label = { Text("Proof server URL") },
            singleLine = true,
            isError = state.urlError != null,
            supportingText = { Text(state.urlError ?: Copy.SERVER_URL_HINT) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.saveUrl() }),
            colors = fieldColors,
            textStyle = MonoStyle,
            modifier = Modifier.fillMaxWidth().testTag("url-field")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton(
                text = "Save & check",
                enabled = !state.isProving && state.urlError == null && state.urlDirty,
                onClick = { if (vm.saveUrl()) scope.launch { snackbar.showSnackbar("Server address saved") } },
                modifier = Modifier.weight(1f).testTag("save-url")
            )
            GradientButton(
                text = "Use this phone",
                enabled = !state.isProving && state.baseUrl != ProofJobInput.DEFAULT_BASE_URL,
                onClick = vm::useThisPhone,
                modifier = Modifier.weight(1f).testTag("use-this-phone")
            )
        }
        OffPhoneCard(state, vm)
        WarningCard(Copy.OPEN_PORT_NOTE)
        Text("Run the server on this phone", style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight,
            modifier = Modifier.semantics { heading() })
        Text(
            "In Termux with a proot Debian environment, run the project's setup script " +
                "(apps/android-prover/server-setup in the repo). It downloads the official 9.0.2 " +
                "image without Docker and starts the server on 127.0.0.1:6300. The first start downloads " +
                "Midnight's public proving files (use Wi-Fi). Android may stop the server in the " +
                "background: exempt Termux from battery optimisation and, on Android 14 or later, turn on " +
                "Developer options → Disable child process restrictions. No phone install has been measured yet.",
            color = MidnightPalette.Moonlight,
            style = MaterialTheme.typography.bodyMedium
        )
        TipCard(snackbar)
    }
}

@Composable
private fun OffPhoneCard(state: ProverUiState, vm: ProverViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MidnightPalette.Hairline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow a server off this phone", color = MidnightPalette.Starlight, style = MaterialTheme.typography.titleSmall)
                    Text("Off by default. When on, you can enter an https:// server you run yourself.",
                        color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = state.allowOffPhone,
                    onCheckedChange = vm::setAllowOffPhone,
                    enabled = !state.isProving,
                    modifier = Modifier.testTag("allow-off-phone")
                )
            }
            if (state.allowOffPhone) Text(Copy.OFF_PHONE_WARNING, color = MidnightPalette.WarningAmber, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TipCard(snackbar: SnackbarHostState) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, MidnightPalette.Violet.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).testTag("tip")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tip", color = MidnightPalette.ProofGlow, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text(Copy.TIP_INTRO, color = MidnightPalette.Starlight, style = MaterialTheme.typography.bodyMedium)
            listOf("Cardano (ADA)" to Attribution.ADA_ADDRESS, "Midnight (shielded)" to Attribution.MIDNIGHT_ADDRESS).forEach { (label, address) ->
                Text(label, color = MidnightPalette.VioletText, style = MaterialTheme.typography.labelLarge)
                Text(address, color = MidnightPalette.Moonlight, style = MonoStyle)
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(address))
                        scope.launch { snackbar.showSnackbar("$label address copied") }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Copy $label address", color = MidnightPalette.VioletText) }
            }
            Text(Copy.FOOTER_INDEPENDENCE, color = MidnightPalette.DimStar, style = MaterialTheme.typography.bodySmall)
        }
    }
}
