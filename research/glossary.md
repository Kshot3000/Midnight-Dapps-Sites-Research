# GROK-13 — Plain-language Midnight glossary

**Accessed 2026-11-25.** Every official source below was opened on 2026-11-25, between about 14:50 and 15:05 CT (America/Chicago, UTC-6).
**Written by:** Bob Research (Grok swarm), for the council board task GROK-13. Packet: `council/feed/GROK_FEED.md`, wave 5.
**Networks:** no chain was touched. Preview and Preprod are public **testnets** and are labelled testnet on every line where they appear. Mainnet is production. `undeployed` is the local Docker network, not a public chain.
**Stack:** every public network runs ledger 9. The pins this page uses are the verified matrix: Compact toolchain **0.36.1**, Midnight.js **5.0.1**, proof server **9.0.2**, DApp Connector API **4.1.0**, indexer **4.4.101** and Wallet SDK **2.0.1** (see [`stack-versions.md`](stack-versions.md)). Older or newer numbers appear only in the [stale-version flags](#stale-or-off-matrix-versions-seen-in-official-pages) and are never pins.

Our briefs use these words without stopping to define them. Each entry below gives a one- or two-sentence definition in plain words, at least one official source opened today, and the repo brief(s) that use the term. Where an official page and a brief say different things, the entry says so and the [Docs vs briefs disagreements](#docs-vs-briefs-disagreements) section gives both sides. A term is only here if a repo brief already uses it, so official terms such as Kachina are left out on purpose.

**Official sources** means pages on docs.midnight.network, repos in the `midnightntwrk` GitHub organization, the Compact repo at `LFDT-Minokawa`, and Lace's own repo (`input-output-hk/lace`) and site.

## Terms

### Addresses (`mn_addr`, `mn_shield-addr`, `mn_dust`)

- **In plain words:** a Midnight wallet is three wallets made from one seed, so it has three kinds of address: an unshielded address (`mn_addr…`) for NIGHT and other public tokens, a shielded address (`mn_shield-addr…`) for private tokens, and a DUST address (`mn_dust…`) that receives the DUST your NIGHT generates. On every network except Mainnet the prefix also names the network, for example `mn_addr_preprod1…` on Preprod (testnet) or `mn_addr_undeployed1…` locally.
- **Official source:** [Keys and addresses](https://docs.midnight.network/concepts/keys-and-addresses) (address and key formats table) · [Glossary: HRP](https://docs.midnight.network/glossary)
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) (DUST addresses, redirecting generation) · [`local-stack-verification.md`](local-stack-verification.md) (`mn_addr_undeployed1…` on the local network)
- **Note:** this repo's Midnight donation address starts `mn_shield-addr1`, which the table above shows is the Mainnet shielded format.

### Circuit

- **In plain words:** a circuit is a fixed program of additions and multiplications that a zero-knowledge proof is about. In Compact, each exported `circuit` is a contract entry point, and the compiler turns it into a ZK circuit, so calling it means proving you ran it correctly.
- **Official source:** [Glossary: Zero-knowledge (ZK) circuit](https://docs.midnight.network/glossary) · [The Compact language](https://docs.midnight.network/compact) ("The Compact compiler outputs zero-knowledge circuits")
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) · [`example-patterns.md`](example-patterns.md) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md) (circuit sizes `k`)

### cNIGHT (and mNIGHT)

- **In plain words:** cNIGHT is NIGHT that lives on Cardano, which is where most NIGHT is held today. Registering cNIGHT on Cardano makes it generate DUST on Midnight after about 12 hours, without moving it. mNIGHT is NIGHT on Midnight itself; the official cNIGHT→mNIGHT bridge ships with ledger 9 but is not switched on yet.
- **Official source:** [Tokens on Midnight](https://docs.midnight.network/tokens/overview) ("Most Mainnet NIGHT is still held on Cardano as cNIGHT"; the 12-hour delay "goes away once native mNIGHT launches") · [DUST architecture](https://docs.midnight.network/concepts/dust-architecture) ("The implementation": the `pallet_cnight_observation` flow)
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) (§7.4, cNIGHT held on Cardano) · [`bridges-status.md`](bridges-status.md) (cNIGHT→mNIGHT bridge is off) · [`ecosystem-directory.md`](ecosystem-directory.md)

### Compact

- **In plain words:** Compact is Midnight's smart-contract language, which looks like TypeScript. You compile it with the Compact toolchain (pin **0.36.1**, language version 0.28.0), and the output is ZK circuits plus JavaScript you call from your DApp.
- **Official source:** [The Compact language](https://docs.midnight.network/compact) · [Glossary: Compact](https://docs.midnight.network/glossary) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (toolchain 0.36.1) · [LFDT-Minokawa/compact](https://github.com/LFDT-Minokawa/compact) (where development lives now)
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) · [`stack-versions.md`](stack-versions.md) (note 2: the repo move) · [`example-patterns.md`](example-patterns.md)
- **Note:** [`midnightntwrk/compact`](https://github.com/midnightntwrk/compact) says in its README that it "is archived" and only hosts release artifacts, but GitHub does not flag the repo as archived (checked today). That matches what `stack-versions.md` note 2 says it does.

### DApp Connector API

- **In plain words:** the DApp Connector API is how a web page talks to a Midnight wallet such as Lace. The wallet puts an object under `window.midnight`, and the DApp uses it to connect, read balances and addresses, ask the wallet to balance and submit transactions, or hand proving to the wallet. Pin **4.1.0** (npm `@midnightntwrk/dapp-connector-api`).
- **Official source:** [midnightntwrk/midnight-dapp-connector-api](https://github.com/midnightntwrk/midnight-dapp-connector-api) (README and specification) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (4.1.0) · [DApp connector API reference](https://docs.midnight.network/api-reference/dapp-connector) (see the stale-version flag below)
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) (Lace injects it under a fresh UUID key) · [`android-prover-feasibility.md`](android-prover-feasibility.md) (the wallet proving provider) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md)

### `disclose()` and selective disclosure

- **In plain words:** in Compact, data from a witness stays private unless you wrap it in `disclose()` before it reaches the public ledger, and the compiler refuses code that would leak it silently. Selective disclosure is the wider idea: prove one fact, such as "over 18", without revealing the data behind it.
- **Official source:** [Glossary: Selective disclosure](https://docs.midnight.network/glossary) · [Manage private state with witnesses](https://docs.midnight.network/guides/private-state-witnesses) (the stored hash is wrapped in `disclose()`)
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) (pattern B1; the compiler error was reproduced there) · [`example-patterns.md`](example-patterns.md) · [`ecosystem-directory.md`](ecosystem-directory.md)

### DUST

- **In plain words:** DUST is what pays transaction fees on Midnight. It is shielded, you cannot send it to anyone, and it is not stored as a balance: your registered NIGHT generates it over time up to a cap of 5 DUST per NIGHT (about a week to fill), and it decays after that NIGHT is spent. 1 DUST = 10^15 SPECK.
- **Official source:** [Tokens on Midnight](https://docs.midnight.network/tokens/overview) ("a shielded, non-transferable fee resource (not a token)") · [DUST architecture](https://docs.midnight.network/concepts/dust-architecture) · [Managing DUST](https://docs.midnight.network/guides/manage-dust)
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) · [`local-stack-verification.md`](local-stack-verification.md) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md)
- **Docs wording differs between pages:** the official glossary calls tDUST "Test DUST tokens", while Tokens on Midnight says DUST is "not a token". The Managing DUST guide explains the gap: "Other pages and tools call DUST a token … This guide calls DUST a resource, because a wallet cannot send it to anyone." Our briefs follow the "resource, not a token" wording. See [disagreement 4](#4-is-dust-a-token-docs-disagree-with-each-other).

### DUST registration

- **In plain words:** holding NIGHT generates nothing until you register it. Registration is a transaction that links your NIGHT to a DUST address, so those NIGHT UTXOs start filling it with DUST. A first registration can pay its own fee out of the DUST the NIGHT would already have earned, although NIGHT received moments ago has earned almost nothing yet.
- **Official source:** [Funding a wallet](https://docs.midnight.network/guides/acquire-tokens) ("Holding NIGHT alone generates nothing; the registration transaction is what starts generation") · [DUST architecture](https://docs.midnight.network/concepts/dust-architecture) ("Registrations and fees")
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) · [`proof-station-qa.md`](proof-station-qa.md)

### Faucet

- **In plain words:** a faucet is a web page that gives out free test tokens. Midnight's faucets send tNIGHT on Preview (testnet) and Preprod (testnet), 5,000 tNIGHT per request, to an unshielded `mn_addr_…` address. There is no faucet on Mainnet, and the local `undeployed` network does not need one.
- **Official source:** [Environments and endpoints](https://docs.midnight.network/relnotes/network) (faucet UI per network) · [Funding a wallet](https://docs.midnight.network/guides/acquire-tokens) (5,000 tNIGHT; the faucet rejects shielded and DUST addresses) · [Glossary: Faucet](https://docs.midnight.network/glossary)
- **Repo briefs:** [`ecosystem-directory.md`](ecosystem-directory.md) (Faucets, testnet only) · [`dust-registration.md`](dust-registration.md)
- **Note:** the official pages give two faucet URLs for Preview (testnet). Environments and endpoints lists `midnight-tmnight-preview.nethermind.dev`, and the Ledger 9 upgrade page links `faucet.preview.midnight.network`. `ecosystem-directory.md` already lists both, so there is no conflict with our briefs. I did not request tokens from either one.

### Halo 2 (Midnight's proof system)

- **In plain words:** Halo 2 is the family of zero-knowledge proof systems that Midnight's prover grew out of. Midnight's version began as a fork of the PSE `halo2` library and is now its own PLONK-style zk-SNARK over the BLS12-381 curve.
- **Official source:** [Glossary: Halo 2](https://docs.midnight.network/glossary)
- **Repo briefs:** [`android-prover-feasibility.md`](android-prover-feasibility.md) (`midnight-proofs`, "the halo2 prover") · [`android-prover-landscape.md`](android-prover-landscape.md) (Mopro's Halo2 adapter)

### Indexer (Midnight Indexer)

- **In plain words:** the indexer follows the chain and stores what happened in a database you can query over GraphQL, so wallets and DApps don't have to read raw blocks. Pin **4.4.101**. The 4.3 line doesn't index ledger 9.
- **Official source:** [midnightntwrk/midnight-indexer](https://github.com/midnightntwrk/midnight-indexer) · [Glossary: Midnight Indexer](https://docs.midnight.network/glossary) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (4.4.101; "The 4.3 line doesn't index ledger 9")
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) · [`local-stack-verification.md`](local-stack-verification.md) (image tag vs binary banner) · [`proof-station-qa.md`](proof-station-qa.md)
- **Note:** the public Preprod (testnet) and Mainnet indexers are now served by Blockfrost, and every request needs a Blockfrost project token ([Environments and endpoints](https://docs.midnight.network/relnotes/network)). Getting that token means creating an account, so it is a step for Kyle and was not done here. The Preview (testnet) indexer keeps its Midnight-hosted URL.

### Lace

- **In plain words:** Lace is IOG's wallet for Cardano, Bitcoin and Midnight, available as a browser extension and a mobile app. On the Midnight test networks you use it to receive faucet tNIGHT and to register it with the **Generate tDUST** button.
- **Official source:** [input-output-hk/lace](https://github.com/input-output-hk/lace) ("A multi-chain, multi-platform wallet for Cardano, Bitcoin, and Midnight"; latest release `lace-extension@2.5.2`, 2026-11-23) · [lace.io](https://www.lace.io/) · [Funding a wallet](https://docs.midnight.network/guides/acquire-tokens) (the Lace procedure)
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) (Lace 2.5.2) · [`dust-registration.md`](dust-registration.md) (§7.1) · [`ecosystem-directory.md`](ecosystem-directory.md)

### Ledger 9

- **In plain words:** ledger 9 is the current version of Midnight's ledger rules. Mainnet moved to it on 6 November 2026, Preview (testnet) relaunched on it on 7 November, and Preprod (testnet) upgraded in place on 9 November. Ledger 8 transactions and tools (Midnight.js 4.x, Wallet SDK 1.x, toolchain 0.31.x for new contracts) no longer work on any public network.
- **Official source:** [Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) ("Known incompatible combinations")
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) · [`dust-registration.md`](dust-registration.md) (§9, the DUST reset) · [`bridges-status.md`](bridges-status.md)
- **Note:** the networks run ledger **9.1.0.1**, while the client packages from the 17 November patch compute with **9.1.0.2** (`@midnightntwrk/ledger-v9` 1.0.2). The upgrade page says this is expected. Both are ledger 9.

### Ledger (public state)

- **In plain words:** the ledger is the public record of every contract's state and every token. In Compact, a `ledger` declaration is the part of your contract's data that everyone can read on-chain, as opposed to private state.
- **Official source:** [Glossary: Ledger and Public state](https://docs.midnight.network/glossary) · [midnightntwrk/midnight-ledger](https://github.com/midnightntwrk/midnight-ledger) (the ledger implementation and its specification)
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) · [`example-patterns.md`](example-patterns.md)

### Midnight.js

- **In plain words:** Midnight.js is the TypeScript toolkit a DApp uses to deploy and call Compact contracts. It plugs together providers for the proof server, the indexer, private state and the wallet. Pin **5.0.1** (npm `@midnight-ntwrk/midnight-js`), as the matrix lists it.
- **Official source:** [midnightntwrk/midnight-js](https://github.com/midnightntwrk/midnight-js) ("TypeScript application development framework for the Midnight blockchain") · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (5.0.1)
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) (note 1: two npm scopes) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md)
- **Newer than the matrix, not adopted:** see [stale or off-matrix versions](#stale-or-off-matrix-versions-seen-in-official-pages). The repo's README and its latest GitHub release are ahead of the matrix today.

### NIGHT

- **In plain words:** NIGHT is Midnight's native token. You can transfer it, it is used for staking and governance, and registered NIGHT generates DUST. NIGHT is always unshielded (public), even at a shielded address. 1 NIGHT = 10^6 STAR.
- **Official source:** [Tokens on Midnight](https://docs.midnight.network/tokens/overview) ("NIGHT is always unshielded") · [Glossary: NIGHT](https://docs.midnight.network/glossary)
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) (§2, NIGHT vs DUST) · [`bridges-status.md`](bridges-status.md) · [`example-patterns.md`](example-patterns.md)

### Preprod (testnet)

- **In plain words:** Preprod (testnet) is the public test network for final checks before Mainnet. Its test tokens have no value. It runs ledger 9 and was upgraded in place, so its history and contracts carried over.
- **Preview vs Preprod (both testnet):** use Preview (testnet) for early experiments and Preprod (testnet) for final validation. Preprod (testnet)'s public indexer and RPC are served by Blockfrost and need a project token, while Preview (testnet) keeps Midnight-hosted endpoints.
- **Official source:** [Networks and environments](https://docs.midnight.network/guides/networks-and-environments) ("`preprod` is a public test network for final validation before mainnet") · [Environments and endpoints](https://docs.midnight.network/relnotes/network) · [Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9)
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) · [`bridges-status.md`](bridges-status.md) · [`ecosystem-directory.md`](ecosystem-directory.md)

### Preview (testnet)

- **In plain words:** Preview (testnet) is the public test network for early development, maintained by Midnight's core engineering team. Its test tokens have no value. It relaunched as a new chain on 7 November 2026, so anything deployed there before that is gone.
- **Official source:** [Networks and environments](https://docs.midnight.network/guides/networks-and-environments) ("`preview` is a public test network for early development and experimentation") · [Environments and endpoints](https://docs.midnight.network/relnotes/network) · [Ledger 9 upgrade](https://docs.midnight.network/relnotes/ledger-9) ("Preview reset", the Preview testnet relaunch)
- **Repo briefs:** [`proof-station-qa.md`](proof-station-qa.md) (Proof Station's only network, Preview testnet) · [`ecosystem-directory.md`](ecosystem-directory.md) · [`bridges-status.md`](bridges-status.md)

### Private state

- **In plain words:** private state is the part of a contract's data that stays on the user's own device and is never published. Witnesses read it while a proof is built, and the network only sees the proof.
- **Official source:** [Glossary: Private state](https://docs.midnight.network/glossary) · [Manage private state with witnesses](https://docs.midnight.network/guides/private-state-witnesses)
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) · [`example-patterns.md`](example-patterns.md)

### Proof server

- **In plain words:** the proof server is a program you run on your own computer (Docker image `midnightntwrk/proof-server`, pin **9.0.2**, port 6300) that turns a transaction's private inputs into the zero-knowledge proofs the network checks. It sees your private data, including the keys for a shielded or DUST spend. The docs therefore say to keep it on your own machine and publish its port on `127.0.0.1` only, because it has no authentication and no TLS.
- **Official source:** [Run the proof server](https://docs.midnight.network/guides/run-proof-server) ("Run the proof server on your own machine") · [Proving transactions locally](https://docs.midnight.network/guides/local-proving) ("What a proof server receives"; this page is labelled ledger-8 era, see the flags below) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (9.0.2)
- **Repo briefs:** [`local-stack-verification.md`](local-stack-verification.md) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md) · [`proof-station-qa.md`](proof-station-qa.md) · [`android-prover-feasibility.md`](android-prover-feasibility.md)

### Public parameters (SRS)

- **In plain words:** public parameters are large shared files that every prover needs, one set per circuit size `k`, plus the keys for the built-in shielded and DUST circuits. The proof server downloads them from Midnight's parameter host at startup and the first time it proves a new size. They are public and the same for everyone, and your proof requests are not part of that download.
- **Official source:** [Run the proof server](https://docs.midnight.network/guides/run-proof-server) ("It contacts one outside host, https://srs.midnight.network/")
- **Repo briefs:** [`wallet-prover-e2e.md`](wallet-prover-e2e.md) · [`local-stack-verification.md`](local-stack-verification.md) · [`android-prover-feasibility.md`](android-prover-feasibility.md)
- **Corrected disagreement:** two briefs once named the host `srs.midnight.net`; both now say `srs.midnight.network`, as the docs do. See [disagreement 1](#1-the-parameter-host-is-srsmidnightnetwork-not-srsmidnightnet).

### Shielded vs unshielded

- **In plain words:** a shielded token is private, so the chain records a commitment but not the owner, amount or token type. An unshielded token is public, like a normal blockchain balance. Privacy comes from the token's type, not from the address: NIGHT is always unshielded, DUST is always shielded, and nothing converts a token from one kind to the other.
- **Official source:** [Tokens on Midnight](https://docs.midnight.network/tokens/overview) ("Shielded and unshielded") · [Keys and addresses](https://docs.midnight.network/concepts/keys-and-addresses) (the shielded address) · [Glossary: Shielded and Unshielded](https://docs.midnight.network/glossary)
- **Repo briefs:** [`example-patterns.md`](example-patterns.md) (token-transfers covers both kinds) · [`compact-patterns-outline.md`](compact-patterns-outline.md) · [`dust-registration.md`](dust-registration.md)

### Stagenet

- **In plain words:** Stagenet is a separate Midnight test network that rehearses changes before Mainnet. Official pages say it runs a ledger 9 release candidate and rehearses the Mainnet procedure for switching on the cNIGHT→mNIGHT bridge. It is neither Preview nor Preprod (testnet).
- **Official source:** [Identity on Midnight](https://docs.midnight.network/concepts/identity) ("Stagenet, a separate Midnight test network that runs a ledger 9 release candidate") · [midnight-node: enabling the C-to-M bridge](https://github.com/midnightntwrk/midnight-node/blob/main/docs/c-to-m-bridge-enabling.md) ("testnets like **Stagenet** that deliberately rehearse the mainnet procedure")
- **Repo briefs:** [`bridges-status.md`](bridges-status.md) · [`ecosystem-directory.md`](ecosystem-directory.md) · [`catalog-partner-status.md`](catalog-partner-status.md)
- **Unverified wording:** our briefs and the research guide call Stagenet "a permissioned test network". Neither official page I opened today uses the word "permissioned". That isn't a contradiction, but it isn't confirmed either.

### tNIGHT and tDUST

- **In plain words:** tNIGHT and tDUST are the names wallets and faucets show for NIGHT and DUST on the test networks, Preview (testnet) and Preprod (testnet). They have no value, but the types, units and API are exactly the same as NIGHT and DUST.
- **Official source:** [Managing DUST](https://docs.midnight.network/guides/manage-dust) ("tDUST and tNIGHT are display names") · [Glossary: tNIGHT and tDUST](https://docs.midnight.network/glossary)
- **Repo briefs:** [`dust-registration.md`](dust-registration.md) (§2 and §7.1) · [`ecosystem-directory.md`](ecosystem-directory.md) (faucets) · [`bridges-status.md`](bridges-status.md)

### `undeployed` (local network)

- **In plain words:** `undeployed` is the network ID of the local development chain: a node, indexer and proof server running in Docker on your own computer, with a pre-funded genesis wallet. It is not a public chain, so nothing on it is testnet or mainnet.
- **Official source:** [Networks and environments](https://docs.midnight.network/guides/networks-and-environments) ("`undeployed` is the local development network") · [midnightntwrk/midnight-examples](https://github.com/midnightntwrk/midnight-examples) (`yarn env:up` starts it) · [midnightntwrk/midnight-local-dev](https://github.com/midnightntwrk/midnight-local-dev) (archived; points to midnight-examples)
- **Repo briefs:** [`local-stack-verification.md`](local-stack-verification.md) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md) · [`stack-versions.md`](stack-versions.md)
- **Corrected disagreement:** `stack-versions.md` used to recommend the archived `midnight-local-dev`; it now points at `midnight-examples`. See [disagreement 3](#3-midnight-local-dev-is-archived-but-stack-versionsmd-still-recommends-it).

### Wallet SDK

- **In plain words:** the Wallet SDK is the official TypeScript library for building wallet features. It derives keys and addresses from a seed, builds, balances and submits transactions, syncs with the indexer and registers NIGHT for DUST. Pin **2.0.1** (npm `@midnightntwrk/wallet-sdk`). Wallet SDK 1.x can't transact on ledger 9.
- **Official source:** [midnightntwrk/midnight-wallet](https://github.com/midnightntwrk/midnight-wallet) (README and [specification](https://github.com/midnightntwrk/midnight-wallet/blob/main/docs/spec/Specification.md)) · [Compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) (2.0.1) · [Keys and addresses](https://docs.midnight.network/concepts/keys-and-addresses) ("match Wallet SDK 2.0.1")
- **Repo briefs:** [`stack-versions.md`](stack-versions.md) · [`dust-registration.md`](dust-registration.md) (§7.2 and the HD-path roles) · [`wallet-prover-e2e.md`](wallet-prover-e2e.md)
- **Corrected disagreement:** `dust-registration.md` took role 4 ("Metadata") from an archived spec; it now gives role 4 as the unshielded ECDSA key. See [disagreement 2](#2-hd-wallet-role-4-is-ecdsa-not-metadata).

### Witness

- **In plain words:** a witness is a function you declare in Compact and write in TypeScript. It hands private data, usually from private state, to a circuit while the proof is built on your machine. The network checks the proof but never sees the witness value, and because the circuit can't trust that value, it has to `assert` whatever it relies on.
- **Official source:** [Manage private state with witnesses](https://docs.midnight.network/guides/private-state-witnesses) ("Witness: A function, declared in Compact and implemented in TypeScript…") · [Glossary: Witness](https://docs.midnight.network/glossary)
- **Repo briefs:** [`compact-patterns-outline.md`](compact-patterns-outline.md) (patterns B2, D2; "`ownPublicKey()` is a prover-claimed witness value") · [`example-patterns.md`](example-patterns.md) · [`android-prover-feasibility.md`](android-prover-feasibility.md)

### Zero-knowledge proof (ZK proof, zk-SNARK)

- **In plain words:** a zero-knowledge proof convinces someone that a statement is true without showing them the secret behind it. A zk-SNARK is the kind Midnight uses: the proof is small, it is quick to check, and it needs no back-and-forth between prover and verifier.
- **Official source:** [Glossary: Zero-knowledge proof (ZKP) and ZK Snarks](https://docs.midnight.network/glossary)
- **Repo briefs:** [`android-prover-landscape.md`](android-prover-landscape.md) · [`android-prover-feasibility.md`](android-prover-feasibility.md) · [`dust-registration.md`](dust-registration.md)

### ZSwap

- **In plain words:** ZSwap (written "Zswap" in the docs) is Midnight's scheme for shielded tokens. Built on Zerocash/Zcash ideas, it keeps sender, receiver and amount private, supports many token types, and lets two people's offers merge into one all-or-nothing swap. It reveals nullifiers (proof a coin was spent) but not which coin or whose.
- **Official source:** [Zswap](https://docs.midnight.network/concepts/zswap) · [Glossary: Zswap](https://docs.midnight.network/glossary)
- **Repo briefs:** [`example-patterns.md`](example-patterns.md) ("Zswap reveals nullifiers, not coins or owners") · [`android-prover-landscape.md`](android-prover-landscape.md) (ZSwap spend and output circuit sizes) · [`local-stack-verification.md`](local-stack-verification.md)
- **Docs wording note:** the glossary says Zswap "combines the strengths of Zcash and SwapCT", while the Zswap concept page says it is "Based on Zerocash concepts" and draws on Zcash Sapling. Both are official. No brief contradicts either one.

## Docs vs briefs disagreements

These started as flags only: when this glossary merged (#66), each brief belonged to its author's seat. After Kshot widened edit permission on 2026-11-25, Kyle assigned items 1 to 3 to a follow-up PR, GROK-13-fixup, which corrected them in the briefs with dated correction notes. Items 4 and 5 stay as flags. The text of each item below is kept as the original record.

### 1. The parameter host is `srs.midnight.network`, not `srs.midnight.net`

- **Briefs:** [`local-stack-verification.md`](local-stack-verification.md) (HERMES-3, §1 and lines 90 and 135) says the proof server downloads keys and parameters "from `srs.midnight.net`". [`proof-station-qa.md`](proof-station-qa.md) (GROK-10, summary item 4 and fix 10) repeats `srs.midnight.net`, including in its proposed README wording.
- **Official docs:** [Run the proof server](https://docs.midnight.network/guides/run-proof-server) says "It contacts one outside host, https://srs.midnight.network/". [Proving transactions locally](https://docs.midnight.network/guides/local-proving) lists the default `MIDNIGHT_PARAM_SOURCE` as `https://srs.midnight.network/`.
- **Other briefs agree with the docs:** [`wallet-prover-e2e.md`](wallet-prover-e2e.md), [`android-prover-feasibility.md`](android-prover-feasibility.md) and GROK-12's captured logs in `research/evidence/grok-12/` all show `srs.midnight.network`.
- **What I checked today:** from the box, `https://srs.midnight.network/` answered (HTTP 403 on its bare root, so the host is up), and `https://srs.midnight.net/` gave no HTTP response within 20 s. I did not run a proof server, so I can't say which host HERMES-3's run actually contacted. The `.net` spelling looks like a slip, but that is not proven.
- **Suggested fix:** change `srs.midnight.net` to `srs.midnight.network` in both briefs, and in GROK-10's proposed fix 10 wording before anyone applies it to the Proof Station README.
- **Resolved 2026-11-25 by GROK-13-fixup (Bob Research):** GROK-10-srs-host (#67) had already corrected `proof-station-qa.md`. GROK-13-fixup corrected all three HERMES-3 mentions in `local-stack-verification.md` (§1, the line-90 excerpt and §7 item 8) and added a dated correction note there. Before editing the excerpt, it looked for the raw log: none is in the repo (`research/evidence/` holds only GROK-12 files, and the brief arrived whole in commit `32ab0b2` with no log), the excerpt is hand-condensed, and the host sat in the author's parenthetical, not a log field. On 2026-11-25 `srs.midnight.network` completed TLS (HTTP 403 at the root) and `srs.midnight.net` failed the TLS handshake.

### 2. HD-wallet role 4 is ECDSA, not "Metadata"

- **Brief:** [`dust-registration.md`](dust-registration.md) (HERMES-5, §7) gives the key-derivation role table "per the spec" as "… **3** = Shielded (Zswap), **4** = Metadata". It cites the WalletEngine specification in `midnightntwrk/midnight-architecture`.
- **Official docs and current spec:** [Keys and addresses](https://docs.midnight.network/concepts/keys-and-addresses), written against Wallet SDK 2.0.1, lists role `4` as `Roles.EcdsaUnshielded`, "The unshielded signing key when the wallet signs with ECDSA instead of Schnorr". The Wallet SDK's own [specification](https://github.com/midnightntwrk/midnight-wallet/blob/main/docs/spec/Specification.md) says "Unshielded (and Night) ECDSA | 4", and `packages/hd/src/HDWallet.ts` defines `EcdsaUnshielded: 4`.
- **Why they differ:** the `midnight-architecture` repo is archived on GitHub (last push 2026-10-25), and its WalletEngine spec still shows "Metadata | 4". So the brief quoted the spec accurately, but that spec is out of date. Roles 0, 2 and 3 match in every source.
- **Suggested fix:** update the role-4 entry and cite the Wallet SDK spec or the Keys and addresses page.
- **Resolved 2026-11-25 by GROK-13-fixup (Bob Research):** `dust-registration.md` §10 now gives role 4 as the unshielded ECDSA key (`Roles.EcdsaUnshielded`), cites the Keys and addresses page, the Wallet SDK spec and `packages/hd/src/HDWallet.ts` at tag `@midnightntwrk/wallet-sdk@2.0.1`, and keeps a note that the archived spec said Metadata. The §7.2 currency note was corrected the same way.

### 3. `midnight-local-dev` is archived, but `stack-versions.md` still recommends it

- **Brief:** [`stack-versions.md`](stack-versions.md) (HERMES-1, note 4 and "For Sol") says "Local network: `midnight-local-dev` (`undeployed`)".
- **Official repo:** [midnightntwrk/midnight-local-dev](https://github.com/midnightntwrk/midnight-local-dev) is archived on GitHub, and its README says: "This repository is archived and no longer maintained … midnight-examples now does both: `yarn env:up` starts a local network for any example, and `yarn fund:wallet` funds a browser wallet such as Lace with DUST."
- **Another brief already has this right:** [`ecosystem-directory.md`](ecosystem-directory.md) marks it archived, and HERMES-3 and GROK-12 used `midnight-examples`. The official [Networks and environments](https://docs.midnight.network/guides/networks-and-environments) guide still links midnight-local-dev as "the local network tool", so the docs disagree with themselves as well.
- **Suggested fix:** point the `stack-versions.md` local-network line at `midnight-examples` (`yarn env:up`).
- **Resolved 2026-11-25 by GROK-13-fixup (Bob Research):** `stack-versions.md` note 4 and "For Sol" now point at [`midnight-examples`](https://github.com/midnightntwrk/midnight-examples), with a dated note that `midnight-local-dev` is archived. They use `npm run env:up`, not `yarn env:up`: midnight-examples ships `package-lock.json` and no `yarn.lock`, and its own README and every example's `package.json` use npm.

### 4. Is DUST a token? (docs disagree with each other)

- **Official pages:** [Tokens on Midnight](https://docs.midnight.network/tokens/overview) says DUST is "a shielded, non-transferable fee resource (not a token)". The [glossary](https://docs.midnight.network/glossary) defines tDUST as "Test DUST tokens", and [Managing DUST](https://docs.midnight.network/guides/manage-dust) acknowledges that "Other pages and tools call DUST a token".
- **Briefs:** [`dust-registration.md`](dust-registration.md) uses "not a token". No brief calls DUST a token.
- **What this page does:** it follows "resource, not a token", the stricter official wording. Nothing in our briefs needs to change.

### 5. Stagenet "permissioned" (unconfirmed)

- **Briefs:** [`bridges-status.md`](bridges-status.md) and the research guide call Stagenet "a permissioned test network".
- **Official pages opened today:** [Identity on Midnight](https://docs.midnight.network/concepts/identity) and the [midnight-node bridge doc](https://github.com/midnightntwrk/midnight-node/blob/main/docs/c-to-m-bridge-enabling.md) call it a separate test network but never say "permissioned". This is not a contradiction. I couldn't confirm the word, and I did not change it.

### Stale or off-matrix versions seen in official pages

These are flagged and not copied into any definition above. The pins stay Compact 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector API 4.1.0, indexer 4.4.101 and Wallet SDK 2.0.1.

- [DApp connector API reference](https://docs.midnight.network/api-reference/dapp-connector): its header still reads "@midnight-ntwrk/dapp-connector-api v4.0.1", and its examples import `@midnight-ntwrk/ledger-v8`. The matrix lists 4.1.0. HERMES-1 ([`stack-versions.md`](stack-versions.md), list C) flagged the header on 2026-11-23, and it is unchanged today.
- [Proving transactions locally](https://docs.midnight.network/guides/local-proving): "This guide describes proof server 8.1.3, Compact toolchain 0.31.1, Midnight.js 4.1.1, DApp Connector API 4.0.1, and Lace 2.4.2". The page labels these as ledger 8 versions and tells readers to run 9.0.2 / 0.36.1 / 5.0.1 instead, which matches `stack-versions.md` list B.
- [Networks and environments](https://docs.midnight.network/guides/networks-and-environments): its ledger 9 callout says "DApps on every network need Wallet SDK 2.0.0, Midnight.js 5.0.0". The matrix pins 2.0.1 and 5.0.1 and lists Midnight.js 5.0.0 and Wallet SDK 2.0.0 under known problems fixed by the 17 November patch. The page has no ledger-8 banner, so it reads as current. This is a new unlabelled stale item that `stack-versions.md` list C does not have yet.
- [midnightntwrk/midnight-js](https://github.com/midnightntwrk/midnight-js): the README now installs `@midnightntwrk/midnight-js` (no hyphen), whose npm `latest` and latest GitHub release are **5.0.3**, published 2026-11-24 16:44 UTC (10:44 CT). That is **newer** than the matrix. The matrix still lists `@midnight-ntwrk/midnight-js` 5.0.1, and npm `latest` for that scope is still 5.0.1. This matches `stack-versions.md` note 1 (which saw 5.0.2 then): keep 5.0.1 until the matrix moves.

## Honesty: what was verified and what wasn't

**Verified today (2026-11-25):**

- Every docs.midnight.network page linked above was opened today through a web fetch, and the quoted text was read on the live page. The box's own `curl` got HTTP 429 (rate limited) from docs.midnight.network on every request, so I also read the official docs source (`midnightntwrk/midnight-docs`, cloned today at `549bd62`, pushed 2026-11-25 14:08 UTC / 08:08 CT) to cross-check the wording and find the pages.
- Every github.com link above returned HTTP 200 to `curl` today. Repo status (archived or not), READMEs, the Wallet SDK role table and the npm `latest` tags were read with `gh api`, `git clone` and `npm view` today.
- Every repo-brief link resolves to a file in `research/` (the guard test checks this), and each term was checked by `grep` against those briefs before it was added.

**Not verified:**

- No chain, faucet, wallet, proof server or indexer was used. No transaction was signed, no tokens were requested, and no account or API key (such as a Blockfrost project token) was created.
- I did not check whether `srs.midnight.net` ever served parameters (disagreement 1, since corrected in the briefs by GROK-13-fixup). I only know it didn't answer from the box today.
- The midnight.network homepage also returned HTTP 429 to `curl`, so it is not cited.
- Definitions are paraphrases of the cited pages. If a page changes after 2026-11-25, its entry may go stale. Recheck against the [compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) first.
- Terms the official glossary defines but no brief uses (for example Kachina, ZKIR and Bech32m on their own) are left out on purpose, as the packet asks.

`tests/glossary.test.mjs` guards this page. It fails if an entry loses its official https source or its brief link, if a brief link breaks, if a line names Preview or Preprod without the testnet label, or if either address changes.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000)

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
