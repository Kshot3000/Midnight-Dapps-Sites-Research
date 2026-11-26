# GROK-14 — Android prover docs + copy-accuracy sweep (v0.3.1)

**Checked:** 2026-11-25
**Checked by:** Linda (Grok swarm, docs/content seat)
**Scope:** `apps/android-prover/` on `main` at `0668f38`: app versionName **0.3.1**, versionCode **4** (`app/build.gradle.kts:15-16`). This is the GROK-10 method applied to the Android prover. I checked every claim in both READMEs and every user-facing string in the app against the app code, the proof-server protocol it implements, the pin sheet ([`stack-versions.md`](stack-versions.md)), the CLAUDE-AP-1 verdict ([`android-prover-feasibility.md`](android-prover-feasibility.md)) and the GROK-11 survey ([`android-prover-landscape.md`](android-prover-landscape.md)).
**Networks:** no chain was touched. The proof server is network-agnostic. Where a network is named below, Preview (testnet) and Preprod (testnet) are public testnets, and `undeployed` is the local devnet.
**Rule for this PR:** every fix below is a proposal only. No app file, README, setup script, hub file or other seat's brief is edited. Victor folds the fixes and the proposed strings into GROK-15.
**Naming note:** "GROK-15" in this brief means Victor's Android prover polish and reliability task, as Kyle assigned it in the team room on 2026-11-25. On `main`, `council/board.md` (wave 7, PR #75) uses GROK-15 for the launch asset pack instead. Muse may want to renumber one of them.

## 1. Method and what "Matches" means

- **Matches:** the claim or string agrees with the code at the cited line and with the cited evidence.
- **Issue:** the claim is wrong, stale, unsourced, or the string overclaims, uses internal jargon or is unclear to a first-time user. Each Issue has a numbered fix in section 5.
- Files read in full: `README.md` (83 lines), `server-setup/README.md` (76 lines), `server-setup/setup-proof-server.sh`, `MainActivity.kt`, `Components.kt`, `ProverViewModel.kt`, `ProofServerClient.kt`, `ProofJob.kt`, `Theme.kt`, `ProofJobInputTest.kt`, `AndroidManifest.xml`, both Gradle files and `.github/workflows/android-prover.yml`.
- The app has **no `res/` folder and no `strings.xml`**. Every user-facing string is hard-coded Kotlin text, so string locations below are `.kt` file:line.
- Pins used as the reference (from `stack-versions.md`, ledger 9 on every network): Compact toolchain 0.36.1, Midnight.js 5.0.1, proof server 9.0.2 (`midnightntwrk/proof-server:9.0.2`, amd64 + arm64, port 6300, job capacity 10, queue full → HTTP 429), DApp Connector API 4.1.0, indexer 4.4.101, Wallet SDK 2.0.1. The app itself only names the proof server version. No Ledger 8-era material appears in the app, and none is used here as current.

## 2. Summary

- **Items checked:** 85: 37 README claims (23 in the app README, 14 in the server-setup README), 37 in-app strings and error classes, and 11 verdict and repo-wide checks.
- **Matches:** 49. **Issues:** 35 (some minor). **Flag only:** 1 (the hub gap).
- **Fixes for GROK-15:** 28, in section 5. Proposed replacement strings for every screen and state are in section 6.

Top issues:
1. The app README still says "v0.2 carries the full Midnight-themed design" and calls history "session" history, which v0.3 made persistent.
2. A busy server (`/ready` answering 503) is shown as **"Proof server unreachable"**, because `getText()` throws on any non-2xx before the busy body is parsed.
3. The in-app tip line (pre-0.4.0 string) said "donation addresses" and didn't show either address.
4. The header and main button say "Prove Midnight transactions on this device", but the app proves one per-circuit payload, and the server may be on a LAN or in the cloud.
5. `server-setup/README.md` step 3 sends users to the Server tab to see the cyan status hero, but the hero only exists on the Prove tab.

## 3. README checks

### 3a. `apps/android-prover/README.md`

| # | Item | File:line | Result | Evidence |
| --- | --- | --- | --- | --- |
| R1 | Title "v0.3.1" | README.md:1 | Matches | `app/build.gradle.kts:16` `versionName = "0.3.1"`, `:15` `versionCode = 4`. |
| R2 | "An Android client for proving Midnight transactions on the phone itself" | README.md:3 | Issue | The app proves **one per-circuit proving payload** per job (`ProofServerClient.kt` `prove()`), not a whole transaction. The phone (arm64) server run hasn't happened yet (README.md:67-68), and the URL can point off the phone (README.md:42-43). Fix 2. |
| R3 | Talks to official midnight-proof-server 9.0.2 | README.md:11 | Matches | `ProofJob.kt:35` `EXPECTED_SERVER_VERSION = "9.0.2"`; `stack-versions.md:29`. |
| R4 | `GET /version` — expect 9.0.2 | README.md:13 | Matches | `ProofServerClient.kt` `status()` reads `/version`; `MainActivity.kt:177-179` warns on a mismatch. |
| R5 | `GET /proof-versions` — `["V2","V3"]` | README.md:14 | Matches | `council/board.md` note of 2026-11-24 (live 9.0.2: `["V2","V3"]`); `proof-station-qa.md` item V8 cites the official local-proving page. The app shows the raw JSON (see C5). |
| R6 | `GET /ready` — readiness + job capacity | README.md:15 | Matches | `ReadyFields.parse` reads `status`, `jobsProcessing`, `jobsPending`, `jobCapacity`; `ready = status == "ok"`. |
| R7 | `POST /prove` with a raw binary body | README.md:16 | Matches | `ProofServerClient.kt` `prove()`: POST `/prove`, `Content-Type: application/octet-stream`, raw bytes from hex. |
| R8 | v0.1–v0.2 used `/prove-tx` with JSON; `/prove-tx` is deprecated and not used now | README.md:18-26 | Matches | No `/prove-tx` call in the code (only comments and the honesty note). Board row CLAUDE-AP-4. Independent: GROK-12 (`wallet-prover-e2e.md:61`) saw Midnight.js 5.0.1 call only `/check` + `/prove`, never `/prove-tx`. |
| R9 | Payload tuple and `midnight:proof-versioned:` header | README.md:22-25 | Matches | `ProofFormat.isProof` checks that exact ASCII header; KDoc at `ProofServerClient.kt:41-46`. |
| R10 | 1,509-byte DUST-spend payload proven in 2.56 s, 2,940-byte proof, "sandbox CPU — not a phone measurement" | README.md:27-30 | Issue | The numbers match board row CLAUDE-AP-4 and PR #69 (Muse's run, not reproduced by me), and the CPU label is honest. But the network of the "live wallet session" isn't stated. The repo rule is that every network is labelled. Fix 3. |
| R11 | Hex in the UI, copyable proofs, cancel | README.md:31-32 | Matches | `MainActivity.kt:444` "Copy proof (hex)"; `ProverViewModel.kt` `cancelProve()` drops the connection. |
| R12 | Failures classified: unreachable / 429 / 503 / 400 with server message | README.md:32-34 | Issue | True for `/prove` (`ProofServerClient.kt:23-31`, `:102-107`). But on **Check server**, `getText()` throws `Http` on any non-2xx (`ProofServerClient.kt:126`), so a `/ready` 503 "busy" (the shape the client's own KDoc at `:50` and the `parsesBusyShape` test describe) ends as `reachable = false` → "Proof server unreachable". A read timeout after 600 s (`:95`) is also caught as `IOException` and reported as "Could not reach the proof server". Fix 4, Fix 19. |
| R13 | Server URL + job history persist across launches | README.md:34-35 | Matches | `ProverViewModel.kt:45-78` SharedPreferences, history capped at 50 (`:67`, `:90`). |
| R14 | Route 3: official arm64 server in a Linux environment (e.g. Termux/proot Debian), app at `http://127.0.0.1:6300`, engine swappable | README.md:37-41 | Matches | `android-prover-feasibility.md:15`, `:21`; `ProofJob.kt:34` `DEFAULT_BASE_URL = "http://127.0.0.1:6300"`. |
| R15 | URL configurable for a LAN or cloud server | README.md:42-43 | Matches | Server tab URL field → `ProverViewModel.onBaseUrlChange`. Editing quirk noted in C16. |
| R16 | "Not yet benchmarked on a phone … claims none" | README.md:47-50 | Matches | `android-prover-feasibility.md:137`; no phone figures anywhere in the app. |
| R17 | Parameters fetched from `srs.midnight.network` on first boot | README.md:51-53 | Matches | GROK-12 evidence `research/evidence/grok-12/run1-failure.txt:23`; fixed repo-wide in #67. |
| R18 | "v0.2 carries the full Midnight-themed design (CLAUDE-AP-3)…" | README.md:54-59 | Issue | Stale: the app is v0.3.1 and the design is still in it. "Session job history" is also stale, because history has persisted since v0.3 (R13). The design list itself (three tabs, pulsing orb, starfield and crescent moon, elapsed-time state) matches `MainActivity.kt:70-74`, `Components.kt`. Fix 1. |
| R19 | Setup installs the arm64 server in Termux/proot, no Docker, registry pull + staged loader | README.md:63-66 | Matches | `setup-proof-server.sh:27-29`, `:54-127`. |
| R20 | Verified end-to-end on amd64 2026-11-25; arm64 phone run still the owner's supervised step | README.md:66-68 | Matches | Board row CLAUDE-AP-5, PR #71. Not reproduced by me. |
| R21 | CI builds every change, `gradle testDebugUnitTest assembleDebug`, APK uploaded as an artifact | README.md:72-74 | Matches | `.github/workflows/android-prover.yml:34-38`. CI runs only when `apps/android-prover/**` or the workflow changes (`:4-12`), so "every change" means every change to the app. |
| R22 | JDK 17, Gradle 8.9, compileSdk 35, minSdk 26 | README.md:74-75 | Issue (minor) | All four match (`app/build.gradle.kts:9`, `:13`, `:28-32`; workflow `:30`, `:33`). `targetSdk = 35` (`:14`) is missing, and it decides the Android 15 foreground-service rules GROK-15 will hit (`android-prover-landscape.md:28`, `:109`). Fix 5. |
| R23 | Attribution: both addresses, @kshot9000, independence line | README.md:79-83 | Matches | Byte-exact against `package.json` `attribution`. Wording is "Tips welcome". |

### 3b. `apps/android-prover/server-setup/README.md`

| # | Item | File:line | Result | Evidence |
| --- | --- | --- | --- | --- |
| S1 | Official 9.0.2 server at `http://127.0.0.1:6300`, no Docker, genuine binary via the glibc loader in its own image | server-setup/README.md:3-9 | Matches | `setup-proof-server.sh:19`, `:27-28`, `:117`, `:127`, `:139`. |
| S2 | Install Termux from F-Droid or GitHub; the Play Store build is outdated | server-setup/README.md:13-14 | Matches | Consistent with CLAUDE-AP-1: a developer setup, not a one-tap Play Store install (`android-prover-feasibility.md:21`, `:27`). I didn't re-check Termux's distribution pages today. |
| S3 | Termux: `pkg install proot-distro curl python3` | server-setup/README.md:17 | Issue (minor) | The script runs inside Debian (step 2), so the Termux-side `curl python3` aren't used. It's harmless but confusing. Fix 10. |
| S4 | Debian: `apt install -y curl python3 tar findutils procps`, then fetch the script from `main` | server-setup/README.md:25-28 | Issue | The packages cover what the script uses (`curl`, `python3`, `tar`, `find`). `procps` isn't used by the script. The `curl` URL tracks the moving `main` branch, so the steps a reader runs can change under them. Fix 9. |
| S5 | `--start` | server-setup/README.md:28 | Matches | `setup-proof-server.sh:37`, `:134-149` (detached via `setsid nohup`, polls `/version` for 60 s). |
| S6 | Defaults to arm64 | server-setup/README.md:31-32 | Matches | `setup-proof-server.sh:29`, `:43`. |
| S7 | First boot downloads parameters from `srs.midnight.network` "(~35 MB)" | server-setup/README.md:32-33 | Issue | No repo evidence gives 35 MB. My own HEAD requests today: `bls_midnight_2p10` … `2p15` (the k=10–15 sets from the board note) total **12,388,632 bytes (≈12.4 MB)**. The zswap/dust key files weren't measured. The total is unverified either way. Fix 7. |
| S8 | Prints `proof server is LIVE: /version = 9.0.2` | server-setup/README.md:33-34 | Matches | `setup-proof-server.sh:141`. |
| S9 | Server tab, URL already `http://127.0.0.1:6300`, tap **Check server**, status hero lights cyan "Proof server online · v9.0.2" | server-setup/README.md:37-39 | Issue | The Server tab has the URL and **Check server** (`MainActivity.kt:356-374`), but its result is a plain text line (`:376-385`). The cyan hero (`ServerStatusCard`) is only on the **Prove** tab (`:208`), and it shows "Proof server online" with a separate `v9.0.2` chip (`:160`, `:173`), not one string. Fix 6. |
| S10 | Desktop/server: `--arch amd64 --start`, point the app at the machine's LAN address | server-setup/README.md:43-48 | Matches | `setup-proof-server.sh:35`, `:44`. The URL needs `http://`; the app adds it if it's missing (`ProofJob.normaliseBaseUrl`). |
| S11 | amd64 verified 2026-11-25, 1,509-byte payload → 2,940-byte proof, "~3 s on a shared sandbox CPU" | server-setup/README.md:52-55 | Matches | PR #71 body. It's a different run from the app README's 2.56 s (PR #69); both are Muse's sandbox CPU and labelled as such. |
| S12 | arm64/phone: no phone run yet; exempt Termux from battery optimisation | server-setup/README.md:56-61 | Issue | The no-phone-run statement is honest. But it leaves out the bigger killer CLAUDE-AP-1 verified: since Android 12 the **phantom process killer** stops child processes like this server (`android-prover-feasibility.md:118`), and Samsung may pin Termux to little cores (`:142`, unverified on the Fold7). Fix 8. |
| S13 | Files: `--arch`, `--dir`, `--start`; launcher at `~/.midnight-proof-server/run-proof-server.sh` | server-setup/README.md:65-68 | Matches | `setup-proof-server.sh:13-15`, `:30`, `:122`. With `--dir`, the launcher lives in that directory instead. |
| S14 | Attribution block | server-setup/README.md:72-76 | Matches | Byte-exact addresses, @kshot9000, independence line. |

## 4. In-app copy and behaviour checks

| # | Item | File:line | Result | Evidence |
| --- | --- | --- | --- | --- |
| C1 | App label "Midnight Prover" | AndroidManifest.xml:5 | Matches | Same name in both READMEs. No launcher icon resource exists (no `res/`). |
| C2 | Header "Prove Midnight transactions on this device, against the official proof server." | MainActivity.kt:129 | Issue | Same overclaim as R2. With a LAN or cloud URL, nothing is proven on this device. Fix 11. |
| C3 | Status "Contacting server…" / "Proof server not checked yet" / "Proof server online" | MainActivity.kt:158-160 | Matches | Driven by `JobState.CHECKING` and `server`. |
| C4 | Status "Proof server unreachable" | MainActivity.kt:161 | Issue | Also shown for a busy server (R12). Fix 4. |
| C5 | Chips `v9.0.2` / `proofs ["V2","V3"]` / `capacity 10` | MainActivity.kt:173-175 | Issue (minor) | The `proofs` chip prints raw JSON from `/proof-versions`. Fix 18. |
| C6 | "Expected 9.0.2 — this server reports a different version." | MainActivity.kt:179 | Matches | Good, honest warning. Plainer copy is in section 6. |
| C7 | "New proof job", field label "Proving payload (hex)", placeholder | MainActivity.kt:210-215 | Matches | Accurate: the field takes one serialized proving payload. |
| C8 | Button "Prove transaction" | MainActivity.kt:226 | Issue | It proves one payload (one circuit), not a transaction. Fix 12. |
| C9 | Warning "Server queue looks full — the request may be refused with 429." | ProverViewModel.kt:144 | Matches | Pre-flight compare against `jobCapacity`; 429 per `stack-versions.md:29`. Plainer copy is in section 6. |
| C10 | Progress "Proving… X elapsed. Mobile proving can take a while — keep the app open." | MainActivity.kt:243-244 | Issue | "Keep the app open" is honest: there's no foreground service in the manifest, matching GROK-11's advice (`android-prover-landscape.md:28`). But "Mobile proving" is wrong when the server is remote, and "a while" hints at a timing claim. Fix 13. |
| C11 | "Cancel proof" | MainActivity.kt:250 | Matches | `cancelProve()` cancels the coroutine and disconnects. |
| C12 | Failure card "Job failed" + error message | MainActivity.kt:260 | Matches | Shows the classified message. The messages themselves are covered in D1-D6. |
| C13 | Activity empty state "No proof jobs yet this session…" | MainActivity.kt:304 | Issue | History persists across launches (R13), so "this session" is wrong. Fix 14. |
| C14 | History chips PROVEN / FAILED | MainActivity.kt:322 | Issue | A user cancel is recorded as `success = false` (`ProverViewModel.kt:174`) and shows as FAILED. Fix 15. |
| C15 | History time `HH:mm:ss`, no date | MainActivity.kt:309, :326 | Issue (minor) | Persisted history spans days, but the date isn't shown. Fix 16. |
| C16 | Server URL field "Proof server URL" | MainActivity.kt:359; ProverViewModel.kt:94-99 | Issue | The label is fine. But every keystroke runs `normaliseBaseUrl`, which prepends `http://` and strips a trailing `/`, so the field can't be cleared below `http://` and the server status resets on each key. This is from reading the code; I didn't run it. Fix 24. |
| C17 | Server tab intro "Route 3 (CLAUDE-AP-1 verdict): the official arm64 proof server runs in a Linux environment on this phone…" | MainActivity.kt:350-352 | Issue | "Route 3 (CLAUDE-AP-1 verdict)" is internal team jargon in a user-facing string, and "runs … on this phone" states as fact what is still unverified on a phone. Fix 17. |
| C18 | Server result "Version … · proof versions … · ready=true · job capacity …" | MainActivity.kt:378-379 | Issue (minor) | `ready=true` is developer notation. Fix 18. |
| C19 | "Unreachable: {detail}" and "Server unreachable: {detail}" | MainActivity.kt:381; ProverViewModel.kt:114 | Issue | Shows raw Java exception text (for example a `ConnectException` message) with no advice. Fix 19. |
| C20 | Setup text "Run the server on this phone…" | MainActivity.kt:387-394 | Issue | The content matches the setup README and its honesty is good ("No phone install has been measured yet"). But "~35 MB" is unsourced (S7), and "apps/android-prover/server-setup in the repo" gives no URL a user could open. Fix 7, Fix 28. |
| C21 | Tip line (pre-0.4.0 string) "Independent builder — … · @kshot9000 / Tips: see the project README for the Cardano and Midnight donation addresses." | MainActivity.kt:400-401 | Issue | The independence line and @kshot9000 are exact. The pre-0.4.0 string used "donation" and broke the repo's tip wording rule ("Tip" / "Support the dev"), and neither address is shown in the app, so a user can't tip without leaving it. Fix 20. |
| C22 | Result "Proven in {duration}" | MainActivity.kt:423 | Issue (minor) | This is wall-clock time measured by the app, including network time. It's fine as the user's own measurement, but it reads like a benchmark. Fix 22. |
| C23 | Result note "The server returned a proof. Copy it back to the wallet or tool that issued the payload." | ProverViewModel.kt:164 | Issue | No wallet in the repo's evidence takes a pasted proof. Midnight.js and the wallet SDK talk to a proof server directly (GROK-12). The string promises an integration nobody has shown. Fix 21. |
| C24 | "Copy proof (hex)" | MainActivity.kt:444 | Matches | Copies the full proof hex. |
| C25 | Honesty note: `/prove` per-circuit, `/prove-tx` deprecated, no phone timings, "supervised run on the owner's phone" | MainActivity.kt:464-468 | Issue (minor) | Accurate (R8, R16). "The owner's phone" means nothing to a public user. Fix 23. |
| C26 | "Proof cancelled." | ProverViewModel.kt:172 | Matches | Shown after a user cancel. |
| C27 | Tab labels Prove / Activity / Server with glyph icons ◆ ☰ ◉ | MainActivity.kt:70-74, :90-91 | Issue (minor) | The glyphs are `Text`, so TalkBack reads the symbol before the label. No `contentDescription` or `semantics` exists anywhere in the app. Fix 25. |
| C28 | Proof summary "Midnight proof · N bytes" / "Server response · N bytes (unexpected header)" | ProofServerClient.kt `ProofFormat.summarise` | Matches | Checks the real header. |
| C29 | Version mismatch only warns, doesn't block | MainActivity.kt:177-183 | Matches | Honest. Proving against another version is allowed and labelled. |

### 4a. Error classes

| # | Item | File:line | Result | Evidence |
| --- | --- | --- | --- | --- |
| D1 | Unreachable: "Could not reach the proof server: {detail}" | ProofServerClient.kt:23, :108 | Issue | Any `IOException` lands here, including the 600 s read timeout (`:95`). A slow prove that times out is reported as "could not reach". Fix 19. |
| D2 | 429: "The proof server's job queue is full (HTTP 429). Wait and try again." | ProofServerClient.kt:25 | Matches | `stack-versions.md:29` (queue full → 429). |
| D3 | 503: "The proof server reports it is busy (HTTP 503). Try again shortly." | ProofServerClient.kt:27 | Issue | Correct for `/prove`, but never reached on **Check server**: a `/ready` 503 throws `Http` at `:126` and becomes "unreachable" (R12). Fix 4. |
| D4 | 400: "Server rejected the payload: {server message}" | ProofServerClient.kt:29 | Matches | Carries the server's own parser message, truncated to 400 chars. |
| D5 | Other HTTP: "HTTP {code}: {body}" | ProofServerClient.kt:31 | Issue (minor) | Raw. There's no plain sentence, and the body is truncated to 300 chars. Fix 19. |
| D6 | Validation: "Paste a proving payload (hex) first." / "Hex must have an even number of characters." / "Proving payload must be hex (0-9, a-f)." | ProofJob.kt:14-17 | Matches | Matches `validationError()`; plainer copy is in section 6. |
| D7 | Code comment "Proving can take minutes on mobile hardware" | ProofServerClient.kt:94 | Issue (minor) | Not user-facing, but it's an unmeasured phone-timing claim the honesty rule forbids. Fix 27. |
| D8 | `/prove-tx` never called | ProofServerClient.kt (whole file) | Matches | Only `/version`, `/proof-versions`, `/ready`, `/prove`. |

### 4b. CLAUDE-AP-1 verdict and repo-wide checks

| # | Item | File:line | Result | Evidence |
| --- | --- | --- | --- | --- |
| V1 | No phone proof times or battery claims | whole app + READMEs | Matches | `android-prover-feasibility.md:137`. The only figures are labelled sandbox CPU (R10, S11). |
| V2 | No Play Store availability promised for the Linux-environment form | whole app + READMEs | Matches | `android-prover-feasibility.md:27`. "Play Store" appears only in S2, about Termux's outdated build. |
| V3 | No claim that the Linux Terminal exists on the Fold7 | whole app + READMEs | Matches | `android-prover-feasibility.md:27`, `:136`. Neither README mentions the Linux Terminal. |
| V4 | No React Native for the WASM route | app build | Matches | Kotlin + Jetpack Compose (`app/build.gradle.kts:1-5`); `android-prover-feasibility.md:28`. |
| V5 | Background proving not promised | AndroidManifest.xml | Matches | No service is declared; the copy says "keep the app open". This matches `android-prover-landscape.md:28`. When GROK-15 adds a foreground service, the progress copy in section 6 is written for that. |
| V6 | Testnet labels for Preview / Preprod | whole app | Issue | The app never names a network. That's fine for a network-agnostic prover, but the repo's testnet-only rule is never told to users, and R10 doesn't label its payload's network. Fix 26, Fix 3. |
| V7 | No Ledger 8-era versions presented as current | whole app + READMEs | Matches | Only proof server 9.0.2 and proof versions V2/V3 appear (ledger 9 line). |
| V8 | Tip wording: "Tip" / "Support the dev", never donation wording | READMEs + app | Issue | READMEs say "Tips welcome" (good); the pre-0.4.0 app string said "donation addresses" (C21). Fix 20. |
| V9 | Strings localisable / font-scalable | whole app | Issue | No `strings.xml`, so nothing is translatable or easy to review. Text uses `MaterialTheme.typography` (sp-based), so system font scaling should apply; I didn't verify it on a device. Fix 25. |
| V10 | Cleartext HTTP allowed | AndroidManifest.xml:6 | Matches | `usesCleartextTraffic="true"` is needed for `http://127.0.0.1:6300` and LAN URLs. Noted, no fix: narrowing it is a GROK-15 design choice. |
| V11 | Hub lists the app | `sites/hub/index.html` | Flag only | See section 7. Sol's hub page, not edited here. |

## 5. Fix list

All proposals only. Nothing below is applied in this PR. Victor applies them in GROK-15 (and keeps the CI APK build green).

1. **File:** `apps/android-prover/README.md` · **Line:** 54-59
   **Current:** "v0.2 carries the full Midnight-themed design (CLAUDE-AP-3): … three tabs (Prove / Activity / Server), a live server-status hero with pulsing orb, an elapsed-time proving state, and a session job history. Sol's improvement pass (SOL-AP-1) builds on this design when she returns."
   **Proposed:** "The app uses the Midnight-themed design (CLAUDE-AP-3, since v0.2): deep-space navy surfaces, indigo-to-violet brand gradient, cyan proof glow, an animated starfield with a crescent moon, three tabs (Prove / Activity / Server), a live server-status hero with a pulsing orb, an elapsed-time proving state, and a job history that is kept between launches (last 50 jobs). Sol's improvement pass (SOL-AP-1) builds on this design."
   **Evidence:** `app/build.gradle.kts:16` (0.3.1); `ProverViewModel.kt:45-78`, `:90` (persisted, capped at 50).
2. **File:** `apps/android-prover/README.md` · **Line:** 3
   **Current:** "An Android client for proving Midnight transactions on the phone itself."
   **Proposed:** "An Android client for Midnight's official proof server. It sends one proving payload at a time to the server and gets the proof back. The goal is to run that server on the phone itself; that phone run has not happened yet."
   **Evidence:** `ProofServerClient.kt` `prove()`; README.md:42-43, :67-68.
3. **File:** `apps/android-prover/README.md` · **Line:** 27-30
   **Current:** "a real 1,509-byte DUST-spend payload captured from a live wallet session was proven by the stock server in 2.56 s (sandbox CPU — not a phone measurement)"
   **Proposed:** Keep the numbers and add the network the wallet session ran on, for example "captured from a wallet session on Preprod (testnet)" or "on the local `undeployed` devnet". If the network wasn't recorded, say "network not recorded".
   **Evidence:** Board row CLAUDE-AP-4 and PR #69 don't name the network; `research/README.md` network-label rule.
4. **File:** `apps/android-prover/app/src/main/java/tech/kshot/midnightprover/ProofServerClient.kt` · **Line:** 126 (and `MainActivity.kt:161`, README.md:32-34)
   **Current:** `if (code !in 200..299) throw ProveException.Http(code, text)` — a `/ready` 503 makes the whole status "Proof server unreachable".
   **Proposed:** In `status()`, read `/ready` without throwing on 503, parse the body with `ReadyFields`, and return `reachable = true, ready = false`. Add a "busy" title: "Proof server busy". After that, README.md:32-34 is true as written.
   **Evidence:** KDoc at `ProofServerClient.kt:50` ("503 with status "busy" when the queue fills"); the `parsesBusyShape` unit test; `status()` catch block sets `reachable = false`.
5. **File:** `apps/android-prover/README.md` · **Line:** 74-75
   **Current:** "Locally: Android Studio, JDK 17, Gradle 8.9, compileSdk 35, minSdk 26."
   **Proposed:** "Locally: Android Studio, JDK 17, Gradle 8.9, compileSdk 35, targetSdk 35, minSdk 26."
   **Evidence:** `app/build.gradle.kts:14`; targetSdk decides the Android 15 foreground-service limits (`android-prover-landscape.md:109`).
6. **File:** `apps/android-prover/server-setup/README.md` · **Line:** 37-39
   **Current:** "Open **Midnight Prover**, go to the **Server** tab (the URL should already read `http://127.0.0.1:6300`), and tap **Check server**. The status hero should light up cyan: *Proof server online · v9.0.2*."
   **Proposed:** "Open **Midnight Prover**. On the **Prove** tab, tap **Check server** (the URL, shown under the status, should read `http://127.0.0.1:6300`; change it on the **Server** tab). The status card turns cyan and reads *Proof server online*, with a `v9.0.2` chip." Or, if GROK-15 adds the status card to the Server tab, keep the current step and drop the single-string quote.
   **Evidence:** `MainActivity.kt:208` (card only on Prove), `:160`, `:173`, `:376-385`.
7. **File:** `apps/android-prover/server-setup/README.md` · **Line:** 32-33 (and `MainActivity.kt:392`)
   **Current:** "(~35 MB)" / "First boot downloads ~35 MB of public parameters"
   **Proposed:** "First boot downloads the public proving parameters (the k=10–15 parameter files alone are about 12 MB; the full download, with keys, was not measured). Use Wi-Fi." Replace with a measured total once someone records one.
   **Evidence:** HEAD requests 2026-11-25: `bls_midnight_2p10`–`2p15` content-length sum 12,388,632 bytes; no repo source for 35 MB.
8. **File:** `apps/android-prover/server-setup/README.md` · **Line:** 56-61
   **Current:** "Android may also kill background Termux processes under battery pressure; exempt Termux from battery optimisation before long proving sessions."
   **Proposed:** "Android can stop the server while it runs. Since Android 12 the phantom process killer stops child processes like this one, and battery optimisation can too. Before a long session, exempt Termux from battery optimisation and, on Android 14 and later, turn on Developer options → Disable child process restrictions. Samsung phones may also slow Termux down; that is unverified on the Fold7."
   **Evidence:** `android-prover-feasibility.md:118`, `:142`.
9. **File:** `apps/android-prover/server-setup/README.md` · **Line:** 26
   **Current:** `curl -fsSLO https://raw.githubusercontent.com/Kshot3000/Midnight-Dapps-Sites-Research/main/apps/android-prover/server-setup/setup-proof-server.sh`
   **Proposed:** Point at a release tag or commit SHA instead of `main` (for example `…/android-prover-v0.3.1/…` once a tag exists), and say which app version the script goes with.
   **Evidence:** The URL tracks `main`; the app expects 9.0.2 (`ProofJob.kt:35`) and the script pins `TAG="9.0.2"` (`setup-proof-server.sh:28`). A moving script can drift from an installed app.
10. **File:** `apps/android-prover/server-setup/README.md` · **Line:** 17
    **Current:** `pkg update && pkg install proot-distro curl python3`
    **Proposed:** `pkg update && pkg install proot-distro` (curl and python3 are installed inside Debian in step 2).
    **Evidence:** The script runs inside Debian (step 2); nothing runs Termux-side curl or python3.
11. **File:** `apps/android-prover/app/src/main/java/tech/kshot/midnightprover/MainActivity.kt` · **Line:** 129
    **Current:** "Prove Midnight transactions on this device, against the official proof server."
    **Proposed:** "Make Midnight zero-knowledge proofs with the official proof server, on this phone or on a server you choose."
    **Evidence:** R2; the URL is configurable (README.md:42-43).
12. **File:** `MainActivity.kt` · **Line:** 226
    **Current:** "Prove transaction"
    **Proposed:** "Prove payload"
    **Evidence:** `ProofServerClient.prove()` proves one per-circuit payload.
13. **File:** `MainActivity.kt` · **Line:** 243-244
    **Current:** "Proving… {t} elapsed. Mobile proving can take a while — keep the app open."
    **Proposed:** "Proving… {t} so far. Keep the app open until the proof comes back." (With a foreground service in GROK-15: "Proving… {t} so far. You can leave the app; a notification shows progress and lets you cancel.")
    **Evidence:** No foreground service in `AndroidManifest.xml`; `android-prover-landscape.md:28`; no phone timing exists (`android-prover-feasibility.md:137`).
14. **File:** `MainActivity.kt` · **Line:** 304
    **Current:** "No proof jobs yet this session. Jobs you run appear here with their server, duration and outcome."
    **Proposed:** "No proof jobs yet. Each job you run shows up here with its server, how long it took and how it ended. The app keeps your last 50 jobs."
    **Evidence:** `ProverViewModel.kt:45-78`, `:90`.
15. **File:** `MainActivity.kt` · **Line:** 322 (and `ProverViewModel.kt:174`)
    **Current:** Chip "PROVEN" / "FAILED"; a cancel is recorded as `success = false`.
    **Proposed:** Add a third outcome, "CANCELLED", in a neutral colour, stored as its own state in history.
    **Evidence:** `ProverViewModel.kt:174` `record(false, startedAt, "Cancelled by user")`.
16. **File:** `MainActivity.kt` · **Line:** 309, 326
    **Current:** `SimpleDateFormat("HH:mm:ss", Locale.US)`
    **Proposed:** Show the date for jobs not from today (for example "Nov 25, 21:04"), with the device's locale and 12/24-hour setting.
    **Evidence:** History persists across days (Fix 14).
17. **File:** `MainActivity.kt` · **Line:** 350-352
    **Current:** "Route 3 (CLAUDE-AP-1 verdict): the official arm64 proof server runs in a Linux environment on this phone and is reached locally. Point the app anywhere else to use a LAN or cloud server instead."
    **Proposed:** "The app needs a Midnight proof server. The plan is to run the official server on this phone, inside Termux, at http://127.0.0.1:6300. You can also enter the address of a proof server on your network or in the cloud."
    **Evidence:** Internal label; phone run unverified (README.md:67-68).
18. **File:** `MainActivity.kt` · **Line:** 174, 378-379
    **Current:** Chip `proofs ["V2","V3"]`; text "Version 9.0.2 · proof versions ["V2","V3"] · ready=true · job capacity 10"
    **Proposed:** Chip "Proofs V2, V3"; text "Version 9.0.2 · Proof formats V2, V3 · Ready: yes · Up to 10 jobs at once" (strip the JSON brackets and quotes before display).
    **Evidence:** `ProofServerClient.status()` stores the raw `/proof-versions` body.
19. **File:** `ProofServerClient.kt` · **Line:** 23, 31, 95, 108 (and `MainActivity.kt:381`, `ProverViewModel.kt:114`)
    **Current:** Raw exception text ("Could not reach the proof server: {detail}", "Unreachable: {detail}", "HTTP {code}: {body}"); a read timeout is reported as unreachable.
    **Proposed:** Catch `SocketTimeoutException` separately as a new `TimedOut` class, and use the plain messages in section 6 (Unreachable, Timed out, Other HTTP). Keep the raw detail behind a "Details" expander.
    **Evidence:** `IOException` catch at `:108` covers timeouts; `readTimeout = 600_000` at `:95`.
20. **File:** `MainActivity.kt` · **Line:** 400-401
    **Current (pre-0.4.0 string):** "Tips: see the project README for the Cardano and Midnight donation addresses."
    **Proposed:** A "Support the dev" block with both addresses in full and a copy button for each:
    "Support the dev — tips are welcome:
    Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
    Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q"
    Keep the line "Independent builder — not affiliated with the Midnight Foundation or IOG. · @kshot9000" as is.
    **Evidence:** Repo tip wording rule; Kyle's GROK-15 scope ("Keep both addresses and the tip copy").
21. **File:** `ProverViewModel.kt` · **Line:** 164
    **Current:** "The server returned a proof. Copy it back to the wallet or tool that issued the payload."
    **Proposed:** "Proof ready. Copy the hex and give it to the tool that made the payload. This app doesn't send anything to a Midnight network."
    **Evidence:** No pasted-proof wallet flow in repo evidence; Midnight.js calls the proof server directly (`wallet-prover-e2e.md:61`).
22. **File:** `MainActivity.kt` · **Line:** 423
    **Current:** "Proven in {duration}"
    **Proposed:** "Proof returned in {duration}", with a small caption: "Timed by this app, network included. Not a benchmark."
    **Evidence:** `ProverViewModel.kt` measures `System.currentTimeMillis()` around the HTTP call.
23. **File:** `MainActivity.kt` · **Line:** 464-468
    **Current:** "… the first device measurements come from a supervised run on the owner's phone."
    **Proposed:** "… No phone proving times are claimed yet. The first measurements will come from a supervised test on the developer's own phone."
    **Evidence:** "Owner" is team-internal; `android-prover-feasibility.md:131-132`.
24. **File:** `ProverViewModel.kt` · **Line:** 94-99 (and `MainActivity.kt:356-368`)
    **Current:** `onBaseUrlChange` normalises and resets the server on every keystroke.
    **Proposed:** Keep raw text while typing; normalise, persist and recreate the client on "Done" or when the user taps **Check server**. Show a hint under the field: "For example http://127.0.0.1:6300 or http://192.168.1.20:6300".
    **Evidence:** `ProofJob.normaliseBaseUrl` prepends `http://` and trims `/`; the field `value` is the normalised state.
25. **File:** App-wide (new `app/src/main/res/values/strings.xml`; `MainActivity.kt:70-74`, `:90`)
    **Current:** All copy hard-coded in Kotlin; the tab glyphs are read aloud by TalkBack; no `contentDescription` anywhere.
    **Proposed:** Move every string in section 6 into `strings.xml`. Mark the glyph icons as decorative (`Modifier.clearAndSetSemantics {}`) so TalkBack reads only "Prove", "Activity", "Server". Add descriptions to the status orb ("Server online" / "Server unreachable" / "Not checked") and the copy buttons.
    **Evidence:** No `res/` folder in the app; no `semantics` or `contentDescription` in the sources.
26. **File:** `MainActivity.kt` (new line on the Server tab and in onboarding)
    **Current:** No network label anywhere.
    **Proposed:** "Testnet only for now: use payloads from Preview or Preprod (both testnets) or a local devnet. The proof server itself doesn't connect to any network."
    **Evidence:** Repo rule (testnet only; Preview/Preprod labelled testnet); the proof server is network-agnostic.
27. **File:** `ProofServerClient.kt` · **Line:** 94
    **Current:** `// Proving can take minutes on mobile hardware; keep the read window generous.`
    **Proposed:** `// Phone proving time is unmeasured; keep the read window generous (10 min).`
    **Evidence:** `android-prover-feasibility.md:137` (no Midnight phone numbers exist).
28. **File:** `MainActivity.kt` · **Line:** 387-394
    **Current:** "… with the project's setup script (apps/android-prover/server-setup in the repo — …)"
    **Proposed:** Replace the repo path with the full link https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/tree/main/apps/android-prover/server-setup and a copy button for the two Debian commands. Better still, move these steps into the first-run onboarding (section 6).
    **Evidence:** A repo path isn't something a phone user can open.

## 6. Proposed on-screen strings

For Victor to wire into GROK-15 (in `strings.xml`, Fix 25). `{…}` are placeholders. The app has no onboarding today, so those rows are new. Plain words, no phone timing numbers, testnet labels where a network comes up, and "Support the dev" for tips.

| Screen / state | Current string (file:line) | Proposed string | Reason |
| --- | --- | --- | --- |
| Onboarding 1 — welcome | (none) | "Midnight Prover makes zero-knowledge proofs for Midnight with the official proof server. You paste a proving payload, the server proves it, and you copy the proof back." | No onboarding exists; says plainly what the app does (R2). |
| Onboarding 2 — where the server runs | (none) | "The proof server can run on this phone, inside Termux, or on a computer you control. Running it on a phone is new and hasn't been measured yet." | Honest Route 3 framing without jargon (C17, V1). |
| Onboarding 3 — testnet | (none) | "Testnet only for now. Use payloads from Preview or Preprod (both testnets) or a local devnet. This app never signs anything and never sends anything to a network." | Testnet rule (V6, Fix 26). |
| Onboarding 4 — set up the server | (none) | "Set up the server: 1. Install Termux from F-Droid or GitHub. 2. In Termux run: pkg install proot-distro, then proot-distro install debian, then proot-distro login debian. 3. In Debian, run the setup script with --start. 4. When it says the server is live, come back and tap Check server." | Brings `server-setup/README.md` steps into the app (Fix 28, Fix 10). |
| Onboarding 4 — data note | `MainActivity.kt:392` "First boot downloads ~35 MB of public parameters" | "The first start downloads Midnight's public proving files. Use Wi-Fi." | 35 MB is unsourced (S7, Fix 7). |
| Onboarding 4 — keep it running | `server-setup/README.md:59-61` (battery note) | "Android may stop the server in the background. Exempt Termux from battery optimisation, and on Android 14 or later turn on Developer options → Disable child process restrictions." | Phantom process killer (S12, Fix 8). |
| Onboarding — finish button | (none) | "Check server" / "I'll use another server" | Two clear paths (local vs LAN/cloud). |
| Header subtitle | `MainActivity.kt:129` | "Make Midnight zero-knowledge proofs with the official proof server, on this phone or on a server you choose." | Overclaim (C2, Fix 11). |
| Status — not checked (empty) | `MainActivity.kt:159` "Proof server not checked yet" | "Server not checked yet. Tap Check server." | Adds the next step. |
| Status — checking | `MainActivity.kt:158` "Contacting server…" | "Checking the server…" | Plainer. |
| Status — online | `MainActivity.kt:160` "Proof server online" | "Proof server online" (keep) | Matches (C3). |
| Status — busy (new) | `MainActivity.kt:161` shows "Proof server unreachable" | "Proof server busy. It's working on other jobs; try again in a moment." | Busy ≠ unreachable (R12, Fix 4). |
| Status — unreachable | `MainActivity.kt:161`, `:381`; `ProverViewModel.kt:114` | "Can't reach the proof server at {url}. Check that it's running and the address is right." | Raw exception text (C19, Fix 19). |
| Status — version mismatch | `MainActivity.kt:179` | "This server reports version {v}. The app was tested with 9.0.2, so results may differ." | Plainer, same meaning (C6). |
| Status chips | `MainActivity.kt:173-175` | "v{version}" · "Proofs V2, V3" · "Up to {n} jobs" | Raw JSON (C5, Fix 18). |
| Server tab intro | `MainActivity.kt:350-352` | "The app needs a Midnight proof server. The plan is to run the official server on this phone, inside Termux, at http://127.0.0.1:6300. You can also enter the address of a proof server on your network or in the cloud." | Jargon (C17, Fix 17). |
| Server URL hint (new) | (none) | "For example http://127.0.0.1:6300 or http://192.168.1.20:6300" | Fix 24. |
| Server details line | `MainActivity.kt:378-379` | "Version {v} · Proof formats {list} · Ready: {yes/no} · Up to {n} jobs at once" | `ready=true` notation (C18). |
| Prove — empty input | `ProofJob.kt:14` "Paste a proving payload (hex) first." | "Paste a proving payload first. It's the hex a wallet or SDK would send to the proof server." | Explains what to paste. |
| Prove — odd hex | `ProofJob.kt:15` "Hex must have an even number of characters." | "That payload looks cut off: hex needs an even number of characters." | Plainer. |
| Prove — not hex | `ProofJob.kt:17` "Proving payload must be hex (0-9, a-f)." | "The payload should only contain 0–9 and a–f." | Plainer. |
| Prove — button | `MainActivity.kt:226` "Prove transaction" | "Prove payload" | One circuit, not a transaction (C8, Fix 12). |
| Prove — queue full (pre-check) | `ProverViewModel.kt:144` | "The server's queue looks full, so this job may be turned away. You can still try." | Plainer (C9). |
| Progress (no foreground service) | `MainActivity.kt:243-244` | "Proving… {t} so far. Keep the app open until the proof comes back." | No timing hint; honest about background (C10, Fix 13). |
| Progress (with GROK-15 foreground service) | (none) | Notification title "Proving on {host}" · body "{t} so far" · action "Cancel" | For the foreground service Kyle scoped. |
| Progress — cancel button | `MainActivity.kt:250` "Cancel proof" | "Cancel" (keep meaning) | Matches. |
| Done — title | `MainActivity.kt:423` "Proven in {duration}" | "Proof returned in {duration}" + caption "Timed by this app, network included. Not a benchmark." | Reads like a benchmark (C22, Fix 22). |
| Done — note | `ProverViewModel.kt:164` | "Proof ready. Copy the hex and give it to the tool that made the payload. This app doesn't send anything to a Midnight network." | No wallet paste flow shown (C23, Fix 21). |
| Done — copy button / confirmation | `MainActivity.kt:444` "Copy proof (hex)" | "Copy proof (hex)" + snackbar "Proof copied" | Adds feedback. |
| Error — unreachable | `ProofServerClient.kt:23` | "Can't reach the proof server. Check that it's running, then try again." | D1. |
| Error — timed out (new) | `ProofServerClient.kt:23` (via `:108`) | "No answer from the server after 10 minutes, so the app stopped waiting. The server may still be working; check it, then try again." | Timeout ≠ unreachable (D1, Fix 19). |
| Error — queue full 429 | `ProofServerClient.kt:25` | "The server's queue is full. Wait a moment and try again." | Plainer; drops the code from the headline (keep it in Details). |
| Error — busy 503 | `ProofServerClient.kt:27` | "The server is busy. Try again in a moment." | Plainer. |
| Error — rejected 400 | `ProofServerClient.kt:29` | "The server couldn't read this payload. Check that it's a complete proving payload. Server said: {message}" | Gives the likely cause. |
| Error — other HTTP | `ProofServerClient.kt:31` | "The server answered with an error (HTTP {code}). Details: {body}" | D5. |
| Error — cancelled | `ProverViewModel.kt:172` "Proof cancelled." | "Cancelled. Nothing was proven." | Clear outcome. |
| Error card title | `MainActivity.kt:260` "Job failed" | "Proof failed" | Matches the app's own word. |
| Activity — empty | `MainActivity.kt:304` | "No proof jobs yet. Each job you run shows up here with its server, how long it took and how it ended. The app keeps your last 50 jobs." | "This session" is wrong (C13, Fix 14). |
| Activity — outcome chips | `MainActivity.kt:322` "PROVEN" / "FAILED" | "PROVEN" / "FAILED" / "CANCELLED" | C14, Fix 15. |
| Honesty note | `MainActivity.kt:464-468` | "How it works: the app sends one proving payload to the server's /prove endpoint, the same per-circuit call Midnight.js uses today (the older /prove-tx is deprecated). No phone proving times are claimed yet. The first measurements will come from a supervised test on the developer's own phone." | "Owner" is internal (C25, Fix 23). |
| Footer — independence | `MainActivity.kt:400` | "Independent builder — not affiliated with the Midnight Foundation or IOG. · @kshot9000" (keep exactly) | Required line. |
| Footer — tip copy | `MainActivity.kt:401` (pre-0.4.0 string) "Tips: see the project README for the Cardano and Midnight donation addresses." | "Support the dev — tips are welcome:" followed by both full addresses, each with a Copy button (exact strings in Fix 20) | Tip wording rule; addresses shown in-app (C21, Fix 20). |

## 7. Hub gap (flag only)

`sites/hub/index.html` doesn't mention the Android prover: a case-insensitive search for "android" returns zero matches on `main` at `0668f38`. The hub is Sol's lane, so I'm **only flagging** this for her hub pass and Muse. I didn't edit the hub.

## NOT verified

- **No phone.** Nothing ran on any phone, including the Fold7. No proving time, battery, thermal or memory figure exists in this brief, and none should be read into it.
- **No emulator.** I didn't build the APK, install it or run it on an emulator. Every UI finding comes from reading the Compose source, including the URL-field behaviour (C16) and the busy-shown-as-unreachable path (R12/D3).
- **No proof server.** I didn't start a proof server, so I didn't run the setup script on arm64 or amd64 and didn't call `/version`, `/proof-versions`, `/ready` or `/prove`. The 2.56 s and ~3 s figures, the 1,509-byte payload and the 2,940-byte proof are Muse's (PRs #69 and #71), not reproduced. I didn't confirm that a real `/ready` returns 503 when busy; that comes from the app's own KDoc and unit test.
- **No CI run by me.** I didn't run the Gradle build or the Kotlin unit tests. CI status is from the board rows.
- **Parameter size:** only the six `bls_midnight_2p10`–`2p15` files were checked (HTTP HEAD content-length). The full first-boot download size is unknown.
- **Termux distribution** (F-Droid / GitHub / Play Store build age), the phantom-process setting name on current Android, and Samsung core pinning on the Fold7 were not re-checked today. They come from CLAUDE-AP-1.
- **Accessibility:** contrast ratios, TalkBack output, touch-target sizes, font scaling and reduced-motion handling were not measured.
- No chain, wallet, faucet, indexer or node was touched. Nothing was signed, and no sign-up or API key was used.

---

Built by @kshot9000. Support the dev — tips welcome:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
