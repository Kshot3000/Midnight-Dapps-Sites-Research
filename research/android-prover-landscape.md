# GROK-11 — Mobile ZK proving landscape (Android focus)

**Checked:** 2026-11-25 (America/Chicago), all pages opened between about 9:55 and 10:40 AM CT.
**Checked by:** Grok swarm (GROK-11, Bob Research).
**Scope:** Mobile zero-knowledge proving as it exists today: the Mopro toolkit, Midnight and Cardano mobile-proving efforts, published WASM-on-phone benchmarks, Rust-to-Android precedents, and the device and Play/OS rules that limit heavy on-device compute.
**Relation to CLAUDE-AP-1:** this is a survey. It does not repeat Muse's feasibility spike, [`android-prover-feasibility.md`](android-prover-feasibility.md), which covers the three routes (NDK, WebView WASM, and the arm64 proof server in Termux/AVF), the `blst` NDK build stop, the 16 KB page-size rule, the `wasm-proving-demos` PoC, React Native's missing `WebAssembly`, proot overhead and the phantom process killer. I cite those sections and don't restate them. **Two findings below update that spike's "nobody has done this" framing for Route 1. They are flagged for Muse under "What this changes for CLAUDE-AP-1 / SOL-AP-1" below.**

## Method and limits

- Every claim below comes from a page or repo I opened on 2026-11-25. I used WebFetch for web pages and `gh api` or shallow `git clone` for GitHub (READMEs, `Cargo.lock`, CI runs, releases, issues and PRs). I made one `curl` to Maven Central's public directory and `npm view` for one package.
- I did **not** install any app, connect a wallet, sign anything, request faucet tokens, create an account or API key, or run anything on a phone. **This brief has no phone measurements of my own.** Every number is someone else's published figure, labelled with its device and source, or an **estimate** labelled as one.
- **Network labels:** Preview and Preprod are **testnets**. Mainnet is production. `undeployed` is the local devnet.
- **Stack pins (current, ledger 9):** Compact toolchain **0.36.1**, Midnight.js **5.0.1**, Wallet SDK **2.0.1**, DApp Connector API **4.1.0**, proof server **9.0.2**, indexer **4.4.101** (see [`stack-versions.md`](stack-versions.md)). Any project pinned to `midnight-ledger` 8.x is marked **Ledger-8-stale**. That is reference only and is not evidence the project works on today's networks.
- **Status labels used:** **Live (store listing opened)**: a public store page exists and I read it. I did not install the app. **Alpha / testnet**: published, but labelled pre-release by its own authors. **Experimental (repo)**: source code only, with no published package I could open. **Announced / planning**: issue, roadmap or design text only. **Ledger-8-stale**: see above. **Unverified**: a claim I could not confirm without installing, signing or a device.

## Headline findings

1. **Mopro supports Circom (Groth16 over BN254 or BLS12-381), Halo2 and Noir (Barretenberg) on Android.** Its Halo2 adapter is a thin wrapper: you hand it your own `prove` and `verify` functions. That means Midnight's `midnight-proofs` could, in principle, sit behind it. Nobody has published that, and Mopro adds nothing Midnight-specific. Status: **Live (open-source toolkit, `mopro-ffi` v0.3.7, 2026-07-14)**.
2. **Native Midnight proving has already been compiled for `aarch64-linux-android` by two third parties.** This is the main update to CLAUDE-AP-1 Route 1:
   - **Kuira Android SDK** ships an on-device prover (`midnight-zkir` over JNI) on Maven Central as `0.1.0-alpha05`. Status: **Alpha / testnet, Ledger-8-stale** (`midnight-ledger` 8.0.3 in the lockfile).
   - **`webisoftSoftware/midnight-mobile`** (the 1AM team's React Native runtime, `@1am/midnight-mobile`) has a local `check` and `prove` path for Android and iOS. Status: **Experimental (repo)**. It links both `midnight-ledger` 8.1.0 and `midnight-ledger-v9` 1.0.0, and its "accept Ledger 9 contract proof responses" PR is still open.

   Neither project publishes an Android proving time.
3. **1AM Wallet is live on Google Play and the App Store, but its two listings disagree on where proving happens.** The iOS release notes for 6.3.12 (Oct 3) say "Generate Midnight proofs directly on your phone, with the option to use a remote proof server instead". The Google Play description says "ZK proving handed off to the 1AM proof service so your phone stays cool". Status: **Live (store listings opened)**. On-device proving on Android is **Unverified**.
4. **The only published Midnight-circuit proving times from a mobile-oriented SDK are not phone numbers.** webisoft's GPU-prover notes give 1.60 s on CPU and 1.14 s on the Metal GPU for a k=15 Zswap spend. Those runs were on an **Apple M4 Max** (a Mac). Their Android GPU status is `unsupported`, so Android proves on the CPU. Kuira's docs say outright that Midnight publishes no proving wall-clock or memory benchmarks.
5. **No published halo2-class proof time exists in a mobile browser or WebView.** Mopro's halo2 "Web" row (26.6 s, Keccak) names no device. Its phone-browser rows (Galaxy S23 Ultra, iPhone 16 Pro) are Circom/Groth16, not halo2. Android WebView gained a cross-origin-isolation allowlist for `SharedArrayBuffer` in `androidx.webkit` 1.18.0 (2026-10-21). That is the switch multithreaded WASM proving needs.
6. **On-device ZK on Android has precedent in Play-store apps outside crypto wallets:** identity apps (ZKPassport, Self, World ID, Privado ID, Rarimo, ZKProofport) per Mopro's project list, and Signal, whose Android app uses Rust `zkgroup` from libsignal. These are smaller or different proof systems than Midnight's (see §4).
7. **Device and policy reality:** mid-range phones still ship with 4 GB of RAM (Galaxy A16: 4–8 GB). Mopro reports a ~3 GB per-app ceiling on the phones it tested. Android caps `shortService` foreground services at about 3 minutes and `dataSync`/`mediaProcessing` at 6 h per 24 h (Android 15+). Long WorkManager jobs count against JobScheduler quotas from Android 16. Every foreground-service type must be declared and justified, with a video, in Play Console. **Practical upshot (my reading, not a rule):** prove while the app is in the foreground, with a visible progress UI. Don't plan on background proving.

---

## 1. Mopro (zkmopro, PSE ecosystem)

| Item | What I found | Source (opened 2026-11-25) |
| --- | --- | --- |
| What it is | "Mopro (Mobile Prover) is a toolkit for ZK app development on mobile." Uses UniFFI, flutter_rust_bridge and uniffi-bindgen-react-native to generate Swift, Kotlin, Flutter and React Native bindings around Rust "adapters". | [zkmopro.org/docs/intro](https://zkmopro.org/docs/intro); `zkmopro/mopro` README |
| Repo state | Not archived, last push 2026-10-19, 244 stars. Latest releases `mopro-ffi-v0.3.7` and `mopro-cli-v0.3.7` (2026-07-14). | `gh api repos/zkmopro/mopro` and its releases |
| Proving systems | Three official adapters: **Circom** (`mopro-ffi/circom`), **Halo2** (`mopro-ffi/halo2`), **Noir** (`mopro-ffi/noir`). Community custom adapters are listed for Binius, Spartan and Nova Scotia. | `docs/docs/adapters/overview.md` |
| Curves: Circom | The CLI's Circom template accepts proofs on **`CURVE_BN254` or `CURVE_BLS12_381`** and rejects others ("Unsupported curve"). Backends: arkworks (default) or rapidsnark. | `cli/src/template/init/src/circom.rs`; `docs/docs/adapters/circom.md` |
| Curves: Halo2 | The adapter supports "both the Halo2 library from Zcash and the PSE's Halo2 fork". It only fixes a function signature (`Halo2ProveFn`, `Halo2VerifyFn`), taking an SRS path, a proving-key path and inputs and returning bytes. The curve and commitment scheme are whatever your prove function uses. It can switch to `plonkish_backend`. | `docs/docs/adapters/halo2.md` |
| Curves: Noir | Barretenberg via the official `barretenberg-rs` crate, Noir `v1.0.0-beta.19`. Prebuilt binaries for `aarch64-linux-android` and `x86_64-linux-android` are marked ✅. (The Ethproofs CSP table lists barretenberg as UltraHonk over BN254 with KZG.) | `docs/docs/adapters/noir.md`; [ethproofs.org/csp-benchmarks](https://ethproofs.org/csp-benchmarks) |
| Android status | First-class. The build emits `MoproAndroidBindings/jniLibs/{arm64-v8a, armeabi-v7a, x86, x86_64}` plus `uniffi/mopro/mopro.kt`, and there is a written Android Studio guide. Also targets Web via `wasm-bindgen-rayon`, pinned to `nightly-2025-11-15`. | `docs/docs/setup/android-setup.md`; `mopro-ffi/src/app_config/constants.rs` |
| Published Android numbers | **Native Halo2, Keccak256:** Pixel 6 Pro 23.9 s prove / 0.51 s verify, against M1 Pro 10.3 s and iPhone 15 Pro 11.0 s. **Halo2 RSA:** crashes on the iPhone 15 Pro and Pixel 6 Pro. It needs "around 5GB of memory", and those phones "usually limit the application memory usage to 3GB". **Noir on a Pixel 6 (2021):** Keccak256 1,303 ms, zkEmail 4,757 ms, Anon Aadhaar 8,179 ms. **Circom on a Galaxy S23 Ultra:** rapidsnark Keccak256 743.7 ms. | `docs/docs/performance.md` (same content as zkmopro.org/docs/performance) |

**Does Midnight's stack fit Mopro?** This is my assessment, from the sources above plus `midnightntwrk/midnight-zk`'s README (opened 2026-11-25):

- Midnight's proof system is "Plonk proof system using KZG commitments" over **BLS12-381 and JubJub**. `proofs` "began as a fork of `halo2` v0.3.0" (PSE), but "our codebases have diverged" and the crates are "no longer maintained as forks". Changes include a generic `PolynomialCommitmentScheme` trait, a new transcript API, additive selectors, committed instances and 128-bit truncated challenges.
- So Mopro's **Halo2 adapter won't run Midnight circuits as-is**, because it expects PSE or Zcash halo2 circuits. The adapter's contract is still just "give me a prove fn that returns bytes", so a wrapper calling `midnight-zkir` / `midnight-proofs` fits the *shape*. In effect, Kuira and webisoft (§2) did this without Mopro, using their own JNI/FFI layers.
- **Fit: partial.** Mopro provides the Android packaging (UniFFI bindings, jniLibs layout, templates) and nothing Midnight-specific: no ZKIR, no Midnight key or SRS handling, no ledger transaction assembly. Its Circom BLS12-381 support is Groth16, a different proof system from Midnight's, so it doesn't help either.
- I found **no** Mopro + Midnight project. GitHub repo search for "mopro midnight" and "midnight-zk mopro" returned nothing on 2026-11-25.

## 2. Midnight and Cardano mobile-proving efforts

| Effort | Claims | Evidence I opened | Network | Status | Checked |
| --- | --- | --- | --- | --- | --- |
| **Kuira Android SDK** (Kuira Labs), [docs](https://kuiralabs.github.io/kuira-sdk-android/on-device-proving/), repo `kuiralabs/kuira-android-sdk`, FFI `nel349/kuira-midnight-ffi` | "Kuira generates zero-knowledge proofs on the phone by default. There is no proof server in the loop." `ProvingMode.LOCAL` is the default, with `REMOTE` as an option. Native `midnight-zkir` over JNI, "not the WebAssembly prover". Wallet circuits: Dust spend k=13, ZSwap output k=14, ZSwap spend k=15. Wallet proving keys plus BLS params total about 33 MB. | Docs page in full. Repo README and CHANGELOG (`0.1.0-alpha01` 2026-05-28: "On-device ZK proving … `arm64-v8a`"). FFI source has `zkir_prove_transaction_local` and links with `-Wl,-z,max-page-size=16384` (16 KB pages). `Cargo.lock` on both `main` (d97b691, 2026-07-22) and `development` (907a47c, 2026-10-27) pins **`midnight-ledger` 8.0.3**, `midnight-proofs` 0.7.0, `midnight-zkir` 2.1.0 and `blst` 0.3.16. Maven Central `io.github.kuiralabs:crypto` latest **0.1.0-alpha05** (metadata lastUpdated 2026-07-14). Docs warn that protocol keys download from a Midnight `-dev-` S3 bucket with "no production SLA". | Docs examples use `MidnightNetwork.PREPROD` (testnet) and localnet. | **Alpha / testnet, Ledger-8-stale.** The authors label it "Alpha … Production usage is not recommended." Whether it proves against today's ledger-9 Preprod is **Unverified**. No Android timings are published. The docs say "measure it" on your own device. | 2026-11-25 |
| **Kuira Vault** (`kuiralabs/kuira-vault-android`) | Android M-of-N multisig treasury dApp on the Kuira SDK. Its README says e2e runs "against a real node". | README | Not stated per run. | **Experimental (repo)**, inherits Kuira's Ledger-8 pin. | 2026-11-25 |
| **`@1am/midnight-mobile`** (`webisoftSoftware/midnight-mobile`) | "Experimental wallet runtime for React Native and Expo … not an official Midnight SDK." `@1am/midnight-mobile/local-prover` "runs the `check` and `prove` proof operations locally on iOS and Android". Android API 24+, `arm64-v8a` / `x86_64`. "Active local proof work cannot be cancelled." | README. `Cargo.lock` has `midnight-ledger` 8.1.0 **and** `midnight-ledger-v9` 1.0.0, plus `midnight-proofs` 0.7.3 and 0.8.3. Android NDK pinned to 27.1.12297006, with CI installing it. Merged PRs #13 "support ZKIR v3 in native proving" (2026-11-08) and #9 "converge the 1AM app runtime into the SDK". **Open** PR #14 "accept Ledger 9 contract proof responses". `npm view @1am/midnight-mobile` returned **404**, so it isn't published on npm. | README: network IDs `preview`, `preprod` (testnets) and `mainnet` are accepted, but "a valid network ID does not prove that a service is safe or compatible." | **Experimental (repo).** Ledger-9 support is in progress, not finished. | 2026-11-25 |
| **webisoft Metal GPU prover** (branch `feat/ios-metal-prover`, commit ec703a7, 2026-11-21 UTC) | Hooks GPU MSM and FFT into both linked `midnight-proofs` versions, following Charles Hoskinson's [midnight-zk#667](https://github.com/midnightntwrk/midnight-zk/pull/667). | `docs/gpu-proving.md` at that commit. Measurements are on an **Apple M4 Max** with 4 prover threads: Spend k=15 1.60 s CPU → 1.14 s GPU, Output k=14 1.1 s → 0.85 s. On Android the accelerator status "reports `unsupported`; proofs run on the CPU". midnight-zk#667 itself is an **open draft PR**, "Add an optional CUDA prover behind the gpu feature" (CUDA plus Metal backends). | n/a | **Experimental (unmerged branch).** The numbers are Mac numbers, **not phone numbers**. | 2026-11-25 |
| **1AM Wallet** (Webisoft) on [Google Play](https://play.google.com/store/apps/details?hl=en_US&id=com.webisoft.oneam) and the [App Store](https://apps.apple.com/us/app/1am-wallet/id6761093596) | Non-custodial Midnight wallet. iOS 6.3.12 notes (Oct 3): "Generate Midnight proofs directly on your phone, with the option to use a remote proof server instead." Play description: "ZK proving handed off to the 1AM proof service so your phone stays cool." | Both listings opened. Play: "Updated on Oct 4, 2026". App Store: version 6.3.22. The official [community wallets reference](https://docs.midnight.network/sdks/community/wallets/community-wallets-reference) lists 1AM's mobile app as "Yes (iOS, Android)". Its proving column reads "Yes (WASM; Halo2/BLS12-381)", but that matrix is labelled "as of June 2026". | Mainnet, Preview and Preprod (listing text). | **Live (store listings opened, not installed).** On-device proving on Android is **Unverified and contradicted by the Play text**. On iOS it is stated by the vendor, not verified by me. | 2026-11-25 |
| **`@dedanzi/midnight-mobile-sdk`** (`mzf11125/midnight-mobile-sdk`) | React Native SDK. Contract deploy and calls are "With ZK proof delegation". | README. Last push 2026-09-02. | "testnet" in its example | **Experimental (repo).** Not on-device proving. | 2026-11-25 |
| **`nel349/midnight-mobile`** | React Native bboard app. "Runtime Integration: Direct contract import needs additional setup for React Native compatibility." | README. Last push 2025-06-09. | none | **Stale demo** (pre-ledger-9). Not proving on device. | 2026-11-25 |
| **Official upstream: mobile** | midnight-ledger [#322](https://github.com/midnightntwrk/midnight-ledger/issues/322) "Mobile (React Native) Platform Support" is still **open**, last updated 2026-08-14 (Muse read it in full). midnight-wallet [#296](https://github.com/midnightntwrk/midnight-wallet/issues/296) "Dynamic Proof Backend Selection" is **open**: "client-side WASM for low-K circuits, remote TEE-based proof server for high-K", threshold "TBC". midnight-js #1213 (open, 2026-09-01) says no package declares a `browser` or `react-native` condition yet. | `gh api` on each issue | n/a | **Announced / planning.** | 2026-11-25 |
| **Midnight Passport** (`midnightntwrk/passport`) | Issue #12 "C6 · Proof generation" lists open questions including "Multi-platform support across iOS, Android, web" and the failure mode "Memory exhaustion on mobile devices for large circuits". Issue #102 weighs a PWA over native apps because of "Play Store reviews". | `gh api` on both issues | n/a | **Announced / planning.** | 2026-11-25 |
| **Lace** | The Lace **extension** 2.5.2 (2026-11-23) adds "Lace can generate proofs on your device for dApps that use newer contracts". The **Lace Android** 2.5.2 notes (2026-11-12) mention no Midnight proving. | `gh release view` for `lace-extension@2.5.2` and `lace-android@2.5.2` | n/a | **Desktop browser only.** No Midnight proving on Lace mobile found. (A web-search summary claimed Lace Android proves on-device. The release notes I opened don't say that.) | 2026-11-25 |
| **Cardano: zkFold Smart Wallet** | Activation proof comes from a remote `Prover("https://wallet-prover.zkfold.io")`. zkFold's Catalyst Fund 11 project is a "ZK-prover backend" offered as a service. | `zkFold/smart-wallet-api` README via `gh`; Catalyst page (search result only) | Cardano | **Server-side proving, not mobile.** No Cardano on-device proving app found. | 2026-11-25 |

## 3. WASM proving on phones (halo2-class)

- **Published halo2 mobile-browser numbers: none found.** Mopro's halo2 table has a "Web" row (Keccak256 26.6 s prove, 0.55 s verify; RSA "Not Support") but doesn't name the device or browser. Its named phone-browser rows (iPhone 16 Pro, Galaxy S23 Ultra, with `wasm-bindgen-rayon`) are **Circom/Groth16 only**. On the S23U, Keccak256 takes 2,733 ms and RSA 9,313 ms (`docs/docs/performance.md`, details in mopro#290).
- **Ethereum Foundation CSP benchmarks** (`ethereum/csp-benchmarks`, opened via `gh`, last push 2026-11-16) run on an AWS `mac2.metal` M1 host. The README says "Our future plans include running the benchmarks in a cloud device farm (Android and iOS mobile devices)". The repo's `mobile/` folder holds only `binius` and `plonky2`, with no halo2. [Ethproofs](https://ethproofs.org/csp-benchmarks) shows the same M1 host, last updated Nov 02, 2026. Its system table (15 provers) has **no halo2 or BLS12-381 entry at all**.
- **Midnight's own browser prover** is the `wasm-proving-demos` PoC that Muse read (CLAUDE-AP-1 Route 2). It proves a DUST spend in a desktop browser, with no phone figure.
- **WebView threading:** `androidx.webkit` **1.18.0 (October 21, 2026)** adds `Profile#setCrossOriginIsolatedAllowlist` "to allow the use of SharedArrayBuffer and other APIs that require cross-origin isolation". "Web pages must also opt in by setting the Document-Isolation-Policy header" ([release notes](https://developer.android.com/jetpack/androidx/releases/webkit)). Before this, a CKB developer reported that Android WebView "does not support `SharedArrayBuffer`, even with proper COOP/COEP headers" (nervosnetwork/ckb-light-client#245, open since 2025-10-10). This updates CLAUDE-AP-1's "fiddly" note on Route 2: a sanctioned switch now exists. Whether it works for the Midnight WASM prover is **Unverified**.

## 4. Rust-to-Android precedents (outside crypto wallets)

| Project | What it shows | Evidence (opened 2026-11-25) | Relevance |
| --- | --- | --- | --- |
| **Firefox for Android** (Mozilla, UniFFI) | Rust shipped in a top Play-store app through generated Kotlin bindings. Mopro uses the same UniFFI tool. | `mozilla/uniffi-rs` README: "used extensively by Mozilla in Firefox mobile and desktop browsers … called from … Kotlin (for Android apps)". | Packaging precedent only. No ZK. |
| **Signal Android** (libsignal) | Rust crypto, including **zero-knowledge** credentials (`zkgroup`, `zkcredential`, `poksho`), in a mass-market Android app. | `signalapp/libsignal` README and `rust/` listing. "Used by the Signal client apps (Android, iOS, and Desktop)". | ZK-on-Android precedent, but these are lightweight credential proofs, **not** large PLONK/KZG circuits. My characterization from the module descriptions. No timing data. |
| **ZKPassport** (identity) | A Play-store app that generates ZK proofs from passport NFC data on the phone. | Mopro's project list: "Generate a zero-knowledge proof on your phone from your passport credentials". [Play listing](https://play.google.com/store/apps/details?id=app.zkpassport.zkpassport&hl=en_US): "Works on your device - Everything happens locally on your phone", "Updated on Nov 22, 2026". | **Live (store listing opened).** Closest real-world precedent for consumer on-device SNARK proving on Android. The proof system is not stated on the listing. |
| **Mopro-listed Play apps:** World ID, Privado.iD, Rarimo, Self Protocol, ZKProofport | Mopro's project list links Play-store pages for each. ZKProofport is "built with Mopro and Noir for on-device proofs". | `docs/static/projects.json`. I did **not** open each Play page. | Listed by Mopro. Store status **Unverified** except ZKPassport. |

## 5. Device reality

**RAM: flagship vs mid-range**

| Device class | Example | RAM | Source |
| --- | --- | --- | --- |
| Team test device (flagship foldable) | Galaxy Z Fold7, Snapdragon 8 Elite for Galaxy | 12 GB (16 GB on the 1 TB model) | CLAUDE-AP-1 §3a (Muse, PhoneArena / Android Headlines) |
| Mid-range / budget (2024, still sold) | Galaxy A16, Helio G99 (2× A76 + 6× A55) | **4, 6 or 8 GB** | [GSMArena](https://www.gsmarena.com/samsung_galaxy_a16-13383.php): "4-8 GB RAM", price "$ 159.00" |
| Low-end floor | Android (Go edition) | Min RAM 2 GB for Android 13 Go | [developer.android.com/guide/topics/androidgo](https://developer.android.com/guide/topics/androidgo) |
| Per-app ceiling seen by a mobile prover | iPhone 15 Pro, Pixel 6 Pro | "usually limits the application memory usage to 3GB" | Mopro performance doc (Halo2 RSA crash note) |

- Android sets "a hard limit on the heap size for each app" that "varies between devices". Under memory pressure the system "kills processes in the cache" and "accounts for processes that hold onto the most memory". A native allocation larger than free memory under pressure gives a `SIGABRT` ([Overview of memory management](https://developer.android.com/topic/performance/memory-overview)).
- **Estimate (not a measurement):** Kuira's docs say prover memory "grows roughly linearly in 2^k" and that wallet circuits top out at k=15 with about 33 MB of keys. Mopro's ~5 GB crash case was an RSA circuit on an older halo2. So wallet-sized Midnight circuits (k≤15) *probably* fit within a 3 GB ceiling, and large contract circuits (k≥17 or so) are where mid-range 4 GB phones would likely fail first. **No one has published peak RSS for a Midnight proof on any phone.**

**Thermals**

- Android's Thermal API: "Devices can only maintain a high level of performance for a limited amount of time before being thermally throttled". `getThermalHeadroom()` "predicts how long the device can maintain the current performance level without overheating". Don't poll it more than once every 10 s, or it returns `NaN` ([Thermal API](https://developer.android.com/games/optimize/adpf/thermal)). A prover UI can use it to pace back-to-back proofs. That use is my suggestion. The docs target games.
- No sustained-proving thermal curve for any Midnight circuit on any phone is published. Mopro's numbers are single runs ("fastest result from multiple runs" for Noir), not sustained-load numbers.

**OS and store policy on heavy compute**

| Rule | What it says | Source (opened 2026-11-25) |
| --- | --- | --- |
| FGS types mandatory | From Android 14 (API 34), "you must declare an appropriate service type for each foreground service". | [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types) |
| `shortService` | "Can only run for a short period of time (about 3 minutes)". Overrunning gives an ANR "even if it has other valid foreground services". | same page |
| `dataSync` / `mediaProcessing` timeout | Apps targeting Android 15+: "a total of 6 hours in a 24-hour period", then `Service.onTimeout()`. Bringing the app to the foreground resets the timer. | [FGS timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout) |
| `specialUse` | "Covers any valid foreground service use cases that aren't covered by the other … types". The use case is free text and "reviewed when you submit your app in the Google Play Console". | Service types page |
| Play Console declaration | For each FGS type you must describe the feature, the "user impact if … deferred … [or] interrupted", and "Include a link to a video demonstrating each foreground service feature". | [Play Console Help 13392821](https://support.google.com/googleplay/android-developer/answer/13392821) |
| WorkManager long-running | Long-running workers "can run longer than 10 minutes" by running a foreground service. "Starting with Android 16, long running workers … can exhaust your app's job quota". Must specify an FGS type when targeting Android 14+. | [Support for long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running) (page "Last updated 2026-11-16 UTC") |
| 16 KB pages, phantom process killer, Samsung little-core pinning | Already covered in CLAUDE-AP-1 (Route 1 packaging caveat, Route 3b). Kuira's build already passes `max-page-size=16384`. | CLAUDE-AP-1; Kuira `CMakeLists.txt` |

**My reading (an inference, not policy text):** no FGS type is named for "CPU-heavy cryptography". A proof that takes seconds to under 3 minutes, started by a user tap while the app is visible, fits normal foreground execution or `shortService`. Anything longer needs a `specialUse` justification that Play reviews by hand. Background or batched proving is the hardest case for policy.

---

## What this changes for CLAUDE-AP-1 / SOL-AP-1 (suggestions for Muse, not edits)

1. **Route 1 is no longer "unbuilt by anyone."** Upstream still has no Android CI (CLAUDE-AP-1 is right about midnight-ledger and midnight-zk). But Kuira (alpha, Ledger-8-stale) and webisoft (experimental, ledger-9 work in progress) both cross-compile `midnight-zkir` / `midnight-proofs` + `blst` for `aarch64-linux-android` with the NDK (r26+ and r27.1 respectively), and both expose an on-device `prove`. CLAUDE-AP-1 says of phone proof times: "None exist anywhere — not in this brief, not upstream, not in third-party Midnight work I could find". On *times* that still holds. On *existence of a native Android prover* it should now cite these two projects.
2. **A cheap first phone measurement may come from these projects, not from Termux.** Kuira's docs say its native prover "logs its own wall-clock duration per proof". webisoft ships `tools/circuit-size-bench`. Either is Ledger-8-era today, so a number from them is a **proxy** for ledger-9 circuits. That's still better than none. Whether to try one is Kyle's call (see For Kyle).
3. **Route 2 has a new sanctioned lever.** `androidx.webkit` 1.18.0's cross-origin-isolation allowlist addresses the `SharedArrayBuffer` blocker for a multithreaded WASM prover in a WebView.
4. **Don't cite 1AM as an Android on-device prover.** Its own Play listing says proving goes to the 1AM proof service.

## Gaps — what nobody has published yet

- **Any Midnight proof time on any Android phone.** Not from upstream, Kuira, webisoft/1AM, or anyone else I could find. The only Midnight-circuit times from a mobile SDK are webisoft's Apple M4 Max numbers.
- **Peak memory (RSS) for any Midnight circuit on a phone,** at any k.
- **Sustained or thermal behaviour** for repeated Midnight proofs on any phone (throttle curve, battery per proof).
- **Any halo2-class (PLONK/KZG over BLS12-381) proof time in a mobile browser or Android WebView.** Mopro's halo2 "Web" row names no device. Its named phone-browser rows are Groth16.
- **A ledger-9 native Android prover with a release.** Kuira is pinned to `midnight-ledger` 8.0.3. webisoft's Ledger 9 proof-response PR is open, and its package isn't on npm.
- **Mid-range numbers for Midnight circuits** on 4–6 GB phones (Galaxy A-class, Helio G99-class).
- **An official Midnight position on mobile proving thresholds.** midnight-wallet #296 leaves the low-K/high-K routing threshold "TBC".
- **Android GPU acceleration for Midnight.** webisoft reports Android GPU as `unsupported`, and midnight-zk#667 covers CUDA and Metal only, as an open draft.
- **A Cardano on-device proving app.** zkFold's documented path is a hosted prover.

## Not verified

- That Kuira's `LOCAL` mode actually proves successfully on a real phone today, or against ledger-9 Preprod. I read the docs and code, but didn't run anything. The Ledger-8 lockfile suggests it would not match current networks without an update.
- That `@1am/midnight-mobile`'s local prover works on Android hardware, or whether the shipping 1AM Android app uses it. The Play listing says it doesn't ("handed off to the 1AM proof service").
- 1AM iOS on-device proving: vendor release-note claim only.
- Play-store status of World ID, Privado.iD, Rarimo, Self and ZKProofport. I only took them from Mopro's list and didn't open those listings. ZKPassport's proof system: the listing doesn't name it.
- The Hyli Labs post "Benchmarking in-browser p256 ECDSA proving systems" (Noir on a Galaxy A23, halo2 failing at runtime). It surfaced in search, but the page failed to load (HTTP 500 via WebFetch, TLS error via curl), so **none of its figures are used here**.
- Whether Android's per-app limit on the Galaxy Z Fold7 matches Mopro's "3GB" observation. That figure was for an iPhone 15 Pro and Pixel 6 Pro.
- The Galaxy A16 RAM and price come from GSMArena, a third-party spec site, not Samsung.
- Whether `androidx.webkit` 1.18.0's allowlist is supported by the WebView version on the team's device (`WebViewFeature` check needed on-device).

## For Kyle

- **Optional on-device data point (needs your phone, your call):** install a debug build of the Kuira Starter or BBoard example, or the webisoft Expo example, on the Fold7 and read the prover's own logged proof time for a Dust spend (k=13) or ZSwap spend (k=15). It's Ledger-8-era, so it's a proxy only. It needs Android Studio and USB debugging on your device, and possibly a localnet. No wallet funds or mainnet are needed.
- **Optional:** check whether the 1AM Android app has an on-device proving setting. That needs installing the app, which I didn't do. Don't import a funded seed.
- Nothing here needs an API key, sign-up, email or spending.

## Sources (all opened 2026-11-25)

- Mopro: [zkmopro.org/docs/intro](https://zkmopro.org/docs/intro). `zkmopro/mopro` via `gh api`: README, releases, `docs/docs/adapters/{overview,circom,halo2,noir}.md`, `docs/docs/performance.md`, `docs/docs/setup/android-setup.md`, `docs/static/projects.json`, `mopro-ffi/src/app_config/constants.rs`, `cli/src/template/init/src/circom.rs`.
- Midnight ZK: `midnightntwrk/midnight-zk` README and `proofs/README.md`. [midnight-zk#667](https://github.com/midnightntwrk/midnight-zk/pull/667) (open draft). midnight-ledger #322, midnight-wallet #296, midnight-js #1213, passport #12 and #102 (all via `gh api`).
- Official docs: [Community wallets reference](https://docs.midnight.network/sdks/community/wallets/community-wallets-reference).
- Kuira: [On-device proving docs](https://kuiralabs.github.io/kuira-sdk-android/on-device-proving/). `kuiralabs/kuira-android-sdk` (README, CHANGELOG, INTEGRATION.md, branches, Actions runs). `nel349/kuira-midnight-ffi` (`Cargo.lock` on `main` and `development`, `CMakeLists.txt`, `src/prove_ffi.rs`). `kuiralabs/kuira-vault-android` README. Maven Central `https://repo1.maven.org/maven2/io/github/kuiralabs/`.
- 1AM / webisoft: `webisoftSoftware/midnight-mobile` (README, `Cargo.lock`, `scripts/native/native-build-config.json`, `.github/actions/setup-native/action.yml`, PR list), `docs/gpu-proving.md` at ec703a7. `npm view @1am/midnight-mobile` (404). [Google Play listing](https://play.google.com/store/apps/details?hl=en_US&id=com.webisoft.oneam). [App Store listing](https://apps.apple.com/us/app/1am-wallet/id6761093596).
- Other Midnight mobile repos: `mzf11125/midnight-mobile-sdk`, `nel349/midnight-mobile` (READMEs).
- Lace: GitHub releases `lace-extension@2.5.2`, `lace-android@2.5.2` (`input-output-hk/lace`).
- Cardano: `zkFold/smart-wallet-api` README.
- Benchmarks: `ethereum/csp-benchmarks` README and `mobile/` listing. [ethproofs.org/csp-benchmarks](https://ethproofs.org/csp-benchmarks).
- WebView: [androidx.webkit release notes](https://developer.android.com/jetpack/androidx/releases/webkit). nervosnetwork/ckb-light-client#245.
- Precedents: `mozilla/uniffi-rs` README, `signalapp/libsignal` README, [ZKPassport on Google Play](https://play.google.com/store/apps/details?id=app.zkpassport.zkpassport&hl=en_US).
- Devices and policy: [GSMArena Galaxy A16](https://www.gsmarena.com/samsung_galaxy_a16-13383.php), [Android (Go edition)](https://developer.android.com/guide/topics/androidgo), [Memory overview](https://developer.android.com/topic/performance/memory-overview), [Thermal API](https://developer.android.com/games/optimize/adpf/thermal), [FGS service types](https://developer.android.com/develop/background-work/services/fgs/service-types), [FGS timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout), [Long-running workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running), [Play Console FGS requirements](https://support.google.com/googleplay/android-developer/answer/13392821).
- Cross-referenced team briefs: [`android-prover-feasibility.md`](android-prover-feasibility.md) (CLAUDE-AP-1), [`stack-versions.md`](stack-versions.md) (HERMES-1), [`ecosystem-directory.md`](ecosystem-directory.md) (GROK-1, already lists 1AM and Kuira).

---

Support — both addresses, character for character, alongside @kshot9000:

@kshot9000

Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v

Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
