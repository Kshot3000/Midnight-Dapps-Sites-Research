import assert from "node:assert/strict";
import { globSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");
const pkg = JSON.parse(read("package.json"));

const ADA = "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v";
const MIDNIGHT = "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q";
const DISCLAIMER = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

test("npm test runs every node test file through one glob", () => {
  assert.equal(pkg.scripts.test, "node --test tests/*.test.mjs");
});

test("every node suite is picked up by the glob and the Playwright file is not", () => {
  const files = globSync("tests/*.test.mjs", { cwd: root }).map((p) => p.split(String.fromCharCode(92)).join("/")).sort();
  for (const expected of [
    "tests/dust-planner.test.mjs",
    "tests/ecosystem-directory.test.mjs",
    "tests/hub.test.mjs",
    "tests/test-runner.test.mjs",
  ]) {
    assert.ok(files.includes(expected), expected + " should match tests/*.test.mjs");
  }
  assert.ok(!files.includes("tests/hub.browser.cjs"));
});

test("no test file imports another test file, so nothing runs twice", () => {
  for (const file of globSync("tests/*.test.mjs", { cwd: root })) {
    const source = read(file);
    assert.doesNotMatch(source, /import\s+["'][^"']*\.test\.mjs["']/, file + " imports another test file");
    assert.doesNotMatch(source, /from\s+["'][^"']*\.test\.mjs["']/, file + " imports another test file");
  }
});

test("package.json carries the exact attribution", () => {
  assert.equal(pkg.attribution.cardano, ADA);
  assert.equal(pkg.attribution.midnightShielded, MIDNIGHT);
  assert.equal(pkg.attribution.x, "@kshot9000");
  assert.equal(pkg.attribution.disclaimer, DISCLAIMER);
  assert.ok(pkg.description.endsWith(DISCLAIMER));
});
