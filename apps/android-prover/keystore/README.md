# Sideload signing key

`midnight-prover.jks` is the signing key for every Midnight Prover
sideload build (CLAUDE-AP-9). Alias `midnight-prover`, store and key
password `android` — the same public convention as the standard
Android debug key.

Certificate SHA-256:
42:14:91:9A:8F:64:B1:99:E2:EC:50:D0:8F:40:F1:AA:0D:1F:B2:2D:41:57:3D:71:AC:F7:B8:27:4C:53:9A:F0

Why a committed key: CI runners generate a fresh debug key per
machine, so consecutive releases carried different certificates and
Android refused in-place updates (INSTALL_FAILED_UPDATE_INCOMPATIBLE,
found by HERMES-7 between v0.7.0 and v0.7.1). One fixed key means
every release from v0.8.1 on updates in place.

This key is deliberately public: the app is free and open source,
and anyone can rebuild it from source. Its only power is signing
updates for this app's sideload channel. It is NOT a Play Store
key — store distribution would use a separate private key that is
never committed.

Support and credits

Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v

Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

@kshot9000 · Independent builder — not affiliated with the Midnight Foundation or IOG.
