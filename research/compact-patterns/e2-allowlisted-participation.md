### E2 Allowlisted participation: enrolment circuit + participation gate

Category: E (composite) · Status: demonstrated

One-line invariant: a circuit enrolls identities into an allowlist (a `Set` or Merkle tree), and every participant circuit gates on membership of that list *plus* a per-action nullifier so each action happens once — participation is structural, not trust.

Compact features: `Set<Bytes<32>>` with `member`/`insert` (A5), the Merkle-tree roster variant (D3), nullifier sets as one-shot action guards (voting's `votedNullifiers`), a state machine phase (A3) gating enrolment vs participation.

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact`:84–93 (`enrol`: `assert(!voterKeys.member(k), "Voter already registered")` then `voterKeys.insert(k)`), 125–134 (`cast` gate: `const proof = merkleProof(voterRoot, k); assert(proof == null, "Proofs not yet revealed"); assert(rootExists(voterRoot, k, proof), "Voter not registered"); ... votedNullifiers.insert(disclose(ballotNullifier))`) — the canonical roster is a MerkleTree so the roster itself stays private (D3); the core below uses a public `Set` for the minimal pattern.

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// E2 pattern core: allowlisted participation — enrolment circuit +
// participation gate. Trimmed from voting.compact:84-93 (enrol), 125-134
// (cast gate) @ 953f864. The canonical roster is a MerkleTree so the
// roster itself stays private (see D3); this core uses a public Set for
// the minimal pattern.
export ledger roster: Set<Bytes<32>>;
export ledger usedNullifiers: Set<Bytes<32>>;

export circuit enrol(pk: Bytes<32>): [] {
  const k = disclose(pk);
  assert(!roster.member(k), "Voter already enrolled");
  roster.insert(k);
}

export circuit act(pk: Bytes<32>, nonce: Bytes<32>): [] {
  const k = disclose(pk);
  assert(roster.member(k), "Not an enrolled voter");
  const n = disclose(nonce);
  assert(!usedNullifiers.member(n), "Already voted");
  usedNullifiers.insert(n);
}
```

Privacy contract: with the public-Set core: the roster is public (who is allowed) — linkability is the trade for the minimal pattern · with the canonical MerkleTree roster (D3): the roster stays private; only membership proofs exist on-chain. The nullifier (a hash of action-scoped values) is public, but a well-chosen salt keeps actions unlinkable to each other.

Documented leaks/traps: a *public* roster is itself a leak — if the participant set is sensitive, use the MerkleTree variant or sealed commitments. Nullifiers must be scoped per action round: a global nullifier set means "once per identity per contract, forever" — for repeatable rounds, fold the round into the nullifier hash (A4). Enrolment and participation usually need a phase gate (`state == Enrolling`) — the core above omits it for minimalism; without it, enrolment and action are concurrent (voting splits them with its state machine, lines 85, 126).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:10:49Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
