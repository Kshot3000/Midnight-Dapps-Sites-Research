### E9 Deadline gating: time windows with blockTimeLt/blockTimeGte

Category: E (composite) · Status: demonstrated

One-line invariant: phase transitions and participation gates are asserted against the block time (`blockTimeLt` for "before X", `blockTimeGte` for "at or after X"), and a *counting window* can extend past the announced deadline so late actions in the window are still valid — all of it checked in the proof, so a late or early call fails the circuit, not the trust.

Compact features: `blockTimeLt` (builtin), `blockTimeGte` (CompactStandardLibrary — the import is required), sealed deadline state set at deploy (A2/A8), phase state machine (A3).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact`:106–117 (`open`: range check `dl < 1000000000000`, `assert(blockTimeLt(dl), "Deadline is in the past")`, then `countingEnds = (dl + 172800) as Uint<64>` — counting ends 48h after the announced deadline), 127 (`cast` gated by `blockTimeLt(deadline)`), 157–162 (`count`: `assert(blockTimeGte(deadline), "Too early")` + `blockTimeLt(countingEnds)`). Stdlib window reference: `LFDT-Minokawa/compact` `examples/std_lib/block_time.compact` (language repo — reference-only, not CI-verified for ledger-9 apps).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// E9 pattern core: deadline gating via blockTimeLt (builtin) and
// blockTimeGte (CompactStandardLibrary — the import below is required).
// Trimmed from voting.compact:106-117 (open), 127 (cast gate),
// 157-162 (counting window: closes 48h = 172800s after the announced
// deadline) @ 953f864. Stdlib window reference:
// LFDT-Minokawa/compact examples/std_lib/block_time.compact (language
// repo — reference-only, not CI-verified for ledger-9 apps).
export ledger deadline: Uint<64>;
export ledger countingEnds: Uint<64>;
export ledger done: Boolean;

export circuit open(d: Uint<64>): [] {
  const dl = disclose(d);
  assert(dl < 1000000000000, "Deadline out of range");
  assert(blockTimeLt(dl), "Deadline is in the past");
  deadline = dl;
  countingEnds = (dl + 172800) as Uint<64>;
}

export circuit close(): [] {
  assert(blockTimeGte(deadline), "Deadline not reached");
  assert(blockTimeLt(countingEnds), "Counting window has ended");
  done = true;
}
```

Privacy contract: time-gating is public mechanics — the deadline and phase are on the public ledger. The privacy lives in *what* happens in the window (sealed ballots, E1); the timestamps themselves leak when actions open and close.

Documented leaks/traps: `blockTimeGte` is *not* builtin — without `import CompactStandardLibrary;` the compile fails with `unbound identifier blockTimeGte` (observed in this wave: first gate run of this core exited 255 until the import was added). Block time is chain-verified but coarse (block granularity) — "before the deadline" means before the deadline at the block your transaction lands in, and mempool timing is out of your control. Always range-check a deadline set from a public argument (voting's `dl < 1000000000000`) or a far-future deadline can wedge every time-gated circuit. If the counting window ends and `count` was never called, there is no state to recover — design the terminal phase so a missed window is a dead end you *want* (voting's `Done` is intentional).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:24:39Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
