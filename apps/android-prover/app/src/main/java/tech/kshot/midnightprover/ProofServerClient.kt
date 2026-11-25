package tech.kshot.midnightprover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ServerStatus(
    val reachable: Boolean,
    val version: String? = null,
    val proofVersions: String? = null,
    val ready: Boolean = false,
    val jobsProcessing: Int = 0,
    val jobsPending: Int = 0,
    val jobCapacity: Int? = null,
    val detail: String = ""
)

/** Ways a prove request can fail, kept distinct so the UI can advise well. */
sealed class ProveException(message: String) : Exception(message) {
    /** Nothing answered at the address (server down, wrong URL, no network). */
    class Unreachable(detail: String) : ProveException("Could not reach the proof server: $detail")
    /** 429 — the server's job queue is full. */
    class QueueFull : ProveException("The proof server's job queue is full (HTTP 429). Wait and try again.")
    /** 503 — the server reports itself busy/not ready. */
    class Busy : ProveException("The proof server reports it is busy (HTTP 503). Try again shortly.")
    /** 400 — the server rejected the payload; carries its own parser message. */
    class Rejected(serverMessage: String) : ProveException("Server rejected the payload: $serverMessage")
    /** Any other HTTP failure. */
    class Http(code: Int, body: String) : ProveException("HTTP $code: ${body.take(300)}")
}

/**
 * Client for the official Midnight proof server (9.0.2) HTTP API.
 *
 * Route 3 (CLAUDE-AP-1 verdict): the server runs in a Linux environment on
 * the phone itself and the app talks to it at localhost:6300. The base URL
 * is configurable so the same app can point at a LAN or cloud server.
 *
 * Protocol (verified live against the real 9.0.2 server, 2026-11-25):
 * proving is PER-CIRCUIT via POST /prove with a raw binary body — the
 * serialized proving payload, a (proof-preimage-versioned,
 * option(proving-data), option(fr-bls)) tuple — and returns raw proof
 * bytes headed `midnight:proof-versioned:`. This is the protocol
 * Midnight.js and the wallet SDK use. The older POST /prove-tx endpoint
 * is deprecated (Midnight's own local-proving guide: "Midnight.js doesn't
 * use it") and is NOT used here. Status endpoints: GET /version,
 * GET /proof-versions, GET /ready (JSON: status/jobsProcessing/
 * jobsPending/jobCapacity; 503 with status "busy" when the queue fills).
 */
class ProofServerClient(private val baseUrl: String) {

    @Volatile private var activeConnection: HttpURLConnection? = null

    /** Aborts an in-flight request, if any. */
    fun cancelActive() {
        activeConnection?.disconnect()
    }

    suspend fun status(): ServerStatus = withContext(Dispatchers.IO) {
        try {
            val version = getText("/version").trim()
            val proofVersions = getText("/proof-versions").trim()
            val ready = ReadyFields.parse(getText("/ready"))
            ServerStatus(
                reachable = true,
                version = version,
                proofVersions = proofVersions,
                ready = ready.status == "ok",
                jobsProcessing = ready.jobsProcessing,
                jobsPending = ready.jobsPending,
                jobCapacity = ready.jobCapacity,
                detail = ready.status
            )
        } catch (e: Exception) {
            ServerStatus(reachable = false, detail = e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Proves one serialized proving payload (given as hex). Returns the raw
     * proof bytes. Throws a [ProveException] subtype on failure.
     */
    suspend fun prove(payloadHex: String): ByteArray = withContext(Dispatchers.IO) {
        val payload = HexCodec.hexToBytes(ProofJobInput.cleanHex(payloadHex))
        val conn = URL(baseUrl + "/prove").openConnection() as HttpURLConnection
        activeConnection = conn
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.doOutput = true
            conn.connectTimeout = 5_000
            // Proving can take minutes on mobile hardware; keep the read window generous.
            conn.readTimeout = 600_000
            conn.outputStream.use { it.write(payload) }
            val code = conn.responseCode
            if (code in 200..299) {
                return@withContext conn.inputStream.use { it.readBytes() }
            }
            val body = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw when (code) {
                400 -> ProveException.Rejected(body.take(400))
                429 -> ProveException.QueueFull()
                503 -> ProveException.Busy()
                else -> ProveException.Http(code, body)
            }
        } catch (e: IOException) {
            throw ProveException.Unreachable(e.message ?: e.javaClass.simpleName)
        } finally {
            if (activeConnection === conn) activeConnection = null
            conn.disconnect()
        }
    }

    private fun getText(path: String): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        activeConnection = conn
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 5_000
            conn.readTimeout = 10_000
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw ProveException.Http(code, text)
            return text
        } finally {
            if (activeConnection === conn) activeConnection = null
            conn.disconnect()
        }
    }
}
