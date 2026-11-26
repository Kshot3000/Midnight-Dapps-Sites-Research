import assert from "node:assert/strict";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-16-fixup / GROK-16-fixup2 guard: HERMES-5 footer + current pins, and every held upstream draft.
// Constants are copied here on purpose: test files must not import each other.
// Built by @kshot9000. Support the dev — tips welcome:
// Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
// Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
// Independent builder — not affiliated with the Midnight Foundation or IOG.

const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");

const HERMES5 = "research/dust-registration.md";
const DRAFTS_DIR = "research/upstream-drafts";
const DRAFT = "research/upstream-drafts/proof-server-host-option.md";
const DRAFT_HEADER =
  "DRAFT — not filed. Held for Kshot's weekly batch; would be posted publicly under his account. Do not file.";
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};

const draftFiles = () => {
  const dir = join(root, DRAFTS_DIR);
  assert.ok(existsSync(dir) && statSync(dir).isDirectory(), DRAFTS_DIR + " is missing");
  const files = readdirSync(dir)
    .filter((name) => statSync(join(dir, name)).isFile())
    .sort();
  assert.ok(files.length > 0, DRAFTS_DIR + " is empty");
  return files.map((name) => DRAFTS_DIR + "/" + name);
};

const section72 = (content) => {
  const start = content.indexOf("### 7.2 ");
  assert.ok(start >= 0, HERMES5 + " is missing §7.2");
  const end = content.indexOf("\n### 7.3 ", start);
  assert.ok(end > start, HERMES5 + " is missing §7.3 after §7.2");
  return content.slice(start, end);
};

test("research/upstream-drafts exists and is not empty", () => {
  draftFiles();
});

test("every file under research/upstream-drafts starts with the DRAFT / not-filed header", () => {
  const files = draftFiles();
  assert.ok(files.includes(DRAFT), DRAFT + " is missing");
  for (const path of files) {
    const content = read(path);
    assert.ok(content.startsWith(DRAFT_HEADER), path + " must start with the exact not-filed header");
  }
});

test("every file under research/upstream-drafts is free of tip addresses and personal-information markers", () => {
  const email = /[\w.+-]+@[\w-]+\.[a-z]{2,}/i;
  const phone = /\(?\b\d{3}\)?[-. ]\d{3}[-. ]\d{4}\b/;
  for (const path of draftFiles()) {
    const content = read(path);
    assert.ok(!content.includes("addr1"), path + " must not contain the addr1 substring");
    assert.ok(!content.includes("mn_shield"), path + " must not contain the mn_shield substring");
    assert.doesNotMatch(content, /@kshot9000\b/, path + " must not contain @kshot9000");
    assert.doesNotMatch(content, email, path + " must not contain an email address");
    assert.doesNotMatch(content, phone, path + " must not contain a phone-number-like pattern");
    assert.doesNotMatch(content, /kcaidevelopment/i, path + " must not contain kcaidevelopment");
    assert.doesNotMatch(content, /Kyle Cox/i, path + " must not contain a Kyle Cox / owner full-name byline");
  }
});

test("HERMES-5 contains the exact independence line, both tip addresses and @kshot9000", () => {
  const content = read(HERMES5);
  assert.ok(content.includes(affiliation), HERMES5 + " is missing the independence line");
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, HERMES5 + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
});

test("HERMES-5 §7.2 no longer says Wallet SDK 1.x or Midnight.js 4.x is current", () => {
  const body = section72(read(HERMES5));
  assert.ok(body.includes("2.0.1"), "§7.2 must name Wallet SDK 2.0.1");
  assert.ok(body.includes("5.0.1"), "§7.2 must name Midnight.js 5.0.1");
  assert.match(body, /Pins corrected 2026-11-25/);
  const marker = /stale|corrected|previously|first called|not a current/i;
  const offenders = body
    .split("\n")
    .map((line, i) => ({ line, n: i + 1 }))
    .filter(({ line }) => /Wallet SDK\s+\*?\*?1\./i.test(line) || /wallet-sdk`?\s+\*?\*?1\./i.test(line) || /Midnight\.js\b[^.\n]*\*?\*?4\./i.test(line) || /\b1\.2\.0\b/.test(line) || /\b4\.1\.1\b/.test(line))
    .filter(({ line }) => !marker.test(line))
    .map(({ n, line }) => "§7.2 line " + n + ": " + line);
  assert.deepEqual(offenders, [], "§7.2 still presents a 1.x / 4.x pin as current: " + offenders.join(" | "));
});
