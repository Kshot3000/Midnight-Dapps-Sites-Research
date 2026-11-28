### B3 Domain-separated identity hashing

Category: B · Status: demonstrated

One-line invariant: the same secret key produces *different* pseudonyms in different applications — identities are unlinkable across DApps because every derivation is hashed under a per-DApp domain string.

Compact features: `persistentHash<Vector<N, Bytes<32>>>` + `pad` from `CompactStandardLibrary` — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md), [crypto](https://docs.midnight.network/compact/standard-library/crypto.md).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 35–40 — the `adminKey`/`voterKey` derivations below are **verbatim** from that file.

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// B3 pattern core: domain-separated identity hashing.
// Verbatim derivation from midnight-examples voting @ 953f864
// (voting.compact:35-40) — the per-DApp domain strings make the
// same secret key produce different pseudonyms in different
// applications, so identities are unlinkable across DApps.
witness secretKey(): Bytes<32>;

export pure circuit adminKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:admin"), sk]);
}
export pure circuit voterKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:pk"), sk]);
}
```

Privacy contract: private: the secret key `sk` (never disclosed) · proven publicly: the derived pseudonym (a deterministic hash — anyone with the sk can reproduce it, anyone else sees only the 32-byte hash) · on the public ledger: pseudonyms only.

Documented leaks/traps: the domain string is a *convention, not a guarantee* — reusing the same domain string in two DApps links pseudonyms across them. Pick a unique, documented domain per DApp (the examples use `mip-vote:*`). The pseudonym is public and stable *within* the DApp — treat it as a handle, not a secret.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:48Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
