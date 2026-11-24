# Midnight Proof Station

I’m building a Windows desktop console for a **Midnight Preview testnet** full node, indexer, and local proving. This is an experimental build for developers. The CUDA lab runs real zero-knowledge proofs on synthetic inputs. The released wallet prover handles Preview transactions.

## Run on Windows

1. Extract the **midnight-proof-station-win-x64** build linked from the **Proof Station** workflow. Keep the executable, Compose files, and shell script together, then open `MidnightProofStation.exe`. The build includes the .NET Desktop Runtime.
2. Start Docker Desktop in Linux container mode using its WSL2 backend. **Start wallet prover** starts the stock proof server at `http://localhost:6300`. Point Lace’s Preview wallet to its local proof server there.
3. For the full node and indexer, open **Cardano Preview database** and paste the PostgreSQL URI of a synced **Cardano Preview** db-sync instance reachable by the Docker engine. For a database on the Windows host, use `host.docker.internal` in the URI. Select its PEM CA certificate to verify the database host’s TLS certificate. **Start Preview stack** starts the full node and waits for block 1 before starting the indexer.
4. Watch the real block height, peer count, and indexed height. **Stop services** stops this app’s containers and preserves the chain and index databases. Services can continue running after the window closes.

The GUI masks the database URI and does not write it to disk. Docker retains it in the local node container configuration, which users with Docker access can inspect. Use a dedicated Preview database login. All published service ports bind to loopback.

| Service | Endpoint |
| --- | --- |
| Preview full node RPC | `http://127.0.0.1:9944` or `ws://127.0.0.1:9944` |
| GraphQL indexer | `http://127.0.0.1:8088/api/v4/graphql` |
| Wallet prover | `http://127.0.0.1:6300` |

The node uses archival pruning settings so the indexer can backfill from genesis. Cardano db-sync and the archive take disk space and time to sync. A testnet wallet that submits later needs tNIGHT registered for DUST generation and spendable DUST. The desktop app does not access wallet keys or submit transactions. [Full node guide](https://docs.midnight.network/nodes/full-node) · [DUST guide](https://docs.midnight.network/guides/manage-dust)

## Use the CUDA lab

On Windows, install the NVIDIA driver with WSL support. In the default WSL2 Linux distribution, have Git, a C++ toolchain, Rust via rustup, and a CUDA toolkit with `nvcc`. Launch Proof Station, set the NVIDIA device index, and click **Detect GPU**. A 3090 should report `sm_86`. **Build CUDA backend** fetches the exact draft revision and compiles the CPU verifier and CUDA prover for the detected architecture. It can take several minutes. Build files stay in the distro’s user data directory. [NVIDIA compute capabilities](https://developer.nvidia.com/cuda/gpus) · [Microsoft CUDA on WSL](https://learn.microsoft.com/windows/ai/directml/gpu-cuda-in-wsl)

**Generate sample proofs** builds a synthetic circuit with test-only parameters and runs one cold and one warm proof on the selected device. The stock CPU verifier checks both. The dashboard shows proof timings, device calls, CPU fallback counts, and peak tracked device memory after both proofs verify and both record actual CUDA calls without device errors. Close GPU-heavy workloads when the 3090 has little free VRAM.

The lab uses [Charles Hoskinson’s draft PR #667](https://github.com/midnightntwrk/midnight-zk/pull/667) at [`6bcc6a5`](https://github.com/CharlesHoskinson/midnight-zk/tree/6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8). The draft’s `midnight-proofs 0.9.0-rc.1` path is experimental. The older Compact CUDA backport described upstream is outside that PR. Wallet transaction proofs use the official `proof-server:9.0.2` image. A wallet-compatible CUDA release will need fresh end-to-end checks against the Ledger 9 proof API, stock verification, and a confirmed Preview transaction before we enable it. Keep real wallet and circuit witnesses on the operator’s machine. [Draft benchmark notes](https://github.com/CharlesHoskinson/midnight-zk/blob/6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8/docs/gpu-prover/benchmarks.md) · [Local proving and witness privacy](https://docs.midnight.network/guides/local-proving)

## Build and verify

On Windows with the .NET 10 SDK, start from the repo root.

```powershell
dotnet run --project apps/proof-station/MidnightProofStation.csproj
```

The [Preview compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) was checked on 2026-11-23. The Compose stack pins node **2.1.101**, indexer **4.4.101**, and proof server **9.0.2**. The matching dApp tools are Compact **0.36.1**, Midnight.js **5.0.1**, and Wallet SDK **2.0.1**.

The **Proof Station** workflow verifies the proof result parser, renders a screenshot of the Windows app, and packages a self-contained Windows x64 build. It runs the released wallet prover and checks that it offers the Ledger 9 proof version. Verifying Preview sync requires a real Cardano Preview db-sync, and verifying CUDA execution requires an NVIDIA device on Windows/WSL2.

## Support

- Cardano (ADA) · `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`
- Midnight (shielded) · `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000)

Independent builder — not affiliated with the Midnight Foundation or IOG.
