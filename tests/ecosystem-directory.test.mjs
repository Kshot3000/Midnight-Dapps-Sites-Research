import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
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
      assert.ok(existsSync(join(root, path)), target + " does not exist in the repo");
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
