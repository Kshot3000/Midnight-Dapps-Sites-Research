### E3 In-proof property of a private value

Category: E (composite) · Status: demonstrated

One-line invariant: a property of a value that never leaves the proof (e.g. `amount >= minimumBid`) is asserted inside the circuit; the network learns *that the property held*, not the value — private-bid's observer model: "an opaque bidder key, and an opaque commitment. Not the amount, not the bidder's wallet, not the secret key."

Compact features: `assert` on witness-derived values, `persistentCommit`, `Map`, domain-identity derivation (B3), `disclose` only for the values meant to be public (B1).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/private-bid/contract/private-bid.compact`: the step-by-step observer model at lines 21–27; `placeBid` at 76–105 with `assert(amount >= minimumBid, "Bid is below the minimum")` at line 82 — "The assertion runs inside the proof; the network learns only that it held (E3)." (line 81)

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// E3 pattern core: in-proof property of a private value.
// Trimmed from private-bid.compact:34-38, 47-69, 76-105 @ 953f864: the
// comparison runs inside the proof; the network learns only that it held —
// "the proof says so" (private-bid.compact:24).
export sealed ledger minimumBid: Uint<64>;
export ledger bidCommitments: Map<Bytes<32>, Bytes<32>>;

witness localSecretKey(): Bytes<32>;

constructor(minimum: Uint<64>) {
  minimumBid = disclose(minimum);
}

circuit derive(purpose: Bytes<32>, secretKey: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<3, Bytes<32>>>([purpose, kernel.self().bytes, secretKey]);
}

export circuit placeBid(amount: Uint<64>): [] {
  const secretKey = localSecretKey();
  assert(amount >= minimumBid, "Bid is below the minimum");
  const bidder = disclose(derive(pad(32, "private-bid:key:"), secretKey));
  assert(!bidCommitments.member(bidder), "This bidder already placed a bid");
  const salt = derive(pad(32, "private-bid:salt:"), secretKey);
  bidCommitments.insert(bidder, persistentCommit<Uint<64>>(amount, salt));
}
```

Privacy contract: private: the bid amount, the bidder's wallet address, and the secret key (witness, never transmitted) · proven publicly: that some bid ≥ minimum exists, tied to an opaque bidder key · on the public ledger: the `minimumBid` (deploy), the opaque `bidderKey → commitment` map entries, and at reveal (not in this core) the amount linked to the key.

Documented leaks/traps: (a) The in-proof property is only as good as the *disclosure surface* — everything you `disclose` (here: the derived bidder key) is public, so the "anonymous" identity is a dapp pseudonym, not a wallet address. (b) The minimum itself is public — the assertion leaks "bid ≥ minimum", which is the design. (c) A deterministic salt (derived from the secret key) is safe here only because each bidder commits once; a repeating committer needs a fresh random salt per commitment — "The compiler does NOT check that the second argument is secret or high-entropy; that is on the author." (private-bid.compact:98–103).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:27Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
