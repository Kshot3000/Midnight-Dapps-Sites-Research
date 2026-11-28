### B4 Salted commitments

Category: B · Status: demonstrated

One-line invariant: a participant commits to a secret value *without revealing it*; the salt makes the commitment non-guessable and the binding lets the opener be checked against the commitment at reveal time.

Compact features: `persistentCommit<T>(v, salt)` from `CompactStandardLibrary` — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md). (The compiler does *not* check salt entropy — that is the author's job; see the trap below.)

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 48–55 (salt + commit derivations), 140 (commit stored on cast), 167 (commit checked against the disclosed vote at count); also private-bid (bid + reserve price), battleship (ship placements), roulette (wager).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// B4 pattern core: salted commitments.
// Trimmed from midnight-examples voting @ 953f864 (voting.compact:48-55,
// 140): the salt is derived per identity (the canonical version binds it
// to proposal + contract instance as well), and the vote is committed
// with persistentCommit so the opening can be checked against it at
// count time. Guessable committed values need a salt.
export enum Vote { YES, NO, ABSTAIN }

witness secretKey(): Bytes<32>;

export pure circuit ballotSalt(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:salt"), sk]);
}

export pure circuit ballotCommit(v: Vote, salt: Bytes<32>): Bytes<32> {
  return persistentCommit<Vote>(v, salt);
}
```

Privacy contract: private: the committed value (vote, bid, placement, wager) until reveal · proven publicly: the commitment itself (public on the ledger from commit time) · on the public ledger: the commitment hash and, at reveal, the opened value + salt.

Documented leaks/traps: from the outline (HERMES-4), verbatim: "Deterministic salts are only safe for single-commit contracts; the compiler doesn't check salt entropy (private-bid source)." A deterministic salt (as here, derived from sk) is reusable *within one commitment per identity* — if the same identity commits twice with different values, derive a fresh random salt per commit or bind the salt to the action (as the canonical version binds it to proposal + instance).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:49Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
