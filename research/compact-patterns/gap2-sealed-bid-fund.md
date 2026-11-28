### GAP-2 Sealed-bid auction: private bid + secret reserve + funded settlement (invented)

Category: GAP (invented, outline gap list item 2) · Status: invented — no official example covers this combination

One-line invariant: a single sealed bid and a secret reserve meet inside the proof at settlement: the bid stays private until reveal, the reserve was committed at deploy, and the settlement circuit moves NIGHT (winner pays the bid; organizer receives bid + stake) only after both values are on the ledger and the bid meets the reserve.

Compact features: composition of B4 (salted commitments), E1 (deploy-commit / reveal re-derivation), E3 (in-proof property), E8 (settlement stake), C2/C3 (public NIGHT settlement), A2/A3 (sealed fields + phases).

Canonical example: **none** — this is an invented gap card. Components are trimmed from `midnightntwrk/midnight-examples` @ `953f864`: private-bid's bid commit/reveal (`examples/private-bid/contract/private-bid.compact:76–124`) and silent-auction's reserve commit + stake (`examples/silent-auction/contract/silent-auction.compact:30–55, 105–153`).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// GAP-2 (invented, outline gap list item 2): sealed-bid auction combining
// a private bid, a secret reserve, and funded settlement. No official
// example covers this combination. Design: one sealed bid (private-bid's
// commit pattern, @ 953f864 examples/private-bid), the organizer's reserve
// committed at deploy (silent-auction's commitPrice pattern), a stake to
// disincentivize a never-settling organizer (E8), and settlement that moves
// NIGHT: the winner pays the bid, the organizer receives bid + stake.
// Claimed privacy boundary: the bid amount stays private until reveal;
// before then the chain shows only that a bid >= minimum exists (E3) and
// nothing about the reserve. Compile-gated only (wave-1 bar); no runtime
// test.
export sealed ledger organizer: Bytes<32>;
export sealed ledger reserveCommit: Bytes<32>;
export sealed ledger minimumBid: Uint<64>;
export sealed ledger stakeAmount: Uint<64>;
export enum Phase { STAKING, BIDDING, RESERVE_REVEALED, SETTLED }
export ledger phase: Phase;
export ledger bidCommit: Bytes<32>;
export ledger bidderKey: Bytes<32>;
export ledger revealedBid: Uint<64>;
export ledger publicReserve: Uint<64>;
export ledger organizerAddress: UserAddress;

witness localSk(): Bytes<32>;
witness localSalt(): Bytes<32>;

circuit commitPrice(value: Uint<64>, salt: Bytes<32>): Bytes<32> {
  return persistentCommit<Uint<64>>(value, salt);
}

circuit orgKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "gap2:org:"), sk]);
}

circuit bidderKeyOf(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "gap2:bidder:"), sk]);
}

circuit bidSalt(key: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<3, Bytes<32>>>([pad(32, "gap2:salt:"), kernel.self().bytes, key]);
}

constructor(minimum: Uint<64>, reserve: Uint<64>, stake: Uint<64>, address: UserAddress) {
  // The organizer's secret reserve is committed here and sealed; the value
  // itself never touches the ledger until revealReserve.
  minimumBid = disclose(minimum);
  reserveCommit = commitPrice(reserve, localSalt());
  stakeAmount = disclose(stake);
  organizerAddress = disclose(address);
  phase = Phase.STAKING;
}

export circuit stake(): [] {
  assert(phase == Phase.STAKING, "Staking is closed");
  assert(organizer == disclose(orgKey(localSk())), "You are not the organizer");
  receiveUnshielded(nativeToken(), stakeAmount as Uint<128>);
  phase = Phase.BIDDING;
}

export circuit placeBid(amount: Uint<64>): [] {
  assert(phase == Phase.BIDDING, "Bidding is closed");
  assert(amount >= minimumBid, "Bid is below the minimum");
  const key = disclose(bidderKeyOf(localSk()));
  assert(key != organizer, "The organizer cannot bid");
  bidderKey = key;
  bidCommit = persistentCommit<Uint<64>>(amount, bidSalt(key));
}

export circuit revealReserve(reserve: Uint<64>): [] {
  assert(phase == Phase.BIDDING, "Reserve already revealed");
  assert(organizer == disclose(orgKey(localSk())), "You are not the organizer");
  assert(commitPrice(reserve, localSalt()) == reserveCommit,
      "Reserve does not match the deploy commitment");
  publicReserve = disclose(reserve);
  phase = Phase.RESERVE_REVEALED;
}

export circuit revealBid(amount: Uint<64>): [] {
  assert(phase == Phase.RESERVE_REVEALED, "Reserve not revealed yet");
  assert(bidderKey == disclose(bidderKeyOf(localSk())), "Not the bidder");
  assert(bidCommit == persistentCommit<Uint<64>>(amount, bidSalt(bidderKey)),
      "Amount does not match the committed bid");
  revealedBid = disclose(amount);
}

export circuit settle(): [] {
  assert(phase == Phase.RESERVE_REVEALED, "Not ready to settle");
  assert(revealedBid >= publicReserve, "Bid does not meet the reserve");
  receiveUnshielded(nativeToken(), revealedBid as Uint<128>);
  const total = revealedBid + stakeAmount;
  sendUnshielded(nativeToken(), total as Uint<128>,
      right<ContractAddress, UserAddress>(organizerAddress));
  phase = Phase.SETTLED;
}
```

Privacy contract (claimed boundary — this card is invented; verify before relying on it): private: the bid amount (until `revealBid`), the reserve (until `revealReserve`), both secret keys (witnesses only) · proven publicly: at placeBid that some bid ≥ minimum exists (opaque key + opaque commitment); at reveals that each disclosed value reproduces its commitment; at settle that bid ≥ reserve and the funds moved · on the public ledger: the sealed fields (opaque), the phase, the commitment hashes, and post-reveal the amounts and the settlement payment.

Documented leaks/traps (design notes): the bid is *single* (one `bidderKey` slot) — a multi-bidder version is GAP-3's territory. The deterministic per-bidder salt is safe here because the bidder commits exactly once (private-bid's trap note applies). `settle` pays the organizer from the bid receipt — there is no escrow delay or dispute window; adding one is the difference from the escrow domain (gap 4). Settlement is public NIGHT: a private-funds version would need the C5–C13 shielded paths.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:22:18Z (offline, `--skip-zk`): exit 0 (second attempt; first failed on a `bidderKey` name collision between the ledger field and the helper circuit — renamed to `bidderKeyOf`).

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
