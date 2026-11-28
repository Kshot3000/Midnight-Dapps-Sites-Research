### C4 Unshielded balance guards

Category: C · Status: demonstrated

One-line invariant: before a contract sweeps its public balance out, the circuit proves the on-ledger balance of the color covers the expected amount — `unshieldedBalanceGte(color, expected)` — so a sweep can't out-run the money actually in.

Compact features: `unshieldedBalanceGte(color, amount)` (guard variant; the stdlib also exposes the balance primitive) from `CompactStandardLibrary` — [standard-library exports](https://docs.midnight.network/compact/standard-library/exports.md).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/private-party/contract/private-party.compact` lines 100–105: `assert(unshieldedBalanceGte(nativeToken(), totalCollected), "Contract balance wrong")` before `sendUnshielded` sweeps the collected entry fees to the organizer. (The outline's C4 row names token-transfers, which carries no guard in-repo — the guard function is stdlib, and private-party is its documented example use.)

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// C4 pattern core: unshielded balance guards inside circuits.
// Pattern from private-party.compact:100-102 @ 953f864
// (unshieldedBalanceGte over collected entry fees before sweeping).
// The outline's C4 row names token-transfers, which carries no guard —
// the guard itself is the stdlib function, used in private-party.
export ledger collected: Uint<64>;

export circuit addEntry(fee: Uint<64>): [] {
  const f = disclose(fee);
  receiveUnshielded(default<Bytes<32>>, f as Uint<128>);
  collected = (collected + f) as Uint<64>;
}

export circuit sweepToOrganizer(user_addr: UserAddress): [] {
  assert(unshieldedBalanceGte(nativeToken(), collected), "Contract balance wrong");
  sendUnshielded(
      nativeToken(),
      collected as Uint<128>,
      right<ContractAddress, UserAddress>(disclose(user_addr))
  );
}
```

Privacy contract: private: nothing (public balances) · proven publicly: that the balance covered the expected collected amount at sweep time · on the public ledger: the receive events and the sweep event.

Documented leaks/traps: the guard compares against *your ledger-tracked expectation* (`collected`), not against some off-chain number — keep the accounting in-circuit or the guard guards nothing. Arithmetic on fixed-width uints widens: `collected + f` is a general uint and must be cast back (`as Uint<64>`) before assignment — the compiler enforces this (observed: `expected right-hand side of = to have type Uint<64>`).

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T06:22:16Z (offline, `--skip-zk`): exit 0 (second attempt; first attempt failed on the uncast sum).

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
