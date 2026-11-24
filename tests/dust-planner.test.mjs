import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";
import {
  NIGHT_DUST_RATIO,
  GENERATION_DECAY_RATE,
  TIME_TO_CAP_SECONDS,
  STARS_PER_NIGHT,
  SPECKS_PER_DUST,
  SECONDS_PER_DAY,
  SOURCES,
  capSpecks,
  generationPerSecond,
  generatedSpecks,
  secondsToCapFrom,
  remainingAfterSpend,
  plan,
  parseNight,
  parseDust,
  parseDaysToSeconds,
  formatDust,
} from "../apps/dust-planner/dust-model.js";

const root = fileURLToPath(new URL("../", import.meta.url));
const read = (path) => readFileSync(join(root, path), "utf8");
const page = read("apps/dust-planner/index.html");
const css = read("apps/dust-planner/planner.css");
const script = read("apps/dust-planner/planner.js");
const appReadme = read("apps/dust-planner/README.md");

const addresses = {
  cardano: "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v",
  midnight: "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q",
};
const affiliation = "Independent builder — not affiliated with the Midnight Foundation or IOG.";

test("planner files exist as a static app with no build step", () => {
  for (const path of [
    "apps/dust-planner/index.html",
    "apps/dust-planner/planner.css",
    "apps/dust-planner/planner.js",
    "apps/dust-planner/dust-model.js",
    "apps/dust-planner/README.md",
    "apps/dust-planner/favicon.png",
  ]) {
    assert.ok(existsSync(join(root, path)), path);
  }
  assert.match(page, /<script defer type="module" src="\.\/planner\.js\?v=1">/);
  assert.match(page, /<link rel="stylesheet" href="\.\/planner\.css\?v=1">/);
});

test("the planner footer has both exact donation addresses and the independence line", () => {
  const footer = page.match(/<footer\b[^>]*>([\s\S]*?)<\/footer>/i)?.[1];
  assert.ok(footer, "planner page is missing a footer");
  const footerText = footer.replace(/<[^>]*>/g, " ");
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(footerText, exactAddress, "footer must show the complete " + network + " address");
  }
  assert.match(footerText, /@kshot9000\b/, "footer needs the builder’s profile");
  assert.ok(footerText.includes(affiliation), "footer is missing the independence line");
});

test("planner copy buttons reference the exact visible address text", () => {
  for (const [network, address] of Object.entries(addresses)) {
    const codeId = network + "-address";
    const code = page.match(new RegExp('<code id="' + codeId + '">([^<]*)</code>'))?.[1];
    assert.equal(code, address);
    assert.match(page, new RegExp('<button[^>]*data-copy="' + codeId + '"[^>]*>'));
  }
  assert.match(page, /<a href="https:\/\/x\.com\/kshot9000">@kshot9000<\/a>/);
});

test("the planner README carries both exact addresses and the independence line", () => {
  for (const [network, address] of Object.entries(addresses)) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(appReadme, exactAddress, "README must show the complete " + network + " address");
  }
  assert.match(appReadme, /@kshot9000\b/);
  assert.ok(appReadme.includes(affiliation));
});

test("every output on the planner is labelled as a modelled estimate", () => {
  assert.match(page, /Every figure below is a modelled estimate/);
  assert.match(page, /a modelled estimate, never a promise/);
  assert.match(page, /This is a static planner/);
  assert.match(page, /does not connect to a wallet/);
  assert.match(page, /Preview &amp; Preprod are testnets/);
  assert.match(page, /Model only · testnet tNIGHT \/ tDUST/);
  assert.match(page, /id="estimate-banner"/);
  assert.equal((page.match(/modelled estimate/gi) || []).length >= 6, true);
  assert.match(script, /modelled estimate/);
});

test("the planner asks for the four specified inputs and warns when unregistered", () => {
  assert.match(page, /<input id="night-amount"/);
  assert.match(page, /<input id="registered"[^>]*type="checkbox"/);
  assert.match(page, /<input id="days-registered"/);
  assert.match(page, /<input id="planned-spend"/);
  assert.match(page, /Unregistered NIGHT generates no DUST/);
  assert.match(page, /id="unregistered-alert"/);
  assert.match(page, /Fees are paid in DUST/);
});

test("official ledger 9 sources are cited in the app and the model", () => {
  for (const url of [SOURCES.architecture, SOURCES.manageDust, SOURCES.ledger9]) {
    assert.ok(page.includes(url), "page should cite " + url);
    assert.ok(appReadme.includes(url), "README should cite " + url);
  }
  assert.equal(NIGHT_DUST_RATIO, 5_000_000_000n);
  assert.equal(GENERATION_DECAY_RATE, 8267n);
  assert.equal(TIME_TO_CAP_SECONDS, 604815n);
  assert.equal(STARS_PER_NIGHT, 1_000_000n);
  assert.equal(SPECKS_PER_DUST, 1_000_000_000_000_000n);
});

test("cap is 5 DUST per 1 NIGHT, matching the official examples", () => {
  const oneNight = STARS_PER_NIGHT;
  const thousandNight = 1_000n * STARS_PER_NIGHT;
  assert.equal(capSpecks(oneNight), 5n * SPECKS_PER_DUST);
  assert.equal(capSpecks(100n * STARS_PER_NIGHT), 500n * SPECKS_PER_DUST);
  assert.equal(capSpecks(thousandNight), 5000n * SPECKS_PER_DUST);
  assert.equal(formatDust(capSpecks(thousandNight)), "5000");
});

test("linear ramp matches the official 1,000 NIGHT manage-dust example", () => {
  const night = 1_000n * STARS_PER_NIGHT;
  const rate = generationPerSecond(night);
  assert.equal(rate, 8_267_000_000_000n);
  assert.equal(formatDust(rate), "0.008267");
  assert.equal(generatedSpecks(night, 1n, true), rate);
  assert.equal(generatedSpecks(night, TIME_TO_CAP_SECONDS - 1n, true) < capSpecks(night), true);
  assert.equal(generatedSpecks(night, TIME_TO_CAP_SECONDS, true), capSpecks(night));
  assert.equal(generatedSpecks(night, TIME_TO_CAP_SECONDS * 2n, true), capSpecks(night));
  assert.equal(secondsToCapFrom(night, 0n), TIME_TO_CAP_SECONDS);
});

test("linear ramp matches the official 100 NIGHT architecture example", () => {
  const night = 100n * STARS_PER_NIGHT;
  const perSecond = generationPerSecond(night);
  const perDay = perSecond * SECONDS_PER_DAY;
  assert.equal(perSecond, 826_700_000_000n);
  assert.equal(formatDust(perSecond), "0.0008267");
  assert.equal(formatDust(perDay), "71.42688");
  assert.equal(generatedSpecks(night, TIME_TO_CAP_SECONDS, true), 500n * SPECKS_PER_DUST);
  const sevenDays = 7n * SECONDS_PER_DAY;
  assert.equal(sevenDays, 604800n);
  const almostCap = generatedSpecks(night, sevenDays, true);
  assert.equal(almostCap < capSpecks(night), true);
  assert.equal(almostCap > 499n * SPECKS_PER_DUST, true);
});

test("time to cap is the same for any NIGHT amount", () => {
  for (const amount of [1n, 100n, 1000n, 12_345n]) {
    const night = amount * STARS_PER_NIGHT;
    assert.equal(secondsToCapFrom(night, 0n), TIME_TO_CAP_SECONDS);
  }
});

test("unregistered NIGHT generates nothing at any elapsed time", () => {
  const night = 100n * STARS_PER_NIGHT;
  assert.equal(generatedSpecks(night, TIME_TO_CAP_SECONDS, false), 0n);
  assert.equal(generatedSpecks(night, 0n, true), 0n);
  const result = plan({
    nightStars: night,
    registered: false,
    secondsElapsed: TIME_TO_CAP_SECONDS,
    plannedSpendSpecks: 20n * SPECKS_PER_DUST,
  });
  assert.equal(result.currentSpecks, 0n);
  assert.equal(result.remainingAfterSpendSpecks, 0n);
  assert.equal(result.shortfallSpecks, 20n * SPECKS_PER_DUST);
  assert.equal(result.capSpecks, 500n * SPECKS_PER_DUST);
});

test("planned fee spend and decay follow official linear rules", () => {
  const night = 100n * STARS_PER_NIGHT;
  const atCap = plan({
    nightStars: night,
    registered: true,
    secondsElapsed: TIME_TO_CAP_SECONDS,
    plannedSpendSpecks: 20n * SPECKS_PER_DUST,
  });
  assert.equal(atCap.currentSpecks, 500n * SPECKS_PER_DUST);
  assert.equal(atCap.remainingAfterSpendSpecks, 480n * SPECKS_PER_DUST);
  assert.equal(atCap.shortfallSpecks, 0n);
  assert.equal(atCap.atCap, true);
  assert.equal(atCap.decaySecondsIfNightSpentNow, TIME_TO_CAP_SECONDS);
  assert.equal(remainingAfterSpend(5n * SPECKS_PER_DUST, 8n * SPECKS_PER_DUST), 0n);
});

test("parsers accept official units and reject invalid input", () => {
  assert.equal(parseNight("100"), 100n * STARS_PER_NIGHT);
  assert.equal(parseNight("1.5"), 1_500_000n);
  assert.equal(parseDust("20"), 20n * SPECKS_PER_DUST);
  assert.equal(parseDaysToSeconds("7"), 604800n);
  assert.equal(parseDaysToSeconds("0"), 0n);
  assert.throws(() => parseNight(""), /Enter a NIGHT amount/);
  assert.throws(() => parseNight("-1"), /non-negative/);
  assert.throws(() => parseNight("1.1234567"), /at most 6 decimal places/);
  assert.throws(() => parseDust("1e2"), /non-negative number/);
});

test("the planner is in the same visual family as the hub and stays keyboard-usable", () => {
  assert.match(css, /--background: #090b15;/);
  assert.match(css, /--accent: #b7c6ff;/);
  assert.match(page, /class="skip-link" href="#main"/);
  assert.match(page, /<main id="main" tabindex="-1">/);
  assert.match(css, /min-height: 2\.75rem/);
  assert.match(css, /:focus-visible/);
});
