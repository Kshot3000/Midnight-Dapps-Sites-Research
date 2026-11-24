import assert from "node:assert/strict";
import { test } from "node:test";
import {
  createCircuitContext,
  createConstructorContext,
  sampleContractAddress,
} from "@midnight-ntwrk/compact-runtime";
import { Contract, ledger, pureCircuits } from "../managed/nightattest/contract/index.js";

// Deterministic fixture bytes only. Real secrets and challenges need secure randomness.
const bytes = (tag) => new Uint8Array(32).fill(tag);
const coinPublicKey = "00".repeat(32);
const issuerSecret = bytes(1);
const issuerDigest = pureCircuits.issuerKey(issuerSecret);
const issuer = { issuerSecret };
const alice = { holderSecret: bytes(2) };
const bob = { holderSecret: bytes(3) };
const aliceCommitment = pureCircuits.credentialCommitment(alice.holderSecret);
const bobCommitment = pureCircuits.credentialCommitment(bob.holderSecret);

const contract = new Contract({
  issuerSecret: ({ privateState }) => [privateState, privateState.issuerSecret],
  holderSecret: ({ privateState }) => [privateState, privateState.holderSecret],
  holderPath: ({ privateState, ledger: publicState }, commitment) => {
    const path = privateState.path ?? publicState.activeCredentials.findPathForLeaf(commitment);
    assert.ok(path, "No active credential path available");
    return [privateState, path];
  },
});

function deploy(digest = issuerDigest) {
  return contract.initialState(createConstructorContext({}, coinPublicKey), digest);
}

async function workshop() {
  const address = sampleContractAddress();
  let state = (await deploy()).currentContractState.data;

  function evaluate(circuit, value, privateState) {
    const context = createCircuitContext({
      circuitId: circuit,
      contractAddress: address,
      coinPublicKeyOrZswapState: coinPublicKey,
      contractState: state,
      privateState,
    });
    return contract.impureCircuits[circuit](context, value);
  }

  return {
    ledger: () => ledger(state),
    evaluate,
    async call(circuit, value, privateState) {
      const result = await evaluate(circuit, value, privateState);
      state = result.context.callContext.currentQueryContext.state;
      return result;
    },
  };
}

test("deployment pins a nonzero issuer digest with a separate hash domain", async () => {
  await assert.rejects(deploy(bytes(0)), /Issuer digest must be nonzero/);
  const sim = await workshop();
  assert.deepEqual(sim.ledger().issuerDigest, issuerDigest);
  assert.notDeepEqual(pureCircuits.credentialCommitment(issuerSecret), issuerDigest);
  assert.equal(sim.ledger().issuedCount, 0n);
});

test("only the issuer can add or revoke an active commitment", async () => {
  const sim = await workshop();
  await assert.rejects(
    sim.call("issue", aliceCommitment, { issuerSecret: issuerDigest }),
    /Issuer authorization required/,
  );
  assert.equal(sim.ledger().issuedCount, 0n);
  assert.equal(sim.ledger().credentials.member(aliceCommitment), false);

  await sim.call("issue", aliceCommitment, issuer);
  const root = sim.ledger().activeCredentials.root();
  await assert.rejects(
    sim.call("revoke", aliceCommitment, { issuerSecret: alice.holderSecret }),
    /Issuer authorization required/,
  );
  assert.deepEqual(sim.ledger().activeCredentials.root(), root);
  assert.deepEqual(sim.ledger().credentials.lookup(aliceCommitment), { slot: 0n, active: true });
  assert.equal(sim.ledger().credentials.member(alice.holderSecret), false);
});

test("issuance rejects zero and duplicates, and revocation rejects unknown commitments", async () => {
  const sim = await workshop();
  await assert.rejects(sim.call("issue", bytes(0), issuer), /Credential commitment must be nonzero/);
  await assert.rejects(sim.call("revoke", bobCommitment, issuer), /Credential commitment was never issued/);
  await sim.call("issue", aliceCommitment, issuer);
  await assert.rejects(sim.call("issue", aliceCommitment, issuer), /already been issued/);
  await sim.call("issue", bobCommitment, issuer);
  assert.equal(sim.ledger().issuedCount, 2n);
  assert.deepEqual(sim.ledger().credentials.lookup(bobCommitment), { slot: 1n, active: true });
});

test("an active holder can answer fresh challenges and each challenge can be accepted once", async () => {
  const sim = await workshop();
  await sim.call("issue", aliceCommitment, issuer);
  await sim.call("issue", bobCommitment, issuer);
  await assert.rejects(sim.call("present", bytes(0), alice), /Verifier challenge must be nonzero/);
  const first = bytes(10);
  const second = bytes(11);
  await sim.call("present", first, alice);
  await sim.call("present", second, alice);
  await assert.rejects(sim.call("present", first, bob), /Verifier challenge has already been used/);
  assert.equal(sim.ledger().acceptedChallenges.size(), 2n);
  assert.ok(sim.ledger().acceptedChallenges.member(first));
  assert.ok(sim.ledger().acceptedChallenges.member(second));
});

test("the circuit rejects a borrowed path and a changed sibling", async () => {
  const sim = await workshop();
  await sim.call("issue", aliceCommitment, issuer);
  const challenge = bytes(10);
  const path = sim.ledger().activeCredentials.findPathForLeaf(aliceCommitment);
  assert.ok(path);
  await assert.rejects(
    sim.call("present", challenge, { ...bob, path }),
    /Path does not match the credential/,
  );

  const tampered = structuredClone(path);
  tampered.path[0].sibling.field += 1n;
  await assert.rejects(
    sim.call("present", challenge, { ...alice, path: tampered }),
    /Credential is not in the current active tree/,
  );
  assert.equal(sim.ledger().acceptedChallenges.isEmpty(), true);
  await sim.call("present", challenge, alice);
});

test("revocation invalidates saved paths and previously prepared presentations", async () => {
  const sim = await workshop();
  await sim.call("issue", aliceCommitment, issuer);
  await sim.call("issue", bobCommitment, issuer);
  const acceptedBeforeRevocation = bytes(10);
  await sim.call("present", acceptedBeforeRevocation, alice);
  const alicePath = sim.ledger().activeCredentials.findPathForLeaf(aliceCommitment);
  const bobPath = sim.ledger().activeCredentials.findPathForLeaf(bobCommitment);
  assert.ok(alicePath && bobPath);

  const challenge = bytes(11);
  const prepared = await sim.evaluate("present", challenge, alice);
  const [presentation] = prepared.context.callProofDataTrace;
  const transcript = {
    gas: prepared.gasCost,
    effects: presentation.finalQueryContext.effects,
    program: presentation.publicTranscript,
  };
  const costModel = prepared.context.costModel;
  const applied = presentation.initialQueryContext.runTranscript(transcript, costModel);
  assert.equal(ledger(applied.state).acceptedChallenges.member(challenge), true);
  assert.throws(() => applied.runTranscript(transcript, costModel));

  const revoked = await sim.call("revoke", aliceCommitment, issuer);
  const [revocation] = revoked.context.callProofDataTrace;
  assert.throws(() => revocation.finalQueryContext.runTranscript(transcript, costModel));
  assert.deepEqual(sim.ledger().credentials.lookup(aliceCommitment), { slot: 0n, active: false });
  assert.equal(sim.ledger().activeCredentials.findPathForLeaf(aliceCommitment), undefined);
  assert.equal(sim.ledger().acceptedChallenges.member(acceptedBeforeRevocation), true);
  await assert.rejects(sim.call("revoke", aliceCommitment, issuer), /already revoked/);
  await assert.rejects(sim.call("issue", aliceCommitment, issuer), /already been issued/);

  await assert.rejects(
    sim.call("present", challenge, { ...alice, path: alicePath }),
    /Credential is not in the current active tree/,
  );
  await assert.rejects(
    sim.call("present", challenge, { ...bob, path: bobPath }),
    /Credential is not in the current active tree/,
  );
  assert.equal(sim.ledger().acceptedChallenges.member(challenge), false);
  await sim.call("present", challenge, bob);
  assert.equal(sim.ledger().acceptedChallenges.member(challenge), true);
});

test("two eligible holders produce the same public transcript at the same root and nonce", async () => {
  const sim = await workshop();
  await sim.call("issue", aliceCommitment, issuer);
  await sim.call("issue", bobCommitment, issuer);
  const challenge = bytes(10);
  const aliceResult = await sim.evaluate("present", challenge, alice);
  const bobResult = await sim.evaluate("present", challenge, bob);
  assert.deepEqual(aliceResult.result, []);
  assert.deepEqual(bobResult.result, []);

  assert.equal(aliceResult.context.callProofDataTrace.length, 1);
  assert.equal(bobResult.context.callProofDataTrace.length, 1);
  const [aliceCall] = aliceResult.context.callProofDataTrace;
  const [bobCall] = bobResult.context.callProofDataTrace;
  assert.deepEqual(aliceCall.input, bobCall.input);
  assert.deepEqual(aliceCall.output, bobCall.output);
  assert.deepEqual(aliceCall.publicTranscript, bobCall.publicTranscript);
  assert.notDeepEqual(aliceCall.privateTranscriptOutputs, bobCall.privateTranscriptOutputs);
});
