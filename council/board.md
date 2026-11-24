# Council Board — Midnight Dapps, Sites & Research

One claimant per task. Claim a task here (commit the claim on your branch) before starting it. Builds and research land as pull requests — nobody pushes `main` directly after the seed commit. **Merging:** Kyle granted Muse standing merge authority for team PRs on 2026-11-23 — Muse merges a PR only after a full clean review (both addresses exact, current stack versions, tests actually run and observed, privacy claims honest), marks drafts ready at merge time, and reports every merge. Any PR with open review findings, or touching money, legal, or public-launch decisions, is held for Kyle.

Seats: **Sol** = builder (sites, apps, code) · **Hermes** = non-stop worker (research, catalogue, docs, tests; 15-minute idle cycle) · **Muse** = guide (ideas, specs, review, verification) · **Kyle** = owner

Attribution on ALL work: both donation addresses, character-for-character, plus @kshot9000 —
ADA: addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

## Board

| ID | Task | Seat | Status | Branch / notes |
| --- | --- | --- | --- | --- |
| SEED-1 | Initial `main`: README, structure, board, backlog | Muse | DONE 2026-11-23 | Direct seed commit on Kyle's order — GitHub requires an initial `main` before any PR can be opened. One-time only; all later work is by PR. |
| SOL-1 | Hub site v1 in `sites/hub/` + repo catalogue/tests | Sol | DONE — PR #2 merged 2026-11-23 | Hub deploys to GitHub Pages via the hub workflow (Pages enabled 2026-11-23 on Kyle's order). |
| SOL-2 | NightAttest: Compact contract + three-role web UI (issuer / holder / verifier) | Sol | Queued — after SOL-1 | Flagship. Grant proposal already drafted ($7,500 / 8 weeks). |
| HERMES-1 | Live stack verification brief → `research/stack-versions.md` | Hermes | DONE — PR #3 merged 2026-11-23 | Official sources only, versions + source URLs + stale-guide locations. The team's pin sheet. |
| HERMES-2 | Example dApp privacy-pattern catalogue → `research/example-patterns.md` | Hermes | CLAIMED by Hermes 2026-11-23 — in progress | Branch `hermes/hermes-2-catalogue`. Full packet: `council/feed/HERMES_NEXT_FEED.md`. |
| HERMES-3 | Hands-on local stack verification → `research/local-stack-verification.md` | Hermes | Queued — after HERMES-2 | REVISED 2026-11-23 (Kyle approved): re-pointed from the archived `midnight-local-dev` to the `midnight-examples` local environment. Hardware task (Hermes' machine). Supersedes the earlier NightAttest-pack definition of HERMES-3 — that research folds into SOL-2 prep. Full packet: `council/feed/HERMES_NEXT_FEED.md`. |
| HERMES-4 | Compact pattern library outline → `research/compact-patterns-outline.md` | Hermes | Unclaimed | Outline only. |
| HERMES-5 | DUST registration explainer research → `research/dust-registration.md` | Hermes | Unclaimed | Quote official sources; label models/estimates. |
| SOL-GPU-1 | Windows Preview full-node GUI with a local CUDA proof lab (`apps/proof-station/`) | Sol | DONE — PR #4 merged 2026-11-23 (v0.1.2) | Attribution exact in GUI + README; stack pins match the verified matrix (node 2.1.101, indexer 4.4.101, proof server 9.0.2); CI green. Follow-ups still open: real CUDA run results on the RTX 3090 (claimed by Hermes on PR #4) and a Preview sync check against Cardano Preview db-sync — both need Kyle's PC. |
| SOL-HUB-2 | Refresh the Hub UI and feature the Windows Proof Station | Sol | DONE — PR #8 merged 2026-11-23, hub redeployed and verified live | `sol/hub-proof-station` · Kept the ten SOL-1 catalogue entries and published the Proof Station showcase. |
| GROK-1 | Midnight ecosystem directory sweep → `research/ecosystem-directory.md` | Grok swarm | CLAIMED by Grok swarm 2026-11-23 — in progress | Branch `grok/GROK-1-ecosystem-directory`. Verify-live discipline: an entry not opened is not listed as live. Full packet: `council/feed/GROK_FEED.md`. |
| GROK-2 | DUST Planner Pro → `apps/dust-planner/` | Grok swarm | Unclaimed | Build to spec in `council/feed/GROK_FEED.md`: official DUST mechanics, outputs labelled modelled estimates, tests assert both addresses + labelling. |
| GROK-3 | Repo QA sweep (links, accessibility, mobile) | Grok swarm | CLAIMED by Grok swarm 2026-11-23 — in progress | Branch `grok/GROK-3-qa-sweep`. Small fix PRs only; judgement calls go to `research/qa-sweep-2026-11.md` for Muse triage. Sol owns design — no redesigns. |

## Proposals

| ID | Task | Seat | Status | Branch / notes |
| --- | --- | --- | --- | --- |
| SOL-PROP-1 | NightAttest workshop pass and privacy receipt proposal | Sol | CLAIMED by Sol 2026-11-23 — proposal branch only, no PR yet | `sol/nightattest-workshop-pass` · Proposal for SOL-2 review |

## Rules (short form — full packets are with Kyle / Muse)

- Sol: branches `sol/<task>`, PRs against `main`, screenshots + test results in every PR.
- Hermes: branches `hermes/<task>`, draft PRs only (max 2/day), titles start "Hermes:", `Authored-By: Hermes (local seat)` commit trailer.
- Grok swarm (joined 2026-11-23 on Kyle's order): branches `grok/<task-id>-<slug>`, PRs against `main`, titles start "Grok:", one task per PR, rebase on latest `main` before opening, claim on this board first. Lane: parallel volume work — research sweeps, content, tests/QA, small tools built to spec. No personal information about Kyle in any file, commit, or PR — ever.
- Verify live versions before coding (Compact toolchain, Midnight.js, wallet SDK, proof server) — version mismatch is the #1 Midnight failure point; do not copy stale Ledger 8-era examples.
- Preview / Preprod testnet only, labelled testnet. No mainnet funds, no wallet signing, no API keys or sign-ups — flag those exact steps for Kyle.
- Honesty: no demo/modelled data presented as live.

## Submissions log

| When (UTC) | Seat | Task | PR | Notes |
| --- | --- | --- | --- | --- |
| 2026-11-23 ~21:55 | Hermes | HERMES-1 | [#3 (draft)](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/3) | Stack verification brief; live-checked 2026-11-23; flags Sol; 3 questions for Muse. |
| 2026-11-24 ~03:24–03:26 | Kyle (merge hand) | SOL-1, HERMES-1, feed, COLLAB-1, SOL-GPU-1 | #2, #3, #6, #1, #4 | All five merged on Kyle's order 2026-11-23. Board conflict on this PR (#5) resolved to post-merge state by Muse the same night. |
