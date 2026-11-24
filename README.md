# Midnight Dapps, Sites & Research

I’m Kyle Cox. I build and publish open-source Midnight dApps, sites, and privacy research. The hub is my build queue and a record of what I can verify on the Preview and Preprod testnets.

## What I’m building

- **Selective disclosure.** Prove one claim, such as meeting an age threshold, while keeping the underlying credential private.
- **Private state with public verification.** Compact circuits use private inputs off-chain and the ledger verifies proofs against public inputs. Each design identifies its public outputs and ledger writes. [Compact privacy guide](https://docs.midnight.network/guides/build-a-private-smart-contract)
- **Shielded fees.** Registered NIGHT generates DUST. The shielded fee spend hides the DUST input. Fee amounts, public transaction data, and timing can still be visible. [DUST guide](https://docs.midnight.network/guides/manage-dust)

The [hub](sites/hub/) presents the ten projects in [the backlog](ideas/backlog.md). NightAttest, my credential dApp, is at the spec stage. I’ll label a dApp live on testnet once its deployment and proof flow can be independently verified.

The [Proof Station showcase](sites/hub/index.html#proof-station) includes the Windows build, CPU and NVIDIA CUDA screenshots, and verification results. [Proof Station setup guide](apps/proof-station/README.md)

[DUST Planner Pro](apps/dust-planner/) is a static modelled-estimate tool for DUST generation and decay. It does not connect to a wallet.


## Repository

| Directory | Contents |
| --- | --- |
| [sites/](sites/) | Websites, starting with the [static hub](sites/hub/) |
| [apps/](apps/) | dApp implementations, [Midnight Proof Station](apps/proof-station/), and [DUST Planner Pro](apps/dust-planner/) |
| [research/](research/) | Sourced research briefs |
| [ideas/](ideas/) | The [build backlog](ideas/backlog.md) |
| [council/](council/) | [Claims](council/board.md), specs, and [submission log](council/submissions.md) |

## Verified Midnight stack

Exact pins for **Preview and Preprod testnets**. I checked these on 2026-11-23 against Midnight’s [compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) and [ledger 9 upgrade notes](https://docs.midnight.network/relnotes/ledger-9). These pins apply to dApps. The hub is static HTML, CSS, and browser JavaScript.

| Component | Version |
| --- | --- |
| Compact devtools | `0.5.1` |
| Compact toolchain | `0.36.1` |
| Compact language | `0.28.0` |
| Compact runtime | `0.21.0` |
| Compact.js | `3.0.2` |
| Midnight.js | `5.0.1` |
| Wallet SDK | `2.0.1` |
| DApp Connector API | `4.1.0` |
| Proof server | `midnightntwrk/proof-server:9.0.2` |
| Indexer | `4.4.101` |
| Network ledger | `9.1.0.1` |

### dApp testnet checklist

1. Set Lace and the dApp Connector to the same Preview or Preprod testnet. Use testnet tNIGHT and synthetic data.
2. **Register tNIGHT for DUST generation** in the testnet wallet and confirm spendable DUST before submitting testnet transactions. [Registration and fee guide](https://docs.midnight.network/guides/manage-dust)
3. Run the pinned proof server locally with Docker. Select the local proof server in Lace’s Midnight settings. [Official setup](https://docs.midnight.network/getting-started/installation)

   ```sh
   docker run --rm -p 127.0.0.1:6300:6300 midnightntwrk/proof-server:9.0.2
   ```

4. The planned indexer setup is self-hosted for Preview and Blockfrost Midnight for Preprod. Blockfrost Preprod requires a project token. I’ll supply that separately if a Preprod build needs it. [Network endpoints](https://docs.midnight.network/relnotes/network)

## Preview and validate the hub

From the repo root, run the tests with Node.js 24. Serve the static files locally with Python 3, then open the preview URL.

```sh
node --test tests/hub.test.mjs
python3 -m http.server 4173 --bind 127.0.0.1 --directory sites/hub
```

[Open the local hub](http://127.0.0.1:4173/)

The tests check the repo layout, catalogue order and project labels, link targets, asset cache keys, and complete donation addresses on every authored page. Bump the `?v=` key in the HTML whenever the referenced CSS or JavaScript changes.

## Publish to GitHub Pages

I review and merge site changes through pull requests. To publish a reviewed version, set **Settings → Pages → Source** to **GitHub Actions**, then run **Hub checks and Pages → Run workflow** on `main`. The workflow tests and serves `sites/hub/` directly. Open the Pages URL from the deploy job and check the filters, both copy buttons, links, and layouts on desktop and mobile. [GitHub Pages setup](https://docs.github.com/en/pages/getting-started-with-github-pages/configuring-a-publishing-source-for-your-github-pages-site)

## Support

If you’d like to support my builds and research, these are my donation addresses.

- **Cardano (ADA)** · `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`
- **Midnight (shielded)** · `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`

Follow my work at [@kshot9000](https://x.com/kshot9000) or [github.com/Kshot3000](https://github.com/Kshot3000). Thanks to the Midnight team for the underlying protocol and tools — [@midnightntwrk](https://github.com/midnightntwrk) on GitHub, [@MidnightNtwrk](https://x.com/MidnightNtwrk) and [@IOHK_Charles](https://x.com/IOHK_Charles) on X.

Independent builder — not affiliated with the Midnight Foundation or IOG.
