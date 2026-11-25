import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const briefPath = "research/proof-station-qa.md";
const brief = () => readFileSync(join(root, briefPath), "utf8");

// These values come from the project brief, independently of the QA file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

// Station pins (preview.compose.yaml) and the verified ledger 9 matrix.
const stationPins = ["9.0.2", "2.1.101", "4.4.101"];
const matrixPins = ["0.36.1", "5.0.1", "2.0.1", "4.1.0"];

// Ledger 8-era versions that must never be presented as current in this brief.
const ledger8Pins = ["8.1.3", "8.1.0", "8.0.3", "0.31.1", "0.30.0", "4.1.1", "4.0.1", "1.2.0", "0.16.0", "ledger-v8"];
const staleMarker = /stale|not current|no public network runs|historical/i;

test("the Proof Station QA brief exists", () => {
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

test("the QA brief has a NOT verified section that covers GPU, Windows and running services", () => {
  const content = brief();
  const match = content.match(/^## NOT verified\n([\s\S]*?)(?=^## |^---$)/m);
  assert.ok(match, briefPath + " needs a '## NOT verified' section");
  const section = match[1];
  for (const topic of [/GPU/, /Windows/, /node/i, /proof server/i]) {
    assert.match(section, topic, "the NOT verified section must mention " + topic);
  }
});

test("the QA brief names the station pins and the matrix pins", () => {
  const content = brief();
  for (const pin of [...stationPins, ...matrixPins]) {
    assert.ok(content.includes(pin), briefPath + " must name " + pin);
  }
});

test("no Ledger 8-era pin is presented as current", () => {
  const lines = brief().split("\n");
  for (const pin of ledger8Pins) {
    const hits = lines.filter((line) => line.includes(pin));
    assert.equal(hits.length, 0, briefPath + " must not carry the Ledger 8-era pin " + pin);
  }
  for (const line of lines.filter((l) => /ledger[ -]?8/i.test(l))) {
    assert.match(line, staleMarker, "a line that mentions Ledger 8 must label it stale: " + line.slice(0, 120));
  }
});

test("Preview is labelled as testnet and fixes are proposals only", () => {
  const content = brief();
  assert.match(content, /Preview \(testnet\)/);
  assert.match(content, /proposals? only/i);
  assert.match(content, /^## 6\. Fix list/m);
  const fixes = [...content.matchAll(/^\s*(\d+)\. \*\*File:\*\*/gm)].map((m) => Number(m[1]));
  assert.ok(fixes.length > 0, "the fix list must have numbered entries");
  assert.deepEqual(fixes, fixes.map((_, i) => i + 1), "fixes must be numbered 1..n");
});

test("the research reading guide lists the QA brief", () => {
  const guide = readFileSync(join(root, "research/README.md"), "utf8");
  assert.match(guide, /^\| \[`proof-station-qa\.md`\]\(proof-station-qa\.md\) \(GROK-10\)/m);
});

test("the QA brief notes which fixes PR #47 already covered", () => {
  const content = brief();
  assert.match(content, /Status after PR #47/);
  assert.match(content, /Fix 1: done by #47/);
  assert.match(content, /Fix 4: done by #47/);
  assert.match(content, /Fix 13: partly done by #47/);
  assert.match(content, /workflow_dispatch/);
});
