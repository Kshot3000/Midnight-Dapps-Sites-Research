# NightAttest workshop pass

I’m building NightAttest to let someone prove a specific claim while keeping their credential private. This first contract tests one claim about one workshop: **I hold an active pass.** The verification here uses the generated Compact contract in a local simulator with synthetic data. The issuer, holder and verifier UI and a proof-server run on a Midnight test network are later milestones.

## How the pass works

| Role | Action | Public result |
| --- | --- | --- |
| Issuer | Deploy with the digest of an issuer secret. Check a holder’s eligibility off-chain and call `issue` with their credential commitment. | The commitment is added to the active Merkle tree. The registry records its slot and active status. |
| Holder | Generate a separate 32-byte secret for this workshop, give the issuer its `credentialCommitment`, then call `present` for a verifier’s challenge. The circuit takes the secret and its current Merkle path as private witnesses. | The contract records the accepted challenge. For a given challenge and root, the circuit’s public transcript is the same for every eligible holder. |
| Verifier | Send a fresh, random 32-byte challenge privately and keep track of it for that session. Check that the expected NightAttest contract recorded it in finalized state. | Acceptance shows that someone with an active pass answered the challenge. A missing acceptance means the challenge is still unverified. |
| Issuer | Call `revoke` with the commitment. | Its leaf is cleared and its registry entry marked revoked. The same commitment cannot be reissued. |

The issuer and holder each need an independently generated cryptographically random secret. A holder proves control of a bearer secret, so sharing it would allow another person to present the pass. The verifier chooses the expected contract address out of band and uses a new challenge for every session. A past acceptance records eligibility at acceptance time. Fresh eligibility requires a new challenge.

The proof checks membership against the tree’s current root. When issuance or revocation changes the root, fetch a fresh path before presenting. The first contract has 1,024 issuance slots. Revocation clears a leaf without reusing its slot.

## Privacy boundary

The holder’s secret, selected commitment and Merkle path stay private during presentation. Issuance and revocation reveal opaque commitments, issuance slots and status. A successful presentation reveals the challenge, the membership result and the root checked. Transaction timing and public metadata can still be correlated with off-chain activity. Keep names, email addresses and other personal information out of commitments, challenges and transactions. [Compact ledger data types](https://docs.midnight.network/compact/reference/ledger-adt)

## Run the contract checks

Use Node.js 24 and Compact compiler 0.36.1 (language 0.28, runtime 0.21.0). See the official instructions to install the Compact CLI. [Midnight toolchain setup](https://docs.midnight.network/getting-started/installation)

```sh
cd apps/nightattest
compact update 0.36.1
npm ci
npm run compile
npm test
```

The compile command produces bindings, circuit definitions and proving and verification keys in `managed/nightattest/`. The simulator calls those generated bindings and checks issuer authorization, duplicates, replay, forged paths, revocation and the public transcript. It runs without a network, a wallet or a proof server after setup and compilation.

## Support

- **Cardano (ADA)** · `addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v`
- **Midnight (shielded)** · `mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q`

I share project updates at [@kshot9000](https://x.com/kshot9000). I used the Midnight team’s voting example as a reference for private Merkle path witnesses. [Midnight voting example](https://github.com/midnightntwrk/midnight-examples/tree/c37451a810313bb22015ded286c3718db95cee19/examples/voting) · [@midnightntwrk](https://github.com/midnightntwrk) · [@CharlesHoskinson](https://github.com/CharlesHoskinson) · [@MidnightNtwrk](https://x.com/MidnightNtwrk) · [@IOHK_Charles](https://x.com/IOHK_Charles)

Independent builder — not affiliated with the Midnight Foundation or IOG.
