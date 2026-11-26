import assert from "node:assert/strict";
import { globSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-13-fixup guard: the three brief disagreements the GROK-13 glossary flagged stay corrected.
// Constants are copied here on purpose: test files must not import each other.
const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");
const posix = (p) => p.split(String.fromCharCode(92)).join("/");

const CORRECT_HOST = "srs.midnight.network";
const WRONG_HOST = /srs\.midnight\.net(?!work)/;
// Lines that may still name the wrong host: dated correction notes and the record of the original disagreement.
const CORRECTION_MARKER = /correct/i;
const RAW_LOG_DIR = "research/evidence/"; // captured raw logs are quoted verbatim and never edited
const GLOSSARY = "research/glossary.md";
const DISAGREEMENT_HEADING = "## Docs vs briefs disagreements";

const HERMES3 = "research/local-stack-verification.md";
const HERMES5 = "research/dust-registration.md";
const HERMES1 = "research/stack-versions.md";
const EXAMPLES_URL = "https://github.com/midnightntwrk/midnight-examples";

const sectionRange = (lines, heading) => {
  const start = lines.indexOf(heading);
  if (start < 0) return [-1, -1];
  let end = lines.findIndex((line, i) => i > start && /^## /.test(line));
  if (end < 0) end = lines.length;
  return [start, end];
};

test("no research brief names srs.midnight.net outside raw logs and correction notes", () => {
  const files = globSync("research/**/*.md", { cwd: root }).map(posix);
  assert.ok(files.includes(HERMES3), HERMES3 + " should be scanned");
  const offenders = [];
  for (const file of files) {
    if (file.startsWith(RAW_LOG_DIR)) continue;
    const lines = read(file).split("\n");
    const [from, to] = file === GLOSSARY ? sectionRange(lines, DISAGREEMENT_HEADING) : [-1, -1];
    lines.forEach((line, i) => {
      if (!WRONG_HOST.test(line)) return;
      if (CORRECTION_MARKER.test(line)) return;
      if (i >= from && i < to) return;
      offenders.push(file + ":" + (i + 1));
    });
  }
  assert.deepEqual(offenders, [], "srs.midnight.net still named at: " + offenders.join(", "));
});

test("HERMES-3 names srs.midnight.network in its setup notes, log excerpt and friction list", () => {
  const content = read(HERMES3);
  assert.ok(content.split(CORRECT_HOST).length - 1 >= 3, HERMES3 + " should name " + CORRECT_HOST + " at least 3 times");
  assert.match(content, /downloading k=6 public parameters from srs\.midnight\.network\)/);
  assert.match(content, /Host corrected 2026-11-25/);
});

test("HERMES-5 no longer describes wallet key role 4 only as Metadata", () => {
  const content = read(HERMES5);
  assert.doesNotMatch(content, /\*\*4\*\* = Metadata/, "role table still says 4 = Metadata");
  for (const line of content.split("\n").filter((l) => /role 4 (is|as) "?Metadata/i.test(l) || /Metadata \| 4/.test(l))) {
    assert.match(line, /ECDSA|EcdsaUnshielded/, "a Metadata mention of role 4 must also give the current ECDSA role: " + line);
    assert.match(line, /archived/i, "a Metadata mention of role 4 must say the spec is archived: " + line);
  }
  assert.match(content, /\*\*4\*\* = Unshielded \(Night\) ECDSA/);
  assert.ok(content.includes("EcdsaUnshielded"));
  assert.ok(content.includes("https://docs.midnight.network/concepts/keys-and-addresses"));
  assert.ok(content.includes("packages/hd/src/HDWallet.ts"));
});

test("HERMES-1 points the local network at midnight-examples, not the archived midnight-local-dev", () => {
  const content = read(HERMES1);
  const lines = content.split("\n");
  const note4 = lines.find((l) => l.startsWith("4. **Local dev stack:**"));
  assert.ok(note4 && note4.includes(EXAMPLES_URL), "note 4 must link " + EXAMPLES_URL);
  const forSol = lines.find((l) => l.startsWith("- Local network:"));
  assert.ok(forSol && forSol.includes(EXAMPLES_URL), "the For Sol local-network line must link " + EXAMPLES_URL);
  assert.match(forSol, /env:up/);
  for (const line of lines.filter((l) => l.includes("midnight-local-dev"))) {
    assert.match(line, /archived/i, "midnight-local-dev may only appear as archived: " + line);
  }
});

test("the glossary marks disagreements 1 to 3 resolved and keeps 4 and 5 as flags", () => {
  const lines = read(GLOSSARY).split("\n");
  const [from, to] = sectionRange(lines, DISAGREEMENT_HEADING);
  assert.ok(from >= 0, GLOSSARY + " lost its disagreements section");
  const section = lines.slice(from, to).join("\n");
  const items = section.split(/^### /m).slice(1);
  const byNumber = Object.fromEntries(items.map((body) => [body.match(/^(\d+)\./)?.[1], body]).filter(([n]) => n));
  for (const n of ["1", "2", "3"]) {
    assert.match(byNumber[n] ?? "", /Resolved 2026-11-25 by GROK-13-fixup/, "disagreement " + n + " should be marked resolved");
  }
  for (const n of ["4", "5"]) {
    assert.ok(byNumber[n], "disagreement " + n + " must still be present");
    assert.doesNotMatch(byNumber[n], /Resolved/, "disagreement " + n + " should stay a flag");
  }
});
