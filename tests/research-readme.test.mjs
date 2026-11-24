import assert from "node:assert/strict";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const guidePath = "research/README.md";

// These values come from the project brief, independently of the guide file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

const guide = () => readFileSync(join(root, guidePath), "utf8");
const briefs = () =>
  readdirSync(join(root, "research"))
    .filter((name) => name.endsWith(".md") && name !== "README.md")
    .sort();
// Table rows whose first cell links a brief: | [`name.md`](name.md) ...
const indexedBriefs = (content) =>
  [...content.matchAll(/^\| \[`([^`]+\.md)`\]\(\1\)/gm)].map((m) => m[1]).sort();

test("the research reading guide exists", () => {
  assert.ok(existsSync(join(root, guidePath)), guidePath + " is missing");
});

test("every research brief has exactly one row in the reading guide, and every row points at a real brief", () => {
  const onDisk = briefs();
  const rows = indexedBriefs(guide());
  assert.ok(onDisk.length > 0, "expected research briefs on disk");
  for (const brief of onDisk) {
    assert.equal(rows.filter((r) => r === brief).length, 1, brief + " must have exactly one row in " + guidePath);
  }
  for (const row of rows) {
    assert.ok(onDisk.includes(row), guidePath + " lists " + row + ", which is not in research/");
  }
});

test("the reading guide contains both exact addresses, @kshot9000 and the independence line", () => {
  const content = guide();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, guidePath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), guidePath + " is missing the independence line");
});

test("the root README tells people to run the full suite with npm test", () => {
  const readme = readFileSync(join(root, "README.md"), "utf8");
  assert.match(readme, /^npm test$/m);
  assert.doesNotMatch(readme, /^node --test tests\/hub\.test\.mjs$/m);
});
