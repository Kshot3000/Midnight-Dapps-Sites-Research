#!/usr/bin/env node
/**
 * GROK-5 — Automated link checker for research/ecosystem-directory.md
 *
 * Structural mode (default, the CI gate): every catalogue table row must
 * keep a description, a network label, a privacy pattern, and an https
 * source URL. A row missing any of those fails the check. In the
 * "Dead, retired, or stale" table only, a non-empty URL/handle cell is
 * accepted when the row has no https URL (some retired hosts were
 * recorded as hostnames or official-doc citations). Those rows are
 * listed in the report and are not a hard fail.
 *
 * Live mode (`--live`) fetches each unique https URL and records status
 * plus the check date. Classification:
 *   ok       — HTTP 2xx/3xx
 *   dead     — HTTP 404/410, DNS failures, ECONNREFUSED
 *   blocked  — HTTP 401/403/407/429/451 and 502/503/504
 *   timeout  — aborted or timed-out fetches
 *   error    — other network/TLS failures and other 4xx/5xx
 *              (including 500, 501, 505)
 * Timeouts, blocked responses, and other flaky results are recorded, not
 * treated as a CI hard-fail. Unit tests must not use --live and must not
 * call the network. Redirected fetches record and print the final URL.
 *
 * Usage:
 *   node scripts/check-directory-links.mjs [path] [--live]
 *   node scripts/check-directory-links.mjs --timeout 15000 --concurrency 6 --live
 *
 * Support — @kshot9000
 * Cardano (ADA): addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v
 * Midnight (shielded): mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q
 *
 * Independent builder — not affiliated with the Midnight Foundation or IOG.
 */

import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

export const CARDANO_ADDRESS =
  "addr1q8hnl6vl5a6k3rw3n5g3jtte696zcl76kfatzv7gpswa9r0dj7fma6klq55y4ffm7tf0em09udnyhuk4ah92pl5x9jpqjae44v";
export const MIDNIGHT_ADDRESS =
  "mn_shield-addr1k5vrk8kh4cj9l6cetlumrjy3w0l9p5tt9mux4mnn2pxn6ck70hlrxasc88v202l8w0c4ne5r03du7s4pxawd6r54a2sw0rgnq253lgsdksa2q";
export const AFFILIATION =
  "Independent builder — not affiliated with the Midnight Foundation or IOG.";
export const USER_AGENT =
  "Midnight-Dapps-Sites-Research directory link checker (+https://github.com/Kshot3000/Midnight-Dapps-Sites-Research; @kshot9000)";

const DEFAULT_DIRECTORY = "research/ecosystem-directory.md";
const DEAD_SECTION = "Dead, retired, or stale";

export function repoRoot() {
  return resolve(dirname(fileURLToPath(import.meta.url)), "..");
}

export function splitRow(line) {
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

export function catalogueTables(markdown) {
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
        rows.push({ line: lines[rowIndex], cells: splitRow(lines[rowIndex]) });
      }
      tables.push({ heading: heading.title, headers, rows });
    }
  }
  return tables;
}

export function stripTrailingPunctuation(value) {
  return value.replace(/[.,;:]+$/u, "");
}

export function extractHttpUrls(text) {
  const found = [];
  const seen = new Set();
  const add = (raw) => {
    const cleaned = stripTrailingPunctuation(String(raw || "").trim());
    if (!cleaned || seen.has(cleaned)) return;
    seen.add(cleaned);
    found.push(cleaned);
  };
  for (const match of text.matchAll(/\[[^\]]*\]\((https?:\/\/[^)\s]+)/g)) {
    add(match[1]);
  }
  for (const match of text.matchAll(/https?:\/\/[^\s)\]>`'"]+/g)) {
    add(match[0]);
  }
  return found;
}

export function extractHttpsUrls(text) {
  return extractHttpUrls(text).filter((url) => url.startsWith("https://"));
}

function columnIndex(headers, names) {
  return headers.findIndex((header) => names.includes(header));
}

function cell(row, index) {
  if (index < 0) return "";
  return (row.cells[index] || "").trim();
}

export function inspectRow(table, row) {
  const descriptionIndex = columnIndex(table.headers, ["what it does", "what i found"]);
  const networkIndex = columnIndex(table.headers, ["network"]);
  const privacyIndex = columnIndex(table.headers, ["privacy pattern"]);
  const urlIndex = columnIndex(table.headers, ["url", "url / handle"]);
  const label = table.heading + " / " + (row.cells[0] || "(unnamed row)");
  const description = cell(row, descriptionIndex);
  const network = cell(row, networkIndex);
  const privacy = cell(row, privacyIndex);
  const urlHandle = cell(row, urlIndex);
  const rowText = row.cells.join(" ");
  const urls = extractHttpUrls(rowText);
  const httpsUrls = urls.filter((url) => url.startsWith("https://"));
  const httpUrls = urls.filter((url) => url.startsWith("http://") && !url.startsWith("https://"));
  const failures = [];
  if (descriptionIndex < 0) {
    failures.push(table.heading + " is missing a description column (What it does / What I found)");
  } else if (!description) {
    failures.push(label + " is missing a description");
  }
  if (networkIndex < 0) {
    failures.push(table.heading + " is missing a Network column");
  } else if (!network) {
    failures.push(label + " is missing a network field");
  }
  if (privacyIndex < 0) {
    failures.push(table.heading + " is missing a Privacy pattern column");
  } else if (!privacy) {
    failures.push(label + " is missing a privacy-pattern field");
  }
  if (httpUrls.length > 0) {
    failures.push(label + " has a non-https URL: " + httpUrls.join(", "));
  }
  const deadSection = table.heading === DEAD_SECTION;
  const hasHttps = httpsUrls.length > 0;
  if (!hasHttps && !(deadSection && urlHandle)) {
    failures.push(label + " is missing an https source URL");
  }
  return {
    heading: table.heading,
    label,
    description,
    network,
    privacy,
    urlHandle,
    httpsUrls,
    httpUrls,
    hasHttps,
    hostnameOnly: deadSection && !hasHttps && Boolean(urlHandle),
    failures,
  };
}

export function checkStructure(markdown) {
  const tables = catalogueTables(markdown);
  const rows = [];
  const failures = [];
  const withoutHttps = [];
  if (tables.length === 0) {
    failures.push("no catalogue tables found");
  }
  for (const table of tables) {
    if (table.rows.length === 0) {
      failures.push(table.heading + " has no data rows");
    }
    for (const row of table.rows) {
      const inspected = inspectRow(table, row);
      rows.push(inspected);
      failures.push(...inspected.failures);
      if (inspected.hostnameOnly) withoutHttps.push(inspected);
    }
  }
  return {
    ok: failures.length === 0,
    tables: tables.length,
    rowCount: rows.length,
    rows,
    failures,
    withoutHttps,
  };
}

// blocked = 401/403/407/429/451 (auth, bot-block, rate-limit, legal) and
// 502/503/504 (gateway / temporarily unavailable). Other 5xx such as
// 500, 501, and 505 are unexpected server errors and land in "error",
// not "blocked".
export function classifyFetch(result) {
  if (result.statusClass) return result.statusClass;
  const errorCode = String(result.errorCode || "");
  const errorMessage = String(result.errorMessage || "");
  const combined = errorCode + " " + errorMessage;
  if (
    errorCode === "TIMEOUT" ||
    errorCode === "ABORT" ||
    errorCode === "ETIMEDOUT" ||
    errorCode === "TimeoutError" ||
    errorCode === "AbortError" ||
    /timeout|aborted/i.test(combined)
  ) {
    return "timeout";
  }
  if (
    errorCode === "ENOTFOUND" ||
    errorCode === "EAI_AGAIN" ||
    errorCode === "ENETUNREACH" ||
    errorCode === "ECONNREFUSED" ||
    /ENOTFOUND|EAI_AGAIN|ENETUNREACH|ECONNREFUSED|getaddrinfo/i.test(combined)
  ) {
    return "dead";
  }
  if (errorCode) return "error";
  const status = Number(result.status);
  if (status >= 200 && status < 400) return "ok";
  if (status === 404 || status === 410) return "dead";
  if (status === 401 || status === 403 || status === 407 || status === 429 || status === 451) {
    return "blocked";
  }
  if (status === 502 || status === 503 || status === 504) return "blocked";
  if (status >= 400) return "error";
  return "error";
}

export function todayInChicago(date = new Date()) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Chicago",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
}

export async function fetchUrl(url, options = {}) {
  const timeoutMs = options.timeoutMs ?? 15_000;
  const fetchImpl = options.fetchImpl ?? globalThis.fetch;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetchImpl(url, {
      method: "GET",
      redirect: "follow",
      signal: controller.signal,
      headers: {
        "user-agent": options.userAgent ?? USER_AGENT,
        accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      },
    });
    if (typeof response.body?.cancel === "function") {
      try {
        await response.body.cancel();
      } catch {
        // Ignore body-cancel failures; status is enough for classification.
      }
    }
    return {
      url,
      finalUrl: response.url || url,
      status: response.status,
      redirected: Boolean(response.redirected),
    };
  } catch (error) {
    const aborted = error?.name === "AbortError" || error?.name === "TimeoutError";
    const causeCode = error?.cause?.code;
    const code =
      (aborted ? "TIMEOUT" : "") ||
      causeCode ||
      error?.code ||
      error?.name;
    return {
      url,
      errorCode: code || "ERROR",
      errorMessage: error?.message || String(error),
    };
  } finally {
    clearTimeout(timer);
  }
}

export async function mapPool(items, limit, fn) {
  const results = new Array(items.length);
  let next = 0;
  const workerCount = Math.max(1, Math.min(limit, items.length));
  async function worker() {
    while (next < items.length) {
      const index = next;
      next += 1;
      results[index] = await fn(items[index], index);
    }
  }
  await Promise.all(Array.from({ length: items.length === 0 ? 0 : workerCount }, () => worker()));
  return results;
}

export async function runLive(urls, options = {}) {
  const unique = [...new Set(urls)];
  const started = todayInChicago(options.now);
  const results = await mapPool(unique, options.concurrency ?? 6, async (url) => {
    const fetched = await fetchUrl(url, options);
    const statusClass = classifyFetch(fetched);
    return { ...fetched, statusClass, checked: started };
  });
  const byClass = { ok: [], dead: [], blocked: [], timeout: [], error: [] };
  for (const result of results) {
    (byClass[result.statusClass] || (byClass[result.statusClass] = [])).push(result);
  }
  return { checked: started, unique: unique.length, results, byClass };
}

export function formatStructuralReport(check, path) {
  const lines = [];
  lines.push("Directory link checker — structural");
  lines.push("File: " + path);
  lines.push(
    "Tables: " + check.tables + " · rows: " + check.rowCount + " · https URLs: " +
      new Set(check.rows.flatMap((row) => row.httpsUrls)).size,
  );
  if (check.withoutHttps.length > 0) {
    lines.push("");
    lines.push("Rows without an https URL (dead/stale table, URL/handle present; not a hard fail):");
    for (const row of check.withoutHttps) {
      lines.push("- " + row.label + " — " + (row.urlHandle || "(empty)"));
    }
  }
  if (check.failures.length > 0) {
    lines.push("");
    lines.push("Structural failures:");
    for (const failure of check.failures) lines.push("- " + failure);
    lines.push("");
    lines.push("Result: FAIL (" + check.failures.length + ")");
  } else {
    lines.push("");
    lines.push("Result: PASS");
  }
  return lines.join("\n");
}

export function formatLiveReport(live) {
  const lines = [];
  const counts = Object.fromEntries(
    ["ok", "dead", "blocked", "timeout", "error"].map((key) => [key, (live.byClass[key] || []).length]),
  );
  lines.push("");
  lines.push("Directory link checker — live fetch");
  lines.push("Checked: " + live.checked + " (America/Chicago)");
  lines.push(
    "Unique https URLs: " + live.unique +
      " · ok " + counts.ok +
      " · dead " + counts.dead +
      " · blocked " + counts.blocked +
      " · timeout " + counts.timeout +
      " · error " + counts.error,
  );
  lines.push("Blocked (401/403/407/429/451 and 502/503/504) is not the same as dead (404/410/DNS). Other 5xx (500/501/505) land in error.");
  const order = [
    ["dead", "Dead"],
    ["blocked", "Blocked or rate-limited"],
    ["timeout", "Timed out"],
    ["error", "Other errors (TLS/network/unexpected status)"],
  ];
  const redirected = (live.results || []).filter(
    (item) => item.redirected && item.finalUrl && item.finalUrl !== item.url,
  );
  if (redirected.length > 0) {
    lines.push("");
    lines.push("Redirected:");
    for (const item of redirected) lines.push(formatFetchLine(item));
  }
  for (const [key, title] of order) {
    const items = live.byClass[key] || [];
    if (items.length === 0) continue;
    lines.push("");
    lines.push(title + ":");
    for (const item of items) lines.push(formatFetchLine(item));
  }
  return lines.join("\n");
}

export function formatFetchLine(item) {
  const detail = item.status
    ? "HTTP " + item.status
    : (item.errorCode || "error") + (item.errorMessage ? " (" + item.errorMessage + ")" : "");
  let line = "- " + item.url + " — " + detail;
  if (item.redirected && item.finalUrl && item.finalUrl !== item.url) {
    line += " (redirected to " + item.finalUrl + ")";
  }
  return line;
}

export function parseArgs(argv) {
  const args = { path: null, live: false, timeoutMs: 15_000, concurrency: 6, help: false };
  const rest = argv.slice(2);
  for (let index = 0; index < rest.length; index += 1) {
    const token = rest[index];
    if (token === "--live") args.live = true;
    else if (token === "--help" || token === "-h") args.help = true;
    else if (token === "--timeout") {
      args.timeoutMs = Number(rest[index + 1]);
      index += 1;
    } else if (token === "--concurrency") {
      args.concurrency = Number(rest[index + 1]);
      index += 1;
    } else if (token.startsWith("-")) {
      throw new Error("Unknown flag: " + token);
    } else if (!args.path) {
      args.path = token;
    } else {
      throw new Error("Unexpected argument: " + token);
    }
  }
  if (!Number.isFinite(args.timeoutMs) || args.timeoutMs <= 0) {
    throw new Error("Invalid --timeout");
  }
  if (!Number.isFinite(args.concurrency) || args.concurrency <= 0) {
    throw new Error("Invalid --concurrency");
  }
  return args;
}

export function isCli(argv = process.argv, metaUrl = import.meta.url) {
  return Boolean(argv[1]) && metaUrl === pathToFileURL(resolve(argv[1])).href;
}

export async function main(argv = process.argv, options = {}) {
  const args = parseArgs(argv);
  const out = options.stdout ?? process.stdout;
  const err = options.stderr ?? process.stderr;
  if (args.help) {
    out.write(
      "Usage: node scripts/check-directory-links.mjs [path] [--live] [--timeout 15000] [--concurrency 6]\n",
    );
    return 0;
  }
  const path = resolve(args.path ?? resolve(repoRoot(), DEFAULT_DIRECTORY));
  const markdown = readFileSync(path, "utf8");
  const check = checkStructure(markdown);
  out.write(formatStructuralReport(check, path) + "\n");
  if (!check.ok) return 1;
  if (args.live) {
    const urls = [...new Set(check.rows.flatMap((row) => row.httpsUrls))];
    const live = await runLive(urls, {
      timeoutMs: args.timeoutMs,
      concurrency: args.concurrency,
      fetchImpl: options.fetchImpl,
      now: options.now,
    });
    out.write(formatLiveReport(live) + "\n");
  }
  return 0;
}

if (isCli()) {
  main().then((code) => {
    process.exit(code);
  }).catch((error) => {
    console.error(error);
    process.exit(1);
  });
}
