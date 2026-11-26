import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-14 — guards for the Android prover docs + copy-accuracy sweep.
// Built by @kshot9000. Support the dev — tips welcome:
// Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
// Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
// Independent builder — not affiliated with the Midnight Foundation or IOG.

const root = fileURLToPath(new URL("../", import.meta.url));
const briefPath = "research/android-prover-qa.md";
const brief = () => readFileSync(join(root, briefPath), "utf8");

// These values come from the project brief, independently of the QA file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

// The verified ledger 9 pin sheet (research/stack-versions.md).
const matrixPins = ["9.0.2", "0.36.1", "5.0.1", "4.1.0", "4.4.101", "2.0.1"];
// Ledger 8-era versions that must never appear in this brief.
const ledger8Pins = ["8.1.3", "8.1.0", "8.0.3", "0.31.1", "0.30.0", "4.1.1", "4.0.1", "1.2.0", "0.16.0", "ledger-v8"];

test("the Android prover QA brief exists", () => {
  assert.ok(existsSync(join(root, briefPath)), briefPath + " is missing");
});

test("the QA brief contains both exact addresses, @kshot9000 and the independence line", () => {
  const content = brief();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, briefPath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), briefPath + " is missing the independence line");
});

test("the QA brief audits v0.3.1 / versionCode 4 and names the pin sheet without Ledger 8 pins", () => {
  const content = brief();
  assert.match(content, /0\.3\.1/);
  assert.match(content, /versionCode \*\*4\*\*|versionCode = 4/);
  for (const pin of matrixPins) {
    assert.ok(content.includes(pin), briefPath + " must name " + pin);
  }
  for (const pin of ledger8Pins) {
    assert.ok(!content.includes(pin), briefPath + " must not carry the Ledger 8-era pin " + pin);
  }
});

test("the known stale v0.2 line is reported as an Issue", () => {
  const content = brief();
  assert.match(content, /^\| R\d+ \| "v0\.2 carries the full Midnight-themed design.*\| Issue \|/m);
});

test("the QA brief has a NOT verified section covering phone, emulator and proof server", () => {
  const content = brief();
  const match = content.match(/^## NOT verified\n([\s\S]*?)(?=^## |^---$)/m);
  assert.ok(match, briefPath + " needs a '## NOT verified' section");
  const section = match[1];
  for (const topic of [/phone/i, /emulator/i, /proof server/i]) {
    assert.match(section, topic, "the NOT verified section must mention " + topic);
  }
});

test("fixes are proposals only and numbered 1..n", () => {
  const content = brief();
  assert.match(content, /proposals? only/i);
  assert.match(content, /^## 5\. Fix list/m);
  const fixes = [...content.matchAll(/^\s*(\d+)\. \*\*File:\*\*/gm)].map((m) => Number(m[1]));
  assert.ok(fixes.length > 0, "the fix list must have numbered entries");
  assert.deepEqual(fixes, fixes.map((_, i) => i + 1), "fixes must be numbered 1..n");
});

test("the proposed on-screen strings table covers onboarding, progress, errors, empty states and tips", () => {
  const content = brief();
  const match = content.match(/^## 6\. Proposed on-screen strings\n([\s\S]*?)(?=^## )/m);
  assert.ok(match, briefPath + " needs a '## 6. Proposed on-screen strings' section");
  const section = match[1];
  assert.match(section, /^\| Screen \/ state \| Current string \(file:line\) \| Proposed string \| Reason \|$/m);
  for (const topic of [/Onboarding/, /set up the server/i, /Progress/, /Error — unreachable/, /Error — queue full 429/, /Error — busy 503/, /Error — rejected 400/, /empty/i, /tip copy/i]) {
    assert.match(section, topic, "the strings table must cover " + topic);
  }
});

test("Preview and Preprod are labelled testnet and no donation wording is proposed", () => {
  const content = brief();
  assert.match(content, /Preview \(testnet\)/);
  assert.match(content, /Preprod \(testnet\)/);
  assert.doesNotMatch(content, /\bdonate\b/i, briefPath + " must not use the word Donate");
  assert.match(content, /Support the dev/);
});

test("the hub gap is flagged for Sol, not edited", () => {
  const content = brief();
  assert.match(content, /sites\/hub\/index\.html/);
  assert.match(content, /flag(ging|ged)? only/i);
  assert.match(content, /Sol/);
});

test("the research reading guide lists the QA brief", () => {
  const guide = readFileSync(join(root, "research/README.md"), "utf8");
  assert.match(guide, /^\| \[`android-prover-qa\.md`\]\(android-prover-qa\.md\) \(GROK-14\)/m);
});
