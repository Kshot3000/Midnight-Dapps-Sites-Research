### B1 Private by default + explicit `disclose()`

Category: B · Status: demonstrated

One-line invariant: witness-derived values (and caller-supplied circuit arguments) never reach the ledger unless the author explicitly calls `disclose()` — an undeclared disclosure is a **compile error**, not a runtime surprise.

Compact features: witness functions (`witness x(): T;`), the `disclose()` enforcer (language construct — [explicit-disclosure](https://docs.midnight.network/compact/reference/explicit-disclosure.md), [smart-contract-security](https://docs.midnight.network/compact/smart-contract-security.md)).

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/hello-world/contract/hello-world.compact` (the minimal contract is one witness → one `disclose()` → one ledger write) and `examples/private-bid/contract/private-bid.compact` (source comment: "removing a single `disclose()` causes the compile to fail").

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;

// B1 pattern core: private-by-default + explicit disclose().
// Witness-derived values never reach the ledger unless passed
// through disclose(). Removing the disclose() below is a COMPILE
// error (the disclosure enforcer), not a runtime surprise.
export ledger stored: Bytes<32>;

witness secret(): Bytes<32>;

export circuit store(): [] {
  stored = disclose(secret());
}
```

Negative gate (same session, same toolchain) — the `disclose()` removed:

```
Exception: b1-disclose-NEGATIVE.compact line 11 char 10:
  potential witness-value disclosure must be declared but is not:
    witness value potentially disclosed:
      the return value of witness secret at line 8 char 1
    nature of the disclosure:
      ledger operation might disclose the witness value
    via this path through the program:
      the right-hand side of = at line 11 char 10
```

Privacy contract: private: the witness value itself (e.g. the secret key, the bid, the vote) · proven publicly: the disclosed projection the circuit chooses · on the public ledger: exactly what `disclose()` names — nothing else.

Documented leaks/traps: the enforcer is per-path: if a witness value can reach a ledger operation by *any* path, that write needs the `disclose`. It does not grade how much you reveal — revealing more than the pattern needs is a design bug the compiler will not catch. See also D8 for binding the message before you sign it.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T04:46:48Z (offline, `--skip-zk`): exit 0. Negative variant (disclose removed) on 2026-11-28T04:46:47Z: exit 255 with the error above, exactly as the private-bid source comment predicts.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
