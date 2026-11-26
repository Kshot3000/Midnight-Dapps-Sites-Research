package tech.kshot.midnightprover

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** Client behaviour against a scripted HTTP server: every failure class gets its own exception. */
class ProofServerClientTest {
    private lateinit var server: MockWebServer
    private val proof = ProofFormat.PROOF_HEADER_TEXT.toByteArray() + ByteArray(32) { it.toByte() }

    @Before fun setUp() { server = MockWebServer(); server.start() }
    @After fun tearDown() { server.shutdown() }

    private fun client(t: ClientTimeouts = ClientTimeouts(connectMs = 2000, statusReadMs = 2000, proveReadMs = 3000)) =
        ProofServerClient(server.url("/").toString().trimEnd('/'), t)

    private fun route(ready: MockResponse, version: String = "9.0.2") {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/version" -> MockResponse().setBody(version)
                "/proof-versions" -> MockResponse().setBody("[\"V2\", \"V3\"]")
                "/ready" -> ready
                else -> MockResponse().setResponseCode(404)
            }
        }
    }

    @Test fun statusOnline() = runBlocking {
        route(MockResponse().setBody("""{"status":"ok","jobsProcessing":1,"jobsPending":0,"jobCapacity":10}"""))
        val s = client().status()
        assertTrue(s.reachable); assertTrue(s.ready); assertEquals("9.0.2", s.version); assertEquals(10, s.jobCapacity)
        assertEquals(HealthKind.ONLINE, ServerHealth.from(s).kind)
    }

    /** Bob P0-1 / GROK-14 fix 4: /ready 503 busy → reachable and BUSY, never "unreachable". */
    @Test fun readyBusy503IsReachable() = runBlocking {
        route(MockResponse().setResponseCode(503).setBody("""{"status":"busy","jobsProcessing":10,"jobsPending":0,"jobCapacity":10}"""))
        val s = client().status()
        assertTrue(s.reachable); assertFalse(s.ready); assertEquals("busy", s.readyStatus)
        assertEquals(HealthKind.BUSY, ServerHealth.from(s).kind)
    }

    @Test fun nothingListeningIsOffline() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val s = ProofServerClient("http://127.0.0.1:$port").status()
        assertFalse(s.reachable)
        assertTrue(s.failure is ProveException.Unreachable)
    }

    @Test fun provesAndSendsRawBinary() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(proof)))
        val out = client().prove(Fixtures.dustSpend)
        assertTrue(out.contentEquals(proof))
        val req = server.takeRequest()
        assertEquals("POST", req.method); assertEquals("/prove", req.path)
        assertEquals("application/octet-stream", req.getHeader("Content-Type"))
        assertTrue(req.body.readByteArray().contentEquals(Fixtures.dustSpend))
    }

    private suspend fun expect(resp: MockResponse): ProveException {
        server.enqueue(resp)
        try { client().prove(byteArrayOf(1, 2)); fail("expected failure") } catch (e: ProveException) { return e }
        throw AssertionError()
    }

    @Test fun failureClasses() = runBlocking {
        val rejected = expect(MockResponse().setResponseCode(400).setBody("expected header tag 'midnight:(proof-preimage-versioned,…)', got 'garbage'"))
        assertTrue(rejected is ProveException.Rejected)
        assertTrue((rejected as ProveException.Rejected).serverMessage.startsWith("expected header tag"))
        assertTrue(expect(MockResponse().setResponseCode(429)) is ProveException.QueueFull)
        assertTrue(expect(MockResponse().setResponseCode(503)) is ProveException.Busy)
        assertTrue(expect(MockResponse().setResponseCode(500).setBody("x")) is ProveException.Http)
        assertTrue(expect(MockResponse().setBody("not a proof")) is ProveException.BadResponse)
    }

    @Test fun slowServerTimesOut() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(proof)).setHeadersDelay(5, TimeUnit.SECONDS))
        try {
            client(ClientTimeouts(connectMs = 1000, statusReadMs = 1000, proveReadMs = 500)).prove(byteArrayOf(1))
            fail("expected timeout")
        } catch (e: ProveException) {
            assertTrue(e.toString(), e is ProveException.Timeout)
        }
    }

    @Test fun cancellationDropsTheRequestPromptly() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val c = client(ClientTimeouts(proveReadMs = 60_000))
        val job = async { runCatching { c.prove(byteArrayOf(1)) } }
        delay(300)
        val t0 = System.nanoTime()
        job.cancel()
        withTimeout(2_000) { job.join() }
        assertTrue("cancel took too long", (System.nanoTime() - t0) / 1_000_000 < 2_000)
        assertTrue(job.isCancelled)
    }

    @Test fun cancelProveDisconnects() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val c = client(ClientTimeouts(proveReadMs = 60_000))
        val job = async { runCatching { c.prove(byteArrayOf(1)) } }
        delay(300)
        c.cancelProve()
        val r = withTimeout(3_000) { job.await() }
        assertTrue(r.isFailure)
        assertFalse(r.exceptionOrNull() is CancellationException)
    }
}
