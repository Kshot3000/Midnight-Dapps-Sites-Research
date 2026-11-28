### GAP-3 Multi-round outbidding privacy (invented)

Category: GAP (invented, outline gap list item 3) · Status: invented — no official example covers this pattern

One-line invariant: in an ascending-bid round, the current high bid exists only as a commitment; a new bidder proves "my bid beats the high bid" *inside their proof* by opening the previous commitment (whose salt is deterministic and therefore public), and no amount ever touches the ledger until the final reveal.

Compact features: `persistentCommit` (B4), `persistentHash` domain derivation (B3), `sealed ledger` minimum (A2), `Counter` round tracking (A5), in-proof assertions on witness values (E3).

Canonical example: **none** — invented gap card. The commit/reveal discipline and the deterministic-salt-safety note come from `midnightntwrk/midnight-examples` @ `953f864`, `examples/private-bid/contract/private-bid.compact:76–105` (and the salt trap note at 98–103).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// GAP-3 (invented, outline gap list item 3): multi-round / outbidding
// privacy. No official example covers it. Design: the current high bid
// lives only as a commitment; a new bidder proves "my bid beats the
// current high bid" inside the proof by opening the previous commitment.
// Each bidder's salt is derived deterministically from their key and is
// public — safe here because each bidder commits exactly once (the
// private-bid trap note, examples/private-bid @ 953f864, applies to the
// once-only case). No amount ever touches the ledger until finalReveal.
// Claimed boundary: each round discloses only that a higher bid exists,
// not its value; bidder keys are disclosed dapp pseudonyms (map keys).
// Compile-gated only (wave-1 bar); no runtime test.
export sealed ledger minimumBid: Uint<64>;
export ledger highBidCommit: Bytes<32>;
export ledger highBidder: Bytes<32>;
export ledger hasHigh: Boolean;
export ledger round: Counter;
export ledger revealedAmount: Uint<64>;

witness localSk(): Bytes<32>;
witness prevAmount(): Uint<64>;

circuit bidderKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "gap3:bidder:"), sk]);
}

circuit saltFor(key: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "gap3:salt:"), key]);
}

constructor(minimum: Uint<64>) {
  minimumBid = disclose(minimum);
  hasHigh = false;
}

export circuit newBid(amount: Uint<64>): [] {
  const key = bidderKey(localSk());
  if (hasHigh) {
    // Open the previous high bid inside this proof only: the claimed
    // prevAmount with the previous bidder's public salt must reproduce the
    // stored commitment, and the new amount must strictly exceed it.
    assert(persistentCommit<Uint<64>>(prevAmount(), saltFor(highBidder)) == highBidCommit,
        "Previous bid does not open");
    assert(amount > prevAmount(), "Bid does not beat the high bid");
  } else {
    assert(amount >= minimumBid, "Bid is below the minimum");
  }
  const myKey = disclose(key);
  assert(!hasHigh || myKey != highBidder, "The high bidder cannot re-bid");
  highBidder = myKey;
  highBidCommit = persistentCommit<Uint<64>>(amount, saltFor(myKey));
  round.increment(1);
}

export circuit finalReveal(amount: Uint<64>): [] {
  const key = disclose(bidderKey(localSk()));
  assert(hasHigh && highBidder == key, "Not the high bidder");
  assert(highBidCommit == persistentCommit<Uint<64>>(amount, saltFor(key)),
      "Amount does not match the committed high bid");
  revealedAmount = disclose(amount);
}
```

Privacy contract (claimed boundary — invented; verify before relying on it): private: every bid amount until `finalReveal` (and even then only the *final* high bid — earlier bidders' amounts are never disclosed, they only opened inside the next bidder's proof) · proven publicly: each round that a bid strictly above the previous high exists; at finalReveal that the disclosed amount reproduces the stored high-commitment · on the public ledger: the sealed minimum, `highBidder` (dapp pseudonym), `highBidCommit` (opaque), the round counter.

Documented leaks/traps (design notes): (a) the *previous* bidder's amount is a witness input to the *next* bidder's proof — operationally, the outgoing high bidder (or an auctioneer) must communicate it out-of-band to the outbidder's successor; the pattern core does not encode that channel. (b) Public salts are safe only under the once-only commit discipline; if a bidder may outbid themselves later, the salt must be fresh-random per commit (private-bid trap note). (c) `highBidder` is a disclosed pseudonym: outbidding links "key A was outbid by key B" — the *wallets* stay private, the auction graph does not. (d) No funds move in this core; settlement is GAP-2/E8 territory.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:29Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
