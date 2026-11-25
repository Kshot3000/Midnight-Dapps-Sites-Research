package tech.kshot.midnightprover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerStatus(
    val reachable: Boolean,
    val version: String? = null,
    val proofVersions: String? = null,
    val ready: Boolean = false,
    val jobCapacity: Int? = null,
    val detail: String = ""
)

/**
 * Client for the official Midnight proof server (9.0.2) HTTP API.
 *
 * Route 3 (CLAUDE-AP-1 verdict): the server runs in a Linux environment on
 * the phone itself and the app talks to it at localhost:6300. The base URL
 * is configurable so the same app can point at a LAN or cloud server.
 * Endpoints verified live against the real 9.0.2 server on 2026-11-24:
 * GET /version, GET /proof-versions, GET /ready, POST /prove-tx.
 */
class ProofServerClient(private val baseUrl: String) {

    suspend fun status(): ServerStatus = withContext(Dispatchers.IO) {
        try {
            val version = getText("/version").trim()
            val proofVersions = getText("/proof-versions").trim()
            val readyJson = JSONObject(getText("/ready"))
            ServerStatus(
                reachable = true,
                version = version,
                proofVersions = proofVersions,
                ready = readyJson.optBoolean("ready", false),
                jobCapacity = if (readyJson.has("jobCapacity")) readyJson.optInt("jobCapacity") else null,
                detail = "ok"
            )
        } catch (e: Exception) {
            ServerStatus(reachable = false, detail = e.message ?: e.javaClass.simpleName)
        }
    }

    /** Submits a transaction body (hex) for proving. Returns the raw server response. */
    suspend fun proveTx(txHex: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("tx", ProofJobInput.cleanHex(txHex)).toString()
        postJson("/prove-tx", body)
    }

    private fun getText(path: String): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5_000
        conn.readTimeout = 10_000
        return readResponse(conn)
    }

    private fun postJson(path: String, body: String): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 5_000
        // Proving can take minutes on mobile hardware; keep the read window generous.
        conn.readTimeout = 600_000
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        return readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code: ${text.take(300)}")
        }
        return text
    }
}
