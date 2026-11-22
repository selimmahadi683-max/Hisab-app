const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_user_123";
const BOB_UID = "bob_user_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write transactions or budgets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/budgets`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/transactions/tx_1`).set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountMinor: 50000,
      currencyCode: "BDT",
      categoryId: "food",
      paymentMethodId: "bkash",
      title: "Lunch",
      note: "",
      occurredAtMillis: Date.now() - 5000,
      clientUpdatedAtMillis: Date.now() - 5000,
      deleted: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid Kotlin transaction create, partial update, and soft-delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const nowMillis = Date.now() - 5000;

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_001`).set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountMinor: 125000,
      currencyCode: "BDT",
      categoryId: "food",
      paymentMethodId: "bkash",
      title: "Bazar",
      note: "Vegetables & fish",
      occurredAtMillis: nowMillis,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_001`).update({
      amountMinor: 130000,
      title: "Weekly Bazar",
      clientUpdatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/transactions`).get()
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_001`).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_002`).set({
      id: "tx_002",
      userId: ALICE_UID,
      type: "EXPENSE",
      amountMinor: 5000,
      currencyCode: "KWD",
      categoryId: "transport",
      paymentMethodId: "knet",
      title: "Taxi",
      note: "",
      occurredAtMillis: nowMillis,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid currency
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/transactions/tx_003`).set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amountMinor: 5000,
      currencyCode: "USD",
      categoryId: "food",
      paymentMethodId: "cash",
      title: "Coffee",
      note: "",
      occurredAtMillis: nowMillis,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Authenticated owner: valid budget create & update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const nowMillis = Date.now() - 5000;

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/budgets/BDT_OVERALL`).set({
      userId: ALICE_UID,
      currencyCode: "BDT",
      categoryId: "OVERALL",
      limitMinor: 3000000,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/budgets/BDT_OVERALL`).update({
      limitMinor: 3500000,
      clientUpdatedAtMillis: Date.now(),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/budgets`).get()
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's transactions or budgets", async () => {
  const created = new Date(Date.now() - 10000);
  const nowMillis = Date.now() - 10000;
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.doc(`users/${BOB_UID}/transactions/bob_tx`).set({
      userId: BOB_UID,
      type: "EXPENSE",
      amountMinor: 25000,
      currencyCode: "BDT",
      categoryId: "food",
      paymentMethodId: "cash",
      title: "Bob Lunch",
      note: "",
      occurredAtMillis: nowMillis,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    });
    await db.doc(`users/${BOB_UID}/budgets/KWD_OVERALL`).set({
      userId: BOB_UID,
      currencyCode: "KWD",
      categoryId: "OVERALL",
      limitMinor: 400000,
      clientUpdatedAtMillis: nowMillis,
      deleted: false,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/transactions`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/transactions/bob_tx`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/transactions/bob_tx`).update({ title: "Hacked" }));
  await assertFails(aliceDb.doc(`users/${BOB_UID}/budgets/KWD_OVERALL`).get());
});
