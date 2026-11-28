### E8 Deposit to disincentivize non-settlement

Category: E (composite) · Status: demonstrated

One-line invariant: the party who must settle (the organizer) stakes real money before the activity opens; the stake is refundable at settlement, so walking away or refusing to reveal costs the stake — the incentive is economic, enforced by the ledger, not by trust.

Compact features: `sealed ledger` for the stake amount + organizer identity (A2), `receiveUnshielded`/`sendUnshielded` on `nativeToken()` (C3), `getDappPubKey`-style organizer identification (B7), state machine (A3).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/silent-auction/contract/silent-auction.compact`: `depositAmount = 50` (line 38, "A 50 NIGHT deposit from the organizer disincentivizes organizers that never reveal"), deposit taken in `receiveTokens` (46–55), refund without winner in `revealWin` (121–127: "the highest bid was below the public price, the auction is cancelled and the deposit returned"), and refund together with the winner's bid in `claimWin` (147–153: "the deposit is sent back with the bid").

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// E8 pattern core: deposit to disincentivize an organizer that never
// settles. Trimmed from silent-auction.compact:16-19, 30-55, 120-127 @
// 953f864: the organizer stakes before bidding opens; the stake is sent
// back when the auction settles without a winning bid (silent-auction
// refunds it in revealWin and together with the winner's bid in claimWin).
export sealed ledger depositAmount: Uint<64>;
export sealed ledger organizer: Bytes<32>;
export ledger organizerAddress: UserAddress;
export enum State { LOCK, OPEN, SETTLED }
export ledger state: State;

witness localSk(): Bytes<32>;

circuit getDappPubKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "auction:pk:"), sk]);
}

constructor(pk: Bytes<32>, address: UserAddress) {
  organizer = disclose(pk);
  depositAmount = 50;
  organizerAddress = disclose(address);
  state = State.LOCK;
}

export circuit stake(): [] {
  assert(state == State.LOCK, "Staking is closed");
  const key = getDappPubKey(localSk());
  assert(organizer == disclose(key), "You are not the organizer");
  receiveUnshielded(nativeToken(), depositAmount as Uint<128>);
  state = State.OPEN;
}

export circuit refund(): [] {
  assert(state == State.OPEN, "Auction not open");
  sendUnshielded(nativeToken(), depositAmount as Uint<128>,
      right<ContractAddress, UserAddress>(organizerAddress));
  state = State.SETTLED;
}
```

Privacy contract: private: the organizer's secret key (witness only) · proven publicly: at stake, that the caller derives the sealed organizer key; at refund, that a stake exists and is being returned · on the public ledger: the deposit event, the refund event, and the sealed (opaque) `organizer`/`depositAmount` fields.

Documented leaks/traps: the stake is public money in motion — anyone can see the 50 NIGHT deposit and its refund, so the deposit also publicizes "an auction of this kind is running". The organizer address for the payout is public (needed for the send); the *dapp key* identity is the private side. If the organizer is unresponsive, the refund path is still open to the organizer's key holder — there is no third-party clawback in the canonical example; that is the economic trade-off, not an oversight to "fix" in the pattern core.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:28Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
