### A1 Basic fields: public and sealed ledger state

Category: A (foundational) · Status: demonstrated

One-line invariant: every piece of contract state is declared up front with `ledger` (public) or `sealed ledger` (public hash, hidden value); circuits read state directly and write it with assignments — the declaration is what decides visibility, not how the data is used.

Compact features: `export ledger` vs `export sealed ledger`, fixed-width `Uint<16>` and `Opaque<"string">` types, `disclose` on state writes (A2), `circuit` entry points.

Canonical example: `midnightntwrk/midnight-examples` @ `953f864` (CI-verified), `examples/hello-world/contract/hello-world.compact`:1–7 (a single `Opaque<"string">` field plus a store circuit — the minimal contract shape) and `examples/calculator/contract/calculator.compact`:1–15 (a `Uint<16>` result field with arithmetic writes; note `as Uint<16>` truncation is explicit, line 6).

Code (pattern core — trimmed to compile standalone):

```compact
pragma language_version 0.28;

// A1 pattern core: basic public ledger fields + simple circuits.
// Trimmed from midnight-examples @ 953f864: hello-world.compact:1-7
// (Opaque<string> field + store circuit) and calculator.compact:1-15
// (fixed-width Uint<16> arithmetic result field).
export ledger message: Opaque<"string">;
export ledger result: Uint<16>;

export circuit storeMessage(newMessage: Opaque<"string">): [] {
  message = disclose(newMessage);
}

export circuit add(num1: Uint<16>, num2: Uint<16>): [] {
  result = disclose(num1 + num2 as Uint<16>);
}
```

Privacy contract: none — this is the public-state baseline. Both fields are readable by anyone; `sealed` (A2) is the upgrade when a value must stay hidden but hash-verifiable.

Documented leaks/traps: fixed-width types are truncated unless you cast explicitly — `num1 + num2 as Uint<16>` in calculator is the pattern; overflow wraps silently. `Opaque<"string">` is arbitrary-length (heap) storage, unlike `Bytes<32>` — use the sized type when the value fits, it is cheaper and hashable.

Verified: compiled with compactc 0.36.1 (7fa3a117c, language 0.28.0) on 2026-11-28T07:10:46Z (offline, `--skip-zk`): exit 0.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
