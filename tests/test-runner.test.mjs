import assert from "node:assert/strict";
import { globSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");
const posix = (p) => p.split(String.fromCharCode(92)).join("/");
const pkg = JSON.parse(read("package.json"));

const ADA = "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v";
const MIDNIGHT = "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q";
const DISCLAIMER = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

// Specifiers that name a test file (*.test.mjs/js/cjs). Does not match
// node:test, node:assert, or helper modules like ./helpers.mjs.
const TEST_FILE_SPEC = String.raw`['"\`][^'"\`]*\.test\.(?:mjs|js|cjs)['"\`]`;
const IMPORTS_OF_A_TEST_FILE = [
  new RegExp(String.raw`\bimport\s+${TEST_FILE_SPEC}`),
  new RegExp(String.raw`\bfrom\s+${TEST_FILE_SPEC}`),
  new RegExp(String.raw`\bimport\s*\(\s*${TEST_FILE_SPEC}`),
  new RegExp(String.raw`\brequire\s*\(\s*${TEST_FILE_SPEC}`),
];

test("npm test runs every node test file through one glob", () => {
  assert.equal(pkg.scripts.test, "node --test tests/*.test.mjs");
});

test("the test script glob expands to exactly the node suites and excludes the Playwright file", () => {
  const script = pkg.scripts.test;
  const match = /^node --test (.+)$/.exec(script);
  assert.ok(match, "test script should be node --test <glob>, got: " + script);
  const expanded = globSync(match[1], { cwd: root }).map(posix).sort();
  const onDisk = readdirSync(join(root, "tests"));
  const expected = onDisk
    .filter((name) => name.endsWith(".test.mjs"))
    .map((name) => "tests/" + name)
    .sort();
  assert.deepEqual(expanded, expected);
  assert.ok(onDisk.includes("hub.browser.cjs"), "tests/hub.browser.cjs should still exist");
  assert.ok(!expanded.includes("tests/hub.browser.cjs"));
});

test("no test file imports another test file, so nothing runs twice", () => {
  for (const file of globSync("tests/*.test.mjs", { cwd: root })) {
    const source = read(file);
    for (const pattern of IMPORTS_OF_A_TEST_FILE) {
      assert.doesNotMatch(source, pattern, file + " imports another test file (" + pattern + ")");
    }
  }
});

test("package.json carries the exact attribution", () => {
  assert.equal(pkg.attribution.cardano, ADA);
  assert.equal(pkg.attribution.midnightShielded, MIDNIGHT);
  assert.equal(pkg.attribution.x, "@kshot9000");
  assert.equal(pkg.attribution.disclaimer, DISCLAIMER);
  assert.ok(pkg.description.endsWith(DISCLAIMER));
});
