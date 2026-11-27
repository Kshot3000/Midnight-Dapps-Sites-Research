import { createCircuitContext, createConstructorContext, dummyContractAddress } from "@midnight-ntwrk/compact-runtime";
import { Contract, ledger, pureCircuits } from "../managed/nightattest/contract/index.js";

const coinPublicKey = "00".repeat(32); // Fixed simulator context, with no wallet involved.
const hex32 = /^[0-9a-f]{64}$/i;

function toHex(bytes) {
  return Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function fromHex(value) {
  if (!hex32.test(value)) throw new Error("Enter exactly 64 hexadecimal characters.");
  return Uint8Array.from(value.match(/../g), (byte) => Number.parseInt(byte, 16));
}

function randomBytes() {
  let bytes;
  do {
    bytes = crypto.getRandomValues(new Uint8Array(32));
  } while (bytes.every((byte) => byte === 0));
  return bytes;
}

const contract = new Contract({
  issuerSecret: ({ privateState }) => [privateState, privateState.issuerSecret],
  holderSecret: ({ privateState }) => [privateState, privateState.holderSecret],
  holderPath: ({ privateState, ledger: publicState }, commitment) => {
    const path = publicState.activeCredentials.findPathForLeaf(commitment);
    if (!path) throw new Error("This simulated pass is unregistered or revoked. Ask the issuer to register your commitment.");
    return [privateState, path];
  },
});

export async function openSimulator() {
  const issuerSecret = randomBytes();
  const address = dummyContractAddress();
  const deployment = await contract.initialState(
    createConstructorContext({}, coinPublicKey),
    pureCircuits.issuerKey(issuerSecret),
  );
  let state = deployment.currentContractState.data;
  let holderSecret;
  let currentChallenge;
  let pending = false;
  const issuedChallenges = new Set();

  async function call(circuit, value, privateState) {
    if (pending) throw new Error("Wait for the current circuit call to finish.");
    pending = true;
    try {
      const context = createCircuitContext({
        circuitId: circuit,
        contractAddress: address,
        coinPublicKeyOrZswapState: coinPublicKey,
        contractState: state,
        privateState,
      });
      const result = await contract.impureCircuits[circuit](context, fromHex(value));
      state = result.context.callContext.currentQueryContext.state;
    } finally {
      pending = false;
    }
  }

  return {
    createPass() {
      holderSecret = randomBytes();
      return toHex(pureCircuits.credentialCommitment(holderSecret));
    },
    register(commitment) {
      return call("issue", commitment, { issuerSecret });
    },
    revoke(commitment) {
      return call("revoke", commitment, { issuerSecret });
    },
    issueChallenge() {
      do {
        currentChallenge = toHex(randomBytes());
      } while (issuedChallenges.has(currentChallenge));
      issuedChallenges.add(currentChallenge);
      return currentChallenge;
    },
    async present(challenge) {
      if (!holderSecret) throw new Error("Create a simulated pass before presenting.");
      const normalized = challenge.toLowerCase();
      if (!currentChallenge || normalized !== currentChallenge) {
        throw new Error("Use the verifier’s current simulated challenge.");
      }
      await call("present", normalized, { holderSecret });
    },
    checkChallenge() {
      if (!currentChallenge) throw new Error("Issue a simulated challenge before checking it.");
      return ledger(state).acceptedChallenges.member(fromHex(currentChallenge));
    },
    snapshot() {
      const publicState = ledger(state);
      let active = 0;
      for (const [, record] of publicState.credentials) {
        if (record.active) active += 1;
      }
      return {
        issued: Number(publicState.issuedCount),
        active,
        accepted: Number(publicState.acceptedChallenges.size()),
      };
    },
  };
}
