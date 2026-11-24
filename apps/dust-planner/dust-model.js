/**
 * DUST Planner Pro — generation and decay model.
 *
 * Protocol parameters and formulas are taken from current official Midnight
 * docs (ledger 9), not from Ledger 8-era guides or the older tokenomics
 * whitepaper speck ratio. This module does not read a wallet, indexer, or
 * network. Every computed balance or projection is a modelled estimate.
 *
 * Sources (checked 2026-11-23):
 * - https://docs.midnight.network/concepts/dust-architecture
 *   Initial parameters: night_dust_ratio = 5_000_000_000 (5 DUST per NIGHT),
 *   generation_decay_rate = 8_267, dust_grace_period = 3 hours.
 *   Time to cap Δ = ρ ÷ g = 5,000,000,000 ÷ 8,267, rounded up to 604,815
 *   seconds. Units: 1 NIGHT = 1,000,000 STAR; 1 DUST = 10^15 SPECK.
 * - https://docs.midnight.network/guides/manage-dust
 *   Formulas: cap = night * nightDustRatio; generation per second =
 *   night * generationDecayRate; timeToCapSeconds is ρ / g rounded up.
 *   Linear generation to cap, then hold; after the backing NIGHT is spent,
 *   decay at the same rate to zero. Unregistered NIGHT does not generate
 *   a DUST UTXO. The guide also prints a Mainnet read at block 2,809,291
 *   with nightDustRatio 5000000000, generationDecayRate 8267, and
 *   timeToCapSeconds 604815. These are ledger parameters and can change.
 * - https://docs.midnight.network/relnotes/ledger-9
 *   "The DUST parameters stay the same: 5 DUST per NIGHT at the cap, and
 *   about 7 days to reach the cap."
 */

export const STARS_PER_NIGHT = 1_000_000n;
export const SPECKS_PER_DUST = 1_000_000_000_000_000n;

/** SPECK per STAR. Source: dust-architecture initial parameters; manage-dust Mainnet example. */
export const NIGHT_DUST_RATIO = 5_000_000_000n;

/** SPECK per STAR per second. Source: dust-architecture initial parameters; manage-dust Mainnet example. */
export const GENERATION_DECAY_RATE = 8267n;

/** Seconds from empty to cap. Source: ρ / g, rounded up; manage-dust Mainnet example. */
export const TIME_TO_CAP_SECONDS = 604815n;

/** Seconds. Source: dust-architecture initial dust_grace_period; manage-dust Mainnet example (10800). */
export const DUST_GRACE_PERIOD_SECONDS = 10800n;

export const SECONDS_PER_DAY = 86400n;

export const SOURCES = Object.freeze({
  architecture: "https://docs.midnight.network/concepts/dust-architecture",
  manageDust: "https://docs.midnight.network/guides/manage-dust",
  ledger9: "https://docs.midnight.network/relnotes/ledger-9",
  parameterNote: "Documented initial ledger parameters, also printed as a Mainnet read at block 2,809,291 in the official DUST guide. Each network stores its own set; governance can replace it.",
});

export function capSpecks(nightStars) {
  return nightStars * NIGHT_DUST_RATIO;
}

export function generationPerSecond(nightStars) {
  return nightStars * GENERATION_DECAY_RATE;
}

export function generatedSpecks(nightStars, secondsElapsed, registered) {
  if (!registered || nightStars <= 0n || secondsElapsed <= 0n) {
    return 0n;
  }
  const cap = capSpecks(nightStars);
  const grown = generationPerSecond(nightStars) * secondsElapsed;
  return grown < cap ? grown : cap;
}

export function secondsToCapFrom(nightStars, currentSpecks) {
  if (nightStars <= 0n) {
    return 0n;
  }
  const cap = capSpecks(nightStars);
  if (currentSpecks >= cap) {
    return 0n;
  }
  const rate = generationPerSecond(nightStars);
  const remaining = cap - currentSpecks;
  return (remaining + rate - 1n) / rate;
}

export function secondsToZero(nightStars, currentSpecks) {
  if (nightStars <= 0n || currentSpecks <= 0n) {
    return 0n;
  }
  const rate = generationPerSecond(nightStars);
  return (currentSpecks + rate - 1n) / rate;
}

export function remainingAfterSpend(currentSpecks, plannedSpendSpecks) {
  return currentSpecks >= plannedSpendSpecks ? currentSpecks - plannedSpendSpecks : 0n;
}

/**
 * @param {object} input
 * @param {bigint} input.nightStars
 * @param {boolean} input.registered
 * @param {bigint} input.secondsElapsed
 * @param {bigint} input.plannedSpendSpecks
 */
export function plan(input) {
  const nightStars = input.nightStars;
  const registered = Boolean(input.registered);
  const secondsElapsed = input.secondsElapsed;
  const plannedSpendSpecks = input.plannedSpendSpecks;
  const cap = capSpecks(nightStars);
  const rate = generationPerSecond(nightStars);
  const current = generatedSpecks(nightStars, secondsElapsed, registered);
  const remaining = remainingAfterSpend(current, plannedSpendSpecks);
  const shortfall = plannedSpendSpecks > current ? plannedSpendSpecks - current : 0n;
  const atCap = registered && nightStars > 0n && current === cap && cap > 0n;

  return {
    nightStars,
    registered,
    secondsElapsed,
    plannedSpendSpecks,
    capSpecks: cap,
    generationPerSecondSpecks: rate,
    generationPerDaySpecks: rate * SECONDS_PER_DAY,
    currentSpecks: current,
    remainingAfterSpendSpecks: remaining,
    shortfallSpecks: shortfall,
    atCap,
    secondsToCap: registered ? secondsToCapFrom(nightStars, current) : TIME_TO_CAP_SECONDS,
    secondsToCapAfterSpend: registered ? secondsToCapFrom(nightStars, remaining) : TIME_TO_CAP_SECONDS,
    decaySecondsIfNightSpentNow: secondsToZero(nightStars, current),
    decaySecondsAfterSpendIfNightSpent: secondsToZero(nightStars, remaining),
    timeToCapSeconds: TIME_TO_CAP_SECONDS,
  };
}

export function formatDecimal(units, scale, options = {}) {
  const negative = units < 0n;
  const value = negative ? -units : units;
  const whole = value / scale;
  let fraction = (value % scale).toString().padStart(scale.toString().length - 1, "0");
  if (options.stripTrailingZeros !== false) {
    fraction = fraction.replace(/0+$/, "");
  }
  const sign = negative ? "-" : "";
  return fraction.length > 0 ? `${sign}${whole}.${fraction}` : `${sign}${whole}`;
}

export function formatDust(specks, options) {
  return formatDecimal(specks, SPECKS_PER_DUST, options);
}

export function formatNight(stars, options) {
  return formatDecimal(stars, STARS_PER_NIGHT, options);
}

export function parseFixed(text, scale, label, maxFractionDigits) {
  const raw = String(text ?? "").trim();
  if (raw === "") {
    throw new Error(`Enter a ${label}.`);
  }
  if (!/^\d+(\.\d+)?$/.test(raw)) {
    throw new Error(`${label} must be a non-negative number.`);
  }
  const [whole, fraction = ""] = raw.split(".");
  if (fraction.length > maxFractionDigits) {
    throw new Error(`${label} can have at most ${maxFractionDigits} decimal places.`);
  }
  const combined = (whole + fraction.padEnd(maxFractionDigits, "0")).replace(/^0+(?=\d)/, "");
  return BigInt(combined);
}

export function parseNight(text) {
  return parseFixed(text, STARS_PER_NIGHT, "NIGHT amount", 6);
}

export function parseDust(text) {
  return parseFixed(text, SPECKS_PER_DUST, "Planned DUST spend", 15);
}

export function parseDaysToSeconds(text) {
  const raw = String(text ?? "").trim();
  if (raw === "") {
    throw new Error("Enter days since registration.");
  }
  if (!/^\d+(\.\d+)?$/.test(raw)) {
    throw new Error("Days since registration must be a non-negative number.");
  }
  const [whole, fraction = ""] = raw.split(".");
  if (fraction.length > 9) {
    throw new Error("Days since registration can have at most 9 decimal places.");
  }
  const dayScale = 1_000_000_000n;
  const daysScaled = parseFixed(raw, dayScale, "Days since registration", 9);
  return (daysScaled * SECONDS_PER_DAY) / dayScale;
}

export function formatSeconds(seconds) {
  const value = seconds < 0n ? 0n : seconds;
  const days = value / SECONDS_PER_DAY;
  const rem = value % SECONDS_PER_DAY;
  const hours = rem / 3600n;
  const minutes = (rem % 3600n) / 60n;
  const secs = rem % 60n;
  const parts = [];
  if (days > 0n) parts.push(`${days} day${days === 1n ? "" : "s"}`);
  if (hours > 0n) parts.push(`${hours} hour${hours === 1n ? "" : "s"}`);
  if (minutes > 0n) parts.push(`${minutes} minute${minutes === 1n ? "" : "s"}`);
  if (secs > 0n || parts.length === 0) parts.push(`${secs} second${secs === 1n ? "" : "s"}`);
  return `${value.toString()} seconds (${parts.join(", ")})`;
}
