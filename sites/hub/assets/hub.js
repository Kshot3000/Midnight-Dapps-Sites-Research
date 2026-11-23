"use strict";

const filters = document.querySelector("#catalogue-filters");
const categoryButtons = [...filters.querySelectorAll("[data-filter]")];
const search = document.querySelector("#project-search");
const status = document.querySelector("#project-status");
const count = document.querySelector("#result-count");
const emptyState = document.querySelector("#empty-state");
const clear = document.querySelector("#clear-filters");
const projects = [...document.querySelectorAll("[data-project]")].map((card) => ({
  card,
  text: card.textContent.toLowerCase().replace(/\s+/g, " "),
}));

let selectedCategory = "all";

function applyFilters() {
  const terms = search.value.toLowerCase().trim().split(/\s+/).filter(Boolean);
  let visible = 0;

  for (const { card, text } of projects) {
    const matchesCategory = selectedCategory === "all" || card.dataset.category === selectedCategory;
    const matchesStatus = status.value === "all" || card.dataset.status === status.value;
    const matchesSearch = terms.every((term) => text.includes(term));

    card.hidden = !(matchesCategory && matchesStatus && matchesSearch);
    if (!card.hidden) visible += 1;
  }

  emptyState.hidden = visible !== 0;
  count.textContent = `Showing ${visible} of ${projects.length} projects`;
}

function selectCategory(category) {
  selectedCategory = category;
  for (const button of categoryButtons) {
    button.setAttribute("aria-pressed", String(button.dataset.filter === category));
  }
  applyFilters();
}

for (const button of categoryButtons) {
  button.addEventListener("click", () => selectCategory(button.dataset.filter));
}

search.addEventListener("input", applyFilters);
status.addEventListener("change", applyFilters);
clear.addEventListener("click", () => {
  search.value = "";
  status.value = "all";
  selectCategory("all");
  search.focus();
});

applyFilters();
filters.hidden = false;
count.hidden = false;

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
