import {
  formatDust,
  formatSeconds,
  parseDaysToSeconds,
  parseDust,
  parseNight,
  plan,
} from "./dust-model.js";

const form = document.querySelector("#planner-form");
const errors = document.querySelector("#form-errors");
const unregisteredAlert = document.querySelector("#unregistered-alert");
const outCurrent = document.querySelector("#out-current");
const outCap = document.querySelector("#out-cap");
const outRate = document.querySelector("#out-rate");
const outToCap = document.querySelector("#out-to-cap");
const outRemaining = document.querySelector("#out-remaining");
const outDecay = document.querySelector("#out-decay");
const outSummary = document.querySelector("#out-summary");

function dustLabel(specks) {
  return `${formatDust(specks)} DUST (modelled estimate)`;
}

function showErrors(messages) {
  errors.hidden = false;
  errors.className = "error-list";
  errors.setAttribute("role", "alert");
  errors.innerHTML = "<ul>" + messages.map((message) => `<li>${message}</li>`).join("") + "</ul>";
}

function clearErrors() {
  errors.hidden = true;
  errors.removeAttribute("role");
  errors.replaceChildren();
}

function render(result) {
  unregisteredAlert.hidden = result.registered;
  outCurrent.textContent = dustLabel(result.currentSpecks);
  outCap.textContent = dustLabel(result.capSpecks);
  outRate.textContent = `${formatDust(result.generationPerSecondSpecks)} DUST/s, ${formatDust(result.generationPerDaySpecks)} DUST/day (modelled estimate)`;
  outToCap.textContent = result.registered
    ? (result.atCap ? "Already at cap (modelled estimate)" : formatSeconds(result.secondsToCap) + " remaining (modelled estimate)")
    : formatSeconds(result.timeToCapSeconds) + " from empty, if registered (modelled estimate)";
  if (result.shortfallSpecks > 0n) {
    outRemaining.textContent = `0 DUST left; shortfall ${formatDust(result.shortfallSpecks)} DUST (modelled estimate)`;
  } else {
    outRemaining.textContent = dustLabel(result.remainingAfterSpendSpecks);
  }
  const decaySeconds = result.decaySecondsIfNightSpentNow;
  outDecay.textContent = decaySeconds === 0n
    ? "No DUST to decay (modelled estimate)"
    : formatSeconds(decaySeconds) + " to zero (modelled estimate)";

  const parts = [
    "Every figure on this panel is a modelled estimate.",
    result.registered
      ? `After a modelled fee spend, regeneration toward the cap would take ${formatSeconds(result.secondsToCapAfterSpend)} if the backing NIGHT stayed unspent.`
      : "With registration off, the modelled spendable balance stays 0.",
    "Preview and Preprod are testnets; this page is not a live testnet or mainnet read.",
  ];
  outSummary.textContent = parts.join(" ");
}

function readPlan() {
  const messages = [];
  let nightStars, secondsElapsed, plannedSpendSpecks;
  try {
    nightStars = parseNight(form.elements.night.value);
  } catch (error) {
    messages.push(error.message);
  }
  try {
    secondsElapsed = parseDaysToSeconds(form.elements.days.value);
  } catch (error) {
    messages.push(error.message);
  }
  try {
    plannedSpendSpecks = parseDust(form.elements.spend.value);
  } catch (error) {
    messages.push(error.message);
  }
  if (messages.length) {
    throw Object.assign(new Error("invalid"), { messages });
  }
  return plan({
    nightStars,
    registered: form.elements.registered.checked,
    secondsElapsed,
    plannedSpendSpecks,
  });
}

function update() {
  try {
    const result = readPlan();
    clearErrors();
    render(result);
  } catch (error) {
    showErrors(error.messages || ["Check the inputs and try again."]);
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  update();
});

form.addEventListener("reset", () => {
  window.setTimeout(update, 0);
});

form.elements.registered.addEventListener("change", update);
for (const name of ["night", "days", "spend"]) {
  form.elements[name].addEventListener("input", update);
}

update();

const copyFeedback = document.querySelector("#copy-feedback");

for (const button of document.querySelectorAll("[data-copy]")) {
  const address = document.getElementById(button.dataset.copy);
  const walletName = button.closest(".wallet").querySelector("h3").textContent.trim();

  button.addEventListener("click", async () => {
    copyFeedback.textContent = "";
    try {
      await navigator.clipboard.writeText(address.textContent);
      copyFeedback.textContent = `${walletName} address copied.`;
    } catch {
      const range = document.createRange();
      range.selectNodeContents(address);
      const selection = window.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
      copyFeedback.textContent = `${walletName} address selected. Press Ctrl+C or Command+C to copy.`;
    }
  });

  button.hidden = false;
}
