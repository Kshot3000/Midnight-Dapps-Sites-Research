const byId = (id) => document.getElementById(id);
const roles = Array.from(document.querySelectorAll("[data-role]"));
const issuerCommitment = byId("issuer-commitment");
const holderCommitment = byId("holder-commitment");
const holderChallenge = byId("holder-challenge");
const verifierChallenge = byId("verifier-challenge");
const loadCommitment = byId("load-commitment");
const loadChallenge = byId("load-challenge");
const checkChallenge = byId("check-challenge");

for (const button of roles) {
  button.addEventListener("click", () => {
    for (const role of roles) {
      const selected = role === button;
      role.setAttribute("aria-pressed", String(selected));
      byId(role.dataset.role).hidden = !selected;
    }
  });
}

function announce(role, message, tone = "success") {
  const status = byId(role + "-status");
  status.dataset.tone = tone;
  status.textContent = message;
}

async function start() {
  const { openSimulator } = await import("./simulator.js");
  const simulator = await openSimulator();
  let busy = false;

  function updateLedger() {
    const { issued, active, accepted } = simulator.snapshot();
    byId("issued").textContent = issued;
    byId("active").textContent = active;
    byId("accepted").textContent = accepted;
  }

  function run(role, action) {
    return async (event) => {
      event.preventDefault();
      if (busy) return;
      busy = true;
      try {
        await action(event);
        updateLedger();
      } catch (error) {
        announce(role, error.message, "error");
      } finally {
        busy = false;
      }
    };
  }

  byId("create-pass").addEventListener("click", run("holder", () => {
    holderCommitment.value = simulator.createPass();
    byId("holder-output").hidden = false;
    byId("create-pass").hidden = true;
    holderCommitment.focus();
    loadCommitment.disabled = false;
    announce("holder", "Simulated pass created. Register this commitment in Issuer.");
  }));

  loadCommitment.addEventListener("click", () => {
    issuerCommitment.value = holderCommitment.value;
    issuerCommitment.focus();
  });

  byId("issuer-form").addEventListener("submit", run("issuer", async (event) => {
    const commitment = issuerCommitment.value;
    if (event.submitter?.value === "revoke") {
      await simulator.revoke(commitment);
      announce("issuer", "Commitment revoked in the simulated ledger.");
    } else {
      await simulator.register(commitment);
      announce("issuer", "Commitment registered in the simulated ledger.");
    }
  }));

  byId("issue-challenge").addEventListener("click", run("verifier", () => {
    verifierChallenge.value = simulator.issueChallenge();
    byId("verifier-output").hidden = false;
    holderChallenge.value = "";
    byId("holder-status").textContent = "";
    loadChallenge.disabled = false;
    checkChallenge.disabled = false;
    announce("verifier", "Fresh simulated challenge issued. Load it in Holder to present.", "neutral");
  }));

  loadChallenge.addEventListener("click", () => {
    holderChallenge.value = verifierChallenge.value;
    holderChallenge.focus();
  });

  byId("holder-form").addEventListener("submit", run("holder", async () => {
    await simulator.present(holderChallenge.value);
    announce("holder", "Simulated presentation accepted. Check the result in Verifier.");
  }));

  checkChallenge.addEventListener("click", run("verifier", () => {
    if (simulator.checkChallenge()) {
      announce("verifier", "Accepted in simulation. An active pass answered this challenge.");
    } else {
      announce("verifier", "Pending in simulation. The contract has no acceptance for this challenge.", "neutral");
    }
  }));

  updateLedger();
  for (const fieldset of document.querySelectorAll("fieldset")) {
    fieldset.disabled = false;
  }
}

start().catch((error) => {
  const status = byId("boot-status");
  status.textContent = "Could not start the contract simulator. " + error.message;
  status.hidden = false;
});
