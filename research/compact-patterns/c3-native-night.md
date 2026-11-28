### C3 Native NIGHT in/out of a contract

Category: C · Status: demonstrated

One-line invariant: native NIGHT has color `default<Bytes<32>>` (equivalently `nativeToken()`); the contract's public balance of it is what deposit and payout circuits check and move.

Compact features: `nativeToken()` from `CompactStandardLibrary`; `receiveUnshielded(nativeToken(), amount)` / `sendUnshielded(nativeToken(), amount, destination)`; amount boundary `Uint<128>` (widened with `as`).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/token-transfers/contract/token-transfers.compact` lines 36–46 (`receiveNightTokens`/`sendNightTokensToUser`, using `default<Bytes<32>>`); `examples/silent-auction/contract/silent-auction.compact` lines 46–55 (organizer deposit: `receiveUnshielded(nativeToken(), depositAmount as Uint<128>)`) and 122–153 (payouts).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// C3 pattern core: native NIGHT in/out of a contract.
// Trimmed from token-transfers.compact:36-46 @ 953f864 (default<Bytes<32>>
// is the NIGHT color) and silent-auction.compact:46-55 (the organizer
// deposit uses nativeToken() — same color, named form).
export circuit receiveNight(amount: Uint<128>): [] {
  receiveUnshielded(nativeToken(), disclose(amount));
}

export circuit sendNightToUser(amount: Uint<64>, user_addr: UserAddress): [] {
  sendUnshielded(
      nativeToken(),
      disclose(amount) as Uint<128>,
      right<ContractAddress, UserAddress>(disclose(user_addr))
  );
}
```

Privacy contract: private: nothing (NIGHT flows here are public) · proven publicly: amounts and destinations · on the public ledger: the in/out events. For private NIGHT flows (the shielded side), see C5/C6/C8 — that is where tip-jar's "tips stay private until withdrawal" model lives.

Documented leaks/traps: `default<Bytes<32>>` and `nativeToken()` are the same color — pick one convention per contract (token-transfers uses `default<Bytes<32>>`, silent-auction uses `nativeToken()`). Public NIGHT movement to/from the contract is always observable; the private variants (C5–C8) exist because of that, not because of the token.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:25Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
