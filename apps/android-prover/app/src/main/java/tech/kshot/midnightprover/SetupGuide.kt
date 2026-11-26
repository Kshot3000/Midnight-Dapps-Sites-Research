package tech.kshot.midnightprover

/**
 * The Route 3 server setup, as the app presents it. Every command is copied
 * from `server-setup/README.md`; `SetupGuideTest` fails if they drift.
 */
object SetupGuide {
    data class Step(val title: String, val body: String, val commands: List<String> = emptyList())

    const val SCRIPT_URL =
        "https://raw.githubusercontent.com/Kshot3000/Midnight-Dapps-Sites-Research/main/apps/android-prover/server-setup/setup-proof-server.sh"

    val steps = listOf(
        Step(
            "Install Termux",
            "Install Termux from F-Droid or GitHub (the Play Store build is outdated), then open it and set up a Debian environment.",
            listOf(
                "pkg update && pkg install proot-distro",
                "proot-distro install debian",
                "proot-distro login debian"
            )
        ),
        Step(
            "Install the proof server",
            "Inside Debian, fetch and run the setup script. It installs the official proof server 9.0.2 (arm64) without Docker. The first start downloads Midnight's public proving files from srs.midnight.network — use Wi-Fi.",
            listOf(
                "apt update && apt install -y curl python3 tar findutils procps",
                "curl -fsSLO $SCRIPT_URL",
                "chmod +x setup-proof-server.sh",
                "./setup-proof-server.sh --start"
            )
        ),
        Step(
            "Keep it running",
            "When it prints “proof server is LIVE: /version = 9.0.2”, leave that Termux session open. Later boots: run the generated launcher.",
            listOf("~/.midnight-proof-server/run-proof-server.sh")
        ),
        Step(
            "Connect this app",
            "The app looks for the server reachable at http://127.0.0.1:6300 on this phone (the server itself listens on every interface). Open the Server tab and tap Check server."
        )
    )

    /** Honest status line used in onboarding and About. */
    const val PHONE_STATUS =
        "The setup is verified on amd64. No phone run has been measured yet — the first arm64 install and timings will come from a supervised test on the developer's own phone. This app makes no on-device speed or battery claims."
}
