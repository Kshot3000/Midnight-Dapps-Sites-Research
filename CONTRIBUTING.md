# Contributing

Thanks for helping build open tools for the Midnight community. This repo is public and outside pull requests are welcome.

## How to contribute

1. **Fork** the repo and branch from `main` (name it after the work, e.g. `fix/hub-typo` or `docs/dust-faq`).
2. **One task per PR.** If the work is substantial, open an issue first or claim a task on `council/board.md` in your PR so two people don't build the same thing.
3. **Run the tests** before you push: `npm test` from the repo root — the full suite must pass on your tree. App work has its own CI (the Android prover workflow builds the APK and runs its unit tests on every PR).
4. **Open the PR against `main`** and fill in the PR template. PRs are reviewed for correctness, honest claims, and the rules below, then merged by the maintainer.

## The rules that keep this repo trustworthy

- **Claims never exceed evidence.** If a number or behaviour wasn't measured or verified, label it as such or leave it out. Testnets (Preview, Preprod) are always labelled testnet.
- **Stack pins follow the verified matrix:** Compact toolchain 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector API 4.1.0, indexer 4.4.101. Flag stale versions when you spot them; don't introduce new ones silently.
- **Attribution footer.** Project documents carry this footer, character-for-character (the test suite checks it):

  Cardano (ADA): `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`
  Midnight (shielded): `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`
  Line: "Independent builder — not affiliated with the Midnight Foundation or IOG." — and @kshot9000.

- **No personal information** about any contributor (addresses, phone numbers, personal emails, IDs) in files, commits, or PR text — ever.
- **No secrets, keys, or wallet material** in the repo. Anything needing funds, signing, or accounts is the maintainer's step, not a PR's.

## Where things live

- `sites/hub/` — the public hub site; `apps/` — Proof Station, DUST Planner, the Android prover; `research/` — sourced briefs (every claim cites its source); `ideas/backlog.md` — what could be built next; `council/board.md` — who is doing what.

Good first contributions: a sourced brief for `research/`, a fix from any brief's findings list, or a backlog idea specced in an issue.

---

Cardano (ADA): `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`

Midnight (shielded): `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`

Independent builder — not affiliated with the Midnight Foundation or IOG. — @kshot9000
