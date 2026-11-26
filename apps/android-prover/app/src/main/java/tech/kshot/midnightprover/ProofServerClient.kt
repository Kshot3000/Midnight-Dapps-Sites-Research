package tech.kshot.midnightprover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ServerStatus(
    val reachable: Boolean,
    val version: String? = null,
    val proofVersions: String? = null,
    val ready: Boolean = false,
    val readyStatus: String = "",
    val jobsProcessing: Int = 0,
    val jobsPending: Int = 0,
    val jobCapacity: Int? = null,
    val latencyMs: Long? = null,
    val failure: ProveException? = null,
    val checkedAtMillis: Long = System.currentTimeMillis()
) {
    val versionMatches: Boolean get() = version == ProofJobInput.EXPECTED_SERVER_VERSION
    /** Matches the server's own rule: busy, or pending jobs at capacity (Bob P2-22). */
    val queueFull: Boolean get() =
        readyStatus == "busy" || (jobCapacity != null && jobCapacity > 0 && jobsPending >= jobCapacity)
}

/**
 * Ways a request can fail, kept distinct so the UI can give each its own
 * state and advice. [title] is a short headline, [message] the detail.
 */
sealed class ProveException(val title: String, message: String) : Exception(message) {
    /** Nothing answered at the address (server down, wrong URL, no network). */
    class Unreachable(val detail: String) : ProveException("Can't reach the proof server", detail)
    /** The server accepted the connection but did not answer in time. */
    class Timeout(what: String, seconds: Int) :
        ProveException("The proof server timed out", "No answer to $what within $seconds s.")
    /** 429 — the server's job queue is full. */
    class QueueFull : ProveException("Server queue is full", "The proof server refused the job (HTTP 429). Wait for running jobs to finish, then retry.")
    /** 503 — the server reports itself busy/not ready. */
    class Busy : ProveException("Proof server busy", "The proof server reports it is busy (HTTP 503). Retry shortly.")
    /** 400 — the server rejected the payload; carries its own parser message. */
    class Rejected(val serverMessage: String) :
        ProveException("The server rejected this payload", serverMessage.ifBlank { "HTTP 400 with no message." })
    /** 2xx but the body is not a Midnight proof. */
    class BadResponse(detail: String) : ProveException("Unexpected server response", detail)
    /** Any other HTTP failure. */
    class Http(val code: Int, body: String) : ProveException("Server error (HTTP $code)", Copy.errHttp(code, body.take(300).ifBlank { "no message" }))
}

/** Timeouts in milliseconds; prove is long because phone proving is unmeasured. */
const val PROVE_TIMEOUT_MINUTES = 11

data class ClientTimeouts(
    val connectMs: Int = 5_000,
    val statusReadMs: Int = 8_000,
    /** 11 min: just above the proof server's own 600 s job timeout, so a dropped job surfaces as Timeout. */
    val proveReadMs: Int = PROVE_TIMEOUT_MINUTES * 60_000
)

/**
 * Client for the official Midnight proof server (9.0.2) HTTP API.
 *
 * Route 3 (CLAUDE-AP-1 verdict): the server runs in a Linux environment on
 * the phone itself and the app talks to it at localhost:6300. The base URL
 * is configurable so the same app can point at a LAN or cloud server.
 *
 * Protocol (verified live against the real 9.0.2 server, 2026-11-25, and
 * again by GROK-17 with a payload captured from the official hello-world
 * flow): proving is PER-CIRCUIT via POST /prove with a raw binary body — a
 * (proof-preimage-versioned, option(proving-data), option(fr-bls)) tuple —
 * and returns raw proof bytes headed `midnight:proof-versioned:`. The older
 * POST /prove-tx is deprecated ("Midnight.js doesn't use it") and not used.
 *
 * Each request owns its own connection: a status poll can never replace
 * the in-flight prove connection, so [cancelProve] always aborts the right
 * request (a v0.3.1 bug: one shared field meant a status call made during a
 * prove left cancel pointing at nothing).
 */
class ProofServerClient(
    val baseUrl: String,
    private val timeouts: ClientTimeouts = ClientTimeouts()
) {
    @Volatile private var proveConnection: HttpURLConnection? = null

    /** Aborts the in-flight prove, if any. Safe from any thread. */
    fun cancelProve() {
        proveConnection?.disconnect()
    }

    /** One status sweep: /version, /proof-versions, /ready. Never throws. */
    suspend fun status(): ServerStatus = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        try {
            val version = getText("/version").trim()
            val proofVersions = getText("/proof-versions").trim()
            val (readyCode, readyBody) = getRaw("/ready")
            val ready = ReadyFields.parse(readyBody)
            ServerStatus(
                reachable = true,
                version = version,
                proofVersions = proofVersions,
                ready = readyCode in 200..299 && ready.status == "ok",
                readyStatus = ready.status.ifBlank { "HTTP $readyCode" },
                jobsProcessing = ready.jobsProcessing,
                jobsPending = ready.jobsPending,
                jobCapacity = ready.jobCapacity,
                latencyMs = (System.nanoTime() - started) / 1_000_000
            )
        } catch (e: ProveException) {
            ServerStatus(reachable = e !is ProveException.Unreachable && e !is ProveException.Timeout, failure = e)
        } catch (e: IOException) {
            ServerStatus(reachable = false, failure = classifyIo(e, "the status check", timeouts.statusReadMs))
        }
    }

    /**
     * Proves one serialized proving payload. Returns the raw proof bytes.
     * Throws a [ProveException] on failure, or CancellationException when the
     * calling coroutine is cancelled (the connection is dropped immediately).
     */
    suspend fun prove(payload: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val conn = URL("$baseUrl/prove").openConnection() as HttpURLConnection
        proveConnection = conn
        try {
            runInterruptible(conn) {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(payload.size)
                conn.connectTimeout = timeouts.connectMs
                conn.readTimeout = timeouts.proveReadMs
                conn.outputStream.use { it.write(payload) }
                val code = conn.responseCode
                if (code in 200..299) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    if (!ProofFormat.isProof(bytes)) {
                        throw ProveException.BadResponse(
                            "HTTP $code but the body (${bytes.size} bytes) isn't headed `${ProofFormat.PROOF_HEADER_TEXT}`."
                        )
                    }
                    bytes
                } else {
                    val body = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw classifyHttp(code, body)
                }
            }
        } catch (e: IOException) {
            throw classifyIo(e, "the proof request", timeouts.proveReadMs)
        } finally {
            if (proveConnection === conn) proveConnection = null
            conn.disconnect()
        }
    }

    /** Runs blocking I/O so coroutine cancellation disconnects the socket at once. */
    private suspend fun <T> runInterruptible(conn: HttpURLConnection, block: () -> T): T =
        suspendCancellableCoroutine { cont ->
            val worker = Thread({
                try {
                    val r = block()
                    if (cont.isActive) cont.resume(r)
                } catch (t: Throwable) {
                    if (cont.isActive) cont.resumeWithException(t)
                }
            }, "prove-io")
            cont.invokeOnCancellation { conn.disconnect(); worker.interrupt() }
            worker.start()
        }

    private fun getRaw(path: String): Pair<Int, String> {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = timeouts.connectMs
            conn.readTimeout = timeouts.statusReadMs
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } catch (e: IOException) {
            throw classifyIo(e, "GET $path", timeouts.statusReadMs)
        } finally {
            conn.disconnect()
        }
    }

    private fun getText(path: String): String {
        val (code, text) = getRaw(path)
        if (code !in 200..299) throw classifyHttp(code, text)
        return text
    }

    companion object {
        fun classifyHttp(code: Int, body: String): ProveException = when (code) {
            400 -> ProveException.Rejected(body.trim().take(400))
            429 -> ProveException.QueueFull()
            503 -> ProveException.Busy()
            else -> ProveException.Http(code, body)
        }

        fun classifyIo(e: IOException, what: String, readTimeoutMs: Int): ProveException = when (e) {
            is SocketTimeoutException ->
                if (e.message?.contains("connect", ignoreCase = true) == true)
                    ProveException.Unreachable("Connection timed out — is the server running at this address?")
                else ProveException.Timeout(what, readTimeoutMs / 1000)
            is ConnectException -> ProveException.Unreachable("Connection refused — nothing is listening at this address. Start the proof server, then retry.")
            is UnknownHostException -> ProveException.Unreachable("Unknown host — check the address on the Server tab.")
            is java.net.UnknownServiceException -> ProveException.Unreachable(Copy.URL_OFF_PHONE_NEEDS_HTTPS)
            is InterruptedIOException -> ProveException.Unreachable("The request was interrupted.")
            else -> ProveException.Unreachable(e.message ?: e.javaClass.simpleName)
        }
    }
}
