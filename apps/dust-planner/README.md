# DUST Planner Pro

A static planner that models how much DUST a given amount of registered NIGHT can generate over time, and whether a planned fee spend fits that model. Every output is labelled as a modelled estimate. This is not a wallet, and it does not connect to a network, request keys, or submit transactions.

Preview and Preprod are testnets. On those networks the display names are tNIGHT and tDUST. This tool does not read a live testnet or mainnet balance.

Open [the planner](./index.html) from a local static server, or use the commands below from the repository root.

```sh
node --test tests/hub.test.mjs tests/dust-planner.test.mjs
python3 -m http.server 4174 --bind 127.0.0.1 --directory apps/dust-planner
```

[Open the local planner](http://127.0.0.1:4174/)

## Inputs and model

The form takes a NIGHT amount, whether that NIGHT is registered for DUST generation, days since registration, and a planned DUST fee spend.

The model follows official ledger 9 DUST mechanics:

- The cap is 5 DUST per 1 NIGHT.
- Generation ramps linearly from empty to that cap in 604,815 seconds, about 7 days.
- Unregistered NIGHT generates nothing. The planner says so when registration is off.
- After the backing NIGHT is spent or deregistered, DUST decays at the same rate to zero, starting at the spend instant. DUST can still pay fees while it decays.
- The official 3-hour `dustGracePeriodSeconds` parameter (10,800 seconds) is a transaction timestamp validity window, not a delay before decay. The manage-dust generation and decay formulas omit it for that reason, and this planner does too. See the parameter table below.
- Fees are paid in DUST. A planned spend here is only compared against the modelled balance.

“Days since registration” is modelled as the time the generating NIGHT UTXO has been unspent. Official docs note that a registration usually respends NIGHT, and that any later spend of that UTXO, including a transfer to yourself or a change output, starts decay and creates a new DUST UTXO from zero.

## Parameters and sources

These are documented ledger parameters, not constants. Each network stores its own set, and governance can replace it. The planner uses the published initial values, which the official DUST guide also prints from a Mainnet read at block 2,809,291. This app did not query an indexer itself.

| Parameter | Value used | Source |
| --- | --- | --- |
| `nightDustRatio` | 5,000,000,000 SPECK per STAR (5 DUST per NIGHT) | [DUST architecture](https://docs.midnight.network/concepts/dust-architecture) initial parameters; [Manage DUST](https://docs.midnight.network/guides/manage-dust) Mainnet example |
| `generationDecayRate` | 8,267 SPECK per STAR per second | Same sources |
| `timeToCapSeconds` | 604,815 seconds (about 7 days) | `nightDustRatio ÷ generationDecayRate`, rounded up; Manage DUST; [Ledger 9 notes](https://docs.midnight.network/relnotes/ledger-9) |
| `dustGracePeriodSeconds` | 10,800 seconds (3 hours) | [DUST architecture](https://docs.midnight.network/concepts/dust-architecture) initial `dust_grace_period`; [Manage DUST](https://docs.midnight.network/guides/manage-dust) field definition and Mainnet example. This is the maximum age, measured against block time, of the timestamp a transaction declares for DUST spends and registrations. The ledger rejects a transaction when that time is older than the window, or later than the block time. It is not added to modelled decay time. |
| Units | 1 NIGHT = 1,000,000 STAR; 1 DUST = 10^15 SPECK | DUST architecture (the ledger constants are authoritative; earlier whitepaper speck figures are not) |

The Ledger 9 notes say the DUST parameters stay the same: 5 DUST per NIGHT at the cap, and about 7 days to reach the cap. The Manage DUST guide, describing those unchanged generation parameters, also notes that the grace period stays at 3 hours.

## Why decay timing does not apply the grace period

Muse asked this follow-up because the model already exported `DUST_GRACE_PERIOD_SECONDS` (3 hours) but did not use it in decay timing. I re-read the official pages on 2026-11-24 (America/Chicago). The right fix is to document the omission, not to add 10,800 seconds to the time to zero:

- Manage DUST defines `dustGracePeriodSeconds` as a transaction validity window, not as a term in the cap, generation, or decay formulas. Its official decay example starts subtracting at the spend second: at `spentAt` the value is still the cap, at `spentAt + 1` it is cap minus one second of rate, and at `2 * spentAt` it is zero.
- DUST architecture says that once the backing NIGHT is spent, "DUST immediately starts to decay, even if it was still in the generation phase." The "grace period" section on that page is about accepting a transaction whose DUST timestamp is within a few hours of block time, to account for network delays, because shielded DUST value is computed for the time of transaction creation.

Those are documented ledger rules, not modelled guesses. The decay figures on the planner remain modelled estimates of that official linear formula.

Pinned stack context for this repo, from `research/stack-versions.md` (checked 2026-11-23): Compact toolchain 0.36.1, Midnight.js 5.0.1, proof server 9.0.2, DApp Connector API 4.1.0, indexer 4.4.101, Wallet SDK 2.0.1, network ledger 9. This planner is static HTML, CSS, and JavaScript and does not load those packages.

## What I did not verify

- I did not read live DUST parameters from Mainnet, Preview, or Preprod myself.
- I did not run a wallet, proof server, or indexer, and I did not estimate real transaction fees.
- I did not model multiple NIGHT UTXOs or a spend that splits change.

Support — both addresses, character for character, alongside [@kshot9000](https://x.com/kshot9000):

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

Independent builder — not affiliated with the Midnight Foundation or IOG.
