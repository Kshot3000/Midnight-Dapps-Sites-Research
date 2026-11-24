# HERMES-1 — Live Stack Verification Brief

**Checked:** 2026-11-23 (all live checks performed this date)
**Checked by:** Hermes (local seat), on the 15-minute idle cycle
**Method:** official docs source (`midnightntwrk/midnight-docs`, cloned this date, head pushed 2026-11-23 16:53 UTC) cross-checked against live npm registry metadata, GitHub releases, and the Docker Hub tag API. No version below was taken from memory or from a third-party blog.

**Source of truth:** the [compatibility (support) matrix](https://docs.midnight.network/relnotes/support-matrix). It states (source file `docs/relnotes/support-matrix.mdx`, line 19):

> "The client versions include the 1.1 patch of 17 November 2026: Compact toolchain 0.36.1, Compact JS 3.0.2, Wallet SDK 2.0.1, Midnight.js 5.0.1, and proof server 9.0.2. The patch fixes contract calls that the networks rejected with the earlier versions, so move to it."

## Current versions (all three public networks)

Mainnet, Preview, and Preprod **all run ledger 9** (Mainnet since 6 Nov 2026, Preview relaunch 7 Nov, Preprod 9 Nov) with identical versions. Pin these for any build:

| Component | Version | Where it lives | Verified |
| --- | --- | --- | --- |
| Compact toolchain (`compact compile`) | **0.36.1** (language 0.28.0) | [GitHub release `compactc-v0.36.1`](https://github.com/midnightntwrk/compact/releases) (2026-11-11) | matrix + GitHub |
| Compact devtools (`compact` CLI) | **0.5.1** (matrix) — 0.5.3 is the newest published | [GitHub releases](https://github.com/midnightntwrk/compact/releases) (0.5.3: 2026-09-29) | matrix + GitHub |
| Compact runtime | **0.21.0** | npm [`@midnight-ntwrk/compact-runtime`](https://www.npmjs.com/package/@midnight-ntwrk/compact-runtime) latest=0.21.0 (2026-11-03) | matrix + npm |
| Compact JS | **3.0.2** | npm [`@midnight-ntwrk/compact-js`](https://www.npmjs.com/package/@midnight-ntwrk/compact-js) latest=3.0.2 (2026-11-17) | matrix + npm |
| Platform JS | **3.0.0** | npm [`@midnight-ntwrk/platform-js`](https://www.npmjs.com/package/@midnight-ntwrk/platform-js) latest=3.0.0 | matrix + npm |
| On-chain runtime | **4.0.2** | npm [`@midnightntwrk/onchain-runtime-v4`](https://www.npmjs.com/package/@midnightntwrk/onchain-runtime-v4) latest=4.0.2 (2026-11-16) | matrix + npm |
| Ledger package | **`@midnightntwrk/ledger-v9` 1.0.2** | npm latest=1.0.2 (2026-11-16) | matrix + npm |
| Wallet SDK | **2.0.1** | npm [`@midnightntwrk/wallet-sdk`](https://www.npmjs.com/package/@midnightntwrk/wallet-sdk) latest=2.0.1 (2026-11-16); also published under `@midnight-ntwrk/wallet-sdk` at 2.0.1 | matrix + npm |
| Midnight.js | **5.0.1** | npm [`@midnight-ntwrk/midnight-js`](https://www.npmjs.com/package/@midnight-ntwrk/midnight-js) latest=5.0.1 (2026-11-17) | matrix + npm — see note 1 |
| testkit-js | **5.0.1** | npm [`@midnight-ntwrk/testkit-js`](https://www.npmjs.com/package/@midnight-ntwrk/testkit-js) latest=5.0.1 (2026-11-17) | matrix + npm |
| Midnight.js subpackages | `midnight-js-protocol` 5.0.1 · `midnight-js-network-id` 4.1.1 · `midnight-js-fetch-zk-config-provider` 5.0.1 · `midnight-js-dapp-connector-proof-provider` 5.0.1 | npm (all `@midnight-ntwrk/` scope) | npm |
| DApp Connector API | **4.1.0** | npm [`@midnightntwrk/dapp-connector-api`](https://www.npmjs.com/package/@midnightntwrk/dapp-connector-api) latest=4.1.0 (2026-11-05); repo [`midnightntwrk/midnight-dapp-connector-api`](https://github.com/midnightntwrk/midnight-dapp-connector-api) | matrix + npm |
| Proof server | **9.0.2** | Docker image [`midnightntwrk/proof-server:9.0.2`](https://hub.docker.com/r/midnightntwrk/proof-server/tags) (tag on Docker Hub, pushed 2026-11-16, amd64 + arm64); port 6300; default job capacity 10 (queue full → HTTP 429) | matrix + Docker Hub API |
| Node | 2.1.101 | [node-2.1.101 release](https://github.com/midnightntwrk/midnight-node/releases/tag/node-2.1.101) | matrix |
| Midnight Indexer | 4.4.101 | matrix (Blockfrost serves the Preprod indexer) | matrix |
| Node toolkit (public networks) | 2.1.101 | ships with the node release; the toolkit *guide* is written against 1.0.300 (local node 1.0.300) — see stale list C2 | matrix + node-toolkit page |

**Wallet — Lace:** latest browser extension is **2.5.2** (published **2026-11-23**, i.e. the check date — [GitHub releases](https://github.com/input-output-hk/lace/releases)); 2.5.1 (2026-11-07), 2.5.0 (2026-10-29). Lace injects the DApp Connector API under `window.midnight`, keyed by a fresh UUID per page load (`rdns` is `io.lace.wallet`); never look it up by a fixed key. Feature-detect methods: docs note Lace 2.4.2 leaves `hintUsage` undefined, and Lace builds before 2.1.0 lack `getProvingProvider()`. Lace 2.4.3 forwards proving to the proof server address in its settings.

### Notes

1. **Two npm scopes both exist and are live.** The matrix's Midnight.js is `@midnight-ntwrk/midnight-js` (latest 5.0.1). A second package, `@midnightntwrk/midnight-js` (no hyphen), is **not** deprecated and has published **5.0.2** (2026-11-19) and 5.0.3 pre-releases (most recent 2026-11-23). Neither is listed in the matrix, so **5.0.1 (`@midnight-ntwrk`) remains the tested pin** until the matrix moves. Do not "upgrade" to 5.0.2 without checking it against the matrix. The same dual-scope pattern applies to the Wallet SDK (`@midnightntwrk/wallet-sdk` per the matrix; a `@midnight-ntwrk/wallet-sdk` mirror also exists).
2. **The Compact repo moved.** [`midnightntwrk/compact`](https://github.com/midnightntwrk/compact) is **archived** — it only hosts release artifacts now. Active development lives at [`LFDT-Minokawa/compact`](https://github.com/LFDT-Minokawa/compact) (Linux Foundation Decentralized Trust); public releases are still published to `midnightntwrk/compact`. Latest release in both places: toolchain 0.36.1 (2026-11-11).
3. **create-mn-app is archived.** The [repo README](https://github.com/midnightntwrk/create-mn-app) carries a warning: it "is no longer Midnight's project scaffolder," the npm package (last version **0.5.1**, 2026-09-01) "will not be updated for future SDK releases." The replacement is the [`midnightntwrk/midnight-examples`](https://github.com/midnightntwrk/midnight-examples) monorepo — every example compiles and runs against one pinned toolchain in CI, all examples target ledger 9, and it has a 7-step [learning path](https://github.com/midnightntwrk/midnight-examples#learning-path) (hello-world → private-party → battleship → private-tip-jar → shielded-chips → zk-loan → leaderboard). **Do not scaffold new projects with `create-mn-app`.**
4. **Local dev stack:** [`midnightntwrk/midnight-local-dev`](https://github.com/midnightntwrk/midnight-local-dev) runs node + indexer + proof server in Docker with a pre-funded genesis wallet (network id `undeployed`); image versions are pinned in its `standalone.yml`. The proof server runs locally on port 6300 for *every* target network, including public ones.
5. **Windows:** the official [installation guide](https://docs.midnight.network/getting-started/installation) says "Windows is not supported natively at this time… development through WSL is recommended." The Compact compiler publishes no native Windows binary — `npm run compile` needs WSL. The proof server Docker image and the scaffolding tooling run natively on Windows. Relevant to this team's setup (Kyle's machine is Windows 11).
6. **Incompatible combinations worth knowing** (from the matrix's "Known incompatible combinations" table): ledger 8 artifacts (toolchain 0.30.0–0.31.1 era) are rejected by Midnight.js 5 for *new* deployments; pre-upgrade contracts compiled with 0.31.1 keep working but need the ZK config provider set to `verify: 'require-if-present'`; Midnight.js 5.0.0 / ledger-v9 < 1.0.2 had three call-rejection bugs, all fixed in the 1.1 patch set; Wallet SDK 2.0.0 had a prover bug fixed in 2.0.1; proof server 9.0.0/9.0.1 crashed on some Jubjub circuits, fixed in 9.0.2.

## Where Midnight's own docs still show stale versions

Audited against the docs source repo this date. Three categories:

### A. Current — matches the matrix (no action)

- `/getting-started/installation` — pins `compact update 0.36.1` and `midnightntwrk/proof-server:9.0.2` (source lines 61–98).
- `/getting-started/hello-world` — "Compact toolchain, version 0.36.1" (line 19).
- `/guides/windows-compact-setup` — `proof-server:9.0.2` (line 105).
- `/troubleshoot/installation-problems` — `proof-server:9.0.2` (line 111).
- `/relnotes/*` — release notes through the 1.1 patch are current.

### B. Labelled ledger-8 era (carry a `<Ledger8Banner />` or an explicit "these are ledger 8 versions" callout — kept as reference, **do not copy into new ledger-9 builds**)

| Page (live URL under docs.midnight.network) | Stale pins, file:line in `midnight-docs` |
| --- | --- |
| `/guides/browser-dapp` | "Midnight.js 4.1.1 and DApp Connector API 4.0.1" (`docs/guides/browser-dapp.mdx:21`); prereq "Compact toolchain 0.31.1, the version the support matrix pairs with Midnight.js 4.1.1"; install `@midnight-ntwrk/dapp-connector-api@4.0.1 @midnight-ntwrk/midnight-js-contracts@4.1.1` (line 75); `@midnight-ntwrk/ledger-v8` wasm import (line 92) |
| `/guides/local-proving` | body targets "proof server 8.1.3, Compact toolchain 0.31.1, Midnight.js 4.1.1, DApp Connector API 4.0.1, and Lace 2.4.2" (`docs/guides/local-proving.mdx:17`); `midnightntwrk/proof-server:8.1.3` (line 95); `@midnight-ntwrk/ledger-v8` import (line 187). Its callout correctly says to run 9.0.2 / 0.36.1 / 5.0.1 instead |
| `/guides/build-a-private-smart-contract` | "Compact CLI and compiler, version 0.31.1" |
| `/guides/deploy-and-operate` | 0.31.1 + `@midnight-ntwrk/compact-runtime` 0.16.0 + `@midnight-ntwrk/midnight-js-*` 4.1.1 + `@midnight-ntwrk/wallet-sdk` 1.2.0, described as "the versions midnight-examples pins" (`docs/guides/deploy-and-operate.mdx:27`) |
| `/guides/install-bun-runtime-midnight` | `compact update 0.31.1` (line 144); `bun add @midnight-ntwrk/compact-runtime@0.16.0` (lines 207, 538) |
| `/guides/node-server-wallet` | 0.31.1; `compact-runtime 0.16.0`; `midnight-js-contracts/http-client-proof-provider/indexer-public-data-provider 4.1.1` (lines 28–75) |
| `/guides/react-wallet-connect` | `@midnight-ntwrk/ledger-v8` wasm import (line 64) |
| `/tokens/unshielded-token` | "This tutorial targets compiler version 0.31.1" (line 85); `proof-server:8.1.3` (line 117); `midnight-js-contracts/-indexer-public-data-provider/-http-client-proof-provider/-node-zk-config-provider @4.1.1` (lines 138–141); `@midnight-ntwrk/wallet-sdk@1.2.0` (line 291) |
| `/tokens/shielded-token` | "the toolchain (compactc 0.31.1)" (line 46) |
| `/sdks/official/build-a-wallet` | banner present, **but** line 21 claims it "targets DApp Connector API 4.0.1 and Wallet SDK 1.2.0, the versions in the support matrix" — the matrix now lists 4.1.0 / 2.0.1, so the claim is wrong even with the banner; install line 29 pins `dapp-connector-api@4.0.1`, `wallet-sdk@1.2.0`, `ledger-v8@8.1.3`, `zkir-v2@2.1.1` |
| `/sdks/official/wallet-dev-guide` | sample code imports `@midnight-ntwrk/ledger-v8` |
| `/sdks/official/midnight-js` | bannered ledger-8 reference |

### C. Unlabelled — reads as current, contains stale numbers (the dangerous ones)

| Page | Stale content (file:line) |
| --- | --- |
| `/guides/nextjs-wallet-connect` | "This guide uses the DApp Connector API v4.0.1" (`docs/guides/nextjs-wallet-connect.mdx:52`) — matrix lists **4.1.0**; no ledger-8 banner on the page |
| `/nodes/node-toolkit` | the check commands pin `@midnightntwrk/wallet-sdk@1.2.0 @midnight-ntwrk/midnight-js-protocol@4.1.1` (`docs/nodes/node-toolkit.mdx:29`) — matrix lists 2.0.1 / 5.0.1. Page is written against toolkit 1.0.300 (local node 1.0.300) and says to use toolkit 2.1.101 against the public networks |
| `/api-reference/dapp-connector` | header reads "@midnight-ntwrk/dapp-connector-api **v4.0.1**" (`api-reference/dapp-connector/README.md:3`) — matrix lists **4.1.0** |
| `/examples/dapps/leaderboard` | `midnightntwrk/proof-server:8.0.3` (`docs/examples/dapps/leaderboard.mdx:183`) |
| `/examples/dapps/zkloan` | "verified against Compact language 0.23, compiler 0.31.1, `@midnight-ntwrk/midnight-js-*` 4.1.1 and wallet SDK 1.2.0"; compose runs `midnight-node:1.0.0`, `indexer-standalone:4.3.3`, `proof-server:8.1.0` (`docs/examples/dapps/zkloan.mdx:56`) |
| `/blog/connect-dapp-lace-wallet` (Dev Diaries, 2025-04-16) | self-flagged "Outdated connector API" — pre-4.0 code (`window.midnight.mnLace`, `isEnabled()`/`enable()`) that doesn't work with current Lace |
| `/0.0.0/…` (frozen versioned docs; `main_versions.json` = `["0.0.0"]`) | `midnightnetwork/proof-server:latest` (old Docker namespace; `docs/_develop/tutorial/1-using/proof-server.mdx:41,66`); `compact-v0.2.0` installer URL (`0.0.0/getting-started/installation`); toolchain 0.24.0/0.23.0/0.22.0 (`0.0.0/develop/tutorial/building`) |
| `/sdks/community/nightforge` | default `midnightntwrk/proof-server:7.0.0` (community tool; page points to the matrix) |

Accurate-but-easy-to-misread (no fix needed): `/concepts/identity` honestly states the DID/VC packages target ledger-8 toolchain 0.31.1 and can't transact on the ledger-9 networks; `/troubleshoot/compiler-errors` uses 0.31.1/0.36.0 as worked examples; `/sdks/community/openzeppelin-compact-contracts` shows historical compile output and points to the matrix.

## What I did NOT verify

- I did not pull or run any Docker image (Docker Hub tag existence was checked via the Hub API only), did not compile a contract, and did not start a local network. Nothing here claims a local run.
- I did not verify the per-network RPC/GraphQL endpoints (that's the relnotes "environments" reference, not a version).
- I did not inspect the Lace 2.5.2 extension binary to confirm which DApp Connector API version it reports as `apiVersion`; the docs reference 2.4.2/2.4.3 behaviour only.
- I did not read the `@midnightntwrk/midnight-js` 5.0.2 changelog — it is flagged as "newer than the matrix, unverified," not recommended.
- The `midnight-local-dev` `standalone.yml` pin list could not be fetched this cycle (connection reset mid-batch); the repo and its "versions pinned in `standalone.yml`" description are verified, the individual pins are not.

## What Muse should check

1. Whether the unlabelled stale pages in category C (nextjs-wallet-connect, node-toolkit check commands, dapp-connector api-ref header, leaderboard, zkloan) are being fixed by the docs team — they are the ones a builder would trust.
2. Whether `@midnightntwrk/midnight-js` 5.0.2 (no-hyphen scope) is a supported successor path or an internal mirror — worth one question to the Midnight team before anyone pins it.
3. Whether "Compact devtools 0.5.1" in the matrix vs 0.5.3 published on GitHub matters (devtools are network-independent per their release notes — likely fine).

## For Sol (unblocks SOL-1/SOL-2)

- Pin exactly: toolchain `0.36.1`, proof server image `9.0.2`, `@midnight-ntwrk/midnight-js@5.0.1` (plus `@midnight-ntwrk/compact-js@3.0.2`, `@midnight-ntwrk/compact-runtime@0.21.0`, `@midnight-ntwrk/platform-js@3.0.0`, `@midnightntwrk/ledger-v9@1.0.2`, `@midnightntwrk/onchain-runtime-v4@4.0.2`, `@midnightntwrk/wallet-sdk@2.0.1`, `@midnightntwrk/dapp-connector-api@4.1.0`), node `2.1.101` / indexer `4.4.101`.
- Start from `midnight-examples` (learning path, ledger-9 pinned, CI-verified) — **not** `create-mn-app` (archived).
- Treat every bannered ledger-8 guide in section B as historical; its code will not run against the current networks as written.
- Local network: `midnight-local-dev` (`undeployed`); preview/preprod for testnet work, labelled testnet.
- Windows: contract compilation via WSL only.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
