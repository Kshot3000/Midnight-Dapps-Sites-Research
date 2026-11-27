import assert from "node:assert/strict";
import { existsSync, globSync, readFileSync, statSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const hubDirectory = join(root, "sites/hub");
const read = (path) => readFileSync(join(root, path), "utf8");
const projectFiles = (patterns) => globSync(patterns, {
  cwd: root,
  // Gradle output and NightAttest's Vite bundle are generated, not authored.
  // Held upstream-issue drafts are meant to be pasted into another repo and must not
  // carry this project's tip addresses. Guarded separately in tests/grok-16-fixup.test.mjs.
  exclude: ["**/node_modules", "apps/**/managed", "apps/nightattest/dist", "research/upstream-drafts/**", "apps/android-prover/**/build", "apps/android-prover/**/.gradle"],
})
  .map((path) => path.replaceAll("\\", "/"));
const hub = read("sites/hub/index.html");
const readme = read("README.md");

// These values come from the project brief, independently of the HTML.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

function assertAttribution(content, path) {
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, path + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/, path + " needs the builder’s profile");
}

test("repository keeps the hub, apps, research, council and backlog structure", () => {
  for (const directory of ["sites/hub", "apps", "research", "ideas", "council/specs"]) {
    assert.ok(statSync(join(root, directory)).isDirectory(), directory);
  }
  for (const file of ["README.md", "council/board.md", "council/submissions.md", "ideas/backlog.md"]) {
    assert.ok(statSync(join(root, file)).isFile(), file);
  }
  assert.match(read("council/board.md"), /\| SOL-1 \|[^\n]+\| Sol \| [^\n]+\|/);
});

test("each authored HTML page has exact donation addresses and attribution in its footer", () => {
  const pages = projectFiles(["sites/**/*.html", "apps/**/*.html"]);
  assert.ok(pages.includes("sites/hub/index.html"));

  for (const path of pages) {
    const footer = read(path).match(/<footer\b[^>]*>([\s\S]*?)<\/footer>/i)?.[1];
    assert.ok(footer, path + " is missing a footer");
    const footerText = footer.replace(/<[^>]*>/g, " ");
    assertAttribution(footerText, path);
    assert.ok(footerText.includes(affiliation), path + " is missing the independence line");
  }
});

test("both hub copy buttons reference the exact visible address text", () => {
  for (const [network, address] of Object.entries(addresses)) {
    const codeId = network + "-address";
    const code = hub.match(new RegExp('<code id="' + codeId + '">([^<]*)</code>'))?.[1];
    assert.equal(code, address);
    assert.match(hub, new RegExp('<button[^>]*data-copy="' + codeId + '"[^>]*>'));
  }
  assert.match(hub, /<a href="https:\/\/x\.com\/kshot9000">@kshot9000<\/a>/);
});

test("project Markdown carries both addresses and the builder handle", () => {
  const docs = projectFiles(["README.md", "{apps,council,ideas,research,sites}/**/*.md"]);
  for (const path of docs) {
    const content = read(path);
    assertAttribution(content, path);
    if (/(^|\/)README\.md$/.test(path)) {
      assert.ok(content.includes(affiliation), path + " is missing the independence line");
    }
  }
});

test("the ten projects appear in backlog order with status and primitive descriptions", () => {
  const expected = [
    ["nightattest", "NightAttest", "spec"],
    ["private-voting", "Private Voting", "idea"],
    ["sealed-bid-auction", "Sealed-Bid Auction", "idea"],
    ["confidential-freelance-escrow", "Confidential Freelance Escrow", "idea"],
    ["eligibility-without-kyc", "Eligibility-Without-KYC Widget", "idea"],
    ["private-payroll-proof", "Private Payroll Proof", "idea"],
    ["privacy-auditor", "“What Does This dApp See?”", "idea"],
    ["privacy-score-directory", "Privacy Score Directory", "idea"],
    ["compact-playground", "Compact Playground + Snippet Library", "idea"],
    ["dust-planner-pro", "DUST Planner Pro", "local-tool"],
  ];
  const statusLabels = { idea: "Idea", spec: "Spec", "local-tool": "Local tool" };
  const cards = [...hub.matchAll(/<li\b[^>]*\bdata-project="([^"]+)"[^>]*\bdata-category="([^"]+)"[^>]*\bdata-status="([^"]+)"[^>]*>([\s\S]*?)<\/li>/g)];
  assert.equal(cards.length, expected.length);
  for (const [index, [, slug, category, status, card]] of cards.entries()) {
    const title = card.match(/<h3 id="[^"]+">([^<]+)<\/h3>/)?.[1];
    assert.deepEqual([slug, title, status], expected[index]);
    assert.ok(hub.includes('data-filter="' + category + '"'), slug + " needs a category filter");
    assert.match(card, /class="honesty">[^<]+</);
    assert.ok(card.includes('<span class="status">' + statusLabels[status] + "</span>"));
    assert.ok(hub.includes('<option value="' + status + '">' + statusLabels[status] + "</option>"));
    for (const primitive of ["Selective disclosure", "Private state + Compact ZK", "Shielded DUST fees"]) {
      assert.ok(card.includes("<dt>" + primitive + "</dt><dd>"), slug + " needs " + primitive);
    }
  }
});

test("the hub shows Proof Station v0.2.0 and a short Android prover v0.5.0 mention", () => {
  assert.match(hub, /Windows x64 · v0\.2\.0/);
  assert.doesNotMatch(hub, /v0\.1\.2/);
  const android = hub.match(/<section\b[^>]*\bid="android-prover"[^>]*>([\s\S]*?)<\/section>/);
  assert.ok(android, "hub needs an #android-prover section");
  const card = android[1];
  assert.match(card, /v0\.5\.0/);
  assert.doesNotMatch(card, /v0\.4\.0/);
  assert.match(card, /built to prove on your phone, first device run coming soon/);
  assert.match(card, /localhost by default/);
  assert.match(card, /testnet/i);
  assert.match(card, /unaudited/i);
  assert.match(card, /\bfree\b/i);
  assert.match(card, /apps\/android-prover\//);
});

test("hub links and assets resolve from a GitHub Pages project path", () => {
  const deployment = new URL("https://example.invalid/Midnight-Dapps-Sites-Research/");
  const ids = [...hub.matchAll(/\sid="([^"]+)"/g)].map((match) => match[1]);
  assert.equal(new Set(ids).size, ids.length, "IDs must be unique");

  for (const [, attribute, value] of hub.matchAll(/\s(href|src|srcset)="([^"]+)"/g)) {
    const targets = attribute === "srcset"
      ? value.split(",").map((candidate) => candidate.trim().split(/\s+/)[0])
      : [value];

    for (const target of targets) {
      const url = new URL(target, deployment);
      assert.equal(url.protocol, "https:");
      assert.ok(!url.username && !url.password, target + " contains credentials");
      if (url.origin !== deployment.origin) continue;

      assert.ok(url.pathname.startsWith(deployment.pathname), target + " leaves the Pages project path");
      const path = url.pathname.slice(deployment.pathname.length) || "index.html";
      assert.ok(existsSync(join(hubDirectory, path)), target + " does not exist");
      if (url.hash) assert.ok(ids.includes(decodeURIComponent(url.hash.slice(1))), target + " has no anchor");
      if (/\.(css|js)$/.test(path)) assert.match(url.search, /^\?v=[1-9]\d*$/, target + " needs a cache key");
    }
  }
});

test("local README links resolve and point to the hub and council", () => {
  for (const [, target] of readme.matchAll(/\[[^\]]+\]\(([^)]+)\)/g)) {
    if (!/^[a-z][a-z\d+.-]*:/i.test(target)) {
      assert.ok(existsSync(resolve(root, target.split("#")[0])), target);
    }
  }
  assert.match(readme, /\]\(sites\/hub\/\)/);
  assert.match(readme, /\]\(council\/board\.md\)/);
});

test("README pins the verified stack and the hub includes DUST testnet registration", () => {
  for (const [component, version] of [
    ["Compact toolchain", "0.36.1"],
    ["Midnight.js", "5.0.1"],
    ["Wallet SDK", "2.0.1"],
    ["Proof server", "midnightntwrk/proof-server:9.0.2"],
    ["Indexer", "4.4.101"],
  ]) {
    assert.ok(
      readme.includes("| " + component + " | `" + version + "` |"),
      "Missing " + component + " version " + version,
    );
  }
  assert.match(hub, /Preview or Preprod testnet/);
  assert.match(hub, /Register tNIGHT for DUST generation/);
  assert.match(hub, /self-hosted for Preview and Blockfrost Midnight for Preprod/);
});
