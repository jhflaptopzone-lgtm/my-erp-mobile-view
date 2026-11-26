const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

test("Unauthenticated user: cannot read or write workspace or sync events", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      erpAccessId: "jhfadmin",
      serverUrl: "https://jhflaptop.cloud.run",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sync_events").get()
  );
});

test("Authenticated owner: valid workspace create and partial update succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "System Admin",
      erpAccessId: "jhfadmin",
      serverUrl: "https://jhflaptop.cloud.run",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Jahid Hasan",
      erpAccessId: "jhfadmin",
      serverUrl: "https://jhflaptop.cloud.run",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").doc("evt_bad").set({
      id: "evt_bad",
      action: "SALE",
      entityType: "sales",
      summary: "Bad extra field",
      serverSynced: true,
      createdAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").doc("evt_future").set({
      action: "SALE",
      entityType: "sales",
      summary: "Future timestamp",
      serverSynced: true,
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: sync_events create, list, and delete succeed; update blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").doc("evt_1").set({
      action: "SALE",
      entityType: "sales",
      summary: "Completed sale JHF-20261126-0001",
      serverSynced: true,
      createdAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").orderBy("createdAt", "desc").limit(30).get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").doc("evt_1").update({
      summary: "Tampered",
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sync_events").doc("evt_1").delete()
  );
});

test("Cross-user isolation: Alice cannot read or write Bob's workspace or sync_events", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      erpAccessId: "bob",
      serverUrl: "https://jhflaptop.cloud.run",
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
    await db.collection("users").doc(BOB_UID).collection("sync_events").doc("bob_evt").set({
      action: "LOGIN",
      entityType: "auth",
      summary: "Bob logged in",
      serverSynced: true,
      createdAt: new Date(Date.now() - 5000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ displayName: "Hijacked" }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("sync_events").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("sync_events").doc("evt_hijack").set({
      action: "HIJACK",
      entityType: "sales",
      summary: "Unauthorized",
      serverSynced: false,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});
