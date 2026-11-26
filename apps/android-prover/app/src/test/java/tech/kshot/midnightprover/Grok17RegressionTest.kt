package tech.kshot.midnightprover

import android.os.Looper
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
 * GROK-17/18 regression tests for the P0s in Bob's UX review and Joe's QA
 * sweep, driven through the real ProverViewModel (and Compose UI) under
 * Robolectric.
 */
@RunWith(AndroidJUnit4::class)
class Grok17RegressionTest {
    @get:Rule val compose = createComposeRule()
    private val app: ProverApplication = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer()

    @Before fun setUp() {
        app.getSharedPreferences("prover", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        app.store.saveSettings(ProverSettings(onboardingDone = true))
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun tearDown() { runCatching { server.shutdown() } }

    private fun vm(): ProverViewModel = ProverViewModel(app, SavedStateHandle())

    /** The ViewModel's combined StateFlow is collected on Main; pump so tests see it. */
    private fun pump() = shadowOf(Looper.getMainLooper()).idle()

    private fun waitFor(timeoutMs: Long = 15_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(250))
            if (cond()) return
            Thread.sleep(20)
        }
        throw AssertionError("condition not met in $timeoutMs ms")
    }

    /** Points the VM at the mock server: the URL is loopback, so no opt-in is needed. */
    private fun vmAt(url: String): ProverViewModel {
        val vm = vm()
        pump()
        vm.onUrlDraftChange(url)
        pump()
        assertTrue(vm.saveUrl())
        pump()
        return vm
    }

    /** Path-based responses so init/saveUrl health checks and retries never drain a finite queue. */
    private fun route(ready: MockResponse, prove: MockResponse? = null) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/version" -> MockResponse().setBody("9.0.2")
                "/proof-versions" -> MockResponse().setBody("[\"V2\", \"V3\"]")
                "/ready" -> ready
                "/prove" -> prove ?: MockResponse().setResponseCode(404)
                else -> MockResponse().setResponseCode(404)
            }
        }
    }


    // ---- P0-1: /ready 503 busy is busy, not unreachable ----
    @Test fun p0_1_busyServerShowsBusy() {
        server.start()
        route(MockResponse().setResponseCode(503).setBody("{\"status\":\"busy\",\"jobsProcessing\":1,\"jobsPending\":1,\"jobCapacity\":1}"))
        val vm = vmAt("http://127.0.0.1:${server.port}")
        waitFor { vm.state.value.health.kind == HealthKind.BUSY }
        assertEquals("Proof server busy", vm.state.value.health.headline)
    }

    // ---- P0-2: Cancel during a hanging prove returns at once and stays cancelled ----
    @Test fun p0_2_cancelDuringHangReturnsAtOnce() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)) // hanging /prove (or check)
        server.start()
        val vm = vmAt("http://127.0.0.1:${server.port}")
        // saveUrl's own check may have consumed the hang; enqueue another for the prove.
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        vm.onDraftChange(Fixtures.dustSpendHex); pump(); vm.prove(); pump()
        assertEquals(JobState.PROVING, vm.state.value.session.state)
        val t0 = System.currentTimeMillis()
        vm.cancelProve(); pump()
        assertEquals(JobState.CANCELLED, vm.state.value.session.state)
        assertTrue(System.currentTimeMillis() - t0 < 1_000)
        assertEquals("Cancelled", vm.state.value.session.failure?.kind)
        assertEquals(Copy.ERR_CANCELLED, vm.state.value.session.failure?.message)
        Thread.sleep(300); shadowOf(Looper.getMainLooper()).idle()
        assertEquals(JobState.CANCELLED, vm.state.value.session.state)
        assertEquals(Copy.OUTCOME_CANCELLED, vm.state.value.history.first().outcome)
        vm.prove()
        assertEquals(JobState.PROVING, vm.state.value.session.state)
        vm.cancelProve()
    }

    // ---- P0-3: the URL field keeps exactly what is typed ----
    @Test fun p0_3_urlDraftIsNotReformattedOrSaved() {
        val vm = vm()
        for (partial in listOf("", "h", "ht", "http:", "127.0.0.1:63", "127.0.0.1:6300/")) {
            vm.onUrlDraftChange(partial); pump()
            assertEquals(partial, vm.state.value.urlDraft)
            assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm.state.value.settings.baseUrl)
        }
        vm.onUrlDraftChange("http://localhost:6300/"); pump()
        assertTrue(vm.saveUrl()); pump()
        assertEquals("http://localhost:6300", vm.state.value.settings.baseUrl)
    }

    // ---- P0-5: off-phone servers need the opt-in AND https ----
    @Test fun p0_5_offPhoneNeedsOptInAndHttps() {
        val vm = vm()
        vm.onUrlDraftChange("http://192.168.1.20:6300"); pump()
        assertEquals(Copy.URL_OFF_PHONE_DISABLED, vm.state.value.urlError)
        assertFalse(vm.saveUrl())
        vm.setAllowOffPhone(true); pump()
        assertEquals(Copy.URL_OFF_PHONE_NEEDS_HTTPS, vm.state.value.urlError)
        assertFalse(vm.saveUrl())
        vm.onUrlDraftChange("https://prover.example.org"); pump()
        assertNull(vm.state.value.urlError)
        assertTrue(vm.saveUrl()); pump()
        assertTrue(vm.state.value.offPhone)
        vm.setAllowOffPhone(false); pump()
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm.state.value.settings.baseUrl)
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm().state.value.settings.baseUrl)
    }

    @Test fun p0_5_savedRemoteUrlIgnoredWithoutOptIn() {
        app.store.saveSettings(ProverSettings(onboardingDone = true, baseUrl = "http://10.0.0.5:6300", allowOffPhone = false))
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, vm().state.value.settings.baseUrl)
    }

    @Test fun inputsLockedWhileProving() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.start()
        val vm = vmAt("http://127.0.0.1:${server.port}")
        vm.onDraftChange("0x00ff"); pump()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        vm.prove(); pump()
        vm.onDraftChange("deadbeef"); pump()
        vm.onUrlDraftChange("http://localhost:1")
        vm.setAllowOffPhone(true)
        assertEquals("0x00ff", vm.state.value.draft)
        assertEquals("00ff", vm.state.value.session.payloadHex)
        assertEquals("http://127.0.0.1:${server.port}", vm.state.value.urlDraft)
        assertFalse(vm.state.value.settings.allowOffPhone)
        assertFalse(vm.saveUrl())
        vm.cancelProve()
    }

    @Test fun pastedPayloadNormalised() {
        val vm = vm()
        vm.onDraftChange("0x6d69\r\n646e\t69 67\r\n"); pump()
        assertNull(ProofJobInput.validationError(vm.state.value.draft))
        assertEquals("6d69646e6967", ProofJobInput.cleanHex(vm.state.value.draft))
    }

    @Test fun unreachableIsItsOwnFailure() {
        val vm = vmAt("http://127.0.0.1:1")
        vm.onDraftChange(Fixtures.dustSpendHex); pump(); vm.prove(); pump()
        waitFor { vm.state.value.session.state == JobState.FAILED }
        assertEquals("Unreachable", vm.state.value.session.failure?.kind)
        assertTrue(vm.state.value.history.first().circuit.contains("dust/spend"))
    }

    /** Checking the server must not wipe a finished proof (Joe / Bob P1-2). */
    @Test fun checkServerDoesNotHideAProof() {
        server.start()
        route(
            ready = MockResponse().setBody("{\"status\":\"ok\",\"jobsProcessing\":0,\"jobsPending\":0,\"jobCapacity\":10}"),
            prove = MockResponse().setResponseCode(400).setBody("rejected-for-test")
        )
        val vm = vmAt("http://127.0.0.1:${server.port}")
        vm.onDraftChange("00ff"); pump(); vm.prove(); pump()
        waitFor { vm.state.value.session.state == JobState.FAILED || vm.state.value.session.state == JobState.DONE }
        val before = vm.state.value.session.state
        val beforeFail = vm.state.value.session.failure
        vm.checkServer()
        waitFor { vm.state.value.health.kind == HealthKind.ONLINE || vm.state.value.health.kind == HealthKind.BUSY }
        assertEquals(before, vm.state.value.session.state)
        assertEquals(beforeFail?.kind, vm.state.value.session.failure?.kind)
    }

    @Test fun uiShowsTestnetHonestHeaderAndTip() {
        val vm = vm()
        compose.setContent {
            val state by vm.state.collectAsState()
            MidnightProverTheme(reduceMotion = true) { ProverApp(vm, state) }
        }
        compose.onNodeWithTag("testnet-chip").assertExists()
        compose.onNodeWithText(Copy.TESTNET_CHIP).assertExists()
        compose.onNodeWithText(Copy.HEADER_SUBTITLE).assertExists()
        compose.onNodeWithText("Prove Midnight transactions on this device", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Prove transaction").assertDoesNotExist()
        compose.onNodeWithTag("tab-SETTINGS").performClick()
        compose.onNodeWithTag("tip").performScrollTo()
        compose.onNodeWithTag("tip").assertExists()
        compose.onNodeWithText("TIP").assertExists()
        compose.onNodeWithText(Attribution.ADA_ADDRESS).assertExists()
        compose.onNodeWithText(Attribution.MIDNIGHT_ADDRESS).assertExists()
        compose.onNodeWithText(Copy.FOOTER_INDEPENDENCE).assertExists()
    }

    @Test fun uiUrlFieldKeepsTypingAndRefusesRemote() {
        val vm = vm()
        compose.setContent {
            val state by vm.state.collectAsState()
            MidnightProverTheme(reduceMotion = true) { ProverApp(vm, state, Tab.SERVER) }
        }
        compose.onNodeWithTag("url-field").performTextReplacement("127.0.0.1:63")
        compose.onNodeWithText("127.0.0.1:63").assertExists()
        compose.onNodeWithTag("url-field").performTextReplacement("http://192.168.1.20:6300")
        compose.onNodeWithText(Copy.URL_OFF_PHONE_DISABLED, substring = true).assertExists()
        compose.onNodeWithTag("save-url").assertIsNotEnabled()
    }
}
