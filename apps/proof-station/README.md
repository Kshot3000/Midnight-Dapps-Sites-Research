# Midnight Proof Station

Midnight Proof Station **v0.2.0** is my Windows desktop console for a **Midnight Preview testnet** full node, indexer, and local proving. The Proof Lab generates zero-knowledge proofs on synthetic inputs with either the stock CPU prover or experimental NVIDIA CUDA. The stock CPU verifier checks both modes. The released CPU wallet prover handles Preview transactions.

## Run on Windows

1. Sign in to GitHub and open a successful build in the Proof Station workflow. Download the **midnight-proof-station-win-x64** ZIP from that run’s artifacts. Extract the whole ZIP and open `MidnightProofStation.exe` from the extracted folder. The portable build includes the .NET Desktop Runtime. [Proof Station builds](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/actions/workflows/proof-station.yml)
2. Start Docker Desktop in Linux container mode using its WSL2 backend. **Start wallet prover** starts the stock proof server at `http://localhost:6300`. Point Lace’s Preview wallet to its local proof server there.
3. For the full node and indexer, open **Cardano Preview database** and paste the PostgreSQL URI of a synced **Cardano Preview** db-sync instance reachable by the Docker engine. For a database on the Windows host, use `host.docker.internal` in the URI. Select its PEM CA certificate if the database uses a private CA. **Start Preview stack** starts the full node and waits for block 1 before starting the indexer.
4. Watch the real block height, peer count, and indexed height. **Stop services** stops this app’s containers and preserves the chain and index databases. Services can continue running after the window closes.

The GUI masks the database URI and does not write it to disk. Docker retains it in the local node container configuration, which users with Docker access can inspect. Use a dedicated Preview database login. All published service ports bind to loopback.

| Service | Endpoint |
| --- | --- |
| Preview full node RPC | `http://127.0.0.1:9944` or `ws://127.0.0.1:9944` |
| GraphQL indexer | `http://127.0.0.1:8088/api/v4/graphql` |
| Wallet prover | `http://127.0.0.1:6300` |

The node uses archival pruning settings so the indexer can backfill from genesis. Cardano db-sync and the archive take disk space and time to sync. A testnet wallet that submits later needs tNIGHT registered for DUST generation and spendable DUST. The desktop app does not access wallet keys or submit transactions. [Full node guide](https://docs.midnight.network/nodes/full-node) · [DUST guide](https://docs.midnight.network/guides/manage-dust)

## Use the Proof Lab

Install a default WSL2 Linux distribution with Git, a C/C++ toolchain, and Rust via rustup. The Proof Lab runs independently of Docker and the Preview stack. The first build downloads the pinned source and Rust dependencies. Build files stay in the distribution’s user data directory.

- **CPU** is selected at launch. Choose **Build CPU prover** once, then **Generate sample proofs**. The stock build works on machines without NVIDIA hardware or a CUDA toolkit.
- **NVIDIA CUDA** requires the Windows NVIDIA driver with WSL support and a CUDA toolkit with `nvcc` in the distribution. Select the NVIDIA device index, click **Detect GPU**, and choose **Build CUDA backend**. **Generate sample proofs** checks that the selected GPU has at least **3,072 MiB** free, then checks again just before the CUDA prover runs. If either check fails, free GPU memory and retry or select CPU. [NVIDIA compute capabilities](https://developer.nvidia.com/cuda/gpus) · [Microsoft CUDA on WSL](https://learn.microsoft.com/windows/ai/directml/gpu-cuda-in-wsl)

Each mode runs one cold and one warm proof of the same synthetic circuit at `k=16` with eight prover threads and test-only parameters. The stock CPU verifier checks both proofs. The CPU view shows cold and warm proof times plus the warm proof’s verification time. CUDA results also show device calls, CPU fallbacks, and peak tracked device memory. A CUDA job passes only if both proofs verify, both record device calls, and both finish without device errors.

The CUDA view displays Hermes’s recorded RTX 3090 (`sm_86`, driver `610.60`) run from November 24, 2026. Both proofs passed the stock CPU verifier. The cold proof took **1.168 s**, the warm proof **0.766 s**, and warm verification **about 1.6 ms**. The run recorded 38 completed CUDA MSM calls, zero fallbacks, and zero device errors. New local results appear in the Proof verification card. [Hermes’s measured run](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/4#issuecomment-7125334907)

My [RTX 3090 benchmark for Charles](https://github.com/Kshot3000/Midnight-Prover-Gpu-Tests-for-BIG-HOSSSS-/blob/50fe68f89404fe38e13566d603f70115d4287b2e/RESULTS.md) used the same pinned circuit code with a larger `k=21` workload and 16 threads.

The lab uses [Charles Hoskinson’s draft PR #667](https://github.com/midnightntwrk/midnight-zk/pull/667) at [`6bcc6a5`](https://github.com/CharlesHoskinson/midnight-zk/tree/6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8). The draft’s `midnight-proofs 0.9.0-rc.1` path is experimental. The older Compact CUDA backport described upstream is outside that PR. Wallet transaction proofs use the official `proof-server:9.0.2` image. A wallet-compatible CUDA release will need fresh end-to-end checks against the Ledger 9 proof API, stock verification, and a confirmed Preview transaction before we enable it. Keep real wallet and circuit witnesses on the operator’s machine. [Draft benchmark notes](https://github.com/CharlesHoskinson/midnight-zk/blob/6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8/docs/gpu-prover/benchmarks.md) · [Local proving and witness privacy](https://docs.midnight.network/guides/local-proving)

## Build and verify

On Windows with the .NET 10 SDK, start from the repo root.

```powershell
dotnet run --project apps/proof-station/MidnightProofStation.csproj
```

The Compose stack pins node **2.1.101**, indexer **4.4.101**, and proof server **9.0.2**. The matching dApp tools are Compact **0.36.1**, Midnight.js **5.0.1**, and Wallet SDK **2.0.1**. [Preview compatibility matrix](https://docs.midnight.network/relnotes/support-matrix)

The **Proof Station** workflow checks the CPU and CUDA result parser and exercises the CUDA memory gate with fixtures. It builds the stock prover and generates two verified CPU proofs on Linux. A Windows runner extracts the packaged ZIP into a path with spaces, launches the native app, and renders both proof modes. CI also starts the released wallet prover and checks that it offers the Ledger 9 proof version. A full Preview sync check needs a synced Cardano Preview db-sync, and a fresh CUDA run needs an NVIDIA device with Windows and WSL2.

## Support

- Cardano (ADA) · `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`
- Midnight (shielded) · `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000)

Thanks to the Midnight team for the protocol and Charles Hoskinson for the upstream GPU prover work. [@midnightntwrk](https://github.com/midnightntwrk) · [@CharlesHoskinson](https://github.com/CharlesHoskinson) · [@MidnightNtwrk](https://x.com/MidnightNtwrk) · [@IOHK_Charles](https://x.com/IOHK_Charles)

Independent builder — not affiliated with the Midnight Foundation or IOG.
