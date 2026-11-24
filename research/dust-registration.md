# HERMES-5 — DUST registration explainer

Research brief for the Midnight Dapps, Sites & Research council (HERMES seat).

- **Run:** 2026-11-24. Every source below was fetched live in this session; version and parameter values are the ones the official pages carried that day.
- **Scope:** what DUST registration is, the generation model it feeds, the first-registration fee problem, every official way to register (Lace, wallet SDK, node toolkit, cNIGHT on Cardano), redirect/stop, the ledger 9 reset, and what is modelled vs live.
- **Method:** official sources only — docs.midnight.network and the midnightntwrk GitHub org. Model/estimates are labelled as such. No transaction was executed in this run (research only; no funds, no signing).
- Testnets in this document are the **Preview / Preprod / local devnet** networks — labelled testnet wherever they appear. Nothing here is a mainnet operation except the quoted mainnet parameters and the ledger 9 mainnet reset, which are read from official pages, not performed.

---

## 1. In one paragraph

On Midnight, every transaction consumes DUST — a shielded, non-transferable fee resource that is not a token and cannot be bought or transferred. DUST is generated over time by NIGHT UTXOs, but **only NIGHT UTXOs that have been registered for DUST generation**. Registration is a transaction (the on-chain operation the protocol and SDKs also call "registration") that sets a table entry in the ledger linking a NIGHT address to a DUST address, causing the UTXOs you pass to spawn DUST UTXOs that grow from zero to a cap of 5 DUST per NIGHT over about a week. A first registration has no chicken-and-egg problem: it pays its own fee out of the DUST the NIGHT *would have generated* since the UTXOs were created, so a wallet with zero DUST can still make it. Once registered, NIGHT that later arrives at the same address generates DUST with no further call.

## 2. Why DUST exists: NIGHT vs DUST

From the official tokens overview and the DUST architecture page:

- **NIGHT** is Midnight's native token. Atomic unit STAR, `1 NIGHT = 10^6 STAR`.
- **DUST** is the native fee resource. Atomic unit SPECK, `1 DUST = 10^15 SPECK`. "DUST is a shielded, non-transferable resource (not a token)." Its only use is paying transaction fees. The resolution is deliberately much finer than NIGHT's so fee payments are fine-grained while the maximum supply still fits a `u128` (ledger spec).
- The docs' standing analogy: NIGHT is the solar panel, DUST is the electricity — the panel produces, the electricity pays for what you do.
- **DUST is not persistent.** The ledger spec states the protocol "reserves the right to freely modify Dust allocation and allocation rules", including resetting all existing Dust (garbage collection). The ledger 9 upgrade exercised exactly this (section 9 below). Plan products accordingly: DUST balances are a derived, time-based quantity, never stored value.
- The value of a DUST UTXO "is not stored anywhere. It is computed from time, so every read is a calculation, and the code that reads it has to say for which moment" (manage-dust, "How DUST behaves").
- **tNIGHT/tDUST are display names only** for test networks; "the types, the units, and the API are the same on every network."

## 3. What registration changes in the ledger

"Registration is the transaction that makes NIGHT UTXOs generate DUST for a DUST address" (manage-dust, "Registration rules").

The ledger mechanism (midnight-ledger `spec/dust.md`):

- NIGHT and DUST use different key material. A **table** (the spec's `address_delegation` map) tracks which DUST public key is associated with which NIGHT public key.
- A new NIGHT UTXO creates a corresponding DUST UTXO (initially value 0) **if and only if** its NIGHT address has a table entry; the DUTXO is created for the DUST address recorded at that entry.
- A `DustRegistration` carries `night_key` plus an optional `dust_address`: `Some(dust_addr)` inserts/updates the entry (register or redirect); `None` removes it (deregister). It is signed with the NIGHT secret key.
- Each generating NIGHT UTXO has exactly **one DUTXO**; the wallet's DUST balance is the sum of its DUTXOs valued at the read time.
- Registrations are processed sequentially, in segment-ID order, inside the guaranteed section of the transaction; the wallet spec treats DUST spends as guaranteed-section while DUST registrations sit in the intent's fallible section.

## 4. How DUST generation works (the model registration feeds)

From manage-dust ("How DUST behaves", "DUST generation parameters") and the ledger spec:

- While the backing NIGHT UTXO is unspent, its DUTXO **grows linearly** to a cap proportional to the NIGHT amount, then stays at the cap. Once the NIGHT UTXO is spent, the value **decays linearly at the same rate** to zero — and DUST can still pay fees while it decays.
- Any spend of a registered NIGHT UTXO starts decay, including a transfer to your own address. Change outputs at a registered address get **new DUTXOs starting from zero**. A wallet's DUST balance can therefore fall while its NIGHT total is unchanged.
- A fee payment **replaces** the DUTXO with a successor: same owner, same backing, value minus the fee (a 1-to-1 "transfer" in the spec's terms, with a ZK proof; there is no change and no refund — "a DUST spend has no refund, so the wallet spends the whole declared fee, overhead included").
- The fee amount is public in a transaction; the paying DUTXO and remaining balance are shielded. The **maximum** DUST a DUST address can hold is publicly computable (the backing NIGHT amounts are public).

### Parameters

Initial values from the ledger spec (`INITIAL_DUST_PARAMETERS`); live Mainnet values as printed by the official manage-dust guide's own run (2026-09-30, per that page):

| Field | Meaning | Initial (spec) | Mainnet (docs run, 2026-09-30) |
| --- | --- | --- | --- |
| `nightDustRatio` | DUTXO cap per STAR of backing NIGHT | 5,000,000,000 SPECK/STAR = **5 DUST per NIGHT** | 5000000000 |
| `generationDecayRate` | value change per second per STAR (rise and fall) | 8,267 SPECK/STAR/s | 8267 |
| `dustGracePeriodSeconds` | max age of the declared tx time vs block time (global TTL; also allows backdating spends up to this window) | 3 hours | 10,800 s |
| `timeToCapSeconds` | read-only, derived `nightDustRatio / generationDecayRate` rounded up | — | 604,815 s ≈ 1 week |

The time to cap is the same for **any** NIGHT amount (cap and rate both scale with `night`, so it cancels). "These are ledger parameters, not constants… governance can replace it" — read them at runtime from the indexer's `ledgerParameters` on the latest block; the docs' Mainnet numbers are a dated sample, not a constant.

Worked example from the official docs: with the values above, **1,000 NIGHT backs a cap of 5,000 DUST**, generates **0.008267 DUST/s (~71 DUST/day)**, and takes **604,815 s (~1 week)** from empty to cap.

**[MODEL]** All of the above is the protocol's defined model as specified; it is not a measured live rate. Our `apps/dust-planner` (GROK-2) implements exactly these constants — see section 12.

## 5. The first-registration fee problem — and its solution

Registration itself costs a fee, but the wallet has no DUST yet. The official resolution (dust-architecture, "Registrations and fees"; manage-dust):

- A non-generating NIGHT UTXO is treated as if it had been **generating DUST all along since its own creation time** — "virtually" accumulating at the same rate and cap. The docs: "A first registration pays its own fee from the DUST that the NIGHT being registered would have generated since it was created, so a wallet with no DUST can still submit it."
- The registration declares a fee allowance (`allow_fee_payment`). The ledger pays `min(remaining_fees, min(allow_fee_payment, dust_in))` from that virtual generation, where `dust_in` is what the qualifying inputs would have generated (spec `apply_registration`). If other parts of the transaction already cover the fee, none of the virtual DUST is taken and the new DUTXOs keep more.
- Only **inputs in the guaranteed section** that are *not* already generating DUST count toward this. Exceeding the accumulated amount fails with **`InsufficientDustForRegistrationFee`**.
- Consequence: a **just-received UTXO has accumulated almost nothing**, so registration of freshly funded NIGHT must wait (or the fee must come from an already-registered wallet).
- The SDK encodes this as a wait: "A first registration pays its own fee from the DUST that the NIGHT would have generated since it arrived. `estimateRegistration` returns the amount to reach, and `waitForGeneratedDust` waits until a UTXO has generated it. **Without the wait, a registration built too soon after the funds arrive throws before the wallet signs anything.**"
- "A first registration needs no DUST balance and no proof. It carries a signature and no DUST spend, so `finalizeRecipe` resolves without a proof server."

## 6. Registration rules and limits

From manage-dust, "Registration rules" (each bullet is the official rule, condensed):

- **Covers what you pass.** UTXOs left out stay unregistered; NIGHT that arrives later at a registered address generates DUST with no further call.
- **Does not consume NIGHT.** The call spends the UTXOs you pass and recreates them as at most two UTXOs with the same total. Identifiers and creation times change — re-read `state.unshielded.availableCoins` after confirm.
- **No fixed minimum, but a UTXO can be too small to register.** One UTXO in the call must be old enough that the DUST it would have generated since creation pays the registration's own fee (capped at that UTXO's cap). `estimateRegistration` returns the amount to reach **including your `additionalFeeOverhead`**; `waitForGeneratedDust` resolves when a UTXO reaches it and rejects if its timeout expires first (pass a longer `{ timeoutMs }` for small/new UTXOs). A UTXO whose cap is below the fee never reaches it.
- **No DUST balance or proof needed for a first registration** (signature only). Changing or removing a registration later pays with a DUST spend → needs a DUST balance and the proof server.
- **The `registeredForDustGeneration` flag is fixed at UTXO creation** and describes one UTXO; `true` means it generates DUST, but the flag does not say *which* DUST address receives it (query the generation info to find out).

## 7. How to register — the official methods

### 7.1 Lace wallet (testnets)

From the acquire-tokens guide (testnets only in this procedure):

1. Fund the unshielded address from the network faucet — **5,000 tNIGHT per faucet request** (Preprod: `midnight-tmnight-preprod.nethermind.dev`, Preview: `midnight-tmnight-preview.nethermind.dev`; captcha-gated; the faucet rejects shielded and DUST addresses).
2. In Lace, open your Midnight wallet and select **Generate tDUST** (the docs: "Lace labels the button **Generate tDUST** and displays the result as your tNIGHT designation"). Your tDUST address populates the input field; select **Review transaction**, then **Confirm**.
3. Verify: "The tDUST tank starts filling and continues to accrue over time, up to a cap set by how much NIGHT you registered."

Holding NIGHT alone does nothing — "the registration transaction is what starts generation."

### 7.2 Wallet SDK (scriptable; current SDK set)

Verified live 2026-11-24: the current funding guide targets the ledger-9 SDK set — **`@midnightntwrk/wallet-sdk` 1.2.0** (single barrel package; note the scope has no hyphen) with `@midnight-ntwrk/midnight-js-network-id` / `midnight-js-protocol` / `midnight-js-utils` **4.1.1**, Node.js 22+, a local proof server on port 6300, and for Preprod a Blockfrost **Midnight Preprod** project token in `BLOCKFROST_PROJECT_ID` (Blockfrost now serves the Preprod indexer + node RPC).

The registration core (acquire-tokens, "Registering NIGHT for DUST generation with the wallet SDK"):

```typescript
const state = await wallet.waitForSyncedState(); // registration needs a fully synced wallet

const unregistered = state.unshielded.availableCoins.filter(
  (coin) => coin.meta?.registeredForDustGeneration !== true,
);

const { fee } = await wallet.estimateRegistration(unregistered);
await wallet.waitForGeneratedDust(unregistered, fee); // wait until the NIGHT has generated the fee

const recipe = await wallet.registerNightUtxosForDustGeneration(
  unregistered,
  unshieldedKeystore.getPublicKey(),
  (payload) => unshieldedKeystore.signData(payload),
  dustReceiver, // optional 4th arg: direct the DUST to another wallet's DUST address
);
const finalized = await wallet.finalizeRecipe(recipe);
await wallet.submitTransaction(finalized);
```

- `MidnightBech32m.parse(target).decode(DustAddress, networkId)` is also the **validation step**: it throws on anything that is not a DUST address for the current network, so a shielded/unshielded address pasted by mistake fails here (the docs' troubleshooting table lists the resulting `Invalid dust address` symptom).
- `costParameters` on the DUST wallet: `additionalFeeOverhead` (docs example `300_000_000_000_000n` = 0.3 DUST) is added to every declared fee — and a DUST spend returns none of it; `feeBlocksMargin` (docs example `5`) is the blocks of finalization the fee estimate allows for.
- Generation begins once the registration transaction lands on-chain; watch `state.dust.balance(new Date())` turn positive.
- The guide's worked outputs (e.g. `tNIGHT received: 1000.000000`, `DUST balance: 2485.035398999999999`) are the **docs' own test output**, not values re-run here.

⚠️ **Docs currency note:** the separate official page `sdks/official/wallet-developer-guide` is still **ledger-8-era**: it installs `@midnight-ntwrk/ledger-v8` + `@midnight-ntwrk/wallet-sdk-*` packages and states the DUST role as `4 (Roles.Dust)`. The WalletEngine architecture spec (section 10) assigns DUST role **2** (role 4 is Metadata), and the current funding guide uses the hyphen-less `@midnightntwrk/wallet-sdk` barrel. Treat the wallet-developer-guide page as stale for new builds; this is flagged for the upstream docs issue.

### 7.3 Node toolkit

Verified live 2026-11-24 (node-toolkit page): the toolkit (current version **1.0.300** per that page) exposes DUST operations including `register-dust-address` and `dust-balance`. Two official behaviours worth knowing: replay-based commands (which re-derive state) **take hours on public networks**, and the documented `Insufficient DUST` error is the expected failure mode when a fee cannot be covered.

### 7.4 cNIGHT held on Cardano

From manage-dust ("DUST from NIGHT held on Cardano", "Registering cNIGHT for DUST on Mainnet") and dust-architecture ("The implementation"):

- NIGHT held as **cNIGHT on Cardano** generates DUST via a registration that is made **on Cardano**, not on Midnight: a Cardano transaction names your **stake credential** plus the Midnight **DUST address**. Exactly **one valid registration per stake address**; re-registering replaces it.
- Only cNIGHT UTXOs **created while the registration is in effect** generate; moving cNIGHT (creating new UTXOs) starts decay on the old ones, and the new ones generate only if the registration is still in force.
- Midnight reacts after the Cardano **stability delay** (about 12 hours on Mainnet) — the ledger receives batched system transactions from the partner chain's cNIGHT observation pallet (`pallet_cnight_observation`), which watches UTXO activity and emits Create/Destroy events. The ledger validates that Cardano-instructed generation does not exceed the cNIGHT circulation, "but otherwise deploys it as instructed."
- On Mainnet the official entry point is the **cNgD DApp** at `https://midnight-dust-mainnet.nethermind.io/` (reach-checked HTTP 200 this session; not operated — it requires a Cardano wallet and cNIGHT, out of scope here, flagged for Kyle if a live run is wanted). The docs' own Mainnet walkthrough cites 2.5 ADA total (1.58608 ADA locked + fee) and notes it is a **Cardano-only transaction** — no Midnight transaction is produced.
- Verification is the indexer `dustGenerationStatus` query (up to 10 stake addresses per call); wallet-side, system transactions are applied by `apply_system_transaction` (WalletEngine spec), which is how cNight-based generations are discovered.
- Post-ledger-9, cNIGHT-backed registrations were the only ones that came back automatically (section 9).

## 8. Redirecting or stopping DUST generation

From manage-dust, "Redirecting or stopping DUST generation":

- **Redirect (re-designate):** call `registerNightUtxosForDustGeneration` again with the optional `dustReceiver` argument set to the new wallet's DUST address (an `mn_dust…` Bech32m). Pass all registered UTXOs to redirect all of them. The recipient gets **new DUTXOs starting from zero**; the sender's old DUTXOs decay toward zero but remain spendable in the meantime.
- **Stop (deregister):** the `DustRegistration` with `dust_address = None` removes the table entry; the NIGHT UTXOs are recreated flagged `registeredForDustGeneration = false`, their DUTXOs decay, and NIGHT arriving later at the address does not generate.
- **Both are changes to already-registered NIGHT, so both pay their fee with a DUST spend** — needing a DUST balance and the proof server (a first registration did not). The node rejects a re-registration that lacks the DUST spend with **`BalanceCheckOverspend`** (the docs cite error 1010, custom 138).

## 9. Ledger 9 and the one-time DUST reset

Verified live 2026-11-24 against the ledger-9 relnotes page and manage-dust ("DUST after the ledger 9 upgrade"):

- Network timeline (relnotes): **Mainnet** moved ledger 8 → 9 on **6 November 2026**; **Preview** relaunched as a **new chain** on **7 November 2026**; **Preprod** upgraded in place on **9 November 2026** (block 3,348,434).
- **The reset is one-time.** "At the upgrade, the network resets DUST once… It clears the DUST state, and NIGHT balances aren't affected."
- **cNIGHT-backed registrations come back on their own (Mainnet).** "Every Mainnet DUST registration is backed by cNIGHT, and after the upgrade the network replays every live registration automatically, straight to its full DUST cap, at the same DUST address." Each replayed registration goes straight to its full cap of 5 DUST per NIGHT even when the cNIGHT UTXO is less than a week old. The replay takes **about 48 blocks (under five minutes)**; transactions submitted during it wait in the pool and are included when it ends (the node doesn't reject or cancel them); it ends with a **`DustReapplyCompleted`** event.
- **NIGHT held on Midnight** (not cNIGHT) needs its own first-time registration after the upgrade: "NIGHT that arrives on Midnight after the upgrade needs a first-time registration for DUST generation, with `registerNightUtxosForDustGeneration`." On Preprod, the in-place upgrade cleared every non-cNIGHT registration, so wallets re-registered; a re-registering holder "loses nothing: DUST generation is credited from when the NIGHT UTXO was created, including the time across the upgrade, up to the cap."
- **Client requirements after the upgrade:** Wallet SDK **2.0.0+** (1.x wallets can't transact on Mainnet), indexer **4.4.101** with finished sync (earlier indexers report DUST capacity incorrectly), and **drop any cached DUST tree indices** at the crossing — "the indexer doesn't send a dedicated reset event." The full mainnet version set per the relnotes matrix: node 2.1.101, Wallet SDK 2.0.1, Midnight.js 5.0.1, Compact toolchain 0.36.1 (language 0.28.0), proof server 9.0.2, indexer 4.4.101.
- The cNIGHT→mNIGHT protocol bridge ships with ledger 9 but **stays off until governance enables it** — don't plan on bridging at the upgrade.
- ⚠️ **Docs inconsistency (flagged for upstream):** within the same manage-dust page, one line says Preview "relaunched as a new ledger 9 chain on **7 November** 2026" and another says "Preview was reset on **6 November** 2026 as a new chain". The relnotes page consistently says **7 November** for the relaunch (6 November is the Mainnet upgrade date). 7 November is the corroborated date.

## 10. Keys and addresses

From the WalletEngine Specification (midnightntwrk/midnight-architecture, fetched live 2026-11-24) and the acquire-tokens guide:

- HD path: `m / 44' / 2400' / account' / role / index`. Role table per the spec: **0** = Unshielded (Night) external, **1** = Unshielded internal (change), **2** = **Dust** ("Dust is needed to pay fees on Midnight"), **3** = Shielded (Zswap), **4** = Metadata. (Contrast the stale role values in the ledger-8-era wallet-developer-guide page — see 7.2.)
- DUST key: SNARK-friendly — the secret key is a scalar in the BLS12-381 scalar field, derived by scalar sampling (margin 32, domain separator `midnight:dsk`) from the key at the role-2 path; the public key is its Poseidon hash.
- **DUST address:** Bech32m, credential type `dust`, payload = SCALE compact-bigint of the DUST public key (variable length, up to 33 bytes). Human-readable part: `mn_dust` (mainnet, no network suffix), `mn_dust_test` / `mn_dust_preprod` / `mn_dust_dev` etc. per network. Troubleshooting: "DUST addresses start with `mn_dust_` plus the network name; shielded (`mn_shield-addr_...`) and unshielded (`mn_addr_...`) addresses are different types."
- A DUST spend (fee payment) needs: the DUST secret key, the DUTXO, both Merkle trees (commitments + generation info) with proofs, the current timestamp, and the declared fee amount (WalletEngine spec, "Creating a Dust spend"). Balancing always leaves DUST last, covering exactly the fees.

## 11. Local devnet note (testnet class)

From HERMES-3 (`research/local-stack-verification.md`, merged PR #24, 2026-11-24): in the `midnight-examples` local environment (Compact compiler **0.36.1**, language **0.28** per `toolchain.json`; node image `midnightntwrk/midnight-node:2.1.101`), there is **no local faucet** — the pre-funded genesis wallet plus DUST accrual is the only funding path. The `wait:dust` helper (waits for ≥1 spendable DUST coin, ≥60 DUST at the latest block, 180 s timeout) printed `DUST ready` in **~90 s** of real time once the node's block production carried a registered UTXO's accrual past the threshold. Labelled local devnet (`networkId` `undeployed`); no real funds involved.

## 12. Model vs live data (labelled)

- **MODEL — our DUST Planner (`apps/dust-planner`, GROK-2, merged PR #14):** a local estimation tool; its outputs are labelled modelled estimates in the app and on the hub. Cross-check performed this run: its constants match the official values exactly — `NIGHT_DUST_RATIO = 5_000_000_000n`, `GENERATION_DECAY_RATE = 8267n`, `TIME_TO_CAP_SECONDS = 604815n`, `DUST_GRACE_PERIOD_SECONDS = 10800n` (`apps/dust-planner/dust-model.js`, each with a source comment pointing at the dust-architecture/manage-dust pages).
- The GROK-2 follow-up (grace-period handling, merged PR #35, Muse spot-verified 2026-11-24) concluded the official docs define the grace period as a **transaction-timestamp validity window** (the ledger rejects declared times older than the window or later than block time), not a decay delay — matching the manage-dust parameter table quoted in section 4.
- **LIVE (read from official pages, not measured here):** the Mainnet parameter values in section 4 (docs run of 2026-09-30), the ledger-9 reset facts, the SDK/toolkit version pins.
- **NOT live-verified here:** anything requiring a transaction, a wallet, or funds — see section 13.

## 13. What was NOT verified in this run (honesty list)

- No transactions were executed; no wallet signing; no mainnet interaction. This is a research document.
- The cNgD DApp (`midnight-dust-mainnet.nethermind.io`) was **reach-checked only** (HTTP 200) — not operated, since that needs a Cardano wallet plus cNIGHT (a flagged-for-Kyle step).
- The servicedesk **duplicate-registration runbook** (linked from the verified manage-dust page) returned **503 twice** (transient GitHub errors) when fetched directly — existence confirmed by the verified page's link, content not read.
- The **DUST sponsorship** page (also linked from manage-dust) returned **429** (rate-limited) — not read.
- Mainnet parameter values are the docs' own sampled run (2026-09-30); parameters are governance-changeable ledger state, so production code must read them at runtime (section 4).
- The docs' worked examples (faucet confirmation, DUST balance prints) are quoted from the pages, not re-run.
- The `dustGenerations` indexer subscription is marked BETA in the official API reference and was not exercised.

## 14. For Muse

1. The **wallet-developer-guide page is ledger-8-era** (ledger-v8 install, DUST role = 4, hyphenated package scope) while the current funding guide uses `@midnightntwrk/wallet-sdk` 1.2.0 + `midnight-js-*` 4.1.1 and the WalletEngine spec assigns DUST role 2. Should this be added to the HERMES-1 stale-page inventory in `research/stack-versions.md`, or is it part of the upcoming upstream docs issue?
2. The **Preview reset-date discrepancy** on manage-dust (6 Nov vs 7 Nov, same page) — include in the upstream docs issue or file separately?
3. The promised **upstream docs issue** (0.27-pragma finding from HERMES-4 + status of HERMES-3 friction items 1–3, now largely resolved by the quickstart rewrite) is queued immediately after this PR lands, per your answer #5 on PR #29. The wallet-developer-guide staleness (item 1) can join it if you prefer one batch.

## 15. Sources

All fetched live 2026-11-24 (this session) unless noted:

| Source | Used for |
| --- | --- |
| https://docs.midnight.network/concepts/dust-architecture | DUST model, registration/fee mechanics, cNIGHT implementation, non-persistence, grace period (full page) |
| https://docs.midnight.network/guides/manage-dust | How DUST behaves, parameter tables + Mainnet sample values, Registration rules, fee payment, redirect/stop, cNIGHT on Cardano, Mainnet cNIGHT registration, ledger-9 DUST section, troubleshooting (full page) |
| https://docs.midnight.network/guides/acquire-tokens | Faucet (5,000 tNIGHT/request), Lace Generate tDUST procedure, current SDK set + registration procedure + error strings (full page) |
| https://docs.midnight.network/sdks/official/wallet-developer-guide | Package table, derivation roles (as stale ledger-8-era reference), address table |
| https://docs.midnight.network/nodes/node-toolkit | Toolkit 1.0.300, `register-dust-address`, replay timing, `Insufficient DUST` error |
| https://docs.midnight.network/tokens/overview | NIGHT/DUST units (STAR/SPECK), "resource not a token" |
| https://docs.midnight.network/relnotes/ledger-9 | Upgrade dates, DUST reset + replay, version matrix, bridge-off, client migration |
| https://raw.githubusercontent.com/midnightntwrk/midnight-ledger/main/spec/dust.md | `DustParameters` initial values, `apply_registration` fee formula, nonce/registration semantics, cNight interaction (full spec) |
| https://github.com/midnightntwrk/midnight-architecture/blob/main/components/WalletEngine/Specification.md | HD role table (Dust = 2), DUST key derivation, DUST address format, DUST spend construction, `apply_system_transaction` (raw fetched) |
| https://midnight-dust-mainnet.nethermind.io/ | cNgD DApp reach-check (HTTP 200, not operated) |
| https://github.com/midnightntwrk/servicedesk/blob/main/runbooks/cnight-dust-after-registration-runbook/cnight-dust-after-registration-runbook.md | Reach-check (HTTP 200) |
| https://github.com/midnightntwrk/servicedesk/blob/main/runbooks/cnight-dust-duplicate-registration-runbook/cnight-dust-duplicate-registration-runbook.md | Linked from manage-dust; direct fetch 503 twice (not read) |
| Local: `.ref/midnight-docs` @ `726028f` (fetched 2026-11-24 01:34 UTC) | API-reference cross-check (DustParameters class) |
| Local: `apps/dust-planner/dust-model.js` | Model constant cross-check (section 12) |
| Local: `research/local-stack-verification.md` (merged PR #24) | Local devnet `wait:dust` ~90 s figure |

---

Attribution on ALL work: both donation addresses, character-for-character, plus @kshot9000 —
ADA: addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
