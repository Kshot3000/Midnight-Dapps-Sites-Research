package tech.kshot.midnightprover

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Midnight Prover — v0.1 functional shell.
 *
 * Talks to the official Midnight proof server 9.0.2 (Route 3 of the
 * CLAUDE-AP-1 feasibility verdict: the arm64 server runs in a Linux
 * environment on this phone, reached at localhost:6300; the engine behind
 * that interface is swappable).
 *
 * UI polish is a separate pass (SOL-AP-1). This build is deliberately
 * plain: status, job input, prove, result. It makes NO on-device
 * performance claims — proving on phone hardware has not been benchmarked
 * yet; the first device measurements will come from a supervised run on
 * the owner's phone.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProverScreen()
                }
            }
        }
    }
}

@Composable
fun ProverScreen(vm: ProverViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Midnight Prover", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Prove Midnight transactions against a local proof server " +
                "(official midnight-proof-server ${ProofJobInput.EXPECTED_SERVER_VERSION}).",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = vm::onBaseUrlChange,
            label = { Text("Proof server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = vm::checkServer,
            enabled = state.jobState != JobState.CHECKING && state.jobState != JobState.PROVING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check server")
        }
        state.server?.let { server ->
            if (server.reachable) {
                Text(
                    "Server ${server.version ?: "?"} · proofs ${server.proofVersions ?: "?"} · " +
                        "ready=${server.ready} · job capacity ${server.jobCapacity ?: "?"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (server.version != null && server.version != ProofJobInput.EXPECTED_SERVER_VERSION) {
                    Text(
                        "Note: expected ${ProofJobInput.EXPECTED_SERVER_VERSION}; " +
                            "the pins this team builds against may differ from this server.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.txHex,
            onValueChange = vm::onTxHexChange,
            label = { Text("Transaction body (hex)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = vm::prove,
            enabled = state.jobState != JobState.PROVING && state.jobState != JobState.CHECKING,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Prove transaction")
        }

        when (state.jobState) {
            JobState.CHECKING, JobState.PROVING -> {
                CircularProgressIndicator()
                Text(
                    if (state.jobState == JobState.CHECKING) "Contacting server…"
                    else "Proving… this can take a while on mobile hardware.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            JobState.DONE -> Text("Proven. Server response:", style = MaterialTheme.typography.titleMedium)
            JobState.FAILED -> Text("Job failed.", style = MaterialTheme.typography.titleMedium)
            JobState.IDLE -> {}
        }
        if (state.result.isNotEmpty()) {
            Text(state.result, style = MaterialTheme.typography.bodySmall)
        }
        if (state.error.isNotEmpty()) {
            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Text(
            "Honest status: on-device proving has not been benchmarked yet. " +
                "This app is the client shell for the Route 3 design (CLAUDE-AP-1); " +
                "the proof server itself is Midnight's official arm64 build, unmodified.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Independent builder — not affiliated with the Midnight Foundation or IOG. · @kshot9000",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
