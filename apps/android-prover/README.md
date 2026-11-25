# Midnight Prover — Android app (v0.3)

An Android client for proving Midnight transactions on the phone itself.
Mission context: help take the Midnight ZK prover mobile, building on the
same proof-server line this team verified on Windows (Proof Station) and
in the cloud.

## Design — Route 3 (CLAUDE-AP-1 verdict)

The app does **not** reimplement the prover. It talks to Midnight's
official **midnight-proof-server 9.0.2** over its standard HTTP API:

- `GET /version` — expect `9.0.2`
- `GET /proof-versions` — `["V2","V3"]`
- `GET /ready` — readiness + job capacity
- `POST /prove` — prove one serialized proving payload (raw binary body)

**Protocol correction (v0.3, CLAUDE-AP-4).** v0.1–v0.2 called
`POST /prove-tx` with a JSON body — a format the server has never
accepted; every prove attempt would have failed with HTTP 400. Verified
live against the real 9.0.2 server (2026-11-25): proving is per-circuit
via `POST /prove`, whose body is the raw serialized proving payload — a
`(proof-preimage-versioned, option(proving-data), option(fr-bls))`
tuple, exactly what Midnight.js and the wallet SDK send — and whose
response is raw proof bytes headed `midnight:proof-versioned:`.
Midnight's own local-proving guide marks `/prove-tx` "Deprecated.
Midnight.js doesn't use it." In the verification run, a real 1,509-byte
DUST-spend payload captured from a live wallet session was proven by
the stock server in 2.56 s (sandbox CPU — not a phone measurement),
returning a 2,940-byte proof. The app now speaks this protocol:
payloads and proofs are handled as hex in the UI, proofs can be copied
out, in-flight proves can be cancelled, failures are classified
(unreachable / queue full 429 / busy 503 / server-rejected 400 with the
server's own message), and the server URL + job history persist across
launches.

Per the CLAUDE-AP-1 feasibility verdict (`research/android-prover-feasibility.md`),
the v1 deployment runs the official **arm64** proof-server build inside a
Linux environment on the phone (e.g. a Termux/proot Debian), reached by
this app at `http://127.0.0.1:6300`. The engine behind that interface is
swappable — a future native route can replace it without changing the app.
The server URL is configurable in the app, so the same build can also
point at a LAN or cloud proof server.

## Honest status

- **Not yet benchmarked on a phone.** No proving-time, thermal, or battery
  numbers exist for on-device proving, and this app claims none. The first
  device measurements will come from a supervised run on the owner's
  phone (a later, owner-gated step).
- The proof server fetches the public proving parameters from
  `srs.midnight.network` on first boot; budget storage and a network
  connection for that one-time setup.
- v0.2 carries the full Midnight-themed design (CLAUDE-AP-3): deep-space
  navy surfaces, indigo-to-violet brand gradient, cyan proof glow, an
  animated starfield with a crescent moon, three tabs (Prove / Activity /
  Server), a live server-status hero with pulsing orb, an elapsed-time
  proving state, and a session job history. Sol's improvement pass
  (SOL-AP-1) builds on this design when she returns.

## Build

CI builds every change (`.github/workflows/android-prover.yml`):
`gradle testDebugUnitTest assembleDebug` — the debug APK is uploaded as a
workflow artifact. Locally: Android Studio, JDK 17, Gradle 8.9,
compileSdk 35, minSdk 26.

## Attribution

Built by @kshot9000. Tips welcome:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
