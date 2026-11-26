import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-15 — guards for the launch advertising asset pack.
// Built by @kshot9000. Support the dev — tips welcome:
// Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
// Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
// Independent builder — not affiliated with the Midnight Foundation or IOG.

const root = fileURLToPath(new URL("../", import.meta.url));
const briefPath = "research/launch-assets.md";
const brief = () => readFileSync(join(root, briefPath), "utf8");

// These values come from the project brief, independently of the assets file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";
const repoUrl = "https://github.com/Kshot3000/Midnight-Dapps-Sites-Research";

const matrixPins = ["9.0.2", "0.36.1", "5.0.1", "4.1.0", "4.4.101", "2.0.1"];
const ledger8Pins = ["8.1.3", "8.1.0", "8.0.3", "0.31.1", "0.30.0", "4.0.1", "1.2.0", "0.16.0", "ledger-v8"];

const textBlocks = (content) =>
  [...content.matchAll(/```text\n([\s\S]*?)```/g)].map((m) => m[1].replace(/\n$/, ""));

// X's weighting: each URL counts as 23 characters, and the arrow → counts as 2.
const xLength = (text) =>
  text.replace(/https?:\/\/[^\s]+/g, "x".repeat(23)).replaceAll("\u2192", "xx").length;

test("the launch assets brief exists", () => {
  assert.ok(existsSync(join(root, briefPath)), briefPath + " is missing");
});

test("the brief contains both exact addresses, @kshot9000 and the independence line", () => {
  const content = brief();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, briefPath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), briefPath + " is missing the independence line");
});

test("the brief is labelled drafts only and does not use Donate wording", () => {
  const content = brief();
  assert.match(content, /DRAFTS ONLY/);
  assert.match(content, /until Kshot okays/i);
  assert.doesNotMatch(content, /\bdonate\b/i, briefPath + " must not use the word Donate");
  assert.match(content, /Support the dev/i);
  assert.match(content, /Tip \/ support the dev/i);
  assert.match(content, /tips unlock nothing/i);
});

test("the brief names the current pin sheet and carries no Ledger 8-era pins", () => {
  const content = brief();
  for (const pin of matrixPins) {
    assert.ok(content.includes(pin), briefPath + " must name " + pin);
  }
  for (const pin of ledger8Pins) {
    assert.ok(!content.includes(pin), briefPath + " must not carry the Ledger 8-era pin " + pin);
  }
});

test("four X drafts each stay within 280 characters, tag @MidnightNtwrk, and use the repo link at most twice", () => {
  const blocks = textBlocks(brief());
  assert.ok(blocks.length >= 5, "expected four X drafts plus the long-form block");
  const posts = blocks.slice(0, 4);
  for (const [i, post] of posts.entries()) {
    const length = xLength(post);
    assert.ok(length <= 280, "post " + (i + 1) + " is " + length + " characters (X-weighted)");
    assert.match(post, /@MidnightNtwrk\b/, "post " + (i + 1) + " must tag @MidnightNtwrk");
  }
  const withRepo = posts.filter((post) => post.includes(repoUrl));
  assert.ok(withRepo.length <= 2, "the repo link may appear in at most two of the four posts");
  assert.equal(withRepo.length, 2, "the wave 7 packet puts the repo link in two of the four posts");
});

test("@IOHK_Charles appears in exactly one post draft, and that post links merged work", () => {
  const posts = textBlocks(brief()).slice(0, 4);
  const withCharles = posts.filter((post) => /@IOHK_Charles\b/.test(post));
  assert.equal(withCharles.length, 1, "@IOHK_Charles must appear in exactly one of the four post drafts");
  assert.ok(withCharles[0].includes(repoUrl), "the @IOHK_Charles post must link the merged repo");
  assert.match(brief(), /at most one @IOHK_Charles tag a week/i);
});

test("the long-form draft is 1,500–2,500 characters and carries attribution", () => {
  const longform = textBlocks(brief())[4];
  assert.ok(longform, "the fifth text block should be the long-form story");
  assert.ok(
    longform.length >= 1500 && longform.length <= 2500,
    "long-form is " + longform.length + " characters, expected 1500–2500",
  );
  assert.ok(longform.includes(affiliation));
  assert.ok(longform.includes(addresses.cardano));
  assert.ok(longform.includes(addresses.midnight));
  assert.match(longform, /@kshot9000\b/);
  assert.match(longform, /Preview \(testnet\)/);
  assert.match(longform, /Preprod \(testnet\)/);
  assert.match(longform, /not a phone measurement/);
  assert.match(longform, /Free during launch, unaudited, not a Play Store listing/);
});

test("Preview and Preprod are labelled testnet wherever the drafts name them", () => {
  const content = brief();
  const barePreview = [...content.matchAll(/\bPreview\b/g)];
  const labelledPreview = [...content.matchAll(/Preview \(testnet\)/g)];
  assert.equal(
    barePreview.length,
    labelledPreview.length,
    "every Preview mention must be written as Preview (testnet)",
  );
  const barePreprod = [...content.matchAll(/\bPreprod\b/g)];
  const labelledPreprod = [...content.matchAll(/Preprod \(testnet\)/g)];
  assert.equal(
    barePreprod.length,
    labelledPreprod.length,
    "every Preprod mention must be written as Preprod (testnet)",
  );
});

test("key verified figures and [CONFIRM AFTER GROK-17] notes are present", () => {
  const content = brief();
  for (const figure of ["1.168 s", "0.766 s", "1,509-byte", "2,940-byte", "2.56 s", "v0.3.1", "v0.2.0"]) {
    assert.ok(content.includes(figure), briefPath + " must include " + figure);
  }
  assert.match(content, /\[CONFIRM AFTER GROK-17/);
  assert.match(content, /no implied endorsement/i);
  assert.doesNotMatch(content, /\bAthena\b/);
});

test("pasteable drafts say first device run coming soon and do not pitch LAN, cloud or remote proving", () => {
  const blocks = textBlocks(brief());
  const posts = blocks.slice(0, 4);
  const longform = blocks[4];
  assert.match(posts[0], /built to prove on your phone, first device run coming soon/);
  assert.match(longform, /built to prove on your phone, first device run coming soon/);
  assert.match(posts[0], /localhost by default/);
  assert.match(longform, /localhost by default/);
  for (const [i, draft] of [...posts, longform].entries()) {
    assert.doesNotMatch(draft, /proves on this phone/, "pasteable draft " + (i + 1) + " must not say proves on this phone");
  }
  assert.match(posts[0], /Muse \(AI seat on Kshot's multi-AI team; reviews and merges\)/);
  assert.match(posts[1], /Hermes \(local AI seat on Kshot's multi-AI team\)/);
  assert.match(longform, /Hermes \(a local AI seat on Kshot's multi-AI team\)/);
  assert.match(longform, /Muse \(an AI seat on Kshot's multi-AI team that reviews and merges\)/);
  for (const [i, draft] of [...posts, longform].entries()) {
    assert.doesNotMatch(draft, /\bLAN\b/, "pasteable draft " + (i + 1) + " must not pitch LAN proving");
    assert.doesNotMatch(draft, /\bcloud\b/i, "pasteable draft " + (i + 1) + " must not pitch cloud proving");
    assert.doesNotMatch(draft, /\bremote\b/i, "pasteable draft " + (i + 1) + " must not pitch remote proving");
  }
});

test("the research reading guide lists the launch assets brief", () => {
  const guide = readFileSync(join(root, "research/README.md"), "utf8");
  assert.match(guide, /^\| \[`launch-assets\.md`\]\(launch-assets\.md\) \(GROK-15/m);
  assert.match(guide, /\w+ sourced research briefs/);
});
