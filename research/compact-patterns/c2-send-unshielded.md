### C2 Unshielded send/receive (always-public token flows)

Category: C · Status: demonstrated

One-line invariant: `sendUnshielded`/`receiveUnshielded` move a domain-colored token in the clear — sender, color, and amount are public, and the receiving circuit must take the same color (re-derived from the same domain + contract) or it will fail against ledger state.

Compact features: `sendUnshielded(color, amount, destination)`, `receiveUnshielded(color, amount)` from `CompactStandardLibrary`; amounts are `Uint<128>` at the API boundary (widened from `Uint<64>` with `as`).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/token-transfers/contract/token-transfers.compact` lines 19–34 (`sendToUser`/`receiveTokens`); also silent-auction's public bid payouts (`sendUnshielded` at silent-auction.compact:122–126, 138, 141–153).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// C2 pattern core: unshielded send/receive — always-public flows.
// Trimmed from token-transfers.compact:19-34 @ 953f864. Amounts are
// Uint<128> at the token API boundary (widened from Uint<64> with `as`).
export circuit sendToUser(amount: Uint<64>, user_addr: UserAddress): [] {
  const domain = pad(32, "auction:fee");
  const color = tokenType(disclose(domain), kernel.self());
  sendUnshielded(
      color,
      disclose(amount) as Uint<128>,
      right<ContractAddress, UserAddress>(disclose(user_addr))
  );
}

export circuit receiveTokens(amount: Uint<128>): [] {
  const domain = pad(32, "auction:fee");
  const color = tokenType(domain, kernel.self());
  receiveUnshielded(color, disclose(amount));
}
```

Privacy contract: private: nothing (this is the public path) · proven publicly: color, amount, destination, and that both sides matched · on the public ledger: the transfer event. Auctions use this for public bid amounts and winner payouts (silent-auction); use C5/C8 instead when the *amount* must stay private.

Documented leaks/traps: unshielded balances are indexable — anyone can track every holder of a color over time, which is exactly why silent-auction documents its model as "bidders are aware of each other's bids and the current highest bid" (silent-auction.compact:3–4). For private-amount flows see C5 (receive shielded) and C8 (send shielded).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:24Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
