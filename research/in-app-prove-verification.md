# In-app prove verification — shipped Android prover (HERMES-7)

**Short answer: yes.** The shipped Android prover — the **release APK v0.7.1**, installed from the official GitHub release — completed a live in-app prove end to end against a **stock proof server 9.0.2** (no code changes, no forks, no patched server): `POST /prove` returned a 2.9 KB `dust/spend` proof in **1.1 s as timed by the app** (server-side `1.035221 s`). Everything below ran on an **Android emulator with WHPX hardware acceleration** — every timing is labelled **EMULATOR only; none of it is a phone figure**. No transaction was submitted anywhere, nothing touched a Midnight network, no wallet was involved.

## 1. What this verifies, and why

Board task HERMES-7 (claimed 2026-11-26, Kyle's order: Midnight first): run the **release** APK in a hardware-accelerated Android emulator against a live stock proof server 9.0.2 (repo installer, amd64), `adb reverse tcp:6300 tcp:6300`, import the team's 1,509-byte DUST test payload, prove it from inside the app, and land the evidence (proof-ready screenshot, server log lines, versions).

This closes the last unverified leg of the Android prover story. The pieces had each been verified separately — the live per-circuit `/prove` protocol end to end against a real 9.0.2 server (CLAUDE-AP-4), the no-Docker installer from scratch on amd64 (CLAUDE-AP-5), and the wallet-built-transaction E2E through the same server (GROK-12) — but the **assembled, signed, released app** had never completed a live prove: Muse's cloud sandbox has no `/dev/kvm`, and the Fold7 device run is Kyle-gated. This brief provides that missing leg: release build → emulator → stock server → proof returned and displayed in the app.

## 2. Version note (the board line said v0.5.1)

The board line pinned v0.5.1 because that was the current release when the task was written. After the claim, the board itself records three more shipped releases: v0.6.0 (privacy cloak, CLAUDE-AP-7), v0.7.0 (monochrome redesign, CLAUDE-AP-8), and v0.7.1 (true Midnight token symbol, CLAUDE-AP-8 fixup). This verification runs the **current release, v0.7.1** — and also records a same-environment v0.7.0 prove from the morning run (§5.5).

## 3. Environment (EMULATOR only — no phone)

| Piece | Value |
| --- | --- |
| Host | Hermes' Windows 11 Home dev machine (build 26200), 11th Gen Intel Core i9-11900KF |
| Hypervisor | **WHPX** (Windows Hypervisor Platform) — emulator log: `WHPX on Windows 10.0.26200 detected. Windows Hypervisor Platform accelerator is operational`; cold boot completed in **44,425 ms** |
| Emulator | Android Studio Emulator 36.6.11.13567682, AVD `hermes7`: Android 15 (API 35), `google_apis`, x86_64, 16 vCPU, 4096 MB RAM, 1080×2400 |
| App | **Release APK v0.7.1** from [GitHub Release v0.7.1](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/releases/tag/v0.7.1): `midnight-prover-v0.7.1.apk`, 8,931,980 B (matches the board's content-verified size), sha256 `9962a150250b95b81b4db40aef02ee530e8bf4332c445a92a4d9edc9cac05b46`; on-device `versionName=0.7.1 versionCode=10 minSdk=26 targetSdk=35`, package `tech.kshot.midnightprover` |
| Proof server | Official `midnight-proof-server` **9.0.2**, installed via the repo installer `apps/android-prover/server-setup/setup-proof-server.sh` (**amd64, no Docker** — pulls the official image and runs the staged binary, per CLAUDE-AP-5), in a WSL2 Linux environment on the same machine, `--port 6300`, job capacity 10; parameters (ledger 1.0.2) fetched and verified from `srs.midnight.network` at startup |
| Bridge | `adb reverse tcp:6300 tcp:6300` — the app talks to `http://127.0.0.1:6300`, which forwards into the WSL2 host where the server listens on 0.0.0.0:6300 |
| Payload | The app's bundled sample, `apps/android-prover/app/src/main/assets/sample-dust-spend-payload.hex` — 3,018 hex chars = **1,509 B**, the team's DUST-spend test payload from CLAUDE-AP-4. The app labels it on screen: *"captured on a local devnet, not testnet or mainnet"* |

**Label that governs every number in this brief: EMULATOR only.** WHPX-accelerated x86_64 emulation on a desktop i9 is not a phone; the server is CPU-only in this environment. The app itself refuses to overclaim ("Not a benchmark.", "This app makes no on-device speed or battery claims.").

## 4. Steps

1. Set up the emulator: Android SDK cmdline-tools + platform-tools under a scratch tree, AVD `hermes7` (API 35 / google_apis / x86_64), launched with WHPX acceleration (boot 44.4 s).
2. Install the stock proof server with the repo installer (amd64 path) into the WSL2 environment; start it with `--port 6300`. It emitted its own network warning at startup (reachable on all interfaces — quoted in the evidence file). Waited for `/version` = 9.0.2 and `/ready` ok.
3. `adb reverse tcp:6300 tcp:6300`.
4. Install the v0.7.1 release APK on the AVD (fresh install — see finding F1).
5. Launch the app; onboarding (5 steps, monochrome v0.7.1) renders correctly; "Skip" advances to the final step 5/5 "Connect", whose card says *"The setup is verified on amd64. No phone run has been measured yet."* — then "Start proving" enters the app.
6. The main screen (Prove tab) **auto-detected the server** — "Proof server online, http://127.0.0.1:6300, v9.0.2, Proofs V2, V3, Up to 10 jobs, Queue 0" with no manual check (finding F4).
7. New proof job → **Try sample** → the 1,509 B DUST-spend payload loaded into the hex field (Inspect step), "Prove payload" enabled.
8. Tap **Prove payload** → the app requested the Android notification permission on first prove (denied — finding F2); the prove ran; the result card appeared.
9. Captured: result card, Activity history, Server tab, and the server-side log lines.

## 5. Evidence

All screenshots are in [`research/evidence/hermes-7/`](evidence/hermes-7/); the server-side capture is [`server-log-excerpt.txt`](evidence/hermes-7/server-log-excerpt.txt).

### 5.1 Versions (live, this session)

- Server: `GET /version` → `9.0.2`; `GET /proof-versions` → `["V2", "V3"]`; `GET /ready` → `{"status":"ok","jobsProcessing":0,"jobsPending":0,"jobCapacity":10}`.
- App: `versionName=0.7.1`, `versionCode=10` (from the installed release APK; sha256 above).

### 5.2 The prove

Server log (the line this brief is about):

```
2026-11-26T19:36:45.345221Z  INFO actix_web::middleware::logger: 127.0.0.1 POST /prove HTTP/1.1; took 1.035221s
```

App result card (`04-prove-result-1.1s-v0.7.1.png`):

- **"Proof returned in 1.1 s"** — *"Timed by this app, network included. Not a benchmark."*
- Circuit: **dust/spend** · Proof size: **2.9 KB** · Format check: **Midnight proof header ✓**
- Verification: *"Not verified on this device — the network checks the proof when the transaction is submitted."*
- Server: `http://127.0.0.1:6300`
- *"Proof ready. Copy the hex and give it to the tool that made the payload. This app doesn't send anything to a Midnight network."*

The 2.9 KB matches the 2,940 B proof CLAUDE-AP-4 measured for the same payload on the same server version — independent corroboration across environments (her WHPX-less sandbox CPU vs this WHPX host; both CPU proving).

### 5.3 History and Server tab

- Activity (`05-activity-proven.png`): job entry **`midnight/dust/spend · 1:36 PM · 1.1 s · PROVEN · Midnight proof · 2.9 KB · http://127.0.0.1:6300`**; chips "1 proven / 0 not proven"; "Your last 50 proof jobs, kept on this device." 1:36 PM = 19:36 UTC — the same minute as the server log line.
- Server tab (`06-server-tab.png`): "Proof server online", chips `v9.0.2 / Proofs V2, V3 / Up to 10 jobs / Queue 0 / 54 ms`, summary "Version 9.0.2 · Proof formats V2, V3 · Ready: yes · Up to 10 jobs at once", URL field with "Default: http://127.0.0.1:6300 (this phone)." and the privacy line *"Your proof inputs stay on the device."*

### 5.4 Screenshots

| File | Shows |
| --- | --- |
| `01-onboarding-step-1-of-5.png` | v0.7.1 monochrome onboarding, step 1/5 (welcome) |
| `02-main-server-auto-online.png` | Main screen: server already online (auto-check), NEW PROOF JOB card (Paste / Import file / Try sample), Prove payload disabled |
| `03-sample-payload-loaded.png` | Sample DUST-spend payload loaded (hex visible, devnet-labelled), Inspect step active, Prove payload enabled |
| `04-prove-result-1.1s-v0.7.1.png` | **The result card: proof returned in 1.1 s, dust/spend, 2.9 KB, Midnight proof header ✓** |
| `05-activity-proven.png` | History: PROVEN entry with time, size, server |
| `06-server-tab.png` | Server tab: full version/endpoint/latency readout |
| `07-v0.7.0-prove-in-progress.png` | Morning run (v0.7.0): prove in flight, 2.1 s elapsed |
| `08-v0.7.0-prove-result-2.6s.png` | Morning run (v0.7.0): proof returned in 2.6 s, 2.9 KB, header ✓ |

### 5.5 The earlier v0.7.0 run (same environment, same morning)

Before the v0.7.1 run, an earlier session this morning proved the same sample payload on the **v0.7.0 release APK** in the identical environment: in-flight at 2.1 s (`07-…`), **"Proof returned in 2.6 s"**, dust/spend, 2.9 KB, Midnight proof header ✓ (`08-…`), server shown online at `http://127.0.0.1:6300` v9.0.2. Honest gap: that run's server-side log was overwritten when the server was restarted at 18:58 UTC before evidence collection — so the v0.7.0 leg is evidenced by app screenshots only, and the v0.7.1 leg above carries the complete app+server evidence set. The 2.6 s vs 1.1 s difference is **not** a benchmark (the app labels its timing as such; run-to-run variance on a CPU server).

## 6. New findings

- **F1 — Release signing key differs between v0.7.0 and v0.7.1.** Installing the v0.7.1 release APK over an installed v0.7.0 release APK fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package tech.kshot.midnightprover signatures do not match newer version`. Users upgrading across those two releases must uninstall + reinstall (onboarding re-runs; the persisted server URL/history are lost on that path). For Muse: check the CI release signing-key configuration so future upgrades are in-place.
- **F2 — First prove on a fresh install triggers the Android notification-permission dialog** (the foreground-service cancel notification from GROK-18). Denied, the prove completed normally — the app works without the permission.
- **F3 — Onboarding "Skip" jumps to step 5/5 ("Connect")**, not out of onboarding; "Start proving" then enters the app.
- **F4 — The main screen auto-checks the server on launch** (after `adb reverse`, no manual "Check server" needed) and polls `/version` + `/proof-versions` + `/ready` roughly every 15 s while visible (server log cadence).
- **F5 — Version drift is board-accurate**: v0.5.1 → v0.6.0 → v0.7.0 → v0.7.1 all ship through the same CI + release pipeline; this brief verified the head of that chain, v0.7.1.

## 7. What was NOT verified

- **No phone run, no on-device prove.** The Fold7 step remains Kyle-gated (CLAUDE-AP-6 waived it as a done-gate); on-device timings are unmeasured and unclaimed. Every number here is EMULATOR + WSL2-server.
- **No arm64 server install.** The installer ran the amd64 path (as the board line specified); the arm64 phone path (CLAUDE-AP-5) is unchanged by this brief.
- **No transaction submission, no network.** The proof was displayed and copied locally only; nothing was sent to Preview, Preprod or mainnet. The app says so on screen, and the run agrees.
- **No independent re-verification of the proof by a second verifier.** The app's own copy says the network checks the proof at submission; we did not submit, so no second-party verify was performed (the 2,940 B / 2.9 KB match with CLAUDE-AP-4's independently captured proof is a size match, not a cryptographic re-check).
- **No GPU in this environment** (WHPX CPU emulation host; CPU-only server), and **no battery/thermal measurements**.
- The v0.7.0 leg has no server-side log line (§5.5).

## 8. Environment state after this run

The proof server 9.0.2 is **left running** in the WSL2 environment (warm, for PR follow-ups) and the AVD is left booted. Stop the server with `pkill -f midnight-proof-server` inside that WSL2 environment; the server's own startup warning applies (port 6300 is reachable on the LAN — trusted networks only).

## Sources

| Source | Role |
| --- | --- |
| [GitHub Release v0.7.1](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/releases/tag/v0.7.1) | Release APK (asset size 8,931,980 B matches the board's content-verified figure; sha256 recorded above) — fetched live 2026-11-26 |
| `apps/android-prover/server-setup/setup-proof-server.sh` (this repo) | Stock server install (amd64, no Docker) — run live 2026-11-26 |
| `apps/android-prover/app/src/main/assets/sample-dust-spend-payload.hex` (this repo) | The 1,509 B sample payload proven in-app |
| Server log + live `/version`, `/proof-versions`, `/ready` (this machine) | `research/evidence/hermes-7/server-log-excerpt.txt` |
| Board entries CLAUDE-AP-4 / CLAUDE-AP-5 / GROK-12 | Prior leg verifications this brief completes (cross-referenced, not re-run) |
| App screens (this run) | `research/evidence/hermes-7/` screenshots 01–08 |

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
