### D7 X25519-encrypted payload + low-order-point rejection

Category: D · Status: demonstrated

One-line invariant: a client can seal a payload to published admin keys (the contract never sees the plaintext), and `addKey` refuses the 14 X25519 low-order public keys so a payload can never be sealed to a key nobody can open.

Compact features: `Opaque<"Uint8Array">` ledger fields (the encrypted blob), a `pure` circuit computing the low-order check over `Bytes<32>` (language constructs; [opaque-data](https://docs.midnight.network/compact/data-types/opaque_data.md), [standard-library](https://docs.midnight.network/compact/standard-library.md)).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 12 and 16 (`adminEncKeys`, `encBallots: Map<Bytes<32>, Opaque<"Uint8Array">>`), 57–78 (`isLowOrderKey` + the 14-point table, **verbatim below**), 96–104 (`addAdminKey`), 119–123 (the sealing comment, quoted verbatim below).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// D7 pattern core: X25519-encrypted payload + low-order-point rejection.
// Trimmed from midnight-examples voting @ 953f864 (voting.compact:12,
// 57-78, 96-104): the client seals the payload to published admin keys;
// isLowOrderKey rejects the 14 X25519 low-order points (0, 1, p-1, the
// two order-8 points, and p, p+1 — each also with the ignored top bit
// set) so a payload can never be sealed to a key nobody can open.
export ledger adminEncKeys: Set<Bytes<32>>;
export ledger sealedPayloads: Map<Bytes<32>, Opaque<"Uint8Array">>;

// The X25519 public keys nobody can encrypt to: the low-order points (0, 1,
// p - 1, the two order-8 points, and p, p + 1), each also with the ignored top
// bit set. A payload can't be sealed to one, so addKey refuses them.
export pure circuit isLowOrderKey(k: Bytes<32>): Boolean {
  const lowOrder = [
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
    [1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128],
    [224, 235, 122, 124, 59, 65, 184, 174, 22, 86, 227, 250, 241, 159, 196, 106, 218, 9, 141, 235, 156, 50, 177, 253, 134, 98, 5, 22, 95, 73, 184, 0],
    [224, 235, 122, 124, 59, 65, 184, 174, 22, 86, 227, 250, 241, 159, 196, 106, 218, 9, 141, 235, 156, 50, 177, 253, 134, 98, 5, 22, 95, 73, 184, 128],
    [95, 156, 149, 188, 163, 80, 140, 36, 177, 208, 177, 85, 156, 131, 239, 91, 4, 68, 92, 196, 88, 28, 142, 134, 216, 34, 78, 221, 208, 159, 17, 87],
    [95, 156, 149, 188, 163, 80, 140, 36, 177, 208, 177, 85, 156, 131, 239, 91, 4, 68, 92, 196, 88, 28, 142, 134, 216, 34, 78, 221, 208, 159, 17, 215],
    [236, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 127],
    [236, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255],
    [237, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 127],
    [237, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255],
    [238, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 127],
    [238, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255]
  ] as Vector<14, Vector<32, Uint<8>>>;
  return fold((found, lo: Vector<32, Uint<8>>): Boolean => found || k == (lo as Bytes<32>), false, lowOrder);
}

export circuit addKey(key: Bytes<32>): [] {
  const k = disclose(key);
  assert(!isLowOrderKey(k), "Not a usable admin key");
  assert(!adminEncKeys.member(k), "Admin key already added");
  assert(adminEncKeys.size() < 8, "Too many admin keys");
  adminEncKeys.insert(k);
}
```

Privacy contract: private: the plaintext payload (only the holder of the admin's secret key can open it) · proven publicly: — (the ciphertext is opaque data; the contract stores and routes it, it cannot read it) · on the public ledger: the key roster, the ciphertexts, and any routing metadata.

Documented leaks/traps: from the canonical source (voting.compact:119–123 comment), verbatim: "`recipients` are the admin keys the voter's client sealed `enc` to, padded with zeros. Each must be a published admin key: the client reads the keys from an indexer, and a substituted key would fail here against the real ledger state. The proof can't see inside `enc`; an honest client seals it to exactly these keys." The pattern core here drops the admin-gate from `addKey` for minimalism — the canonical version gates it on `adminKey(secretKey()) == adminPk` (`requireAdmin`, voting.compact:80–82).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:51Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
