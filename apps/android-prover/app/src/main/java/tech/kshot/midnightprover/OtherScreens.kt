package tech.kshot.midnightprover

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------- Activity

@Composable
fun ActivityScreen(state: ProverUiState, vm: ProverViewModel) {
    var confirmClear by remember { mutableStateOf(false) }
    AdaptivePanes(
        primary = {
            ScreenHeader("History", "Activity", "Your last ${ProverStore.MAX_HISTORY} proof jobs, kept on this device.")
            if (state.history.isEmpty()) {
                StateMessage(Icons.Filled.List, "No proof jobs yet", Copy.ACTIVITY_EMPTY, modifier = Modifier.testTag("activity-empty"))
            } else {
                val proven = state.history.count { it.success }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MidnightChip("$proven proven", MidnightPalette.ProofGlow)
                    MidnightChip("${state.history.size - proven} not proven", MidnightPalette.Moonlight)
                }
                state.history.forEach { HistoryRow(it, state.settings.privacyCloak) }
                GhostButton("Clear history", onClick = { confirmClear = true }, icon = Icons.Filled.Delete, accent = MidnightPalette.ErrorRose)
            }
        },
        secondary = {}
    )
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes all ${state.history.size} jobs from this device. Proofs you've already copied aren't affected.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear", color = MidnightPalette.ErrorRose) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
            containerColor = MidnightPalette.NebulaHigh
        )
    }
}

@Composable
private fun HistoryRow(r: JobRecord, cloaked: Boolean) {
    val context = LocalContext.current
    val whenText = remember(r.finishedAtMillis) {
        val pattern = if (DateUtils.isToday(r.finishedAtMillis)) (if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a")
        else (if (DateFormat.is24HourFormat(context)) "MMM d, HH:mm" else "MMM d, h:mm a")
        DateFormat.format(DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), pattern), r.finishedAtMillis).toString()
    }
    val color = when { r.success -> MidnightPalette.ProofGlow; r.cancelled -> MidnightPalette.Moonlight; r.interrupted -> MidnightPalette.WarningAmber; else -> MidnightPalette.ErrorRose }
    MidnightCard(modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "${r.outcome}, ${r.circuit.ifEmpty { "unknown circuit" }}, $whenText, ${JobHistoryFormat.spokenDuration(r.durationSeconds)}. ${r.detail}"
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                when { r.success -> Icons.Filled.CheckCircle; r.cancelled -> Icons.Filled.Close; r.interrupted -> Icons.Filled.Warning; else -> Icons.Filled.Warning },
                contentDescription = null, tint = color, modifier = Modifier.size(24.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(r.circuit.ifEmpty { HexCloak.label(r.txPreview, cloaked) }, style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
                Text("$whenText · ${JobHistoryFormat.formatDuration(r.durationSeconds)}", style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
            }
            MidnightChip(r.outcome, color)
        }
        Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
        Text(r.serverUrl, style = MonoStyle, color = MidnightPalette.DimStar)
    }
}

// ---------------------------------------------------------------- Server

@Composable
fun ServerScreen(state: ProverUiState, vm: ProverViewModel) {
    val snack = LocalSnackbar.current
    AdaptivePanes(
        primary = {
            ScreenHeader("Proof server", "Server", Copy.SERVER_INTRO)
            ServerStatusCard(state, onCheck = vm::checkServer)
            val s = state.health.status
            if (s != null && s.reachable && s.failure == null) {
                Text(
                    Copy.serverDetails(s.version ?: "?", Copy.formatProofVersions(s.proofVersions), s.ready, s.jobCapacity),
                    style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight
                )
            }
            MidnightCard {
                SectionLabel("Address")
                OutlinedTextField(
                    value = state.urlDraft,
                    onValueChange = vm::onUrlDraftChange,
                    label = { Text("Proof server URL") },
                    singleLine = true,
                    isError = state.urlError != null,
                    supportingText = { Text(state.urlError ?: Copy.SERVER_URL_HINT) },
                    textStyle = MonoStyle.copy(color = MidnightPalette.Starlight),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, autoCorrectEnabled = false),
                    keyboardActions = KeyboardActions(onDone = { if (vm.saveUrl()) snack("Server address saved") }),
                    enabled = !state.session.isRunning,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MidnightPalette.Violet, unfocusedBorderColor = MidnightPalette.FieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("url-field")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("Save & check", enabled = state.urlError == null && state.urlDirty && !state.session.isRunning,
                        onClick = { if (vm.saveUrl()) snack("Server address saved") }, icon = Icons.Filled.Done,
                        modifier = Modifier.weight(1f).testTag("save-url"))
                    GhostButton("Use this phone", onClick = vm::resetUrl, icon = Icons.Filled.Refresh, modifier = Modifier.weight(1f))
                }
                Text(
                    if (!state.offPhone) "Using the proof server on this phone. Your proof inputs stay on the device."
                    else Copy.OFF_PHONE_WARNING,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!state.offPhone) MidnightPalette.Moonlight else MidnightPalette.WarningAmber
                )
            }
            StateMessage(Icons.Filled.Warning, "The proof-server port is open on your Wi-Fi", Copy.OPEN_PORT_NOTE, tone = Tone.WARN)
            StateMessage(Icons.Filled.Info, "Testnet only", Copy.TESTNET_NOTE, tone = Tone.INFO)
        },
        secondary = { SetupGuideCard(onReplay = vm::reopenOnboarding) }
    )
}

@Composable
fun SetupGuideCard(onReplay: (() -> Unit)? = null) {
    MidnightCard {
        SectionLabel("Set up the server on this phone")
        SetupGuide.steps.forEachIndexed { i, step -> SetupStep(i + 1, step) }
        Text(Copy.ONBOARD_DATA_NOTE, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
        Text(Copy.ONBOARD_KEEP_RUNNING, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.WarningAmber)
        Text(Copy.OPEN_PORT_NOTE, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.WarningAmber)
        Text(SetupGuide.PHONE_STATUS, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.DimStar)
        if (onReplay != null) LinkButton("Replay the setup walkthrough", onClick = onReplay)
    }
}

@Composable
fun SetupStep(n: Int, step: SetupGuide.Step) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$n. ${step.title}", style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
        Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
        step.commands.forEach { CommandRow(it) }
    }
}

@Composable
fun CommandRow(command: String) {
    val clipboard = LocalClipboardManager.current
    val snack = LocalSnackbar.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightPalette.DeepSpace.copy(alpha = 0.7f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(command, style = MonoStyle, color = MidnightPalette.ProofGlow, modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp))
        IconButton(onClick = { clipboard.setText(AnnotatedString(command)); snack("Command copied") }) {
            Icon(CopyIcon, contentDescription = "Copy command: $command", tint = MidnightPalette.Moonlight)
        }
    }
}

// ---------------------------------------------------------------- Settings + About

@Composable
fun SettingsScreen(state: ProverUiState, vm: ProverViewModel) {
    AdaptivePanes(
        primary = {
            ScreenHeader("Preferences", "Settings")
            MidnightCard {
                SectionLabel("Proving")
                Text("Stop waiting for a proof after", style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
                Text("Phone proving time hasn't been measured. The default, 11 minutes, sits just above proof server 9.0.2's own 600 s job timeout; raise it only if you raised the server's MIDNIGHT_PROOF_SERVER_JOB_TIMEOUT.", style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
                listOf(5, 11, 30, 60).forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .selectable(selected = state.settings.proveTimeoutMinutes == m, role = Role.RadioButton) { vm.setProveTimeout(m) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = state.settings.proveTimeoutMinutes == m, onClick = null)
                        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
                        Text(if (m == ProverSettings.DEFAULT_TIMEOUT_MINUTES) "$m minutes (default)" else "$m minutes", style = MaterialTheme.typography.bodyLarge, color = MidnightPalette.Starlight)
                    }
                }
                HorizontalDivider(color = MidnightPalette.Hairline)
                SwitchRow("Keep screen on while proving", "Stops the screen from sleeping during a proof.", state.settings.keepScreenOn, vm::setKeepScreenOn)
                SwitchRow("Reduce motion", "Freezes the starfield and pulsing animations. The system's \"Remove animations\" setting also does this.", state.settings.reduceMotion, vm::setReduceMotion)
                SwitchRow("Privacy cloak", "Masks proof and payload hex on screen — for screenshots and screen sharing. Copy, share and save still use the real proof.", state.settings.privacyCloak, vm::setPrivacyCloak)
            }
            OffPhoneCard(state, vm)
        },
        secondary = { AboutCard() }
    )
}

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight)
        }
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = MidnightPalette.ButtonEnd))
    }
}

@Composable
fun AboutCard() {
    MidnightCard(modifier = Modifier.testTag("about")) {
        SectionLabel("About")
        Text("Midnight Prover ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge, color = MidnightPalette.Starlight)
        Text(Copy.HONESTY_NOTE, style = MaterialTheme.typography.bodyMedium, color = MidnightPalette.Moonlight)
        FactRow("Proof server", "9.0.2 (expected)")
        FactRow("Protocol", "POST /prove, raw binary")
        FactRow("Default address", ProofJobInput.DEFAULT_BASE_URL, mono = true)
    }
    TipCard()
}

@Composable
fun TipCard() {
    MidnightCard(modifier = Modifier.testTag("tip"), borderColor = MidnightPalette.Violet.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MidnightPalette.WarningAmber)
            SectionLabel("Tip")
        }
        Text(Copy.TIP_INTRO, style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
        AddressRow("Cardano (ADA)", Attribution.ADA_ADDRESS)
        AddressRow("Midnight (shielded)", Attribution.MIDNIGHT_ADDRESS)
        Text(Copy.FOOTER_INDEPENDENCE, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.Moonlight, modifier = Modifier.testTag("independence"))
    }
}

@Composable
private fun AddressRow(label: String, address: String) {
    val clipboard = LocalClipboardManager.current
    val snack = LocalSnackbar.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MidnightPalette.VioletText)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(address, style = MonoStyle, color = MidnightPalette.Starlight, modifier = Modifier.weight(1f))
            IconButton(onClick = { clipboard.setText(AnnotatedString(address)); snack("$label address copied") }) {
                Icon(CopyIcon, contentDescription = "Copy $label tip address", tint = MidnightPalette.Moonlight)
            }
        }
    }
}

/** P0-5: servers off this phone are opt-in, https-only, behind a clear warning. */
@Composable
private fun OffPhoneCard(state: ProverUiState, vm: ProverViewModel) {
    var confirm by remember { mutableStateOf(false) }
    MidnightCard(borderColor = if (state.settings.allowOffPhone) MidnightPalette.WarningAmber.copy(alpha = 0.6f) else MidnightPalette.Hairline) {
        SectionLabel("Privacy")
        SwitchRow(
            "Allow a server off this phone",
            "Off by default. When on, you can enter an https:// proof server on your network or in the cloud.",
            state.settings.allowOffPhone
        ) { on -> if (on) confirm = true else vm.setAllowOffPhone(false) }
        if (state.settings.allowOffPhone) Text(Copy.OFF_PHONE_WARNING, style = MaterialTheme.typography.bodySmall, color = MidnightPalette.WarningAmber)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MidnightPalette.WarningAmber) },
            title = { Text("Send private inputs off this phone?") },
            text = { Text(Copy.OFF_PHONE_WARNING) },
            confirmButton = { TextButton(onClick = { vm.setAllowOffPhone(true); confirm = false }, modifier = Modifier.testTag("confirm-off-phone")) { Text("Allow", color = MidnightPalette.WarningAmber) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep on this phone") } },
            containerColor = MidnightPalette.NebulaHigh
        )
    }
}
