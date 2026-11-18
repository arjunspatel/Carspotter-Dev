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

function validCarSpotPayload(userId, overrides = {}) {
  const pastTime = new Date(Date.now() - 5000);
  return {
    userId,
    make: "Porsche",
    model: "911 GT3 RS",
    year: 2024,
    trim: "Weissach",
    rarityTier: "EXOTIC",
    rarityScore: 92,
    pointsAwarded: 850,
    horsepower: 518,
    zeroToSixtyMph: 3.0,
    topSpeedMph: 184,
    estimatedPriceUsd: 241300,
    engineType: "4.0L Flat-6",
    drivetrain: "RWD",
    description: "Track focused coupe",
    trivia: "High downforce rear wing",
    locationName: "Los Angeles, CA",
    latitude: 34.05,
    longitude: -118.24,
    timestamp: Date.now(),
    isAuthentic: true,
    antiCheatConfidence: 0.98,
    antiCheatDetails: "Verified real vehicle",
    isFirstDiscovery: true,
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
  };
}

function validSpotterPayload(userId, overrides = {}) {
  const pastTime = new Date(Date.now() - 5000);
  return {
    userId,
    username: "Alice Spotter",
    handle: "@alice_spotter",
    avatarUrl: null,
    totalScore: 850,
    uniqueSpotsCount: 1,
    totalSpotsCount: 1,
    rankTitle: "Street Scout (Tier II)",
    topCar: "2024 Porsche 911 GT3 RS",
    topHorsepower: 518,
    topSpeedMph: 184,
    isPublicProfile: true,
    recentSpots: [
      {
        id: "spot_1",
        carName: "2024 Porsche 911 GT3 RS",
        rarityTier: "EXOTIC",
        pointsEarned: 850,
        horsepower: 518,
        topSpeedMph: 184,
        locationName: "Los Angeles, CA",
        timestamp: Date.now(),
        isVerified: true,
      },
    ],
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
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

// --- UNAUTHENTICATED ACCESS TESTS ---

test("Unauthenticated user: cannot read spotters, car_spots, or friends", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("spotters").where("isPublicProfile", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("car_spots").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("friends").get());
});

test("Unauthenticated user: cannot write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("car_spots").doc("s1").set(validCarSpotPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("spotters").doc(ALICE_UID).set(validSpotterPayload(ALICE_UID))
  );
});

// --- PRIVATE GARAGE ISOLATION TESTS ---

test("Owner can create, list, update, and delete own car spot", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("car_spots").doc("spot_1");
  const initial = validCarSpotPayload(ALICE_UID);

  await assertSucceeds(ref.set(initial));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("car_spots").get());
  await assertSucceeds(
    ref.update({
      pointsAwarded: 950,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Cross-user isolation: Bob cannot read, create, update, or delete Alice's car spots", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("car_spots")
      .doc("spot_alice")
      .set(validCarSpotPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRef = bobDb.collection("users").doc(ALICE_UID).collection("car_spots").doc("spot_alice");

  await assertFails(aliceRef.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("car_spots").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("car_spots").doc("spot_new").set(validCarSpotPayload(ALICE_UID))
  );
  await assertFails(aliceRef.update({ pointsAwarded: 9999 }));
  await assertFails(aliceRef.delete());
});

test("CarSpot schema validation: rejects mismatched userId, ghost fields, and shadow createdAt updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spots = aliceDb.collection("users").doc(ALICE_UID).collection("car_spots");

  // Spoofed userId
  await assertFails(spots.doc("bad1").set(validCarSpotPayload(BOB_UID)));
  // Ghost field 'id' or 'admin'
  await assertFails(spots.doc("bad2").set(validCarSpotPayload(ALICE_UID, { id: "bad2" })));
  await assertFails(spots.doc("bad3").set(validCarSpotPayload(ALICE_UID, { admin: true })));
  // Invalid rarityTier
  await assertFails(spots.doc("bad4").set(validCarSpotPayload(ALICE_UID, { rarityTier: "MYTHIC" })));

  // Valid create, then reject mutating createdAt or userId
  await assertSucceeds(spots.doc("good1").set(validCarSpotPayload(ALICE_UID)));
  await assertFails(
    spots.doc("good1").update({ createdAt: new Date(Date.now() - 1000) })
  );
});

// --- PUBLIC SPOTTER DIRECTORY TESTS ---

test("Spotter directory: owner can write valid non-PII profile and signed-in users can query public profiles", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("spotters").doc(ALICE_UID).set(validSpotterPayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("spotters").where("isPublicProfile", "==", true).get());
  await assertSucceeds(bobDb.collection("spotters").doc(ALICE_UID).get());
});

test("Spotter directory: rejects unfiltered list, PII/ghost fields, cross-user writes, and recentSpots > 5", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unfiltered list without where("isPublicProfile", "==", true) must fail
  await assertFails(bobDb.collection("spotters").get());

  // Cross-user write fails
  await assertFails(bobDb.collection("spotters").doc(ALICE_UID).set(validSpotterPayload(ALICE_UID)));

  // Ghost PII field email fails
  await assertFails(
    aliceDb.collection("spotters").doc(ALICE_UID).set(validSpotterPayload(ALICE_UID, { email: "alice@example.com" }))
  );

  // Oversized recentSpots (> 5) fails
  const tooManySpots = Array.from({ length: 6 }, (_, i) => ({
    id: `s_${i}`,
    carName: "2024 Porsche 911",
    rarityTier: "EXOTIC",
    pointsEarned: 850,
    horsepower: 500,
    topSpeedMph: 180,
    locationName: "LA",
    timestamp: Date.now(),
    isVerified: true,
  }));
  await assertFails(
    aliceDb.collection("spotters").doc(ALICE_UID).set(validSpotterPayload(ALICE_UID, { recentSpots: tooManySpots }))
  );
});

// --- FRIEND LINKS TESTS ---

test("Friend links: owner can add/list/remove another user; rejects self-friend, update, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const pastTime = new Date(Date.now() - 5000);

  // Valid friend add
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).set({
      userId: ALICE_UID,
      friendId: BOB_UID,
      createdAt: pastTime,
    })
  );

  // Cannot self-friend
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(ALICE_UID).set({
      userId: ALICE_UID,
      friendId: ALICE_UID,
      createdAt: pastTime,
    })
  );

  // Cannot update friend link
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).update({
      friendId: "someone_else",
    })
  );

  // Bob cannot read or delete Alice's friends
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());

  // Alice can list and remove friend
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("friends").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("friends").doc(BOB_UID).delete());
});

// --- PUBLIC SIGHTINGS RADAR TESTS ---

function validPublicSightingPayload(userId, overrides = {}) {
  const pastTime = new Date(Date.now() - 5000);
  return {
    userId,
    spotterName: "Alice Spotter",
    spotterHandle: "@alice_spotter",
    spotterAvatarUrl: null,
    make: "Porsche",
    model: "911 GT3 RS",
    year: 2024,
    trim: "Weissach",
    rarityTier: "EXOTIC",
    rarityScore: 92,
    pointsAwarded: 850,
    horsepower: 518,
    zeroToSixtyMph: 3.0,
    topSpeedMph: 184,
    estimatedPriceUsd: 241300,
    engineType: "4.0L Flat-6",
    drivetrain: "RWD",
    description: "Track coupe",
    trivia: "High downforce",
    locationName: "Los Angeles, CA",
    latitude: 34.0522,
    longitude: -118.2437,
    timestamp: Date.now(),
    isAuthentic: true,
    antiCheatConfidence: 0.99,
    antiCheatDetails: "Verified real vehicle",
    isPublic: true,
    createdAt: pastTime,
    updatedAt: pastTime,
    ...overrides,
  };
}

test("Public sightings radar: signed-in users can create own sighting and query isPublic == true", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_sightings").where("isPublic", "==", true).get());

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("public_sightings").doc("sighting_1").set(validPublicSightingPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("public_sightings").where("isPublic", "==", true).get());
  await assertFails(bobDb.collection("public_sightings").get());
  await assertFails(bobDb.collection("public_sightings").doc("sighting_1").delete());
  await assertFails(
    bobDb.collection("public_sightings").doc("spoofed").set(validPublicSightingPayload(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("public_sightings").doc("bad_coords").set(validPublicSightingPayload(ALICE_UID, { latitude: 120.0 }))
  );
  await assertSucceeds(aliceDb.collection("public_sightings").doc("sighting_1").delete());
});

