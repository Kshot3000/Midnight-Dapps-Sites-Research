# NightAttest workshop pass

I’m building NightAttest to let someone prove they hold an active workshop pass while keeping the underlying secret private. This build runs the compiled Compact contract in a browser simulator. The issuer, holder and verifier share simulated state in one tab. It is an evaluation of the contract’s rules. The app generates no cryptographic proof bytes, submits no transactions and uses no wallet or proof server.

## Run the app

Use Node.js 24 and the [Compact CLI](https://docs.midnight.network/getting-started/installation). Compile with toolchain **0.36.1** (language **0.28.0**) and runtime **0.21.0**.

```sh
cd apps/nightattest
compact update 0.36.1
npm ci
npm run compile
npm run dev
```

Open the localhost URL Vite prints. Build static assets with `npm run build` and serve that build locally with `npm run preview`. Compilation generates the contract bindings and circuit artifacts in `managed/nightattest/`. Reloading the app clears the simulated ledger, secrets and challenges. The simulator keeps no copies in browser storage.

## Try the three roles

1. In **Holder**, choose **Create simulated pass**. The tab generates a random 32-byte secret and derives its commitment with the contract’s `credentialCommitment` circuit.
2. In **Issuer**, choose **Use holder commitment**, then **Register commitment**. The simulated ledger records its issuance slot and marks it active. The issuer’s eligibility check happens off-chain.
3. In **Verifier**, choose **Issue fresh challenge**. The app generates a random 32-byte challenge. A check at this point reads pending from the simulated ledger.
4. In **Holder**, choose **Use verifier challenge**, then **Simulate presentation**. The contract evaluates the holder secret and the current Merkle path as private witnesses. A successful call adds the challenge to the simulated ledger.
5. In **Verifier**, choose **Check simulated result**. The app checks for that exact challenge in the simulated contract’s accepted-challenge set.
6. In **Issuer**, choose **Revoke commitment**. Issue a new challenge in Verifier and try presenting again in Holder. The revoked pass fails the new check. Acceptance of an older challenge records eligibility at that earlier presentation time.

A holder proves control of a bearer secret. Sharing it would let someone else present the pass. The verifier uses a different challenge for each session. The contract can issue 1,024 commitments. Revocation clears a leaf and keeps the registry entry, so the slot and commitment cannot be reused.

## Privacy boundary on a deployed contract

- The holder secret, selected commitment and Merkle path stay private during presentation. The circuit checks the active Merkle root, so a saved pre-revocation path cannot prove eligibility at a newer root.
- Issuance and revocation reveal opaque commitments, issuance slots and status. A successful presentation reveals the challenge, acceptance and the root checked. Public transaction timing and metadata could still be correlated with off-chain activity.
- I treat input to a future proof server as sensitive and keep the proving service under the holder’s control. The browser simulator uses fresh session data and requires no personal information.

## Check the build

Run `npm test` from the repo root for the repo suite. From `apps/nightattest/`, run the contract and browser checks after compilation.

```sh
npm test
npm run build
npx --no-install playwright install --only-shell chromium
npm run test:browser
```

The contract tests cover issuer authorization, replay, forged paths, revocation and the public transcript. The browser checks exercise handoffs between the roles, simulated acceptance, rejection after revocation, reset on reload, keyboard access and the exact footer. Set `NIGHTATTEST_SCREENSHOTS` to a directory to save the simulated desktop and mobile captures.

## Live proof run

For the separate live slice, I’m targeting Preview (testnet) with Midnight.js **5.0.1**, DApp Connector API **4.1.0**, Wallet SDK **2.0.1**, proof server **9.0.2**, indexer **4.4.101** and node **2.1.101** on Ledger 9. The public SRS host is `srs.midnight.network`. Preview (testnet) uses the Midnight-hosted indexer. Preprod (testnet) and mainnet use Blockfrost Midnight or self-hosted indexing. The [browser DApp guide](https://docs.midnight.network/guides/browser-dapp) is Ledger 8-era. I use the [compatibility matrix](https://docs.midnight.network/relnotes/support-matrix) for the pins.

I need my Preview (testnet) wallet address for faucet funding and my wallet signature for DUST registration. The faucet supplies 5,000 tNIGHT per request. **NIGHT must be registered for DUST generation before it can generate DUST to pay fees.** Once it has DUST, the live flow needs a deployed NightAttest contract, a registration transaction, a fresh challenge and a proof-backed presentation transaction. The verifier must check the expected contract’s finalized state for that exact challenge. The [DUST registration brief](../../research/dust-registration.md) covers registration and the first-fee flow.

## Support

Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v

Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

@kshot9000 · Independent builder — not affiliated with the Midnight Foundation or IOG.
