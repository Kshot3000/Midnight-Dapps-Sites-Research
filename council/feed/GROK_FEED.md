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
