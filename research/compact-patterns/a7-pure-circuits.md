### A7 Pure circuits: shared derivation, no state

Category: A (foundational) · Status: demonstrated

One-line invariant: `export pure circuit` functions take only their arguments and return a value — no ledger reads or writes — so the same derivation runs inside the proof *and* off-chain in the JS runtime, and both sides are guaranteed to agree.

Compact features: `export pure circuit`, `persistentHash`/`persistentCommit` on pure inputs, key-derivation helpers shared by contract and client (the voting `adminKey`/`voterKey`/`ballotCommit` trio).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact`:35–55 — `adminKey`, `voterKey`, `ballotCommit` (lines 45–48: `persistentCommit<Vote>(v, salt)`), each exported pure and reused by the circuits (e.g. `cast` at line 132 recomputes `ballotCommit(vote, salt)` against the sealed commitment).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// A7 pattern core: exported pure circuits — reusable derivation functions
// callable by the contract and by clients (JS runtime) with no ledger
// state. Trimmed verbatim from voting.compact:35-55 @ 953f864 (vote
// value narrowed to Uint<8> for standalone compilation).
export pure circuit adminKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:admin"), sk]);
}

export pure circuit voterKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:pk"), sk]);
}

export pure circuit ballotCommit(v: Uint<8>, salt: Bytes<32>): Bytes<32> {
  return persistentCommit<Uint<8>>(v, salt);
}
```

Privacy contract: pure circuits leak nothing by themselves — they are the same computation on-chain and off-chain. Privacy depends entirely on what the *calling* circuit discloses or keeps sealed.

Documented leaks/traps: a `pure` circuit that touches ledger state will not compile — if you need state, it is a plain `circuit` (which may be unexported and internal, like voting's non-pure helpers). Keep the domain tag (`"mip-vote:admin"` vs `"mip-vote:pk"`) distinct per purpose: reusing one tag for two roles lets a key be cross-checked across roles. When the JS runtime and the contract compute the same hash, the runtime uses the same stdlib functions — if you fork the derivation, the two sides silently diverge and gates fail at runtime, not compile time.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:10:48Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
