# UI v2 — design package (mockups, not shipped product)

**Status: design reference.** Everything in this folder is a static HTML/CSS
design mockup. It is not a shipped app, it is not connected to any network,
and nothing in it performs a real transaction or a real proof.

The figures inside the mockups (balances, DUST amounts, percentages, prices)
are **simulated placeholder data** chosen by the designer to lay out the
screens. They are not measurements, not prices, and not claims about the
Midnight network or any product in this repo.

## What's here

| File | What it is |
| --- | --- |
| `index.html` | Showcase hub that switches between the three surfaces below |
| `assets/midnight-tokens.css` | Shared "obsidian" design tokens (palette, glass surfaces, privacy-cloak CSS) |
| `website/index.html` | Research-portal concept: dApp directory + DUST economics simulator |
| `desktop-app/index.html` | PC "command center" concept: Ctrl+K palette, Shift+P privacy cloak, local prover, vault / lending / Zswap / Passport surfaces |
| `mobile-app/index.html` | Mobile concept: 4-tab wallet-style app (Vault / Zswap / Passport / dApps) with an on-device prover demo flow |

## How the team uses it

- **Sol (SOL-AP-1 improvement pass):** `mobile-app/` + the shared tokens are
  the visual target for refining the shipped Android prover's UI. The
  mockup's tabs are a wallet super-app concept — the shipped app is the
  prover in `apps/android-prover/` (Prove / Activity / Server), and only the
  design language carries over, not the concept's features.
- **Desktop concept:** the command-center layout is reference material for a
  possible future PC companion. Its lending / Zswap / Passport surfaces are
  concept screens for backlog directions (see `ideas/backlog.md`) — none of
  them are built, and this package does not announce them.
- **Privacy cloak:** the one interaction that maps directly onto the shipped
  prover — a one-tap mode that masks sensitive on-screen material — was
  adopted into the Android app itself (CLAUDE-AP-7).

## What this package is not

- Not the live hub. The live site stays where it has always been, deployed
  from `sites/hub/` — do not move these files to the repo root or point
  GitHub Pages at them.
- Not a release. The shipped Android prover is `apps/android-prover/`,
  released from CI builds (see the repo's Releases page).

---

Independent builder — not affiliated with the Midnight Foundation or IOG. @kshot9000

Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v

Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
