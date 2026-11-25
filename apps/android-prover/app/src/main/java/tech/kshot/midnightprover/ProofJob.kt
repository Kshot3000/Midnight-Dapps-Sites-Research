package tech.kshot.midnightprover

/** Lifecycle of one proof job, mirroring the proof-server's own states. */
enum class JobState { IDLE, CHECKING, PROVING, DONE, FAILED }

/**
 * Pure helpers for proof-job input. Kept free of Android APIs so they are
 * unit-testable on the JVM.
 */
object ProofJobInput {
    /** A proving payload is submitted as hex. Returns null when valid. */
    fun validationError(txHex: String): String? {
        val cleaned = txHex.trim().replace(" ", "").replace("\n", "")
        if (cleaned.isEmpty()) return "Paste a proving payload (hex) first."
        if (cleaned.length % 2 != 0) return "Hex must have an even number of characters."
        if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return "Proving payload must be hex (0-9, a-f)."
        }
        return null
    }

    fun cleanHex(txHex: String): String =
        txHex.trim().replace(" ", "").replace("\n", "").lowercase()

    /** Normalises a user-entered server base URL: no trailing slash. */
    fun normaliseBaseUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url.trimEnd('/')
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
    val detail: String
)

object JobHistoryFormat {
    /** "0x9f3c…ab21 · 84 bytes" style one-liner for a payload or proof. */
    fun summariseTx(cleanedHex: String): String {
        val bytes = cleanedHex.length / 2
        return if (cleanedHex.length <= 12) {
            "0x$cleanedHex · $bytes bytes"
        } else {
            "0x${cleanedHex.take(8)}…${cleanedHex.takeLast(4)} · $bytes bytes"
        }
    }

    fun formatDuration(seconds: Double): String =
        if (seconds < 60) String.format("%.1f s", seconds)
        else String.format("%d min %.0f s", (seconds / 60).toInt(), seconds % 60)
}

/** Hex codec for the binary payloads the proof server speaks. Pure + tested. */
object HexCodec {
    fun hexToBytes(cleanedHex: String): ByteArray {
        require(cleanedHex.length % 2 == 0) { "Hex must have an even number of characters." }
        return ByteArray(cleanedHex.length / 2) { i ->
            val hi = Character.digit(cleanedHex[i * 2], 16)
            val lo = Character.digit(cleanedHex[i * 2 + 1], 16)
            require(hi >= 0 && lo >= 0) { "Proving payload must be hex (0-9, a-f)." }
            ((hi shl 4) + lo).toByte()
        }
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}

/** Facts about a proof the server returned. Pure + tested. */
object ProofFormat {
    private val PROOF_HEADER = "midnight:proof-versioned:".toByteArray(Charsets.US_ASCII)

    fun isProof(bytes: ByteArray): Boolean =
        bytes.size > PROOF_HEADER.size &&
            PROOF_HEADER.indices.all { bytes[it] == PROOF_HEADER[it] }

    fun summarise(bytes: ByteArray): String =
        if (isProof(bytes)) "Midnight proof · ${bytes.size} bytes"
        else "Server response · ${bytes.size} bytes (unexpected header)"
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
