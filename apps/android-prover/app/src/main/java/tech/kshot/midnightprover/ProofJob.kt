package tech.kshot.midnightprover

import java.net.URI
import java.util.Locale

/** Lifecycle of one proof job. */
enum class JobState { IDLE, PROVING, DONE, FAILED, CANCELLED }

/**
 * Pure helpers for proof-job input. Kept free of Android APIs so they are
 * unit-testable on the JVM.
 */
object ProofJobInput {
    private val WHITESPACE = Regex("\\s+")

    /** Strips all whitespace and an optional 0x prefix, lower-cases. */
    fun cleanHex(txHex: String): String {
        val noWs = txHex.replace(WHITESPACE, "")
        val body = if (noWs.startsWith("0x") || noWs.startsWith("0X")) noWs.substring(2) else noWs
        return body.lowercase(Locale.ROOT)
    }

    /** A proving payload is submitted as hex. Returns null when valid. */
    fun validationError(txHex: String): String? {
        val cleaned = cleanHex(txHex)
        if (cleaned.isEmpty()) return Copy.INPUT_EMPTY
        if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' }) {
            return Copy.INPUT_NOT_HEX
        }
        if (cleaned.length % 2 != 0) return Copy.INPUT_ODD
        return null
    }

    /** Normalises a user-entered server base URL: adds http:// when no scheme, no trailing slash. */
    fun normaliseBaseUrl(raw: String): String {
        var url = raw.trim()
        if (!url.contains("://")) url = "http://$url"
        return url.trimEnd('/')
    }

    /**
     * Validates a server URL as typed. Returns null when usable, otherwise a
     * plain-language reason. Only http/https with a host are accepted; a path,
     * query or fragment is refused because the client appends API paths.
     */
    fun baseUrlError(raw: String, allowOffPhone: Boolean = false): String? {
        if (raw.isBlank()) return "Enter the proof server address, e.g. http://127.0.0.1:6300"
        if (raw.trim().contains(' ')) return "The address can't contain spaces."
        val url = normaliseBaseUrl(raw)
        val uri = runCatching { URI(url) }.getOrNull() ?: return "That doesn't look like a web address."
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return "Use http:// or https://."
        if (uri.host.isNullOrBlank()) return "The address needs a host, e.g. 127.0.0.1."
        if (uri.port != -1 && uri.port !in 1..65535) return "The port must be between 1 and 65535."
        if (!uri.rawPath.isNullOrEmpty() && uri.rawPath != "/") return "Leave off any path — just scheme, host and port."
        if (uri.rawQuery != null || uri.rawFragment != null) return "Leave off any ? or # part."
        if (!isLocalhost(url)) {
            if (!allowOffPhone) return Copy.URL_OFF_PHONE_DISABLED
            if (scheme != "https") return Copy.URL_OFF_PHONE_NEEDS_HTTPS
        }
        return null
    }

    /** True when the URL points at this device (Route 3). */
    fun isLocalhost(url: String): Boolean {
        val host = runCatching { URI(normaliseBaseUrl(url)).host }.getOrNull()?.lowercase(Locale.ROOT)
        return host == "127.0.0.1" || host == "localhost" || host == "::1" || host == "[::1]"
    }

    const val DEFAULT_BASE_URL = "http://127.0.0.1:6300"
    const val EXPECTED_SERVER_VERSION = "9.0.2"
}

/** One completed (or failed) proof job, shown in the Activity history. */
data class JobRecord(
    val finishedAtMillis: Long,
    val serverUrl: String,
    val txPreview: String,
    val success: Boolean,
    val durationSeconds: Double,
    val detail: String,
    val circuit: String = "",
    val cancelled: Boolean = false
) {
    val outcome: String get() = when {
        success -> Copy.OUTCOME_PROVEN
        cancelled -> Copy.OUTCOME_CANCELLED
        else -> Copy.OUTCOME_FAILED
    }
}

object JobHistoryFormat {
    /** "0x9f3c…ab21 · 84 bytes" style one-liner for a payload or proof. */
    fun summariseTx(cleanedHex: String): String {
        val bytes = cleanedHex.length / 2
        return if (cleanedHex.length <= 12) {
            "0x$cleanedHex · ${formatBytes(bytes)}"
        } else {
            "0x${cleanedHex.take(8)}…${cleanedHex.takeLast(4)} · ${formatBytes(bytes)}"
        }
    }

    fun formatBytes(bytes: Int): String = when {
        bytes < 1024 -> "$bytes bytes"
        bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
        else -> String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0))
    }

    /** Joe P2-1: round to a tenth first, then split, so 119.7 s never reads "1 min 60 s". */
    fun formatDuration(seconds: Double): String {
        val tenths = Math.round(seconds.coerceAtLeast(0.0) * 10) / 10.0
        if (tenths < 60) return String.format(Locale.ROOT, "%.1f s", tenths)
        val whole = tenths.toInt()
        return String.format(Locale.ROOT, "%d min %02d s", whole / 60, whole % 60)
    }

    /** TalkBack-friendly duration: "1 minute 5 seconds". */
    fun spokenDuration(seconds: Double): String {
        val total = seconds.toInt()
        val m = total / 60
        val s = total % 60
        val sec = if (s == 1) "1 second" else "$s seconds"
        return if (m == 0) sec else (if (m == 1) "1 minute " else "$m minutes ") + sec
    }
}

/** Hex codec for the binary payloads the proof server speaks. Pure + tested. */
object HexCodec {
    fun hexToBytes(cleanedHex: String): ByteArray {
        require(cleanedHex.length % 2 == 0) { "Hex must have an even number of characters." }
        return ByteArray(cleanedHex.length / 2) { i ->
            val hi = Character.digit(cleanedHex[i * 2], 16)
            val lo = Character.digit(cleanedHex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "The payload must be hex (0-9, a-f)." }
            ((hi shl 4) + lo).toByte()
        }
    }

    private val DIGITS = "0123456789abcdef".toCharArray()

    fun bytesToHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        bytes.forEachIndexed { i, b ->
            val v = b.toInt() and 0xff
            out[i * 2] = DIGITS[v ushr 4]
            out[i * 2 + 1] = DIGITS[v and 0x0f]
        }
        return String(out)
    }
}

/** Facts about a proof the server returned. Pure + tested. */
object ProofFormat {
    const val PROOF_HEADER_TEXT = "midnight:proof-versioned:"
    private val PROOF_HEADER = PROOF_HEADER_TEXT.toByteArray(Charsets.US_ASCII)

    fun isProof(bytes: ByteArray): Boolean =
        bytes.size > PROOF_HEADER.size &&
            PROOF_HEADER.indices.all { bytes[it] == PROOF_HEADER[it] }

    private val PROOF_HEADER_HEX = HexCodec.bytesToHex(PROOF_HEADER_TEXT.toByteArray(Charsets.US_ASCII))

    fun isProofHex(hex: String): Boolean = hex.length > PROOF_HEADER_HEX.length && hex.startsWith(PROOF_HEADER_HEX)

    fun summarise(bytes: ByteArray): String =
        if (isProof(bytes)) "Midnight proof · ${JobHistoryFormat.formatBytes(bytes.size)}"
        else "Server response · ${JobHistoryFormat.formatBytes(bytes.size)} (unexpected header)"
}

/**
 * What can be read from a serialized proving payload without a ledger
 * library: its header tag, the circuit's key location (e.g.
 * `midnight/dust/spend` or `contract:<address>/<circuit>`), and whether the
 * prover key travels inside it. The circuit size k is NOT stored in the
 * payload in readable form, so it is never guessed. Pure + tested.
 */
data class PayloadInfo(
    val headerOk: Boolean,
    val keyLocation: String?,
    val circuitName: String?,
    val contractAddress: String?,
    val carriesProverKey: Boolean,
    val sizeBytes: Int
) {
    val kindLabel: String get() = when {
        keyLocation == null -> "Unknown circuit"
        keyLocation.startsWith("midnight/") -> "Built-in ledger circuit"
        contractAddress != null -> "Contract circuit"
        else -> "Circuit"
    }
}

object PayloadInspector {
    const val PROVE_HEADER = "midnight:(proof-preimage-versioned,option(proving-data),option(fr-bls)):"
    private val BUILTIN = Regex("midnight/[a-z0-9_]+(?:/[a-z0-9_]+)+")
    private val CONTRACT = Regex("contract:([0-9a-f]{64})/([A-Za-z_][A-Za-z0-9_]*)")

    fun inspect(bytes: ByteArray): PayloadInfo {
        // Latin-1 keeps one char per byte so offsets stay meaningful.
        val text = String(bytes, Charsets.ISO_8859_1)
        val contract = CONTRACT.find(text)
        val builtin = BUILTIN.find(text)
        val keyLocation: String?
        val circuit: String?
        val address: String?
        if (contract != null && (builtin == null || contract.range.first < builtin.range.first)) {
            address = contract.groupValues[1]
            circuit = contract.groupValues[2]
            keyLocation = "contract:${address.take(8)}…/$circuit"
        } else if (builtin != null) {
            address = null
            keyLocation = builtin.value
            circuit = builtin.value.removePrefix("midnight/")
        } else {
            address = null; keyLocation = null; circuit = null
        }
        return PayloadInfo(
            headerOk = text.startsWith(PROVE_HEADER),
            keyLocation = keyLocation,
            circuitName = circuit,
            contractAddress = address,
            carriesProverKey = text.contains("midnight:prover-key["),
            sizeBytes = bytes.size
        )
    }

    fun inspectHex(hex: String): PayloadInfo? {
        if (ProofJobInput.validationError(hex) != null) return null
        return inspect(HexCodec.hexToBytes(ProofJobInput.cleanHex(hex)))
    }
}

/** The flat fields of GET /ready, parsed without a JSON library so the
 *  parsing is unit-testable on the JVM. Pure + tested. */
data class ReadyFields(
    val status: String = "",
    val jobsProcessing: Int = 0,
    val jobsPending: Int = 0,
    val jobCapacity: Int? = null
) {
    companion object {
        fun parse(body: String): ReadyFields {
            fun stringField(name: String): String? =
                Regex("\"$name\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
            fun intField(name: String): Int? =
                Regex("\"$name\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()
            return ReadyFields(
                status = stringField("status") ?: "",
                jobsProcessing = intField("jobsProcessing") ?: 0,
                jobsPending = intField("jobsPending") ?: 0,
                jobCapacity = intField("jobCapacity")
            )
        }
    }
}

/** Attribution shown in About and asserted by tests, character for character. */
object Attribution {
    const val HANDLE = "@kshot9000"
    const val ADA_ADDRESS =
        "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v"
    const val MIDNIGHT_ADDRESS =
        "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q"
    const val INDEPENDENCE = "Independent builder — not affiliated with the Midnight Foundation or IOG."
}
