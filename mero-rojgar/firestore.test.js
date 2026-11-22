const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_np_123";
const BOB_UID = "bob_np_456";
const ADMIN_EMAIL = "abishekshakya1221@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("jobs").where("status", "==", "published").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauth",
      locale: "ne",
      activeRole: "job_seeker",
      visibility: "private",
      contactHidden: true,
      verificationLevel: "self_reported",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner create, partial update, cross-user denial, and immutable createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sita Sharma",
      locale: "ne",
      activeRole: "job_seeker",
      headline: "Civil Engineer",
      district: "Kathmandu",
      visibility: "private",
      contactHidden: true,
      expectedSalaryMin: 45000,
      expectedSalaryMax: 75000,
      experienceMonths: 24,
      educationLevel: "Bachelor",
      skills: ["AutoCAD", "Site Supervision"],
      verificationLevel: "self_reported",
      consentsRecruiterDiscovery: false,
      consentsRewardsTerms: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update on allowed fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      locale: "en",
      visibility: "recruiter_visible",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verification level cannot be self-upgraded
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verificationLevel: "identity_checked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt cannot be modified
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Append-only ledger_entries and single-completion reward_completions invariants", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  // Create one completion for camp_1
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reward_completions")
      .doc("camp_1")
      .set({
        userId: ALICE_UID,
        campaignId: "camp_1",
        campaignTitle: "Nepal Telecom Fiber Awareness",
        sponsorName: "Nepal Telecom",
        watchedMs: 15000,
        questionPassed: true,
        rewardPaisa: 2500,
        status: "approved",
        createdAt: past,
      })
  );

  // Second completion for same campaign fails (update forbidden, create blocked by !exists)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reward_completions")
      .doc("camp_1")
      .set({
        userId: ALICE_UID,
        campaignId: "camp_1",
        campaignTitle: "Nepal Telecom Fiber Awareness",
        sponsorName: "Nepal Telecom",
        watchedMs: 15000,
        questionPassed: true,
        rewardPaisa: 2500,
        status: "approved",
        createdAt: past,
      })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("reward_completions")
      .doc("camp_1")
      .delete()
  );

  // Create ledger entry
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledger_entries")
      .doc("txn_1")
      .set({
        userId: ALICE_UID,
        txnId: "txn_1",
        accountKind: "user_available",
        direction: "credit",
        amountPaisa: 2500,
        reason: "completion_approved",
        campaignId: "camp_1",
        idempotencyKey: "camp_1_" + ALICE_UID,
        createdAt: past,
      })
  );

  // Ledger entries cannot be updated or deleted (append-only)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledger_entries")
      .doc("txn_1")
      .update({ amountPaisa: 999999 })
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledger_entries")
      .doc("txn_1")
      .delete()
  );

  // Cross-user ledger read fails
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ledger_entries").get()
  );
});

test("Jobs collection: filtered published query succeeds, unfiltered fails, owner update succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb.collection("jobs").doc("job_ktm_1").set({
      postedByUserId: ALICE_UID,
      orgName: "Himalayan Hydropower Ltd.",
      orgPan: "601234567",
      orgVerificationLevel: 2,
      title: "Senior Civil Supervisor",
      titleNe: "वरिष्ठ सिभिल सुपरभाइजर",
      description: "Lead site supervision and quality control in Kathmandu & Sindhupalchok.",
      employmentType: "full_time",
      workMode: "onsite",
      district: "Kathmandu",
      salaryMin: 55000,
      salaryMax: 85000,
      salaryVisible: true,
      requiredSkills: ["AutoCAD", "Site Supervision"],
      minExperienceMonths: 24,
      educationLevel: "Bachelor",
      urgent: true,
      scamRiskScore: 0.05,
      scamRiskReason: "Verified PAN and transparent NPR salary",
      status: "published",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Aligned filtered query succeeds
  await assertSucceeds(
    bobDb.collection("jobs").where("status", "==", "published").get()
  );

  // Unfiltered query on jobs fails
  await assertFails(bobDb.collection("jobs").get());

  // Non-owner cannot mutate Alice's job
  await assertFails(
    bobDb.collection("jobs").doc("job_ktm_1").update({
      title: "Scam Job",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner can pause job
  await assertSucceeds(
    aliceDb.collection("jobs").doc("job_ktm_1").update({
      status: "paused",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Employer applicants & talent directory: query alignment and opt-in isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    bobDb.collection("employer_applicants").doc("app_1").set({
      employerUserId: ALICE_UID,
      applicantUserId: BOB_UID,
      applicantName: "Bikash Tamang",
      applicantHeadline: "Site Supervisor",
      jobId: "job_ktm_1",
      jobTitle: "Senior Civil Supervisor",
      stage: "new",
      matchScore: 86,
      matchReasons: "Skills 90%, Location 100%",
      source: "manual",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Employer query filtered by employerUserId succeeds
  await assertSucceeds(
    aliceDb.collection("employer_applicants").where("employerUserId", "==", ALICE_UID).get()
  );

  // Employer moves stage
  await assertSucceeds(
    aliceDb.collection("employer_applicants").doc("app_1").update({
      stage: "shortlisted",
      notes: "Strong hydropower background",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Talent directory allows opt-in visibility query
  await assertSucceeds(
    bobDb.collection("talent_directory").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bikash Tamang",
      headline: "Site Supervisor",
      district: "Lalitpur",
      skills: ["AutoCAD"],
      verificationLevel: "self_reported",
      visibility: "recruiter_visible",
      openStatus: "open_to_opportunities",
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("talent_directory")
      .where("visibility", "in", ["public", "recruiter_visible"])
      .get()
  );
});

test("Admin approval on withdrawal succeeds only with verified admin email", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_1", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_1", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("withdrawals")
      .doc("w_1")
      .set({
        userId: ALICE_UID,
        amountPaisa: 15000,
        method: "esewa",
        destinationMasked: "9841****89",
        status: "requested",
        createdAt: past,
        updatedAt: past,
      })
  );

  // Unverified email spoof fails
  await assertFails(
    spoofAdminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("withdrawals")
      .doc("w_1")
      .update({
        status: "approved",
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Verified admin succeeds
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("withdrawals")
      .doc("w_1")
      .update({
        status: "approved",
        providerRef: "ESEWA-REF-9921",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Saved jobs: owner bookmark create/list/delete succeeds, cross-user and update denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_jobs")
      .doc("job_ktm_1")
      .set({
        userId: ALICE_UID,
        jobId: "job_ktm_1",
        jobTitle: "Senior Civil Supervisor",
        orgName: "Himalayan Hydropower Ltd.",
        savedAt: past,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_jobs").get()
  );

  // Cross-user read/write fails
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_jobs").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_jobs")
      .doc("job_ktm_2")
      .set({
        userId: ALICE_UID,
        jobId: "job_ktm_2",
        savedAt: past,
      })
  );

  // Update denied, owner delete succeeds
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_jobs")
      .doc("job_ktm_1")
      .update({ jobTitle: "Edited" })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_jobs")
      .doc("job_ktm_1")
      .delete()
  );
});

