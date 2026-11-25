import assert from "node:assert/strict";
import { existsSync, readFileSync, statSync } from "node:fs";
import { dirname, join, normalize, relative } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const glossaryPath = "research/glossary.md";
const guidePath = "research/README.md";

// These values come from the project brief, independently of the glossary file.
// (Test files must not import each other, so they are copied here.)
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

// Official sources a glossary entry may cite (GROK-13 packet: docs.midnight.network or an official Midnight repo page).
const officialSource =
  /\]\(https:\/\/(docs\.midnight\.network\/|midnight\.network\/|github\.com\/midnightntwrk\/|github\.com\/LFDT-Minokawa\/|github\.com\/input-output-hk\/lace|www\.lace\.io\/)[^)\s]*\)/;
const noBriefMarker = "no repo brief yet";

// Terms the GROK-13 packet requires at minimum (matched against entry headings).
const requiredTerms = [
  /^NIGHT$/, /^tNIGHT/, /^cNIGHT/, /^DUST$/, /^DUST registration$/, /^Shielded vs unshielded$/,
  /^Preview \(testnet\)$/, /^Preprod \(testnet\)$/, /^Proof server$/, /^Compact$/, /^Witness$/, /^ZSwap$/, /^Ledger 9$/,
];

// The verified pin matrix (stack-versions.md / support matrix, 2026-11-25).
const pins = ["0.36.1", "5.0.1", "9.0.2", "4.1.0", "4.4.101", "2.0.1"];
// Ledger 8-era or off-matrix versions that may only appear in the flagged stale-version section.
const staleVersions = ["0.31.1", "4.1.1", "4.0.1", "8.1.3", "1.2.0", "5.0.0", "2.0.0", "5.0.2", "5.0.3", "ledger-v8"];

const glossary = () => readFileSync(join(root, glossaryPath), "utf8");

const section = (content, heading) => {
  const start = content.indexOf("\n## " + heading + "\n");
  assert.ok(start >= 0, glossaryPath + " is missing the '## " + heading + "' section");
  const rest = content.slice(start + 1);
  const next = rest.indexOf("\n## ", 4);
  return next >= 0 ? rest.slice(0, next) : rest;
};

// Each "### Term" block inside "## Terms".
const entries = (content) =>
  section(content, "Terms")
    .split(/^### /m)
    .slice(1)
    .map((block) => ({ term: block.split("\n", 1)[0].trim(), body: block }));

// Relative markdown links (no scheme, no pure #anchor), resolved from research/.
const relativeLinks = (text) =>
  [...text.matchAll(/\]\(([^)\s]+)\)/g)]
    .map((m) => m[1])
    .filter((href) => !/^[a-z]+:/i.test(href) && !href.startsWith("#"))
    .map((href) => href.split("#")[0]);

test("the glossary exists", () => {
  assert.ok(existsSync(join(root, glossaryPath)), glossaryPath + " is missing");
});

test("the glossary contains both exact addresses, @kshot9000 and the independence line", () => {
  const content = glossary();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, glossaryPath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), glossaryPath + " is missing the independence line");
});

test("the glossary covers every term the GROK-13 packet lists, in alphabetical order", () => {
  const terms = entries(glossary()).map((e) => e.term);
  assert.ok(terms.length >= requiredTerms.length, "expected at least " + requiredTerms.length + " entries, found " + terms.length);
  for (const required of requiredTerms) {
    assert.ok(terms.some((t) => required.test(t)), "missing required term " + required);
  }
  const key = (t) => t.replace(/[`()]/g, "").toLowerCase();
  const sorted = [...terms].sort((a, b) => key(a).localeCompare(key(b)));
  assert.deepEqual(terms, sorted, "entries must be alphabetical");
});

test("every term entry has an official https source and a repo-brief link (or the 'no repo brief yet' marker)", () => {
  for (const { term, body } of entries(glossary())) {
    assert.match(body, /\*\*Official source:\*\*/, term + " has no 'Official source' line");
    assert.match(body, officialSource, term + " has no https link to an official Midnight source");
    const briefLine = body.split("\n").find((line) => line.includes("**Repo brief"));
    assert.ok(briefLine, term + " has no 'Repo briefs' line");
    const briefLinks = relativeLinks(briefLine).filter((href) => href.endsWith(".md"));
    assert.ok(
      briefLinks.length > 0 || briefLine.includes(noBriefMarker),
      term + " must link a research/ brief or say '" + noBriefMarker + "'",
    );
  }
});

test("every relative link in the glossary resolves to a real file inside the repo", () => {
  const content = glossary();
  const base = dirname(join(root, glossaryPath));
  const links = relativeLinks(content);
  assert.ok(links.length > 0, "expected relative links to repo briefs");
  for (const href of links) {
    const target = normalize(join(base, href));
    assert.ok(!relative(root, target).startsWith(".."), href + " points outside the repo");
    assert.ok(existsSync(target) && statSync(target).isFile(), glossaryPath + " links " + href + ", which does not exist");
  }
});

test("Preview and Preprod are labelled testnet on every line that names them", () => {
  const content = glossary();
  assert.match(content, /Preview \(testnet\)/);
  assert.match(content, /Preprod \(testnet\)/);
  for (const line of content.split("\n")) {
    if (/\b(Preview|Preprod)\b/.test(line)) assert.match(line, /testnet/, "unlabelled Preview/Preprod: " + line);
  }
});

test("the glossary shows an access date, a disagreements section and an honesty section", () => {
  const content = glossary();
  assert.match(content, /Accessed 2026-11-25/);
  const disagreements = section(content, "Docs vs briefs disagreements");
  assert.ok((disagreements.match(/^### \d+\. /gm) ?? []).length >= 1, "expected at least one numbered disagreement");
  assert.match(content, /^## Honesty: what was verified and what wasn't$/m);
  assert.match(content, /\*\*Not verified:\*\*/);
});

test("only the current pins are presented; stale versions appear only in the flagged section", () => {
  const content = glossary();
  for (const pin of pins) assert.ok(content.includes(pin), glossaryPath + " should state the pin " + pin);
  const flagged = content.indexOf("### Stale or off-matrix versions seen in official pages");
  const honesty = content.indexOf("## Honesty: what was verified and what wasn't");
  assert.ok(flagged > 0 && honesty > flagged, "stale-version section must sit before the honesty section");
  const outside = content.slice(0, flagged) + content.slice(honesty);
  for (const stale of staleVersions) {
    const pattern = new RegExp("(?<![\\w.])" + stale.replace(/\./g, "\\.") + "(?![\\w.])");
    assert.doesNotMatch(outside, pattern, stale + " appears outside the flagged stale-version section");
  }
});

test("the research guide has a row that links the glossary", () => {
  const readme = readFileSync(join(root, guidePath), "utf8");
  assert.match(readme, /^\| \[`glossary\.md`\]\(glossary\.md\) \(GROK-13\) \|/m);
});
