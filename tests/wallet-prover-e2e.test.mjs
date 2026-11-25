import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

const root = fileURLToPath(new URL("../", import.meta.url));
const docPath = "research/wallet-prover-e2e.md";
const evidenceDir = "research/evidence/grok-12";
const evidenceFiles = ["client-test-local.txt", "proof-server-log.txt", "node-indexer-excerpt.txt", "run1-failure.txt"];

// These values come from the project brief, independently of the doc.
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

const doc = () => readFileSync(join(root, docPath), "utf8");
const evidence = () => evidenceFiles.map((f) => readFileSync(join(root, evidenceDir, f), "utf8")).join("\n");

// Our own numbers quoted in the doc; each must appear verbatim in the captured evidence.
const quoted = [
  "433c4b813a333c23009a19e7083c468e9319e11779dcae4af66a093b678a50e2",
  "Tests  2 passed (2)",
  "Duration  41.13s",
  "Deploys the contract 20774ms",
  "Stores Hello World! 17075ms",
  "0.736448s",
  "0.001258s",
  "0.578181s",
  "0.577842s",
  "k=6",
  "1adc99242a7662a4a9f5f4e5da11632cff725e6527118f339b0cb4701954e35f",
  "\"height\":14",
  "Wallet sync timeout after 600000ms",
  "Connection timeout exceeded: 10s",
  "sha256:787ef99db4b4706eb14d94ad389ca45f1c09bcdf740afecb01899bfbda63dc64",
];

test("the GROK-12 doc and its evidence files exist", () => {
  assert.ok(existsSync(join(root, docPath)), docPath + " is missing");
  for (const f of evidenceFiles) assert.ok(existsSync(join(root, evidenceDir, f)), f + " is missing");
});

test("the GROK-12 doc carries both exact addresses, @kshot9000 and the independence line", () => {
  const content = doc();
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(content, exactAddress, docPath + " must show the complete " + network + " address");
  }
  assert.match(content, /@kshot9000\b/);
  assert.ok(content.includes(affiliation), docPath + " is missing the independence line");
});

test("the GROK-12 doc labels the network honestly and says it is an independent reproduction", () => {
  const content = doc();
  assert.match(content, /local `undeployed` devnet/);
  assert.match(content, /Not testnet, not mainnet/);
  assert.match(content, /Preview and Preprod \(testnet\)/);
  assert.match(content, /\*\*independent reproduction\*\*/i);
  assert.match(content, /different environment/);
  assert.match(content, /## 5\. What was NOT verified/);
  // Any mention of Preview/Preprod must be labelled testnet in the same line.
  for (const line of content.split("\n")) {
    if (/\b(Preview|Preprod)\b/.test(line)) assert.match(line, /testnet/, "unlabelled Preview/Preprod: " + line);
  }
});

test("every number the GROK-12 doc quotes from our run appears in the captured evidence", () => {
  const content = doc();
  const ev = evidence() + readFileSync(join(root, evidenceDir, "compose.hostnet.override.yml"), "utf8");
  for (const value of quoted) {
    assert.ok(content.includes(value), docPath + " should quote " + value);
    assert.ok(ev.includes(value), value + " is quoted in the doc but missing from " + evidenceDir);
  }
});

test("the GROK-12 evidence carries no secrets or personal info markers", () => {
  const ev = evidence();
  assert.doesNotMatch(ev, /mnemonic|private[_ ]key|BLOCKFROST_PROJECT_ID=|APP__INFRA__SECRET/i);
  assert.doesNotMatch(ev, /\b[0-9a-f]{64}\b(?=.*seed)/i);
});
