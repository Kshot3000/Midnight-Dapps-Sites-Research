# CLAUDE-AP-1 — Android ZK Prover Feasibility Spike

**Question:** Can a phone prove Midnight transactions on-device — no server trusted with witness data — and if so, by which route? This brief tests the three candidate routes as far as a cloud workspace allows and gives SOL-AP-1 (the app design) a verdict it can honestly design against.

**Checked:** 2026-11-24. **Checked by:** Muse (guide seat), in Muse's own workspace (x86_64 Linux VM — no Android hardware, no GPU).
**Method:** upstream repos and CI read via the GitHub API; official docs and forum opened; Docker Hub registry API queried directly; one real build attempt run locally (Route 1). Every claim below is labelled **Verified** (I opened the source or ran the command), **Reported** (a secondary source I opened but did not independently confirm), or **PoC** (upstream's own label).
**Stack pins (unchanged):** Compact 0.36.1 / Midnight.js 5.0.1 / proof server 9.0.2 (ledger 1.0.2) / node 2.1.101 / indexer 4.4.101 — see [`stack-versions.md`](stack-versions.md).

**No phone was touched. This brief contains zero phone measurements, and any number in it is either a desktop number from this repo's own briefs or a third-party non-Midnight benchmark, labelled as such.**

---

## Verdict (for SOL-AP-1)

**Design v1 against Route 3 — the official arm64 proof server 9.0.2 running in an Android Linux environment, reached over `localhost:6300` through the standard proof-provider interface.** It is the only route whose prover is the actual shipping, already-verified code: the same binary this team runs on desktop (Proof Station) and that Muse runs in its workspace, and an official arm64 build already exists. Routes 1 and 2 both depend on infrastructure upstream itself lists as not built (native bindings/CI) or labels a proof of concept (the in-browser prover).

Design the app so the proving engine sits behind that localhost interface and can be swapped later: Route 2 (WASM prover in a WebView) is upstream's own pragmatic direction and the likely product form; Route 1 (native NDK) is upstream's stated long-term answer and the performance ceiling. Neither is designable as a promise today.

**What the design may honestly promise:**

- On-device proving of the built-in shielded (zswap) and DUST circuits and small contract circuits, in a *developer/enthusiast setup* (Termux/proot, or the Android Linux Terminal where a device offers it) — not a one-tap Play Store install.
- A prove flow with progress, cancel, and patient copy: desktop reference points are ~0.50 s warm for a k=6 circuit (HERMES-3, i9 desktop) and 1.168 s cold / 0.766 s warm at k=16 (Hermes' 3090-host run, PR #4 comment) — phone times will be slower and are **unmeasured**.

**What the design must not promise:**

- Any phone proof time, battery figure, or thermal behaviour. None exist anywhere — not in this brief, not upstream, not in third-party Midnight work I could find.
- Play Store availability for the Linux-environment form, or availability of Android's Linux Terminal on the team's Galaxy Z Fold7 (reported Exynos-only on Samsung; the Fold7 is Snapdragon — §3).
- React Native as the app shell for the WASM route: React Native's Hermes engine has no WebAssembly at all (upstream issue #322, §2).

---

## Route 1 — Native Rust via the Android NDK

**Idea:** compile Midnight's Rust proving crates for `aarch64-linux-android` and ship them in the app (JNI/uniffi), the way Mopro and other mobile-ZK toolkits ship native provers.

### Upstream status — **Verified**

- `midnightntwrk/midnight-ledger` (read via GitHub API, 2026-11-24) is a Cargo workspace: `ledger`, `proof-server`, `zkir`, `zkir-wasm`, `ledger-wasm`, `onchain-runtime`, `onchain-runtime-wasm`, and more. A code search for "android" across the repo returns only two things: CI lines deleting the GitHub runner's preinstalled Android SDK to free disk space, and `android_system_properties` as a transitive entry in `Cargo.lock`. **No Android build, test, or release job exists in its workflows.**
- `midnightntwrk/midnight-zk` (active, not archived; last push 2026-11-24) holds the proving core: `midnight-proofs` (the halo2 prover), `midnight-curves`, `midnight-circuits`, `midnight-aggregation`, `zk_stdlib`. No `android` or `aarch64` mention anywhere in its workflows. (Correction to this task's framing: midnight-zk has **not** moved to LFDT-Minokawa — that was the Compact repo, as HERMES-1 already recorded. midnight-zk is still `midnightntwrk/midnight-zk`.)
- Upstream tracks the whole problem in **midnight-ledger issue #322, "Mobile (React Native) Platform Support"** (open, labelled TO DO / epic, created 2026-04-09; read 2026-11-24). Its verdict on the native route: crates would need "uniffi bindings for iOS and jni bindings for Android, plus CI to cross-compile for `aarch64-linux-android`" — i.e. upstream lists the NDK route as future work that does not exist, and calls it the "long-term answer" against the WebView bridge's "pragmatic starting point".

### Build attempt — **Verified, run by me 2026-11-24**

Environment: this workspace, rustc/cargo 1.99.0 default with the zk repo's pinned 1.90.0 toolchain honoured via its `rust-toolchain.toml`. No Android NDK installed (deliberately not installed — it is a multi-GB download, and the attempt below already answers the question).

1. `rustup target add aarch64-linux-android` — **succeeded**, for both the default toolchain and the repo-pinned 1.90.0. (First `cargo check` run failed with E0463, "can't find crate for `core`", because the target std had only been added to the default toolchain, not the pinned one. Fixed by adding the target to 1.90.0; that failure was setup, not signal.)
2. Shallow clone of `midnightntwrk/midnight-zk` @ `main`, then the smallest proving-relevant crate I could isolate — `midnight-proofs`, the prover itself:
   `cargo check -p midnight-proofs --target aarch64-linux-android`
3. Result: **43 dependency crates checked/compiled, then the build stopped**, before any Midnight crate was reached. Verbatim first blocking error:

   ```
   error: failed to run custom build command for `blst v0.3.17`
   error occurred in cc-rs: failed to find tool "aarch64-linux-android-clang": No such file or directory (os error 2)
   ```

   `blst` (the C/assembly BLS12-381 library underneath `midnight-curves`) is the first crate in the tree that needs a C compiler for the target. `aarch64-linux-android-clang` ships with the **Android NDK**, which also provides the linker a full `cargo build` (beyond `check`) would need. So the precise statement is: *the proving tree does not check for Android out of the box; the first blocker is a missing NDK, and blst may not be the last C-dependent crate in the graph — the check never got past it.*

### Packaging caveat — **Verified**

Even with a working NDK build, shipping a native library in an app has a current platform gate: since 2025-11-01, new apps and updates targeting Android 15+ on Google Play must support 16 KB memory page sizes for native code (Android Developers, "Support 16 KB page sizes"; Android Developers Blog, 2025-05). NDK r28+ aligns by default; an older or hand-rolled Rust cross-build can silently produce a `.so` Play will reject.

### Assessment

Highest ceiling, longest road. Third-party evidence says native mobile halo2 proving is roughly laptop-class on flagships (Mopro's published Halo2 numbers, §2) — but for Midnight specifically, everything between "NDK installed" and "prover in an APK" (bindings, CI, 16 KB alignment, the rest of the C-dependency graph, then the full ledger/zkir crates on top of `midnight-proofs`) is unbuilt, by upstream's own accounting. A team project of weeks-to-months, not a spike. **Not the v1 route.**

---

## Route 2 — WASM prover in-app (WebView)

**Idea:** the ledger already ships as WebAssembly for browser wallets. Run that WASM prover inside an Android WebView (or a WebView bridge behind a native shell) and prove in the page context.

### What exists — **Verified**

- The WASM packages are real: `ledger-wasm`, `zkir-wasm`, `onchain-runtime-wasm` in midnight-ledger, published as the `@midnightntwrk/ledger-v9` family. The wallet SDK contains a `WasmProverImpl` that, in issue #322's words, "directly instantiates WASM for proof generation". So the WASM build does not merely assemble transactions — a prover implementation exists in the stack.
- Midnight's own in-browser prover demo, `wasm-proving-demos/` in midnight-ledger (read 2026-11-24), opens with: *"This is \*not\* production code! It's a PoC demonstrating a) how to use the existing wasm packages to prove in-browser, and b) how to make a multithreaded in-browser prover work, with the caveats that come with it."* Its `zkir-mt` demo proves a DUST spend in the browser. Its build script is a warning label in itself: `RUSTC_BOOTSTRAP=1`, `RUSTFLAGS='-C target-feature=+atomics,+bulk-memory'`, `wasm-pack build … -Z build-std=panic_abort,std`, and a `wasm-bindgen-rayon` thread pool — nightly-style build hacks, and multithreading that rests on SharedArrayBuffer-class browser features (in an Android WebView, that means the app must serve its own pages cross-origin-isolated; fiddly, and single-threaded fallback proving is slower still).

### What production wallets actually do today — **Verified**

The official guide "Proving transactions locally" (docs.midnight.network, opened 2026-11-24) lists exactly two supported setups: a **local proof server** over HTTP (`httpClientProofProvider('http://localhost:6300', …)`) or the **wallet proving provider** (`dappConnectorProofProvider`). Behind the second, per-wallet behaviour decides — and for Lace, the guide states that `getProvingProvider()` "returns a provider that sends requests to the proof server address in the wallet's settings", with the local proof server "the only choice the settings offer". The guide does say a wallet *can* prove "in a browser with the wallet SDK's WebAssembly prover in a Web Worker, or natively on a mobile device" — as possibilities to check per wallet, not as anything a shipping wallet is documented to do.

Delegation itself is young: on the Midnight forum ("Lace wallet doesn't implement getProvingProvider()", team-answered, read 2026-11-24), the answer is that Lace does **not yet** expose `getProvingProvider()` on the DApp Connector API at all, and 1AM is the reference implementation. And issue #322 adds the shell constraint: React Native's Hermes engine has **no `WebAssembly` global**, so a React Native app cannot run the WASM prover directly — upstream's suggested workaround is a hidden WebView bridge (their estimate: ~50–100 ms added latency per call) or native FFI (Route 1).

### Performance context — **Reported, third-party, NOT Midnight circuits, NOT measured by me**

No Midnight-circuit proving benchmark on any phone exists in anything I opened. The honest anchors from adjacent halo2 work:

- **Mopro** (zkmopro.org/docs/performance/, opened 2026-11-24), Halo2 Keccak circuit: native MacBook Pro M1 Pro 10.3 s · iPhone 15 Pro 11.0 s · Pixel 6 Pro (native) 23.9 s · **Web/WASM 26.6 s** — and their larger RSA circuit *crashes* on phones: ~5 GB needed vs a ~3 GB per-app memory limit. Circuit size and memory ceilings, not the runtime, are the binding constraint.
- **ChainSafe's Zcash web-wallet feasibility report**: one Orchard (halo2) spend proved in ≈5.4 s — in desktop Firefox on an M2 MacBook Air, not a phone.
- **Aleph Zero**: an optimized halo2 WASM membership proof at ≈1.38 s on a "moderately fast laptop", after a dedicated optimization programme (multithreading took it 6.5 s → 3.7 s before circuit changes).

Read together: small circuits are seconds-class in desktop WASM; a phone WebView will be slower; nobody knows by how much for Midnight's circuits.

### Assessment

The only route that becomes a *normal installable app* without NDK work, and upstream's own "pragmatic starting point" — but the prover it would ship is officially a PoC built with nightly hacks, no production wallet proves this way on mobile today, and there are no Midnight phone numbers to design against. **The v2 route: design the app so this engine can replace Route 3's behind the same interface.**

---

## Route 3 — The official arm64 proof server in an Android Linux environment

**Idea:** skip porting the prover — run the prover that already ships. The official proof-server image has an arm64 build; Android offers two Linux environments that can host an arm64 Linux binary: Google's AVF-based Linux Terminal (a real Debian VM) and Termux + proot-distro (a glibc userland inside an Android app).

### The artifact — **Verified**

Docker Hub registry API, queried by me 2026-11-24: the manifest list for `midnightntwrk/proof-server:9.0.2` contains **linux/amd64** (`sha256:fe574d00…`) and **linux/arm64** (`sha256:aa9a9841…`). This is the same 9.0.2 the team already runs: Hermes ran the full local stack on the 3090 host (HERMES-3), Sol's Proof Station ships it on Windows, and Muse runs the amd64 binary in its own workspace (verified 2026-11-24: health/version/ready endpoints answering, 18 parameter sets fetched and verified from srs.midnight.network). The server listens on port 6300 with no authentication — on a phone as on a desktop, it must be bound to localhost only, exactly as the official guide warns.

### Sub-route 3a: Android Linux Terminal (AVF) — real VM, likely not on our device

Android 15 QPR2 / 16's Terminal app boots a full Debian VM through the Android Virtualization Framework — its own kernel, hypervisor-isolated, near-native CPU (Wikipedia, "Android 16", and the AOSP package record at sysapps.bayton.org; opened 2026-11-24). Inside that VM the arm64 proof server runs as it would on any Debian arm64 machine — this is the cleanest form of Route 3.

The catch is device support. Coverage opened 2026-11-24 reports Samsung's implementation is **Exynos-only**: SamMobile reporting (via kunalganglani.com's developer write-up) says One UI 8.5's Terminal is supported only on Exynos-powered Galaxy devices, and linuxvox's survey notes the Galaxy Tab S11 (Dimensity) qualifies while "many Snapdragon-based Samsung tablets and phones do not". The team's only Android device, Kyle's **Galaxy Z Fold7, is Snapdragon 8 Elite for Galaxy (SM8750)** — PhoneArena and Android Headlines spec sheets, opened 2026-11-24 (12 GB RAM, 16 GB on the 1 TB model; 4,400 mAh; Android 16 / One UI 8). On current reporting, **the AVF sub-route is probably unavailable on the very phone we would test on**. That is a *Reported* conclusion, not a device-verified one — only Kyle can check his Fold7's developer options, and that check is his to make.

### Sub-route 3b: Termux + proot-distro — works anywhere, costs overhead

Termux (F-Droid build) plus proot-distro's Debian gives a glibc arm64 userland on essentially any Android phone, no root — the extracted arm64 proof-server binary can run inside it, and an app (or the user, via any local client) reaches it at `localhost:6300`. Costs, from sources opened 2026-11-24:

- **proot overhead — Reported.** proot fakes a root filesystem via ptrace syscall rewriting. One benchmark comparison puts proot-distro at ~44% lower UNIXbench than native, and a 2026 Android-runtime research write-up (github.com/bentlybro/kern) puts glibc distros under proot at "roughly 40–50% benchmark penalty". Those are syscall-heavy general benchmarks; proving is CPU-bound once running, so the *proving* penalty is plausibly smaller — and **unknown**. Nobody has measured it.
- **Android kills background work — Verified as a platform behaviour.** Since Android 12, the phantom process killer terminates child processes it judges excessive (documented in Termux's own README notice, via Cosyra's Termux/Rust field guide, opened 2026-11-24). Mitigations exist (Android 14+ Developer option "Disable child process restrictions"; ADB on older versions) — all developer-grade, none app-store-grade. A multi-minute prove is exactly the workload this killer targets.
- **Samsung background behaviour — Reported, single-source.** The kern research write-up also reports One UI 8.x pinning backgrounded Termux to little cores even with unrestricted battery and a foreground service (described as an open bug). Unverified; noted because the test device is a Samsung.
- **Thermals and battery.** Sustained proving is sustained all-core CPU. Every phone throttles; the Fold7's 4,400 mAh is not large. No numbers exist until someone measures them on the device.

### Assessment

Not a normal app — an enthusiast/developer setup (sideloaded Termux, or a developer-options VM), with real platform friction. But it is the **only route that can put the genuine, current prover on the team's actual phone** without building new proving infrastructure first, and it composes with Sol's design instead of competing with it: the app talks to `localhost:6300` exactly as Midnight.js and Lace already do on desktop. **The v1 route.**

---

## Device-testing reality

- The team's only Android hardware is Kyle's Galaxy Z Fold7 (specs in §3a). There is no test farm, no second device, no emulator result in this brief (an emulator would run x86_64 or emulated arm64 and prove nothing about the phone).
- Every on-device step — checking whether the Fold7 offers the Linux Terminal, installing Termux, timing a single real proof, watching battery and thermals — is a later step that requires Kyle and his phone. This brief deliberately claims none of it.
- The first honest on-device milestone, when Kyle calls it: one built-in circuit (a DUST spend is the natural candidate — it is what upstream's own WASM demo proves) proved on the Fold7 via Route 3b, timed by hand, reported whatever the number is.

## Honest unknowns

- Whether the Fold7's One UI build offers the Linux Terminal at all (reported Exynos-only; unverified on the device).
- Real proving time, battery drain, and thermal behaviour for **every** route on **any** phone. No published Midnight numbers exist; this spike measured none.
- proot's true penalty on a CPU-bound halo2 prove (available benchmarks are syscall/general-purpose).
- Whether `blst` is the only NDK blocker in `midnight-proofs`' tree — the check stopped at the first C-dependent crate — and what the full ledger + zkir + proof-server crates would add on top.
- The WASM prover's production timeline: `wasm-proving-demos` is demo code today; whether upstream productionizes it (and what the wallet SDK's `WasmProverImpl` is actually used by, if anything shipping) is not documented in what I opened.
- Per-app memory ceilings vs prover-key + witness sizes for larger circuits on the Fold7 specifically (Mopro's crash data point is a different stack, same constraint class).
- Whether Samsung's reported background pinning of Termux reproduces on the Fold7.

## Sources (all opened 2026-11-24)

- `midnightntwrk/midnight-ledger` and `midnightntwrk/midnight-zk` — workspaces, workflows, and code search via the GitHub API; `wasm-proving-demos/README.md` and `run.sh` read in full.
- midnight-ledger issue #322, "Mobile (React Native) Platform Support" (open epic, TO DO) — read in full via `gh`.
- Midnight Docs, "Proving transactions locally" — https://docs.midnight.network/guides/local-proving
- Midnight Forum, "Lace wallet doesn't implement getProvingProvider()" — https://forum.midnight.network/t/lace-wallet-doesnt-implement-getprovingprovider-expected-behavior-or-version-gap/1213
- Docker Hub registry API — `midnightntwrk/proof-server:9.0.2` manifest list (amd64 + arm64 digests above); tag page: https://hub.docker.com/r/midnightntwrk/proof-server/tags
- Mopro performance and benchmarks (third-party halo2 context) — https://zkmopro.org/docs/performance/
- ChainSafe, "Zcash Web Wallet — Feasibility Report" — https://raw.githubusercontent.com/ChainSafe/zcash-wasm-benchmark/main/REPORT.md
- Aleph Zero, "How we have optimized ZK operations to under one second" — https://alephzero.org/blog/zk-operations-optimized-to-under-one-second
- Android Developers, 16 KB page sizes — https://android-developers.googleblog.com/2025/05/prepare-play-apps-for-devices-with-16kb-page-size.html
- Android 16 / Linux Terminal (AVF) — https://en.wikipedia.org/wiki/Android_16 ; Terminal package record — https://sysapps.bayton.org/packages/com.android.virtualization.terminal
- Samsung Terminal support reporting — https://www.kunalganglani.com/blog/samsung-dex-coding-dev-machine ; https://linuxvox.com/blog/linux-on-samsung-tablet/
- Galaxy Z Fold7 specifications — https://www.phonearena.com/phones/Samsung-Galaxy-Z-Fold-7_id12645 ; https://www.androidheadlines.com/samsung-galaxy-z-fold-7-specs
- Termux/proot behaviour — https://Cosyra.com/guides/rust-on-termux.html ; https://github.com/bentlybro/kern/blob/HEAD/docs/research/runtime-exec.md
- This repo's desktop reference points — [`local-stack-verification.md`](local-stack-verification.md) (HERMES-3); Hermes' Proof Station results comment on PR #4.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
