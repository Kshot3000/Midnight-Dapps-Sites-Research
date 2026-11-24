import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";
import {
  NIGHT_DUST_RATIO,
  GENERATION_DECAY_RATE,
  TIME_TO_CAP_SECONDS,
  DUST_GRACE_PERIOD_SECONDS,
  STARS_PER_NIGHT,
  SPECKS_PER_DUST,
  SECONDS_PER_DAY,
  SOURCES,
  capSpecks,
  generationPerSecond,
  generatedSpecks,
  secondsToCapFrom,
  secondsToZero,
  decayedSpecks,
  remainingAfterSpend,
  plan,
  parseNight,
  parseDust,
  parseDaysToSeconds,
  formatDust,
  formatSeconds,
} from "../apps/dust-planner/dust-model.js";
import {
  OUTPUT_PLACEHOLDER,
  showErrors,
  resetOutputs,
} from "../apps/dust-planner/planner.js";

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
  assert.equal(DUST_GRACE_PERIOD_SECONDS, 10800n);
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

test("the official 3-hour grace period is a transaction validity window, not a decay delay", () => {
  const model = read("apps/dust-planner/dust-model.js");
  assert.equal(DUST_GRACE_PERIOD_SECONDS, 10800n);
  assert.equal(DUST_GRACE_PERIOD_SECONDS, 3n * 3600n);

  const night = 100n * STARS_PER_NIGHT;
  const cap = capSpecks(night);
  const rate = generationPerSecond(night);
  const atCap = plan({
    nightStars: night,
    registered: true,
    secondsElapsed: TIME_TO_CAP_SECONDS,
    plannedSpendSpecks: 0n,
  });

  // Official manage-dust decay example: value at spentAt is the cap, at
  // spentAt+1 is cap minus one second of rate, and at 2*spentAt is zero.
  // There is no grace-period offset on those times.
  assert.equal(decayedSpecks(night, cap, 0n), cap);
  assert.equal(decayedSpecks(night, cap, 1n), cap - rate);
  assert.equal(decayedSpecks(night, cap, 2n), cap - 2n * rate);
  assert.equal(decayedSpecks(night, cap, TIME_TO_CAP_SECONDS), 0n);
  assert.equal(decayedSpecks(night, cap, TIME_TO_CAP_SECONDS * 2n), 0n);
  assert.equal(decayedSpecks(night, cap, DUST_GRACE_PERIOD_SECONDS), cap - rate * DUST_GRACE_PERIOD_SECONDS);
  assert.notEqual(decayedSpecks(night, cap, DUST_GRACE_PERIOD_SECONDS), cap);

  assert.equal(atCap.decaySecondsIfNightSpentNow, TIME_TO_CAP_SECONDS);
  assert.equal(atCap.decaySecondsIfNightSpentNow, secondsToZero(night, cap));
  assert.notEqual(atCap.decaySecondsIfNightSpentNow, TIME_TO_CAP_SECONDS + DUST_GRACE_PERIOD_SECONDS);

  const mid = plan({
    nightStars: night,
    registered: true,
    secondsElapsed: SECONDS_PER_DAY,
    plannedSpendSpecks: 0n,
  });
  const midDecay = secondsToZero(night, mid.currentSpecks);
  assert.equal(mid.decaySecondsIfNightSpentNow, midDecay);
  assert.equal(decayedSpecks(night, mid.currentSpecks, midDecay), 0n);
  assert.notEqual(mid.decaySecondsIfNightSpentNow, midDecay + DUST_GRACE_PERIOD_SECONDS);

  assert.match(model, /does not add DUST_GRACE_PERIOD_SECONDS to decay/);
  assert.match(model, /maximum age, measured against the block time/);
  assert.match(page, /dustGracePeriodSeconds/);
  assert.match(page, /not a delay before decay/);
  assert.match(page, /10,800 seconds \(3 hours\)/);
  assert.match(appReadme, /dustGracePeriodSeconds/);
  assert.match(appReadme, /Why decay timing does not apply the grace period/);
  assert.match(appReadme, /transaction timestamp validity window, not a delay before decay/);
  assert.equal(script.includes("innerHTML"), false);
});

test("parsers accept official units and reject invalid input", () => {
  assert.equal(parseNight("100"), 100n * STARS_PER_NIGHT);
  assert.equal(parseNight("1.5"), 1_500_000n);
  assert.equal(parseDust("20"), 20n * SPECKS_PER_DUST);
  assert.equal(parseDaysToSeconds("7"), 604800n);
  assert.equal(parseDaysToSeconds("0"), 0n);
  assert.throws(() => parseNight(""), /Enter a NIGHT amount/);
  assert.throws(() => parseDaysToSeconds(""), /Enter days since registration/);
  assert.throws(() => parseNight("1.1234567"), /at most 6 decimal places/);
});

test("parsers use a distinct message for malformed input vs a negative number", () => {
  const negative = /must be a non-negative number/;
  const malformed = /is not a valid number/;
  assert.throws(() => parseNight("-1"), negative);
  assert.throws(() => parseDust("-0.5"), negative);
  assert.throws(() => parseDaysToSeconds("-1"), negative);
  assert.throws(() => parseNight("1e2"), malformed);
  assert.throws(() => parseDust("1e2"), malformed);
  assert.throws(() => parseDaysToSeconds("1e2"), malformed);
  assert.throws(() => parseNight("abc"), malformed);
  const negativeMessage = (() => {
    try {
      parseNight("-1");
    } catch (error) {
      return error.message;
    }
  })();
  const malformedMessage = (() => {
    try {
      parseNight("1e2");
    } catch (error) {
      return error.message;
    }
  })();
  assert.notEqual(negativeMessage, malformedMessage);
  assert.match(negativeMessage, /NIGHT amount must be a non-negative number/);
  assert.match(malformedMessage, /NIGHT amount is not a valid number/);
  assert.doesNotMatch(malformedMessage, /non-negative/);
});

test("formatSeconds omits a parenthetical that only repeats the same duration", () => {
  assert.equal(formatSeconds(0n), "0 seconds");
  assert.equal(formatSeconds(1n), "1 second");
  assert.equal(formatSeconds(15n), "15 seconds");
  assert.equal(formatSeconds(60n), "60 seconds (1 minute)");
  assert.equal(formatSeconds(90n), "90 seconds (1 minute, 30 seconds)");
  assert.equal(formatSeconds(86400n), "86400 seconds (1 day)");
  assert.equal(formatSeconds(TIME_TO_CAP_SECONDS), "604815 seconds (7 days, 15 seconds)");
  assert.doesNotMatch(formatSeconds(15n), /\(.*\)/);
});

test("showErrors builds the list with textContent, not innerHTML", () => {
  assert.equal(script.includes("innerHTML"), false);
  const created = [];
  const element = (tag) => {
    const node = {
      tagName: tag,
      textContent: "",
      children: [],
      append(child) {
        this.children.push(child);
      },
    };
    created.push(node);
    return node;
  };
  const container = {
    hidden: true,
    className: "",
    attrs: {},
    children: [],
    setAttribute(name, value) {
      this.attrs[name] = value;
    },
    replaceChildren(...nodes) {
      this.children = nodes;
    },
  };
  const payload = "<img src=x onerror=alert(1)>";
  showErrors(container, [payload, "Second message"], element);
  assert.equal(container.hidden, false);
  assert.equal(container.className, "error-list");
  assert.equal(container.attrs.role, "alert");
  assert.equal(created[0].tagName, "ul");
  assert.equal(created[1].tagName, "li");
  assert.equal(created[1].textContent, payload);
  assert.equal(created[2].textContent, "Second message");
  assert.equal(container.children[0], created[0]);
  assert.equal(created[0].children[0].textContent, payload);
  assert.equal(typeof container.innerHTML, "undefined");
});

test("invalid input resets estimate outputs to the placeholder", () => {
  const ui = {
    unregisteredAlert: { hidden: false },
    outCurrent: { textContent: "500 DUST (modelled estimate)" },
    outCap: { textContent: "500 DUST (modelled estimate)" },
    outRate: { textContent: "0.0008267 DUST/s" },
    outToCap: { textContent: "Already at cap (modelled estimate)" },
    outRemaining: { textContent: "480 DUST (modelled estimate)" },
    outDecay: { textContent: "604815 seconds (7 days, 15 seconds) to zero (modelled estimate)" },
    outSummary: { textContent: "Every figure on this panel is a modelled estimate." },
  };
  resetOutputs(ui);
  assert.equal(ui.unregisteredAlert.hidden, true);
  for (const key of ["outCurrent", "outCap", "outRate", "outToCap", "outRemaining", "outDecay"]) {
    assert.equal(ui[key].textContent, OUTPUT_PLACEHOLDER);
  }
  assert.equal(ui.outSummary.textContent, "");
  assert.equal(page.includes(`>${OUTPUT_PLACEHOLDER}<`) || page.includes(">—</"), true);
  assert.match(script, /resetOutputs\(ui\)/);
});


test("the planner is in the same visual family as the hub and stays keyboard-usable", () => {
  assert.match(css, /--background: #090b15;/);
  assert.match(css, /--accent: #b7c6ff;/);
  assert.match(page, /class="skip-link" href="#main"/);
  assert.match(page, /<main id="main" tabindex="-1">/);
  assert.match(css, /min-height: 2\.75rem/);
  assert.match(css, /:focus-visible/);
});

test("the wordmark accessible name includes its visible text", () => {
  const wordmark = page.match(/<a\s[^>]*class="[^"]*\bwordmark\b[^"]*"[^>]*>[\s\S]*?<\/a>/)?.[0];
  assert.ok(wordmark, "missing .wordmark link");
  const decode = (value) =>
    value
      .replace(/&amp;/g, "&")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&quot;/g, '"')
      .replace(/&#39;|&apos;/g, "'");
  const withoutHidden = wordmark.replace(
    /<[^>]*\baria-hidden="true"[^>]*>[\s\S]*?<\/[^>]+>/g,
    " ",
  );
  const visible = decode(withoutHidden.replace(/<[^>]+>/g, " ")).replace(/\s+/g, " ").trim();
  assert.ok(visible.length > 0, "wordmark has no visible text");
  const labelled = wordmark.match(/\baria-label="([^"]*)"/)?.[1];
  const accessibleName = labelled
    ? decode(labelled).replace(/\s+/g, " ").trim()
    : visible;
  assert.ok(
    accessibleName.includes(visible),
    `wordmark accessible name "${accessibleName}" does not include visible text "${visible}"`,
  );
});

test("the results panel has a role that allows aria-labelledby", () => {
  const open = page.match(/<([a-zA-Z0-9]+)([^>]*\bclass="[^"]*\bresults-panel\b[^"]*"[^>]*)>/);
  assert.ok(open, "missing .results-panel");
  const tag = open[1].toLowerCase();
  const attrs = open[2];
  assert.match(attrs, /\baria-labelledby\s*=/, "results panel should keep aria-labelledby");
  const role = attrs.match(/\brole="([^"]+)"/)?.[1]?.toLowerCase();
  const implicitNamedRegions = new Set([
    "section",
    "article",
    "aside",
    "nav",
    "main",
    "header",
    "footer",
    "form",
  ]);
  const explicitNamedRegions = new Set([
    "region",
    "group",
    "complementary",
    "navigation",
    "main",
    "banner",
    "contentinfo",
    "form",
    "search",
    "article",
  ]);
  assert.ok(
    implicitNamedRegions.has(tag) || (role && explicitNamedRegions.has(role)),
    `results panel <${tag}${role ? ` role="${role}"` : ""}> has no role that allows a name via aria-labelledby`,
  );
});
