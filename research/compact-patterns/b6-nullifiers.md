### B6 Nullifiers (one-time actions, replay-rejected)

Category: B · Status: demonstrated

One-line invariant: each secret identity can perform an action exactly once per contract instance — the nullifier is a public hash the circuit checks against a `Set`, and it is bound to the instance so the same identity in two deployments can't be linked or replayed across them.

Compact features: `persistentHash<Vector<N, Bytes<32>>>` + `Set<Bytes<32>>` from `CompactStandardLibrary`; `kernel.self().bytes` for instance binding — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 41–47 (`ballotNullifier` + the binding comment, quoted verbatim below), 132–134 (check + insert in `cast`). Language-repo reference: [zerocash.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/zerocash.compact) (nullifier set in an older ZK example — reference-only).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// B6 pattern core: nullifier — one-time action, replay-rejected.
// The binding follows the canonical scheme (voting.compact:41-47 @
// 953f864) minus the proposal id: the nullifier is hashed from a
// per-DApp domain, the secret key, and the contract instance, so the
// same identity in two deployments can't be linked or replayed
// across them.
export ledger used: Set<Bytes<32>>;

witness secretKey(): Bytes<32>;

export pure circuit actionNullifier(sk: Bytes<32>, instance: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<3, Bytes<32>>>([pad(32, "mip-vote:cast"), sk, instance]);
}

export circuit act(): [] {
  const n = disclose(actionNullifier(secretKey(), kernel.self().bytes));
  assert(!used.member(n), "Already used");
  used.insert(n);
}
```

Privacy contract: private: the secret key (the nullifier hides it) · proven publicly: that *some* identity not in the set is acting (the nullifier itself is public) · on the public ledger: the growing `Set` of used nullifiers.

Documented leaks/traps: from the canonical source (voting.compact:41–44 comment), verbatim: "The nullifier and salt are bound to the proposal and to this contract instance, so the same voter's ballots in two deployments (a rehearsal and the real vote, or a clone that reuses the label) can't be linked or opened with each other's salts." Consequence: the nullifier set is public — anyone can count how many actions happened, and two actions by the same identity in the *same* instance are linked to each other (that is the point). The canonical version also binds the *proposal* id — the pattern core here omits it; include it when one identity may act in several proposals. Use B3's domain separation if the identity must also stay unlinked to other DApps.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T05:26:32Z (offline, `--skip-zk`): exit 0 (final comment version; earlier gate 2026-11-28T04:46:50Z, exit 0, same code modulo comments).

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
