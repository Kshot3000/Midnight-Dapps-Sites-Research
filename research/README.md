# Research reading guide

This folder holds nine sourced research briefs. Each row below lists the question the brief answers, the date it was checked (as written in the brief), the networks it covers, and the status labels it uses. Open the brief itself for the sources and evidence.

**Network labels:** Preview and Preprod are public **testnets**. Mainnet is production. Stagenet is a permissioned test network. `undeployed` is the local Docker devnet, not a public chain. A brief that never touched a chain says so.

**Stack:** every network runs ledger 9. Current pins are in [`stack-versions.md`](stack-versions.md): Compact toolchain 0.36.1, Midnight.js 5.0.1, Wallet SDK 2.0.1 and proof server 9.0.2. Treat Ledger 8-era docs and tooling as stale reference, never as current.

## Briefs

| Brief | Question it answers | Checked | Networks | Status labels used |
| --- | --- | --- | --- | --- |
| [`stack-versions.md`](stack-versions.md) (HERMES-1) | Which versions should a Midnight build pin today, and which official docs pages still show stale versions? | 2026-11-23 | Mainnet, Preview and Preprod (testnet), all on ledger 9, read from the support matrix. `undeployed` local devnet is mentioned for local work. | Version rows marked *Verified* by source (matrix + npm, matrix + GitHub). Docs pages sorted into **A. Current**, **B. Labelled ledger-8 era** (reference only) and **C. Unlabelled stale**. Has a "What I did NOT verify" list. |
| [`example-patterns.md`](example-patterns.md) (HERMES-2) | What privacy pattern does each of the 12 official `midnight-examples` dApps use, and which patterns does no example cover? | 2026-11-24 (source read at `main` 953f864) | No public network. Examples target the local devnet (`undeployed`) or a simulator, not Preview or Preprod. | Links marked *verified live* (fetched or HTTP 200). Separate gap list and "Not verified / for Muse" list. The `build-a-private-smart-contract` guide is flagged as carrying the ledger-8 banner. |
| [`local-stack-verification.md`](local-stack-verification.md) (HERMES-3) | Does the official local stack (node, indexer, proof server) actually run end to end, and how long does each step take? | 2026-11-24 | Local devnet only (`undeployed`). Preview and Preprod (testnet) were not exercised. | Test suites reported as passed with measured times (hello-world 2/2, calculator 6/6). A first-boot failure is labelled recovered, not reproduced. Has a "What was NOT verified" list. |
| [`compact-patterns-outline.md`](compact-patterns-outline.md) (HERMES-4) | What should a tested Compact pattern library contain, and where is each pattern shown in official code? | 2026-11-24 | No public network. Any runtime check is labelled local devnet. Preview and Preprod (testnet) only if a follow-up asks. | Pattern status **demonstrated / reference-only / invented (gap N)**. Sources marked *verified live*. Ledger-8 pages flagged **ledger-8-stale** or **ledger-8 banner**. Outline only, no pattern code. |
| [`dust-registration.md`](dust-registration.md) (HERMES-5) | What is DUST registration, how does it feed DUST generation, and what are the official ways to register, redirect or stop it? | 2026-11-24 | Preview, Preprod and local devnet (testnet). Mainnet parameters and the ledger 9 reset are read from official pages, not performed. | **MODEL** (DUST Planner modelled estimates), **LIVE** (read from official pages, not measured), **NOT live-verified**. Has an honesty list of what was not verified. |
| [`ecosystem-directory.md`](ecosystem-directory.md) (GROK-1, bridges gap updated by GROK-8) | Which Midnight dApps, wallets, explorers, faucets, indexers, tools and bridges can actually be opened today? | 2026-11-23. The bridges gap was corrected on 2026-11-24. | Mainnet, Preview and Preprod (testnet), Stagenet (permissioned test network) and `undeployed` (local). | Live (opened), testnet, announced, stale, dead and unverified. An entry is only marked live if it was opened. Guarded by [`scripts/check-directory-links.mjs`](../scripts/check-directory-links.mjs) and the `ecosystem-directory` and `directory-link-checker` tests. |
| [`bridges-status.md`](bridges-status.md) (GROK-6) | What ways exist to move value onto or off Midnight, and which of them really work? | 2026-11-24 | Mainnet, Preview and Preprod (testnet), and Stagenet (permissioned test network). | **Live per public evidence, not used by me**, **Announced only**, **Testnet / in development**, **Testnet demo, stale**, **Not a bridge**, **No Midnight support found**. Nothing is labelled "live (verified usable)" because nothing was signed. |
| [`catalog-partner-status.md`](catalog-partner-status.md) (GROK-7) | Which partner cards in the official ecosystem catalog are live Midnight products, and which are announcements only? | 2026-11-24 | Mainnet, Preview and Preprod (testnet), and Stagenet (permissioned test network). | **Live product**, **Live per public evidence, not used by me**, **Testnet demo**, **Opened, not independently verified**, **Announcement or partnership only**, **Opened empty or broken site**. Its corrections are suggestions only. |
| [`qa-sweep-2026-11.md`](qa-sweep-2026-11.md) (GROK-3) | Are this repo's links, live hub, accessibility, mobile layout, attribution and stack pins in order? | 2026-11-23. Link and live-site fetches also ran around 04:30 UTC on 2026-11-24. | No chain interaction. Repo and hub QA only. | Fixed (with fix PRs #11 and #12), left as a judgement call for Muse, and "What I did not verify". It records the test command used at the time (`node --test tests/hub.test.mjs`). The full suite is now `npm test`. |

## How to read the labels

- **Live** means someone opened it and has evidence. A marketing page that returns HTTP 200 is not a working product.
- **Testnet** means Preview or Preprod. Nothing on a testnet is described as mainnet.
- **Announced** means only a blog post, catalog card or partnership exists, with no product to point at.
- **Unverified** means the brief could not confirm it without signing, a wallet, an API key or funds. None of these briefs signed a transaction or moved funds.
- **Modelled estimate** means local calculation output, such as the DUST Planner. It is never shown as live data.

## Keeping this guide current

`tests/research-readme.test.mjs` fails if a `research/*.md` brief is missing from this table or if either address below is changed. Add a row here in the same PR as any new brief.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
