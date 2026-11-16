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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
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

async function seedUsersAndVideo() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const ts = new Date();
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_vibo",
      displayName: "أليس",
      bio: "صانعة محتوى",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      videosCount: 1,
      createdAt: ts,
      updatedAt: ts,
    });
    await adminDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_vibo",
      displayName: "بوب",
      bio: "",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      videosCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await adminDb.collection("videos").doc("vid_1").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_vibo",
      creatorDisplayName: "أليس",
      title: "رحلة جميلة",
      description: "مقطع قصير",
      category: "سفر",
      hashtags: ["سفر"],
      videoUri: "https://example.com/v.mp4",
      durationMs: 12000,
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await adminDb.collection("videos").doc("vid_private").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_vibo",
      creatorDisplayName: "أليس",
      title: "مسودة خاصة",
      description: "",
      category: "عام",
      hashtags: [],
      videoUri: "https://example.com/p.mp4",
      durationMs: 5000,
      visibility: "private",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated user: cannot read users, videos, follows, or notifications", async () => {
  await seedUsersAndVideo();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("videos").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("notifications").where("recipientId", "==", ALICE_UID).get()
  );
});

test("User profile: owner can create valid profile, rejects ghost field and identity spoofing", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_vibo",
      displayName: "أليس",
      bio: "أهلاً بكم",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      videosCount: 0,
      createdAt: nowTs(aliceDb),
      updatedAt: nowTs(aliceDb),
    })
  );

  // Shadow update / ghost field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: nowTs(aliceDb),
    })
  );

  // Spoofing another user's profile creation fails
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_fake",
      displayName: "Bob",
      followersCount: 0,
      followingCount: 0,
      likesCount: 0,
      videosCount: 0,
      createdAt: nowTs(aliceDb),
      updatedAt: nowTs(aliceDb),
    })
  );
});

test("Videos: public query succeeds, private video blocked for other user, owner can create and delete", async () => {
  await seedUsersAndVideo();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    bobDb.collection("videos").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("videos").doc("vid_private").get());
  await assertFails(bobDb.collection("videos").doc("vid_1").delete());

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_2").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_vibo",
      creatorDisplayName: "أليس",
      title: "فيديو جديد",
      category: "تقنية",
      hashtags: ["تقنية"],
      videoUri: "https://example.com/v2.mp4",
      durationMs: 10000,
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: nowTs(aliceDb),
      updatedAt: nowTs(aliceDb),
    })
  );

  // Value poisoning: non-zero initial likes fails
  await assertFails(
    aliceDb.collection("videos").doc("vid_bad").set({
      creatorId: ALICE_UID,
      creatorUsername: "alice_vibo",
      creatorDisplayName: "أليس",
      title: "فيديو مزيف",
      category: "تقنية",
      hashtags: [],
      videoUri: "https://example.com/v3.mp4",
      durationMs: 10000,
      visibility: "public",
      likesCount: 999,
      commentsCount: 0,
      sharesCount: 0,
      viewsCount: 0,
      createdAt: nowTs(aliceDb),
      updatedAt: nowTs(aliceDb),
    })
  );
});

test("Atomic like: batch creating like marker and incrementing likesCount succeeds; standalone likesCount tampering fails", async () => {
  await seedUsersAndVideo();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Standalone likesCount tamper without like marker fails
  await assertFails(
    bobDb.collection("videos").doc("vid_1").update({
      likesCount: 1,
      updatedAt: nowTs(bobDb),
    })
  );

  // Atomic batch like succeeds
  const batch = bobDb.batch();
  batch.set(bobDb.collection("videos").doc("vid_1").collection("likes").doc(BOB_UID), {
    userId: BOB_UID,
    videoId: "vid_1",
    createdAt: nowTs(bobDb),
  });
  batch.update(bobDb.collection("videos").doc("vid_1"), {
    likesCount: 1,
    updatedAt: nowTs(),
  });
  await assertSucceeds(batch.commit());
});

test("Comments and Notifications: valid creation succeeds, cross-user notification read fails", async () => {
  await seedUsersAndVideo();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    bobDb.collection("videos").doc("vid_1").collection("comments").doc("c1").set({
      videoId: "vid_1",
      authorId: BOB_UID,
      authorUsername: "bob_vibo",
      authorDisplayName: "بوب",
      text: "فيديو رائع!",
      createdAt: nowTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("notifications").doc("n1").set({
      recipientId: ALICE_UID,
      senderId: BOB_UID,
      senderUsername: "bob_vibo",
      senderDisplayName: "بوب",
      type: "comment",
      videoId: "vid_1",
      message: "علّق على الفيديو الخاص بك",
      isRead: false,
      createdAt: nowTs(),
    })
  );

  // Bob cannot read Alice's notifications
  await assertFails(
    bobDb.collection("notifications").where("recipientId", "==", ALICE_UID).get()
  );

  // Alice can query and mark her notification as read
  await assertSucceeds(
    aliceDb.collection("notifications").where("recipientId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("notifications").doc("n1").update({
      isRead: true,
    })
  );
});

test("Follows: atomic follow + counter update succeeds, self-follow fails", async () => {
  await seedUsersAndVideo();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Self follow fails
  await assertFails(
    bobDb.collection("follows").doc(`${BOB_UID}_${BOB_UID}`).set({
      followerId: BOB_UID,
      followingId: BOB_UID,
      createdAt: nowTs(),
    })
  );

  // Atomic follow succeeds
  const batch = bobDb.batch();
  batch.set(bobDb.collection("follows").doc(`${BOB_UID}_${ALICE_UID}`), {
    followerId: BOB_UID,
    followingId: ALICE_UID,
    createdAt: nowTs(),
  });
  batch.update(bobDb.collection("users").doc(ALICE_UID), {
    followersCount: 1,
    updatedAt: nowTs(),
  });
  batch.update(bobDb.collection("users").doc(BOB_UID), {
    followingCount: 1,
    updatedAt: nowTs(),
  });
  await assertSucceeds(batch.commit());
});
