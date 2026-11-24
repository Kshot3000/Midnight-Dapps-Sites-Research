import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";
import {
  AFFILIATION,
  CARDANO_ADDRESS,
  MIDNIGHT_ADDRESS,
  checkStructure,
  classifyFetch,
  extractHttpUrls,
  formatLiveReport,
  isCli,
  main,
  parseArgs,
  runLive,
  todayInChicago,
} from "../scripts/check-directory-links.mjs";

const root = fileURLToPath(new URL("../", import.meta.url));
const scriptPath = "scripts/check-directory-links.mjs";
const directoryPath = "research/ecosystem-directory.md";
const fixtures = join(root, "tests/fixtures/directory-link-checker");
const read = (path) => readFileSync(join(root, path), "utf8");
const readFixture = (name) => readFileSync(join(fixtures, name), "utf8");

test("the link-checker script carries both exact donation addresses, @kshot9000, and the independent-builder line", () => {
  const script = read(scriptPath);
  for (const [network, address] of [
    ["Cardano", CARDANO_ADDRESS],
    ["Midnight", MIDNIGHT_ADDRESS],
  ]) {
    const exactAddress = new RegExp("(?<![\\w-])" + address + "(?![\\w-])");
    assert.match(script, exactAddress, scriptPath + " must show the complete " + network + " address");
    assert.equal(script.includes(address), true, network + " address must appear verbatim");
  }
  assert.equal(CARDANO_ADDRESS, "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v");
  assert.equal(MIDNIGHT_ADDRESS, "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q");
  assert.match(script, /@kshot9000\b/, scriptPath + " needs the builder’s profile");
  assert.ok(script.includes(AFFILIATION), scriptPath + " is missing the independence line");
  assert.equal(AFFILIATION, "Independent builder — not affiliated with the Midnight Foundation or IOG.");
});

test("the structural check passes on a valid fixture, including a dead/stale hostname-only row", () => {
  const check = checkStructure(readFixture("valid.md"));
  assert.equal(check.ok, true, check.failures.join("\n"));
  assert.equal(check.rowCount, 4);
  assert.equal(check.withoutHttps.length, 1);
  assert.match(check.withoutHttps[0].label, /Retired host/);
  assert.deepEqual(
    check.rows.find((row) => row.label.includes("Partner docs")).httpsUrls,
    ["https://example.com/docs", "https://github.com/example/repo"],
  );
});

test("the structural check fails a row that is missing a privacy-pattern field", () => {
  const check = checkStructure(readFixture("missing-privacy.md"));
  assert.equal(check.ok, false);
  assert.ok(check.failures.some((failure) => /privacy-pattern/.test(failure)), check.failures.join("\n"));
});

test("the structural check fails a row that is missing a network field", () => {
  const check = checkStructure(readFixture("missing-network.md"));
  assert.equal(check.ok, false);
  assert.ok(check.failures.some((failure) => /network field/.test(failure)), check.failures.join("\n"));
});

test("the structural check fails a live catalogue row that has no https source URL", () => {
  const check = checkStructure(readFixture("missing-url.md"));
  assert.equal(check.ok, false);
  assert.ok(check.failures.some((failure) => /https source URL/.test(failure)), check.failures.join("\n"));
});

test("the structural check fails a row whose source URL is http rather than https", () => {
  const check = checkStructure(readFixture("http-url.md"));
  assert.equal(check.ok, false);
  assert.ok(check.failures.some((failure) => /non-https URL/.test(failure)), check.failures.join("\n"));
});

test("URL extraction strips trailing punctuation and ignores surrounding backticks", () => {
  const urls = extractHttpUrls(
    "See `https://docs.midnight.network/relnotes/network`. Also [docs](https://docs.midnight.network/)",
  );
  assert.deepEqual(urls, [
    "https://docs.midnight.network/",
    "https://docs.midnight.network/relnotes/network",
  ]);
});

test("fetch classification distinguishes ok, dead, and blocked (403/429) without calling the network", () => {
  assert.equal(classifyFetch({ status: 200 }), "ok");
  assert.equal(classifyFetch({ status: 301 }), "ok");
  assert.equal(classifyFetch({ status: 404 }), "dead");
  assert.equal(classifyFetch({ status: 410 }), "dead");
  assert.equal(classifyFetch({ status: 403 }), "blocked");
  assert.equal(classifyFetch({ status: 429 }), "blocked");
  assert.equal(classifyFetch({ status: 401 }), "blocked");
  assert.equal(classifyFetch({ status: 503 }), "blocked");
  assert.equal(classifyFetch({ errorCode: "TIMEOUT" }), "timeout");
  assert.equal(classifyFetch({ errorCode: "AbortError", errorMessage: "The operation was aborted" }), "timeout");
  assert.equal(classifyFetch({ errorCode: "ENOTFOUND", errorMessage: "getaddrinfo ENOTFOUND" }), "dead");
  assert.equal(classifyFetch({ errorCode: "ECONNREFUSED" }), "dead");
  assert.equal(classifyFetch({ errorCode: "UNABLE_TO_VERIFY_LEAF_SIGNATURE" }), "error");
  assert.equal(classifyFetch({ status: 500 }), "error");
});

test("default structural mode and unit tests never call fetch", async () => {
  let calls = 0;
  const original = globalThis.fetch;
  globalThis.fetch = async () => {
    calls += 1;
    throw new Error("network is not allowed in unit tests");
  };
  try {
    const check = checkStructure(read(directoryPath));
    assert.equal(check.ok, true, check.failures.join("\n"));
    const chunks = [];
    const code = await main(["node", "scripts/check-directory-links.mjs", join(fixtures, "valid.md")], {
      stdout: { write(chunk) { chunks.push(chunk); return true; } },
      stderr: { write() { return true; } },
      fetchImpl: async () => {
        calls += 1;
        throw new Error("network is not allowed in unit tests");
      },
    });
    assert.equal(code, 0);
    assert.match(chunks.join(""), /Result: PASS/);
    assert.equal(calls, 0, "structural mode must not call fetch");
  } finally {
    globalThis.fetch = original;
  }
});

test("live mode records mixed results through an injected fetch and does not hard-fail on blocked or dead URLs", async () => {
  const now = new Date("2026-11-24T17:00:00Z");
  const live = await runLive(
    [
      "https://example.com/ok",
      "https://example.com/gone",
      "https://example.com/blocked",
      "https://example.com/slow",
    ],
    {
      now,
      concurrency: 4,
      fetchImpl: async (url) => {
        if (url.endsWith("/ok")) return { status: 200, url, redirected: false };
        if (url.endsWith("/gone")) return { status: 404, url, redirected: false };
        if (url.endsWith("/blocked")) return { status: 429, url, redirected: false };
        const error = new Error("The operation was aborted");
        error.name = "AbortError";
        throw error;
      },
    },
  );
  assert.equal(live.checked, todayInChicago(now));
  assert.equal(live.byClass.ok.length, 1);
  assert.equal(live.byClass.dead.length, 1);
  assert.equal(live.byClass.blocked.length, 1);
  assert.equal(live.byClass.timeout.length, 1);
  const report = formatLiveReport(live);
  assert.match(report, /Blocked \(403\/429\/401\/5xx\) is not the same as dead/);
  assert.match(report, /https:\/\/example.com\/gone — HTTP 404/);
  assert.match(report, /https:\/\/example.com\/blocked — HTTP 429/);
});

test("the CLI exits 1 on a deliberately broken fixture and 0 on a valid one", async () => {
  const sink = { write() { return true; } };
  const failed = await main(["node", "scripts/check-directory-links.mjs", join(fixtures, "missing-privacy.md")], {
    stdout: sink,
    stderr: sink,
  });
  const passed = await main(["node", "scripts/check-directory-links.mjs", join(fixtures, "valid.md")], {
    stdout: sink,
    stderr: sink,
  });
  assert.equal(failed, 1);
  assert.equal(passed, 0);
});

test("the structural check passes on the current ecosystem directory", () => {
  const check = checkStructure(read(directoryPath));
  assert.equal(check.ok, true, check.failures.join("\n"));
  assert.ok(check.rowCount >= 70, "expected a full catalogue, found " + check.rowCount + " rows");
  const liveRows = check.rows.filter((row) => row.heading !== "Dead, retired, or stale");
  assert.ok(liveRows.every((row) => row.hasHttps), "every live catalogue row must have an https source URL");
  assert.ok(check.withoutHttps.length >= 1, "the dead/stale table still has hostname-only rows, listed not failed");
});

test("an aborted fetch with a DOM abort code of 20 is classified as timeout, not as error code 20", async () => {
  const live = await runLive(["https://example.com/slow"], {
    now: new Date("2026-11-24T17:00:00Z"),
    fetchImpl: async () => {
      const error = new Error("This operation was aborted");
      error.name = "AbortError";
      error.cause = { code: 20 };
      throw error;
    },
  });
  assert.equal(live.byClass.timeout.length, 1);
  assert.equal(live.results[0].errorCode, "TIMEOUT");
  assert.match(formatLiveReport(live), /TIMEOUT/);
});

test("parseArgs and isCli keep --live opt-in and do not treat the test file as the CLI entry", () => {
  const args = parseArgs(["node", "scripts/check-directory-links.mjs", "research/ecosystem-directory.md"]);
  assert.equal(args.live, false);
  assert.equal(args.path, "research/ecosystem-directory.md");
  const live = parseArgs(["node", "scripts/check-directory-links.mjs", "--live", "--timeout", "8000"]);
  assert.equal(live.live, true);
  assert.equal(live.timeoutMs, 8000);
  assert.equal(isCli(["node", resolve(root, "tests/directory-link-checker.test.mjs")]), false);
});
