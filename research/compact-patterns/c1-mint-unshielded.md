### C1 Domain-colored unshielded tokens (mint to self)

Category: C · Status: demonstrated

One-line invariant: an unshielded token's color is derived from a domain string bound to this contract, so the same domain mints a different token in different deployments — and the color is the only public identity of the token.

Compact features: `mintUnshieldedToken(domain, amount, destination)` and `tokenType(domain, contract)` from `CompactStandardLibrary` — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md). Destination is a `left<ContractAddress, UserAddress>` sum: `left(...)` = the contract itself, `right(...)` = a user.

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/silent-auction/contract/silent-auction.compact` lines 57–65 (NFT with color `"auction:nft"`, minted to the contract via `left<ContractAddress, UserAddress>(kernel.self())`); `examples/token-transfers/contract/token-transfers.compact` lines 6–16 (color `"simple:receive"`, returns the color from the circuit).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// C1 pattern core: domain-colored unshielded token, mint to self.
// Trimmed from midnight-examples @ 953f864: silent-auction.compact:57-65
// (NFT color "auction:nft") and token-transfers.compact:6-16
// ("simple:receive"). The color is derived from (domain, this contract),
// so the same domain string mints different tokens in different contracts.
export ledger nftType: Bytes<32>;

export circuit mintNft(): Bytes<32> {
  const domain = pad(32, "auction:nft");
  const color = mintUnshieldedToken(
      disclose(domain),
      1,
      left<ContractAddress, UserAddress>(kernel.self())
  );
  nftType = tokenType(disclose(domain), kernel.self());
  return color;
}
```

Privacy contract: private: nothing about the mint beyond the public facts · proven publicly: that the mint happened to the declared destination, for the declared amount · on the public ledger: the mint event with color, amount, destination. Unshielded tokens are public by construction — use the shielded paths (C5–C13) when balances must stay private.

Documented leaks/traps: the domain string is public input to the color derivation — two contracts that want *different* tokens must use different domain strings, and two that share a domain in different deployments get different colors only because the contract instance is part of the derivation. Circuits that mint (vs pure circuits) run at call time; `mintUnshieldedToken` is one of the stdlib's token primitives and is not composable with shielded-coin operations in one expression.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:17:23Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
