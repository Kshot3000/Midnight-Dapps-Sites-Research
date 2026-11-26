# Grok swarm — feed (from Muse, updated 2026-11-23)

Welcome to the team. Rules and lanes are in your start prompt and on `council/board.md` — claim on the board first, branches `grok/<task-id>-<slug>`, PRs against `main`, Muse reviews and merges.

**Wave 1 (GROK-1..3) is done and merged — thank you.** Wave 2 is below: GROK-4..7, all approved and queued on the board as Unclaimed. Claim one on the board first, one task per PR. GROK-4 and GROK-5 are small tooling jobs; GROK-6 and GROK-7 are research sweeps (Bob's proposals). There is also one small follow-up on GROK-2 at the end.

## GROK-1 — Midnight ecosystem directory sweep

Build `research/ecosystem-directory.md`: a verified catalogue of what actually exists in the Midnight ecosystem today — live dApps, games, explorers, wallets, developer tools, faucets, indexers, bridges (note status honestly if a thing is announced/testnet/dead).

For each entry: name, one-line what it does, live URL (verified by opening it — record the check date), network (mainnet / Preview / Preprod), and the privacy pattern it uses if any (shielded tokens, selective disclosure, private state + ZK, none/public).
Close with: dead or stale entries you found listed elsewhere but could not verify, and gaps — categories with nothing live in them.

Discipline: an entry you did not open does not go in as live. Both addresses + @kshot9000 + independent-builder line in the footer, exact.

## GROK-2 — DUST Planner Pro (build, to spec)

A small front-end tool in `apps/dust-planner/` (static HTML/CSS/JS, same family look as the hub, no build step):

- Inputs: NIGHT amount, whether it is registered, days since registration, planned spend.
- Model (official mechanics, from `research/stack-versions.md` and the DUST docs): cap = 5 DUST per 1 NIGHT; generation ramps roughly linearly over ~1 week to cap; DUST decays after the backing NIGHT is spent or deregistered; fees are paid in DUST; unregistered NIGHT generates nothing — the tool must say so loudly when "registered" is off.
- Every output is a **modelled estimate and must be labelled as one** on the page itself. No live wallet connection, no keys, no transactions — a planner, not a wallet.
- Tests (node, like `tests/hub.test.mjs`): both donation addresses exact in the footer, the estimate labelling present, the model math (cap, ramp) asserted on fixed cases. All tests must pass on your tree — report the real count.

## GROK-3 — Repo QA sweep

One pass over the whole repo as it stands: every external link in README/board/backlog/hub checked live; hub accessibility pass (contrast, focus order, alt text, keyboard) on desktop + mobile widths; anything broken filed as a small fix PR (one issue per PR) or, if it's a judgement call, listed in `research/qa-sweep-2026-11.md` for Muse to triage. Do not redesign anything — Sol owns design. Report what you checked, what you fixed, what you left and why.

---

## Wave 2 — claim on the board first

## GROK-4 — Test-runner fix so every test file runs

Branch: `grok/GROK-4-test-runner`

Today the suites only all run if someone names every file. Fix that:

- Add a root `package.json` with a `test` script that runs `node --test tests/*.test.mjs`, so `npm test` runs the hub, ecosystem-directory and dust-planner suites (and any future `*.test.mjs`) in one pass.
- `tests/hub.test.mjs` currently imports the other test files manually so they run inside its process — drop that manual import once the runner covers them, so nothing runs twice. Keep every existing test itself unchanged.
- Leave `tests/hub.browser.cjs` to Playwright — it is not part of the node runner.
- Do **not** touch `.github/workflows/` — the Grok token cannot push workflow files, and the workflow command/path filters stay with Muse. If the workflow needs a matching change, say so in the PR and I will land it.
- Acceptance: `npm test` on your tree runs all three suites with the real total reported in the PR (45 tests on current main), 0 failures, no test counted twice.

## GROK-5 — Automated link checker for the ecosystem directory

Branch: `grok/GROK-5-directory-link-checker`

A small test or script (your call — say which and why in the PR) that guards `research/ecosystem-directory.md`:

- Every directory row must keep: a description, a network label, a privacy pattern, and an `https` source URL. A row missing any of those fails the check.
- Plus an on-demand live fetch of those URLs that records status and check date. A flaky fetch (timeout, 403 bot-block, 429) is **recorded, not a CI hard-fail** — the structural check is the gate, the live fetch is a report.
- Acceptance: the checker passes on the current directory, and you show it catching a deliberately broken row (missing field or non-https URL) in the PR description.

## GROK-6 — Bridge and cross-chain status sweep (Bob)

Branch: `grok/GROK-6-bridges-status`

Deliverable: `research/bridges-status.md`. GROK-1 found no live bridge into Midnight — verify that properly. For each candidate (VIA Labs, Pintent, cNgD, and any other announced Midnight bridge you find):

- Classify it: live / testnet / announced only / dead, with dated evidence from pages you actually opened (URL + check date).
- Nothing is listed as live unless it was opened and usable **without a wallet signature**. No wallet signing, no mainnet funds, no sign-ups — if using it needs one of those, record that as the finding and stop there.
- Label Preview / Preprod as testnet wherever they appear. Close with what the gap means for a user who wants to move value onto Midnight today.
- Footer: both addresses + @kshot9000 + the independent-builder line, exact, same as GROK-1.

## GROK-7 — Official-catalog partner verification (Bob)

Branch: `grok/GROK-7-catalog-partner-status`

Deliverable: `research/catalog-partner-status.md`. For each partner card in the official Midnight ecosystem catalog (e.g. PLAY NATION, Paima, DEGA, NMKR, NorthStar, CSWAP — cover the full catalog, not just these):

- Classify each as: live product / testnet demo / announcement-or-partnership only, with a source URL you opened and the check date.
- Where a partner's status contradicts how `research/ecosystem-directory.md` lists it, flag the contradiction in your file — do not edit the directory in this PR; the corrections land in a follow-up once I have reviewed the evidence.
- Footer: both addresses + @kshot9000 + the independent-builder line, exact.

## Follow-up on GROK-2 (small, whoever claims GROK-4 or GROK-5 can take it as a second PR)

The DUST planner model exports `DUST_GRACE_PERIOD_SECONDS` (3h) but the decay timing does not apply it. Verify against the official manage-dust formula: either apply the grace period in the model (with a test), or document in the code and the app README why the guide's formula omits it. One small PR, branch `grok/GROK-2-grace-period`.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*

## Wave 3 — 2026-11-24 (Kyle: the prover line keeps moving while Hermes is paused)

### GROK-10 — Proof Station docs + QA sweep
- **Deliverable:** `research/proof-station-qa.md` — an audit of `apps/proof-station/README.md` against the app code and the verified matrix (Compact 0.36.1 / Midnight.js 5.0.1 / proof server 9.0.2 / connector 4.1.0 / indexer 4.4.101; station pins: node 2.1.101, proof server 9.0.2).
- Check every command, path and version named in the README; check the Windows setup steps against `research/local-stack-verification.md` (HERMES-3) and `research/stack-versions.md`.
- Output = findings + a fix list. Fixes are proposals for a later task — do not change the app or README in this PR.
- **Honesty rule:** anything needing a GPU, a Windows machine, or a running node goes in a NOT-verified section. Claim first on the board, one PR, branch `grok/GROK-10-proof-station-qa`.

## Wave 4 — 2026-11-24 (Kyle's order: take the Midnight prover to Android — the whole team's next mission)

Context for all three tasks: Charles Hoskinson's GPU prover work (midnight-zk PR #667, which this team tested on the RTX 3090) takes proving off the CPU. The next frontier is the phone: a Midnight prover that runs **on an Android device**, so a wallet user never has to trust a server with their witness data. Kyle has made this the team's next mission for every seat — Sol designs the app (SOL-AP-1, on the board), Muse runs the technical feasibility spike (CLAUDE-AP-1, on the board), and the swarm takes the research sweep below plus the queued wallet E2E.

## GROK-11 — Android prover research sweep

Branch: `grok/GROK-11-android-prover-landscape`

Deliverable: `research/android-prover-landscape.md`. Survey mobile ZK proving as it actually exists today:

- The **Mopro** framework (mobile proving by the PSE ecosystem): which proving systems and curves it supports, its Android status, and whether Midnight's stack (ZKIR over BLS12-381, halo2-derived) fits it or not — with sources.
- Any existing Midnight or Cardano mobile-proving efforts (apps, repos, announcements) — status labelled honestly (live / testnet / announced / unverified).
- WASM proving on phones: any published benchmarks of halo2-class proving in mobile browsers/WebViews.
- Rust-to-Android proving projects (NDK, `aarch64-linux-android`) outside crypto wallets, if any set a precedent.
- Device reality: RAM and thermal limits for sustained proving, flagship vs mid-range, and any app-store or OS policy constraints on heavy background compute.
- Discipline: every claim comes from a page you actually opened, with the check date. Close with an honest gaps section — what nobody has published yet. Footer: both addresses + @kshot9000 + the independent-builder line, exact.

## GROK-12 — Wallet-built transaction body → real proof-server proof (E2E) — queued after GROK-11

Branch: `grok/GROK-12-wallet-prover-e2e`

Deliverable: `research/wallet-prover-e2e.md` + the captured evidence (client output + proof server log excerpt). Build an unproven transaction from a real wallet (your own wallet tooling, or the midnight-examples hello-world local flow with its seeded genesis wallet on the local `undeployed` network), submit the wallet-built body to a proof server **9.0.2** (`POST /prove-tx`, plus whatever /check + /prove calls the flow makes), and capture: HTTP status, which circuits were proven and their k, and the server-side timings from the proof server log. If you ran the full local stack, confirm the proven transaction lands; if you only got the proof, say plainly where it stopped. Muse is attempting the same E2E in parallel against its own running 9.0.2 server — check the board before starting; if Muse's run lands first, yours becomes the independent reproduction, which is still valuable and should say so. Local network + CPU prover labelled honestly; quote only numbers from your own captured output.

## Wave 5 — 2026-11-25 (GROK-11/12 landed; the glossary is next in the queue)

## GROK-13 — Plain-language Midnight glossary

Branch: `grok/GROK-13-glossary` · PR title starts "Grok:" · Claim GROK-13 on `council/board.md` first, one task in this PR only.

Deliverable: `research/glossary.md` — one alphabetical page a newcomer can actually read. Our briefs and READMEs use these terms without defining them; this page fixes that.

- **Terms (at minimum):** NIGHT, tNIGHT, cNIGHT, DUST, DUST registration, shielded vs unshielded, Preview vs Preprod, proof server, Compact, witness, ZSwap, ledger 9. Add a term only if a repo brief already uses it.
- **Per term:** a one- or two-sentence plain-language definition, an official source (docs.midnight.network or an official Midnight repo page) that you opened on the day, with its https URL, and a link to the repo brief that covers the term (e.g. DUST registration → `dust-registration.md`, bridges → `bridges-status.md`).
- **Discipline:** every definition sourced, nothing invented; where the official docs and a repo brief disagree, flag it in the entry instead of picking one silently. Preview / Preprod are testnets — label them testnet wherever they appear. No Ledger 8-era material; pins follow the verified matrix (Compact toolchain 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector 4.1.0, indexer 4.4.101).
- **Index + guard:** add a glossary row to `research/README.md`, and a test in the GROK-9 style asserting every term entry carries an https source, every repo-brief link resolves to a real file, and the new file contains both addresses byte-exact, @kshot9000, and the independence line.
- Footer: both addresses + @kshot9000 + the independent-builder line, exact, same as every other brief.
- No personal information about Kyle in any file, commit, or PR — ever.

## Wave 6 — 2026-11-25 (GROK-PROP-6 approved: the Android prover gets its docs sweep)

## GROK-14 — Android prover docs + copy-accuracy sweep

Branch: `grok/GROK-14-android-prover-qa` · PR title starts "Grok:" · Claim GROK-14 on `council/board.md` first, one task in this PR only.

Deliverable: `research/android-prover-qa.md` — the GROK-10 method applied to `apps/android-prover/` (v0.3.1, landed via PRs #60/#64/#69/#71 in a single day; nobody has docs-checked it yet).

- **Check every claim** in `apps/android-prover/README.md` and `apps/android-prover/server-setup/README.md` against the app code and the pin sheet: endpoints (`/version`, `/proof-versions`, `/ready`, per-circuit `POST /prove` — the deprecated `/prove-tx` JSON path must not be described as current), versions (proof server 9.0.2; app versionName 0.3.1 / versionCode 4), SDK levels, the server-setup steps (registry pull, staged loader, arm64 default / amd64 supported), and the honest-status wording. Known starting example: the README still says "v0.2 carries the full Midnight-themed design".
- **Output = findings + a fix list.** Fixes are proposals in the brief only — do not edit the app, its READMEs, or the setup script in this PR.
- **Hub gap:** note in the brief that `sites/hub/index.html` does not mention the Android prover yet. That is flagged for Sol's hub lane — do not edit the hub.
- **Honesty rule:** no phone runs and no benchmarks — anything needing a physical device goes in a NOT-verified section; on-device proving stays the owner's supervised step, and no phone proof times or battery figures may be claimed.
- **Index + guard:** add an android-prover-qa row to `research/README.md`, and guard tests in the GROK-9 style asserting the new brief exists, is indexed, and contains both addresses byte-exact, @kshot9000, and the independence line.
- Footer: both addresses + @kshot9000 + the independent-builder line, exact, same as every other brief.
- No personal information about Kyle in any file, commit, or PR — ever.

## Wave 7 — 2026-11-25 (Kyle's order: get this project DONE and ADVERTISED — launch push)

### GROK-15 — Advertising asset pack for the launch

Branch: `grok/GROK-15-launch-assets` · PR title starts "Grok:" · Claim GROK-15 on `council/board.md` first, one task in this PR only.

Deliverable: `research/launch-assets.md` — ready-to-use copy the owner can post as-is. Drafts only; posting itself is the owner's/Muse's step, not yours.

- **X posts (draft 4):** (1) Android prover works — v0.3.1, real per-circuit /prove protocol against official proof server 9.0.2, verified end-to-end with a real payload → real proof, no-Docker on-phone server installer; (2) Proof Station v0.2.0 — RTX 3090 measured k=16 cold 1.168 s / warm 0.766 s, 0 fallbacks, CPU-verifier passed; (3) the research library — glossary, DUST registration explainer, bridge status (VIA the only mainnet-evidenced route, official bridge off pending governance); (4) NightAttest first slice — Compact contract + simulator, honest scope. Each post ≤280 characters, tags @MidnightNtwrk and @IOHK_Charles, repo link https://github.com/Kshot3000/Midnight-Dapps-Sites-Research in at most two of them.
- **One long-form draft** (~1,500–2,500 characters) telling the whole story for a thread or community post: hub, Proof Station, Android prover, research library, what's NOT done (no phone benchmarks yet; NightAttest UI + live testnet run still to come).
- **Discipline:** verified milestones only — every number must trace to a merged brief or board entry; no phone performance claims; Preview/Preprod labelled testnet wherever named; no implied endorsement by the tagged accounts (credit/visibility only).
- Footer: both addresses + @kshot9000 + the independent-builder line, exact. Index the brief in `research/README.md` with a GROK-9-style guard test. No personal information about Kyle — ever.

### GROK-16 — Pre-launch audit of the whole repo

Branch: `grok/GROK-16-launch-audit` · PR title starts "Grok:" · Claim GROK-16 on `council/board.md` first, one task in this PR only.

Deliverable: `research/launch-audit.md` — the final sweep before the project is called done.

- **Every public surface checked:** hub pages, all app READMEs (Proof Station, DUST Planner, Android prover + server-setup), every research brief. For each: donation addresses byte-exact + @kshot9000 + independence line present; stack pins on the verified matrix (Compact toolchain 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector 4.1.0, indexer 4.4.101) with stale versions flagged as stale, never presented as current; Preview/Preprod labelled testnet; internal links resolve; no claim that exceeds its evidence (each claim cites the brief/board entry that backs it, or lands in a NOT-verified list).
- **Known open items to record, not fix:** HERMES-3's `srs.midnight.net` lines (Hermes' observed record, correction waits for her return or an addendum); the hub's missing Android prover section (Sol's lane, due on her return); GROK-14's fix list if still open.
- **Output = pass/fail per surface + a defect list.** Findings only — fixes land as follow-up tasks, not in this PR.
- Footer + index + guard test, same as GROK-15. No personal information about Kyle — ever.
