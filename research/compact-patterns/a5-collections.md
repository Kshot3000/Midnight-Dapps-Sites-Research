### A5 Collections: `Map` / `Set` / `Counter`

Category: A · Status: demonstrated

One-line invariant: structured ledger state (maps of entries, membership sets, auto-incrementing counters) is available as first-class ledger fields with in-circuit membership and lookup.

Compact features: `Map<K,V>`, `Set<T>`, `Counter` from `CompactStandardLibrary` (`.member/.insert/.lookup/.size`, `.read()/.increment()`) — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md). Fixed-depth `MerkleTree<N,T>` is the sibling for bounded membership (see D3).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 10–20 (`voters` MerkleTree, `voterKeys` Set, `ballots` Map, `enrolled`/`castCount` Counters); also battleship, leaderboard, token-transfers.

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// A5 pattern core: Map / Set / Counter as ledger fields.
export ledger scores: Map<Bytes<32>, Uint<64>>;
export ledger seen: Set<Bytes<32>>;
export ledger rounds: Counter;

export circuit setScore(k: Bytes<32>, v: Uint<64>): [] {
  const key = disclose(k);
  const val = disclose(v);
  assert(!seen.member(key) || scores.member(key), "Unknown key");
  seen.insert(key);
  scores.insert(key, val);
}

export circuit tickRound(): [] {
  rounds.increment(1);
}
```

Privacy contract: private: nothing specific (foundation pattern) · proven publicly: — · on the public ledger: the collections' contents (keys and values of any collection stored in public ledger state are public; pair with B1–B7 to keep the *meaning* of entries private).

Documented leaks/traps: (1) `Counter`/`Map`/`Set` are stdlib types — the `import CompactStandardLibrary;` line is required (verified this session: without it the compiler reports `unbound identifier Counter`, exit 255). (2) Circuit arguments are caller-supplied: writing one to ledger state requires `disclose()` first (verified this session — the enforcer rejects the undeclared write, exit 255). (3) Collections in public state are observable — a `Set` of identities is a public roster of pseudonyms (see D3 for the voting example where that is the intended design).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:49:40Z (offline, `--skip-zk`): exit 0 (second attempt after the import fix; first attempt 2026-11-28T04:46:46Z failed with the `unbound identifier Counter` error above).

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
