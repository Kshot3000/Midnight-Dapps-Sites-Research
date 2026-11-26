# HERMES-3 — Hands-on Local Stack Verification

**Run:** 2026-11-24 (all commands executed and timed on this date, 05:30–07:00 CST / 11:30–13:00 UTC)
**Machine:** Kyle's Windows 11 box (i9 / RTX 3090 24 GB / 128 GB RAM), Docker Desktop with WSL2 backend, Ubuntu distro (`octanode`). Node **v22.23.5** (nvm), npm **11.21.0** — both matching the repo's `toolchain.json` pins (`"node": "22"`, `"npmCli": "11.21.0"`).
**Repo under test:** `midnightntwrk/midnight-examples`, `main` @ **953f864** (merge of upstream PR #135, "leaderboard-skip-l8-dust") — the same commit Muse verified against the live repo for HERMES-2.
**Toolchain:** Compact CLI (devtools) **0.5.3**, compiler **0.36.1**, language **0.28** — per `toolchain.json` (the "one toolchain pin"), confirmed by `npm run toolchain:check`: `✓ toolchain:check: 26 projects agree with toolchain.json`.

Every number below was measured on this machine in this session. Nothing here was taken from memory or the docs.

## 1. Setup (exact commands)

```bash
# in WSL (Ubuntu distro)
git clone https://github.com/midnightntwrk/midnight-examples
cd midnight-examples
npm install                      # workspace monorepo: deps hoist to root node_modules (520 packages);
                                 # postinstall builds the mnkit packages
npm run toolchain:check          # ✓ 26 projects agree with toolchain.json
cd examples/hello-world
npm run env:up                   # docker compose up -d --wait
```

- First `env:up` (images pre-pulled): **50 s** wall, all three containers healthy.
- Images, pinned in `toolchain.json` → `images`: `midnightntwrk/midnight-node:2.1.101` (node image digest `sha256:0c1ad548…8a`), `midnightntwrk/proof-server:9.0.2`, `midnightntwrk/indexer-standalone:4.4.101`.
- Endpoints (from `toolchain.json` → `networks.local`): node WS `ws://127.0.0.1:9944`, indexer GraphQL `http://127.0.0.1:8088/api/v4/graphql`, proof server `127.0.0.1:6300`. `networkId` is `undeployed`; the node log calls the chain spec **`undeployed1`**, and wallet addresses carry that prefix (`mn_addr_undeployed1…`).
- Block time: **6 s** (6-second slots observed continuously in the node log).
- The "local" stack is **not offline**: on first boot the proof server downloads Zswap/Dust ZKIR sources and proving/verifying keys from `srs.midnight.network`, and the first `k=6` proof fetches public parameters from the same host (see §4).

## 2. Compilation (toolchain 0.36.1 / language 0.28)

| Example | Command | Circuits | Measured time |
| --- | --- | --- | --- |
| hello-world | `npm run compile` | 1 | **1.18 s** (clean rebuild) |
| calculator | `npm run compile` | 5 | **0.93 s** (clean rebuild) |
| hello-world | `npm run compile:fast` (`--skip-zk`) | 1 | 2.29 s (measured; both runs are sub-3 s, machine warm) |

All contracts declare `pragma language_version 0.28;` and compile unmodified. `toolchain:check` enforces that every project's pragmas/pins agree with `toolchain.json`.

**The feed asked for "hello-world and counter". `counter` does not exist in the current `midnight-examples` tree.** The 12 examples are: battleship, calculator, hello-world, leaderboard, private-bid, private-party, private-tip-jar, shielded-chips, silent-auction, token-transfers, voting, zk-loan. The "counter" contract lives only inside the official docs' ledger-8-era guides (see §6, item 4). I ran **calculator** as the closest live stand-in for the feed's counter E2E: public-ledger arithmetic updated through ZK circuits, including a `divMod` witness circuit.

## 3. First-boot failure (what broke #1 — recovered, not reproduced)

On the very first node start, ~27 s in, after producing blocks #1–#5, the node shut itself down at block #6:

```
2026-11-24 11:40:50 💤 Idle (0 peers), best: #5 (0x56bd…9e3c), finalized #1 (0x5811…f589), ⬇ 0 ⬆ 0
Essential task 'txpool-background' failed. Shutting down service.
Dropped HF storage after rollback (separate)
```

- No deeper error is logged at the default log level — that one line is the whole story.
- The proof server shut down cleanly in the same second (actix "shutting down idle worker"); the indexer kept running (it has `restart: on-failure`, the other two have no restart policy).
- Recovery was exactly what the docs' troubleshooting table prescribes for first-start flakiness: rerun `npm run env:up`. The node resumed from block #5 (ParityDb state persists in the container layer — the compose files define **no volumes**) and then produced continuously with no recurrence: block **#400+** and 40+ minutes of uninterrupted operation at the time of writing.
- Assessment: one-off first-boot failure, plausibly a race on the WSL2 Docker storage whose I/O is far below the node's authority minimum (warning below). Not a blocker; root cause not identified from the available logs.

Node startup warning, present on **both** boots (the node continues anyway, dev preset):

```
⚠️  The hardware does not meet the minimal requirements Failed checks: Copy(expected: 15.84 GiBs, found: 12.25 GiBs),
Seq Write(expected: 5.39 GiBs, found: 101.85 MiBs), Rnd Write(expected: 1.90 GiBs, found: 33.95 MiBs), for role 'Authority'.
```

(Second boot: Copy 12.33 GiB/s, Seq Write 88.32 MiB/s, Rnd Write 50.40 MiB/s — same verdict.)

## 4. wait:dust

```bash
npm run wait:dust        # hello-world; vite-node scripts/wait-for-dust.ts
```

Wallet built from the repo's saved seed (generated by `npm run wallets:new`), synced, and `DUST ready` printed in **~90 s** — DUST was already accrual-eligible on-chain at that point; it accrues as the node produces blocks. The script waits for ≥1 spendable DUST coin (≥60 DUST at the latest block), timeout 180 s (`WAIT_FOR_DUST_TIMEOUT_MS`). There is **no local faucet** (`toolchain.json` → `networks.local.faucet` is empty): the pre-funded genesis wallet plus DUST accrual is the only funding path.

## 5. End-to-end runs (deploy → state-changing call → indexer confirms)

Both runs used `npm run test:local` = `MIDNIGHT_NETWORK=local` + vitest 5.0.4 (local `hookTimeout` 15 min, `testTimeout` 10 min per `vitest.config.ts`; actual runs used ~1/5 of that).

### hello-world — 2/2 passed, 37.8 s total

| Step | Time (test timer) |
| --- | --- |
| Deploy contract (private `ledger message`) | **14.9 s** |
| `storeMessage("Hello World!")` + read back | **18.3 s** |

Contract: `4bcd747c9cfd2514cc210332e27d66ed1cd99d401c0474b102cc222bcca6fafa`.

Proof-server log (UTC) for the state-changing call:

```
12:09:08.469  Starting to process request for /check...      → took 0.001566s
12:09:08.478  Starting to process request for /prove...       → took 1.125684s  (includes 0.70 s downloading k=6 public parameters from srs.midnight.network)
12:09:09.631  Starting to process request for /prove...       → took 0.498643s  (parameters cached)
```

> **Host corrected 2026-11-25 (GROK-13-fixup, Bob Research):** this brief first named the parameter host `srs.midnight.net` here and in §1 and §7 item 8. All three are now `srs.midnight.network`. What was checked before editing the excerpt above: no raw HERMES-3 proof-server log exists anywhere in the repo (`research/evidence/` holds only GROK-12 files; `git log --all -p` shows this brief arrived whole in one commit, `32ab0b2`, with no log attachment). The excerpt is a hand-condensed summary (shortened timestamps, `→ took` arrows), and the host sits in the author's own parenthetical, not in a log field: proof server 9.0.2 does not print the host on a successful fetch (see the verbatim GROK-12 log, [`evidence/grok-12/proof-server-log.txt`](evidence/grok-12/proof-server-log.txt), "Fetching public parameters for k=6 - finished."). Only the parenthetical host was changed; the timings are untouched. Evidence for `.network`: the same 9.0.2 server's own error in [`evidence/grok-12/run1-failure.txt`](evidence/grok-12/run1-failure.txt) line 23 fetches `https://srs.midnight.network/bls_midnight_2p10`; the official [Run the proof server](https://docs.midnight.network/guides/run-proof-server) guide names `https://srs.midnight.network/`; and on 2026-11-25 `srs.midnight.network` completed a TLS handshake (HTTP 403 at the bare root) while `srs.midnight.net` failed the TLS handshake (`unexpected eof while reading`).

**CPU proof generation time for the state-changing call (k=6): ~0.50 s warm; ~1.13 s cold including the one-time public-parameter fetch.** The 18.3 s wall time is the rest of the path: circuit evaluation in JS (midnight-js/wasm), wallet sync/sign, submission, and waiting on the indexer to confirm against 6 s blocks.

### calculator — 6/6 passed, 144.1 s total

| Step | Time |
| --- | --- |
| Deploy contract (public `ledger result: Uint<16>`) | 17.9 s |
| `add` two numbers | 16.9 s |
| `subtract` two numbers | 16.0 s |
| `multiply` two numbers | 18.7 s |
| `square` a number | 16.1 s |
| `divide` two numbers (verifying the `divMod` witness) | 17.3 s |

Contract: `a05b0954ca5123e8912ec7971bf4feb353b8467960087ce750d33dd7b4ad06f3`; wallet address `mn_addr_undeployed1h3ssm5ru2t6eqy4g3she78zlxn96e36ms6pq996aduvmateh9p9sk96u7s`; wallet sync completed after 12 emissions. One unexplained anomaly: vitest printed its first output ~30 min after the process launched (`Start at 06:41:13` vs launch ≈ 06:11), with the stack and WSL VM verifiably healthy throughout (continuous, gapless block production; no VM reboot — `uptime` showed one continuous boot). The suite itself then ran cleanly in 144 s. Cause not identified; WSL client scheduling under concurrent clients is the prime suspect. Logged here as an observation, not a stack fault.

## 6. Resources (measured, 5 s sampling across the hello-world test window)

| Container | RAM peak | CPU |
| --- | --- | --- |
| node | 264.5 MiB | 0.8–7 % |
| proof-server | 121.6 MiB | **390 % spike** during the prove burst, 39 % on the second prove |
| indexer | 48.1 MiB | 0.1–4.8 % |

**Stack RAM peak ≈ 434 MiB total** (of 120.9 GiB visible to the WSL VM on the 128 GB host).

GPU (feed item 5, "report what you observe, not what you expect"): **no stack-attributable GPU activity.** The compose files map no GPU devices into any container and the proof path is CPU-only; `nvidia-smi` during proving showed only a resident LLM model on the RTX 3090 (≈22.6 GB of 24.6 GB VRAM, 2–35 % util at idle sampling). The local stack does not touch the 3090 at all.

## 7. Version friction / what was confusing (input for the Version Doctor spec)

1. **Docs quickstart says `yarn`, repo is npm.** The official hello-world tutorial's local section runs `yarn compile`, `yarn env:up`, `yarn wait:dust`, `yarn test:local` (docs source `docs/getting-started/hello-world.mdx:60–107`); the repo ships `package-lock.json` and pins `npmCli 11.21.0` — there is no `yarn.lock`.
2. **Docs compile path is stale (plural).** The tutorial compiles `contracts/hello-world.compact → contracts/managed/hello-world`; the repo path is `contract/…` (singular). `contracts/` does not exist.
3. **Quoted compile output is stale in both places.** Docs expect `circuit "storeMessage" (k=6, rows=26)`; the repo README expects `(k=6, rows=36)`. Compiler 0.36.1 at default verbosity prints only `Compiling 1 circuits:` — no per-circuit detail line at all.
4. **Docs' inline counter contracts don't compile with the current toolchain.** Both ledger-8-era guides' `counter.compact` (node-toolkit guide: `language_version 0.22`, compiler 0.30.0; browser-dapp guide: `language_version 0.23`, compiler 0.31.1) were compiled here with 0.36.1 and both were rejected with the exact error:
   ```
   Exception: <file>.compact line 1 char 1:
     language version 0.28.0 mismatch
   ```
   (The node-toolkit page itself quotes the same error format for 0.31.1: "language version 0.23.0 mismatch".)
5. **"The counter example from midnight-examples" is a phantom.** No counter exists in the current tree (§2); guides and tasks that reference it point at the docs' inline contract, not a repo example.
6. **Image tag ≠ binary banner.** `midnightntwrk/indexer-standalone:4.4.101` prints "Midnight Indexer **v3.0.15**" on boot — two different versioning schemes for the same artifact.
7. **First-start flakiness has a second mode.** The docs' troubleshooting table lists `…-indexer-1 exited (1)` on first start (→ rerun `env:up`); this run hit a different first-start failure (node `txpool-background`, §3) that is not in the table. Same recovery.
8. **The "local" stack phones home.** First-use SRS parameter/key downloads from `srs.midnight.network` (§1, §4) — worth stating explicitly in the docs for air-gapped expectations.
9. **Cosmetic stderr noise.** Wallet sync repeatedly logs `RPC-CORE: subscribeRuntimeVersion(): disconnected from ws://127.0.0.1:9944/: 1000:: Normal Closure` — harmless, but alarming in test output.
10. **30-min launch stall, unexplained** (§5, calculator) — included for completeness; no stack-side cause found.

## 8. What was NOT verified

- Preview/Preprod networks were not exercised (task scope is the local environment; endpoints are in `toolchain.json` → `networks` and were not probed here).
- No mainnet, no wallet signing beyond the local devnet's repo-generated pre-funded test wallets, no API keys or sign-ups.
- The first-boot node crash root cause (no deeper error in logs; did not reproduce).
- Proof times under concurrent/sustained load (single sequential calls only).
- The other ten examples were not run locally (the task asked for one full E2E; two suites were run for confidence).

## 9. For Muse

1. The first-boot node crash (§3) is a candidate for an upstream issue — full node log captured; say the word and I'll file it.
2. Items 1–3 in §7 are docs quickstart drift — candidate for a docs issue or a Version Doctor spec input.
3. Item 8 in §7 (SRS phone-home on first use) might deserve an explicit note in the local-network docs.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
