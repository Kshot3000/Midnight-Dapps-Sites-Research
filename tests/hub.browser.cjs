const assert = require("node:assert/strict");
const fs = require("node:fs");
const http = require("node:http");
const path = require("node:path");
const { chromium } = require("playwright");

const root = path.resolve(__dirname, "../sites/hub");
const previews = path.resolve(process.argv[2] || "hub-previews");
fs.mkdirSync(previews, { recursive: true });
const prefix = "/Midnight-Dapps-Sites-Research/";
const types = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css",
  ".js": "text/javascript",
  ".webp": "image/webp",
  ".png": "image/png",
};
const server = http.createServer((request, response) => {
  const pathname = new URL(request.url, "http://localhost").pathname;
  if (!pathname.startsWith(prefix)) {
    response.writeHead(404).end();
    return;
  }
  const filename = path.resolve(root, pathname.slice(prefix.length) || "index.html");
  if (!filename.startsWith(root + path.sep) || !fs.existsSync(filename)) {
    response.writeHead(404).end();
    return;
  }
  response.writeHead(200, { "Content-Type": types[path.extname(filename)] });
  fs.createReadStream(filename).pipe(response);
});

async function verify() {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = "http://127.0.0.1:" + server.address().port + prefix;
  const browser = await chromium.launch();

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1050 } });
    await context.grantPermissions(["clipboard-read", "clipboard-write"]);
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (error) => errors.push(error.message));
    page.on("response", (response) => {
      if (response.url().startsWith(origin) && !response.ok()) errors.push(response.url() + " " + response.status());
    });
    await page.goto(origin, { waitUntil: "networkidle" });
    const cards = page.locator("[data-project]:visible");

    assert.equal(await cards.count(), 10);
    assert.equal(await page.locator("#result-count").innerText(), "Showing 10 of 10 projects");
    await page.screenshot({ path: path.join(previews, "sol-1-desktop.png") });
    await page.keyboard.press("Tab");
    assert.equal(await page.evaluate(() => document.activeElement.className), "skip-link");
    await page.keyboard.press("Enter");
    assert.equal(await page.evaluate(() => document.activeElement.id), "main");

    await page.getByRole("link", { name: "Explore Proof Station" }).click();
    assert.equal(await page.evaluate(() => location.hash), "#proof-station");
    const cpu = page.getByRole("button", { name: "CPU", exact: true });
    const cuda = page.getByRole("button", { name: "NVIDIA CUDA", exact: true });
    assert.equal(await cpu.getAttribute("aria-pressed"), "true");
    assert.equal(await page.locator("#proof-preview-cpu").isVisible(), true);
    assert.equal(await page.locator("#proof-preview-cuda").isVisible(), false);
    await cuda.focus();
    await page.keyboard.press("Space");
    assert.equal(await cuda.getAttribute("aria-pressed"), "true");
    assert.equal(await cpu.getAttribute("aria-pressed"), "false");
    assert.equal(await page.locator("#proof-preview-cpu").isVisible(), false);
    assert.equal(await page.locator("#proof-preview-cuda").isVisible(), true);
    await page.waitForFunction(() => document.querySelector("#proof-preview-cuda img").naturalWidth === 1028);
    await page.screenshot({ path: path.join(previews, "proof-station-cuda.png") });
    await cpu.click();
    assert.equal(await cpu.getAttribute("aria-pressed"), "true");
    await page.waitForFunction(() => document.querySelector("#proof-preview-cpu img").naturalWidth === 1028);
    assert.equal(await page.locator("#proof-preview-cuda").isVisible(), false);
    await page.screenshot({ path: path.join(previews, "proof-station-cpu.png") });
    console.log("Proof Station: hero link, CPU and CUDA screenshots, and keyboard switching verified.");

    const auction = page.locator('[data-project="sealed-bid-auction"]');
    assert.equal(await auction.locator(".primitives").isVisible(), false);
    await auction.locator("summary").click();
    assert.equal(await auction.locator(".primitives").isVisible(), true);
    await auction.locator("summary").click();
    assert.equal(await auction.locator(".primitives").isVisible(), false);

    await page.getByRole("button", { name: "Credentials", exact: true }).click();
    assert.equal(await cards.count(), 3);
    assert.equal(await page.getByRole("button", { name: "Credentials", exact: true }).getAttribute("aria-pressed"), "true");
    const statusFilter = page.getByRole("combobox", { name: "Project status" });
    await statusFilter.selectOption("spec");
    assert.equal(await cards.count(), 1);
    assert.equal(await cards.first().getAttribute("data-project"), "nightattest");
    await page.getByRole("searchbox").fill("sEaLeD");
    assert.equal(await cards.count(), 0);
    assert.equal(await page.locator("#empty-state").isVisible(), true);
    await page.getByRole("button", { name: "Clear filters" }).click();
    assert.equal(await cards.count(), 10);
    assert.equal(await page.evaluate(() => document.activeElement.id), "project-search");
    await page.getByRole("searchbox").fill("sealed-bid");
    assert.equal(await cards.count(), 1);
    assert.equal(await cards.first().getAttribute("data-project"), "sealed-bid-auction");
    await page.getByRole("searchbox").fill("");
    await statusFilter.selectOption("local-tool");
    assert.equal(await cards.count(), 1);
    assert.equal(await cards.first().getAttribute("data-project"), "dust-planner-pro");
    assert.equal(await cards.first().getByRole("link", { name: "Run locally" }).isVisible(), true);
    await statusFilter.selectOption("all");
    assert.equal(await cards.count(), 10);

    const footer = page.locator("footer");
    for (const [handle, url] of [
      ["@MidnightNtwrk", "https://x.com/MidnightNtwrk"],
      ["@IOHK_Charles", "https://x.com/IOHK_Charles"],
      ["@midnightntwrk", "https://github.com/midnightntwrk"],
      ["@CharlesHoskinson", "https://github.com/CharlesHoskinson"],
    ]) {
      const credit = footer.getByRole("link", { name: handle, exact: true });
      assert.equal(await credit.isVisible(), true);
      assert.equal(await credit.getAttribute("href"), url);
    }
    await footer.screenshot({ path: path.join(previews, "hub-credits.png") });
    for (const network of ["cardano", "midnight"]) {
      const address = await page.locator("#" + network + "-address").innerText();
      await page.locator('[data-copy="' + network + '-address"]').click();
      await page.waitForFunction(() => document.querySelector("#copy-feedback").textContent.endsWith("address copied."));
      assert.equal(await page.evaluate(() => navigator.clipboard.readText()), address);
    }
    assert.deepEqual(errors, []);
    console.log("Desktop: 10 cards, keyboard skip link, combined filters, Local tool status, footer credits, and both clipboards verified.");

    for (const width of [320, 390, 768, 1024, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      const expectedImage = width <= 704 ? "eclipse-mobile.webp" : "eclipse.webp";
      await page.waitForFunction((asset) => {
        const image = document.querySelector(".hero-art img");
        return image.complete && image.naturalWidth > 0 && image.currentSrc.endsWith("/" + asset);
      }, expectedImage);
      const extents = await page.evaluate(() => ({
        inner: window.innerWidth,
        page: document.documentElement.scrollWidth,
        wallet: [...document.querySelectorAll(".wallet code")].map((node) => node.scrollWidth <= node.clientWidth),
      }));
      assert.equal(extents.page, extents.inner, "No horizontal overflow at " + width);
      assert.ok(extents.wallet.every(Boolean), "Wallet addresses must fit at " + width);
    }
    console.log("Layout: 320, 390, 768, 1024, and 1440px without horizontal overflow or truncated addresses.");

    const mobile = await browser.newContext({
      viewport: { width: 390, height: 970 },
      isMobile: true,
      hasTouch: true,
      deviceScaleFactor: 1,
      reducedMotion: "reduce",
    });
    const phone = await mobile.newPage();
    await phone.goto(origin, { waitUntil: "networkidle" });
    assert.ok((await phone.locator(".hero-art img").evaluate((img) => img.currentSrc)).endsWith("eclipse-mobile.webp"));
    await phone.screenshot({ path: path.join(previews, "sol-1-mobile.png") });
    await phone.getByRole("link", { name: "Explore the catalogue" }).tap();
    assert.equal(await phone.evaluate(() => location.hash), "#catalogue");
    await phone.getByRole("button", { name: "Tools", exact: true }).tap();
    assert.equal(await phone.locator("[data-project]:visible").count(), 4);
    await phone.getByRole("link", { name: "Explore Proof Station", exact: true }).tap();
    assert.equal(await phone.evaluate(() => location.hash), "#proof-station");
    const phoneCuda = phone.getByRole("button", { name: "NVIDIA CUDA", exact: true });
    await phoneCuda.tap();
    assert.equal(await phoneCuda.getAttribute("aria-pressed"), "true");
    await phone.waitForFunction(() => document.querySelector("#proof-preview-cuda img").naturalWidth === 1028);
    await phone.screenshot({ path: path.join(previews, "proof-station-mobile.png") });
    console.log("Mobile: hero image, navigation, touch filtering, and CUDA preview verified.");

    const rejected = await browser.newContext();
    await rejected.addInitScript(() => {
      Object.defineProperty(navigator, "clipboard", {
        value: { writeText: () => Promise.reject(new DOMException("Clipboard denied", "NotAllowedError")) },
      });
    });
    const fallback = await rejected.newPage();
    await fallback.goto(origin);
    await fallback.getByRole("button", { name: "Copy address for the Midnight shielded donation" }).click();
    assert.equal(await fallback.evaluate(() => window.getSelection().toString()), await fallback.locator("#midnight-address").innerText());
    assert.match(await fallback.locator("#copy-feedback").innerText(), /address selected/);

    const staticContext = await browser.newContext({ javaScriptEnabled: false });
    const staticPage = await staticContext.newPage();
    await staticPage.goto(origin);
    assert.equal(await staticPage.locator("[data-project]:visible").count(), 10);
    assert.equal(await staticPage.locator(".wallet code:visible").count(), 2);
    assert.equal(await staticPage.locator("#catalogue-filters").isVisible(), false);
    assert.equal(await staticPage.locator("#proof-preview-cpu").isVisible(), true);
    assert.equal(await staticPage.locator("#proof-modes").isVisible(), false);
    assert.equal(await staticPage.getByRole("link", { name: "Download Windows x64 ZIP" }).isVisible(), true);
    console.log("Fallbacks: blocked clipboard selects the complete address. The catalogue, addresses, and CPU preview render without JavaScript.");
  } finally {
    await browser.close();
    server.close();
  }
}

verify().catch((error) => {
  console.error(error);
  server.close();
  process.exitCode = 1;
});
