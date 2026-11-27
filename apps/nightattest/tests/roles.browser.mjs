import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { after, before, test } from "node:test";
import { chromium } from "playwright";
import { preview } from "vite";

let server;
let browser;
let url;
const screenshots = process.env.NIGHTATTEST_SCREENSHOTS;

before(async () => {
  server = await preview({ preview: { host: "127.0.0.1", port: 0 } });
  const { port } = server.httpServer.address();
  url = `http://127.0.0.1:${port}/`;
  browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH });
  if (screenshots) await mkdir(screenshots, { recursive: true });
});

after(async () => {
  await browser?.close();
  await server?.close();
});

function role(page, name) {
  return page.getByRole("button", { name, exact: true }).click();
}

function click(page, name) {
  return page.getByRole("button", { name, exact: true }).click();
}

function message(page, text) {
  return page.getByText(text, { exact: false }).waitFor({ state: "visible" });
}

async function screenshot(page, filename) {
  if (screenshots) await page.screenshot({ path: join(screenshots, filename), fullPage: true });
}

test("three roles use the compiled simulator and preserve the ledger through rejection and revocation", async () => {
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
  const errors = [];
  const requests = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("request", (request) => requests.push(request.url()));
  try {
    await page.goto(url);
    await role(page, "Holder");
    await click(page, "Create simulated pass");
    const commitment = await page.getByLabel("Commitment to share with issuer").inputValue();
    assert.match(commitment, /^[0-9a-f]{64}$/);

    await role(page, "Verifier");
    await click(page, "Issue fresh challenge");
    const first = await page.getByLabel("Current simulated challenge").inputValue();
    assert.match(first, /^[0-9a-f]{64}$/);
    await click(page, "Check simulated result");
    await message(page, "Pending in simulation");

    await role(page, "Holder");
    await click(page, "Use verifier challenge");
    await click(page, "Simulate presentation");
    await message(page, "unregistered or revoked");

    await role(page, "Issuer");
    await click(page, "Use holder commitment");
    assert.equal(await page.getByLabel("Credential commitment").inputValue(), commitment);
    await click(page, "Register commitment");
    await message(page, "Commitment registered in the simulated ledger.");
    assert.equal(await page.locator("#active").textContent(), "1");
    await screenshot(page, "01-simulated-issuer.png");
    await click(page, "Register commitment");
    await message(page, "Credential commitment has already been issued");

    await role(page, "Holder");
    await click(page, "Simulate presentation");
    await message(page, "Simulated presentation accepted.");
    await screenshot(page, "02-simulated-holder.png");
    await click(page, "Simulate presentation");
    await message(page, "Verifier challenge has already been used");

    await role(page, "Verifier");
    await click(page, "Check simulated result");
    await message(page, "Accepted in simulation.");
    await screenshot(page, "03-simulated-verifier.png");

    await role(page, "Issuer");
    await click(page, "Revoke commitment");
    await message(page, "Commitment revoked in the simulated ledger.");
    assert.equal(await page.locator("#active").textContent(), "0");

    await role(page, "Verifier");
    await click(page, "Check simulated result");
    await message(page, "Accepted in simulation.");
    await click(page, "Issue fresh challenge");
    const second = await page.getByLabel("Current simulated challenge").inputValue();
    assert.notEqual(second, first);

    await role(page, "Holder");
    await page.getByLabel("Verifier challenge", { exact: true }).fill(first);
    await click(page, "Simulate presentation");
    await message(page, "Use the verifier’s current simulated challenge.");
    await click(page, "Use verifier challenge");
    await click(page, "Simulate presentation");
    await message(page, "unregistered or revoked");

    await role(page, "Verifier");
    await click(page, "Check simulated result");
    await message(page, "Pending in simulation");
    assert.equal(await page.locator("#accepted").textContent(), "1");
    assert.equal(await page.locator("#issued").textContent(), "1");
    assert.deepEqual(errors, []);
    assert.ok(requests.every((request) => request.startsWith(url)), "simulator made an external request");

    await page.reload();
    await page.getByRole("button", { name: "Register commitment" }).waitFor();
    await page.waitForFunction(() => document.getElementById("issued").textContent === "0");
    assert.equal(await page.locator("#accepted").textContent(), "0");
  } finally {
    await page.close();
  }
});

test("a narrow viewport keeps roles keyboard-accessible and attribution text exact", async () => {
  const page = await browser.newPage({ viewport: { width: 360, height: 900 }, isMobile: true, hasTouch: true });
  try {
    await page.goto(url);
    await page.keyboard.press("Tab");
    assert.equal(await page.locator(":focus").textContent(), "Skip to workspace");
    await page.getByRole("button", { name: "Verifier", exact: true }).focus();
    await page.keyboard.press("Enter");
    await click(page, "Issue fresh challenge");
    await message(page, "Fresh simulated challenge issued.");
    assert.equal(await page.getByRole("button", { name: "Verifier", exact: true }).getAttribute("aria-pressed"), "true");
    assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth));
    const footer = await page.locator("footer p").allTextContents();
    assert.deepEqual(footer, [
      "Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
      "Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
      "@kshot9000 · Independent builder — not affiliated with the Midnight Foundation or IOG.",
    ]);
    await screenshot(page, "04-simulated-verifier-mobile.png");
  } finally {
    await page.close();
  }
});
