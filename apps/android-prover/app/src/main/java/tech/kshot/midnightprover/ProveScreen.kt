package tech.kshot.midnightprover

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Lays out [primary] and [secondary] side by side on expanded widths (unfolded Fold), stacked otherwise. */
@Composable
fun AdaptivePanes(primary: @Composable () -> Unit, secondary: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    if (LocalWidthClass.current == WidthClass.EXPANDED) {
        Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(
                Modifier.weight(1.15f).verticalScroll(scroll).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { primary() }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { secondary() }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(scroll).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) { primary(); secondary() }
        }
    }
}

/** The four steps of a proof job; the current one is highlighted. */
@Composable
fun FlowSteps(current: Int) {
    val steps = listOf("Receive", "Inspect", "Prove", "Hand back")
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = "Step ${current + 1} of 4: ${steps[current]}"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { i, label ->
            val active = i == current
            val done = i < current
            val color = when { active -> MidnightPalette.ProofGlow; done -> MidnightPalette.VioletText; else -> MidnightPalette.DimStar }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                        .background(if (active || done) color else MidnightPalette.Hairline)
                )
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

fun flowStep(state: ProverUiState): Int = when (state.session.state) {
    JobState.PROVING -> 2
    JobState.DONE -> 3
    else -> if (state.draftInfo != null) 1 else 0
}

@Composable
fun ProveScreen(state: ProverUiState, vm: ProverViewModel, onOpenServer: () -> Unit) {
    val context = LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.prove() }
    val startProve: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.prove()
    }
    val s = state.session
    // Auto-check on resume, then a quiet refresh every 15 s while idle on this tab (Bob P1-11).
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        vm.refreshQuietly()
        onPauseOrDispose { }
    }
    androidx.compose.runtime.LaunchedEffect(s.isRunning) {
        while (!s.isRunning) { kotlinx.coroutines.delay(15_000); vm.refreshQuietly() }
    }
    AdaptivePanes(
        primary = {
            ScreenHeader("Midnight", "Prover",
                if (state.offPhone) Copy.headerSubtitleRemote(java.net.URI(state.settings.baseUrl).host ?: state.settings.baseUrl) else Copy.HEADER_SUBTITLE)
            FlowSteps(flowStep(state))
            when (s.state) {
                JobState.PROVING -> ProvingCard(s, vm::cancelProve)
                JobState.DONE -> ResultCard(s, cloaked = state.settings.privacyCloak, onNew = vm::dismissResult)
                JobState.FAILED, JobState.CANCELLED, JobState.INTERRUPTED ->
                    FailureCard(s, state.settings.proveTimeoutMinutes, onRetry = startProve, onDismiss = vm::dismissResult, onOpenServer = onOpenServer)
                JobState.IDLE -> {}
            }
            if (s.state != JobState.PROVING && s.state != JobState.DONE) JobEditor(state, vm, startProve)
        },
        secondary = {
            ServerStatusCard(state, onCheck = vm::checkServer, onOpen = onOpenServer)
            val info = if (s.state == JobState.PROVING || s.state == JobState.DONE) s.info else state.draftInfo
            if (info != null) PayloadCard(info)
            else if (LocalWidthClass.current == WidthClass.EXPANDED) {
                StateMessage(Icons.Filled.Info, "No payload yet", Copy.INPUT_EMPTY)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobEditor(state: ProverUiState, vm: ProverViewModel, onProve: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.use { vm.importPayload(it.readNBytesCompat(MainActivity.MAX_IMPORT_BYTES), "a file") }
        }
    }
    MidnightCard {
        SectionLabel("New proof job")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Paste", onClick = {
                clipboard.getText()?.text?.let { vm.importPayload(it.toByteArray(), "the clipboard") }
            }, icon = Icons.Filled.Create, modifier = Modifier.testTag("paste"))
            GhostButton("Import file", onClick = { picker.launch(arrayOf("*/*")) }, icon = Icons.Filled.Share)
            GhostButton("Try sample", onClick = vm::loadSample, icon = Icons.Filled.PlayArrow, modifier = Modifier.testTag("sample"))
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = vm::onDraftChange,
            label = { Text("Proving payload (hex)") },
            placeholder = { Text("Paste the serialized proving payload a wallet or SDK would send the proof server…") },
            textStyle = MonoStyle.copy(color = MidnightPalette.Starlight),
            minLines = 3,
            maxLines = 6,
            isError = state.draftError != null,
            supportingText = {
                Text(
                    state.draftError ?: state.importMessage.ifEmpty {
                        if (state.draft.isBlank()) "Share a payload to this app, import a file, or paste hex." else ""
                    }
                )
            },
            trailingIcon = if (state.draft.isNotEmpty()) {
                { IconButton(onClick = vm::clearDraft) { Icon(Icons.Filled.Clear, contentDescription = "Clear payload") } }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightPalette.Violet,
                unfocusedBorderColor = MidnightPalette.FieldBorder,
                focusedContainerColor = MidnightPalette.DeepSpace.copy(alpha = 0.6f),
                unfocusedContainerColor = MidnightPalette.DeepSpace.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth().testTag("payload-field")
        )
        val serverQueueFull = state.health.status?.queueFull == true
        if (serverQueueFull) Text(Copy.QUEUE_FULL_PRECHECK, color = MidnightPalette.WarningAmber, style = MaterialTheme.typography.bodySmall)
        if (state.health.kind == HealthKind.OFFLINE && state.draftInfo != null) {
            Text(Copy.statusUnreachable(state.settings.baseUrl), color = MidnightPalette.WarningAmber, style = MaterialTheme.typography.bodySmall)
        }
        GradientButton(
            text = Copy.PROVE_BUTTON,
            enabled = state.draftInfo != null,
            onClick = onProve,
            icon = Icons.Filled.PlayArrow,
            modifier = Modifier.fillMaxWidth().testTag("prove")
        )
    }
}

/** What the app can read from the payload: circuit, size, header, prover key; k honestly marked unknown. */
@Composable
fun PayloadCard(info: PayloadInfo) {
    MidnightCard(modifier = Modifier.testTag("payload-card")) {
        SectionLabel("Circuit")
        Text(
            info.circuitName ?: "Not found in payload",
            style = MaterialTheme.typography.headlineSmall,
            color = if (info.circuitName != null) MidnightPalette.Starlight else MidnightPalette.WarningAmber
        )
        Text(info.kindLabel, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
        FactRow("Key location", info.keyLocation ?: "—", mono = true)
        FactRow("Size", JobHistoryFormat.formatBytes(info.sizeBytes))
        FactRow(
            "Format",
            if (info.headerOk) "Proving payload ✓" else "Unrecognised header",
            valueColor = if (info.headerOk) MidnightPalette.ProofGlow else MidnightPalette.WarningAmber
        )
        FactRow("Prover key", if (info.carriesProverKey) "Included in payload" else "Server's own key")
        FactRow("Circuit size (k)", "Not in payload — set by the circuit's key", valueColor = MidnightPalette.Moonlight)
    }
}

@Composable
private fun ProvingCard(s: ProveSession, onCancel: () -> Unit) {
    val now = rememberTicker(running = true)
    val secs = ((now - s.startedAtMillis).coerceAtLeast(0)) / 1000.0
    MidnightCard(borderColor = MidnightPalette.Violet.copy(alpha = 0.6f), modifier = Modifier.testTag("proving-card")) {
        Column(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = "Proving. ${JobHistoryFormat.spokenDuration(secs)} so far."
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProvingRing(JobHistoryFormat.formatDuration(secs))
            Text(s.info?.circuitName ?: "Proving payload", style = MaterialTheme.typography.titleLarge, color = MidnightPalette.Starlight)
            Text(
                Copy.progress(JobHistoryFormat.formatDuration(secs)),
                style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight
            )
        }
        GhostButton(Copy.CANCEL, onClick = onCancel, icon = Icons.Filled.Close, accent = MidnightPalette.ErrorRose,
            modifier = Modifier.fillMaxWidth().testTag("cancel"))
    }
}

@Composable
private fun ResultCard(s: ProveSession, cloaked: Boolean, onNew: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val snack = LocalSnackbar.current
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(HexCodec.hexToBytes(s.proofHex)) }
            snack("Proof saved")
        }.onFailure { snack("Couldn't save the file") }
    }
    val headerOk = ProofFormat.isProofHex(s.proofHex)
    MidnightCard(borderColor = MidnightPalette.ProofGlow.copy(alpha = 0.6f), modifier = Modifier.testTag("result-card")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MidnightPalette.ProofGlow, modifier = Modifier.size(36.dp))
            Column {
                Text(Copy.doneTitle(JobHistoryFormat.formatDuration(s.elapsedSeconds)), style = MaterialTheme.typography.titleLarge, color = MidnightPalette.Starlight)
                Text(Copy.DONE_CAPTION, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
            }
        }
        FactRow("Circuit", s.info?.circuitName ?: "—")
        FactRow("Proof size", JobHistoryFormat.formatBytes(s.proofBytes))
        FactRow(
            "Format check",
            if (headerOk) "Midnight proof header ✓" else "Unexpected header",
            valueColor = if (headerOk) MidnightPalette.ProofGlow else MidnightPalette.WarningAmber
        )
        FactRow("Verification", "Not verified on this device — the network checks the proof when the transaction is submitted.", valueColor = MidnightPalette.Moonlight)
        FactRow("Server", s.serverUrl, mono = true)
        Text(
            HexCloak.preview(s.proofHex, cloaked),
            style = MonoStyle, color = MidnightPalette.VioletText,
            modifier = Modifier.semantics { contentDescription = "Proof hex preview" }
        )
        Text(Copy.DONE_NOTE, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
        GradientButton(Copy.COPY_PROOF, enabled = true, icon = CopyIcon, onClick = {
            clipboard.setText(AnnotatedString(s.proofHex)); snack(Copy.PROOF_COPIED)
        }, modifier = Modifier.fillMaxWidth().testTag("copy-proof"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Share", icon = Icons.Filled.Share, modifier = Modifier.weight(1f), onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, s.proofHex)
                context.startActivity(Intent.createChooser(send, "Hand the proof back"))
            })
            GhostButton("Save file", modifier = Modifier.weight(1f), onClick = {
                saver.launch("midnight-proof-${s.info?.circuitName?.replace('/', '-') ?: "job"}.bin")
            })
        }
        LinkButton("Start a new job", onClick = onNew)
    }
}

@Composable
private fun FailureCard(s: ProveSession, timeoutMinutes: Int, onRetry: () -> Unit, onDismiss: () -> Unit, onOpenServer: () -> Unit) {
    val f = s.failure ?: FailureInfo("Unexpected", Copy.ERR_CARD_TITLE, "")
    val tone = when (s.state) { JobState.CANCELLED -> Tone.NEUTRAL; JobState.INTERRUPTED -> Tone.WARN; else -> Tone.ERROR }
    val title = when (s.state) {
        JobState.CANCELLED -> "Proof cancelled"
        JobState.INTERRUPTED -> "Proof interrupted"
        else -> if (f.kind == "Invalid") f.title else "${Copy.ERR_CARD_TITLE}: ${f.title.replaceFirstChar { it.lowercase() }}"
    }
    StateMessage(
        icon = if (tone == Tone.NEUTRAL) Icons.Filled.Info else Icons.Filled.Warning,
        title = title,
        message = Copy.failureMessage(f, timeoutMinutes),
        tone = tone,
        modifier = Modifier.testTag("failure-card").semantics { stateDescription = f.kind }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.payloadHex.isNotEmpty() && f.kind != "Invalid") {
                GradientButton("Retry", enabled = true, onClick = onRetry, icon = Icons.Filled.Refresh, modifier = Modifier.weight(1f).testTag("retry"))
            }
            if (f.kind == "Unreachable" || f.kind == "Busy" || f.kind == "QueueFull" || f.kind == "Timeout") {
                GhostButton("Server", onClick = onOpenServer, modifier = Modifier.weight(1f))
            }
        }
        LinkButton("Dismiss", onClick = onDismiss)
    }
}
