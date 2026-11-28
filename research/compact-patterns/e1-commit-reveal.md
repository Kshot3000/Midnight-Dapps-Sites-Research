### E1 Commit–reveal with reveal-time re-derivation

Category: E (composite) · Status: demonstrated

One-line invariant: a secret value is committed at deploy (sealed on the ledger) and later revealed by re-deriving the commitment from the claimed value plus the witness salt; equality is checked *in the proof* before the value is disclosed, so a mismatched reveal is rejected by the network, not by trust.

Compact features: `persistentCommit<T>(v, salt)` + `sealed ledger` + `witness` + `disclose` — the B4 commitment machinery composed with sealed fields (A2) and the state machine (A3).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/silent-auction/contract/silent-auction.compact`: reserve committed at deploy (line 36, `hiddenPrice = commitPrice(disclose(minPrice), localSalt())`), re-derived and checked at reveal (lines 105–128: `const salt = localSalt(); assert(commitPrice(minPrice, salt) == hiddenPrice, "Attempt to change min price detected, shame on you."); publicPrice = disclose(minPrice);`), `commitPrice` helper at 157–159.

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// E1 pattern core: commit-reveal with reveal-time re-derivation.
// Trimmed from silent-auction.compact:17, 30-44, 105-128, 157-159 @ 953f864:
// the organizer commits the reserve at deploy (sealed); reveal re-derives
// the commitment from the claimed value + witness salt and asserts equality
// before the value is disclosed ("Attempt to change min price detected").
export sealed ledger hiddenPrice: Bytes<32>;
export ledger publicPrice: Uint<16>;
export enum State { LOCKED, REVEALED }
export ledger state: State;

witness localSalt(): Bytes<32>;

circuit commitPrice(price: Uint<16>, salt: Bytes<32>): Bytes<32> {
  return persistentCommit<Uint<16>>(price, salt);
}

constructor(minPrice: Uint<16>) {
  hiddenPrice = commitPrice(disclose(minPrice), localSalt());
  state = State.LOCKED;
}

export circuit reveal(minPrice: Uint<16>): [] {
  assert(state == State.LOCKED, "Already revealed");
  const salt = localSalt();
  assert(commitPrice(minPrice, salt) == hiddenPrice, "Attempt to change min price detected, shame on you.");
  state = State.REVEALED;
  publicPrice = disclose(minPrice);
}
```

Privacy contract: private: the committed value from deploy until reveal · proven publicly: at reveal, that the disclosed value + salt reproduce the deploy-time commitment · on the public ledger: the sealed commitment field (opaque hash) and, post-reveal, the value.

Documented leaks/traps: the salt must be the *same* salt used at deploy — the witness is contract-level (implemented in the monorepo's `witnesses.ts`), and both the constructor and the reveal circuit call the same `localSalt()`. If you can't reproduce the exact deploy witness later, the reveal can never pass: silent-auction's reveal depends on the organizer's client holding the deploy-time salt. The committed value is sealed, but the *commitment hash* is public state — an observer knows a commitment exists and can watch for the reveal.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:26Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
