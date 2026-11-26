# GROK-15 — Launch advertising asset pack

**Status: DRAFTS ONLY.** Nothing in this file is posted or published until Kshot okays it. Posting is the owner's or Muse's step, not the author's.
**Written:** 2026-11-25
**Written by:** Linda (Grok swarm, docs/content seat)
**Scope:** ready-to-use copy for the four verified milestones named in the wave 7 packet (`council/feed/GROK_FEED.md`), plus one long-form story. This PR does not edit `apps/android-prover/` or its README — those files are in GROK-17 (Victor).
**Networks:** no chain was touched while writing these drafts. Where a network is named, Preview (testnet) and Preprod (testnet) are public testnets.

## How to use these drafts

- Do not post until Kshot okays a launch.
- Tags of @MidnightNtwrk and @IOHK_Charles are for credit and visibility only. These drafts must not be read as an endorsement by those accounts.
- Each X post is at most 280 characters using X's URL weight (each `http://` or `https://` URL counts as 23 characters). Raw character counts are also recorded below.
- The repo link `https://github.com/Kshot3000/Midnight-Dapps-Sites-Research` appears in at most two of the four posts (posts 1 and 3).
- Preview (testnet) and Preprod (testnet) are labelled testnet wherever they are named.
- Tip copy says "Tip" or "Support the dev" only. Tips unlock nothing.
- Apps mentioned here are free during launch, unaudited, and not a Play Store listing.
- The four short posts cannot hold both tip addresses. After a post, or at the end of a thread, paste the attribution reply in section 3.
- **[CONFIRM AFTER GROK-17]:** Post 1 and the long-form name Android prover **v0.3.1**, which is the version on `main` today (`apps/android-prover/app/build.gradle.kts`). If GROK-17 ships a new version, update the string before posting. These drafts also do not claim that current in-app copy, `/ready` 503 handling, or tip wording is launch-ready — those are open GROK-14 findings for Victor to fold into GROK-17.

## Sources for every number

Every figure in the drafts traces to a merged brief or a board entry. Muse's sandbox numbers are labelled as Muse's. Hermes's RTX 3090 numbers are labelled as Hermes's. Nothing here is a phone measurement.

| Figure | Where it appears | Source |
| --- | --- | --- |
| Android prover v0.3.1 / versionCode 4 | Post 1, long-form | `apps/android-prover/app/build.gradle.kts`; board row CLAUDE-AP-5; [`android-prover-qa.md`](android-prover-qa.md) (GROK-14) |
| Official proof-server 9.0.2; live per-circuit `POST /prove` | Post 1, long-form | Board row CLAUDE-AP-4 (PR #69); [`stack-versions.md`](stack-versions.md) |
| Real 1,509-byte DUST-spend payload → 2,940-byte proof in 2.56 s on sandbox CPU, not a phone measurement | Long-form | Board row CLAUDE-AP-4; Muse's run on 2026-11-25, not reproduced here. Labelled as Muse's. |
| No-Docker official-server installer verified on amd64; arm64 phone run still the owner's supervised step | Post 1, long-form | Board row CLAUDE-AP-5 (PR #71). Muse's amd64 run, not reproduced here. |
| Proof Station v0.2.0 | Post 2, long-form | Board row SOL-GPU-2 (PR #47); `apps/proof-station/README.md` |
| RTX 3090, k=16, cold 1.168 s / warm 0.766 s, 0 CUDA fallbacks, stock CPU verifier passed | Post 2, long-form | Board rows SOL-GPU-1 and SOL-GPU-2; Hermes's measured run posted on PR #4, recorded in `apps/proof-station/README.md`. Labelled as Hermes's. Not reproduced here. |
| CUDA path still experimental | Post 2, long-form | Proof Station README: draft `midnight-zk` PR #667 / experimental. A wallet-compatible CUDA release still needs fresh checks. |
| Preview (testnet) sync still pending | Long-form, not-done list | Board rows SOL-GPU-1 and SOL-GPU-2: needs a synced Cardano Preview (testnet) db-sync. |
| VIA USDM is the only mainnet-evidenced value route onto Midnight; official NIGHT bridge off pending governance | Post 3, long-form | [`bridges-status.md`](bridges-status.md) (GROK-6), checked 2026-11-24. "Live per public evidence, not used by me." Official C2M bridge: Ledger 9 page, "a separate governance action." |
| Glossary, DUST registration, bridges status | Post 3, long-form | [`glossary.md`](glossary.md) (GROK-13), [`dust-registration.md`](dust-registration.md) (HERMES-5), [`bridges-status.md`](bridges-status.md) (GROK-6) |
| NightAttest first slice: Compact 0.36.1 contract + local simulator; role UI and live testnet run still ahead | Post 4, long-form | Board row SOL-2 (PR #40); `apps/nightattest/README.md` |
| Compact 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector 4.1.0, indexer 4.4.101, Wallet SDK 2.0.1 | Long-form | [`stack-versions.md`](stack-versions.md). Ledger 8-era pins are not current and are not used here. |
| Hub URL | Long-form | Live GitHub Pages hub for this repo. The hub does not yet mention the Android prover; that gap is Sol's lane and is not claimed here. |

## 1. Four X post drafts

Paste the text inside each `text` block as-is, after Kshot okays posting. Do not add claims that are not already in the block.

### Post 1 — Android prover protocol

- **X length:** 278 characters (URL weighted at 23). Raw length 313.
- **Repo link:** yes (1 of 2).
- **[CONFIRM AFTER GROK-17]:** the `v0.3.1` version string.

```text
Android prover v0.3.1 speaks real per-circuit POST /prove to official proof-server 9.0.2. Muse verified a real payload → real proof on sandbox CPU, not a phone. No-Docker installer is in the repo; arm64 phone run still ahead.
@MidnightNtwrk @IOHK_Charles
https://github.com/Kshot3000/Midnight-Dapps-Sites-Research
```

### Post 2 — Proof Station measurements

- **X length:** 269 characters. Raw length 269.
- **Repo link:** no.

```text
Proof Station v0.2.0: Windows Preview (testnet) node, indexer, and local proving. Hermes measured k=16 on an RTX 3090 at 1.168 s cold / 0.766 s warm, 0 CUDA fallbacks; both proofs passed the stock CPU verifier. CUDA path still experimental.
@MidnightNtwrk @IOHK_Charles
```

### Post 3 — Research library and bridges

- **X length:** 278 characters (URL weighted at 23). Raw length 313.
- **Repo link:** yes (2 of 2).

```text
Sourced Midnight research is in the repo: glossary, DUST registration, and bridges status. Evidence: VIA USDM is the only mainnet-evidenced value route onto Midnight today. Official NIGHT bridge still off, pending governance.
@MidnightNtwrk @IOHK_Charles
https://github.com/Kshot3000/Midnight-Dapps-Sites-Research
```

### Post 4 — NightAttest first slice

- **X length:** 259 characters. Raw length 259.
- **Repo link:** no.

```text
NightAttest, first slice: a Compact 0.36.1 workshop-pass contract that proves "I hold an active pass" while the credential stays private. Compiled and checked in the local simulator. Role UI and a live testnet run are still ahead.
@MidnightNtwrk @IOHK_Charles
```

## 2. Long-form story draft

A thread or community post. **2,498 characters**, in the requested 1,500–2,500 range, including the tip addresses and the independence line. **[CONFIRM AFTER GROK-17]:** the `v0.3.1` version string, and do not treat current in-app copy as final.

```text
Independent builder — not affiliated with the Midnight Foundation or IOG. I publish Midnight apps, a public hub, and sourced research at @kshot9000.

The hub is the public build queue and a record of what I can verify: https://kshot3000.github.io/Midnight-Dapps-Sites-Research/

Proof Station v0.2.0 is a Windows console for a Midnight Preview (testnet) full node, indexer, and local proving, plus a Proof Lab for CPU and experimental CUDA proofs. Hermes measured a k=16 circuit on an RTX 3090 at 1.168 s cold and 0.766 s warm, with 0 CUDA fallbacks; both proofs passed the stock CPU verifier (SOL-GPU-1 / SOL-GPU-2; Proof Station README). A fresh CUDA re-run and Preview (testnet) sync are still pending.

The Android prover is at v0.3.1. It talks to official proof-server 9.0.2 over the live per-circuit POST /prove protocol. Muse verified that path on 2026-11-25: a 1,509-byte DUST-spend payload returned a 2,940-byte proof in 2.56 s on sandbox CPU — not a phone measurement (CLAUDE-AP-4, PR #69). A no-Docker official-server installer was verified on amd64 the same day (CLAUDE-AP-5, PR #71). No phone install or phone timings have been run; the app claims none. Free during launch, unaudited, not a Play Store listing.

The research library includes a glossary, a DUST registration explainer, and a bridges status brief. DUST is the shielded fee resource generated by registered NIGHT, not a transferable token. VIA's USDM route is the only mainnet-evidenced way to move value onto Midnight today. The official Cardano→Midnight NIGHT bridge shipped in ledger 9 but stays off pending governance.

NightAttest's first slice is a Compact 0.36.1 workshop-pass contract, compiled and checked in a local simulator with synthetic data. It proves one claim: I hold an active pass. The role UI and a live Preview (testnet) or Preprod (testnet) proof run are still ahead.

Not done: no phone benchmarks; NightAttest has no UI and has not run on a testnet; Proof Station's Preview (testnet) sync is still open. Pins: Compact 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector 4.1.0, indexer 4.4.101, Wallet SDK 2.0.1. Repo: https://github.com/Kshot3000/Midnight-Dapps-Sites-Research

Tip / support the dev (tips unlock nothing):
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
```

## 3. Attribution reply (for after an X post)

The four short posts cannot hold both addresses. After posting, or as the last reply in a thread, paste:

```text
Independent builder — not affiliated with the Midnight Foundation or IOG.

I share updates at @kshot9000. Tip / support the dev (tips unlock nothing):
Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
```

## 4. What these drafts do not claim

- No phone proof times, thermals, or battery figures. The 2.56 s figure is Muse's sandbox CPU, labelled as such.
- No Play Store availability, no Linux Terminal on any named phone, and no React Native / WASM route for v1.
- No implied endorsement by @MidnightNtwrk or @IOHK_Charles.
- NightAttest has no UI and has not run on Preview (testnet) or Preprod (testnet).
- Proof Station's Preview (testnet) sync is still pending.
- The Android prover has not had an owner-supervised arm64 phone install.
- Rivals are not named. Held-back internal workstreams stay out of this pack.
- Store-listing short/long descriptions and a separate press one-pager were not in the board row or the wave 7 packet, so they are not in this pack.

## 5. What was not verified in this task

- I did not post anything, and I did not open an X account or a store listing.
- I did not run a phone, emulator, proof server, GPU, Windows box, or NightAttest compile.
- I did not reproduce Muse's 1,509-byte → 2,940-byte / 2.56 s run, Muse's amd64 installer run, or Hermes's RTX 3090 timings. Those numbers are cited from the board and the merged briefs, and they are labelled as their authors'.
- I did not re-fetch VIA Scan, the official bridge governance pages, or the live hub in this task. Those claims rest on the merged GROK-6 brief (checked 2026-11-24) and on the already-published hub URL.

---

Support the dev — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
