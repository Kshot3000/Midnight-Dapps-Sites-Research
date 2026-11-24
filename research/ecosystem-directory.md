# GROK-1 — Midnight ecosystem directory

**Checked:** 2026-11-23 (America/Chicago)
**Checked by:** Grok swarm (GROK-1)
**Scope:** What I could actually open today — live dApps, games, explorers, wallets, developer tools, faucets, indexers, and bridges — plus honest status for announced, testnet, stale, or dead listings.

## Method and limits

I started from official Midnight docs, the [official ecosystem catalog](https://midnight.network/ecosystem-catalog), the [community projects](https://docs.midnight.network/sdks/community) and [community wallets](https://docs.midnight.network/sdks/community/wallets/community-wallets-overview) guides, and the official [awesome-dapps](https://github.com/midnightntwrk/midnight-awesome-dapps) list (repository last pushed 2026-11-18). I then opened each live URL listed below on 2026-11-23. An entry is not marked live unless I opened it. HTTP 200 on a marketing page is not the same as a working mainnet dApp.

I did **not** connect a wallet, sign a transaction, request faucet tokens, create an API key, or sign up for an account. Blockfrost Preprod and Mainnet endpoints returned HTTP 403 without a project token, which matches official docs; I did not obtain a token. Where a site is a single-page app with little server-rendered text, I say so.

**Network labels:** Preview and Preprod are public **testnets**. Mainnet is production. The local `undeployed` network is local Docker, not a public chain. Official docs also describe Stagenet as a permissioned test network used by the Midnight Passport demo — not Preview, Preprod, or Mainnet.

**Stack pins (do not use Ledger 8-era examples):** Official [compatibility matrix](https://docs.midnight.network/relnotes/support-matrix), opened 2026-11-23, matches this repo's `research/stack-versions.md`. Mainnet, Preview, and Preprod all run ledger 9 with Compact toolchain **0.36.1**, Midnight.js **5.0.1**, Wallet SDK **2.0.1**, DApp Connector API **4.1.0**, proof server **9.0.2**, and indexer **4.4.101**. npm `latest` for Midnight.js, both Wallet SDK package names, Compact JS, Compact runtime, and the DApp Connector API matched those pins on 2026-11-23. The Docker Hub tag `midnightntwrk/proof-server:9.0.2` exists (last updated 2026-11-16). One official guide page still mentions Wallet SDK 2.0.0 and Midnight.js 5.0.0 in passing; those are the pre-patch versions the matrix says to leave. The pins above are the ones to use.

## Official docs and catalogues

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Midnight docs | Official documentation for networks, SDKs, and guides | https://docs.midnight.network/ | n/a (docs) | n/a | Live. This box's direct HTTP client often received Vercel HTTP 429; the pages were opened through a separate fetch that returned the docs. | Official site | 2026-11-23 |
| Compatibility matrix | Current component versions for Preview, Preprod, and Mainnet | https://docs.midnight.network/relnotes/support-matrix | All three public networks, ledger 9 | n/a | Live. Source of the stack pins above. | Official docs | 2026-11-23 |
| Environments and endpoints | Per-network RPC, indexer, faucet, and explorer URLs | https://docs.midnight.network/relnotes/network | Mainnet, Preview (testnet), Preprod (testnet) | n/a | Live. Cross-check against the retired-endpoint section below before copying an old hostname. | Official docs | 2026-11-23 |
| Networks and environments guide | How to choose Preview, Preprod, Mainnet, or local `undeployed` | https://docs.midnight.network/guides/networks-and-environments | All of the above plus local | n/a | Live. States that `testnet-02` is retired. | Official docs | 2026-11-23 |
| Ecosystem catalog | Official partner and project directory | https://midnight.network/ecosystem-catalog | Mixed / often unspecified | Mixed | Live catalog page opened. Many cards are partners or announcements, not products I could open as Midnight dApps. A catalog listing alone is not treated as a live product in the sections below. | Official site | 2026-11-23 |
| Community projects | Official index of community-maintained tools (not Foundation-supported) | https://docs.midnight.network/sdks/community | n/a (docs) | n/a | Live. Lists OpenZeppelin Compact Contracts, Midnight Live View, Midnight Explorer, Nightforge, the Edda starter, and Midnames, with a compatibility disclaimer. | Official docs | 2026-11-23 |
| Community wallets overview | Official landscape of Lace plus community wallets | https://docs.midnight.network/sdks/community/wallets/community-wallets-overview | Mixed | Shielded tokens, selective disclosure, where noted | Live. The page itself is dated "Accurate as of June 2026" and tells readers to re-check the catalog and forum. Later product claims (for example Gero in the September 2026 official blog) are noted under Wallets. | Official docs | 2026-11-23 |
| Awesome Midnight dApps | Officially hosted, community-curated project list | https://github.com/midnightntwrk/midnight-awesome-dapps | Mixed | Mixed | Live repository. Last push 2026-11-18. Includes a `dormant_projects` folder. Repos on that list are not automatically live dApps. | GitHub (official org) | 2026-11-23 |
| Midnight Forum | Official technical forum | https://forum.midnight.network/ | n/a | n/a | Live (HTTP 200). | Official site | 2026-11-23 |
| This hub | Kyle Cox's public build queue and research record | https://kshot3000.github.io/Midnight-Dapps-Sites-Research/ | Preview / Preprod (testnet) when a dApp is later labelled live | n/a | Live (HTTP 200). NightAttest is still at spec in this repo; nothing here is labelled as a live testnet dApp yet. | This repository / GitHub Pages | 2026-11-23 |

## Explorers

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Midnight Explorer (Mainnet) | Blocks, transactions, and related on-chain views | https://midnightexplorer.com/ | Mainnet | Public explorer (shielded data stays unrevealed) | Live. Page title/heading: "Mainnet \| Midnight Explorer". Direct HTTP from this box was often 429; the page was opened successfully through a separate fetch. | Official endpoints doc | 2026-11-23 |
| Midnight Explorer (Preview) | Same explorer on the Preview testnet | https://preview.midnightexplorer.com/ | Preview (testnet) | Public explorer | Live. Heading: "Preview \| Midnight Explorer". | Official endpoints doc | 2026-11-23 |
| Midnight Explorer (Preprod) | Same explorer on the Preprod testnet | https://preprod.midnightexplorer.com/ | Preprod (testnet) | Public explorer | Live. Heading: "Preprod \| Midnight Explorer". | Official endpoints doc | 2026-11-23 |
| Subscan (Mainnet) | Third-party multi-chain explorer for Midnight Mainnet | https://midnight.subscan.io/ | Mainnet | Public explorer | Live (HTTP 200). | Official endpoints doc | 2026-11-23 |
| Subscan (Preview) | Subscan for the Preview testnet | https://midnight-preview.subscan.io/ | Preview (testnet) | Public explorer | Live (HTTP 200). | Official endpoints doc | 2026-11-23 |
| Subscan (Preprod) | Subscan for the Preprod testnet | https://midnight-preprod.subscan.io/ | Preprod (testnet) | Public explorer | Live (HTTP 200). | Official endpoints doc | 2026-11-23 |
| 1AM Explorer | Community explorer for blocks, transactions, addresses, and contracts | https://explorer.1am.xyz/ | Mainnet default; Preview and Preprod via `?network=` | Public explorer | Live (HTTP 200) at the default URL and at `?network=preview` and `?network=preprod`. I did not independently audit its index completeness. | Official endpoints doc; 1am.xyz | 2026-11-23 |

## Faucets (testnet only)

Mainnet has no faucet. I opened the pages below and did **not** request tokens.

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Preview faucet | Dispenses tNIGHT on the Preview testnet (rate limited; no real value) | https://faucet.preview.midnight.network/ | Preview (testnet) | none/public | Live page (HTTP 200, title "Midnight Faucet"). Mostly client-rendered. Token grant not verified. | Official networks guide | 2026-11-23 |
| Preprod faucet | Dispenses tNIGHT on the Preprod testnet (rate limited; no real value) | https://faucet.preprod.midnight.network/ | Preprod (testnet) | none/public | Live page (HTTP 200, title "Midnight Faucet"). Token grant not verified. | Listed alongside the official Preview faucet; also linked from community posts | 2026-11-23 |
| Nethermind Preview faucet | Alternate Preview tNIGHT faucet UI listed in official endpoint docs | https://midnight-tmnight-preview.nethermind.dev/ | Preview (testnet) | none/public | Flaky on this date: first GET returned HTTP 503; a later GET returned a "Midnight Faucet" page. Prefer the official `faucet.preview.midnight.network` URL until this host is stable. Token grant not verified. | Official endpoints doc | 2026-11-23 |
| Nethermind Preprod faucet | Alternate Preprod tNIGHT faucet UI listed in official endpoint docs | https://midnight-tmnight-preprod.nethermind.dev/ | Preprod (testnet) | none/public | Live page (HTTP 200, title "Midnight Faucet"). Token grant not verified. | Official endpoints doc | 2026-11-23 |
| Google Cloud Web3 faucet (Midnight path) | Blog-announced testnet faucet inside Google Cloud's Web3 portal | https://cloud.google.com/application/web3/faucet/midnight | Unverified (claimed testnet) | n/a | The URL returns HTTP 200 but the page I opened is a Google Cloud sign-in shell. I did not sign in, so I could not verify that it still dispenses tNIGHT. The September 2026 official Midnight blog said a Google Cloud testnet NIGHT faucet existed. Treat as unverified today. | Official Sept 2026 blog; URL opened | 2026-11-23 |

## Indexers, RPC, and the proof server

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Preview node RPC | Public JSON-RPC for the Preview testnet (no project token) | https://rpc.preview.midnight.network | Preview (testnet) | none/public | Live. A `system_chain` call returned `"Midnight Preview"`. GET without JSON-RPC returns HTTP 405, which is expected. | Official endpoints doc; live RPC probe | 2026-11-23 |
| Preview indexer | Public GraphQL indexer for the Preview testnet (no project token) | https://indexer.preview.midnight.network/api/v4/graphql | Preview (testnet) | Public index of public data | Live. A `{ __typename }` GraphQL POST returned a Query type. I did not run a full schema or history query. | Official endpoints doc; live GraphQL probe | 2026-11-23 |
| Blockfrost Midnight (Preprod / Mainnet) | Public indexer and RPC provider for Preprod and Mainnet; every request needs a project token | https://blockfrost.io/ and the `*.blockfrost.io` Midnight hosts in the endpoints doc | Preprod (testnet) and Mainnet | Public index / RPC | Provider site live (HTTP 200). Unauthenticated calls to `rpc.midnight-preprod.blockfrost.io`, `rpc.midnight-mainnet.blockfrost.io`, and the corresponding indexer hosts returned HTTP 403, which matches official docs. I did not create a project or use a token, so I did not verify query results. Official Midnight-hosted Preprod and Mainnet indexer hostnames are retired (see Dead or stale). | Official endpoints doc; live HTTP checks | 2026-11-23 |
| Proof server 9.0.2 | Local ZK proof generation. Official setup runs it on localhost:6300; it is not a public network service | https://hub.docker.com/r/midnightntwrk/proof-server/tags | Local, used with every public network | Handles private proving data off-chain | Docker tag `9.0.2` exists (updated 2026-11-16, amd64 and arm64). I did not start the image in this pass. | Compatibility matrix; Docker Hub API | 2026-11-23 |

## Wallets

I opened each wallet's public site or docs. I did not install an extension, create a wallet, or test a DApp Connector session. Connector behavior is therefore taken from official docs or vendor pages and labelled as such.

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Lace | IOG multi-chain wallet (Cardano, Bitcoin, Midnight). Midnight is in the Chrome/Edge extension, not the Lace mobile app, per official wallet docs | https://www.lace.io/ | Mainnet, Preview (testnet), Preprod (testnet) — as configured in the wallet | Shielded and unshielded Midnight transfers (privacy opt-in, not shielded-by-default, per official wallet docs) | Live site (HTTP 200). Official docs (as of the June 2026 wallet guide, still published today) describe a live DApp Connector and a required local proof server. I did not install Lace or run a proof server in this pass. | Official wallet docs; lace.io | 2026-11-23 |
| 1AM | Midnight-native self-custody wallet with browser extension and iOS/Android apps; shielded by default, in-browser proving | https://1am.xyz/ | Midnight plus Cardano support (site and official docs) | Shielded tokens (default), selective disclosure, unshielded lane also present | Live site (HTTP 200). Official wallet docs call the wallet live, with a v4 connector at `window.midnight['1am']`. The site itself labels some product visuals as previews or illustrative balances. I did not install the app or extension. | Official wallet docs; 1am.xyz | 2026-11-23 |
| urble | Brick Towers mobile app for holding, sending, and receiving shielded Midnight tokens, with planned shared savings | https://urble.io/ | Midnight (and the site also mentions Cardano, Bitcoin, and Ethereum) | Shielded tokens, selective disclosure (vendor and official wallet docs) | Live site (HTTP 200). Official wallet docs treat urble as a consumer app, not a DApp Connector target. I did not install the mobile app. | Official wallet docs; urble.io; catalog | 2026-11-23 |
| Kuira | Alpha Android wallet and embeddable SDK with on-device proving | https://kuiralabs.github.io/kuira-sdk-android/ | Midnight (alpha) | Shielded and unshielded (per official wallet matrix) | Live docs site (HTTP 200). Official docs mark it alpha (`v0.1.0-alpha02` in the June 2026 reference). I did not build an Android app or verify the Maven package. | Official wallet docs; Kuira docs | 2026-11-23 |
| Gero Wallet | Cardano / Midnight self-custody wallet | https://gerowallet.io/ | Cardano and Midnight, per vendor and the official Sept 2026 blog | Vendor claims include shielded balances and ZK features; not independently verified here | Live site (HTTP 200). The June 2026 official wallet guide was cautious (treat as a NIGHT claim wallet, connector not then documented). The official Midnight blog of September 2026 says Gero 2.7.1 added full Midnight dApp support, shielded mainnet balances, and a connector. I did not install Gero or test the connector. | gerowallet.io; official Sept 2026 blog; official wallet docs | 2026-11-23 |
| Ctrl | Multi-chain self-custody wallet | https://www.ctrl.xyz/ | Multi-chain; official wallet docs list Midnight as available | Not independently verified | Live site (HTTP 200). Official docs (June 2026) say to feature-detect its connector and treat it as vendor-stated. I did not install Ctrl or confirm Midnight support in the current build. | Official wallet docs; ctrl.xyz | 2026-11-23 |
| SubWallet | Multi-chain self-custody wallet | https://www.subwallet.app/ | Multi-chain; official wallet docs list Midnight as available in the browser matrix | Not independently verified | Live site (HTTP 200). I did not install SubWallet or confirm Midnight support in the current build. | Official wallet docs; subwallet.app | 2026-11-23 |
| NuFi | Multi-chain self-custody wallet | https://nu.fi/ | Multi-chain; official wallet docs list Midnight as available in the browser matrix | Not independently verified | Live site at https://nu.fi/ (HTTP 200). https://nufi.io/ failed TLS from this box. I did not install NuFi or confirm Midnight support in the current build. | Official wallet docs; nu.fi | 2026-11-23 |
| midnight-wallet-cli | Community CLI wallet (`midnight serve`) plus an MCP server for agents | https://www.npmjs.com/package/midnight-wallet-cli and https://github.com/nel349/midnight-wallet-cli-hub | Local / developer use | Depends on usage | npm registry `latest` is 0.6.1 (HTTP 200 on the registry API). Official wallet docs list this as the CLI/MCP path. I did not install or run the CLI. The npm website HTML was HTTP 403 from this box's scraper; the registry API succeeded. | Official wallet docs; npm registry; awesome-dapps | 2026-11-23 |
| Midnight Passport | Planned passkey / contract-account identity layer | https://midnightpassport.com/ and https://docs.midnightpassport.com/ | Stagenet only (permissioned test network), per official wallet docs | Smart-contract account (in development) | Both sites live (HTTP 200). Official wallet docs (status dated 3 October 2026, still published today): in development; demo is Stagenet-only, not Preview, Preprod, or Mainnet; no published Passport SDK packages; not a DApp Connector wallet. I did not sign up or use the demo. | Official wallet docs; product and docs sites | 2026-11-23 |
| Fireblocks | Institutional MPC custody; official docs list it for institutional NIGHT | https://www.fireblocks.com/ | Emerging / institutional, per official wallet docs | MPC / threshold | Company site live (HTTP 200). I did not verify a shippable self-custody or consumer Midnight product. | Official wallet docs; fireblocks.com | 2026-11-23 |

Nocturne Wallet is listed in awesome-dapps with a Chrome Web Store link. I did not open the Chrome Web Store listing in this pass, so it is not listed as live here.

## dApps and end-user products

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| cNgD (Dust Generation / cNIGHT mapping) | Official non-custodial UI to register Cardano cNIGHT so it can generate DUST on Midnight | https://midnight-dust-mainnet.nethermind.io/ | Mainnet (and Cardano, for the cNIGHT side) | DUST generation; the page is a mapping UI, not a general token bridge | Live page (HTTP 200). The page states it is non-custodial and that users must verify addresses before approving anything. I did not connect a wallet or submit a registration. | Official endpoints doc (Mainnet table) | 2026-11-23 |
| NorthStar DEX (beta) | Midnight DEX UI for token swaps and liquidity | https://beta.northstardex.com/ | Not independently confirmed from the pages I opened (see notes) | Docs describe pool-based swap and liquidity orders; I did not confirm which data is public vs shielded in the live UI | Live app shell (HTTP 200) with a Swap view and activity/pools sections. Marketing site https://www.northstardex.com/ and docs https://docs.northstardex.com/ also opened (HTTP 202/200). The docs I opened do not clearly state Mainnet vs a testnet. I did not connect a wallet or submit a swap, so on-chain function and network are unverified. | Product and docs sites; official catalog | 2026-11-23 |
| Noctis Zone / DarkVeil | Token launchpad whose DarkVeil window takes private Midnight orders and later anchors a certificate on Cardano | https://noctis.zone/ and https://noctis.zone/darkveil/ | Official Sept 2026 blog: deployed to Preprod (testnet). Current live sale not verified | Private state + ZK on Midnight for the buying window; Cardano is used for claim/payment and the certificate, per the product page | Both pages live (HTTP 200). The DarkVeil page describes ZK allowlist proofs and hidden buyer identities during the window. I did not join a launch, so I cannot confirm a sale is running today. | Product site; official Sept 2026 blog; awesome-dapps | 2026-11-23 |
| Ura Relay Club | Sponsors DUST network fees for approved applications | https://relayclub.ura.finance/ | Preprod (testnet) for approved projects; the site says Mainnet is not enabled | Fee sponsorship (application still builds and proves the transaction) | Live page (HTTP 200). Documentation is public; live sponsorship needs project eligibility and API keys. I did not request access or send a transaction. | Product site; official Sept 2026 blog | 2026-11-23 |
| Midnames Compact Playground | In-browser Compact editor and compiler, including a simulator | https://playground.midnames.com/ | Tooling (compile is local or remote; not a ledger deployment I verified) | n/a (developer tool) | Site opened (HTTP 200, title "Compact Playground"). Official community docs still list it. The Midnames core repo is in awesome-dapps' dormant folder (last GitHub push 2026-06-12). I did not compile a contract or check the compiler version against 0.36.1. | Official community docs; live site | 2026-11-23 |
| Midnames / midnight.domains | Name service for human-readable Midnight names (docs describe `.night` domains) | https://midnight.domains/ and https://docs.midnight.domains/ | Not independently confirmed | Name service; selective-disclosure / DID claims exist in older Midnames descriptions, not verified here | Both sites live (HTTP 200). Docs describe register/resolve/transfer flows that require a wallet. I did not connect a wallet or register a name. Same dormancy caveat as the playground. | Official community docs; live sites | 2026-11-23 |
| CSWAP x Midnight | Partnership / future DEX and launchpad page for shielded trading and RWAs | https://www.cswap.info/midnight (same campaign also at https://www.cswap.fi/midnight) | Not live as a Midnight DEX I could use | Announced: shielded trading, private liquidity, selective disclosure | Pages live (HTTP 200) but they advertise an LBE "coming Jan 15th" and use "can enable" language. Not listed as a live dApp. | Official catalog; product pages | 2026-11-23 |

## Developer tools and SDKs

| Name | What it does | URL | Network | Privacy pattern | Status / notes | Source | Date checked |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Midnight.js 5.0.1 | Official TypeScript DApp framework | https://www.npmjs.com/package/@midnight-ntwrk/midnight-js (registry: https://registry.npmjs.org/@midnight-ntwrk/midnight-js/latest) | All current public networks when pinned correctly | Contract / proof integration | npm `latest` is **5.0.1**. The npm website HTML was 403 from this box; the registry API returned 200. | Compatibility matrix; npm registry | 2026-11-23 |
| Wallet SDK 2.0.1 | Official SDK for wallet integrations | Registry latest for `@midnightntwrk/wallet-sdk` and `@midnight-ntwrk/wallet-sdk` | All current public networks when pinned correctly | Wallet / DUST / proving | npm `latest` is **2.0.1** under both package names. | Compatibility matrix; npm registry | 2026-11-23 |
| DApp Connector API 4.1.0 | Interface between dApps and wallets | Registry latest for `@midnightntwrk/dapp-connector-api` | Wallet / dApp integrations | n/a | npm `latest` is **4.1.0**. | Compatibility matrix; npm registry | 2026-11-23 |
| Compact toolchain | Official Compact compiler releases | https://github.com/midnightntwrk/compact | n/a (compiler) | n/a | Live, not archived. Repository last pushed 2026-11-11. Pin **0.36.1** from the matrix, not older Ledger 8 toolchains. | Official GitHub; compatibility matrix | 2026-11-23 |
| midnight-examples | Official monorepo of current example DApp code | https://github.com/midnightntwrk/midnight-examples | Local and testnet examples, as documented in the repo | Varies by example | Live, not archived. Last pushed 2026-11-23. This is the current official starting point (see also Dead or stale). | Official GitHub; this repo's HERMES-1 brief | 2026-11-23 |
| OpenZeppelin Contracts for Compact | Compact modules for tokens, access control, and security | https://github.com/OpenZeppelin/compact-contracts and https://docs.openzeppelin.com/contracts-compact/ | Library (network depends on the dApp that uses it) | Includes shielded-owner and ShieldedAccessControl modules | Repo and docs live (HTTP 200). Official community docs still call the library experimental and not for production use. Repo last pushed 2026-11-22. I did not compile the contracts. | Official community docs; GitHub; OZ docs | 2026-11-23 |
| Edda Labs Midnight starter | Community full-stack starter (React, Compact, CLI) | https://github.com/eddalabs/midnight-starter-template | Depends on how it is configured | n/a | Repo live, not archived. Last pushed 2026-08-18, so treat stack pins as unverified against 0.36.1 / Midnight.js 5.0.1 until someone re-checks the tree. I did not build it. | Official community docs; GitHub | 2026-11-23 |
| Compact Playground (community) | Browser Compact IDE listed in awesome-dapps | https://midnight-playground-one.vercel.app/ | Tooling | n/a | Live (HTTP 200). I did not compile a contract or check the compiler version. Distinct from the Midnames playground. | awesome-dapps | 2026-11-23 |
| Web3Fast Midnight | Community development tools site listed in awesome-dapps | https://midnight.web3fast.dev/ | Unverified | n/a | Site live (HTTP 200). I did not create an account or run a deploy. | awesome-dapps | 2026-11-23 |
| EchoMKB | Agent skill / site that searches live Midnight docs and reports version drift | https://m.echoforgeef.com/echomkb and https://github.com/EchoForge-Dev/EchoMKB | n/a (docs tool) | n/a | Site live (HTTP 200). I did not run a query against the official matrix. | awesome-dapps | 2026-11-23 |
| Night Check | Compact / SDK review tool with a website | https://midnight.gridservices.xyz/ and https://github.com/CjDabrow/night-check | Unverified | Claims on-chain certification; not verified here | Site live (HTTP 200). I did not submit a contract. | awesome-dapps | 2026-11-23 |
| MIDSKILLS | Modular agent skills for Compact and dApp scaffolds | https://midskills.sevryn.xyz/ and https://github.com/Kali-Decoder/Midnight-skills | n/a (dev tool) | n/a | Site live (HTTP 200). | awesome-dapps | 2026-11-23 |
| Nightforge | Community CLI environment for compiling, deploying, and operating Compact contracts | https://github.com/cadalt0/NIGHTFORGE | Unverified against current pins | n/a | Repo exists and is not archived, but last pushed 2026-04-06, well before the ledger 9 / 0.36.1 pins. Official community docs still list Nightforge. Treat current-stack compatibility as unverified. I did not run it. | Official community docs; GitHub | 2026-11-23 |

Mesh Midnight is listed in awesome-dapps at https://midnight.meshjs.dev/. TLS from this box failed (`UNEXPECTED_EOF_WHILE_READING`), so it is **not** listed as live today.

## Community demo sites (opened; on-chain function unverified)

These front ends are listed in awesome-dapps and returned HTTP 200 when opened on 2026-11-23. I did not connect a wallet or confirm they still talk to a current ledger 9 testnet. They are demos or hackathon apps, not verified production dApps.

| Name | URL | Date checked |
| --- | --- | --- |
| MatkaNight (card prediction game demo) | https://matkanight.vercel.app/ | 2026-11-23 |
| Midnight Private Voting demo | https://midnight-khaki.vercel.app/ | 2026-11-23 |
| DarkPool DEX demo | https://midnight-inky.vercel.app/ | 2026-11-23 |
| Midnight Private Auction demo | https://midnight-private-auction.vercel.app/ | 2026-11-23 |
| MidPilot demo | https://midpilot.vercel.app/ | 2026-11-23 |
| Tally | https://www.tallylend.site/ | 2026-11-23 |
| Anonymous Whispers demo | https://anonymous-whispers.vercel.app/ | 2026-11-23 |
| Lantern demo | https://lantern-midnight.vercel.app/demo | 2026-11-23 |
| Midnight-ZK-Judge demo | https://midnight-zk-judge.vercel.app/ | 2026-11-23 |

## Dead, retired, or stale

| Item | What I found | URL / handle | Date checked |
| --- | --- | --- | --- |
| `create-mn-app` | GitHub repository **archived**. Official and team guidance is to start from `midnight-examples`, not this package. | https://github.com/midnightntwrk/create-mn-app | 2026-11-23 |
| `midnight-local-dev` | GitHub repository **archived** (last push 2026-10-26). Some official guide pages still link it for the local network. This repo already re-pointed HERMES-3 to `midnight-examples`. | https://github.com/midnightntwrk/midnight-local-dev | 2026-11-23 |
| `testnet-02` endpoints | Official docs: the name and hosts are retired and no longer resolve. From this box, `rpc.testnet-02.midnight.network` and `indexer.testnet-02.midnight.network` failed TLS. | `rpc.testnet-02.midnight.network`, `indexer.testnet-02.midnight.network` | 2026-11-23 |
| Official Preprod RPC and indexer hostnames | Documented as retired 2026-10-09, with RPC shutdown 2026-11-18. Live check: `https://rpc.preprod.midnight.network` and `https://indexer.preprod.midnight.network` return HTTP 410 `ENDPOINT_DECOMMISSIONED` and point to Blockfrost. | Official endpoints doc; live HTTP | 2026-11-23 |
| Official Mainnet indexer hostname | Documented as retired. Live check: `https://indexer.mainnet.midnight.network` returns HTTP 410. | Official endpoints doc; live HTTP | 2026-11-23 |
| Official Mainnet RPC hostname | Official endpoints doc says `rpc.mainnet.midnight.network` was shut down on 2026-11-18 and "no longer answers requests," and tells teams to use Blockfrost. **Discrepancy:** a `system_chain` JSON-RPC call on 2026-11-23 still returned `"Midnight Mainnet"`. I am not listing this hostname as a supported public endpoint. Follow the official migration to Blockfrost (token required) or a self-hosted node. | Official endpoints doc; live RPC probe | 2026-11-23 |
| Ledger 8 tooling | Compatibility matrix: Ledger 8 transaction formats and older pins (for example Compact toolchain 0.31.1, Midnight.js 4.1.1, Wallet SDK 1.2.0, proof server 8.x) are rejected on current networks. Do not copy Ledger 8-era examples or guides as if they were current. | Official compatibility matrix | 2026-11-23 |
| Midnight Developer Academy URL | awesome-dapps still lists https://academy.midnight.network/ as a Developer Academy. The page I opened is a Midnight marketing landing page ("Midnight is live"), not a course catalog. Direct HTTP from this box was often 429. I could not verify a working academy curriculum at that URL today. | awesome-dapps; page open | 2026-11-23 |
| Midnames core repo | Listed under awesome-dapps dormant projects. GitHub last pushed 2026-06-12. The playground and domains sites still load (see above). | https://github.com/midnames/core | 2026-11-23 |
| Midnight Live View | Still listed in official community projects docs, but awesome-dapps places it in dormant projects. GitHub last pushed 2026-01-15. I did not run the tool. | https://github.com/Midnight-Scripts/Midnight-Live-View | 2026-11-23 |
| Nethermind Preview faucet host | Officially listed, but HTTP 503 on the first check this date (see Faucets). | https://midnight-tmnight-preview.nethermind.dev/ | 2026-11-23 |
| Mesh Midnight site | Listed in awesome-dapps; TLS failed from this box today. | https://midnight.meshjs.dev/ | 2026-11-23 |

Other official hosts I also confirmed as decommissioned in passing: the endpoints doc states that as of 2026-10-13, retired indexer hostnames answer HTTP 410 with `ENDPOINT_DECOMMISSIONED`. That matched Mainnet and Preprod indexer hosts today.

## Gaps — categories with nothing I could verify as live

- **General-purpose bridges.** I did not find a live Midnight↔other-chain asset bridge I could open and use. cNgD maps cNIGHT to DUST generation; it is not a general token bridge. Catalog partners such as VIA Labs, and the Pintent repo in awesome-dapps, were not verified as live products in this pass.
- **Games.** The official catalog includes gaming partners (for example PLAY NATION, Paima Studios, DEGA) but I did not find a playable Midnight game with a verified current-network deployment. Community game demos exist (see the demo table); on-chain function is unverified.
- **NFT marketplaces.** NMKR and similar names appear as catalog partners. I did not open a live Midnight NFT marketplace. Official `example-kitties` is an example repo, not a production market.
- **Consumer custodial wallets.** Official wallet docs still say there is no documented first-party consumer custodial Midnight wallet.
- **Mainnet faucet.** None, by design.
- **Independently verified mainnet DeFi.** NorthStar's beta UI opened but I could not confirm its network or complete a swap. CSWAP's Midnight pages are a future LBE / partnership narrative, not a working DEX today. I am not willing to list a "live mainnet DEX" from those checks alone.

The official ecosystem catalog, opened on 2026-11-23, contains a long list of partners across AI, banking, DAOs, DeFi, exchanges, gaming, identity, infrastructure, custody, NFTs, prediction markets, security, RWAs, and wallets (from 1AM and AEON through zkMe). Many of those cards are companies or announced collaborations, not Midnight products I could open. I did not copy the full partner list into this directory as if each one were a live dApp.

## What I did not verify

- Any wallet install, connector session, or transaction.
- Any faucet disbursement or Blockfrost query (no keys, no sign-ups).
- Compiler or SDK behavior beyond published version numbers.
- Completeness or correctness of explorer and indexer data.
- That community demo front ends still target ledger 9 / the current pins.
- Hardware wallets, CUDA / proof-station performance, or mainnet funds.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

[@kshot9000](https://x.com/kshot9000) · [github.com/Kshot3000](https://github.com/Kshot3000) · thanks to the Midnight team — [@midnightntwrk](https://github.com/midnightntwrk) — for the stack this work stands on.

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
