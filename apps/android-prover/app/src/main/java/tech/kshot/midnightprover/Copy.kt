package tech.kshot.midnightprover

/**
 * Every user-facing sentence in one place. Wording follows the proposed
 * on-screen strings in `research/android-prover-qa.md` section 6 (GROK-14,
 * Linda), adapted where GROK-17 changed the flow (foreground service,
 * onboarding). Kept as Kotlin constants so the JVM tests can assert them; a
 * later move to `strings.xml` for translation is mechanical.
 */
object Copy {
    // Onboarding
    const val ONBOARD_WELCOME_TITLE = "Welcome to Midnight Prover"
    const val ONBOARD_WELCOME = "Midnight Prover makes zero-knowledge proofs for Midnight with the official proof server. You paste a proving payload, the server proves it, and you copy the proof back."
    const val ONBOARD_WHERE_TITLE = "Where the server runs"
    const val ONBOARD_WHERE = "The proof server runs on this phone, inside Termux. Your private proof inputs never leave the phone. Running it on a phone is new and hasn't been measured yet."
    const val ONBOARD_TESTNET_TITLE = "Testnet only"
    const val ONBOARD_TESTNET = "Testnet only for now. Use payloads from Preview or Preprod (both testnets) or a local devnet. This app never signs anything and never sends anything to a network."
    const val ONBOARD_SETUP_TITLE = "Set up the server"
    const val ONBOARD_DATA_NOTE = "The first start downloads Midnight's public proving files. Use Wi-Fi."
    const val ONBOARD_KEEP_RUNNING = "Android may stop the server in the background. Exempt Termux from battery optimisation, and on Android 14 or later turn on Developer options → Disable child process restrictions."
    const val ONBOARD_CONNECT_TITLE = "Connect"
    const val ONBOARD_CHECK = "Check server"
    const val ONBOARD_OTHER = "Skip for now"

    // Header
    const val HEADER_SUBTITLE = "Make Midnight zero-knowledge proofs on this phone, with the official proof server."
    fun headerSubtitleRemote(host: String) = "Make Midnight zero-knowledge proofs with the official proof server on $host — a server off this phone."

    // Status
    const val STATUS_NOT_CHECKED = "Server not checked yet. Tap Check server."
    const val STATUS_CHECKING = "Checking the server…"
    const val STATUS_ONLINE = "Proof server online"
    const val STATUS_BUSY = "Proof server busy. It's working on other jobs; try again in a moment."
    const val STATUS_NOT_READY = "The server answered but isn't ready yet. It may still be loading its proving files; try again in a moment."
    fun statusUnreachable(url: String) = "Can't reach the proof server at $url. Check that it's running and the address is right."
    const val STATUS_TIMEOUT = "The proof server timed out. It accepted the connection but didn't finish answering in time."
    const val STATUS_UNENCRYPTED = "This server is not using https. A server off this phone must sit behind a TLS proxy; proof server 9.0.2 only speaks plain http."
    fun statusVersionMismatch(v: String) = "This server reports version $v. The app was tested with 9.0.2, so results may differ."
    fun chipProofs(list: String) = "Proofs $list"
    fun chipJobs(n: Int) = "Up to $n jobs"
    fun serverDetails(v: String, formats: String, ready: Boolean, n: Int?) =
        "Version $v · Proof formats $formats · Ready: ${if (ready) "yes" else "no"}" + (n?.let { " · Up to $it jobs at once" } ?: "")

    // Server tab
    const val SERVER_INTRO = "The app needs Midnight's official proof server running on this phone, inside Termux, reachable at http://127.0.0.1:6300."
    const val SERVER_URL_HINT = "Default: http://127.0.0.1:6300 (this phone)."
    const val URL_OFF_PHONE_DISABLED = "Only this phone (127.0.0.1, localhost or ::1) is allowed. To use a server off this phone, turn on \"Allow a server off this phone\" in Settings first."
    const val URL_OFF_PHONE_NEEDS_HTTPS = "Servers off this phone must use https://. Android only allows plain http to this phone (127.0.0.1, localhost or ::1)."
    const val OFF_PHONE_WARNING = "A server off this phone receives your private proof inputs, which can include wallet secret keys. Midnight recommends proving only on your own device. Use only a server you run yourself, over https."
    const val OPEN_PORT_NOTE = "Proof server 9.0.2 has no bind-address option and listens on every network interface, so other devices on the same Wi-Fi can reach port 6300. Use trusted networks and stop the server when you're done."
    const val TESTNET_CHIP = "Testnet only · Preview / Preprod"
    const val TESTNET_NOTE = "Testnet only for now: use payloads from Preview or Preprod (both testnets) or a local devnet. The proof server itself doesn't connect to any network."

    // Prove input
    const val INPUT_EMPTY = "Paste a proving payload first. It's the hex a wallet or SDK would send to the proof server."
    const val INPUT_ODD = "That payload looks cut off: hex needs an even number of characters."
    const val INPUT_NOT_HEX = "The payload should only contain 0–9 and a–f."
    const val PROVE_BUTTON = "Prove payload"
    const val QUEUE_FULL_PRECHECK = "The server's queue looks full, so this job may be turned away. You can still try."
    fun progress(t: String) = "Proving… $t so far. You can leave the app; a notification shows progress and lets you cancel."
    fun notificationTitle(host: String) = "Proving on $host"
    fun notificationBody(t: String) = "$t so far"
    const val CANCEL = "Cancel"

    // Done
    fun doneTitle(d: String) = "Proof returned in $d"
    const val DONE_CAPTION = "Timed by this app, network included. Not a benchmark."
    const val DONE_NOTE = "Proof ready. Copy the hex and give it to the tool that made the payload. This app doesn't send anything to a Midnight network."
    const val COPY_PROOF = "Copy proof (hex)"
    const val PROOF_COPIED = "Proof copied"

    // Errors
    const val ERR_UNREACHABLE = "Can't reach the proof server. Check that it's running, then try again."
    fun errTimeout(minutes: Int) = "Timed out: no answer from the server after $minutes minutes, so the app stopped waiting. The server's own job timeout is 600 s by default (MIDNIGHT_PROOF_SERVER_JOB_TIMEOUT). Check the server, then try again."
    const val ERR_QUEUE_FULL = "The server's queue is full. Wait a moment and try again."
    const val ERR_BUSY = "The server is busy. Try again in a moment."
    fun errRejected(msg: String) = "The server couldn't read this payload. Check that it's a complete proving payload. Server said: $msg"
    fun errHttp(code: Int, body: String) = "The server answered with an error (HTTP $code). Details: $body"
    const val ERR_CANCELLED = "Stopped waiting. The proof server may still finish this job in the background — 9.0.2 doesn't cancel a job when the app disconnects."
    const val ERR_INTERRUPTED = "Android closed the app while this proof was running. The payload is kept, so you can retry."
    const val ERR_CARD_TITLE = "Proof failed"

    // Activity
    const val ACTIVITY_EMPTY = "No proof jobs yet. Each job you run shows up here with its server, how long it took and how it ended. The app keeps your last 50 jobs."
    const val OUTCOME_PROVEN = "PROVEN"
    const val OUTCOME_FAILED = "FAILED"
    const val OUTCOME_CANCELLED = "CANCELLED"

    // About
    const val HONESTY_NOTE = "How it works: the app sends one proving payload to the server's /prove endpoint, the same per-circuit call Midnight.js uses today (the older /prove-tx is deprecated). No phone proving times are claimed yet. The first measurements will come from a supervised test on the developer's own phone."
    const val TIP_INTRO = "Support the dev — tips are welcome:"
    val FOOTER_INDEPENDENCE = "${Attribution.INDEPENDENCE} · ${Attribution.HANDLE}"

    /** Plain message for a failure kind (see [FailureInfo.kind]). */
    fun failureMessage(f: FailureInfo, timeoutMinutes: Int): String = when (f.kind) {
        "Unreachable" -> ERR_UNREACHABLE
        "Timeout" -> errTimeout(timeoutMinutes)
        "QueueFull" -> ERR_QUEUE_FULL
        "Busy" -> ERR_BUSY
        "Rejected" -> errRejected(f.message)
        "Http" -> f.message
        "Cancelled" -> ERR_CANCELLED
        "Interrupted" -> ERR_INTERRUPTED
        else -> f.message
    }

    /** `["V2", "V3"]` → `V2, V3`. */
    fun formatProofVersions(raw: String?): String =
        raw.orEmpty().trim().removePrefix("[").removeSuffix("]").split(',')
            .map { it.trim().trim('"') }.filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "?" }
}
