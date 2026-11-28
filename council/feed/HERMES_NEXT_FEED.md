> **▶ KYLE'S ORDER 2026-11-28 — MIDNIGHT ONLY; ATHENA PAUSED AGAIN.**
> Effective now, until HERMES-6 ships: **disable your 15-minute "Hermes
> Athena Work Loop" (disable, don't delete) and park the Athena queue
> exactly where it stands** — SEC5-R2-AUTHOR and everything behind it.
> Leave Athena branches and commits as they are; push no further Athena
> work. Note where the Athena queue is parked in your next board log
> line here, so it resumes cleanly when Kyle restarts it. This
> supersedes the 2026-11-27 Athena restart (Addendum 30) until Kyle
> says otherwise — a pause, not a rollback. **All cycles to Midnight:**
> finish HERMES-6 (24/54 cards at bc452bb — complete wave 1 at the same
> compile-gated bar and open the PR), then the green-lit upstream
> midnight-docs issue (5 findings), the srs.midnight.network correction
> addendum to HERMES-3, and the Preview db-sync check. Kyle's reasoning:
> Midnight is the primary project and HERMES-6 is the closest open item
> to done — finish it rather than splitting across two queues.

> **▶ KYLE'S ORDER 2026-11-27 — MIDNIGHT IS THE PRIMARY PROJECT; WORK IT 24/7.**
> Kyle is handing you a full restart prompt directly (24/7 cron on this repo,
> your queue, and the standing rules). When it arrives it supersedes the older
> banners below. Until then: **HERMES-6 is your next task** (Compact pattern
> cards, wave 1 → `research/compact-patterns/`, branch
> `hermes/hermes-6-compact-cards`, compile-gate every card against toolchain
> 0.36.1 / language 0.28). After HERMES-6: the green-lit upstream
> midnight-docs issue (5 findings), the srs.midnight.net correction addendum
> to your HERMES-3 brief (the host is srs.midnight.network), and the Preview
> db-sync check when db-sync exists on your machine. HERMES-7 is DONE and
> verified — thank you; the signing fix it surfaced shipped as CLAUDE-AP-9 /
> app v0.8.1.

> **▶ RESUMED — Kyle's order, 2026-11-26 ~06:29 CST.** The pause is lifted for Midnight Dapps — Kyle: "midnight is not paused hermes is working on finshing the project first." You are back on this project and finishing it FIRST, ahead of Poseidon. **Your priority finish task is HERMES-7** (in-app prove verification of the shipped Android prover v0.5.1 — run the release APK in your accelerated emulator against a live 9.0.2 server and land the evidence as a PR); Kyle has the full copy-ready prompt and will hand it to you directly. HERMES-6 resumes after HERMES-7, on its existing branch. The upstream docs issue is still green-lit. (Your Athena pause still stands — this resume is Midnight only.)

# Hermes — next feed (from Muse, updated 2026-11-24)

## ⚑ PRIORITY ORDER FROM KYLE (2026-11-24): Midnight first — no Athena for a week

Kyle's order: **for one week (through 2026-12-01), your priority is THIS project — HERMES-2 first, then HERMES-3, then HERMES-4/5. No Athena work this week.** The Athena queue waits; this catalogue and the local-stack verification are what the team needs from you now.

**Update 2026-11-24 (Muse): HERMES-2 is DONE — PR #21 reviewed clean and merged.** Thank you — the catalogue is the team's pattern inventory now. My answers to your three questions (gap list accepted, the 4.1.0 pin decision, ROSCA added to the backlog as idea #11) are on PR #21 and recorded on the board and pin sheet. **HERMES-3 below is your next task — claim it on the board when you start it.**

Also recorded: your RTX 3090 Proof Station results (posted on PR #4) are verified and on the board — thank you. The Preview db-sync check stays open on db-sync availability.

**Update 2026-11-24 (Muse, later): HERMES-3 is DONE — PR #24 reviewed clean and merged.** Excellent run: hello-world 2/2 + calculator 6/6 E2E, CPU proof ~0.50 s warm, and the 10 friction items are now Version Doctor input. My answers to your three questions are on PR #24: (1) yes — file the first-boot node crash upstream with the full log; (2) friction items 1–3 are also worth an upstream docs issue, batch or separate, your call; (3) the counter phantom is a docs problem, not a catalogue gap — no HERMES-2 change needed. **HERMES-4 below is your next task — claim it on the board when you start it.**

## HERMES-4 — Compact pattern library outline (NEXT)

Outline only → `research/compact-patterns-outline.md`. Distil the reusable Compact patterns from the HERMES-2 catalogue and your HERMES-3 hands-on work: for each pattern — name, the privacy job it does (what stays private / what is proven / what lands on the public ledger), the Compact features it uses (witnesses, `disclose()`, shielded tokens, commitments/nullifiers, sealed state), which examples demonstrate it, and which backlog ideas it feeds (NightAttest first — SOL-2 builds on this). Close with the gaps: patterns no example covers that our builds must invent (the HERMES-2 gap list is your starting inventory). Outline depth, not full code. Same PR rules: branch `hermes/hermes-4-compact-patterns`, draft PR, title "Hermes: HERMES-4 — …", `Authored-By: Hermes (local seat)` trailer, both addresses + @kshot9000 + independent-builder line exact in the footer. HERMES-5 (DUST registration explainer research) follows after.


This file now lives **in the repo** (Kyle's decision, 2026-11-23) so you read your feed directly here on your cycle — no courier step. HERMES-1 is reviewed clean (see PR #3). Your stack brief is the team's pin sheet.

Kyle's standing rule: anything that needs real hardware to test is yours (i9 / RTX 3090 / 128 GB).

## HERMES-2 — Example dApp privacy-pattern catalogue

Catalogue every example in `midnightntwrk/midnight-examples` (current ledger-9 tree) into `research/example-patterns.md`:

For each example: name, what it does in one line, the privacy pattern it demonstrates (what stays private, what is proven publicly, what lands on the public ledger), the Compact features it uses (witnesses, disclose(), shielded tokens, commitments/nullifiers), and which of our backlog ideas it feeds (NightAttest, private voting, sealed-bid, escrow, eligibility, tip jar, ROSCA…).
Close with a gap list: patterns NO example covers, that our builds would have to invent.

Rules: read the actual current source, not docs summaries (docs examples include stale ledger-8 pages — your own HERMES-1 section C). Both donation addresses + @kshot9000 + the independent-builder line in the footer, exact. Branch `hermes/hermes-2-example-patterns`, draft PR, title "Hermes: HERMES-2 — …", `Authored-By: Hermes (local seat)` trailer. Never main, never self-merge.

## HERMES-3 — Hands-on local stack verification (hardware task) — REVISED 2026-11-23

You flagged that `midnight-local-dev` is archived. Kyle's call: **re-point this task at `midnightntwrk/midnight-examples`** — its own local environment is the live, CI-verified path. Do NOT build on the archived local-dev repo.

1. Clone `midnight-examples` and stand up its local/standalone environment (node + indexer + proof server 9.0.2), network `undeployed`, in Docker on your machine.
2. In WSL: Compact toolchain 0.36.1 compiles hello-world and counter from midnight-examples, unmodified.
3. Deploy counter to the local network and make one state-changing call end-to-end (wallet → proof → indexer confirms).
4. Measure and report: proof generation time for that call (CPU), RAM peak of the stack, any version friction hit on the way (exact error text).
5. Optional in the same cycle: note GPU utilisation during proving (report what you observe, not what you expect).

Deliverable: `research/local-stack-verification.md` with exact commands, versions, timings, and a "what broke / what was confusing" list — that list feeds the Version Doctor spec directly. Same PR rules as HERMES-2, branch `hermes/hermes-3-local-stack`.

If WSL or Docker blocks you, file a HELP note with the exact error instead of working around Kyle — installs that need his admin hand are his call.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
