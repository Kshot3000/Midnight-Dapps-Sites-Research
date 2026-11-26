package tech.kshot.midnightprover

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Live check against a real proof server. Skipped unless PROOF_SERVER_URL is
 * set, e.g. `PROOF_SERVER_URL=http://127.0.0.1:6301 gradle testDebugUnitTest`.
 * Uses the real 1,509-byte DUST-spend payload captured from the official
 * midnight-examples hello-world flow on a local `undeployed` devnet (not
 * testnet, not mainnet). Timings are from the machine running the test —
 * NOT a phone measurement.
 */
class LiveProofServerTest {
    private val url = System.getenv("PROOF_SERVER_URL")

    @Test fun realServerEndToEnd() = runBlocking {
        assumeTrue("PROOF_SERVER_URL not set — live test skipped", !url.isNullOrBlank())
        val client = ProofServerClient(url!!)
        val log = StringBuilder()

        val t0 = System.nanoTime()
        val status = client.statusWithRetry()
        val statusMs = (System.nanoTime() - t0) / 1_000_000
        log.appendLine("server=$url version=${status.version} proofVersions=${status.proofVersions} ready=${status.readyStatus} capacity=${status.jobCapacity} statusSweepMs=$statusMs")
        assertTrue(status.reachable)
        assertEquals(ProofJobInput.EXPECTED_SERVER_VERSION, status.version)
        assertTrue(status.ready)

        val info = PayloadInspector.inspect(Fixtures.dustSpend)
        assertEquals("midnight/dust/spend", info.keyLocation)

        repeat(2) { i ->
            val p0 = System.nanoTime()
            val proof = client.prove(Fixtures.dustSpend)
            val ms = (System.nanoTime() - p0) / 1_000_000
            log.appendLine("prove#${i + 1} payload=${Fixtures.dustSpend.size}B proof=${proof.size}B headerOk=${ProofFormat.isProof(proof)} clientMs=$ms")
            assertTrue(ProofFormat.isProof(proof))
        }

        val rejected = runCatching { client.prove("garbage".toByteArray()) }.exceptionOrNull()
        log.appendLine("garbage -> ${rejected?.javaClass?.simpleName}: ${rejected?.message}")
        assertTrue(rejected is ProveException.Rejected)
        assertTrue((rejected as ProveException.Rejected).serverMessage.startsWith("expected header tag"))

        println(log)
        File("build/live-proof-server.txt").apply { parentFile?.mkdirs() }.writeText(log.toString())
    }
}
