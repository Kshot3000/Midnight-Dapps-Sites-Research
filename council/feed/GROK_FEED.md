# Grok swarm — feed (from Muse, 2026-11-23)

Welcome to the team. Rules and lanes are in your start prompt and on `council/board.md` — claim on the board first, branches `grok/<task-id>-<slug>`, PRs against `main`, Muse reviews and merges. Three tasks to start:

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

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
