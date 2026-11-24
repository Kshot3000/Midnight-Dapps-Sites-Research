# Compact Pattern Library — Outline (HERMES-4)

**Task:** outline for a curated, tested library of reusable Compact patterns → feeds backlog idea #9 (**Compact Playground + Snippet Library** — "tested Compact patterns for voting, credentials, auctions, escrow") and the team's own builds (SOL-2 NightAttest, #2 Private Voting, #3 Sealed-Bid, #4 Escrow, #5/#6 eligibility/payroll, #11 ROSCA).
**Scope: outline only.** No pattern code, no new contracts, no UI. Every item below is a pointer to official sources plus a status; the follow-up task authorizes the pattern cards.
**Session:** 2026-11-24, all sources below verified live in this session (list in "Verified live").

## Pinned sources (verified 2026-11-24)

| Source | State | URL |
|---|---|---|
| Compact language (dev home) | active, `main`, pushed 2026-11-24T09:00:38Z, 48 stars, Apache-2.0; holds language docs, formal Agda spec, compiler, runtime, CLI, editor extensions, reference examples | [LFDT-Minokawa/compact](https://github.com/LFDT-Minokawa/compact) |
| Compact releases | release-artifacts repo; **not archived as of 2026-11-24** (the HERMES-1 pin sheet recorded it archived on 2026-11-23 — flag flipped within a day, noted as an observation); latest tag `compactc-v0.36.1` | [midnightntwrk/compact](https://github.com/midnightntwrk/compact) |
| Toolchain (local binary self-report) | `compactc 0.36.1`, commit `7fa3a117`, 2026-11-11, **language 0.28.0**, runtime 0.21.0 | matches [release notes](https://docs.midnight.network/relnotes/compact/toolchain-0.36.1.md) (fetched: "Language version: 0.28.0 … works with a Midnight ledger 9 blockchain") |
| CI-verified app examples | `main` @ `953f864`, 12 examples read from source in HERMES-2 (merged) | [midnightntwrk/midnight-examples](https://github.com/midnightntwrk/midnight-examples) |
| Docs (Compact section) | live section with reference, stdlib API, security, tooling pages — list in "Docs currency map" | [docs.midnight.network/compact/](https://docs.midnight.network/compact/) |
| MIPs | MIP-0011 native shielded token, MIP-0004 fungible token (both verified live in HERMES-2, same day) | [MIP-0011](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0011-native-shielded-token.md) · [MIP-0004](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0004-fungible-token-standard-with-utxo.md) |

**Compiler language-version enforcement (new, tested this session):** compiler 0.36.1 accepts **exactly** `pragma language_version 0.28;`. A contract with `0.27` (the pragma the live "Writing a contract" docs page shows) is rejected: `language version 0.28.0 mismatch` (exit 255). The same test also confirmed the disclosure enforcer live: assigning a witness-derived value to a ledger field without `disclose()` fails with "potential witness-value disclosure must be declared but is not … ledger operation might disclose the witness value"; adding `disclose()` compiles. Consequence for the library: every card's code must pin `0.28`, and any doc example quoted must be checked against that.

## Library structure (proposed)

Five sections, ordered from primitive to composite. Each entry below is an outline row; "Demonstrated in" points at the CI-verified example (midnight-examples @ 953f864) or a language-repo reference example (marked ref), and "Official ref" at the docs/stdlib/MIP backing it.

### A. Foundation primitives (language level, no privacy content)

| # | Pattern | One line | Demonstrated in | Official ref |
|---|---|---|---|---|
| A1 | Basic ledger fields + core types | `Uint/Bytes/Field/Opaque` fields, typed fixed-width arithmetic | hello-world, calculator; docs "Writing a contract" | [compact-reference](https://docs.midnight.network/compact/reference/compact-reference.md) (reviewed 2026-09-25 for lang 0.28.0) |
| A2 | `sealed` constructor-frozen fields | parameters frozen at deploy; post-init writes rejected | private-party, silent-auction, private-bid, tip-jar, voting | [smart-contract-security](https://docs.midnight.network/compact/smart-contract-security.md) |
| A3 | Explicit state machine | `enum` + transition `assert`s, one enum per lifecycle phase | private-party (5 states), silent-auction (4), battleship (3); ref: [election.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/election.compact) | examples source |
| A4 | `round: Counter` anti-linking field | per-transaction counter breaks cross-tx linking of the same identity | docs "Writing a contract" lock example; Compact README | [writing](https://docs.midnight.network/compact/reference/writing.md) |
| A5 | Collections: `Map/Set/List/Counter` | storage + auto-incrementing ids | battleship, voting, leaderboard | compact-reference |
| A6 | `struct` + nominal `new type` | compile-time type separation (user key ≠ admin key) | zk-loan; ref: `examples/adt/` | compact-reference |
| A7 | `pure circuit`s | derivations shared verbatim with off-chain TS/tests | tip-jar (`ownerKey`), voting, leaderboard, zk-loan | writing |
| A8 | Constructor + deploy initialization | initial state set exactly once, by constructor/helper circuits | all 12 examples | writing |
| A9 | Bounded execution / fixed-size batching | circuits can't loop variable-length collections → cursor + fixed batch per tx | zk-loan `changePin` (5 loans/tx) | [specification](https://github.com/LFDT-Minokawa/compact/tree/main/specification) + examples |

### B. Privacy primitives

| # | Pattern | One line | Demonstrated in | Official ref |
|---|---|---|---|---|
| B1 | Private-by-default + `disclose()` | witness-derived data never reaches the ledger unless explicitly disclosed — enforced at **compile time** (error text tested this session) | hello-world (single `disclose`), private-bid (removing the one `disclose` fails compilation) | [explicit-disclosure](https://docs.midnight.network/compact/reference/explicit-disclosure.md) |
| B2 | Witness callbacks vs private circuit args | on-demand private data from local TS vs private arg passed to the circuit | calculator (`witness divMod`), private-party (`_secret` private arg), leaderboard (on-demand) | [private-state-witnesses](https://docs.midnight.network/guides/private-state-witnesses.md) |
| B3 | Domain-separated identity hashing | `persistentHash([domain, sk])` with per-DApp domain strings → unlinkable pseudonyms | private-party, silent-auction, battleship, chips, zk-loan, voting, tip-jar, leaderboard | examples source |
| B4 | Salted commitments + salt discipline | `persistentCommit(value, salt)`; guessable values need salt; deterministic salt safe only for single-commit contracts | private-party (guest list), private-bid (source documents the trap), voting (ballot) | examples source |
| B5 | `transientHash` in-proof binding | ephemeral hash binding a message to this proof only | zk-loan (anti-replay: `transientHash(userPubKey)` in the signed message) | [stdlib exports](https://docs.midnight.network/compact/standard-library/exports.md) |
| B6 | Nullifiers (one-time action) | hashed, deployment-bound nullifier rejects replay/double-use | voting (`cast` bound to proposal + `kernel.self().bytes`); ref: [zerocash.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/zerocash.compact) | stdlib + examples |
| B7 | DApp-key role pseudonyms | roles identified by derived keys, never wallet addresses | 8 of 12 examples (`getDappPubKey` derivation) | examples source |
| B8 | Anti-pattern: `ownPublicKey()` never for authorization | prover-claimed, unbound — routing funds only | stated in zk-loan, chips, DUST guide source | examples + [dust-sponsorship](https://docs.midnight.network/guides/dust-sponsorship.md) |
| B9 | "Identity-private, behavior-public" tradeoff | pseudonym on chain: in-contract behavior observable, identity not | roulette source (documented) | examples source |

### C. Token patterns

| # | Pattern | One line | Demonstrated in | Official ref |
|---|---|---|---|---|
| C1 | `mintUnshieldedToken` (domain color, mint-to-self) | `kernel.self()` recipient, color derived from domain | token-transfers, silent-auction (NFT color `"auction:nft"`) | [tokens/unshielded-token](https://docs.midnight.network/tokens/unshielded-token.md) (**ledger-8-stale** per HERMES-1 audit — code from examples) |
| C2 | `sendUnshielded`/`receiveUnshielded` + `tokenType` | unshielded flows are always public | token-transfers, private-party (entry fee) | stdlib exports |
| C3 | `nativeToken()` NIGHT flows | native token in/out of contracts | private-party | stdlib exports |
| C4 | `unshieldedBalance{,Lt,Gte,Gt,Lte}` guards | balance checks inside circuits | token-transfers guards; stdlib | stdlib exports |
| C5 | `receiveShielded` + disclosed coin metadata | contract keeps a public view of every coin it touches | tip-jar, token-transfers | examples source |
| C6 | Fresh-nonce contract coins | client mints a **new** coin under a fresh random nonce for the contract → pot entry links to no wallet | tip-jar (documented client duty + residual) | examples source |
| C7 | `insertCoin` + pot map | Merkle index recorded so the coin is spendable later; overwrite guard | tip-jar (`!pot.member(coin.nonce)`) | stdlib + examples |
| C8 | `sendShielded` (Merkle) vs `sendImmediateShielded` (transient) | two spend paths; same-tx transient burn vs persisted spend | token-transfers, chips | stdlib + examples |
| C9 | `mergeCoin`/`mergeCoinImmediate` payout unlinkability | merged nonce derives from first input → payout coin non-recomputable by observers | roulette `claimWinnings` (merge secret escrow first) | stdlib + examples |
| C10 | Burn: `shieldedBurnAddress()` + `isZeroTarget` | zero address **is** the burn address | chips | stdlib + examples |
| C11 | MIP-0011 shielded token reference | house-only mint/burn, supply accounting, treasury, caller-supplied random nonce | `chips.compact` (native MIP-0011 implementation) | [MIP-0011](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0011-native-shielded-token.md) |
| C12 | `evolveNonce` derived-nonce extension — deliberately NOT composed | MIP's derived-nonce extension makes mint recipients **public**; chips keeps the old public-nonce-chain lesson documented | chips source note | MIP-0011 + stdlib |
| C13 | Shielded escrow: commit-only storage | escrowed bet stored only as `persistentCommit([nonce,color,value], salt)` — **no coin nonce ever in public state** | roulette source | examples source |
| C14 | MIP-0004 fungible token (unshielded) standard | the unshielded token standard | docs tutorial (**stale**) + token-transfers practice | [MIP-0004](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0004-fungible-token-standard-with-utxo.md) |

### D. Cryptographic patterns

| # | Pattern | One line | Demonstrated in | Official ref |
|---|---|---|---|---|
| D1 | In-circuit Schnorr/Jubjub verification — two routes | stdlib `jubjubSchnorrVerify` **vs** witness-assisted module (zk-loan's co-located `schnorr.compact`, `import "schnorr" prefix Schnorr_`) — comparison card (see Q3) | zk-loan (module route; ledger-9 CI-verified); stdlib lists the builtin | stdlib exports + `examples/zk-loan/contract/schnorr.compact` |
| D2 | Witness-assisted heavy arithmetic | off-chain computes the division; circuit range-checks and re-derives | calculator `divMod`; `schnorr.compact` challenge reduction | examples source |
| D3 | Merkle membership proof | `MerkleTree<H,_>` + `MerkleTreePath` witness + `checkRoot`/`merkleTreePathRoot` | voting (enrolment tree); ref: [election.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/election.compact) | [ledger-adt](https://docs.midnight.network/compact/reference/ledger-adt.md) (lang 0.28.0) |
| D4 | `HistoricMerkleTree` | growing commitment tree with historical roots | ref: [zerocash.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/zerocash.compact), [proposal.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/proposal.compact) | ledger-adt page |
| D5 | External ECDSA verification | `secp256k1EcdsaVerify` + `secp256k1EthereumAddress`; Secp256r1; Ed25519 | ref: language repo `examples/ecdsa/`, `examples/secp256k1/`; stdlib lists all | stdlib exports |
| D6 | Hashing suite | `keccak256`, `sha512`, `hashToCurve` | stdlib lists; no example uses them yet (gap note) | stdlib exports |
| D7 | X25519-encrypted ballot + low-order-point rejection | ciphertext sealed to published admin keys; 14 low-order points (±top bit) rejected in-circuit | voting | examples source |
| D8 | Signature-message binding | signed message includes `transientHash(pubkey)` → attestation to someone else fails | zk-loan | examples source |
| D9 | `emit` events (observability) | `ShieldedSpend/Receive/Mint/Burn`, `Unshielded*`, `Paused/Unpaused` — indexer/UI observability hooks | **not used by any of the 12 examples** (gap note; stdlib lists them) | stdlib exports |

### E. Composite application patterns

| # | Pattern | One line | Demonstrated in | Official ref |
|---|---|---|---|---|
| E1 | Commit-reveal + reveal-time re-derivation | organizer re-derives the commitment at reveal; mismatch asserts | silent-auction (reserve), private-party (check-in), voting (opening) | examples source |
| E2 | Anonymous allow-listed participation | Merkle eligibility + nullifier + one-vote + anyone-can-count | voting | examples source |
| E3 | In-proof property of a private value | `assert(amount >= minimumBid)` inside the proof — network learns only "a qualifying bid exists" | private-bid | examples + [build-a-private-smart-contract](https://docs.midnight.network/guides/build-a-private-smart-contract.md) (**ledger-8 banner** — use the monorepo port) |
| E4 | Claim from private state, re-verify on-chain | witness claims HIT/MISS from defender's private board; contract re-derives membership vs committed board | battleship | [tutorials/battleship](https://docs.midnight.network/tutorials/battleship.md) |
| E5 | Attestation: registry + in-circuit signature verify + rules | registered provider signs profile off-chain; contract verifies in-proof, applies tier rules | zk-loan | [tutorials/zk-loan](https://docs.midnight.network/tutorials/zk-loan.md) |
| E6 | Per-entry ownership proof | prove "this entry is mine" without revealing the key — closest existing cousin of credential presentation | leaderboard | [tutorials/leaderboard](https://docs.midnight.network/tutorials/leaderboard.md) |
| E7 | Role rotation | hand the role to a **public** derived key; no secret ever leaves either party | zk-loan `rotateAdmin` | examples source |
| E8 | Deposit to incentivize settlement | organizer stakes 50 NIGHT before auction; stake gates settlement | silent-auction | examples source |
| E9 | Deadline gating | `blockTimeLt`/`blockTimeGte` windows (voting deadline + 48-h counting window) | voting; ref: `examples/std_lib/block_time.compact` | stdlib + examples |
| E10 | Cross-contract identity + C2C gap | same key derivation in two contracts = one operator; coin to a *different* contract is rejected until C2C lands (protocol gap) | chips source | examples + MIP-0011 |

## Gap list — patterns to INVENT (carried from the HERMES-2 catalogue)

No official example demonstrates any of these; the library must mark them "invented / to be validated" (they are the substance of backlog #3/#4/#5/#6/#10/#11 and SOL-2):

1. Portable, **reusable** credentials — issuance → repeated private presentation → in-circuit revocation (NightAttest core).
2. Sealed-bid auction combining private bids **and** secret reserve **and** funded settlement.
3. Multi-round / outbidding privacy (new private bid must beat a private high bid).
4. Timelocked / dispute-capable escrow, partial refunds, "delivery proven".
5. Range-only disclosure ("value ≥ threshold ⇒ true, nothing else", no tier leak).
6. Multi-party fairness / verifiable randomness (commit-reveal pools, threshold randomness).
7. ROSCA — private per-member contributions against a public round counter, private rotation draw, shielded payout sequencing.
8. Cross-contract private transfers (C2C) — currently a protocol-level gap.
9. Threshold / multi-party authority (no example has a single-secret role that isn't one key).
10. Recurring / scheduled payments (timer-driven private payment; only `blockTime` window checks exist).
11. Shielded-value transaction with sponsored DUST (the DUST guide's reference implementation is unshielded).
12. Selective decryption by a chosen third party (encrypt to an external verifier with commitment-checked opening).

## Trap list (documented in official source — every card must carry the applicable ones)

- Deterministic salt is safe only for single-commit contracts; the compiler does **not** check salt entropy (private-bid source).
- `ownPublicKey()` is a prover-claimed witness value — never for authorization (zk-loan, chips, DUST guide).
- Any coin nonce in public state is a linkability oracle against a guessed wallet key (roulette source).
- A reused wallet-coin nonce for a contract tip identifies the wallet (tip-jar source).
- Dropping change persistence strands shielded funds (chips source).
- Circuits can't loop variable-length collections → fixed-size batches + ledger cursor (zk-loan).
- Undeclared disclosure is a **compile error**, not a runtime surprise (B1, tested this session).
- Compiler 0.36.1 enforces language version **exactly 0.28.0** (tested this session); ledger-8 artifacts (0.30.0–0.31.1 era) are rejected by Midnight.js 5 for new deployments (HERMES-1 pin sheet).
- Image tag ≠ binary version (indexer image 4.4.101 prints v3.0.15) — pin by image tag (HERMES-3 item 6).

## Pattern-card template (for the follow-up authoring task)

```
### <ID> <name>
Category: A–E · Status: demonstrated | reference-only | invented (gap N)
One-line invariant: <what the pattern guarantees>
Compact features: <stdlib functions / language constructs, with the stdlib page anchor>
Canonical example: <repo + file (+ line range), CI-verified? which commit>
Privacy contract: private: … · proven publicly: … · on the public ledger: …
Documented leaks/traps: <copied verbatim from the example's source comments where present>
Verified: compiled with <toolchain> on <date> [ + simulator/devnet results, labelled local devnet ]
```

Rules carried from the board: official sources only; testnet-labelled if any network is used; no demo/modelled data presented as live; both addresses + @kshot9000 in the footer of the final library.

## Verification plan for the follow-up (what "tested" will mean)

1. **Compile-gate every card** against toolchain 0.36.1 / language 0.28 on the local stack (offline `compactc` — environment already proven in HERMES-3). A card whose quoted code doesn't compile is not a card.
2. **Runtime behaviour** where a pattern is about runtime (state machines, nullifiers, balance guards): simulator via `@midnight-ntwrk/testkit-js` 5.0.1, then local devnet E2E (node 2.1.101 / indexer 4.4.101 / proof server 9.0.2, `undeployed` network) — always labelled local devnet.
3. **Privacy claims copied from the example's own source comments** (several contracts document their accepted leaks — that is the honesty bar); where a pattern is invented, the card states the claimed boundary and what was actually tested.
4. No mainnet, no funds, no sign-ups; Preview/Preprod only if a follow-up explicitly asks for network-level checks, and labelled testnet.

## Docs currency map (Compact-relevant pages)

**Current (ledger-9 / 0.28 generation, live-verified 2026-11-24):** [compact/](https://docs.midnight.network/compact/) section — [reference](https://docs.midnight.network/compact/reference/compact-reference.md) (reviewed 2026-09-25, lang 0.28.0), [grammar](https://docs.midnight.network/compact/reference/compact-grammar.md) (lang 0.28.0), [keywords](https://docs.midnight.network/compact/reference/compact-keywords.md), [explicit-disclosure](https://docs.midnight.network/compact/reference/explicit-disclosure.md), [ledger-adt](https://docs.midnight.network/compact/reference/ledger-adt.md) (lang 0.28.0 / compiler 0.36.0), [opaque data](https://docs.midnight.network/compact/data-types/opaque_data.md), [smart-contract-security](https://docs.midnight.network/compact/smart-contract-security.md), [test-and-debug](https://docs.midnight.network/compact/test-and-debug.md), [standard library](https://docs.midnight.network/compact/standard-library.md) + [exports](https://docs.midnight.network/compact/standard-library/exports.md), tooling pages (compiler/dev-tool/fixup/formatter/neovim/vscode), [toolchain 0.36.1 release notes](https://docs.midnight.network/relnotes/compact/toolchain-0.36.1.md); [compact-privacy-first-language](https://docs.midnight.network/concepts/how-midnight-works/compact-privacy-first-language.md); [identity](https://docs.midnight.network/concepts/identity.md) (accurate but easy to misread — DID/VC packages target ledger-8, per HERMES-1); [private-state-witnesses](https://docs.midnight.network/guides/private-state-witnesses.md); [compact-javascript-runtime](https://docs.midnight.network/guides/compact-javascript-runtime.md); [contract examples](https://docs.midnight.network/examples/contracts.md) (+ battleship-simple, private-guest-list, private-reserve-auction); [OpenZeppelin Compact contracts](https://docs.midnight.network/sdks/community/openzeppelin-compact-contracts.md) (community library — precedent for a third-party pattern library).

**New finding this session:** the flagship [Writing a contract](https://docs.midnight.network/compact/reference/writing.md) page's code starts with `pragma language_version 0.27;` — rejected by compiler 0.36.1 (tested: `language version 0.28.0 mismatch`). Candidate for the docs-issue batch with the HERMES-3 quickstart drift.

**Stale (ledger-8 era, from the HERMES-1 audit 2026-11-23 — reuse, don't re-audit):** `/tokens/unshielded-token`, `/tokens/shielded-token`, `/guides/browser-dapp`, `/guides/local-proving`, `/guides/build-a-private-smart-contract` (its private-bid contract lives on in the monorepo), `/guides/deploy-and-operate`, `/guides/node-server-wallet`, `/guides/install-bun-runtime-midnight`, `/examples/dapps/zkloan`, `/nodes/node-toolkit`, `/sdks/official/midnight-js`, and all frozen `/0.0.0/…` pages.

## Open questions for Muse

1. **Where does the library physically live?** Markdown cards in `research/` (this lane's shape: outline → cards) vs a code monorepo with compilable `.compact` files + CI (would need Sol's lane). Suggestion: cards start as markdown with code blocks; a compiled-`patterns/` repo is a later, separate task.
2. **First-wave target:** should the first card batch cover exactly the four domains of backlog idea #9 (voting, credentials, auctions, escrow)?
3. **D1 route:** is stdlib `jubjubSchnorrVerify` the endorsed path on 0.36.1, or the witness-assisted module route (the only one a CI-verified ledger-9 example actually uses)? Worth asking the Midnight team if neither is documented as preferred.
4. **Language-repo examples:** should `election.compact` / `zerocash.compact` / `proposal.compact` (LFDT repo, older style, not CI-verified against ledger-9 apps) count as "reference" cards, or stay out?
5. **Docs issue batch:** file the `writing.md` 0.27-pragma finding with the HERMES-3 friction items as one upstream docs issue (Kyle accepted items 1–3 as docs-issue candidates on PR #24)?

## Verified live this session (2026-11-24)

| Source | Check |
|---|---|
| [LFDT-Minokawa/compact](https://github.com/LFDT-Minokawa/compact) | GitHub API: active, `main`, pushed 2026-11-24T09:00:38Z, 48 stars; tree read (specification/doc/examples/test-contracts); README, stdlib API README + exports list, and `counter/election/zerocash/proposal/block_time.compact` fetched from source |
| [midnightntwrk/compact](https://github.com/midnightntwrk/compact) | GitHub API: `archived: false` on 2026-11-24, description "Compact Releases", latest tag `compactc-v0.36.1`, `prerelease/` artifacts present |
| `compactc` 0.36.1 binary (local, HERMES-3 install) | self-report: release 0.36.1, commit 7fa3a117 (2026-11-11), language 0.28.0, runtime 0.21.0 |
| Pragma enforcement | live compile tests: `0.27` → `language version 0.28.0 mismatch` (exit 255); `0.28` without `disclose` → exact disclosure error; `0.28` + `disclose` → compile OK (ZKIR key-gen skipped in bare env — expected outside the monorepo toolchain) |
| [docs.midnight.network/compact/](https://docs.midnight.network/compact/) + [writing](https://docs.midnight.network/compact/reference/writing.md) + [smart-contract-security](https://docs.midnight.network/compact/smart-contract-security.md) + [standard-library](https://docs.midnight.network/compact/standard-library.md) + [toolchain 0.36.1 notes](https://docs.midnight.network/relnotes/compact/toolchain-0.36.1.md) | fetched with content (writing page body: `pragma language_version 0.27;`) |
| [llms.txt](https://docs.midnight.network/llms.txt) (full docs index) | fetched; per-page version claims extracted for the currency map |
| `examples/zk-loan/contract/schnorr.compact` | present in the local `midnight-examples` clone @ 953f864; `import "schnorr" prefix Schnorr_;` at `zk-loan.compact:38` |
| HERMES-2 catalogue (12 examples, features, gap list) + HERMES-3 run (toolchain matrix, error texts) + HERMES-1 pin sheet | merged on main 2026-11-23/24, same-day live verification — reused, not re-audited |

## Not verified

- `/examples/contracts*` pages: in the live docs index, but their version claims were not fetched this session.
- The stdlib `exports.md` **site page** (the repo source it is generated from was fetched; the site page itself not fetched).
- Language-repo reference examples were **not** compiled against 0.36.1 (older style — they are cited as reference, not as compiling code).
- No runtime/E2E verification of any pattern (outline only — the verification plan above governs the follow-up).
- No testnet, mainnet, funds, wallets, keys, or sign-ups of any kind.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
