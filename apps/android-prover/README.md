# Midnight Prover — Android app (v0.4.0)

An Android client for Midnight's official proof server: it sends one
proving payload to the server and gets the proof back. The goal is to run
that server on the phone itself; that phone run hasn't happened yet.
**Testnet only** (Preview and Preprod are testnets).
Mission context: help take the Midnight ZK prover mobile, building on the
same proof-server line this team verified on Windows (Proof Station) and
in the cloud.

## Design — Route 3 (CLAUDE-AP-1 verdict)

The app does **not** reimplement the prover. It talks to Midnight's
official **midnight-proof-server 9.0.2** over its standard HTTP API:

- `GET /version` — expect `9.0.2`
- `GET /proof-versions` — `["V2","V3"]`
- `GET /ready` — readiness + job capacity (`503` with `"status":"busy"`
  when the queue is full — shown as **busy**, never as unreachable)
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
(unreachable / timed out / queue full 429 / busy 503 / server-rejected
400 with the server's own message / cancelled), and the server URL, the
off-phone opt-in and the last 50 jobs are kept between launches.

GROK-17 ships a real proving payload as the test fixture
(`app/src/test/resources/fixtures/dust-spend-payload.hex`, 1,509 bytes,
`midnight/dust/spend`). It was captured from the official hello-world flow
on a **local `undeployed` devnet** — not testnet, not mainnet.

Per the CLAUDE-AP-1 feasibility verdict (`research/android-prover-feasibility.md`),
the v1 deployment runs the official **arm64** proof-server build inside a
Linux environment on the phone (e.g. a Termux/proot Debian), reached by
this app at `http://127.0.0.1:6300`. The engine behind that interface is
swappable — a future native route can replace it without changing the app.

**Privacy (v0.4.0).** Proving payloads carry private witness data, so
plain http is allowed **only to this phone** (`127.0.0.1` / `localhost`,
enforced by `res/xml/network_security_config.xml`). A server off the
phone needs an explicit opt-in on the Server tab, behind a warning that
private inputs leave the phone, and must use **https**.

## Honest status

- **Not yet benchmarked on a phone.** No proving-time, thermal, or battery
  numbers exist for on-device proving, and this app claims none. The first
  device measurements will come from a supervised run on the owner's
  phone (a later, owner-gated step).
- The proof server fetches the public proving parameters from
  `srs.midnight.network` on first boot (the k=10–15 files alone are about
  12.4 MB; the full download hasn't been measured). Use Wi-Fi.
- **Wi-Fi warning: port 6300 is open to your network.** Proof server
  9.0.2 has no bind-address option and listens on `0.0.0.0` (every
  interface), so anyone on the same Wi-Fi can reach it and send it
  proving jobs. There is no bind shim. The setup script's post-start
  check reads `/proc/net/tcp{,6}` and warns loudly unless the port is
  loopback-only; that check is verified on the build box only, not yet
  on a phone. Use trusted networks and stop the server when you're done.
  Details: `server-setup/README.md`.
- v0.4.0 (GROK-17) keeps the Midnight-themed design (CLAUDE-AP-3): three
  tabs (Prove / Activity / Server), a server-status card (online, busy,
  not ready, wrong version, unreachable), a "Testnet only" label, a Tip
  card with both addresses, a dark launch theme and adaptive icon, and an
  accessible palette (Bob's P1-14 hex values; 52 dp primary buttons). The
  bigger redesign (job flow, two-pane foldable layout, splash screen,
  edge-to-edge) is GROK-18.

## Running the server on the phone

`server-setup/` installs the official arm64 proof server inside a
Termux/proot Debian environment — no Docker: the setup script pulls the
official 9.0.2 image layers from Docker Hub and runs the genuine binary
via its own staged loader. Verified end-to-end on amd64 (2026-11-25:
fresh install → live server → real payload proven); the arm64 phone run
is still the owner's supervised step. See `server-setup/README.md`.

## Build and test

CI builds every change (`.github/workflows/android-prover.yml`):
`gradle testDebugUnitTest assembleDebug` — the debug APK is uploaded as a
workflow artifact. Locally: Android Studio, JDK 17, Gradle 8.9,
compileSdk 35, targetSdk 35, minSdk 26.

Unit tests run on the JVM (Robolectric pinned to `sdk=34` so JDK 17
works). An optional live check runs the app's client against a real
proof server:

```sh
PROOF_SERVER_URL=http://127.0.0.1:6300 gradle testDebugUnitTest --tests '*LiveProofServerTest*'
```

## Attribution

Built by @kshot9000. Tips welcome:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
