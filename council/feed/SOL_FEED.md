# Sol — feed (from Muse, created 2026-11-27)

Kyle's order 2026-11-27: this project is the primary build focus.
Sol's lane: websites, app designs, and code. SOL-AP-1 shipped
(PR #107, released as Android prover v0.8.0/v0.8.1) — thank you.
Your next task is SOL-2's remaining slices, below.

## SOL-2 — NightAttest role UI + live testnet proof run (NEXT)

State: the Compact contract slice is DONE (PR #40, merged
2026-11-24 — issuer registers/revokes an opaque commitment, holder
answers a verifier's fresh challenge, credential secret + Merkle
path stay private; contract + simulator, CI green under the pinned
toolchain 0.36.1 / language 0.28).

Remaining slices, in order:

1. **Three-role web UI** — issuer / holder / verifier flows over
   the merged contract, in `apps/nightattest/` (follow the hub's
   design language and the repo's site conventions). Each role gets
   its own view: issuer registers/revokes a commitment; holder
   presents against a verifier's fresh challenge; verifier checks
   the proof result. Honest capability copy throughout — what the
   circuit proves, what stays private, what lands on the public
   ledger — and testnet labelled testnet wherever a network is
   named (Preview).
2. **Live testnet proof run** — one real end-to-end presentation on
   Preview (labelled testnet): register → challenge → prove →
   verify, with the run recorded (commands, timings, tx ids) in
   `apps/nightattest/` docs or a research brief. Proof server
   9.0.2, Midnight.js 5.0.1, DApp Connector API 4.1.0. Wallet
   funding from the Preview faucet is testnet-only (5,000
   tNIGHT/request); NIGHT must be REGISTERED for DUST generation
   before fees generate — say so in the runbook where it applies.
   No mainnet funds, no signing with any real wallet, no API keys:
   if a step needs one of those, stop and flag it for Kyle.

Rules (every PR): claim the slice on `council/board.md` in the same
PR; branch `sol/sol-2-nightattest`; one slice per PR; run the repo
suite (`npm test`) and report observed counts only; both donation
addresses + @kshot9000 + the independent-builder line,
character-for-character, in every deliverable's footer (see the
repo root README for the exact block). Never push to main directly;
Muse reviews and merges under Kyle's standing authority.

Support and credits

Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v

Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

@kshot9000 · Independent builder — not affiliated with the Midnight Foundation or IOG.
