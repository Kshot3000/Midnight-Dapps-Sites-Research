### A4 Round counter: fresh key space per round (anti-linking)

Category: A (foundational) · Status: demonstrated (canonical source is the docs example, not the CI-verified examples repo)

One-line invariant: fold a round nonce into the key-derivation domain hash so each round gets a fresh key space — a participant's key in round N cannot be linked to their key in round M, while the contract still knows who is who inside a round.

Compact features: `Field` as an unbounded nonce, `persistentHash` domain separation (B1), a ledger round counter advanced by a gated circuit.

Canonical example: `midnightntwrk/midnight-docs` (local clone @ the HERMES-6 audit), `docs/compact/reference/writing.mdx:51–68` — the `lock.compact` running example: `publicKey(round, sk)` at lines 65–68 folds `round as Bytes<32>` into `persistentHash([pad(32, "midnight:examples:lock:pk"), round, sk])`. Note: the docs example carries `pragma language_version 0.27;` (line 23) — the known writing.md drift (HERMES-4 finding); the pattern core below pins 0.28 per the verified matrix.

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// A4 pattern core: round-scoped key derivation — a fresh key space per
// round. Trimmed from the writing.md lock.compact example (midnight-docs,
// docs/compact/reference/writing.mdx:51-68 @ the HERMES-6 local clone):
// the round is folded into the domain hash, so a user's key in round N
// can't be linked to their key in round M. The docs example carries
// pragma 0.27 — the known writing.md drift (HERMES-4 finding); this core
// pins 0.28 per the verified matrix.
export ledger authority: Bytes<32>;
export ledger currentRound: Field;

circuit publicKey(round: Field, sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<3, Bytes<32>>>(
      [pad(32, "midnight:examples:lock:pk"), round as Bytes<32>, sk]);
}

constructor(pk: Bytes<32>) {
  authority = disclose(pk);
}

export circuit advance(sk: Bytes<32>): [] {
  assert(publicKey(currentRound, sk) == authority, "Not this round's authority");
  currentRound = currentRound + 1;
}
```

Privacy contract: private: the secret key and which round keys belong to the same person · public: the round counter, the authority key, and each round's derived keys (linkable *within* a round, unlinkable *across* rounds by construction).

Documented leaks/traps: anti-linking is only as strong as the domain separation — keep the `pad(32, "midnight:examples:lock:pk")` prefix (or a project-specific tag); derive different purposes from the same secret with *different* tags. If the round is ever disclosed together with a secret in the same transaction, the link re-appears. The docs example's 0.27 pragma will not compile under toolchain 0.36.1 (exit 255, verified in HERMES-4) — pin 0.28.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:10:47Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
