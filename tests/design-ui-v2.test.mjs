import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const dir = join(root, "design", "ui-v2");

// These values come from the project brief, independently of the files.
const addresses = {
  cardano:
    "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight:
    "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation =
  "Independent builder — not affiliated with the Midnight Foundation or IOG.";

const pkgFiles = [
  "index.html",
  "assets/midnight-tokens.css",
  "website/index.html",
  "desktop-app/index.html",
  "mobile-app/index.html",
  "README.md",
];

const read = (rel) => readFileSync(join(dir, rel), "utf8");
const allFiles = (base, prefix = "") =>
  readdirSync(base, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory()
      ? allFiles(join(base, e.name), `${prefix}${e.name}/`)
      : [`${prefix}${e.name}`],
  );

test("the UI v2 design package is complete", () => {
  for (const rel of pkgFiles) {
    assert.ok(existsSync(join(dir, rel)), `design/ui-v2/${rel} is missing`);
  }
});

test("the design README labels the package as mockups with simulated data", () => {
  const readme = read("README.md");
  assert.match(readme, /design mockup/i);
  assert.match(readme, /simulated placeholder data/i);
  assert.match(readme, /not a shipped app/i);
});

test("the design README carries the exact attribution footer", () => {
  const readme = read("README.md");
  assert.ok(readme.includes(addresses.cardano), "Cardano address altered");
  assert.ok(readme.includes(addresses.midnight), "Midnight address altered");
  assert.ok(readme.includes(affiliation), "independence line missing");
  assert.ok(readme.includes("@kshot9000"), "@kshot9000 missing");
});

test("no file in the design package carries personal information", () => {
  const banned = [/945-8094/, /Silver Lake/, /Wisconsin Ave/, /kcaidevelopment/];
  for (const rel of allFiles(dir)) {
    const content = read(rel);
    for (const pattern of banned) {
      assert.doesNotMatch(
        content,
        pattern,
        `design/ui-v2/${rel} matches ${pattern}`,
      );
    }
  }
});
