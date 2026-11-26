import assert from "node:assert/strict";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-16 — guards for the pre-launch audit of the whole repo.
// Built by @kshot9000. Support the dev — tips welcome:
// Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
// Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
// Independent builder — not affiliated with the Midnight Foundation or IOG.

const root = fileURLToPath(new URL("../", import.meta.url));
const briefPath = "research/launch-audit.md";
const read = (path) => readFileSync(join(root, path), "utf8");
const brief = () => read(briefPath);

// These values come from the project brief, independently of the audit file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

// The verified ledger 9 pin sheet (research/stack-versions.md), checked against the live matrix on 2026-11-25.
const matrixPins = ["9.0.2", "0.36.1", "5.0.1", "4.1.0", "4.4.101", "2.0.1"];
// Ledger 8-era versions: allowed in the audit only on a line that also says "stale".
const ledger8Pins = ["8.1.3", "8.1.0", "8.0.3", "0.31.1", "0.30.0", "4.1.1", "4.0.1", "1.2.0", "0.16.0", "ledger-v8"];

// Public surfaces: the hub, root README, app READMEs, two in-app attribution screens, every research file but this one.
const surfaces = () => [
  "README.md",
  "sites/hub/index.html",
  "apps/dust-planner/index.html",
  "apps/dust-planner/README.md",
  "apps/proof-station/README.md",
  "apps/proof-station/MainWindow.xaml",
  "apps/android-prover/README.md",
  "apps/android-prover/server-setup/README.md",
  "apps/android-prover/app/src/main/java/tech/kshot/midnightprover/MainActivity.kt",
  "apps/nightattest/README.md",
  ...readdirSync(join(root, "research"))
    .filter((name) => name.endsWith(".md") && name !== "launch-audit.md")
    .sort()
    .map((name) => "research/" + name),
];

// Section 4 rows: | `path` ... | VERDICT |. One row names the in-app file with an ellipsis path.
const section = (content, heading) => {
  const match = content.match(new RegExp("^## " + heading + "[^\\n]*\\n([\\s\\S]*?)(?=^## |^---$)", "m"));
  assert.ok(match, briefPath + " needs a '## " + heading + "' section");
  return match[1];
};
const surfaceRows = (content) => {
  const rows = [];
  for (const line of section(content, "4\\. Pass/fail per surface").split("\n")) {
    const m = line.match(/^\| `([^`]+)`/);
    if (m) rows.push({ key: m[1], line });
  }
  return rows;
};
const findRow = (rows, path) =>
  rows.find(({ key }) => key === path || (key.includes("/.../") && key.split("/").at(-1) === path.split("/").at(-1)));
const verdictOf = (line) => line.split("|").map((cell) => cell.trim()).filter(Boolean).at(-1);

test("the launch audit exists", () => {
  assert.ok(existsSync(join(root, briefPath)), briefPath + " is missing");
});

test("the audit contains both exact addresses, @kshot9000 and the independence line", () => {
  const content = brief();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, briefPath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), briefPath + " is missing the independence line");
});

test("the audit names the six verified pins and carries Ledger 8-era versions only on lines that say stale", () => {
  const content = brief();
  for (const pin of matrixPins) assert.ok(content.includes(pin), briefPath + " must name " + pin);
  const offenders = content
    .split("\n")
    .map((line, i) => ({ line, n: i + 1 }))
    .filter(({ line }) => ledger8Pins.some((pin) => line.includes(pin)) && !/stale/i.test(line))
    .map(({ n }) => n);
  assert.deepEqual(offenders, [], "Ledger 8-era pins without 'stale' on the line at: " + offenders.join(", "));
});

test("every public surface has a row in the pass/fail table, and each row ends in a verdict", () => {
  const rows = surfaceRows(brief());
  for (const path of surfaces()) {
    const row = findRow(rows, path);
    assert.ok(row, "the audit table has no row for " + path);
    assert.match(verdictOf(row.line), /^(PASS|FAIL)\b/, "row for " + path + " must end in PASS or FAIL");
  }
  assert.equal(rows.length, surfaces().length, "the table must have one row per surface, no extras");
});

test("a surface that really lacks an exact address or the independence line is marked FAIL in the audit", () => {
  const rows = surfaceRows(brief());
  const exact = (text, address) => new RegExp("(?<![\\w-])" + address + "(?![\\w-])").test(text);
  for (const path of surfaces()) {
    const text = read(path);
    const complete = exact(text, addresses.cardano) && exact(text, addresses.midnight) && text.includes(affiliation);
    if (complete) continue;
    const row = findRow(rows, path);
    assert.ok(row, "no audit row for " + path);
    assert.match(verdictOf(row.line), /^FAIL\b/, path + " lacks an address or the independence line, so the audit must mark it FAIL");
    assert.match(row.line, /D\d+/, "the FAIL row for " + path + " must cite a defect id");
  }
});

test("every surface the audit scans carries only the one Cardano address and no personal-information markers", () => {
  const strays = [];
  for (const path of surfaces().concat([briefPath])) {
    const text = read(path);
    for (const m of text.matchAll(/(?<![\w-])addr1[0-9a-z]{10,}/g)) {
      if (m[0] !== addresses.cardano) strays.push(path + ": " + m[0].slice(0, 14));
    }
    for (const m of text.matchAll(/(?<![\w-])mn_[a-z_]+-addr1[0-9a-z]{10,}/g)) {
      if (m[0] !== addresses.midnight) strays.push(path + ": " + m[0].slice(0, 20));
    }
  }
  assert.deepEqual(strays, [], "unexpected addresses: " + strays.join(", "));
  const audit = brief();
  assert.doesNotMatch(audit, /[\w.+-]+@[\w-]+\.[a-z]{2,}/i, "the audit must not contain an email address");
  assert.doesNotMatch(audit, /\(?\b\d{3}\)?[-. ]\d{3}[-. ]\d{4}\b/, "the audit must not contain a phone number");
});

test("defects are numbered D1..Dn with a severity, and every FAIL row cites a defect that exists", () => {
  const content = brief();
  const defects = [...section(content, "3\\. Defect list").matchAll(/^\| D(\d+) \| (High|Medium|Low) \|/gm)];
  assert.ok(defects.length >= 10, "expected the defect table to be filled in");
  assert.deepEqual(defects.map((m) => Number(m[1])), defects.map((_, i) => i + 1), "defects must be numbered D1..Dn");
  const known = new Set(defects.map((m) => "D" + m[1]));
  for (const { line } of surfaceRows(content)) {
    if (!/FAIL/.test(verdictOf(line))) continue;
    for (const id of verdictOf(line).match(/D\d+/g) ?? []) assert.ok(known.has(id), "FAIL row cites unknown defect " + id);
  }
});

test("the audit answers the proof-server bind question from the 9.0.2 source and the official docs", () => {
  const content = brief();
  assert.match(content, /proof-server-9\.0\.2\/proof-server\/src\/lib\.rs#L54/, "must link lib.rs:54 at the 9.0.2 tag");
  assert.match(content, /no bind-address option/i);
  assert.match(content, /0\.0\.0\.0/);
  assert.match(content, /https:\/\/docs\.midnight\.network\/guides\/local-proving/);
  assert.match(content, /setup-proof-server\.sh/);
});

test("the audit records the packet's known open items and has a NOT verified section", () => {
  const content = brief();
  assert.match(content, /srs\.midnight\.net/);
  assert.match(content, /Hub Android prover section: still open/);
  assert.match(content, /GROK-14's fix list/);
  const notVerified = section(content, "7\\. NOT verified");
  for (const topic of [/phone/i, /emulator/i, /wallet/i, /bot-blocked/i]) {
    assert.match(notVerified, topic, "the NOT verified section must mention " + topic);
  }
});

test("the audit is findings only: it proposes fixes as follow-ups and does not claim to have edited app or hub files", () => {
  const content = brief();
  assert.match(content, /Findings only/);
  assert.match(content, /follow-up tasks/);
});

test("the board claims GROK-16 for Bob Research and records GROK-13-fixup as done with merge 2efcc1e", () => {
  const board = read("council/board.md");
  const row = (id) => board.split("\n").find((line) => line.startsWith("| " + id + " |"));
  assert.match(row("GROK-16"), /CLAIMED by Bob Research|DONE/);
  assert.match(row("GROK-16"), /launch-audit\.md/);
  assert.match(row("GROK-13-fixup"), /DONE/);
  assert.match(row("GROK-13-fixup"), /2efcc1e/);
});
