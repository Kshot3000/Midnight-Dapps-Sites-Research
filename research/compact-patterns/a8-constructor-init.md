### A8 Constructor initialization: the only sealed-write window

Category: A (foundational) · Status: demonstrated

One-line invariant: `constructor` runs once at deploy and is the *only* place `sealed ledger` fields may be written — from constructor arguments (disclosed to the network) or deploy-time witnesses (kept private); afterwards sealed state is frozen, so anything sealed must be decided at deploy time.

Compact features: `constructor(args)`, sealed fields written from witnesses (`witness secretKey(): Bytes<32>`), derived sealed state (hash of a witness-derived key), plain public fields initialized alongside.

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact`:7–8 (sealed `adminPk`, `proposalId`), 25–27 (witness `secretKey()`), 29–33 (`constructor(pid: Bytes<32>)` writes `adminPk = disclose(adminKey(secretKey()))`, `proposalId = disclose(pid)`, `state = State.Enrolling`).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;
import CompactStandardLibrary;

// A8 pattern core: constructor-initialized state (sealed + public),
// trimmed from voting.compact:7-8, 25-33 @ 953f864: the constructor is
// the only place sealed fields are written — from constructor arguments
// and deploy-time witnesses.
export sealed ledger adminPk: Bytes<32>;
export sealed ledger proposalId: Bytes<32>;
export ledger phase: Uint<8>;

witness secretKey(): Bytes<32>;

circuit adminKey(sk: Bytes<32>): Bytes<32> {
  return persistentHash<Vector<2, Bytes<32>>>([pad(32, "mip-vote:admin"), sk]);
}

constructor(pid: Bytes<32>) {
  adminPk = disclose(adminKey(secretKey()));
  proposalId = disclose(pid);
  phase = 0;
}
```

Privacy contract: private: the deploy secret key (witness) and any sealed value derived only from it · proven publicly: the constructor's proof (e.g. that `adminPk` is the correct hash of the claimed key) · on the public ledger: the constructor arguments and any non-sealed field you assign.

Documented leaks/traps: constructor *arguments* are public — `disclose(pid)` is an explicit public write; if the proposal id must stay private, pass it as a witness and seal the hash, or don't deploy with it at all. You cannot rotate sealed state later: sealed `adminPk` set in the constructor is the forever admin (rotation requires a public key + admin-gated public field, the E7 pattern). A deploy where the witness can't be reproduced by the deployer's client fails at deploy, not later — keep the deploy witness (e.g. the keypair) with the deployer, exactly as the E1 salt trap.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:10:48Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
