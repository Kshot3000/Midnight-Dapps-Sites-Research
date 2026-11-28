### A3 Explicit state machine

Category: A · Status: demonstrated

One-line invariant: the contract has exactly one public lifecycle enum, and every transition circuit asserts the phase it leaves — invalid transitions fail in-proof, not in a UI.

Compact features: `export enum` + ledger field of that enum + transition `assert`s (language constructs; see [compact-reference](https://docs.midnight.network/compact/reference/compact-reference.md)).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/voting/contract/voting.compact` line 4 (`enum Phase { ENROLMENT, OPEN }`) with phase asserts in `enrol`/`addAdminKey`/`open`/`closeEarly`/`count` (lines 84–171); silent-auction (4 phases), private-party (5 phases), battleship (3 phases). Language-repo reference: [election.compact](https://github.com/LFDT-Minokawa/compact/blob/main/examples/election.compact) (older style, not CI-verified — reference-only).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;

// A3 pattern core: one enum per lifecycle phase; every transition
// asserts the phase it leaves. (voting: 2 phases; silent-auction: 4;
// private-party: 5 — each with transition asserts.)
export enum Phase { IDLE, RUNNING, DONE }
export ledger phase: Phase;

constructor() {
  phase = Phase.IDLE;
}

export circuit start(): [] {
  assert(phase == Phase.IDLE, "Already started");
  phase = Phase.RUNNING;
}

export circuit finish(): [] {
  assert(phase == Phase.RUNNING, "Not running");
  phase = Phase.DONE;
}
```

Privacy contract: private: nothing specific (foundation pattern) · proven publicly: — · on the public ledger: the current phase (public by design — participants must know the window they are in).

Documented leaks/traps: the compiler does not check state-machine completeness — a missing transition assert is a real bug, not a compile error. The zero-argument constructor form used here compiles on 0.36.1 (verified this session); if a parameter is needed, pass it in and `disclose` it like the voting constructor does.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:46Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
