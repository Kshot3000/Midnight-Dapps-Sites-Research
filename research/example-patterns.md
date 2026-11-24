# Example dApp privacy-pattern catalogue (HERMES-2)

Catalogue of every example in [`midnightntwrk/midnight-examples`](https://github.com/midnightntwrk/midnight-examples) — the official, CI-verified, single-toolchain monorepo of Midnight example DApps and smart contracts — read from the actual current source, not from docs summaries.

**Source tree:** branch `main`, HEAD `953f8648b20d82bf5ebf486381f62a1aa2d22d3c` (fetched and read 2026-11-24; latest commit: "Merge pull request #135 from Kanasjnr/kanas/leaderboard-skip-l8-dust"). The repo README states: *"Everything here targets Ledger 9"*.

**Scope:** the 12 DApp examples under `examples/` (one npm workspace each, every one with its own `README.md`, `AGENTS.md`, simulator suite `npm run test:sim` and devnet suite `npm run test:local`). `experimental/q2-ledger9` is a stagenet **test harness** for the `compact-end-2-end` dapps (one vitest suite per dapp, the only witnessed dapp being `usdcx`); it is a harness, not a DApp, so it is noted at the end but not catalogued per-example. The repo's own learning path orders 7 of the 12 (hello-world → private-party → battleship → private-tip-jar → shielded-chips → zk-loan → leaderboard).

## Pinned toolchain (as declared in the repo)

From the monorepo README's "Pinned toolchain" table, which `npm run toolchain:check` enforces against `toolchain.json`, `ci.yaml`, `.nvmrc`, every `compose.yml`, `package.json` and contract pragma:

| Component | Version |
|---|---|
| Compact language (`pragma language_version`) | `0.28` |
| Compact compiler (`setup-compact-action`) | `0.36.1` |
| `@midnight-ntwrk/midnight-js-*` | `5.0.1` |
| `@midnight-ntwrk/testkit-js` | `5.0.1` |
| Wallet SDK | `2.0.1` |
| Node.js | `22` (`.nvmrc`) |
| npm | `11.21.0` (CI; `11.10.0` or newer locally) |
| Compact CLI (`compact`) | `0.5.3` |
| `@midnight-ntwrk/dapp-connector-api` | `4.0.1` |
| proof server image (local devnet) | `9.0.2` |
| node image (local devnet) | `2.1.101` |
| indexer image (local devnet) | `4.4.101` |

These agree with the merged HERMES-1 pin sheet (`research/stack-versions.md`) except one row: the monorepo pins `dapp-connector-api` **4.0.1** while the pin sheet records npm latest / support-matrix **4.1.0** (checked 2026-11-23). 4.0.1 is the CI-verified working combination; the 4.1.0 bump is not yet demonstrated in the examples. Flagged under "Not verified" below.

## Method

- Every contract in `examples/*/contract/*.compact` was read in full from the cloned source (14 contract files across the 12 examples).
- Every link cited below was verified live on 2026-11-24 (fetch with content, or HTTP 200 + source-repo cross-check). Table in "Verified live".
- Privacy claims are taken from the contract source and its in-source comments (several contracts — `private-bid`, `private-tip-jar`, `chips.compact`, `roulette.compact` — document their accepted privacy leaks in source). Nothing here is modelled or inferred as observed behaviour; where a pattern is *invented by us* rather than demonstrated, it is in the gap list.
- All examples run against a **local devnet in Docker** (node + indexer + proof server, `undeployed` network) or a simulator — not Preview/Preprod. No testnet, no mainnet, no funds, no wallets, no signatures were involved in this research.

## Summary matrix

| Example | One line | Privacy in one line | Main primitives |
|---|---|---|---|
| hello-world | Minimal contract: store a message | None — everything disclosed | `disclose`, private-by-default circuit args |
| calculator | Public arithmetic; one off-chain witness re-checked on-chain | None — result always disclosed | `witness` + on-chain re-verification |
| private-party | Event RSVPs stay private until guests pay the NIGHT fee | Guests + organizer private until the unshielded payment | salted `persistentCommit`, DApp key, state machine |
| token-transfers | Mint/send/receive across all three token flavors | Only protocol-level (shielded coins); contract circuits disclose coin info | unshielded + NIGHT + shielded primitives |
| battleship | Two-player board game with cheating caught on-chain | Ship positions private; shots, turns, winner public | hashed commitments, witness claims re-verified, role checks |
| silent-auction | NFT auction with a secret reserve price | Reserve private until close; bids + high bid public | commit-reveal (`persistentCommit`), deposit, unshielded NFT |
| private-bid | Prove a bid meets a public minimum without revealing it | Amount + wallet private; "a bid ≥ min exists" public | in-proof `assert`, salted commit, contract-bound derived key |
| private-tip-jar | Anonymous shielded tips into an owner-only pot | Tipper private; values public | fresh-nonce shielded coins, `insertCoin`, hashed owner key |
| shielded-chips | MIP-0011 shielded token + roulette that custodies and pays chips privately | Wallet identity + escrowed coin + winning number private; bet color/value public | MIP-0011 token, escrow commits, `mergeCoin`, commit-reveal |
| zk-loan | Private credit scoring from a Schnorr-signed attestation | Profile private; tier outcome only public | in-circuit Schnorr (Jubjub) verify, witness-derived identity |
| leaderboard | Arcade scores under anonymous names with ownership proofs | Secret key private; scores + names public | witness on demand, ZK ownership proof, Lace + Vercel |
| voting | Anonymous allow-listed votes, admin-counted encrypted ballots | Vote + voter-linkage private; nullifier + result public | Merkle membership, nullifiers, X25519 ciphertext, commit-then-open |

## The examples

### 1. hello-world

**What it does:** environment smoke test — one ledger field, one circuit that stores a message (the entry point of the [docs quick start](https://docs.midnight.network/getting-started/hello-world)).

**Privacy pattern:**
- Private: nothing — the stored message is `disclose()`d to the ledger.
- Proven publicly: that the circuit ran with some private input (a ZK proof over `storeMessage`).
- On the public ledger: the full message.

**Compact features used:** `export ledger message: Opaque<"string">`; circuit arguments are **private by default** (the whole privacy model in miniature: data stays private until `disclose()` says otherwise); a single `disclose(newMessage)`. No witnesses, no commitments, no token ops.

**Feeds our backlog:** the baseline for [#9 Compact Playground + Snippet Library] and the environment contract every other idea builds on. No privacy content.

### 2. calculator

**What it does:** a single public `result: Uint<16>` updated by `add`/`subtract`/`multiply`/`square`, plus `divide` — the smallest demonstration of the **verify-off-chain-work pattern**: `witness divMod(num1, num2): [Uint<16>, Uint<16>]` computes quotient/remainder in TypeScript, and the circuit re-checks `assert(rem < num2 && quo * num2 + rem == num1)` before storing `quo`.

**Privacy pattern:**
- Private: the witness's intermediate work (it never reaches the ledger).
- Proven publicly: that the off-chain division is arithmetically correct.
- On the public ledger: every result, disclosed.

**Compact features used:** `witness` declarations with on-chain re-verification (the pattern that later powers battleship's cheat checks and zk-loan's tier rules); typed fixed-width arithmetic; explicit `disclose` of stored results.

**Feeds our backlog:** [#9] snippet library (the canonical witness-recheck snippet); the recheck idiom is the load-bearing trick for any pattern where an off-chain computation must be trusted (payroll math in #6, ROSCA arithmetic).

### 3. private-party

**What it does:** a party DApp teaching **the privacy boundary**: guests RSVP under a salted commitment and stay anonymous until check-in, where they pay an unshielded NIGHT entry fee; the organizer stays anonymous until claiming the fees. Step 2 of the learning path; also the reference implementation behind the [DUST sponsorship guide](https://docs.midnight.network/guides/dust-sponsorship) (see [its SPONSORSHIP.md](https://github.com/midnightntwrk/midnight-examples/tree/main/examples/private-party)).

**Privacy pattern:**
- Private until check-in: each guest's `UserAddress` — the RSVP list holds only `persistentCommit(_address, _secret)` (a salted commitment; the same secret re-derives the same commitment, which is what dedupes the list via `hashedPartyGoers.member(...)`).
- Private until `claimFees`: the organizer, identified only by a DApp-specific public key (`persistentHash(["private-party:pk:"], _secret)`), not a wallet address.
- Proven publicly (per RSVP): that the caller is not the organizer, the party is not started, the list is not full, and the caller is not already on it.
- On the public ledger at check-in: the guest address (`checkedInParty: Set<UserAddress>`), the fee (`receiveUnshielded(nativeToken(), entryFee)` — unshielded transfers are always public), and at claim, the organizer address + total collected.

**Compact features used:** `sealed ledger` for organizer/fee/size (constructor-writable only); an explicit 5-state machine (`NOT_STARTED → READY → STARTED → DOORS_CLOSED → FEES_CLAIMED`); `persistentCommit`/`persistentHash` with per-DApp domain separators; `receiveUnshielded`/`sendUnshielded` on `nativeToken()`; the secret arrives as a **private circuit argument** (`_secret: Bytes<32>`), not a witness.

**Feeds our backlog:** [#2 Private Voting] (allow-list + commit-then-open attendance), [#5 Eligibility-Without-KYC] (private allow-list pattern), [#1 NightAttest] (event entry = a one-shot credential; the guide's DUST sponsorship shows gasless onboarding), [#9] (state machine + privacy boundary as teaching pattern).

### 4. token-transfers

**What it does:** a contract that exercises Midnight's token primitives end to end — minting, sending, receiving across all three flavors: contract-domain **unshielded** tokens, native **NIGHT**, and **shielded** (Zswap) coins. Each circuit is one focused operation, not an application.

**Privacy pattern:**
- Private: shielded coins move at protocol level (Zswap reveals nullifiers, not coins or owners).
- Proven publicly: each circuit's validity; notably `receiveShielded(disclose(coin))` **discloses the coin's nonce/color/value** — the contract keeps a public view of every coin it touches.
- On the public ledger: every unshielded/NIGHT amount and address; for shielded ops, the disclosed coin metadata (fine for a contract that owns the coins — the tip-jar example relies on exactly this — but it would link anyone who knew that coin to the contract flow).

**Compact features used:** `mintUnshieldedToken` (domain-separated color, mint-to-self via `kernel.self()`), `tokenType`, `sendUnshielded`/`receiveUnshielded`, `nativeToken()`, `receiveShielded`, `sendShielded` (qualified coin → `ZswapCoinPublicKey`), `mintShieldedToken` (domain + nonce), `mintAndSendShielded`; return types `ShieldedCoinInfo` / `ShieldedSendResult`.

**Feeds our backlog:** the plumbing layer for [#3 Sealed-Bid Auction] and [#4 Confidential Freelance Escrow] (shielded fees, locked funds, colored settlement tokens) and [#6 Private Payroll Proof] (payday token moves). No application-level privacy of its own.

### 5. battleship

**What it does:** a two-player board game on a single number line (2 ships each); the teaching target is **defensive programming**: a state machine, role checks, and on-chain verification of off-chain data that catches cheating. Step 3 of the learning path.

**Privacy pattern:**
- Private: each player's ship positions — stored only as a `Set` of `persistentHash([x, sk])` (hash of position with the player's own secret).
- Proven publicly (per shot): the shot coordinate, the caller's role, turn order, and — the key move — that the claimed HIT/MISS from the defender's private state is consistent with the defender's committed board: on MISS `assert(!board.member(commitBoardSpace(shot, sk)))`, on HIT `assert(board.member(...))`. The witness `localCheckBoard` supplies the claim; the contract re-derives the membership check on-chain, so a dishonest defender cannot flip a HIT to a MISS (and "Cheat Detected" asserts exist for repeat-hit and out-of-turn plays too).
- On the public ledger: both players' DApp keys, all shots (`disclose(x)`), hit/miss records, turn state, winner.

**Compact features used:** `witness localSetBoard/localCheckBoard/localSk` (intermediate witnesses — claims about private state); `Set`/`List`/`Counter` collections; three enums driving the turn state machine; DApp-key role checks (`getDappPubKey`, domain `"battleship:pk:"`); hash commitments to private data with re-verification.

**Feeds our backlog:** the "claim from private state, re-verify against an on-chain commitment" idiom is the core mechanic for private multi-round games and for [#3] (a losing bidder's claim that "I was below the winner" can be re-verified this way); the state-machine + role-check scaffold is reusable in [#9] as a pattern card.

### 6. silent-auction

**What it does:** a "silent" auction for a single NFT: the organizer commits to a secret reserve price that stays hidden from bidders until bidding closes, while the bids and the current high bid are public. Combines commit-reveal with a `RECEIVE → OPEN → CLOSED → PAID` state machine, a 50-NIGHT organizer deposit (incentive to settle), and unshielded token flows throughout.

**Privacy pattern:**
- Private until `revealWin`: the reserve price — held on-chain only as `persistentCommit(minPrice, salt)` (`hiddenPrice`, salt from a witness); at reveal the organizer re-derives the commitment and the price is disclosed. A mismatch is caught ("Attempt to change min price detected").
- Proven publicly: that the organizer deposited 50 NIGHT, that the NFT was minted (unshielded token, color from domain `"auction:nft"`, quantity 1), that each bid is higher than the bidder's previous one, and at settlement that the winner paid `highestBid`.
- On the public ledger: every bid amount, every bidder's DApp key, the high bid, the reserve (after close), the NFT transfer, and all NIGHT flows.

**Compact features used:** `sealed ledger` for organizer/hiddenPrice/maxBids/deposit; `persistentCommit` commit-reveal; DApp-key organizer gating (`"silent-auction:pk:"`); `mintUnshieldedToken`/`sendUnshielded`/`receiveUnshielded`; explicit state machine; organizer cannot bid.

**Feeds our backlog:** the reserve + settlement half of [#3 Sealed-Bid Auction] — but with **public** bids, which is exactly what our idea says should be private; the deposit-to-incentivize-settlement move is directly reusable in [#4 Confidential Freelance Escrow].

### 7. private-bid

**What it does:** the contract from the docs guide ["How to build a private smart contract"](https://docs.midnight.network/guides/build-a-private-smart-contract) (note: that guide page currently carries the ledger-8 banner; this is its ledger-9 port in the monorepo). A bid must meet a public minimum; the network learns that some bidder placed a bid ≥ minimum — not the amount, not the wallet — and the bidder may reveal later.

**Privacy pattern:**
- Private until `revealBid`: the bid amount — the ledger holds only `persistentCommit(amount, salt)`. The source explains why not `persistentHash`: bid amounts are guessable, so a salt is required.
- Private always: the secret key (private state via the `localSecretKey()` witness).
- Proven publicly (per bid): that `amount >= minimumBid` — the comparison runs **inside the proof** (`assert`), so a failing bid aborts on the bidder's machine before any proof is generated.
- On the public ledger: `sealed` `minimumBid`; per bid, a derived bidder key (`persistentHash([purpose, kernel.self().bytes, sk])` — bound to this deployment, so the same secret in another contract is unlinkable) and the commitment; the bid count. On reveal: the amount.

**Compact features used:** `sealed ledger`; `witness`; domain-separated, contract-bound derived keys; `persistentCommit` with a **deterministic salt** (safe only because each bidder commits once — the source warns that contracts committing repeatedly need a fresh random salt kept in private state, and that the compiler does not check salt entropy); `Map<Bytes<32>, Bytes<32>>` commitments; compiler-enforced disclosure discipline (removing the one `disclose(amount)` in `revealBid` fails compilation).

**Feeds our backlog:** the bid-privacy core of [#3 Sealed-Bid Auction]; the "prove a property of a private value without revealing it" idiom generalizes to [#5] and [#6]. The deterministic-salt caveat is a pattern-library trap worth a snippet in [#9].

### 8. private-tip-jar

**What it does:** anyone can drop a shielded coin into a contract-held pot without revealing who tipped; only the jar's owner can withdraw. Step 4 of the learning path; ships with a bare demo token (`tip-token.compact`) because a fresh devnet has no shielded tokens to tip with.

**Privacy pattern (from the contract's own documented model):**
- Private: **who tipped** — the tipped coin is *not* a wallet coin; the tipper's client mints a **new coin with a fresh random nonce** for the contract, and the wallet pays for it from whatever coins it holds. No wallet coin shares that nonce, so the public pot entry points back at nobody (Zswap reveals nullifiers, not coins).
- Private: the owner's secret — only the domain-separated hash `persistentHash(["private-tip-jar:owner:"], sk)` is stored (`sealed`).
- Public: each tip's **value** (the pot stores full coins, because the contract must be able to spend them later), the token color, the number of tips, when each `tip`/`withdraw` happened (circuit names are public), and which pot coin each withdrawal took. An observer sees "someone tipped 40" and not who.
- Documented client duty + residual: the client must supply a fresh nonce (a reused wallet-coin nonce would identify the wallet to anyone who can guess the key — the issuer can); a withdrawal's payout nonce is derived from a public pot nonce, so a wallet-key guesser can confirm the owner's receipt. The owner is a public role by design.

**Compact features used:** `sealed ledger` (owner + accepted color, no transfer/hijack); `receiveShielded` + `insertCoin` (records the Merkle-tree index so the coin is spendable later); `sendShielded(coin, ownPublicKey(), value)` — `ownPublicKey()` used only to *route* funds, never to authenticate; `witness ownerSecretKey()` for the owner-only check; `pure circuit ownerKey` so off-chain code/tests can compute the expected hash; pot-overwrite guard (`!pot.member(coin.nonce)` before insert).

**Feeds our backlog:** the tip-jar pattern itself (gasless sponsorship is demonstrated on its sibling private-party via the [DUST guide](https://docs.midnight.network/guides/dust-sponsorship)); the fresh-nonce contract-coin technique is the custody primitive for [#4 Confidential Freelance Escrow]; the documented-leak style of its comments is the template for [#7 "What Does This dApp See?"] and the [#8] directory's data.

### 9. shielded-chips (two contracts)

**What it does:** a worked example of **shielded tokens**: `chips.compact` is a native shielded token conforming to [MIP-0011](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0011-native-shielded-token.md) (house-only mint/burn, supply accounting, treasury), and `roulette.compact` is a RED/BLACK roulette that custodies chip coins privately and pays winners 2×.

**Privacy pattern:**
- Private (chips): **mint recipients** — `mint` takes a caller-supplied uniformly-random nonce, so the commitment is unlinkable to the recipient; the contract deliberately does **not** compose MIP-0011's derived-nonce extension, which the MIP calls out as *recipient-public* (the source notes an earlier version kept a public nonce chain and every mint recipient was identifiable from chain data alone).
- Private (chips): the *amounts* of bare burns (composing the supply counters makes contract-mediated burn amounts public — the counter write is what puts the amount in the transcript; `totalBurned` is a lower bound, since holders can also burn from their wallets without the contract).
- Private (roulette): the player's wallet identity — the on-chain pseudonym is `persistentHash(["roulette:pk:"], sk)`, and **no player coin nonce is ever stored** (the escrowed bet is stored only as `persistentCommit([nonce, color, value], escrowSalt)`, because any coin nonce in public state is a linkability oracle against a guessed wallet key).
- Private (roulette): the winning number until `revealWinningNumber` — only `persistentHash([num, sk])` is on chain before the reveal.
- Public (roulette): the pseudonym, bet color, bet value, chip color, the payout record, and every house coin — an observer can follow one player's behavior within the contract, just not identify them. The source calls this a deliberate **"identity-private, behavior-public"** tradeoff.
- Payout privacy: `claimWinnings` merges the player's secret escrow coin **first** (`mergeCoin` derives the merged nonce from its first input), so the 2× payout coin's nonce is non-recomputable by observers.

**Compact features used:** MIP-0011 metadata + `tokenColor()` (computed at call time, never precomputed); `mintShieldedToken` with `Either<ZswapCoinPublicKey, ContractAddress>` recipients; transient spend `sendImmediateShielded` for same-transaction burns (to `shieldedBurnAddress()`) vs Merkle-spend `sendShielded` for treasury burns; change persistence (drop it and it's stranded); `isZeroTarget` guards (zero address *is* the burn address); `requireHouse()` — the hash-based commitment authorization pattern the MIP points to; cross-contract identity (same `getDappPublicKey` derivation in both contracts so one operator = one identity); `insertCoin`/`pot`-style maps of `QualifiedShieldedCoinInfo`.

**Feeds our backlog:** the shielded-fee + custody stack for [#3] and [#4]; the MIP-0011 reference implementation for [#9]; the commit-then-merge payout trick for any private prize distribution (ROSCA payouts); the documented privacy-cost table (what each accounting choice leaks) is the model for honest privacy claims in [#8].

### 10. zk-loan

**What it does:** a privacy-preserving credit scorer: the applicant's profile (score, income, tenure) never leaves their machine; a registered attestation provider signs it off-chain; the contract verifies that Schnorr signature **inside the ZK proof** (Jubjub) and runs tier rules; only the outcome — status and authorized amount — reaches the ledger. Ported from [`midnightntwrk/example-zkloan`](https://github.com/midnightntwrk/example-zkloan). Step 6 of the learning path.

**Privacy pattern:**
- Private always: the credit profile — the witness returns it, but it is trusted only *after* in-circuit signature verification; it is never written to the ledger.
- Proven publicly (per application): that a **registered** provider signed this exact profile for this exact identity (the signature message is `[score, income, tenure, transientHash(userPubKey)]` — the key-hash binding is the anti-replay check: an attestation issued to someone else fails), and that the tier rules were applied honestly.
- On the public ledger: per user, derived identity keys (`"zkloan:user:pk:v1"` PIN-bound user key, `"zkloan:admin:pk:v1"` admin key — domain-separated nominal `new type`s so a user key can't be passed where an admin key is expected), the provider id, and only `LoanApplication { authorizedAmount, status }` (Approved / Rejected / Proposed / NotAccepted).
- Explicit anti-pattern, stated in source: `ownPublicKey()` is **never** used for authorization — it is a prover-claimed witness value with no binding to the signer, so any check on it is bypassable.

**Compact features used:** `import "schnorr" prefix Schnorr_` — a Compact **module** (not a contract) implementing Schnorr-on-Jubjub verification, itself witness-assisted for the expensive reduction (the witness supplies quotient/remainder of the challenge divided by 2^248; the circuit range-checks and re-derives — off-chain does the division, the circuit only checks, the same idiom as calculator); `pure circuit`s exporting key derivation to TypeScript; `witness getAttestedScoringWitness(): [Applicant, SchnorrSignature, providerId]`; in-proof tier rules over private inputs; nested `Map` with auto-incrementing ids; **fixed-size batching** — `changePin` migrates loans 5 per transaction with a ledger cursor because circuits can't loop over variable-length collections; `rotateAdmin` passes the role to a *public* derived key (no secret ever leaves either party); blacklist + PIN-migration bookkeeping.

**Feeds our backlog:** the attestation pattern is the engine of [#5 Eligibility-Without-KYC] and [#6 Private Payroll Proof] (salary-above-threshold = signed profile + in-circuit range check — though see the gap list on leaking tiers); the provider-registry + identity-rotation machinery is infrastructure any issuer-based build (NightAttest's issuer role) will reuse.

### 11. leaderboard

**What it does:** an arcade-style leaderboard with a browser UI, deployed as a static site on Vercel: every score submission is a new public entry under a chosen name or an anonymous derived name, and each entry stores a commitment to the player's key so the player can later **prove ownership with a ZK proof without revealing the key**. Step 7 of the learning path; the only example that ends as a deployed website (React + Lace wallet, TypeScript API shared by browser and Node).

**Privacy pattern:**
- Private: the player's secret key — `ownerHash = persistentHash(["leaderboard:owner:"], sk)` is the identity.
- Proven publicly: in `verifyOwnership`, that the caller's `ownerCommitment(sk)` matches the entry's stored `ownerHash` — the minimal credential-presentation proof (prove "this entry is mine") without revealing the preimage.
- On the public ledger: every score, display name (chosen or `persistentHash(sk)`), owner hash, and entry id.

**Compact features used:** `witness localSecretKey()` + `witness getCustomName()` (private data fed **on demand** into a circuit); `Map<Uint<64>, ScoreEntry>` with `Counter` auto-incrementing keys; in-circuit ownership equality; `Opaque` string display via 32-byte names.

**Feeds our backlog:** the per-entry ownership proof is the closest existing cousin of [#1 NightAttest]'s *presentation* leg (a portable "I control this credential" proof) — though see the gap list: it's per-entry, not a reusable credential; its public/private surface (documented in its AGENTS.md) is a ready-made sample for [#7 "What Does This dApp See?"] and a first row for the [#8] directory.

### 12. voting

**What it does:** anonymous, allow-listed voting on one proposal (built for MIP/MPS-style votes): the admin enrols a fixed list of voter keys (≤1024) and opens voting with a deadline; each voter casts one yes/no/abstain ballot in one transaction; after the deadline, encrypted ballot openings are checked against commitments and tallied publicly.

**Privacy pattern:**
- Private: **the vote itself** and **which enrolled voter cast it** — on chain a ballot is only a hiding commitment (`persistentCommit(vote, salt)`) plus a copy encrypted to the admin's X25519 keys.
- Proven publicly (per cast): Merkle **membership** in the enrolment tree (`voters: MerkleTree<10, _>` checked via a `MerkleTreePath` witness — the cast never names the voter); that the ballot's recipients are all published admin keys (a substituted key fails against real ledger state — the proof can't see inside the ciphertext, the honest client seals to the right keys).
- One-vote enforcement: a **nullifier** `persistentHash(["mip-vote:cast"], sk, proposalId, kernel.self().bytes)` bound to the proposal *and the contract instance* — rehearsal and real votes (or cloned deployments) can't be linked or replayed; `!ballots.member(n)` rejects a second cast.
- Public: the nullifier, the commitment, the ciphertext, the tally counters, deadlines. Counting is checkable by **anyone** (a voter can count their own ballot if the admin doesn't), and `count` verifies the opening against the stored commitment, so the admin can't change a vote — the admin decrypts, it doesn't decide.

**Compact features used:** `MerkleTree`/`MerkleTreePath`/`checkRoot` membership proofs; `sealed` admin key + proposal id; up to 8 admin encryption keys with an in-circuit **low-order-point rejection list** (14 X25519 low-order points, each ±top bit — a ballot sealed to one would be unusable); `Opaque<"Uint8Array">` ciphertext fields; `persistentCommit` vote commitments; `Counter`s for enrolled/cast/tallies; `blockTimeLt`/`blockTimeGte` deadline + 48-hour counting window; `pure circuit`s for all the derivations (nullifier, salt, commit, admin key) so off-chain code computes the same values.

**Feeds our backlog:** the core of [#2 Private Voting] — this *is* an anonymous allow-listed vote with a public auditable result, which is exactly backlog #2's spec; the nullifier + Merkle-membership + commit-then-open + anyone-can-count quartet is the pattern card for #2 and reusable for any eligibility-gated action (#5's "proven eligibility" leg).

## Gap list — patterns no example covers

Patterns our backlog builds would have to **invent** (or synthesize from the pieces above), because no example demonstrates them:

1. **Portable, reusable credentials (NightAttest core).** No example issues a private credential that a holder can later *present repeatedly to different verifiers*. zk-loan's attestation is consumed inside one proof against one contract; the leaderboard's ownership proof is per-entry. Issuance → repeated private presentation → in-circuit revocation is unrepresented. NightAttest must be invented (SOL-2).
2. **Sealed-bid auction with private bids AND private reserve AND funded settlement.** `silent-auction` has a secret reserve but **public** bids and no private settlement; `private-bid` has private bids but no reserve, no funds, no settlement. No single example combines private bid + secret reserve + winner proven without revealing losing bids + money movement — that synthesis *is* backlog #3.
3. **Multi-round / outbidding privacy.** No example supports iterative bidding where a new private bid must beat a private high bid (the comparison in private-bid is against a public constant; battleship's multi-round state is fully public).
4. **Timelocked / dispute-capable escrow.** No example has a time-locked refund, a dispute path, or partial refunds; the closest (silent-auction's 50-NIGHT deposit) is one-way and unconditioned. Backlog #4's "funds proven locked, delivery proven" has no example for the *delivery proven* half at all.
5. **Range-proof selective disclosure without leaking tiers.** zk-loan discloses the tier *and* which provider attested; no example does a pure "value ≥ threshold ⇒ true, nothing else" disclosure. Backlog #5/#6 need the boolean-only variant.
6. **Multi-party fairness / verifiable randomness.** Roulette's winning number is a single house secret (a trusted house). No example has multi-party commit-reveal pools, VRF, or threshold randomness — a rotating-savings (ROSCA) receiver-order draw would need one.
7. **ROSCA / rotating savings and lending circles.** No example: no per-member private contribution ledger against a public round counter, no private draw of the rotation order, no shielded payout sequencing. (ROSCA is on the team's idea list, not yet in `ideas/backlog.md`.)
8. **Cross-contract private transfers (C2C).** `chips.compact` states that a coin addressed to a *different* contract "would be rejected by the node until C2C calls land" — so multi-contract private flows (e.g., a NightAttest credential contract coordinating with an escrow contract) are currently a protocol-level gap, not a design gap; no example demonstrates C2C.
9. **Threshold / multi-party authority.** Every privileged role (organizer, house, admin, tip-jar owner) is a **single secret**; `rotateAdmin` hands one key to one successor; voting supports up to 8 *encryption* keys but a single admin secret still gates enrol/open. No threshold decryption, no multisig reveal, no committee.
10. **Recurring / scheduled payments.** No example covers subscriptions, payroll cycles, or any timer-driven private payment (only `blockTime` deadline checks for voting windows).
11. **Shielded-value transactions with sponsored DUST.** The DUST-sponsorship guide's reference implementation is private-party's **unshielded** NIGHT flow; a shielded-value transaction with a gasless DUST sponsor is not demonstrated in the examples.
12. **Selective decryption by a third party.** Voting's ciphertexts are sealable only to admin keys; no example lets a data owner encrypt to a *chosen external* verifier (e.g., a regulator or counterparty) with the same commitment-checked opening.

## Cross-cutting building blocks (as they recur)

- **DApp-key pseudonyms:** `persistentHash([domain, sk])` gates roles without exposing wallets and is unlinkable across dapps (private-party, silent-auction, battleship, shielded-chips, zk-loan, voting, tip-jar, leaderboard).
- **Salted commitments:** `persistentCommit(value, salt)` for private lists, bids, reserves, votes; the private-bid source documents the guessability trap and the single-commit-only-safety of deterministic salts.
- **Fresh-nonce contract coins:** for any shielded value a contract must hold, the client mints a *new* coin under a fresh nonce (tip-jar, roulette) so public pot/escrow nonces never link back to wallets — and the escrow-never-store-the-coin discipline of roulette.
- **Witness claims, re-verified:** the off-chain witness supplies data/claims; the contract re-derives the check on-chain (calculator → battleship → zk-loan → roulette).
- **Disclose-only-the-outcome:** zk-loan discloses a status+amount; private-bid discloses a boolean-ness of "≥ min"; the compiler enforces the boundary (undeclared disclosure fails compilation).
- **`sealed` + state machines:** constructor-frozen parameters plus explicit enums for every example with lifecycle.
- **Anti-pattern, documented everywhere:** never authorize with `ownPublicKey()` — use it only to route funds (stated in zk-loan, chips, and the DUST guide).

## Verified live (2026-11-24)

| Link | Check |
|---|---|
| [midnight-examples repo](https://github.com/midnightntwrk/midnight-examples) | cloned + fetched; HEAD `953f864` on `main` |
| 12 example tree paths (`examples/<name>` under main) | all 12 fetched, HTTP 200 |
| [docs.midnight.network/tutorials](https://docs.midnight.network/tutorials) | fetched; lists private-party, battleship, private-tip-jar, zk-loan, leaderboard |
| [docs.midnight.network/getting-started/hello-world](https://docs.midnight.network/getting-started/hello-world) | fetched (quick start; see note: it targets the standalone `example-hello-world` repo, toolchain 0.36.0) |
| [docs.midnight.network/tutorials/private-party](https://docs.midnight.network/tutorials/private-party) | fetched |
| [docs.midnight.network/tutorials/battleship](https://docs.midnight.network/tutorials/battleship) | fetched (page footer: last updated Nov 24, 2026) |
| [docs.midnight.network/tutorials/private-tip-jar](https://docs.midnight.network/tutorials/private-tip-jar) | fetched |
| [docs.midnight.network/tutorials/zk-loan](https://docs.midnight.network/tutorials/zk-loan) | fetched |
| [docs.midnight.network/tutorials/leaderboard](https://docs.midnight.network/tutorials/leaderboard) | fetched |
| [docs.midnight.network/guides/build-a-private-smart-contract](https://docs.midnight.network/guides/build-a-private-smart-contract) | fetched; **carries the ledger-8 banner** (consistent with HERMES-1 section B) |
| [docs.midnight.network/guides/dust-sponsorship](https://docs.midnight.network/guides/dust-sponsorship) | fetched; reference implementation is `examples/private-party` |
| [MIP-0011 native shielded token](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0011-native-shielded-token.md) | HTTP 200 |
| [MIP-0004 fungible token standard with UTXO](https://github.com/midnightntwrk/midnight-improvement-proposals/blob/main/mips/mip-0004-fungible-token-standard-with-utxo.md) | HTTP 200 |
| [example-zkloan (port source)](https://github.com/midnightntwrk/example-zkloan) | HTTP 200 |

Docs-source cross-check: all nine `docs.midnight.network` paths above also exist in `midnightntwrk/midnight-docs` at `origin/main` (fetched 2026-11-24) — the doc-source clone is this repo's `.ref/midnight-docs`.

## Not verified / for Muse

- **Pin discrepancy:** the monorepo pins `@midnight-ntwrk/dapp-connector-api` **4.0.1**, while the merged HERMES-1 pin sheet records npm latest / support matrix **4.1.0** (2026-11-23). I treated the monorepo pin as the working CI-verified combination and did not test 4.1.0. Worth a one-line decision in the pin sheet or a coordinated bump in the monorepo (its rule is "version changes happen in a coordinated pass").
- **Docs quick-start drift:** the live hello-world quick start targets the standalone `midnightntwrk/example-hello-world` repo (Yarn, toolchain **0.36.0**) rather than the monorepo (npm, compiler 0.36.1). Flagged as an observation; both are current-generation.
- No example test suite was run this session (no Docker network stood up for research); "compiles and runs in CI" is the repo's own claim, and I read sources rather than executed them.
- `experimental/q2-ledger9` read at README level only (stagenet harness; its `COMPATIBILITY.md` notes parts of its stack are still pre-release). Not part of the example catalogue.
- No testnet or mainnet interaction of any kind; no wallets, keys, sign-ups, or funds touched.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
