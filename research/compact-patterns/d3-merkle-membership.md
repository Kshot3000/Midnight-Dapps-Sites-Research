### D3 Merkle membership proofs

Category: D · Status: demonstrated

One-line invariant: a participant proves "I am on the list" without revealing who else is — a fixed-depth Merkle tree in ledger state, a `MerkleTreePath` witness, and a root check against the ledger root.

Compact features: `MerkleTree<N, T>`, `MerkleTreePath<N, T>`, `merkleTreePathRoot<N, T>` from `CompactStandardLibrary` — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 10–11 (`voters: MerkleTree<10, Bytes<32>>`), 26 (`witness voterPath()`), 84–93 (`enrol`), 129–131 (membership check inside `cast`).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// D3 pattern core: Merkle membership proof.
// Trimmed from midnight-examples voting @ 953f864 (voting.compact:10-11,
// 26, 84-93, 129-131): the admin enrols voter keys into a fixed-depth
// MerkleTree; a voter proves membership with a MerkleTreePath witness —
// the leaf is disclosed (that is the voter's public pseudonym), the tree
// root check happens against ledger state, and the path itself never
// reveals who else is on the list.
export ledger voters: MerkleTree<10, Bytes<32>>;
export ledger voterKeys: Set<Bytes<32>>;
export ledger enrolled: Counter;

witness voterPath(): MerkleTreePath<10, Bytes<32>>;

export circuit enrol(pk: Bytes<32>): [] {
  const k = disclose(pk);
  assert(!voterKeys.member(k), "Voter already enrolled");
  assert(enrolled.read() < 1024, "Voter list is full");
  voterKeys.insert(k);
  voters.insert(k);
  enrolled.increment(1);
}

export circuit proveMembership(pk: Bytes<32>): [] {
  const path = voterPath();
  assert(disclose(path.leaf == pk), "Path is not for this voter");
  assert(voters.checkRoot(disclose(merkleTreePathRoot<10, Bytes<32>>(path))), "Not an enrolled voter");
}
```

Privacy contract: private: the membership *witness* (the path) and everyone else's leaves · proven publicly: that this leaf is in the tree (root check against ledger state) · on the public ledger: the root, the enrolled keys (as a public `Set` in the canonical example), and the counter.

Documented leaks/traps: (1) The tree is **fixed depth**: `MerkleTree<10>` holds at most 2^10 = 1024 leaves — size the depth for the real cohort (A9's bounded-execution theme). (2) In the canonical voting example the allowlist is *also* kept in a public `Set` (`voterKeys`) — roster membership is observable by design (an allowlist of pseudonyms). If leaves must be private to a verifier who holds the list, the tree is the whole mechanism and the `Set` goes away. (3) The leaf that is disclosed is the participant's public pseudonym — pair with B3 for cross-DApp unlinkability.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:51Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
