# GROK-10 — Proof Station docs + QA sweep

**Checked:** 2026-11-24, about 18:15–18:45 CT (America/Chicago, UTC-6). External links were opened read-only on that date.
**Checked by:** Linda (Grok swarm, docs/content seat).
**Under test:** `apps/proof-station/README.md` and the app files next to it (`StationBackend.cs`, `CommandRunner.cs`, `MainWindow.xaml`, `MainWindow.xaml.cs`, `App.xaml.cs`, `MidnightProofStation.csproj`, `preview.compose.yaml`, `db-tls.compose.yaml`, `gpu-lab.sh`, `tests/attribution.py`, `tests/proof-result.sh`) and `.github/workflows/proof-station.yml`. Source is the `grok/GROK-10-proof-station-qa` branch, cut from `main` @ 7b37a4c, downloaded as a tarball.
**Networks:** Midnight **Preview (testnet)** only, which is the network the app targets. Cardano Preview (testnet) db-sync is a prerequisite. Nothing in this sweep touched any chain.
**Compared against:** the live [compatibility matrix](https://docs.midnight.network/relnotes/support-matrix), opened 2026-11-24, plus [`stack-versions.md`](stack-versions.md) (HERMES-1) and [`local-stack-verification.md`](local-stack-verification.md) (HERMES-3).

This is a read-only audit. I did not change the app, its README or its workflow. Every fix below is a proposal for a later task.

## Summary

- I checked **44** items named in the README: **11** commands and GUI controls, **11** paths, files and endpoints, **12** versions and parameters, and **10** external links (plus the two attribution links). Then I checked the four Windows setup steps and the Proof Lab setup paragraph line by line.
- **35** items match the code, the matrix or the live source. **9** are wrong, unclear or incomplete: C4, C5, P1, P6, P10, P11, V4, V7 and the local-proving link.
- The fix list has **13** proposals. Twelve are for the README: 8 come from those 9 items, and 4 come from the Windows setup comparison and the matrix re-check. One (fix 13) is for the workflow file.
- There are no blocking errors. Every version pin in the README, the Compose file and the GUI matches the live matrix: node **2.1.101**, indexer **4.4.101**, proof server **9.0.2**, Compact toolchain **0.36.1**, Midnight.js **5.0.1**, Wallet SDK **2.0.1**. I found no stale Ledger 8-era pin presented as current.
- The most important issues:
  1. **The database CA step reads as required, but the app treats it as optional** (fix 1). Node 2.1.101 connects to db-sync over TLS only, so the README should say that plainly.
  2. **Step 1 cannot be followed from `main`** (fixes 4 and 13). The Proof Station workflow has only ever run from `sol/*` branches and PRs. It has no `workflow_dispatch` trigger, and it does not link to a run.
  3. **"Start Preview stack" also starts the wallet prover, and its node health check has a limit of about 10.5 minutes (estimate)** (fix 2). The README does not mention either.
  4. **"Local proving" is not offline on first use.** HERMES-3 saw proof server 9.0.2 download keys and parameters from `srs.midnight.net` (fix 10).

## 1. Commands and GUI controls

| # | README text (line) | Where it lives in the code | Result |
| --- | --- | --- | --- |
| C1 | `dotnet run --project apps/proof-station/MidnightProofStation.csproj` (L40), from the repo root, .NET 10 SDK (L37) | The csproj exists at that path with `net10.0-windows` and `UseWPF`. The Compose files, `gpu-lab.sh` and `README.md` are `CopyToOutputDirectory`, so `dotnet run` finds them next to the binary. | Matches. Not run (no Windows, no .NET SDK on the box). |
| C2 | **Start wallet prover** (L8) | `StartWalletProverAsync` runs `docker --context desktop-linux compose … up -d --no-deps proof-server` | Matches |
| C3 | **Cardano Preview database** (L9) | `Expander Header="Cardano Preview database"` holding a `PasswordBox` | Matches |
| C4 | "Select its PEM CA certificate" (L9) | Button **Select CA certificate**, placeholder text "Optional database CA". `caPath` may be empty, and `db-tls.compose.yaml` is added only when a CA is chosen. | **Issue**: fix 1 |
| C5 | **Start Preview stack** "starts the full node and waits for block 1 before starting the indexer" (L9) | `up -d node indexer proof-server`. The indexer has `depends_on: node: condition: service_healthy`. The node health check asks for `chain_getBlockHash [1]`, with `start_period 30s`, `interval 15s` and `retries 40`. | Partly matches. It also starts the proof server, and the health check limit is not mentioned. **Issue**: fix 2 |
| C6 | **Stop services** keeps the chain and index databases (L10) | `compose stop`, not `down`. The named volumes `node-data` and `indexer-data` remain. | Matches |
| C7 | "Services can continue running after the window closes" (L10) | Closing the window does not stop containers. It is blocked only while an operation is running. | Matches |
| C8 | **CPU** selected at launch, **Build CPU prover**, **Generate sample proofs** (L26) | `CpuMode IsChecked="True"`. The button text is `Build CPU prover`, which calls `gpu-lab.sh build-cpu` and then `prove-cpu`. | Matches |
| C9 | **Detect GPU** (L27) | `CheckGpu` runs `gpu-lab.sh detect <index>` (`nvidia-smi --query-gpu=…compute_cap…`) | Matches |
| C10 | **Build CUDA backend** (L27) | Button text in CUDA mode. It runs `gpu-lab.sh build`, which runs upstream `bench/gpu-prover/rc1/scripts/build.sh`. | Matches (statically). Not run. |
| C11 | Mode names **CPU** / **NVIDIA CUDA** (L26–27) | `RadioButton Content="CPU"` / `"NVIDIA CUDA"` | Matches |

## 2. Paths, files and endpoints

| # | README text (line) | Evidence | Result |
| --- | --- | --- | --- |
| P1 | Artifact **midnight-proof-station-win-x64** (L7) | Workflow `upload-artifact` name `midnight-proof-station-win-x64`, path `publish/` | Name matches. **Issue**: there is no link and no `main` run (fix 4) |
| P2 | `MidnightProofStation.exe` (L7) | `AssemblyName` `MidnightProofStation`. The workflow checks that `publish/MidnightProofStation.exe` exists. | Matches |
| P3 | "Keep the executable, Compose files, and shell script together" (L7) | `StationBackend` loads `preview.compose.yaml`, `db-tls.compose.yaml` and `gpu-lab.sh` from `AppContext.BaseDirectory`. The workflow checks that all of them are in `publish/`. | Matches |
| P4 | "The build includes the .NET Desktop Runtime" (L7) | `dotnet publish … --self-contained true -p:PublishSingleFile=true` | Matches |
| P5 | `host.docker.internal` for a database on the Windows host (L9) | The URI is passed to the node as `DB_SYNC_POSTGRES_CONNECTION_STRING`, and the app accepts only `postgres`/`postgresql` schemes | Consistent. Not run. |
| P6 | "does not write [the URI] to disk", "Docker retains it" (L12) | `PasswordBox` input. Log lines replace the URI with `[database URI hidden]`. It reaches the container as an environment variable. | Matches. **Incomplete**: the app does write an indexer secret to disk (fix 5) |
| P7 | "All published service ports bind to loopback" (L12) | Ports are `127.0.0.1:9944`, `127.0.0.1:8088` and `127.0.0.1:6300`. The official local-proving page also says to publish the proof server on `127.0.0.1` only. | Matches |
| P8 | Node RPC `http://127.0.0.1:9944` / `ws://127.0.0.1:9944` (L16) | Compose ports, plus `RpcAsync` posting to `http://127.0.0.1:9944`. The indexer uses `ws://node:9944`. | Matches |
| P9 | Indexer `http://127.0.0.1:8088/api/v4/graphql` (L17) | `ReadIndexerAsync` uses the same URL. HERMES-3 measured the same path on the local stack. | Matches |
| P10 | Wallet prover `http://127.0.0.1:6300` (L18), but `http://localhost:6300` in step 2 (L8) | Compose `127.0.0.1:6300:6300` | Inconsistent. **Issue**: fix 3 |
| P11 | "Build files stay in the distribution's user data directory" (L24) | `${XDG_DATA_HOME:-$HOME/.local/share}/midnight-proof-station/<revision>` | Matches. The path could be named (fix 9) |

## 3. Versions and parameters

| # | README value (line) | Code / live source (checked 2026-11-24) | Result |
| --- | --- | --- | --- |
| V1 | Node **2.1.101** (L43) | `midnightntwrk/midnight-node:2.1.101` in Compose and in the GUI label. Live matrix: Preview node 2.1.101. Docker Hub tag exists (amd64 + arm64, pushed 2026-11-08 UTC). | Matches |
| V2 | Indexer **4.4.101** (L43) | `midnightntwrk/indexer-standalone:4.4.101`, GUI label, live matrix 4.4.101. Docker Hub tag exists. | Matches |
| V3 | Proof server **9.0.2** (L33, L43) | `midnightntwrk/proof-server:9.0.2`, GUI label, live matrix 9.0.2. Docker Hub tag exists (pushed 2026-11-16 UTC). | Matches |
| V4 | Compact **0.36.1** (L43) | Live matrix: Compact *toolchain* 0.36.1, language 0.28.0. Compact *devtools* is 0.5.1. | Correct, but "Compact" alone is ambiguous. Fix 7 |
| V5 | Midnight.js **5.0.1** (L43) | Live matrix 5.0.1 | Matches |
| V6 | Wallet SDK **2.0.1** (L43) | Live matrix 2.0.1 | Matches |
| V7 | DApp Connector API, not named | Live matrix 4.1.0. Lace talks to dApps through it. | Missing. Fix 7 |
| V8 | "Ledger 9 proof version" (L45) | `ReadProofServerAsync` and the workflow require `V3` from `/proof-versions`. The official local-proving page says proof server 9 returns `["V2", "V3"]`. | Matches |
| V9 | PR #667 at `6bcc6a5`, `midnight-proofs 0.9.0-rc.1`, draft (L33) | `gpu-lab.sh` pins `6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8`. Upstream `proofs/Cargo.toml` at that commit: `midnight-proofs` `0.9.0-rc.1`. The PR page shows status **Draft** on 2026-11-24. | Matches |
| V10 | `k=16`, eight prover threads, one cold + one warm proof (L29) | `RC1_K=16`, `RAYON_NUM_THREADS=8`, `RC1_WARM=1`, `RC1_REP=1`. `verify_proof_rows` needs exactly one `cold` row and one `warm` row (`NR == 3`). | Matches |
| V11 | RTX 3090 reports `sm_86` (L27) | NVIDIA's compute-capability page lists GeForce RTX 3090. `gpu-lab.sh` builds `sm_<major><minor>` from `compute_cap`. Compute capability 8.6 for Ampere GeForce is from NVIDIA's table, not from a device. | Matches (from documentation, no device) |
| V12 | Benchmark `k=21`, 16 threads (L31) | The linked `RESULTS.md` @ 50fe68f has a "k=21, EMPTY card … 16 prover threads" section for head `6bcc6a56`. | Matches |

Pass/fail rule (L29): `verify_proof_rows` rejects a row that is not verified, has device errors, has zero device calls in CUDA mode, or has any device work in CPU mode. `bash tests/proof-result.sh` printed **10 / 10 PASS** lines on the box (GNU bash, no GPU needed). The README's description is accurate.

## 4. Windows setup, line by line

Compared with HERMES-3 (a hands-on run on a Windows 11 + Docker Desktop WSL2 machine, **local `undeployed` devnet**, not Preview) and HERMES-1 (versions and docs audit).

| README step | Compared with | Finding |
| --- | --- | --- |
| **1.** Extract the build linked from the Proof Station workflow (L7) | Workflow file. Runs listed through the GitHub connector on 2026-11-24. GROK-3 judgement call 3. | The workflow triggers only on `push` to `sol/midnight-proof-station` and on `pull_request`. It has no `workflow_dispatch`. The 10 listed runs all came from `sol/midnight-proof-station` or `sol/sol-gpu-2-proof-station`, and all succeeded (latest 2026-11-24 17:44 CT). No run on `main`. Downloading an artifact needs a GitHub sign-in, and artifacts expire. Fixes 4 and 13. |
| **2.** Docker Desktop, Linux containers, WSL2 backend. The proof server is at `localhost:6300`. Point Lace there (L8) | HERMES-3 §1 (same Docker Desktop + WSL2 setup), HERMES-1 note 5 (the proof server image runs natively on Windows), HERMES-1 Lace note (Lace 2.4.3 and later forward proving to the proof server in settings; the latest release was 2.5.2 on 2026-11-23). App: `EnsureLocalEngineAsync` requires the `desktop-linux` context at `npipe:////./pipe/dockerDesktopLinuxEngine`. | Consistent. Use `127.0.0.1` (fix 3). The proof server downloads parameters on first boot (fix 10). |
| **3.** db-sync URI, `host.docker.internal`, PEM CA, Start Preview stack (L9) | Official [full-node guide](https://docs.midnight.network/nodes/full-node): "Node 2.1.101 connects to Cardano-db-sync over TLS only". The guide uses `CFG_PRESET=preview CARDANO_SECURITY_PARAMETER=432` and the same two Preview bootnodes. | The preset, security parameter `432`, bootnodes, `--pool-limit 35`, `--no-private-ip` and the archive pruning flags all match the guide. The guide also passes `--chain …/chain-spec-raw.json`. Compose does not, and relies on the image's `res/` and `CFG_PRESET` (not verified). The app checks at runtime that `system_chain` is "Midnight Preview". The CA wording conflicts with the GUI (fix 1). The start order is described incompletely (fix 2). |
| **4.** Watch height, peers and indexed height. Stop keeps the data (L10) | HERMES-3 §3: on first boot the node shut itself down with `txpool-background` (local devnet preset). The fix was to run start again. HERMES-3 §7.6: indexer tag 4.4.101 prints "Midnight Indexer v3.0.15". | Preview Compose has **no `restart:` policy**, so a node that exits stays stopped until the user clicks start again (fix 11). The indexer log banner number differs from the tag (fix 12). The full-node guide says 0 peers means checking port 30333 or the bootnodes (see the note after the fix list). |
| **Proof Lab setup**: default WSL2 distribution, Git, C/C++ toolchain, Rust via rustup (L24) | `gpu-lab.sh` checks `git`, `cargo` and (CUDA only) `nvcc`, `/dev/dxg` and `nvidia-smi`. It also uses `flock`, `timeout`, `awk` and `install`, which are standard on Ubuntu. Upstream `rust-toolchain.toml` @ 6bcc6a5 pins channel **1.90.0**. HERMES-1 note 5: WSL is the supported Windows path. | Consistent. rustup will install Rust 1.90.0 on the first build. Worth saying, together with the build path (fix 9). |

## 5. External links (opened 2026-11-24, ~18:19 CT, read-only)

| Link (README line) | Status |
| --- | --- |
| docs.midnight.network/nodes/full-node (L20) | Rate-limited with HTTP 429 to `curl`. Opened with the fetch tool: the page is current and names node 2.1.101 |
| docs.midnight.network/guides/manage-dust (L20) | 429 to `curl`. Opened with the fetch tool: current page |
| developer.nvidia.com/cuda/gpus (L27) | HTTP 200. Lists GeForce RTX 3090 |
| learn.microsoft.com/windows/ai/directml/gpu-cuda-in-wsl (L27) | HTTP 200 (redirects to `/en-us/`) |
| RTX 3090 `RESULTS.md` @ 50fe68f (L31) | HTTP 200 |
| github.com/midnightntwrk/midnight-zk/pull/667 (L33) | HTTP 200, status **Draft** |
| CharlesHoskinson/midnight-zk tree @ 6bcc6a5 (L33) | HTTP 200 |
| `docs/gpu-prover/benchmarks.md` @ 6bcc6a5 (L33) | HTTP 200 |
| docs.midnight.network/guides/local-proving (L33) | 429 to `curl`. Opened with the fetch tool. **Its body describes Ledger 8-era (stale, not current) versions**, with a callout that says to use proof server 9.0.2, toolchain 0.36.1 and Midnight.js 5.0.1. Fix 6 |
| docs.midnight.network/relnotes/support-matrix (L43) | 429 to `curl`. Opened with the fetch tool. All six pins match |
| x.com/kshot9000, github.com/Kshot3000 (L52) | HTTP 200 |

## 6. Fix list (proposals only, not applied)

Line numbers refer to `apps/proof-station/README.md` at `main` @ 7b37a4c unless another file is named.

1. **File:** `apps/proof-station/README.md`, **line 9.**
   **Current:** "Select its PEM CA certificate to verify the database host’s TLS certificate."
   **Proposed:** "The node connects to db-sync over TLS only. If your database certificate is signed by a private CA, choose **Select CA certificate** and pick that CA’s PEM file. The app treats this as optional."
   **Evidence:** the GUI placeholder "Optional database CA" (`MainWindow.xaml` L271). `StationBackend.StartPreviewAsync` accepts an empty `caPath`, and `ComposeAsync` adds `db-tls.compose.yaml` only when a CA is set. The official full-node guide says "Node 2.1.101 connects to Cardano-db-sync over TLS only." The node's behaviour without a CA file is not verified (see NOT verified).

2. **File:** `apps/proof-station/README.md`, **line 9.**
   **Current:** "**Start Preview stack** starts the full node and waits for block 1 before starting the indexer."
   **Proposed:** "**Start Preview stack** starts the full node and the wallet prover. It starts the indexer once the node returns a hash for block 1. If the node is not healthy after about 10 minutes (estimate from the health-check settings), the start fails and you can click **Start Preview stack** again."
   **Evidence:** `StartPreviewAsync` runs `up -d node indexer proof-server`. In `preview.compose.yaml` L34–44, the health check is `chain_getBlockHash [1]` with `start_period: 30s`, `interval: 15s` and `retries: 40`. That gives about 30 s + 40 × 15 s ≈ 10.5 min. This is an estimate: Compose timing also depends on the check timeout and on the start period. The app's own timeout is 25 min.

3. **File:** `apps/proof-station/README.md`, **line 8.**
   **Current:** "starts the stock proof server at `http://localhost:6300`."
   **Proposed:** "starts the stock proof server at `http://127.0.0.1:6300`."
   **Evidence:** Compose publishes `127.0.0.1:6300:6300` only. The README endpoint table (L18) and the GUI label both say `127.0.0.1`. On Windows, `localhost` can resolve to IPv6 `::1` first, and no port is published there. Whether Lace falls back to IPv4 is not verified.

4. **File:** `apps/proof-station/README.md`, **line 7.**
   **Current:** "Extract the **midnight-proof-station-win-x64** build linked from the **Proof Station** workflow."
   **Proposed:** "Download the **midnight-proof-station-win-x64** artifact from the latest successful [Proof Station workflow run](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/actions/workflows/proof-station.yml) and extract it. Downloading Actions artifacts needs a GitHub sign-in, and artifacts expire."
   **Evidence:** the README links to no run. The workflow runs listed on 2026-11-24 all came from `sol/*` branches (most recent: run 45512465128, `sol/sol-gpu-2-proof-station`, success). GROK-3 (`qa-sweep-2026-11.md` judgement call 4) found the same gap. Kyle decides between a Release and a `main` run.

5. **File:** `apps/proof-station/README.md`, **line 12** (add a sentence at the end).
   **Current:** "… All published service ports bind to loopback."
   **Proposed:** add "The app also stores a random indexer secret in `%LOCALAPPDATA%\MidnightProofStation\indexer-key`."
   **Evidence:** `StationBackend.EnsureIndexerSecret` writes 32 random bytes as hex to that path and passes them as `APP__INFRA__SECRET`. The README says only that the database URI is not written to disk.

6. **File:** `apps/proof-station/README.md`, **line 33.**
   **Current:** "[Local proving and witness privacy](https://docs.midnight.network/guides/local-proving)"
   **Proposed:** "[Local proving and witness privacy](https://docs.midnight.network/guides/local-proving) (its body uses older versions that are now stale. Follow its ledger 9 note and use proof server 9.0.2)"
   **Evidence:** the page, opened 2026-11-24, says "The versions above are ledger 8 versions, which no public network runs anymore." HERMES-1 lists it in category B (labelled stale). On GROK-3, Muse chose to leave these links as they are. This is an optional re-raise, now that the Proof Station README is under audit.

7. **File:** `apps/proof-station/README.md`, **line 43.**
   **Current:** "The matching dApp tools are Compact **0.36.1**, Midnight.js **5.0.1**, and Wallet SDK **2.0.1**."
   **Proposed:** "The matching dApp tools are Compact toolchain **0.36.1**, Midnight.js **5.0.1**, Wallet SDK **2.0.1**, and DApp Connector API **4.1.0**."
   **Evidence:** the live matrix (2026-11-24) lists Compact devtools 0.5.1 and Compact toolchain 0.36.1 separately, and DApp Connector API 4.1.0. HERMES-1 confirms 4.1.0 is the pin for new builds.

8. **File:** `apps/proof-station/README.md`, **line 43.**
   **Current:** "The [Preview compatibility matrix](…) was checked on 2026-11-23."
   **Proposed:** "The [Preview compatibility matrix](…) was checked on 2026-11-23 and again on 2026-11-24."
   **Evidence:** this sweep re-opened the matrix on 2026-11-24 and every pin was unchanged. Optional. It only records the re-check.

9. **File:** `apps/proof-station/README.md`, **line 24.**
   **Current:** "The first build downloads the pinned source and Rust dependencies. Build files stay in the distribution’s user data directory."
   **Proposed:** "The first build downloads the pinned source, the Rust 1.90.0 toolchain it pins, and the Rust dependencies. Build files stay in `~/.local/share/midnight-proof-station/` inside the distribution."
   **Evidence:** upstream `rust-toolchain.toml` @ 6bcc6a5 sets `channel = "1.90.0"`. `gpu-lab.sh` L50 uses `${XDG_DATA_HOME:-$HOME/.local/share}/midnight-proof-station/$revision`.

10. **File:** `apps/proof-station/README.md`, **after line 20** (new sentence).
    **Current:** (none)
    **Proposed:** "On first start, and the first time it proves a circuit size, the proof server downloads keys and public parameters from `srs.midnight.net`, so it needs internet access."
    **Evidence:** HERMES-3 §1 and §7 item 8 (proof server 9.0.2 on the local devnet, 2026-11-24): first-boot downloads of ZKIR sources and keys, plus a 0.70 s parameter fetch for `k=6`. This was observed on the local devnet, not on Preview.

11. **File:** `apps/proof-station/README.md`, **line 10** (add a sentence).
    **Current:** "… Services can continue running after the window closes."
    **Proposed:** add "If a service exits, it stays stopped until you click **Start Preview stack** again. The services have no automatic restart."
    **Evidence:** `preview.compose.yaml` defines no `restart:` key. HERMES-3 §3 recorded a one-off first-boot node exit (`txpool-background`, local devnet preset), and running start again fixed it.

12. **File:** `apps/proof-station/README.md`, **after line 19** (note under the endpoint table).
    **Current:** (none)
    **Proposed:** "The indexer image tag is 4.4.101, but its log banner prints a different internal version number. That is expected."
    **Evidence:** HERMES-3 §7 item 6: `indexer-standalone:4.4.101` prints "Midnight Indexer v3.0.15" on boot.

13. **File:** `.github/workflows/proof-station.yml`, **lines 3–13** (Muse's file. The Grok token cannot push workflows).
    **Current:** `on:` `push` to `sol/midnight-proof-station` and `pull_request` only.
    **Proposed:** add `workflow_dispatch:` (and optionally `push` to `main` with the same `paths`). Then a `main` artifact can be produced without a PR.
    **Evidence:** the workflow file. The 10 runs listed on 2026-11-24 include no `main` run. GROK-3 suggested "Dispatch Proof Station on `main`", which is not possible without this trigger.

Also worth a README troubleshooting line, but not counted as a fix: the full-node guide says that if the status stays at "Idle (0 peers)", you should check the firewall (port 30333) or the `--bootnodes` addresses. Proof Station shows a "Waiting for peers" state but gives no hint.

## 7. What I ran on the box

- `npm test` from the repo root (Node v24.22.1). Results are in the PR body.
- `bash -n gpu-lab.sh`: OK. `shellcheck` 0.11.0 on `gpu-lab.sh` and `tests/proof-result.sh`: no findings.
- `bash tests/proof-result.sh`: 10 / 10 PASS lines. `python3 tests/attribution.py`: PASS.
- Both Compose files parse as YAML (`yaml` 2.x on Node). Services: `node`, `indexer`, `proof-server`, plus the `node` override. This is **not** `docker compose config`, because the box has no Docker.
- I read the node image metadata from the public registry. `midnight-node:2.1.101` (amd64) installs `curl-minimal` and `jq`, which the health check needs, and copies `res/`. I did not pull or run the image.

## NOT verified

- **Windows:** I did not run the app, `dotnet run` or `dotnet publish` (no Windows machine, no .NET SDK on the box). I did not check WPF rendering, the `desktop-linux` Docker context check, `wsl.exe --exec bash -s` or the Windows file dialog.
- **Running services:** I started no node, indexer or proof server. Not verified: Preview (testnet) sync, block height or peers, the `system_chain` string "Midnight Preview", the GraphQL query `{ block { height } }`, `/proof-versions` returning `V3` on 9.0.2 (from the docs and the workflow only), whether the health check actually passes, the indexer environment variable names, and whether the node starts on Preview from `CFG_PRESET` alone without `--chain`.
- **db-sync and TLS:** I had no Cardano Preview (testnet) db-sync. I did not check how the node behaves with or without `SSL_ROOT_CERT`, or with `host.docker.internal`.
- **GPU:** I ran no CUDA build, GPU detection or CUDA proofs. The `sm_86` value comes from NVIDIA's table, not from a device. The RTX 3090 results on the Board (SOL-GPU-1) were posted by Hermes. I did not reproduce them.
- **CPU Proof Lab:** I did not build the upstream prover or generate proofs. The workflow runs that report CPU proofs succeeding are CI evidence, and I did not reproduce them.
- **Wallet:** I did not connect Lace to the local prover, and I did no signing, DUST registration or transactions.
- **Artifact:** I did not download the Windows ZIP (it needs a GitHub sign-in).
- **Estimates:** the ~10.5 min health-check limit in fix 2 is an estimate from the Compose settings, not a measurement. I measured no disk use or sync time.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
