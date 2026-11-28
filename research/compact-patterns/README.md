# Compact Pattern Library — Wave 1 (HERMES-6)

**Task:** backlog idea #9 (Compact Playground + Snippet Library — "tested Compact patterns for voting, credentials, auctions, escrow"), wave 1 of the card authoring task authorized by the HERMES-4 outline. Claimed 2026-11-27 (commit 7f9b324), card work 2026-11-28.
**This file is the wave's source of truth for scope and progress** — any resuming tick reads this table and continues where it stops.

## Scope

One markdown card per outline pattern row (sections A–E, `research/compact-patterns-outline.md`) that feeds any of the four domains, plus the four invented-gap cards (gaps 1–4 of the outline's gap list). Total: **54 cards** (50 row cards + 4 gap cards).

- E4 (battleship claim/re-verify) is **out of wave-1 scope** — battleship is a game, not one of the four domains. It stays in the outline for a later wave.
- A card is written once and may serve several domains (e.g. A2 serves voting, auctions, escrow); the domain map below lists every assignment.
- **Reference-only cards** (language-repo examples, not CI-verified, no compile gate expected): D4, D5, D9, and D6 unless the stdlib gate below passes.
- **Invented cards** (gaps 1–4): no official example exists; the code is authored here, compile-gated, and the card states the claimed privacy boundary plus exactly what was tested (compile only, this wave).

## Domain map

| Domain | Cards |
|---|---|
| Voting | A1, A2, A3, A4, A5, A7, A8, B1, B3, B4, B6, D3, D7, E1, E2, E9 |
| Credentials | A6, A7, A8, A9, B2, B3, B5, B7, B8, B9, D1, D2, D4 (ref), D5 (ref), D6, D8, E5, E6, E7, GAP-1 |
| Auctions | A2, A3, B4, C1, C2, C3, C4, C14, E1, E3, E8, E9, GAP-2, GAP-3 |
| Escrow | A2, A4, A5, A6, A8, A9, B1, B3, B5, B8, B9, C5, C6, C7, C8, C9, C10, C11, C12, C13, D1, D6, D8, E9, E10, GAP-4 |

## Conventions

- **Template:** the outline's pattern-card template (name, privacy job, Compact features, canonical example with file:lines, code block, compile-gate result, documented leaks/traps copied verbatim from example source comments where present, Verified line). Cards add a `Code (pattern core — trimmed to compile standalone)` label and a per-card footer.
- **Compile gate (mandatory before a card counts as done):** offline `compactc 0.36.1 (7fa3a117c 2026-11-11) --skip-zk <src> <outdir>` on the octanode WSL host, language `0.28` pinned exactly (0.27 is rejected, exit 255 — outline, tested). Gate line format: `GATE <name> exit=<rc> toolchain=0.36.1 (7fa3a117c 2026-11-11) <UTC timestamp>`.
- **No runtime/devnet leg in wave 1** — the outline's verification plan item 1 (offline compile) is this wave's bar; runtime (item 2) is a later wave. No testnet, no funds, no wallets, no sign-ups.
- **Canonical examples:** `midnightntwrk/midnight-examples` @ `953f864` (CI-verified; local `.ref` clone at the same commit). Line numbers cite that commit.
- **D1 route (per outline Q3, Muse-answered):** both the stdlib `jubjubSchnorrVerify` route and the witness-assisted module route (zk-loan's `schnorr.compact`) are presented; neither is labelled endorsed — upstream has not documented a preference.
- **Card code** is a trimmed pattern core (minimal contract that compiles standalone), not the full example contract — full source is cited at file:lines. Verbatim quotes from example source comments are re-verified against the local clone at authoring time.

## Progress (wave 1)

Status: ✅ = card written + gate exit 0 this session · ⬜ = queued.

### ✅ Done (18 cards, all compile-gated 2026-11-28 UTC, offline --skip-zk)

| ID | Card file | Domains | Gate line |
|---|---|---|---|
| A2 | `a2-sealed-fields.md` | voting, auctions, escrow | `GATE a2-sealed-fields exit=0 toolchain=0.36.1 (7fa3a117c 2026-11-11) 2026-11-28T04:46:45Z` |
| A3 | `a3-state-machine.md` | voting, auctions | `GATE a3-state-machine exit=0 … 2026-11-28T04:46:46Z` |
| A5 | `a5-collections.md` | voting, escrow | `GATE a5-collections exit=0 … 2026-11-28T04:49:40Z` (2nd attempt; 1st failed: missing stdlib import → `unbound identifier Counter`) |
| B1 | `b1-disclose.md` | voting, credentials, escrow | `GATE b1-disclose exit=0 … 2026-11-28T04:46:48Z` + negative control `GATE b1-disclose-NEGATIVE exit=255 … 04:46:47Z` (enforcer error quoted in card) |
| B3 | `b3-domain-identity.md` | voting, credentials | `GATE b3-domain-identity exit=0 … 2026-11-28T04:46:48Z` |
| B4 | `b4-salted-commitments.md` | voting, auctions | `GATE b4-salted-commitments exit=0 … 2026-11-28T04:46:49Z` |
| B6 | `b6-nullifiers.md` | voting | `GATE b6-nullifiers exit=0 … 2026-11-28T05:26:32Z` (final comments) |
| C1 | `c1-mint-unshielded.md` | auctions | `GATE c1-mint-unshielded exit=0 … 2026-11-28T06:17:23Z` |
| C2 | `c2-send-unshielded.md` | auctions | `GATE c2-send-unshielded exit=0 … 2026-11-28T06:17:24Z` |
| C3 | `c3-native-night.md` | auctions, escrow | `GATE c3-native-night exit=0 … 2026-11-28T06:17:25Z` |
| C4 | `c4-balance-guards.md` | auctions | `GATE c4-balance-guards exit=0 … 2026-11-28T06:22:16Z` (2nd attempt; uncast uint sum) |
| D3 | `d3-merkle-membership.md` | voting | `GATE d3-merkle-membership exit=0 … 2026-11-28T04:46:51Z` |
| D7 | `d7-x25519-ballot.md` | voting | `GATE d7-x25519-ballot exit=0 … 2026-11-28T04:46:51Z` |
| E1 | `e1-commit-reveal.md` | voting, auctions, credentials | `GATE e1-commit-reveal exit=0 … 2026-11-28T06:17:26Z` |
| E3 | `e3-inproof-property.md` | auctions | `GATE e3-inproof-property exit=0 … 2026-11-28T06:17:27Z` |
| E8 | `e8-settlement-deposit.md` | auctions | `GATE e8-settlement-deposit exit=0 … 2026-11-28T06:17:28Z` |
| GAP-2 | `gap2-sealed-bid-fund.md` | auctions | **invented** · `GATE gap2-sealed-bid-fund exit=0 … 2026-11-28T06:22:18Z` (2nd attempt; `bidderKey` name collision) |
| GAP-3 | `gap3-multi-round-outbid.md` | auctions | **invented** · `GATE gap3-multi-round-outbid exit=0 … 2026-11-28T06:17:29Z` |

**Auctions domain: 12/14 cards done** (C14 and E9 still queued below).

### ⬜ Queued (36 cards)

| ID | Card file (planned) | Domains | Status / notes |
|---|---|---|---|
| A1 | `a1-basic-fields.md` | voting, credentials | queued — hello-world + calculator as canonical |
| A4 | `a4-round-counter.md` | voting, escrow | queued — anti-linking `round: Counter` (writing.md lock example) |
| A6 | `a6-struct-nominal.md` | credentials | queued — zk-loan |
| A7 | `a7-pure-circuits.md` | voting, credentials | queued — tip-jar `ownerKey` et al. |
| A8 | `a8-constructor-init.md` | all | queued — all 12 examples |
| A9 | `a9-bounded-execution.md` | credentials, escrow | queued — zk-loan `changePin` (5 loans/tx) |
| B2 | `b2-witness-callbacks.md` | credentials | queued — calculator, private-party, leaderboard |
| B5 | `b5-transienthash.md` | credentials, escrow | queued — zk-loan anti-replay |
| B7 | `b7-dapp-key-roles.md` | credentials | queued — `getDappPubKey` in 8/12 examples |
| B8 | `b8-ownpublickey-antipattern.md` | credentials, escrow | queued — anti-pattern card; legitimate use snippet gated |
| B9 | `b9-identity-private-behavior-public.md` | escrow | queued — roulette source (documented) |
| C5 | `c5-receive-shielded.md` | escrow | queued — tip-jar, token-transfers |
| C6 | `c6-fresh-nonce-coins.md` | escrow | queued — tip-jar |
| C7 | `c7-insert-coin-pot.md` | escrow | queued — tip-jar |
| C8 | `c8-send-shielded-paths.md` | escrow | queued — token-transfers, chips |
| C9 | `c9-merge-coin-payouts.md` | escrow | queued — roulette `claimWinnings` |
| C10 | `c10-burn.md` | escrow | queued — chips |
| C11 | `c11-mip0011-token.md` | escrow | queued — chips (native MIP-0011) |
| C12 | `c12-evolvenonce-not-composed.md` | escrow | queued — chips source note |
| C13 | `c13-shielded-escrow-commit.md` | escrow | queued — roulette |
| C14 | `c14-mip0004-unshielded.md` | auctions | queued — MIP-0004 + token-transfers (docs page stale — code from examples) |
| D1 | `d1-schnorr-two-routes.md` | credentials, escrow | queued — comparison card; both routes presented, neither endorsed |
| D2 | `d2-witness-arithmetic.md` | credentials | queued — calculator `divMod`, `schnorr.compact` |
| D4 | `d4-historic-merkle.md` | credentials | **reference-only** — zerocash + proposal (language repo) |
| D5 | `d5-external-ecdsa.md` | credentials | **reference-only** — `examples/ecdsa/`, `examples/secp256k1/` (language repo) |
| D6 | `d6-hashing-suite.md` | credentials, escrow | queued — try gate on stdlib `keccak256/sha512/hashToCurve`; reference-only if the gate fails |
| D8 | `d8-signature-binding.md` | credentials, escrow | queued — zk-loan |
| D9 | `d9-emit-events.md` | all | **reference-only** — no example uses `emit` (gap note; stdlib lists events) |
| E2 | `e2-allowlisted-participation.md` | voting | queued — voting composite |
| E5 | `e5-attestation.md` | credentials | queued — zk-loan |
| E6 | `e6-ownership-proof.md` | credentials | queued — leaderboard |
| E7 | `e7-role-rotation.md` | credentials | queued — zk-loan `rotateAdmin` |
| E9 | `e9-deadline-gating.md` | voting, auctions, escrow | queued — voting windows + `block_time.compact` ref |
| E10 | `e10-cross-contract-identity.md` | escrow | queued — chips; C2C protocol gap documented |
| GAP-1 | `gap1-portable-credentials.md` | credentials | **invented (gap 1)** — issue → repeated private presentation → in-circuit revocation |
| GAP-4 | `gap4-timelock-escrow.md` | escrow | **invented (gap 4)** — timelocked/dispute-capable escrow, partial refunds |

## How to resume (for future ticks)

1. Write the next queued card(s): canonical contract at file:lines from `midnight-examples` @ `953f864` (local `.ref/midnight-examples`), pattern-core `.compact` in scratch `h6/cards-src/`, gate via `h6/gate.sh` on octanode (`wsl -d octanode -- bash /mnt/c/…/h6/gate.sh <name> <src>`), fix until exit 0, then the card `.md` in this directory with the exact gate line in `Verified:`.
2. Invented cards: design minimal contracts, gate them, state the claimed boundary + "compile-gated only, no runtime test" explicitly.
3. When a card lands, move its row from Queued to Done (with the full gate line) and commit.
4. All 54 done → board row SUBMITTED + draft PR (title `Hermes:`), PR body: what was verified live / what was NOT verified / questions for Muse.

## What was NOT verified (this wave so far)

- No runtime/simulator/devnet results — compile gate only (outline verification plan item 1; item 2 is a later wave).
- No testnet, mainnet, funds, wallets, or sign-ups of any kind.
- Language-repo reference examples (D4/D5/D9 citations) were not compiled against 0.36.1 — cited as reference only.
- D6 gate attempt not yet made (queued above).

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
