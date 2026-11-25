package tech.kshot.midnightprover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Midnight Prover — the on-device client for Midnight's official proof
 * server 9.0.2 (Route 3, CLAUDE-AP-1 verdict: the arm64 server runs in a
 * Linux environment on this phone, reached at localhost:6300; the engine
 * behind that interface is swappable).
 *
 * Design: Midnight theme — deep-space navy, indigo-to-violet brand
 * gradient, cyan proof glow, starfield with a crescent moon. It makes NO
 * on-device performance claims: proving on phone hardware has not been
 * benchmarked yet; first measurements come from a supervised run on the
 * owner's phone.
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

private enum class Tab(val label: String, val glyph: String) {
    PROVE("Prove", "◆"),
    ACTIVITY("Activity", "☰"),
    SERVER("Server", "◉")
}

@Composable
fun ProverApp(vm: ProverViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.PROVE) }
    val state by vm.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground()
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = MidnightPalette.Nebula.copy(alpha = 0.92f)) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Text(t.glyph) },
                            label = { Text(t.label) },
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
                    Tab.PROVE -> ProveScreen(state, vm)
                    Tab.ACTIVITY -> ActivityScreen(state)
                    Tab.SERVER -> ServerScreen(state, vm)
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "MIDNIGHT",
            style = MaterialTheme.typography.labelLarge,
            color = MidnightPalette.ProofGlow
        )
        Text(
            "Prover",
            style = MaterialTheme.typography.headlineLarge,
            color = MidnightPalette.Starlight
        )
        Text(
            "Prove Midnight transactions on this device, against the official proof server.",
            style = MaterialTheme.typography.bodyMedium,
            color = MidnightPalette.Moonlight
        )
    }
}

@Composable
private fun ServerStatusCard(state: ProverUiState, vm: ProverViewModel) {
    val server = state.server
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MidnightPalette.Hairline, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusOrb(
                live = server?.reachable == true,
                failed = server != null && !server.reachable
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        state.jobState == JobState.CHECKING -> "Contacting server…"
                        server == null -> "Proof server not checked yet"
                        server.reachable -> "Proof server online"
                        else -> "Proof server unreachable"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightPalette.Starlight
                )
                Text(
                    text = state.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MidnightPalette.Moonlight
                )
                if (server?.reachable == true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MidnightChip("v${server.version ?: "?"}")
                        MidnightChip("proofs ${server.proofVersions ?: "?"}")
                        MidnightChip("capacity ${server.jobCapacity ?: "?"}")
                    }
                    if (server.version != null && server.version != ProofJobInput.EXPECTED_SERVER_VERSION) {
                        Text(
                            "Expected ${ProofJobInput.EXPECTED_SERVER_VERSION} — this server reports a different version.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MidnightPalette.WarningAmber
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            GradientButton(
                text = "Check server",
                enabled = state.jobState != JobState.CHECKING && state.jobState != JobState.PROVING,
                onClick = vm::checkServer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProveScreen(state: ProverUiState, vm: ProverViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Header()
        ServerStatusCard(state, vm)

        Text("New proof job", style = MaterialTheme.typography.titleMedium, color = MidnightPalette.Starlight)
        OutlinedTextField(
            value = state.txHex,
            onValueChange = vm::onTxHexChange,
            label = { Text("Transaction body (hex)") },
            placeholder = { Text("Paste the unproven transaction body…") },
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightPalette.Violet,
                unfocusedBorderColor = MidnightPalette.Hairline,
                focusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f),
                unfocusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        GradientButton(
            text = "Prove transaction",
            enabled = state.jobState != JobState.PROVING && state.jobState != JobState.CHECKING,
            onClick = vm::prove,
            modifier = Modifier.fillMaxWidth()
        )

        when (state.jobState) {
            JobState.PROVING -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MidnightPalette.ProofGlow)
                    Text(
                        "Proving… ${JobHistoryFormat.formatDuration(state.elapsedSeconds)} elapsed. " +
                            "Mobile proving can take a while — keep the app open.",
                        color = MidnightPalette.Moonlight,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            JobState.DONE -> {
                ResultCard(
                    title = "Proven in ${JobHistoryFormat.formatDuration(state.elapsedSeconds)}",
                    body = state.result,
                    accent = MidnightPalette.ProofGlow
                )
            }
            JobState.FAILED -> {
                ResultCard(title = "Job failed", body = state.error, accent = MidnightPalette.ErrorRose)
            }
            else -> {}
        }
        if (state.error.isNotEmpty() && state.jobState != JobState.FAILED) {
            Text(state.error, color = MidnightPalette.ErrorRose, style = MaterialTheme.typography.bodyMedium)
        }
        HonestyNote()
    }
}

@Composable
private fun ResultCard(title: String, body: String, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = accent, style = MaterialTheme.typography.titleMedium)
            if (body.isNotEmpty()) {
                Text(body, color = MidnightPalette.Moonlight, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActivityScreen(state: ProverUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Activity", style = MaterialTheme.typography.headlineMedium, color = MidnightPalette.Starlight)
        if (state.history.isEmpty()) {
            Text(
                "No proof jobs yet this session. Jobs you run appear here with their server, duration and outcome.",
                color = MidnightPalette.Moonlight,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
        state.history.forEach { job ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MidnightPalette.Nebula.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MidnightChip(
                            if (job.success) "PROVEN" else "FAILED",
                            if (job.success) MidnightPalette.ProofGlow else MidnightPalette.ErrorRose
                        )
                        Text(
                            "${fmt.format(Date(job.finishedAtMillis))} · ${JobHistoryFormat.formatDuration(job.durationSeconds)}",
                            color = MidnightPalette.Moonlight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(job.txPreview, color = MidnightPalette.Starlight, style = MaterialTheme.typography.bodyMedium)
                    Text(job.detail, color = MidnightPalette.DimStar, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ServerScreen(state: ProverUiState, vm: ProverViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Server", style = MaterialTheme.typography.headlineMedium, color = MidnightPalette.Starlight)
        Text(
            "Route 3 (CLAUDE-AP-1 verdict): the official arm64 proof server runs in a Linux " +
                "environment on this phone and is reached locally. Point the app anywhere else " +
                "to use a LAN or cloud server instead.",
            color = MidnightPalette.Moonlight,
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = vm::onBaseUrlChange,
            label = { Text("Proof server URL") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MidnightPalette.Violet,
                unfocusedBorderColor = MidnightPalette.Hairline,
                focusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f),
                unfocusedContainerColor = MidnightPalette.Nebula.copy(alpha = 0.85f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        GradientButton(
            text = "Check server",
            enabled = state.jobState != JobState.CHECKING && state.jobState != JobState.PROVING,
            onClick = vm::checkServer,
            modifier = Modifier.fillMaxWidth()
        )
        state.server?.let { server ->
            Text(
                if (server.reachable) {
                    "Version ${server.version} · proof versions ${server.proofVersions} · " +
                        "ready=${server.ready} · job capacity ${server.jobCapacity}"
                } else {
                    "Unreachable: ${server.detail}"
                },
                color = MidnightPalette.Moonlight,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        HonestyNote()
        Text(
            "Independent builder — not affiliated with the Midnight Foundation or IOG. · @kshot9000\n" +
                "Tips: see the project README for the Cardano and Midnight donation addresses.",
            color = MidnightPalette.DimStar,
            style = MaterialTheme.typography.bodySmall
        )
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
        Text(
            "Honest status: on-device proving has not been benchmarked yet, and this app claims " +
                "no phone proving times. The prover is Midnight's official arm64 proof server, " +
                "unmodified — the first device measurements come from a supervised run on the owner's phone.",
            color = MidnightPalette.Moonlight,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
