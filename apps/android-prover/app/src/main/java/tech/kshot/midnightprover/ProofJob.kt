package tech.kshot.midnightprover

/** Lifecycle of one proof job, mirroring the proof-server's own states. */
enum class JobState { IDLE, CHECKING, PROVING, DONE, FAILED }

/**
 * Pure helpers for proof-job input. Kept free of Android APIs so they are
 * unit-testable on the JVM.
 */
object ProofJobInput {
    /** A transaction body is submitted as hex. Returns null when valid. */
    fun validationError(txHex: String): String? {
        val cleaned = txHex.trim().replace(" ", "").replace("\n", "")
        if (cleaned.isEmpty()) return "Paste a transaction body (hex) first."
        if (cleaned.length % 2 != 0) return "Hex must have an even number of characters."
        if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return "Transaction body must be hex (0-9, a-f)."
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
    /** "0x9f3c…ab21 · 84 bytes" style one-liner for a transaction body. */
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
