const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_herbal_123";
const BOB_UID = "bob_herbal_456";

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

function buildValidFormulaPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: "ยาต้มแก้ไอสูตรบ้าน",
    englishName: "Home Cough Decoction",
    category: "RESPIRATORY",
    tasteProfile: "ขม เย็น",
    indications: "บรรเทาอาการไอ ขับเสมหะ ชุ่มคอ",
    ingredientsSummary: "มะแว้งเครือ 10 กรัม, ชะเอมเทศ 5 กรัม, มะขามป้อม 10 กรัม",
    preparationAndDosage: "ต้มกับน้ำสะอาด 3 แก้ว เคี่ยวเหลือ 1 แก้ว ดื่มครั้งละครึ่งแก้ว เช้า-เย็น",
    precautions: "ไม่ควรใช้ติดต่อกันเกิน 7 วัน หากอาการไม่ดีขึ้นควรปรึกษาแพทย์",
    referenceSource: "บันทึกตำรับสมุนไพรประจำบ้าน",
    externalUse: false,
    createdAt: now,
    updatedAt: now,
  };
}

function buildValidBookmarkPayload(uid, formulaId = "std_fah_talai_chon", now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    formulaId,
    formulaName: "ยาฟ้าทะลายโจร",
    personalNote: "รับประทานหลังอาหารเมื่อเริ่มมีอาการเจ็บคอ",
    favorite: true,
    createdAt: now,
    updatedAt: now,
  };
}

function buildValidRemedyPayload(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: "ขิง (Ginger)",
    properties: "ขับลม แก้คลื่นไส้อาเจียน แก้ท้องอืดท้องเฟ้อ",
    components: "เหง้าขิงแก่สดหรือแห้ง 100%",
    instructions: "ฝานเหง้าสด 5 กรัม ชงน้ำร้อนดื่มวันละ 3 ครั้ง",
    precautions: "ระวังในผู้ที่มีภาวะเลือดออกง่ายหรือใช้ยาละลายลิ่มเลือด",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or write formulas or bookmarks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/formulas`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/bookmarks`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/formulas`).doc("f1").set(buildValidFormulaPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/bookmarks`).doc("b1").set(buildValidBookmarkPayload(ALICE_UID))
  );
});

test("Authenticated owner: valid Kotlin formula create, list, update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const formulasRef = aliceDb.collection("users").doc(ALICE_UID).collection("formulas");

  // List empty collection must succeed (no existing() inspection on allow list)
  await assertSucceeds(formulasRef.get());

  // Create without @DocumentId `id`
  await assertSucceeds(
    formulasRef.doc("formula_1").set(buildValidFormulaPayload(ALICE_UID, created))
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    formulasRef.doc("formula_1").update({
      name: "ยาต้มแก้ไอสูตรบ้าน (ปรับปรุง)",
      precautions: "ห้ามใช้ในสตรีมีครรภ์",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List after create succeeds
  await assertSucceeds(formulasRef.get());

  // Delete succeeds
  await assertSucceeds(formulasRef.doc("formula_1").delete());
});

test("Authenticated owner: valid Kotlin bookmark create, update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const bookmarksRef = aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks");

  await assertSucceeds(bookmarksRef.get());

  await assertSucceeds(
    bookmarksRef.doc("std_fah_talai_chon").set(buildValidBookmarkPayload(ALICE_UID, "std_fah_talai_chon", created))
  );

  await assertSucceeds(
    bookmarksRef.doc("std_fah_talai_chon").update({
      personalNote: "อัปเดตบันทึกส่วนตัว",
      favorite: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(bookmarksRef.doc("std_fah_talai_chon").delete());
});

test("Security bounds: rejects ghost fields, id field, invalid category, future timestamp, and immutable mutation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const formulasRef = aliceDb.collection("users").doc(ALICE_UID).collection("formulas");

  // Reject `@DocumentId` `id` field in payload
  await assertFails(
    formulasRef.doc("formula_bad_id").set({
      id: "formula_bad_id",
      ...buildValidFormulaPayload(ALICE_UID, created),
    })
  );

  // Reject ghost field
  await assertFails(
    formulasRef.doc("formula_ghost").set({
      ...buildValidFormulaPayload(ALICE_UID, created),
      isAdminVerified: true,
    })
  );

  // Reject invalid category enum
  await assertFails(
    formulasRef.doc("formula_bad_cat").set({
      ...buildValidFormulaPayload(ALICE_UID, created),
      category: "INVALID_CATEGORY",
    })
  );

  // Reject future timestamp
  await assertFails(
    formulasRef.doc("formula_future").set(
      buildValidFormulaPayload(ALICE_UID, new Date(Date.now() + 60000))
    )
  );

  // Create valid doc then reject createdAt mutation
  await assertSucceeds(
    formulasRef.doc("formula_valid").set(buildValidFormulaPayload(ALICE_UID, created))
  );
  await assertFails(
    formulasRef.doc("formula_valid").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read, list, create, update, or delete Alice's formulas or bookmarks", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).collection("formulas").doc("alice_f1").set(
      buildValidFormulaPayload(ALICE_UID)
    );
    await db.collection("users").doc(ALICE_UID).collection("bookmarks").doc("std_triphala").set(
      buildValidBookmarkPayload(ALICE_UID, "std_triphala")
    );
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("formulas").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("formulas").doc("alice_f1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("formulas").doc("alice_f1").update({ name: "Hacked", updatedAt: new Date(Date.now() - 1000) }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("formulas").doc("alice_f1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("formulas").doc("bob_inject").set(buildValidFormulaPayload(BOB_UID)));

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("std_triphala").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("std_triphala").delete());
});

test("HerbRemedy CRUD rules: owner create/get/list/update/delete succeed and unauth/cross-user/invalid payloads fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const aliceRemediesRef = aliceDb.collection("users").doc(ALICE_UID).collection("remedies");

  // Unauthenticated denied
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/remedies`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/remedies`).doc("r1").set(buildValidRemedyPayload(ALICE_UID)));

  // Empty list succeeds for owner
  await assertSucceeds(aliceRemediesRef.get());

  // Valid create without @DocumentId `id`
  await assertSucceeds(
    aliceRemediesRef.doc("remedy_ginger").set(buildValidRemedyPayload(ALICE_UID, created))
  );

  // Get & list succeed
  await assertSucceeds(aliceRemediesRef.doc("remedy_ginger").get());
  await assertSucceeds(aliceRemediesRef.get());

  // Partial update preserving userId & createdAt succeeds
  await assertSucceeds(
    aliceRemediesRef.doc("remedy_ginger").update({
      properties: "ขับลม ลดอาการคลื่นไส้ อาเจียน และช่วยย่อยอาหาร",
      precautions: "ผู้มีโรคนิ่วในถุงน้ำดีควรปรึกษาแพทย์ก่อนใช้",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification, @DocumentId `id` key, and ghost fields
  await assertFails(
    aliceRemediesRef.doc("remedy_ginger").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceRemediesRef.doc("remedy_bad_id").set({
      id: "remedy_bad_id",
      ...buildValidRemedyPayload(ALICE_UID, created),
    })
  );
  await assertFails(
    aliceRemediesRef.doc("remedy_ghost").set({
      ...buildValidRemedyPayload(ALICE_UID, created),
      extraField: "disallowed",
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("remedies").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("remedies").doc("remedy_ginger").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("remedies").doc("remedy_ginger").update({
      name: "Tampered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("remedies").doc("remedy_ginger").delete());

  // Owner delete succeeds
  await assertSucceeds(aliceRemediesRef.doc("remedy_ginger").delete());
});
