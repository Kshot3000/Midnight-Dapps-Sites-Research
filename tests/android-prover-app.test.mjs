import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-17: guards for the Android prover app's in-app attribution, Tip
// wording and network policy. Values come from the project brief,
// independently of the app files.
const root = fileURLToPath(new URL("../", import.meta.url));
const app = "apps/android-prover";
const src = join(root, app, "app/src/main/java/tech/kshot/midnightprover");
const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";
const read = (p) => readFileSync(join(root, p), "utf8");
const allKotlin = () => readdirSync(src).filter((f) => f.endsWith(".kt")).map((f) => readFileSync(join(src, f), "utf8")).join("\n");

test("the app's in-app attribution has both exact addresses, @kshot9000 and the independence line", () => {
  const attribution = read(join(app, "app/src/main/java/tech/kshot/midnightprover/ProofJob.kt"));
  for (const [network, address] of Object.entries(addresses)) {
    assert.ok(attribution.includes('"' + address + '"'), "in-app " + network + " address must match character for character");
  }
  assert.ok(attribution.includes('"@kshot9000"'));
  assert.ok(attribution.includes('"' + affiliation + '"'));
});

test("both app READMEs carry both exact addresses and the independence line", () => {
  for (const p of [join(app, "README.md"), join(app, "server-setup/README.md")]) {
    const content = read(p);
    for (const [network, address] of Object.entries(addresses)) {
      assert.match(content, new RegExp("(?<![\\w-])" + address + "(?![\\w-])"), p + " must show the complete " + network + " address");
    }
    assert.match(content, /@kshot9000\b/);
    assert.ok(content.includes(affiliation), p + " is missing the independence line");
  }
});

test("the app and its READMEs say Tip / tip addresses, never Donate / donation addresses", () => {
  const code = allKotlin();
  assert.doesNotMatch(code, /donat/i);
  assert.match(code, /Text\("Tip"/);
  assert.doesNotMatch(read(join(app, "server-setup/README.md")), /donat/i);
  assert.doesNotMatch(read(join(app, "README.md")), /donat/i);
});

test("cleartext HTTP is allowed only to this phone (P0-5)", () => {
  const nsc = read(join(app, "app/src/main/res/xml/network_security_config.xml"));
  assert.match(nsc, /<base-config cleartextTrafficPermitted="false"/);
  const domains = [...nsc.matchAll(/<domain[^>]*>([^<]+)<\/domain>/g)].map((m) => m[1]).sort();
  assert.deepEqual(domains, ["127.0.0.1", "localhost"]);
  const manifest = read(join(app, "app/src/main/AndroidManifest.xml"));
  assert.match(manifest, /android:networkSecurityConfig="@xml\/network_security_config"/);
  assert.doesNotMatch(manifest, /usesCleartextTraffic="true"/);
});

test("the app expects proof server 9.0.2 and the bundled sample payload is the real 1,509-byte fixture", () => {
  assert.match(read(join(app, "app/src/main/java/tech/kshot/midnightprover/ProofJob.kt")), /EXPECTED_SERVER_VERSION = "9\.0\.2"/);
  const hex = read(join(app, "app/src/main/assets/sample-dust-spend-payload.hex")).trim();
  assert.equal(hex.length / 2, 1509);
  assert.ok(Buffer.from(hex, "hex").toString("latin1").includes("midnight/dust/spend"));
  assert.equal(hex, read(join(app, "app/src/test/resources/fixtures/dust-spend-payload.hex")).trim());
});

test("the app is labelled testnet only and doesn't overclaim (GROK-17)", () => {
  const code = allKotlin();
  assert.match(code, /TESTNET_CHIP = "Testnet only · Preview \/ Preprod"/);
  assert.doesNotMatch(code, /Prove Midnight transactions on this device/);
  assert.doesNotMatch(code, /"Prove transaction"/);
  assert.doesNotMatch(code, /this session/i);
});

test("both setup READMEs carry the plain Wi-Fi exposure warning", () => {
  for (const p of [join(app, "README.md"), join(app, "server-setup/README.md")]) {
    const content = read(p);
    assert.match(content, /no\s+bind-address\s+option/i, p);
    assert.match(content, /0\.0\.0\.0/, p);
    assert.match(content, /same\s+Wi-Fi/i, p);
  }
});
