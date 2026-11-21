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

function validProfilePayload(uid, createdAt) {
  return {
    userId: uid,
    userName: "Amaya",
    setupDone: true,
    pin: "2468",
    themeMode: "light",
    petalsEnabled: true,
    petalStyle: "sakura",
    petalSize: 22,
    petalOpacityPct: 34,
    defaultCycleLength: 28,
    defaultPeriodDuration: 5,
    dailyWaterGoal: 2000,
    periodStarts: ["2026-11-01"],
    bfWhatsapp: "94771234567",
    bfEmail: "care@example.com",
    careNotePeriod: "Rest well and drink warm water.",
    careNotePms: "Take it easy today.",
    careNoteOvulation: "Stay hydrated.",
    careNoteNormal: "Keep drinking water.",
    createdAt,
    updatedAt: createdAt,
  };
}

function validDailyLogPayload(uid, dateStr, createdAt) {
  return {
    userId: uid,
    date: dateStr,
    periodDay: true,
    flow: "සාමාන්‍යයි (Medium)",
    pain: 2,
    genSeverity: 1,
    waterMl: 1200,
    moods: ["සතුටින් 😊"],
    symptoms: ["බඩේ කැක්කුම (Cramps)"],
    genSymptoms: ["හිසරදය (Headache)"],
    cravings: ["චොකලට් 🍫"],
    note: "Feeling calm today.",
    createdAt,
    updatedAt: createdAt,
  };
}

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

test("Unauthenticated user cannot read or write profile or daily logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, ts)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("daily_logs").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18")
      .set(validDailyLogPayload(ALICE_UID, "2026-11-18", ts))
  );
});

test("Owner can create, read, and partially update their profile; ghost fields & invalid PIN rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themeMode: "dark",
      dailyWaterGoal: 2500,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid PIN (not 4 digits)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pin: "12ab",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create, list, read, update, and delete daily logs; invalid date ID & future timestamp rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18")
      .set(validDailyLogPayload(ALICE_UID, "2026-11-18", created))
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18")
      .update({
        waterMl: 1450,
        pain: 1,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject invalid date document ID
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("bad_id")
      .set(validDailyLogPayload(ALICE_UID, "bad_id", created))
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-19")
      .set(validDailyLogPayload(ALICE_UID, "2026-11-19", new Date(Date.now() + 60000)))
  );

  // Reject out-of-range pain level
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18")
      .update({
        pain: 9,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("daily_logs").doc("2026-11-18").delete()
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile or daily logs", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(validProfilePayload(BOB_UID, created));
    await db.collection("users").doc(BOB_UID).collection("daily_logs").doc("2026-11-18")
      .set(validDailyLogPayload(BOB_UID, "2026-11-18", created));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ userName: "Hacked", updatedAt: new Date(Date.now() - 500) }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").doc("2026-11-18").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("daily_logs").doc("2026-11-18").delete());
});
