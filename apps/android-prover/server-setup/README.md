# Running the proof server on the phone (Route 3)

The Midnight Prover app is a client. The prover itself is Midnight's
official **midnight-proof-server 9.0.2**, running in a Linux environment
on the same phone and reached by the app at `http://127.0.0.1:6300`.
This folder installs it **without Docker**: `setup-proof-server.sh`
pulls the official image layers straight from Docker Hub and runs the
genuine, unmodified binary through the glibc loader that ships inside
its own image.

## On Android (Termux + proot Debian)

1. Install **Termux** (from F-Droid or GitHub — the Play Store build is
   outdated), then inside Termux:

   ```sh
   pkg update && pkg install proot-distro
   proot-distro install debian
   proot-distro login debian
   ```

2. Inside the Debian environment:

   ```sh
   apt update && apt install -y curl python3 tar findutils procps
   curl -fsSLO https://raw.githubusercontent.com/Kshot3000/Midnight-Dapps-Sites-Research/main/apps/android-prover/server-setup/setup-proof-server.sh
   chmod +x setup-proof-server.sh
   ./setup-proof-server.sh --start
   ```

   The script defaults to the image's **arm64** variant (the phone's
   architecture). First boot downloads the public proving parameters
   from `srs.midnight.network` (the k=10–15 parameter files alone are
   about 12.4 MB; the full download, with keys, has not been measured) —
   do it on Wi-Fi. When it prints
   `proof server is LIVE: /version = 9.0.2`, leave that Termux session
   running.

3. Open **Midnight Prover**. The status card on the **Prove** tab (and
   on the **Server** tab) shows the server: tap **Check server** — the
   address should already read `http://127.0.0.1:6300`. When the server
   answers, the card turns cyan and reads *Proof server online*, with a
   `v9.0.2` chip. If it reads *Proof server busy* (amber), the server is
   up but working on other jobs.

4. **Keep it running.** Android can stop the server while it runs.
   Since Android 12 the *phantom process killer* stops child processes
   like this one, and battery optimisation can too. Before a long
   session, exempt Termux from battery optimisation and, on Android 14
   and later, turn on
   **Developer options → Disable child process restrictions**. Samsung phones may also slow Termux down; that is
   unverified on the Fold7.

## Wi-Fi warning: port 6300 is open to your network

**Proof server 9.0.2 cannot be limited to 127.0.0.1.** It has no
bind-address option: its `--help` lists only `--port`, `--verbose`,
`--job-capacity`, `--num-workers`, `--job-timeout` and
`--no-fetch-params`, and no environment variable or doc covers a bind
address. It listens on `0.0.0.0` — every network interface. This
script does **not** ship a bind shim; there is nothing to make it
loopback-only yet.

So while the server runs, **anyone on the same Wi-Fi can reach it and
send it proving jobs, using your phone's CPU.** Run it on trusted
networks only (not café, hotel or office Wi-Fi), and stop it (Ctrl-C in
Termux) when you are done. The app itself only ever talks to it over
`127.0.0.1` on this phone.

**Post-start check.** After every start, the launcher reads
`/proc/net/tcp` and `/proc/net/tcp6` to see where the port is actually
listening (`check-exposure.sh` in the install folder; run it yourself
any time with `~/.midnight-proof-server/check-exposure.sh 6300`).
Anything other than loopback only (`127.0.0.1` / `::1`) prints a loud
warning — and so does a table it can't read or a port it can't find, so
a failed check can never look safe. Verified on the build box only (it
correctly reported `0.0.0.0`); **not yet run on a phone**, where Android
may hide `/proc/net` from apps — in that case it warns that it couldn't
verify.

## On a desktop or server (advanced, off this phone)

```sh
./setup-proof-server.sh --arch amd64 --start
```

Same flow, amd64 image. The app does **not** use a server off this phone
by default, because the proving payload contains your private proof
inputs. To use one anyway: on the app's **Server** tab, turn on **Allow a
server off this phone** (read the warning), then enter an **https://** URL — the app
refuses plain http to anything except 127.0.0.1/localhost. Put the server
behind a TLS reverse proxy you control; the proof server itself serves
plain http only.

## Verified status

- amd64: verified end-to-end 2026-11-25 — fresh install via this
  script, server live (`/version` 9.0.2, `/ready` ok), and a real
  1,509-byte DUST-spend proving payload proven via `/prove`
  (2,940-byte proof returned, ~3 s on a shared sandbox CPU).
  Re-run for GROK-17 as a non-root user: install exit 0, launcher up on
  a custom port, `/version` 9.0.2, `/ready` ok, and the app's own client
  proved the fixture twice (722 ms and 449 ms, box CPU). That run found
  and fixed an extraction bug: the image's read-only `nix/store`
  directories made a non-root `tar -x` fail, and the old `|| tar -xf`
  fallback hid the error. The script now extracts with
  `--delay-directory-restore`, makes the tree writable, and stops on a
  real failure. Re-checked as uid 1000 after the exposure check was
  added: install exit 0 with no tar errors, server live on a custom
  port, and the post-start check printed the `0.0.0.0` warning.
- arm64/phone: the script's steps are identical and the official image
  ships the arm64 variant, but **no phone run has happened yet** — the
  first on-device install + measurements will come from a supervised
  test on the developer's own phone. See step 4 for keeping the server
  alive (phantom process killer, battery optimisation).

## Follow-ups

- Pin the `curl` URL in step 2 to a release tag instead of `main`
  (GROK-14 fix 9).

## Files

- `setup-proof-server.sh` — the installer/launcher generator
  (`--arch`, `--dir`, `--start`; `--check-exposure PORT [tables…]` runs
  the listen-address check on its own, which the repo tests use).
- The generated `check-exposure.sh` (in the install dir) — the
  post-start listen-address check.
- The generated `run-proof-server.sh` (in the install dir) starts the
  server again on later boots: `~/.midnight-proof-server/run-proof-server.sh`

## Attribution

Built by @kshot9000. Tips welcome:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
