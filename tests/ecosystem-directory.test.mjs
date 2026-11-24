import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const directoryPath = "research/ecosystem-directory.md";
const content = readFileSync(join(root, directoryPath), "utf8");

// These values come from the project brief, independently of the directory file.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

test("the ecosystem directory exists as a research brief", () => {
  assert.ok(existsSync(join(root, directoryPath)), directoryPath + " is missing");
});

test("the ecosystem directory contains both exact donation addresses and fails if either is missing or altered", () => {
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, directoryPath + " must show the complete " + network + " address");
    assert.equal(content.includes(address), true, network + " address must appear verbatim");
  }
});

test("the ecosystem directory names @kshot9000 and the independent-builder line exactly", () => {
  assert.match(content, /@kshot9000\b/, directoryPath + " needs the builder’s profile");
  assert.ok(content.includes(affiliation), directoryPath + " is missing the independence line");
});

test("the ecosystem directory keeps the required catalogue sections", () => {
  for (const heading of [
    "## Official docs and catalogues",
    "## Explorers",
    "## Faucets (testnet only)",
    "## Indexers, RPC, and the proof server",
    "## Wallets",
    "## dApps and end-user products",
    "## Developer tools and SDKs",
    "## Community demo sites (opened; on-chain function unverified)",
    "## Dead, retired, or stale",
    "## Gaps — categories with nothing I could verify as live",
    "## What I did not verify",
  ]) {
    assert.ok(content.includes(heading), "Missing section: " + heading);
  }
});

test("Preview and Preprod are labelled testnet in the directory", () => {
  assert.match(content, /Preview \(testnet\)/);
  assert.match(content, /Preprod \(testnet\)/);
  assert.match(content, /Mainnet has no faucet/);
});

test("the directory records the current ledger 9 stack pins", () => {
  for (const pin of ["0.36.1", "5.0.1", "2.0.1", "4.1.0", "9.0.2", "4.4.101"]) {
    assert.ok(content.includes(pin), "Missing stack pin " + pin);
  }
});

test("markdown links in the ecosystem directory are https, have no credentials, and use a sane host", () => {
  const links = [...content.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)].map((match) => match[1]);
  assert.ok(links.length > 0, "the directory should cite sources as links");
  for (const target of links) {
    if (target.startsWith("#") || target.startsWith("mailto:")) continue;
    if (!/^[a-z][a-z\d+.-]*:/i.test(target)) {
      const path = target.split("#")[0];
      // Relative links resolve from the directory file's own folder, the way GitHub renders them.
      assert.ok(existsSync(join(root, dirname(directoryPath), path)), target + " does not exist next to " + directoryPath);
      continue;
    }
    const url = new URL(target);
    assert.equal(url.protocol, "https:", target + " must be https");
    assert.ok(!url.username && !url.password, target + " contains credentials");
    assert.ok(url.hostname.includes("."), target + " is missing a hostname");
  }
});

test("bare http(s) URLs in the ecosystem directory are https and have no credentials", () => {
  const urls = [...content.matchAll(/https?:\/\/[^\s)\]>'"]+/g)].map((match) => match[0].replace(/[.,;:]+$/, ""));
  assert.ok(urls.length > 20, "expected a sourced URL catalogue, found " + urls.length);
  for (const raw of urls) {
    const url = new URL(raw);
    assert.equal(url.protocol, "https:", raw + " must be https");
    assert.ok(!url.username && !url.password, raw + " contains credentials");
  }
});

function splitRow(line) {
  const inner = line.trim().replace(/^\|/, "").replace(/\|$/, "");
  const cells = [];
  let current = "";
  for (let index = 0; index < inner.length; index += 1) {
    const char = inner[index];
    if (char === "\\" && inner[index + 1] === "|") {
      current += "|";
      index += 1;
      continue;
    }
    if (char === "|") {
      cells.push(current.trim());
      current = "";
      continue;
    }
    current += char;
  }
  cells.push(current.trim());
  return cells;
}

function catalogueTables(markdown) {
  const tables = [];
  const sectionPattern = /^## (.+)$/gm;
  const headings = [];
  let match;
  while ((match = sectionPattern.exec(markdown))) {
    headings.push({ title: match[1], start: match.index });
  }
  for (let index = 0; index < headings.length; index += 1) {
    const heading = headings[index];
    const end = index + 1 < headings.length ? headings[index + 1].start : markdown.length;
    const body = markdown.slice(heading.start, end);
    const lines = body.split("\n");
    for (let lineIndex = 0; lineIndex < lines.length; lineIndex += 1) {
      if (!/^\| .+\|/.test(lines[lineIndex]) || !/^\| ---/.test(lines[lineIndex + 1] || "")) continue;
      const headers = splitRow(lines[lineIndex]).map((header) => header.toLowerCase());
      const rows = [];
      for (let rowIndex = lineIndex + 2; rowIndex < lines.length; rowIndex += 1) {
        if (!/^\| /.test(lines[rowIndex])) break;
        rows.push(splitRow(lines[rowIndex]));
      }
      tables.push({ heading: heading.title, headers, rows });
    }
  }
  return tables;
}

function columnIndex(headers, names) {
  return headers.findIndex((header) => names.includes(header));
}

test("every directory row in every category has non-empty description, network, and privacy-pattern fields", () => {
  const tables = catalogueTables(content);
  assert.ok(tables.length >= 9, "expected a table in each catalogue category, found " + tables.length);
  let rowCount = 0;
  for (const table of tables) {
    const description = columnIndex(table.headers, ["what it does", "what i found"]);
    const network = columnIndex(table.headers, ["network"]);
    const privacy = columnIndex(table.headers, ["privacy pattern"]);
    assert.ok(description >= 0, table.heading + " is missing a description column (What it does / What I found)");
    assert.ok(network >= 0, table.heading + " is missing a Network column");
    assert.ok(privacy >= 0, table.heading + " is missing a Privacy pattern column");
    assert.ok(table.rows.length > 0, table.heading + " has no data rows");
    for (const row of table.rows) {
      const label = table.heading + " / " + (row[0] || "(unnamed row)");
      assert.ok((row[description] || "").trim(), label + " is missing a description");
      assert.ok((row[network] || "").trim(), label + " is missing a network field");
      assert.ok((row[privacy] || "").trim(), label + " is missing a privacy-pattern field");
      rowCount += 1;
    }
  }
  assert.ok(rowCount >= 70, "expected a full catalogue, found " + rowCount + " rows");
});
