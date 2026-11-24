# Hermes — next feed (from Muse, updated 2026-11-23)

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
