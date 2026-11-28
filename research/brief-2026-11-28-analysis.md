# Brief Analysis 2026-11-28 — Six Midnight dApp Concepts, Ranked and Architected

Source: project brief supplied by Kyle 2026-11-28 (six candidate concepts built on
Midnight's selective disclosure). Names approved by Kyle the same day. This analysis
ranks the six, architects the top two, maps the desktop/mobile experience, and flags
risks honestly — including where the brief's framing runs ahead of what Midnight
does today.

## The named family

| Brief concept | Product name | Backlog |
| --- | --- | --- |
| 1. Reusable Compliance & Access Passport | **NightPass** (protocol: NightAttest) | #1 |
| 2. Private Credit & Undercollateralized Lending | **Eclipse** | #12 |
| 3. Confidential Trading & Sealed-Bid Auctions | **Silent Gavel** | #3 |
| 4. Coercion-Resistant Governance | **VeilVote** | #2 |
| 5. Confidential B2B Settlement & Regulated RWAs | **MoonManifest** | #13 |
| 6. Consent-Gated Healthcare & AI Data Marketplaces | **NightWard** | #14 |

## 1. Prioritization & MVP selection

Ranked by (a) technical feasibility on Midnight as it exists today, (b) immediate
ecosystem utility, (c) clarity for everyday users.

**1 — Silent Gavel (sealed-bid auctions).** The most buildable concept in the set.
Commit-reveal is the canonical Compact pattern: a bid commitment is a hash, the
reveal is a proof of opening, and losing bids never exist on the public ledger at
all. It needs no external data, no regulated inputs, no oracle, and no counterparty
infrastructure — one contract, one deadline, one winner. Hermes' HERMES-6 pattern
wave is already carding this family (auction cards C1–C4 plus her own gap patterns:
sealed-bid fund, multi-round outbid), compile-gated on toolchain 0.36.1. The user
story is one sentence: "Your bid stays secret until the auction ends — everyone's
does." This is the fastest route to a second working dApp after NightAttest.

**2 — NightPass (compliance passport).** Already in flight as NightAttest: the
Compact contract is merged, the three-role UI is in build (SOL-2), and the live
Preview testnet proof run is next. Feasibility is high for the attribute-proof core
(over-18, jurisdiction, accreditation as boolean proofs over a committed
credential). It ranks below Silent Gavel only on time-to-first-demo, and above
everything else on platform value: NightPass credentials are the input layer for
Eclipse (a credit credential is a passport credential), VeilVote (eligibility),
and NightWard (consent + eligibility). Build the passport once; three later
products plug into it.

**3 — VeilVote (coercion-resistant governance).** Voting patterns sit in the same
HERMES-6 wave (eligibility private, choice private, tally public). Honest limit:
Midnight gives a private ballot and a public, verifiable tally; full
coercion-resistance (receipt-freeness against a coercer who demands your keys) is
a research-grade property no production chain fully delivers, and our copy must
never claim it. "Nobody can buy a vote they cannot verify" is the true claim.

**4 — MoonManifest (B2B settlement & RWAs).** The contract patterns are feasible
(private amounts, public settlement validity), but the product depends on asset
issuers and bridges that are not open yet: the official cNIGHT→mNIGHT bridge is
shipped but OFF pending governance, and VIA (USDM) is currently the only
cross-chain value route with public mainnet evidence — and it is unshielded.
Right-sized as a design-and-simulator track until the bridge opens.

**5 — Eclipse (private credit lending).** The highest-value concept and the
correct long-term flagship — undercollateralized lending is DeFi's biggest
unlocked market, and the default-reveal clause is something only a
selective-disclosure chain can express. It is not first because it needs things
that do not exist yet: a credit attester (someone must sign the creditworthiness
credential — that is NightPass with a credit issuer), liquidation mechanics over
private collateral, and an arbitration protocol for the reveal. Sequenced after
NightPass deliberately: Eclipse is NightPass's second act.

**6 — NightWard (healthcare & AI data).** Furthest out, for non-cryptographic
reasons: GDPR/HIPAA exposure, and the fact that Midnight proves facts about data
— the data itself still moves off-chain, so the marketplace is mostly an
off-chain product with on-chain consent proofs. Worth holding in the backlog;
not a near-term build.

**MVP call:** Silent Gavel is the next build after NightAttest's live run;
NightPass remains the platform flagship. Together they prove both halves of
Midnight's pitch — private inputs with public verification (Gavel) and reusable
selective disclosure (Pass).

## 2. Compact architecture — the top two

### NightPass (NightAttest protocol)

- **Private local state (holder's device only):** the credential secret, the
  attribute values (DOB, jurisdiction, accreditation status), and the Merkle path
  tying the holder's commitment to the issuer's credential set. Raw documents
  never leave the device and never touch the ledger.
- **Public ledger state:** the issuer registry (issuer keys + credential-schema
  ids), holder commitments (opaque hashes — no attributes), and the revocation
  set. An observer learns that credentials exist and get revoked; nothing about
  who holds what.
- **Circuits:** (a) *presentation* — prove a predicate over the committed
  attributes ("DOB implies over 18", "jurisdiction ∉ sanctioned set") against a
  verifier's fresh challenge, revealing only the boolean; (b) *non-revocation* —
  prove the commitment is absent from the revocation set at presentation time;
  (c) *issuance/revocation* — issuer-signed state transitions.
- **Selective disclosure, honestly scoped:** disclosure on Midnight today is
  **holder-mediated** — the holder generates a proof (or presents a scoped
  record) to a specific verifier. There is no passive viewing key that lets a
  regulator decrypt records unilaterally, Zcash-style; any design or copy that
  promises that is ahead of the platform. The lawful-inquiry path is therefore:
  the regulator presents the inquiry to the holder or issuer, and the holder
  discloses the specific record — which is arguably the correct civil-liberties
  shape, and we say so plainly.

### Silent Gavel (sealed-bid auction)

- **Private local state:** bid amount and blinding nonce, per bidder. Losing
  bids are never revealed to anyone — a stronger property than the brief's
  auditor viewing key, and the honest headline.
- **Public ledger state:** bid commitments (hashes), bid deposits, the deadline,
  and at close: the winning commitment, the clearing price, and settlement.
- **Circuits:** (a) *commit* — prove the commitment is a correct hash of a bid
  and that the deposit covers the bid's ceiling rules without revealing the bid;
  (b) *reveal/open* — prove an opening matches a recorded commitment;
  (c) *winner determination* — the highest valid opening wins; unopened bids
  forfeit per the auction rules, enforced in the contract.
- **Selective disclosure:** post-close, a bidder can prove facts about their own
  bid to an auditor (e.g., tax) by opening it privately to that auditor; the
  contract never learns losing amounts and neither does the market.

Both designs pay fees in DUST, which means the standing rule applies in every
runbook: **NIGHT must be REGISTERED for DUST generation or no fees generate.**
Testnet work is labelled testnet (Preview/Preprod); the hosted mainnet/Preprod
indexer is retired, so mainnet-facing builds plan on Blockfrost Midnight or a
self-hosted indexer (Preview stays Midnight-hosted).

## 3. Desktop & mobile UI/UX flow

**Desktop (both products):** browser dApp + wallet via the DApp Connector API
(our pin: 4.1.0), proof server 9.0.2 local (Docker) or LAN. NightPass flow:
issuer site issues → credential lands in the wallet's private store → any gated
site issues a challenge (QR or deep link) → holder sees exactly which fact is
being asked ("Prove you're over 18 — nothing else is shared") → one-click prove
→ boolean result. Silent Gavel flow: auction page shows deadline, deposit, and
the commitment count; bidding is a single form whose confirmation screen states
plainly that the amount is sealed until close.

**Mobile:** our shipped **Midnight Prover** Android app is the proving layer —
it talks to proof server 9.0.2 at localhost:6300 (on-device via a local
environment, or a LAN server; timings from any such setup are the server's and
are labelled that way — in-app proving is verified end-to-end on emulator
evidence, and no phone benchmarks are claimed). Credential storage lives in the
app's private state with the privacy cloak on by default for screenshots.
Challenge handoff is QR-first: desktop shows a challenge, phone scans, proves,
and returns the result — the "prove anywhere" half of NightPass's name.

**Design rule across both:** every screen names the disclosure before it happens
— what will be proven, what stays private, who learns what. That sentence is the
product.

## 4. Risks & ecosystem fit

- **Disclosure expectations vs platform reality** (biggest copy risk): the
  brief's "viewing keys for regulators/auditors" is holder-mediated disclosure
  today, not passive decryption. All public copy must use the true model.
- **Proving dependency:** every flow needs proof server 9.0.2 and its public
  parameters (srs.midnight.network, ~35 MB first fetch). Mobile proving
  performance on real phones is unmeasured; we claim emulator evidence only.
- **DUST economics:** unregistered NIGHT generates no DUST and every flow
  stalls; onboarding must walk users through registration explicitly.
- **Wallet maturity:** GROK-7's catalog verification found only Lace + 1AM with
  live evidence in the priority set; partner-dApp integration assumptions must
  be re-verified per wallet, not assumed.
- **Bridge dependency (MoonManifest/Eclipse):** cNIGHT→mNIGHT is off pending
  governance; VIA is live but unshielded. Designs must degrade honestly.
- **Coercion claims (VeilVote):** private ballot + public tally, yes;
  receipt-freeness, no. Copy is held to that line.
- **Regulatory surface (Eclipse/NightWard):** lending and health data carry
  licensing and privacy-law weight that is Kyle's call, not the team's; both
  stay simulator/design track until he rules.
- **Language evolution:** Compact is at language 0.28 / toolchain 0.36.1 and
  moving; every pattern card compile-gates against the pinned toolchain so
  drift surfaces as a failed gate, not a silent break.

Support — all work in this repo carries both addresses:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
@kshot9000 · Independent builder — not affiliated with the Midnight Foundation or IOG.
