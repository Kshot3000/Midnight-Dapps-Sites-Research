import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-16-fixup guard: HERMES-5 footer + current pins, and the held upstream host-option draft.
// Constants are copied here on purpose: test files must not import each other.
// Built by @kshot9000. Support the dev — tips welcome:
// Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
// Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
// Independent builder — not affiliated with the Midnight Foundation or IOG.

const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");

const HERMES5 = "research/dust-registration.md";
const DRAFT = "research/upstream-drafts/proof-server-host-option.md";
const DRAFT_HEADER =
  "DRAFT — not filed. Held for Kshot's weekly batch; would be posted publicly under his account. Do not file.";
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};

const section72 = (content) => {
  const start = content.indexOf("### 7.2 ");
  assert.ok(start >= 0, HERMES5 + " is missing §7.2");
  const end = content.indexOf("\n### 7.3 ", start);
  assert.ok(end > start, HERMES5 + " is missing §7.3 after §7.2");
  return content.slice(start, end);
};

test("the upstream host-option draft exists and starts with the DRAFT / not-filed header", () => {
  assert.ok(existsSync(join(root, DRAFT)), DRAFT + " is missing");
  const content = read(DRAFT);
  assert.ok(content.startsWith(DRAFT_HEADER), DRAFT + " must start with the exact not-filed header");
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
