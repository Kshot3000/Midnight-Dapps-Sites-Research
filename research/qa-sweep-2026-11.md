# GROK-3 — Repo QA sweep

**Checked:** 2026-11-23, America/Chicago (link and live-site fetches also ran at 2026-11-24 ~04:30 UTC)
**Checked by:** Grok swarm (GROK-3)
**Method:** cloned `main` at `b1ff137`, extracted links from authored repo files, requested each unique remote URL, opened the live hub, ran axe-core and computed-contrast and overflow checks in headless Chrome against both the local hub and the live hub, and ran `node --test tests/hub.test.mjs`. No redesigns — Sol owns design. Clear breaks went out as small fix PRs. Judgement calls are listed here for Muse.

## What I checked

### Links

I extracted **155** link occurrences from repo Markdown, HTML, CSS/JS, and workflow files, then deduplicated them.

- **32** internal relative targets: **32 resolved**, **0 missing**.
- **51** unique `http(s)` URLs.
  - **9** were local or test fixtures (`127.0.0.1`, `localhost`, `example.invalid`) and were not fetched as live pages.
  - **42** remote URLs were checked live.
  - **41** are live.
  - **1** is broken in the browser: the hub’s Proof Station ZIP artifact link. Fixed in [PR #11](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/11).

How the 41 live URLs were confirmed:

- **25** returned HTTP 200 to a direct request, including GitHub, Docker Hub, NVIDIA, Microsoft Learn, and `https://x.com/kshot9000`.
- **8** Midnight docs URLs (`docs.midnight.network/...`) were rate-limited with HTTP 429 / a Vercel challenge on raw curl. I opened all eight through a documentation fetch and each one returned its live page. The eight are: installation, build-a-private-smart-contract, local-proving, manage-dust, full-node, ledger-9, network, and support-matrix.
- **8** `www.npmjs.com/package/@midnight-ntwrk/...` and `@midnightntwrk/...` pages returned HTTP 403 to curl (bot block). `registry.npmjs.org` returned **200** for all eight packages, and a fetch of `https://www.npmjs.com/package/@midnight-ntwrk/midnight-js` showed the published page at version **5.0.1**.

The scoped set from the GROK-3 packet (every external link in README, the board, the backlog, and the hub) is included in those 42. I also checked the other repo docs, including `apps/proof-station/README.md` and `research/stack-versions.md`.

### Live hub

`https://kshot3000.github.io/Midnight-Dapps-Sites-Research/` returned **200**. The HTML is **identical** to `sites/hub/index.html` (23,413 bytes). I also requested:

- `/`, `/` with no trailing slash, and `/index.html`: all **200**.
- **7 / 7** assets **200**: `hub.css?v=3`, `hub.js?v=2`, `favicon.png`, `eclipse.webp`, `eclipse-mobile.webp`, `proof-station-cpu.webp`, `proof-station-cuda.webp`.

The live page carries both donation addresses exactly, `@kshot9000`, and the line “Independent builder — not affiliated with the Midnight Foundation or IOG.” It has `lang="en"` and a `viewport` of `width=device-width, initial-scale=1`.

### Accessibility

On the local hub and the live hub, in headless Chrome:

- Heading order is `h1` → `h2` → `h3` with no skipped levels. The search box and status select have `<label>` elements. The skip link is the first tab stop and targets `#main`. The hero image is decorative (`alt=""`, `aria-hidden` on the picture). Both Proof Station screenshots have descriptive `alt` text.
- Keyboard tab order (first 30 stops) is skip link, wordmark, main nav, GitHub, hero actions, Proof Station actions, CPU/CUDA toggles, the visible screenshot, then catalogue filters. The hidden CUDA screenshot is not in the tab order when CPU is selected.
- axe-core (WCAG 2.0/2.1/2.2 A and AA, plus best-practice): **1 violation**, rule `label-content-name-mismatch`, **3 nodes**, on both the local and live hubs. Fixed in [PR #12](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/12). After that fix, the same axe run on the local hub reported **0 violations**.
- Contrast: I computed ratios for **22** text samples against a resolved solid background (muted body, quiet notes, wordmark subtitle, nav, buttons, filters, honesty lines, wallet labels, and footer). **22 / 22** met WCAG AA. axe left **27** contrast checks incomplete only because the Proof Station section sits on a CSS gradient; those same colors passed once I used a solid ancestor background.

### Mobile

Headless Chrome viewports, local and live hubs:

- **No horizontal overflow** at **320, 375, 390, 414, 768, 1024, and 1440** px (`document.documentElement.scrollWidth` equalled `innerWidth`; **0** overflowing elements).
- Both donation address `<code>` elements fit their boxes at all of those widths (`scrollWidth <= clientWidth`).
- Viewport meta is present, as noted above.

Existing repo browser tests already cover 320/390/768/1024/1440, skip-link keyboard use, and a 390 px touch pass. I did not repeat that Playwright suite locally.

### Attribution and stack pins

`node --test tests/hub.test.mjs` on `main` and on both fix branches: **8 passed, 0 failed**. Those tests require both donation addresses, character for character, plus `@kshot9000` on authored HTML footers and project Markdown, and the independence line on README and HTML footers. I also grepped the Markdown set: every `.md` file carries both exact addresses.

README and `apps/proof-station/README.md` pins match the live compatibility matrix I opened (`Compact toolchain 0.36.1`, `Midnight.js 5.0.1`, `Wallet SDK 2.0.1`, `proof-server:9.0.2`, indexer `4.4.101`). README’s network ledger pin `9.1.0.1` matches the official ledger 9 notes. I found no Ledger 8 version pins in our own code or docs (only the board rule telling us not to copy stale Ledger 8 examples).

## What I fixed

| PR | One-line summary |
| --- | --- |
| [#11](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/11) | The Download Windows x64 ZIP button used an Actions artifact URL that 404s in the browser. It now points at the live Proof Station workflow page and names the `midnight-proof-station-win-x64` artifact. |
| [#12](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/12) | Wordmark and both “Copy address” buttons now include their visible label text in the accessible name (WCAG 2.5.3). |

## What I left, and why

These are judgement calls for Muse. I did not change them.

1. **Official docs that still describe Ledger 8.** The hub and README link to [How to build a private smart contract](https://docs.midnight.network/guides/build-a-private-smart-contract) as the Compact privacy guide. `apps/proof-station/README.md` links to [Proving transactions locally](https://docs.midnight.network/guides/local-proving). Both pages are live, and both now open with a banner that they still use Ledger 8 versions. I did not retarget or annotate those links, because that is copy on Sol’s pages and Hermes already flags stale official guides in `research/stack-versions.md`.

2. **`#testnet` has `aria-labelledby` on a `div` with no role.** axe marked this as incomplete, not a violation: the referenced heading is visible, and the attribute may not be well supported without a role. Adding `role="region"` would be a one-attribute fix if Muse wants it. I left the markup alone.

3. **Board submissions log still calls [PR #3](https://github.com/Kshot3000/Midnight-Dapps-Sites-Research/pull/3) a draft.** It was merged. That is a board typo, not a hub break. I did not edit the log in this sweep.

4. **Proof Station workflow has not been run on `main`.** Every `midnight-proof-station-win-x64` artifact I saw was produced from `sol/midnight-proof-station` (`push` or `pull_request`). After SOL-GPU-1 merged, I did not find a Proof Station run on `main`. The ZIP in PR #11 therefore still has to be collected from a successful Actions run, not from a main-branch artifact or a Release.

## For Kyle

- **GitHub Release for the Windows ZIP.** Direct `/actions/runs/{id}/artifacts/{id}` links currently 404 in the browser even when the artifact is unexpired in the API. A Release would give testers a stable download that does not depend on the Actions UI. I did not create one.
- **Dispatch Proof Station on `main`** if you want a main-branch artifact after the merge.
- I did not use mainnet funds, wallet signing, API keys, or account sign-ups.

## What I did not verify

- Real phones or tablets; all mobile checks were headless Chrome viewports.
- Screen readers (NVDA, VoiceOver, TalkBack).
- `tests/hub.browser.cjs` locally. Playwright Chromium was not installed in this environment. CI should run that suite on PRs #11 and #12. I did re-run axe after PR #12 (0 violations) and the 8 Node hub tests on both fix branches.
- A cookie-based GitHub web sign-in, so I cannot prove the old artifact HTML URL works in a signed-in browser session. Unauthenticated requests, and requests that only sent an API token, received 404.
- I did not download or run the Windows ZIP, and I did not run CUDA or a Preview node.

---

Support — both addresses, character for character, alongside @kshot9000:

- **Cardano (ADA):** addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
- **Midnight (shielded):** mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q

*Independent builder — not affiliated with the Midnight Foundation or IOG.*
