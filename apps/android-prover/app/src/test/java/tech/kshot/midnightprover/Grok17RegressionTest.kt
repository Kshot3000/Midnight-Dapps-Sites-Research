package tech.kshot.midnightprover

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * GROK-17 regression tests for the P0s in Bob's UX review and Joe's QA sweep,
 * driven through the real ProverViewModel (and Compose UI) under Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class Grok17RegressionTest {
    @get:Rule val compose = createComposeRule()
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer()

    @Before fun setUp() {
        app.getSharedPreferences("prover", Application.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun tearDown() { runCatching { server.shutdown() } }

    private fun waitFor(timeoutMs: Long = 10_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (cond()) return
            Thread.sleep(20)
        }
        throw AssertionError("condition not met in $timeoutMs ms")
    }

    /** Points the VM at the mock server: the URL is loopback, so no opt-in is needed. */
    private fun vmAt(url: String): ProverViewModel {
        val vm = ProverViewModel(app)
        vm.onUrlDraftChange(url)
        assertTrue(vm.saveUrl())
        return vm
    }

    // ---- P0-1: /ready 503 busy is busy, not unreachable ----
    @Test fun p0_1_busyServerShowsBusy() {
        server.enqueue(MockResponse().setBody("9.0.2"))
        server.enqueue(MockResponse().setBody("[\"V2\", \"V3\"]"))
        server.enqueue(MockResponse().setResponseCode(503).setBody("{\"status\":\"busy\",\"jobsProcessing\":1,\"jobsPending\":1,\"jobCapacity\":1}"))
        server.start()
        val vm = vmAt("http://127.0.0.1:${server.port}") // saveUrl() runs the check
        waitFor { vm.state.value.health.kind != HealthKind.CHECKING && vm.state.value.health.status != null }
        assertEquals(HealthKind.BUSY, vm.state.value.health.kind)
        assertEquals("Proof server busy", vm.state.value.health.headline)
    }

    // ---- P0-2: Cancel during the pre-flight check never leaves the app stuck ----
    @Test fun p0_2_cancelDuringPreflightReturnsAtOnce() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)) // /version never answers
        server.start()
        val vm = ProverViewModel(app)
        vm.onUrlDraftChange("http://127.0.0.1:${server.port}")
        vm.saveUrl()
        vm.onTxHexChange(Fixtures.dustSpendHex)
        // The save's own health check holds the first response; enqueue another hang for the pre-flight.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        vm.prove()
        assertEquals(JobState.PROVING, vm.state.value.jobState)
        val t0 = System.currentTimeMillis()
        vm.cancelProve()
        assertEquals(JobState.CANCELLED, vm.state.value.jobState)
        assertTrue(System.currentTimeMillis() - t0 < 1_000)
        assertEquals("Cancelled", vm.state.value.failure?.kind)
        assertEquals(Copy.ERR_CANCELLED, vm.state.value.failure?.message)
        // The abandoned job must not overwrite the state later.
        Thread.sleep(300); shadowOf(Looper.getMainLooper()).idle()
        assertEquals(JobState.CANCELLED, vm.state.value.jobState)
        assertEquals(Copy.OUTCOME_CANCELLED, vm.state.value.history.first().outcome)
        // And a new job can start straight away.
        vm.prove()
        assertEquals(JobState.PROVING, vm.state.value.jobState)
        vm.cancelProve()
    }

    // ---- P0-3: the URL field keeps exactly what is typed ----
    @Test fun p0_3_urlDraftIsNotReformattedOrSaved() {
        val vm = ProverViewModel(app)
        for (partial in listOf("", "h", "ht", "http:", "127.0.0.1:63", "127.0.0.1:6300/")) {
            vm.onUrlDraftChange(partial)
            assertEquals(partial, vm.state.value.urlDraft)
            assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm.state.value.baseUrl)
        }
        assertEquals(ProofJobInput.DEFAULT_BASE_URL,
            app.getSharedPreferences("prover", Application.MODE_PRIVATE).getString("baseUrl", ProofJobInput.DEFAULT_BASE_URL))
        vm.onUrlDraftChange("http://localhost:6300/")
        assertTrue(vm.saveUrl())
        assertEquals("http://localhost:6300", vm.state.value.baseUrl)
    }

    // ---- P0-5: off-phone servers need the opt-in AND https ----
    @Test fun p0_5_offPhoneNeedsOptInAndHttps() {
        val vm = ProverViewModel(app)
        vm.onUrlDraftChange("http://192.168.1.20:6300")
        assertEquals(Copy.URL_OFF_PHONE_DISABLED, vm.state.value.urlError)
        assertFalse(vm.saveUrl())
        vm.setAllowOffPhone(true)
        assertEquals(Copy.URL_OFF_PHONE_NEEDS_HTTPS, vm.state.value.urlError)
        assertFalse(vm.saveUrl())
        vm.onUrlDraftChange("https://prover.example.org")
        assertNull(vm.state.value.urlError)
        assertTrue(vm.saveUrl())
        assertTrue(vm.state.value.offPhone)
        // Turning the opt-in off moves the app back to this phone, and that survives a restart.
        vm.setAllowOffPhone(false)
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm.state.value.baseUrl)
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, ProverViewModel(app).state.value.baseUrl)
    }

    @Test fun p0_5_savedRemoteUrlIgnoredWithoutOptIn() {
        app.getSharedPreferences("prover", Application.MODE_PRIVATE).edit()
            .putString("baseUrl", "http://10.0.0.5:6300").putBoolean("allowOffPhone", false).commit()
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, ProverViewModel(app).state.value.baseUrl)
    }

    // ---- Joe P1-3 / P2-3: inputs are locked while proving ----
    @Test fun inputsLockedWhileProving() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.start()
        val vm = vmAt("http://127.0.0.1:${server.port}")
        vm.onTxHexChange("0x00ff")
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        vm.prove()
        vm.onTxHexChange("deadbeef")
        vm.onUrlDraftChange("http://localhost:1")
        vm.setAllowOffPhone(true)
        assertEquals("0x00ff", vm.state.value.txHex)
        assertEquals("00ff", vm.state.value.jobPayloadHex)
        assertEquals("http://127.0.0.1:${server.port}", vm.state.value.urlDraft)
        assertFalse(vm.state.value.allowOffPhone)
        assertFalse(vm.saveUrl())
        vm.cancelProve()
    }

    // ---- Joe P1-7: 0x prefixes and CRLF line breaks are accepted ----
    @Test fun pastedPayloadNormalised() {
        val vm = ProverViewModel(app)
        vm.onTxHexChange("0x6d69\r\n646e\t69 67\r\n")
        assertNull(ProofJobInput.validationError(vm.state.value.txHex))
        assertEquals("6d69646e6967", ProofJobInput.cleanHex(vm.state.value.txHex))
    }

    // ---- Unreachable server: its own failure class, recorded in history ----
    @Test fun unreachableIsItsOwnFailure() {
        val vm = vmAt("http://127.0.0.1:1")
        vm.onTxHexChange(Fixtures.dustSpendHex)
        vm.prove()
        waitFor { vm.state.value.jobState == JobState.FAILED }
        assertEquals("Unreachable", vm.state.value.failure?.kind)
        assertEquals("dust/spend", vm.state.value.history.first().circuit)
    }

    // ---- UI: testnet label, honest header, Tip with both addresses ----
    @Test fun uiShowsTestnetHonestHeaderAndTip() {
        val vm = ProverViewModel(app)
        compose.setContent { MidnightProverTheme { ProverApp(vm) } }
        compose.onNodeWithTag("testnet-chip").assertExists()
        compose.onNodeWithText(Copy.TESTNET_CHIP).assertExists()
        compose.onNodeWithText(Copy.HEADER_SUBTITLE).assertExists()
        compose.onNodeWithText("Prove Midnight transactions on this device", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Prove transaction").assertDoesNotExist()
        compose.onNodeWithTag("tab-SERVER").performClick()
        compose.onNodeWithTag("tip").performScrollTo()
        compose.onNodeWithText("Tip").assertExists()
        compose.onNodeWithText(Attribution.ADA_ADDRESS).assertExists()
        compose.onNodeWithText(Attribution.MIDNIGHT_ADDRESS).assertExists()
        compose.onNodeWithText(Copy.FOOTER_INDEPENDENCE).assertExists()
    }

    @Test fun uiUrlFieldKeepsTypingAndRefusesRemote() {
        val vm = ProverViewModel(app)
        compose.setContent { MidnightProverTheme { ProverApp(vm, Tab.SERVER) } }
        compose.onNodeWithTag("url-field").performTextReplacement("127.0.0.1:63")
        compose.onNodeWithText("127.0.0.1:63").assertExists()
        compose.onNodeWithTag("url-field").performTextReplacement("http://192.168.1.20:6300")
        compose.onNodeWithText(Copy.URL_OFF_PHONE_DISABLED).assertExists()
        compose.onNodeWithTag("save-url").assertIsNotEnabled()
    }
}
