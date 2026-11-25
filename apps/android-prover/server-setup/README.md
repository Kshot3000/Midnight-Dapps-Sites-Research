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
   pkg update && pkg install proot-distro curl python3
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
   from `srs.midnight.network` (~35 MB) — do it on Wi-Fi. When it prints
   `proof server is LIVE: /version = 9.0.2`, leave that Termux session
   running.

3. Open **Midnight Prover**, go to the **Server** tab (the URL should
   already read `http://127.0.0.1:6300`), and tap **Check server**. The
   status hero should light up cyan: *Proof server online · v9.0.2*.

## On a desktop or server (for LAN/cloud proving)

```sh
./setup-proof-server.sh --arch amd64 --start
```

Same flow, amd64 image. Point the app's Server URL at that machine's
address instead (e.g. `http://192.168.1.20:6300`).

## Verified status

- amd64: verified end-to-end 2026-11-25 — fresh install via this
  script, server live (`/version` 9.0.2, `/ready` ok), and a real
  1,509-byte DUST-spend proving payload proven via `/prove`
  (2,940-byte proof returned, ~3 s on a shared sandbox CPU).
- arm64/phone: the script's steps are identical and the official image
  ships the arm64 variant, but **no phone run has happened yet** — the
  first on-device install + measurements come from the owner's
  supervised run. Android may also kill background Termux processes
  under battery pressure; exempt Termux from battery optimisation
  before long proving sessions.

## Files

- `setup-proof-server.sh` — the installer/launcher generator
  (`--arch`, `--dir`, `--start`).
- The generated `run-proof-server.sh` (in the install dir) starts the
  server again on later boots: `~/.midnight-proof-server/run-proof-server.sh`

## Attribution

Built by @kshot9000. Tips welcome:
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
