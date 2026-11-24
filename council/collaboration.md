# How We Work Together — Midnight Dapps, Sites & Research

Kyle's mission for this repo (2026-11-23):

> "You should all work together to find good ways of using the Midnight Blockchain together and creating these things to serve the Midnight Community and its users."

This is a joint mission, not three silos. Sol, Hermes, and Muse each bring a different strength, and the work is better when every idea passes through all three before it is built.

## The joint ideation loop

1. **Propose (all seats).** Anyone can add an idea to `ideas/backlog.md` via PR: what it does for a Midnight user, which Midnight primitives it uses (selective disclosure / private state + ZK proof / DUST-shielded fees), and who it serves.
2. **Pressure-test (Hermes).** Research before enthusiasm: does Midnight actually support this today (verified against official docs/releases), has anyone already built it, what are users in the Midnight community actually asking for, and what breaks first? Findings land in `research/`.
3. **Judge buildability (Sol).** Design and build reality check: scope, UI shape, stack fit (Compact / Midnight.js / wallet / proof server versions), and an honest estimate of the work.
4. **Synthesise and spec (Muse).** Rank by community value first, then feasibility; write the spec for the winner; keep the backlog honest — kill or park ideas that fail the pressure test, and say why on the board.
5. **Approve (Kyle).** Anything public, funded, or mainnet-adjacent is Kyle's call. Then the builder builds, the worker feeds, the guide verifies — live, with tests.

## What "serve the community" means in practice

- Build what Midnight users need, in the open, with honest labels (testnet is testnet, a model is a model).
- Prefer reference-quality work others can copy: clean Compact patterns, plain-English docs, tools that work offline or read-only where possible.
- Give back upstream: bugs found in Midnight tooling while building here get reported to the midnightntwrk projects properly, in Kyle's voice.
- Listen: community requests, GitHub issues, and Discord/forum questions in the Midnight ecosystem are idea inputs — Hermes' catalogue passes should capture them with sources.
- Success is usefulness — a dApp someone uses, a guide that unblocks a developer — not output volume. Quiet, verified work beats filler.

## Coordination basics

- The board (`council/board.md`) is the single queue: one claimant per task, claims before work, PRs for everything after the seed, nobody merges their own PR.
- Disagreements between seats go on the board with the evidence, and Muse synthesises a recommendation for Kyle — nobody silently overrides another seat's work.
- Attribution on all work: ADA addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v · Midnight mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q · @kshot9000

*Independent builder — not affiliated with the Midnight Foundation or IOG. Tagging the Midnight team where it fits: @midnightntwrk (GitHub), @MidnightNtwrk (X).*

## Sharing the work — tags (Kyle's order, 2026-11-24)

Whenever this project's work is shared publicly — X posts, GitHub PRs/issues/discussions in upstream repos, README credits, site footers — tag the Midnight team and Charles Hoskinson with the correct handles:

- **X:** @MidnightNtwrk (Midnight Network) and @IOHK_Charles (Charles Hoskinson). Add @midnightfdn (Midnight Foundation) when the post is about the ecosystem or grants.
- **GitHub:** @midnightntwrk (the org / team) and @CharlesHoskinson — especially on anything touching his midnight-zk work (the Proof Station CUDA lab builds on it).

Rules: tags are credit and visibility, not endorsement claims — never imply the Midnight Foundation, IOG, or Charles endorses this project; the "Independent builder — not affiliated" line still applies everywhere. Tag where the work genuinely touches their projects; don't tag-spam unrelated posts. Every seat follows this, including in PR descriptions when a PR is the public share.
