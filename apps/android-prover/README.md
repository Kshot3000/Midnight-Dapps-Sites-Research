# Midnight Prover — Android app (v0.8.0)

An Android client for Midnight's official proof server: it sends one
proving payload to the server and gets the proof back. The goal is to run
that server on the phone itself; that phone run hasn't happened yet.
**Testnet only** (Preview and Preprod are testnets).
Mission context: help take the Midnight ZK prover mobile, building on the
same proof-server line this team verified on Windows (Proof Station) and
in the cloud.

This is the Fold7 sideload build (versionCode 11). Sideloading and the
first on-device run stay the owner's supervised step.

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
Midnight.js doesn't use it." The app speaks this protocol: payloads and
proofs are handled as hex in the UI, proofs can be copied, shared or
saved, in-flight proves can be cancelled, and failures are classified
(unreachable / timed out / queue full 429 / busy 503 / server-rejected
400 with the server's own message, stripped of non-printable characters /
cancelled).

GROK-17 ships a real proving payload as the test fixture
(`app/src/test/resources/fixtures/dust-spend-payload.hex`, 1,509 bytes,
`midnight/dust/spend`). It was captured from the official hello-world flow
on a **local `undeployed` devnet** — not testnet, not mainnet.

Per the CLAUDE-AP-1 feasibility verdict (`research/android-prover-feasibility.md`),
the v1 deployment runs the official **arm64** proof-server build inside a
Linux environment on the phone (e.g. a Termux/proot Debian), reached by
this app at `http://127.0.0.1:6300`. The engine behind that interface is
swappable — a future native route can replace it without changing the app.
The server listens on `0.0.0.0`; the app describes it as **reachable at**
`127.0.0.1`, not as starting only on loopback.

**Privacy.** Proving payloads carry private witness data, so plain http
is allowed **only to this phone** (`127.0.0.1`, `localhost`, and `::1`,
enforced by `res/xml/network_security_config.xml` in lockstep with the
app's on-phone check). A server off the phone — **LAN included** — needs
an explicit opt-in in Settings, behind a warning that private
inputs leave the phone, and must use **https**. Proof server 9.0.2 only
speaks plain http, so a LAN (or cloud) server must sit behind a TLS
reverse proxy; there is no https mode in 9.0.2 itself.

The app stores no cloud backup (`android:allowBackup="false"`, with
`dataExtractionRules` / `fullBackupContent` excluding app data).

## What v0.5.0 adds (GROK-18)

- Job flow: Paste or Open file, a payload summary, a progress track, and
  a result card with Copy, Share and Save.
- Fold7 two-pane layout with a navigation rail (window size classes).
- First-run onboarding that walks through server-setup.
- Auto-polling server status card: Online, Busy, Wrong version,
  Unencrypted, Timeout, and Unreachable (a timed-out check is a timeout,
  not unreachable).
- Foreground service with a notification and a Cancel action while
  proving; the job is restored after process death.
- Splash screen API, edge-to-edge, and motion that respects
  reduced-motion.
- About / Tip sheet with both tip addresses exact, the Tip wording,
  @kshot9000, and the independence line.
- TalkBack labels on the tab icons, live announcements, a starfield that
  pauses during proofs, and "Check server" no longer hiding the proof.
- Android Gradle Plugin 8.7.3 (Gradle 8.9, compileSdk 35).

## What v0.8.0 changes (SOL-AP-1)

Sol's native GUI refinement of the shipped app, against the UI v2
mobile reference and the black-and-white Midnight direction:

- A compact server status card brings the current connection,
  refresh, and server settings onto the Prove screen.
- The proof job opens with Paste, Import file, and Try sample. A
  loaded payload shows its circuit, size, and format before the
  primary Prove action; detailed metadata and raw hex are on demand.
  Replace payload restores the import choices without clearing the
  current draft.
- Copy, Share, and Save sit near the top of the result; the result
  keeps the privacy cloak, and Start a new job clears the draft
  while preserving activity history.
- In-app and shared file imports share one asynchronous size check
  with error feedback; a rejected file leaves the current draft
  intact. At larger font scales the step indicator gives the active
  stage its own full-width label, and an off-phone destination stays
  visible before proving.

The proof-server client and circuit protocol are unchanged.

## What v0.7.1 fixes (CLAUDE-AP-8 fixup)

The app mark is now the actual Midnight token symbol — a white ring
with three stacked squares on black — redrawn from the owner's
reference image, replacing the crescent approximation that shipped in
v0.7.0. Same vector everywhere: screen headers, the onboarding
welcome, and the launcher icon.

## What v0.7.0 changes (CLAUDE-AP-8)

A full visual redesign to a monochrome system, at the owner's
direction: true black surfaces, white primary actions and text, a
grayscale ramp for everything else — no hue anywhere; state is carried
by icon, label and lightness. The Midnight token symbol (a ring with three
stacked squares) is the app's mark: it leads every screen header,
fronts the onboarding welcome, and is the launcher icon on black. The starfield stays but whispers — dimmer, grayscale, behind
near-black cards with hairline borders. Every contrast invariant is
unchanged and still test-enforced (all text AA on the card surface;
the primary button is now white with black text at 16:1+). The design
tokens live in `Theme.kt` (`MidnightPalette`); the mark is
`res/drawable/ic_midnight_token.xml`.

## What v0.6.0 adds (CLAUDE-AP-7)

Privacy cloak, adopted from the UI v2 design package (`design/ui-v2/`):
a Settings toggle that masks proof and payload hex wherever the app
renders it read-only — the result card's proof preview and the history
rows — so screenshots and screen shares don't leak payload material.
Display only: copy, share and save always use the real proof. The
masking itself is a pure helper (`HexCloak`) with unit tests.

## What v0.5.1 fixes (GROK-20)

Joe's PR #88 nits, in this order: the onboarding copy no longer says
inputs "never leave the phone" or that the app "never sends anything to
a network" (the default on-phone server keeps inputs on the device; an
off-phone server is an explicit Settings opt-in and must use https);
nav and step labels no longer break mid-word at 360 dp / font scale 2.0;
the onboarding Skip control sits on a dark pill so it stays readable
over the moon; a second start of the foreground service no longer leaks
a wake lock or posts a "Proof failed" notification when nothing is
running; typed server URLs with a path, query, fragment or userinfo are
refused instead of being silently stripped; the README points the
off-phone opt-in at Settings; the Wi-Fi exposure note is no longer
hard-coded to port 6300; interrupted jobs are recorded in history;
file imports run off the main thread and report an oversize file
instead of ignoring it; `ACTION_VIEW` has an intent-filter so a payload
file can open the app.

## Honest status

- **Not yet benchmarked on a phone.** No proving-time, thermal, or battery
  numbers exist for on-device proving, and this app claims none. The first
  device measurements will come from a supervised run on the owner's
  phone (a later, owner-gated step). Timings below are from the build box.
- The proof server fetches the public proving parameters from
  `srs.midnight.network` on first boot. On this build box a first start
  of 9.0.2 landed about 33 MB in the params cache (k=10–15, plus proving
  and verifying keys and ZKIR sources). Use Wi-Fi. A phone's first-boot
  download has not been measured.
- **Wi-Fi warning: the proof-server port is open to your network.** Proof
  server 9.0.2 has no bind-address option and listens on `0.0.0.0` (every
  interface), so anyone on the same Wi-Fi can reach it and send it
  proving jobs. There is no bind shim. The setup script's post-start
  check reads `/proc/net/tcp{,6}` and warns loudly unless the port is
  loopback-only; if `/proc/net/tcp6` can't be read, the check reports
  "can't verify", never OK, even when IPv4 looks like loopback. That
  check is verified on the build box only, not yet on a phone. Use
  trusted networks and stop the server when you're done. The installer
  accepts `--port N` (default 6300) and passes it through to the listen
  check. Details: `server-setup/README.md`.
- v0.4.0 (GROK-17) was the safe working build: three tabs, the classified
  failure modes, "Testnet only", Tip with both addresses, and Bob's
  accessible palette (exact hex values; 52 dp primary buttons). v0.5.0
  is the redesign plus the nits from that review.

**Live check (build box, 2026-11-25, America/Chicago).** The app's own
client was run again against official proof server 9.0.2 on
`http://127.0.0.1:6301` (this box's 9.0.2 port; do not assume port 6300
is 9.0.2). Status sweep 23 ms, version `9.0.2`, proof versions
`["V2","V3"]`, `/ready` ok, capacity 10. The 1,509-byte DUST-spend
fixture was proven twice: 708 ms then 473 ms, each returning a 2,940-byte
proof headed `midnight:proof-versioned:`. A garbage body was rejected
with the server's own header-tag message. These are sandbox CPU times,
not a phone measurement. Parameters had already been fetched in that
server process before the first prove.

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
Android Gradle Plugin 8.7.3, compileSdk 35, targetSdk 35, minSdk 26.

Unit tests run on the JVM (Robolectric pinned to `sdk=34` so JDK 17
works). Screenshot fixtures for five configurations (411 dp, 360 dp,
360 dp at font scale 2.0, 884 dp unfolded, landscape) are recorded into
`docs/screenshots/`. An optional live check runs the app's client against
a real 9.0.2 proof server:

```sh
PROOF_SERVER_URL=http://127.0.0.1:6301 gradle testDebugUnitTest --tests '*LiveProofServerTest*'
```

Point `PROOF_SERVER_URL` at a 9.0.2 server. On the QA box used for this
release that is port 6301; port 6300 may be an older 9.0.0 and must not
be used for this check.

## Attribution

Built by @kshot9000. Tips welcome:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
