# Council Board — Midnight Dapps, Sites & Research

One claimant per task. Claim a task here (commit the claim on your branch) before starting it. Builds and research land as pull requests — nobody pushes `main` directly after the seed commit, and nobody merges their own PR. Kyle merges.

Seats: **Sol** = builder (sites, apps, code) · **Hermes** = non-stop worker (research, catalogue, docs, tests; 15-minute idle cycle) · **Muse** = guide (ideas, specs, review, verification) · **Kyle** = owner

Attribution on ALL work: both donation addresses, character-for-character, plus @kshot9000 —
ADA: addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

## Board

| ID | Task | Seat | Status | Branch / notes |
| --- | --- | --- | --- | --- |
| SEED-1 | Initial `main`: README, structure, board, backlog | Muse | DONE 2026-11-23 | Direct seed commit on Kyle's order — GitHub requires an initial `main` before any PR can be opened. One-time only; all later work is by PR. |
| SOL-1 | Hub site v1 in `sites/hub/` + repo catalogue/tests | Sol | SUBMITTED — PR #2 open, reviewed clean by Muse 2026-11-23 (tests run on the PR tree: 8/8) | Branch `sol/sol-1-hub-v1`. Awaiting Kyle's merge. |
| SOL-2 | NightAttest: Compact contract + three-role web UI (issuer / holder / verifier) | Sol | Queued — after SOL-1 | Flagship. Grant proposal already drafted ($7,500 / 8 weeks). |
| HERMES-1 | Live stack verification brief → `research/stack-versions.md` | Hermes | SUBMITTED — draft PR #3, reviewed clean by Muse 2026-11-23 | Official sources only, versions + source URLs + stale-guide locations. Unblocks Sol. Awaiting Kyle's merge. |
| HERMES-2 | Example dApp privacy-pattern catalogue → `research/example-dapps-catalogue.md` | Hermes | CLAIMED by Hermes 2026-11-23 — in progress | Branch `hermes/hermes-2-catalogue`. Every link verified live. |
| HERMES-3 | NightAttest research pack → `research/nightattest-pack.md` | Hermes | Unclaimed | Research only — no contract code, no UI. |
| HERMES-4 | Compact pattern library outline → `research/compact-patterns-outline.md` | Hermes | Unclaimed | Outline only. |
| HERMES-5 | DUST registration explainer research → `research/dust-registration.md` | Hermes | Unclaimed | Quote official sources; label models/estimates. |
| SOL-GPU-1 | Windows Preview full-node GUI with a local CUDA proof lab (`apps/proof-station/`) | Sol | SUBMITTED — draft PR #4, reviewed clean by Muse 2026-11-23 | Branch `sol/midnight-proof-station`. Attribution exact in GUI + README; stack pins match the verified matrix (node 2.1.101, indexer 4.4.101, proof server 9.0.2); tests run on the PR tree (attribution PASS, 6/6 proof fixtures) and CI green. Still open: real CUDA run on the RTX 3090 and a Preview sync check against Cardano Preview db-sync — both need Kyle's PC. |

## Proposals

| ID | Task | Seat | Status | Branch / notes |
| --- | --- | --- | --- | --- |
| SOL-PROP-1 | NightAttest workshop pass and privacy receipt proposal | Sol | CLAIMED by Sol 2026-11-23 — proposal branch only, no PR yet | `sol/nightattest-workshop-pass` · Proposal for SOL-2 review |

## Rules (short form — full packets are with Kyle / Muse)

- Sol: branches `sol/<task>`, PRs against `main`, screenshots + test results in every PR.
- Hermes: branches `hermes/<task>`, draft PRs only (max 2/day), titles start "Hermes:", `Authored-By: Hermes (local seat)` commit trailer.
- Verify live versions before coding (Compact toolchain, Midnight.js, wallet SDK, proof server) — version mismatch is the #1 Midnight failure point; do not copy stale Ledger 8-era examples.
- Preview / Preprod testnet only, labelled testnet. No mainnet funds, no wallet signing, no API keys or sign-ups — flag those exact steps for Kyle.
- Honesty: no demo/modelled data presented as live.
