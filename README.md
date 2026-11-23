# Midnight Dapps, Sites & Research

Real dApps, websites, and research that use the [Midnight](https://midnight.network) blockchain to its fullest potential — built in the open.

Midnight is a privacy-first blockchain. The builds here are not "on Midnight" as a label: each one names and actually uses Midnight's primitives —

- **Selective disclosure** — prove one fact (over 21, member in good standing, eligible voter) and reveal nothing else
- **Private state with public verification** — data stays private, a zero-knowledge proof (Compact circuits) is verified on-chain
- **Shielded fees** — transaction fees are paid in DUST generated from registered NIGHT, so activity is not linked to the NIGHT wallet

## Status

Seed commit — structure and coordination only. The hub site (SOL-1) and the flagship dApp **NightAttest** (selective-disclosure credentials) are in progress. Networks are Preview / Preprod testnet only unless a build says otherwise, and testnet work is labelled as such.

## Layout

| Directory | What lives there |
| --- | --- |
| `sites/` | Website builds (Sol) — starting with the hub site in `sites/hub/` |
| `apps/` | dApp builds (Sol) — starting with NightAttest |
| `research/` | Verified research briefs, catalogues, and specs (Hermes) |
| `ideas/` | The ideas backlog (Muse) — see `ideas/backlog.md` |
| `council/` | Coordination: claim board, specs, submission log |

## The team

- **Sol** — builder: websites, app designs, and code
- **Hermes** — non-stop worker: research, catalogues, docs, and tests on a 15-minute idle cycle
- **Muse** — guide: ideas, specs, review, and verification
- **Kyle Cox** — owner

Work is coordinated on the claim board in `council/board.md` — one claimant per task, builds land as pull requests, and nothing here claims to be live until it has been tested and verified.

## Ideas backlog

NightAttest (flagship), Private Voting, Sealed-Bid Auction, Confidential Freelance Escrow, Eligibility-Without-KYC widget, Private Payroll Proof, "What Does This dApp See?" privacy auditor, Privacy Score Directory, Compact Playground + snippet library, DUST Planner Pro. Full list with the Midnight primitives each one uses: `ideas/backlog.md`.

## Support

Built independently — tips are appreciated and go to the builder, not to any foundation:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Follow along: [@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000)

Thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for building the chain this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
