package tech.kshot.midnightprover

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Engine lifecycle, persistence and process-death recovery (Robolectric for SharedPreferences/files). */
@RunWith(AndroidJUnit4::class)
class ProveEngineTest {
    private lateinit var server: MockWebServer
    private lateinit var store: ProverStore
    private val proof = ProofFormat.PROOF_HEADER_TEXT.toByteArray() + ByteArray(40)

    @Before fun setUp() {
        server = MockWebServer(); server.start()
        store = ProverStore(ApplicationProvider.getApplicationContext())
    }
    @After fun tearDown() { server.shutdown() }

    private val url get() = server.url("/").toString().trimEnd('/')

    private fun ProveEngine.awaitDone() = runBlocking { withTimeout(5_000) { session.first { !it.isRunning } } }

    @Test fun successIsRecordedAndPersisted() {
        server.enqueue(MockResponse().setBody(Buffer().write(proof)))
        val engine = ProveEngine(store)
        assertTrue(engine.start(Fixtures.dustSpendHex, url, ClientTimeouts()))
        val done = engine.awaitDone()
        assertEquals(JobState.DONE, done.state)
        assertEquals(proof.size, done.proofBytes)
        assertEquals("midnight/dust/spend", engine.history.value.first().circuit)
        assertTrue(engine.history.value.first().success)
        // A fresh engine (new process) still has the result and the history.
        val again = ProveEngine(store)
        assertEquals(JobState.DONE, again.session.value.state)
        assertEquals(done.proofHex, again.session.value.proofHex)
        assertEquals(1, again.history.value.size)
    }

    @Test fun failureKeepsItsKind() {
        server.enqueue(MockResponse().setResponseCode(429))
        val engine = ProveEngine(store)
        engine.start(Fixtures.dustSpendHex, url, ClientTimeouts())
        val done = engine.awaitDone()
        assertEquals(JobState.FAILED, done.state)
        assertEquals("QueueFull", done.failure?.kind)
        assertEquals("FAILED", engine.history.value.first().outcome)
    }

    @Test fun cancelIsItsOwnOutcome() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val engine = ProveEngine(store)
        engine.start(Fixtures.dustSpendHex, url, ClientTimeouts(proveReadMs = 60_000))
        Thread.sleep(200)
        engine.cancel()
        assertEquals(JobState.CANCELLED, engine.session.value.state)
        assertEquals("CANCELLED", engine.history.value.first().outcome)
        assertFalse(engine.start("zz", url, ClientTimeouts()))
    }

    @Test fun processDeathMidProveBecomesInterruptedWithPayloadKept() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val engine = ProveEngine(store)
        engine.start(Fixtures.dustSpendHex, url, ClientTimeouts(proveReadMs = 60_000))
        assertEquals(JobState.PROVING, store.loadSession().state) // persisted synchronously
        // Simulate the process being killed: a new engine reads the same store.
        val revived = ProveEngine(store)
        val s = revived.session.value
        assertEquals(JobState.INTERRUPTED, s.state)
        assertEquals(Fixtures.dustSpendHex, s.payloadHex)
        assertEquals("Interrupted", s.failure?.kind)
        engine.cancel()
    }

    @Test fun invalidPayloadFailsWithoutNetwork() {
        val engine = ProveEngine(store)
        assertFalse(engine.start("abc", url, ClientTimeouts()))
        assertEquals("Invalid", engine.session.value.failure?.kind)
        assertEquals(0, server.requestCount)
    }

    @Test fun settingsRoundTrip() {
        store.saveSettings(ProverSettings(baseUrl = "https://10.0.0.2:6300", proveTimeoutMinutes = 30, reduceMotion = true, keepScreenOn = false, onboardingDone = true, allowOffPhone = true))
        val s = store.loadSettings()
        assertEquals("https://10.0.0.2:6300", s.baseUrl); assertEquals(30, s.proveTimeoutMinutes)
        assertTrue(s.reduceMotion); assertFalse(s.keepScreenOn); assertTrue(s.onboardingDone)
        // Off-phone URL without the opt-in is not honoured on load (P0-5).
        store.saveSettings(s.copy(allowOffPhone = false))
        assertEquals(ProofJobInput.DEFAULT_BASE_URL, store.loadSettings().baseUrl)
    }
}
