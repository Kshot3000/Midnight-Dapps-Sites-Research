import {
  formatDust,
  formatSeconds,
  parseDaysToSeconds,
  parseDust,
  parseNight,
  plan,
} from "./dust-model.js";

export const OUTPUT_PLACEHOLDER = "—";

export function showErrors(container, messages, createElement) {
  container.hidden = false;
  container.className = "error-list";
  container.setAttribute("role", "alert");
  const list = createElement("ul");
  for (const message of messages) {
    const item = createElement("li");
    item.textContent = String(message);
    list.append(item);
  }
  container.replaceChildren(list);
}

export function clearErrors(container) {
  container.hidden = true;
  container.removeAttribute("role");
  container.replaceChildren();
}

export function resetOutputs(ui) {
  ui.unregisteredAlert.hidden = true;
  ui.outCurrent.textContent = OUTPUT_PLACEHOLDER;
  ui.outCap.textContent = OUTPUT_PLACEHOLDER;
  ui.outRate.textContent = OUTPUT_PLACEHOLDER;
  ui.outToCap.textContent = OUTPUT_PLACEHOLDER;
  ui.outRemaining.textContent = OUTPUT_PLACEHOLDER;
  ui.outDecay.textContent = OUTPUT_PLACEHOLDER;
  ui.outSummary.textContent = "";
}

function dustLabel(specks) {
  return `${formatDust(specks)} DUST (modelled estimate)`;
}

export function renderPlan(ui, result) {
  ui.unregisteredAlert.hidden = result.registered;
  ui.outCurrent.textContent = dustLabel(result.currentSpecks);
  ui.outCap.textContent = dustLabel(result.capSpecks);
  ui.outRate.textContent = `${formatDust(result.generationPerSecondSpecks)} DUST/s, ${formatDust(result.generationPerDaySpecks)} DUST/day (modelled estimate)`;
  ui.outToCap.textContent = result.registered
    ? (result.atCap ? "Already at cap (modelled estimate)" : formatSeconds(result.secondsToCap) + " remaining (modelled estimate)")
    : formatSeconds(result.timeToCapSeconds) + " from empty, if registered (modelled estimate)";
  if (result.shortfallSpecks > 0n) {
    ui.outRemaining.textContent = `0 DUST left; shortfall ${formatDust(result.shortfallSpecks)} DUST (modelled estimate)`;
  } else {
    ui.outRemaining.textContent = dustLabel(result.remainingAfterSpendSpecks);
  }
  const decaySeconds = result.decaySecondsIfNightSpentNow;
  ui.outDecay.textContent = decaySeconds === 0n
    ? "No DUST to decay (modelled estimate)"
    : formatSeconds(decaySeconds) + " to zero (modelled estimate)";

  const parts = [
    "Every figure on this panel is a modelled estimate.",
    result.registered
      ? `After a modelled fee spend, regeneration toward the cap would take ${formatSeconds(result.secondsToCapAfterSpend)} if the backing NIGHT stayed unspent.`
      : "With registration off, the modelled spendable balance stays 0.",
    "Preview and Preprod are testnets; this page is not a live testnet or mainnet read.",
  ];
  ui.outSummary.textContent = parts.join(" ");
}

export function bindPlanner(doc) {
  const form = doc.querySelector("#planner-form");
  const ui = {
    errors: doc.querySelector("#form-errors"),
    unregisteredAlert: doc.querySelector("#unregistered-alert"),
    outCurrent: doc.querySelector("#out-current"),
    outCap: doc.querySelector("#out-cap"),
    outRate: doc.querySelector("#out-rate"),
    outToCap: doc.querySelector("#out-to-cap"),
    outRemaining: doc.querySelector("#out-remaining"),
    outDecay: doc.querySelector("#out-decay"),
    outSummary: doc.querySelector("#out-summary"),
  };
  const createElement = (tag) => doc.createElement(tag);

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
      clearErrors(ui.errors);
      renderPlan(ui, result);
    } catch (error) {
      resetOutputs(ui);
      showErrors(ui.errors, error.messages || ["Check the inputs and try again."], createElement);
    }
  }

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    update();
  });

  form.addEventListener("reset", () => {
    globalThis.setTimeout(update, 0);
  });

  form.elements.registered.addEventListener("change", update);
  for (const name of ["night", "days", "spend"]) {
    form.elements[name].addEventListener("input", update);
  }

  update();
  return { update, ui };
}

if (typeof document !== "undefined") {
  if (document.querySelector("#planner-form")) {
    bindPlanner(document);
  }

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
}
