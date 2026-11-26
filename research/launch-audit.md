# GROK-16 — Pre-launch audit of the whole repo

**Checked:** 2026-11-25 (America/Chicago), about 21:10–22:30 CT
**Checked by:** Bob Research (Grok swarm), read-only. Findings only: nothing outside this brief, the guide row, the board rows and the guard test was edited.
**Repo state audited:** `main` at `aa44411` (after #75–#79), re-read after a rebase. Android prover v0.3.1 / versionCode 4 (`apps/android-prover/app/build.gradle.kts:15-16`); Victor's GROK-17 polish is **not** on `main` yet, so every Android finding below is against v0.3.1.
**Networks:** no chain was touched. Preview and Preprod are public **testnets**; Mainnet is production.

Status words: **PASS** = every check on that surface passed. **PASS (notes)** = passes the packet's checks, with a low-severity note in section 3. **FAIL** = at least one check failed; the defect id points to section 3.

## 1. Verdict

The repo is **not ready to advertise as-is, for three reasons**, none of which is a tip-address or pin problem:

1. **One launch draft claims more than the evidence.** Post 1 of `launch-assets.md` says the Android prover "proves on this phone". Nobody has run it on a phone (D1).
2. **The Android app on `main` still has the open v0.3.1 defects** (five P0s from Bob's UX review, Linda's 28 GROK-14 fixes) and the setup script starts a proof server that listens on every interface (D2, D3). GROK-17 is the fix path; the launch copy should wait for it.
3. **The hub still labels Proof Station v0.1.2** (main is v0.2.0) and does not mention the Android prover at all (D6, D7).

What passed cleanly: both tip addresses are byte-exact on every surface that shows them (26 of 27 surfaces; the 27th, the Android app screen, shows neither, see D3). `@kshot9000` is on all 27. The independence line is on 26 of 27 (D4). Every stack pin on the current-pin surfaces matches today's compatibility matrix. All 195 internal links resolve. No personal-information marker (home address, phone, email, ID, payout wallet) was found in any file.

## 2. Method and re-verified pins

- **Surfaces:** every public surface in the packet: the hub (`sites/hub/index.html` plus the live GitHub Pages copy), the root README, all app READMEs (Proof Station, DUST Planner, Android prover and its server-setup, NightAttest), the in-app screens that show attribution (DUST Planner page, Proof Station window, Android `MainActivity.kt`), and all 17 files in `research/` (16 briefs plus the reading guide) as they stood before this brief was added: 27 surfaces in all.
- **Mechanical checks (scripted, rerun by the guard test where noted):** exact addresses, `@kshot9000`, the independence line, any other `addr1…` or `mn_shield-addr1…` string (none), Preview/Preprod sentences with no "testnet" within two sentences, internal links and anchors, repo paths in backticks, stale-pin mentions with no stale/ledger-8 wording nearby, and every external URL.
- **Judgement checks:** the headline numbers and statuses on the hub, the READMEs and the launch drafts were traced to a merged brief, board row or official page. Claims I could not trace are in section 5.

**Pins re-verified today** against the official [support matrix](https://docs.midnight.network/relnotes/support-matrix) (Mainnet, Preview and Preprod tables are identical) and the registries:

| Pin | Value | Source opened 2026-11-25 |
| --- | --- | --- |
| Compact toolchain | 0.36.1 (language 0.28.0) | [support matrix](https://docs.midnight.network/relnotes/support-matrix); GitHub release `compactc-v0.36.1`, 2026-11-11 |
| Midnight.js | 5.0.1 | matrix; npm `@midnight-ntwrk/midnight-js` latest 5.0.1 |
| Proof server | 9.0.2 | matrix; Docker Hub tag `midnightntwrk/proof-server:9.0.2` (amd64 + arm64, pushed 2026-11-16; `latest` is the same push) |
| DApp Connector API | 4.1.0 | matrix; npm `@midnightntwrk/dapp-connector-api` latest 4.1.0 |
| Indexer | 4.4.101 | matrix; [midnight-indexer releases](https://github.com/midnightntwrk/midnight-indexer/releases) |
| Wallet SDK | 2.0.1 | matrix; npm `@midnightntwrk/wallet-sdk` latest 2.0.1 |
| Also matching | Compact runtime 0.21.0, Compact JS 3.0.2, Platform JS 3.0.0, on-chain runtime 4.0.2, ledger 9.1.0.1 on the networks, `@midnightntwrk/ledger-v9` 1.0.2 | matrix; [Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9); npm |

One upstream drift, already recorded in `stack-versions.md` and the glossary: the `@midnightntwrk/midnight-js` package (no hyphen) has published 5.0.2 and 5.0.3 (GitHub release v5.0.3, 2026-11-24). The matrix still pins 5.0.1, so the pin stays 5.0.1. Re-confirmed today.

## 3. Defect list

Severity: **High** = blocks advertising or is a safety/accuracy problem a visitor would hit. **Medium** = wrong or stale public statement. **Low** = polish or wording. Owner is the lane that should fix it; fixes are follow-up tasks, not part of this PR.

| Id | Sev | Surface | Finding | Evidence | Owner / proposed follow-up |
| --- | --- | --- | --- | --- | --- |
| D1 | High | `research/launch-assets.md` post 1 | "Android prover v0.3.1 **proves on this phone**" reads as a verified on-phone result. Nothing has run on a phone: the same file says "No arm64 phone install or on-device timings yet" (long-form) and the app README says "Not yet benchmarked on a phone". The only proof run is Muse's amd64 sandbox. | `launch-assets.md:57` against `:109` and `:134`; `apps/android-prover/README.md:47`; board CLAUDE-AP-4 and CLAUDE-AP-5 (verified on amd64 only) | Linda: reword to "built to prove on the phone, not yet run on one", or hold post 1 until Kshot's supervised Fold7 run. |
| D2 | High | `apps/android-prover/server-setup/` | The proof server 9.0.2 has **no bind-address option** and listens on `0.0.0.0` with permissive CORS and no authentication. `setup-proof-server.sh` launches it with no arguments, so the server should accept connections from other devices on the same network (not tested on a phone, where Wi-Fi client isolation or a firewall could block it). Neither README says so. See the sources in the table below this one. | [`lib.rs:54`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/lib.rs#L54) (`.bind(("0.0.0.0", port))`), [`lib.rs:47`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/lib.rs#L47) (`Cors::permissive()`), [`main.rs:31-58`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/main.rs#L31-L58) (flags: port, verbose, job capacity, workers, job timeout, no-fetch-params; no host), [official local-proving guide](https://docs.midnight.network/guides/local-proving) ("no authentication and no TLS… listens on every interface inside its container… Publish its port on `127.0.0.1` only"), `setup-proof-server.sh:122-128,136` | Victor (GROK-17): say it plainly in the setup README, tell users to use trusted networks only. Kyle: an upstream issue or PR asking for a host option is an externally visible step, so it needs your go-ahead. |
| D3 | High | Android app on `main` (v0.3.1) | Open defects reproduced against proof server 9.0.2 in Bob's UX review (posted 2026-11-25; not in the repo): P0-1 a busy server (`/ready` 503) is shown as unreachable; P0-2 Cancel during pre-flight freezes the screen in PROVING; P0-3 the server URL field rewrites itself as you type; P0-4 no theme and no icon (action bar over the UI, white launch window, default icon); P0-5 private witness data can go over plain HTTP to any host (Kyle's decision: HTTP only to `127.0.0.1`/`localhost` by default, LAN by explicit opt-in, everything else https). Also the About line says "donation addresses" and shows neither address. | UX review §5; [`android-prover-qa.md`](android-prover-qa.md) fixes 1–28; `MainActivity.kt:400-401` | Victor, GROK-17. All five P0s are required before Joe merges. Re-audit the app surfaces after GROK-17 lands. |
| D4 | Medium | `research/dust-registration.md` | The footer has both addresses and `@kshot9000` but **no independence line**. It is the only one of 27 surfaces without it. | `dust-registration.md:229-232` (footer) | Hermes' file, any seat may fix via PR: append the exact line. |
| D5 | Medium | `research/dust-registration.md` §7.2 and §14 item 1 | Calls the funding guide's SDK set "current": a stale Wallet SDK 1.2.0 with stale Midnight.js 4.1.1 packages. The live guide now pins Wallet SDK **2.0.1** and `midnight-js-protocol` **5.0.1**, so the brief states a stale set as current and its code excerpt may no longer match the guide. | `dust-registration.md:100,199`; live [acquire-tokens guide](https://docs.midnight.network/guides/acquire-tokens) (install block pins `@midnightntwrk/wallet-sdk` 2.0.1) | Hermes' lane, a small PR: re-read the guide, update §7.2, add a dated correction note. |
| D6 | Medium | `sites/hub/index.html` Proof Station card | Shows **"Windows x64 · v0.1.2"** (line 53) and its "Build and test results" link (line 98) opens workflow run 45300060466 (commit `2567da6`, csproj `0.1.2`). `main` is **v0.2.0** (`MidnightProofStation.csproj:10`, `apps/proof-station/README.md:3`, launch post 2). The "Download Windows x64 ZIP" button itself goes to the workflow page (lines 57-58), so it reaches the newest run, and the "Setup guide" link opens the v0.2.0 README. A v0.2.0 run with unexpired artifacts exists: run 45518872521 on `main` (`3915c9f`). | `gh api` run and artifact records, 2026-11-25; the live Pages copy is byte-identical to `main` | Sol's lane (hub) or Kshot's call: update the version label and the results link, or keep v0.1.2 deliberately. |
| D7 | Medium | Hub and root README | Neither mentions the Android prover (zero matches for "android"). The README's repository table and "What I'm building" list omit it too. Known open item. | `grep -ci android README.md sites/hub/index.html` = 0 | Sol (hub) on her return; README row can go in the next docs PR. |
| D8 | Low | Root README and hub | "Compact privacy guide" links `docs.midnight.network/guides/build-a-private-smart-contract`. Today that official page carries the banner "Ledger 8 versions … This page still uses ledger 8 versions" and teaches the stale compiler. The link is fine for concepts but is not labelled. | `README.md:8`; `sites/hub/index.html:332`; live page header | Sol/Kshot: label the link "(page still shows ledger 8 versions)" or point at a ledger-9 page. |
| D9 | Low | Hub Proof Station card (lines 55, 74, 86, 96) and `MainWindow.xaml` (lines 199, 365) | "Preview" appears with no "testnet" nearby. The hub hero does say "Preview & Preprod testnets" (line 45), and the Proof Station README says "Preview testnet" up front. | scripted two-sentence check | Sol / Hermes: add "(testnet)" on first mention in each block. |
| D10 | Low | `research/launch-assets.md` post 3 and long-form | "Official NIGHT bridge still off" is accurate per the official docs today ([Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9), "Turning it on is a separate governance action… Don't rely on bridging NIGHT until then"), but the brief itself says the Mainnet on-chain check is still pending a Blockfrost token. | `bridges-status.md:43`; ledger-9 page opened today | Linda: say "per Midnight's docs". |
| D11 | Low | `research/ecosystem-directory.md` row 143 | Records a 2026-11-23 discrepancy: the Mainnet RPC host still answered. Today `rpc.mainnet.midnight.network`, `indexer.mainnet.midnight.network`, `indexer.preprod.midnight.network` and `rpc.preprod.midnight.network` all return HTTP 410 `ENDPOINT_DECOMMISSIONED`. The discrepancy is closed. | live probes 2026-11-25; [network endpoints](https://docs.midnight.network/relnotes/network) | Add a dated note to the row. |
| D12 | Low | Hub NightAttest card | Status is "Spec / Grant proposal drafted · Testnet build pending" while `apps/nightattest` has a simulator-tested contract. This understates the work. It is not an overclaim. | hub line 146; `apps/nightattest/README.md` | Sol/Kshot: optional. |
| D13 | Low | `research/proof-station-qa.md`, `research/android-prover-landscape.md` | Backtick paths that do not resolve from the repo root: `tests/attribution.py` and `tests/proof-result.sh` (they live in `apps/proof-station/tests/`), and `scripts/native/native-build-config.json` (an upstream path). | scripted path check, 85 paths, 3 misses | Add the prefix or say "upstream". |
| D14 | Low | Android README and setup README | Linda's GROK-14 list also includes the unsourced "~35 MB" first-boot figure and the missing phantom-process-killer warning. Cited, not redone. | [`android-prover-qa.md`](android-prover-qa.md) S7 and fix list | Victor, GROK-17. |

**Owner decision, not a defect:** the owner's full-name byline appears as a public attribution line in six public files (root README, the hub, the DUST Planner page, the Proof Station window, and `ecosystem-directory.md`). The packet bars personal information about the owner in files and PRs, so this brief does not repeat the name. If the byline is meant to stay, nothing to do; if not, it is a repo-wide change for Kyle or Kshot to order.

### Sources for D2 (proof server bind address, Part A of the order)

| Question | Answer | Source |
| --- | --- | --- |
| Is there a CLI flag, env var or config to bind to `127.0.0.1` only? | **No.** The only environment-backed options in 9.0.2 are `MIDNIGHT_PROOF_SERVER_PORT`, `_VERBOSE`, `_JOB_CAPACITY`, `_NUM_WORKERS`, `_JOB_TIMEOUT` and `_NO_FETCH_PARAMS`. | [`proof-server/src/main.rs` at tag `proof-server-9.0.2`, lines 31-58](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/main.rs#L31-L58) |
| What is the default bind? | `0.0.0.0` on the chosen port (6300 by default), hard-coded. The same line is on `ledger-9` and `main`, so a newer tag does not change it. | [`proof-server/src/lib.rs:54`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/lib.rs#L54) |
| What do the official docs say? | "It listens on every interface inside its container… Publish its port on `127.0.0.1` only." The container approach works because `docker run -p 127.0.0.1:6300:6300` limits the published port. A no-Docker install has no such wrapper. | [Proving transactions locally](https://docs.midnight.network/guides/local-proving) |
| How does the setup script launch it? | With no arguments: the generated launcher is `exec "$LOADER" --library-path "$LIBPATH" "$BIN" "$@"` and `--start` runs it bare, then probes `http://127.0.0.1:6300/version`. | `apps/android-prover/server-setup/setup-proof-server.sh:122-128,136-139` |

## 4. Pass/fail per surface

Columns: **Addr** = both tip addresses byte-exact, with no other `addr1…` string; **Tag** = `@kshot9000`; **Indep** = the independence line exactly; **Pins** = no stale pin presented as current; **Testnet** = Preview/Preprod labelled testnet; **Links** = internal links and repo paths resolve; **Claims** = headline claims trace to evidence or sit in section 5.

| Surface | Addr | Tag | Indep | Pins | Testnet | Links | Claims | Verdict |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `README.md` | pass | pass | pass | pass (checked 2026-11-23, still current) | pass | pass | pass | PASS (notes: D7, D8) |
| `sites/hub/index.html` (and live Pages copy) | pass | pass | pass | n/a | pass (notes: D9) | pass | **fail**: v0.1.2 label (D6) | FAIL (D6, D7; notes: D8, D9, D12) |
| `apps/dust-planner/index.html` | pass | pass | pass | n/a | pass | pass | pass: model values trace to [manage-dust](https://docs.midnight.network/guides/manage-dust) (cap ratio, 604,815 s, 3 h grace window) and `dust-registration.md` | PASS |
| `apps/dust-planner/README.md` | pass | pass | pass | pass | pass | pass | pass | PASS |
| `apps/proof-station/README.md` | pass | pass | pass | pass | pass (notes: D9) | pass | pass: 1.168 s / 0.766 s / 38 MSM calls trace to [PR #4 comment](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/4#issuecomment-7125334907) | PASS (notes) |
| `apps/proof-station/MainWindow.xaml` (in-app text) | pass | pass | pass | n/a | notes: D9 | n/a | pass | PASS (notes) |
| `apps/android-prover/README.md` | pass | pass | pass | pass | n/a (names no network) | pass | open items in GROK-14 | PASS (notes: D14; see D3) |
| `apps/android-prover/server-setup/README.md` | pass | pass | pass | pass | n/a | pass | **fail**: silent on the all-interfaces bind (D2) | FAIL (D2, D14) |
| `apps/android-prover/.../MainActivity.kt` (in-app text) | **fail**: neither address, "donation addresses" wording | pass | pass | n/a | n/a | n/a | open items in GROK-14 | FAIL (D3) |
| `apps/nightattest/README.md` | pass | pass | pass | pass (Compact 0.36.1, runtime 0.21.0) | n/a | pass | pass: claims only a local simulator pass | PASS |
| `research/README.md` (guide) | pass | pass | pass | pass | pass | pass | pass | PASS |
| `research/stack-versions.md` (HERMES-1) | pass | pass | pass | pass: every table value matches the matrix today; stale versions only in the labelled stale-pages sections | pass | pass | pass | PASS |
| `research/glossary.md` (GROK-13) | pass | pass | pass | pass: stale values only in the flagged section | pass | pass | pass | PASS |
| `research/bridges-status.md` (GROK-6) | pass | pass | pass | n/a | pass | pass | pass (Mainnet switch check is listed as pending) | PASS |
| `research/catalog-partner-status.md` (GROK-7) | pass | pass | pass | n/a | pass | pass | pass (blocked hosts recorded, not claimed) | PASS |
| `research/compact-patterns-outline.md` | pass | pass | pass | pass: ledger-8 pages listed as stale | pass | pass | pass | PASS |
| `research/dust-registration.md` (HERMES-5) | pass | pass | **fail** (D4) | **fail** (D5) | pass | pass | pass | FAIL (D4, D5) |
| `research/ecosystem-directory.md` (GROK-1) | pass | pass | pass | pass: ledger-8 values only in the "do not use" row | pass | pass | pass (notes: D11; unreachable hosts recorded as unverified) | PASS (notes) |
| `research/example-patterns.md` (HERMES-2) | pass | pass | pass | pass: stale values flagged | pass | pass | pass | PASS |
| `research/local-stack-verification.md` (HERMES-3) | pass | pass | pass | pass | n/a (local `undeployed` devnet) | pass | pass: the `srs.midnight.net` lines are corrected (see section 6) | PASS |
| `research/proof-station-qa.md` (GROK-10) | pass | pass | pass | pass | pass | pass (notes: D13) | pass | PASS (notes) |
| `research/qa-sweep-2026-11.md` (GROK-3) | pass | pass | pass | n/a | pass | pass | pass | PASS |
| `research/wallet-prover-e2e.md` (GROK-12) | pass | pass | pass | pass: the stale connector 4.0.1 vs 4.1.0 pin drift is called out | pass | pass | pass | PASS |
| `research/android-prover-feasibility.md` (CLAUDE-AP-1) | pass | pass | pass | pass | n/a | pass | pass | PASS |
| `research/android-prover-landscape.md` (GROK-11) | pass | pass | pass | pass: third-party ledger-8 pins described as such | pass | pass (notes: D13) | pass: no phone numbers claimed | PASS (notes) |
| `research/android-prover-qa.md` (GROK-14) | pass | pass | pass | pass | pass | pass | pass | PASS |
| `research/launch-assets.md` (GROK-15) | pass | pass | pass | pass | pass | pass | **fail**: post 1 (D1); note D10 | FAIL (D1; note D10) |
| `research/in-app-prove-verification.md` (HERMES-7) | pass | pass | pass | pass: 9.0.2, v0.7.1 and ledger 1.0.2 all live-verified 2026-11-26 | n/a (no chain interaction; payload labelled devnet-captured) | pass (release URL live-verified) | pass: every timing labelled EMULATOR, no phone figures claimed | PASS |

Totals: 28 surfaces audited: **16 PASS, 7 PASS (notes), 5 FAIL**. The five FAILs are the hub (D6, D7), `server-setup/README.md` (D2), the Android app screen `MainActivity.kt` (D3), `dust-registration.md` (D4, D5) and `launch-assets.md` (D1).

## 5. Claims traced to evidence, and links

**Headline claims that traced cleanly:**

| Claim (surface) | Evidence |
| --- | --- |
| RTX 3090, k=16, cold 1.168 s, warm 0.766 s, 38 CUDA MSM calls, 0 fallbacks, stock verifier passed (hub, Proof Station README, launch posts 2 and long-form) | Hermes' run on [PR #4](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/4#issuecomment-7125334907); the raw `STATION_PROOF` row reads 1.168362619 / 0.765928636 / 0.001643113 / 38 / 0 fallbacks. The hub's separate "Original RTX 3090 benchmark" link (k=21, 5.352 s cold / 4.370 s warm) is a different workload and is labelled as such in the Proof Station README. |
| DUST model: cap 5 DUST per NIGHT, 604,815 s to cap, 3-hour validity window not a decay delay (DUST Planner) | [manage-dust](https://docs.midnight.network/guides/manage-dust) parameter table: `nightDustRatio` 5,000,000,000, `generationDecayRate` 8,267, `timeToCapSeconds` derived and rounded up (5,000,000,000 ÷ 8,267 = 604,814.6, so 604,815). |
| Network ledger 9.1.0.1, node 2.1.101 (root README) | [Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9) networks table. |
| Proof server container command binds the published port to loopback (root README) | [local-proving guide](https://docs.midnight.network/guides/local-proving): `-p 127.0.0.1:6300:6300`. |
| Android prover: real 1,509 B DUST payload → 2,940 B proof in 2.56 s, sandbox CPU, "not a phone time" (launch long-form) | Board CLAUDE-AP-4, [`android-prover-qa.md`](android-prover-qa.md) NOT verified section, `apps/android-prover/README.md`. |
| No phone benchmark, thermal or battery figure anywhere | Searched README, hub, both Android READMEs, launch drafts and glossary: only "not yet benchmarked" statements. |
| Both X-draft lengths ≤ 280 | Recomputed with URLs weighted at 23: posts 279, 279, 264, 245. `@IOHK_Charles` appears in exactly one draft. |

**Links.** 335 unique external URLs were extracted from the 25 documents and pages (the in-app files `MainWindow.xaml` and `MainActivity.kt` were checked for attribution only).

| Result | Count | Notes |
| --- | --- | --- |
| HTTP 200 or 202 (direct) | 217 | |
| `docs.midnight.network` | 61 | Vercel's bot challenge answers 429 to curl on HTML pages, so each page was opened as its `.md` twin: all 61 answered 200. |
| npm package pages | 9 | Answer 403 to curl; each confirmed through the npm registry (versions in section 2). |
| GitHub web URLs | 4 | Rate-limited as HTML; each file confirmed through `gh api`. |
| Live endpoints that refuse a plain GET | 2 | `indexer.preview.midnight.network/api/v4/graphql` and `rpc.preview.midnight.network` answer 405 (POST-only, so up). |
| Retired Midnight-hosted endpoints | 3 | `indexer.mainnet`, `indexer.preprod` and `rpc.preprod` answer HTTP 410 `ENDPOINT_DECOMMISSIONED`, as the official [network endpoints](https://docs.midnight.network/relnotes/network) page says. `rpc.mainnet.midnight.network` (not a link in the repo, but named in two briefs) answers 410 too (D11). |
| Reopened with a browser-style fetch | 6 | `midnight.network/faq`, `/roadmap`, `/ecosystem-catalog`, `midnight.fun`, `academy.midnight.network` and `midnightexplorer.com` loaded (curl had got 429). |
| Not reachable from this box | 5 | `iamx.id`, `midnight.meshjs.dev`, `nufi.io`, `wallet-prover.zkfold.io`, and `srs.midnight.net` (the wrong parameter host, now corrected in the briefs; the glossary records it). All fail TLS from here (this box's DNS answers `198.18.0.1` for them) and a second network also failed or timed out. The briefs already record each as unverified or failed, never as live. |
| Bot-blocked or rate-limited, not reopened | 21 | 403 or 429 from nine `midnight.network/blog/*` posts, `midnight.fun/dapps`, `preview.` and `preprod.midnightexplorer.com`, `www.midnight.gd/redeem` (403 on both fetches), `app.houdiniswap.com`, `tokeo.io`, `www.dega.org`, the Fold7 spec page, the bare `srs.midnight.network/` root (403, the host is up) and `dynamic.xyz` (500 after its redirect to fireblocks.com). The source briefs opened these on 2026-11-23 or 24; I did not reopen them today. |
| Local or example addresses | 7 | `127.0.0.1` and `192.168.1.20`, not links to check. |
| Internal links and anchors | 195 | All resolve. The six scripted "misses" were cache-key query strings and prose ellipses. |

## 6. Known open items from the packet

- **HERMES-3 `srs.midnight.net` lines: closed.** The packet listed them as open. PR #77 corrected all three mentions in `local-stack-verification.md` to `srs.midnight.network` and kept the raw log line untouched with a dated note (`local-stack-verification.md:94`). The host is `srs.midnight.network`; `srs.midnight.net` gave no HTTP response from this box today.
- **Hub Android prover section: still open (D7).** Sol's lane.
- **GROK-14's fix list: open.** Merged as the brief in #76; its 28 fixes and 43 proposed strings are GROK-17's input (D3, D14).

## 7. NOT verified

- **Nothing was run on a phone or an emulator.** No Android build, install, screen walk-through or TalkBack check was done for this audit. Android findings come from code, Bob's earlier UX review (which did run the app's own client code against a local 9.0.2 proof server on amd64) and Linda's GROK-14.
- **Not audited line by line:** the 17 research briefs were checked mechanically on every surface, and by claim only where a claim is public-facing (hub, READMEs, launch drafts) or conflicted with a live source. Each brief's own sources are as of its own "Checked" date.
- **Not reopened today:** the 21 bot-blocked or rate-limited external URLs in section 5, and every third-party product page (catalogue, bridges and landscape briefs). Those stay at the date in their briefs.
- **Not checked:** the CI workflows, the `.github/` files, and the `council/` coordination files other than the board rows this PR edits. The CUDA numbers were traced to the recorded run, not re-run. The on-chain Mainnet NIGHT-bridge switch needs a Blockfrost token (For Kyle, see `bridges-status.md`). No wallet was connected and nothing was signed.
- **Not tested live:** the DUST Planner page in a browser and the hub's filters and copy buttons beyond what `tests/hub.test.mjs` asserts.

## 8. For Kyle

1. **D1:** hold launch post 1, or approve the "not yet run on a phone" wording, until Kshot's Fold7 run.
2. **D2:** decide whether to file an upstream request for a proof-server host option (visible outside the repo), and whether the setup README should tell phone users to avoid public Wi-Fi.
3. **D6:** decide whether the hub label and results link should move to v0.2.0.
4. **Byline:** confirm the full-name attribution line in six public files is intended.
5. Fixes for D4, D5, D8–D13 are small and can be one follow-up PR each lane; GROK-17 owns D3 and D14.

---

Attribution on ALL work: both tip addresses, character-for-character, alongside @kshot9000.

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000)

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
