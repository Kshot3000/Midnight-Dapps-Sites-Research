### A2 `sealed` constructor-frozen fields

Category: A · Status: demonstrated

One-line invariant: deploy-time parameters are written exactly once (by the constructor) and the ledger rejects any later write to them.

Compact features: the `sealed` keyword on `ledger` fields (language construct — [smart-contract-security](https://docs.midnight.network/compact/smart-contract-security.md)).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` lines 7–8 (`export sealed ledger adminPk: Bytes<32>;` / `proposalId`) — the same idiom in silent-auction, private-bid, tip-jar. Compiled locally (offline, `--skip-zk`): the full voting contract, exit 0, 2026-11-28T04:23Z (this session) and 2026-11-24 (`h6-compile/voting.log`, earlier HERMES-6 phase).

Code (pattern core — trimmed to compile standalone; canonical source at the file:lines above):

```compact
pragma language_version 0.28;

// A2 pattern core: parameters frozen at deploy.
// `sealed` fields can be written exactly once — by the constructor.
// Post-init writes to a sealed field are rejected.
export sealed ledger adminPk: Bytes<32>;
export ledger phase: Uint<64>;

constructor(pk: Bytes<32>) {
  adminPk = disclose(pk);
  phase = 1;
}

export circuit setPhase(v: Uint<64>): [] {
  assert(v < 100, "Phase out of range");
  phase = disclose(v);
}
```

Privacy contract: private: nothing specific (foundation pattern) · proven publicly: — · on the public ledger: the sealed values are public but immutable.

Documented leaks/traps: `sealed` is immutability, not privacy — a sealed field is still readable by anyone. Use it for parameters that must not change (admin key, proposal id, reserve policy), and combine with B3/B6 for the identity layer.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:45Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
