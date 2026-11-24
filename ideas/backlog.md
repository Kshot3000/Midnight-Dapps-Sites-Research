# Ideas Backlog — Midnight Dapps, Sites & Research

Maintained by Muse (guide seat). Each idea names the Midnight primitives it actually uses. Status: idea / spec / building / live-testnet.

| # | Idea | Midnight primitives | Status |
| --- | --- | --- | --- |
| 1 | **NightAttest** — selective-disclosure credentials: issuer grants a private credential, only a commitment goes on-ledger; holder proves one fact (over-21, member in good standing); verifier learns only yes/no; revocation enforced in the circuit | Selective disclosure, private state + ZK proof, DUST-shielded fees | Spec — flagship, SOL-2 |
| 2 | **Private Voting** — private vote, proven eligibility, public auditable result | Selective disclosure, private state + ZK proof | Idea |
| 3 | **Sealed-Bid Auction** — bids private until close, winner proven without revealing losing bids | Private state + ZK proof, shielded fees | Idea |
| 4 | **Confidential Freelance Escrow** — funds proven locked, delivery proven, rates and history stay private | Private state + ZK proof, selective disclosure | Idea |
| 5 | **Eligibility-Without-KYC widget** — drop-in boolean proof (age / residency / licence) other sites can embed | Selective disclosure | Idea |
| 6 | **Private Payroll Proof** — prove employment, tenure, or salary above a threshold without revealing employer or exact pay | Selective disclosure, private state | Idea |
| 7 | **"What Does This dApp See?"** — privacy auditor: a dApp's permissions/contract in, a plain-English share / prove / keep-private breakdown out | Tooling (off-chain), Midnight data model | Idea |
| 8 | **Privacy Score Directory** — catalogue of Midnight dApps rated by what data they actually collect and prove | Tooling (off-chain) | Idea |
| 9 | **Compact Playground + Snippet Library** — tested Compact patterns for voting, credentials, auctions, escrow | Compact / ZK circuits | Idea |
| 10 | **DUST Planner Pro** — estimates DUST generation, decay, and fee capacity from registered NIGHT | DUST mechanics | Local tool · [Modelled estimates and setup](../apps/dust-planner/) |
| 11 | **ROSCA / Rotating Savings Circle** — members contribute privately against a public round counter; each round's receiver is drawn fairly and paid shielded, without revealing who contributed what. Open problems (HERMES-2 gap list #6/#7): no example covers a verifiable multi-party draw or a private contribution ledger — both must be invented | Private state + ZK proof, shielded fees, commit-reveal | Idea — added 2026-11-24 at Hermes' suggestion from the HERMES-2 catalogue |

Support — all work in this repo carries both addresses:
ADA: addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
@kshot9000 — independent builder, not affiliated with the Midnight Foundation or IOG.
