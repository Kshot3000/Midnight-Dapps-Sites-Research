# GROK-12 — Wallet-built transaction → proof server 9.0.2, end to end (independent reproduction)

**Task:** GROK-12 · **Seat:** Victor (Grok swarm) · **Checked:** 2026-11-25 · **Network:** local `undeployed` devnet only (Docker on the Kshot box). Not testnet, not mainnet. Preview and Preprod (testnet) were not touched.

**Status: REPRODUCED.** This is an **independent reproduction** of Muse's 2026-11-24 run (board note at the top of `council/board.md`). It is not the first run. I ran the official `midnight-examples` hello-world local flow from a fresh upstream clone on a different machine. `test:local` passed **2/2**: the wallet-built deploy and `storeMessage` transactions were proven by proof server **9.0.2** and landed on the local chain.

Every number below comes from my own captured output in [`evidence/grok-12/`](evidence/grok-12/). Nothing is modelled or copied from another seat. Muse's numbers appear only in the comparison table, labelled as Muse's.

## 1. What was run

| Item | Value |
| --- | --- |
| Upstream source | `midnightntwrk/midnight-examples` `main` at `c37451a810313bb22015ded286c3718db95cee19` (commit date 2026-11-24), fresh shallow clone in a new box folder, `examples/hello-world` |
| Toolchain check | `npm run toolchain:check` → `✓ toolchain:check: 26 projects agree with toolchain.json` |
| Install | `npm install` at the repo root → `added 836 packages, and audited 861 packages in 21s` |
| Compact | compact CLI 0.5.3, compiler **0.36.1** (`contract-info.json`: `"compiler-version": "0.36.1"`, language 0.28.0, runtime 0.21.0). One circuit, `storeMessage`, prover and verifier keys generated |
| Midnight.js / Wallet SDK | `@midnight-ntwrk/midnight-js-contracts` **5.0.1**, `@midnightntwrk/wallet-sdk` **2.0.1**, `@midnightntwrk/ledger-v9` 1.0.2 (as installed in `node_modules`) |
| Node.js on the box | v24.22.1 |
| Docker | Engine 26.1.5, Compose v2.32.4, vfs storage driver |
| Commands | `npm run wait:dust`, then `npm run test:local`, with `MIDNIGHT_PROOF_SERVER=http://127.0.0.1:6301` |

### Images actually used

| Service | Tag | Repo digest | Image ID |
| --- | --- | --- | --- |
| proof server | `midnightntwrk/proof-server:9.0.2` | `sha256:787ef99db4b4706eb14d94ad389ca45f1c09bcdf740afecb01899bfbda63dc64` | `sha256:af59a9b5f6e6…` |
| indexer | `midnightntwrk/indexer-standalone:4.4.101` | `sha256:119f35db548ba1c390845f215bb1e928e73038bef7c315825aee0461bd54499d` | `sha256:86c73fcd22c7…` |
| node | `midnightntwrk/midnight-node:2.1.101` | `sha256:0c1ad548d523c1109aa1f16daab1bf72dcb78c7e17b8a8c6d97424ad0eba7b8a` | `sha256:bd5855d79575…` |

The node log reports `version 2.1.101-bbd616b1` and `Chain specification: undeployed1`.

### One deviation from the stock compose file (box networking)

On this box, Docker's user-defined bridge network did not carry traffic: a container on the compose network could not resolve `srs.midnight.network` and timed out reaching the node at `node:9944` and at its IP (run 1, below). I also found host port 6300 already held by another proof server on the box (a non-Docker process reporting `9.0.0`, not mine, left alone). So run 2 used [`evidence/grok-12/compose.hostnet.override.yml`](evidence/grok-12/compose.hostnet.override.yml) on top of the upstream `compose.yml`. It puts all three services on host networking, points the indexer at `ws://127.0.0.1:9944`, and runs the proof server as `midnight-proof-server --port 6301`. Images, versions and every other setting are upstream's.

## 2. Results (run 2: success)

Times are CST (America/Chicago, UTC-6). The proof-server and node logs use UTC, so 16:48 UTC is 10:48 CST.

**Client (`npm run test:local`, [`client-test-local.txt`](evidence/grok-12/client-test-local.txt)):**

- `wait:dust` → `Wallet synced.` then `DUST ready.` (exit 0)
- `Contract deployed at: 433c4b813a333c23009a19e7083c468e9319e11779dcae4af66a093b678a50e2`
- `✓ Deploys the contract 20774ms`
- `✓ Stores Hello World! 17075ms`
- `Tests  2 passed (2)`, `Duration  41.13s`

**Proof server 9.0.2 (server-side, from the container log, [`proof-server-log.txt`](evidence/grok-12/proof-server-log.txt)):**

| # | UTC time | Endpoint | Server-side time | Test window |
| --- | --- | --- | --- | --- |
| 1 | 16:48:18 | `POST /prove` | `0.736448s` | Deploys the contract |
| 2 | 16:48:38 | `POST /check` | `0.001258s` | Stores Hello World! |
| 3 | 16:48:38 | `POST /prove` | `0.578181s` | Stores Hello World! (the server fetched `k=6` public parameters during this request) |
| 4 | 16:48:39 | `POST /prove` | `0.577842s` | Stores Hello World! |

Each request is matched to a test by timestamp. The deploy test logged its address at 10:48:38.782 CST, before request 2 started.

- **HTTP status:** the proof server's access log in 9.0.2 prints `POST /prove HTTP/1.1; took …` with **no status code**, so I have no captured status. Both tests passed and asserted on-chain state after each proven transaction, so the requests worked, but that is an inference, not a captured 200.
- **Circuits and k:** the log never names a circuit. At startup the server fetched and verified public parameters for k=10 to k=15, plus the Zswap and Dust proving keys. During request 3 it fetched `k=6` on demand. The example README says `storeMessage` is `k=6, rows=36`. My compiler run (non-interactive shell) printed only `Compiling 1 circuits:`, without the k line, so the link between k=6 and `storeMessage` is consistent but not captured.
- **`/prove-tx`:** this flow (Midnight.js 5.0.1 `httpClientProofProvider`) made **no** `/prove-tx` calls. It used `/check` plus `/prove` only.

**The transaction landed ([`node-indexer-excerpt.txt`](evidence/grok-12/node-indexer-excerpt.txt)):** after the run, the indexer GraphQL `contractAction` query for the deployed address returned `"__typename":"ContractCall"`, transaction `1adc99242a7662a4a9f5f4e5da11632cff725e6527118f339b0cb4701954e35f`, at block `"height":14`. That is the `storeMessage` call on the local chain. The node log shows block `#14` imported at 16:48:42 UTC.

## 3. Run 1: failed, kept for the record

My first attempt used the stock compose bridge network, with only the proof server moved to host networking and port 6301. It failed in wallet sync: `Error: Wallet sync timeout after 600000ms`, with `Tests  2 skipped (2)`. The cause was in the indexer log: `chain-indexer exited with ERROR … cannot create reconnecting subxt RPC client: Connection timeout exceeded: 10s`. The indexer's healthcheck still reported healthy. Before that, the proof server on the bridge network exited with `Failed to fetch data from https://srs.midnight.network/bls_midnight_2p10 after 3 attempts`. This is a box/sandbox networking problem, not a Midnight bug. Details: [`run1-failure.txt`](evidence/grok-12/run1-failure.txt).

## 4. Comparison with Muse's run (different environment)

| | Muse, 2026-11-24 (board note) | Victor, 2026-11-25 (this brief) |
| --- | --- | --- |
| Environment | Muse cloud workspace, "sandbox CPUs (1.5 shared)" | Kshot box, 8 CPUs (proof server started 8 workers), Docker vfs, host networking |
| Stack | node 2.1.101 / indexer 4.4.101 / proof server 9.0.2 | same tags (digests above) |
| Compact | 0.36.1 | 0.36.1 |
| `test:local` | 2/2 passed | 2/2 passed |
| Contract address | `c9db791b…` | `433c4b81…` (a new deployment, so a different address is expected) |
| `/prove` (deploy) | 3.277 s | 0.736448 s |
| `/check` | 0.001 s | 0.001258 s |
| `/prove` | 1.321 s | 0.578181 s |
| `/prove` (storeMessage) | 2.661 s | 0.577842 s |

The flow reproduces: the same request sequence (one `/prove` during deploy, then `/check`, `/prove`, `/prove` during `storeMessage`), the same pass count, and a landed call. The timings differ because the machines differ. Neither column is a hardware benchmark: each is one run on one shared or sandboxed host, and it includes a one-time `k=6` parameter fetch.

## 5. What was NOT verified

- No captured HTTP status codes (the 9.0.2 access log omits them; I did not add a client-side HTTP trace).
- `POST /prove-tx` was never called. Only `/check` and `/prove` were exercised.
- Which circuit each `/prove` request proved, and its k, is not in the log. k=6 for `storeMessage` comes from the upstream README, not my compiler output.
- One run only. No repeat runs, no warm/cold split, no variance.
- No Preview or Preprod (testnet), and no mainnet. No real wallet: the only wallet was the example's local genesis dev wallet, and no seed is printed in the evidence.
- No browser or DApp Connector path. hello-world does not use the DApp Connector. Upstream `toolchain.json` pins `dapp-connector-api` **4.0.1**, while this repo's stack pin says 4.1.0. That drift is noted here, not tested.
- The stock compose networking was not shown to work on this box (run 1). Run 2 needed the host-network override.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000)

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
