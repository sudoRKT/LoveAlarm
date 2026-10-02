// Love Alarm sender.
//
// Runs on the home server. Listens to Firestore for alerts with
// pushStatus == "pending", sends each one to the recipient's phone as a
// high-priority data-only FCM message, then marks it "sent".
// Alerts older than MAX_AGE_MINUTES when first seen (server was down) are
// marked "skipped" and not pushed. Outbound connections only.

import { readFileSync } from "node:fs";
import { initializeApp, cert } from "firebase-admin/app";
import { getFirestore, FieldValue, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

const KEY_PATH = process.env.GOOGLE_APPLICATION_CREDENTIALS || "/secrets/serviceAccountKey.json";
const MAX_AGE_MINUTES = Number(process.env.MAX_AGE_MINUTES || 30);

function log(line) {
  console.log(`${new Date().toISOString()} ${line}`);
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(readFileSync(KEY_PATH, "utf8"));
} catch (err) {
  console.error(`Cannot read service account key at ${KEY_PATH}: ${err.message}`);
  process.exit(1);
}

initializeApp({ credential: cert(serviceAccount), projectId: serviceAccount.project_id });
const db = getFirestore();
const messaging = getMessaging();

// Alert ids currently being handled, so a second snapshot for the same
// document does not cause a double send.
const inFlight = new Set();
// pairId -> unsubscribe function for that pair's alert listener.
const pairListeners = new Map();

function ageMinutes(createdAt) {
  const created = createdAt instanceof Timestamp ? createdAt.toMillis() : Date.now();
  return (Date.now() - created) / 60000;
}

async function handleAlert(pairId, doc) {
  const id = doc.id;
  if (inFlight.has(id)) return;
  inFlight.add(id);
  const ref = doc.ref;
  const alert = doc.data();
  const text = String(alert.text || "");
  const shortText = text.length > 40 ? text.slice(0, 37) + "..." : text;

  try {
    // Claim the alert so nothing else processes it.
    const claimed = await db.runTransaction(async (tx) => {
      const fresh = await tx.get(ref);
      if (!fresh.exists || fresh.get("pushStatus") !== "pending") return false;
      tx.update(ref, { pushStatus: "sending", claimedAt: FieldValue.serverTimestamp() });
      return true;
    });
    if (!claimed) return;

    const age = ageMinutes(alert.createdAt);
    if (age > MAX_AGE_MINUTES) {
      await ref.update({ pushStatus: "skipped", pushError: `too old (${Math.round(age)} min)` });
      log(`skipped alert=${id} pair=${pairId} to=${alert.toUid} reason=too-old age=${Math.round(age)}min "${shortText}"`);
      return;
    }

    const [toSnap, fromSnap] = await Promise.all([
      db.doc(`users/${alert.toUid}`).get(),
      db.doc(`users/${alert.fromUid}`).get(),
    ]);
    const token = toSnap.get("fcmToken");
    if (!token) {
      await ref.update({ pushStatus: "skipped", pushError: "recipient has no FCM token" });
      log(`skipped alert=${id} pair=${pairId} to=${alert.toUid} reason=no-token "${shortText}"`);
      return;
    }
    const fromName = fromSnap.get("name") || "Love Alarm";

    await messaging.send({
      token,
      android: { priority: "high", ttl: MAX_AGE_MINUTES * 60 * 1000 },
      data: {
        alertId: id,
        pairId,
        fromUid: String(alert.fromUid || ""),
        fromName: String(fromName),
        text,
        type: String(alert.type || "tile"),
        replyTo: String(alert.replyTo || ""),
        createdAt: String(alert.createdAt instanceof Timestamp ? alert.createdAt.toMillis() : Date.now()),
      },
    });

    await ref.update({ pushStatus: "sent", sentAt: FieldValue.serverTimestamp() });
    log(`sent alert=${id} pair=${pairId} from=${fromName} to=${alert.toUid} "${shortText}"`);
  } catch (err) {
    const code = err?.errorInfo?.code || err?.code || "unknown";
    const unregistered =
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token" ||
      code === "messaging/invalid-argument";
    try {
      await ref.update({ pushStatus: "skipped", pushError: `${code}: ${err.message}` });
    } catch (updateErr) {
      log(`error alert=${id} could not record failure: ${updateErr.message}`);
    }
    log(`failed alert=${id} pair=${pairId} to=${alert.toUid} code=${code}${unregistered ? " (phone token is stale; it refreshes on next app open)" : ""} "${shortText}"`);
  } finally {
    inFlight.delete(id);
  }
}

// If the sender died between claiming an alert and finishing it, the alert is
// stuck on "sending". Put those back to "pending" so they get handled again
// (the age check still applies, so nothing stale is pushed).
async function recoverStuck(pairId) {
  const stuck = await db.collection(`pairs/${pairId}/alerts`).where("pushStatus", "==", "sending").get();
  for (const doc of stuck.docs) {
    await doc.ref.update({ pushStatus: "pending" });
    log(`recovered alert=${doc.id} pair=${pairId} (was stuck on sending)`);
  }
}

function watchPair(pairId) {
  if (pairListeners.has(pairId)) return;
  recoverStuck(pairId).catch((err) => log(`recover error pair=${pairId}: ${err.message}`));
  const unsubscribe = db
    .collection(`pairs/${pairId}/alerts`)
    .where("pushStatus", "==", "pending")
    .onSnapshot(
      (snap) => {
        for (const change of snap.docChanges()) {
          if (change.type === "added") handleAlert(pairId, change.doc);
        }
      },
      (err) => {
        log(`listener error pair=${pairId}: ${err.message}; retrying in 10s`);
        pairListeners.delete(pairId);
        setTimeout(() => watchPair(pairId), 10000);
      }
    );
  pairListeners.set(pairId, unsubscribe);
  log(`watching pair=${pairId}`);
}

function watchPairs() {
  db.collection("pairs").onSnapshot(
    (snap) => {
      for (const change of snap.docChanges()) {
        if (change.type === "removed") {
          const stop = pairListeners.get(change.doc.id);
          if (stop) stop();
          pairListeners.delete(change.doc.id);
          log(`stopped watching pair=${change.doc.id}`);
        } else {
          watchPair(change.doc.id);
        }
      }
    },
    (err) => {
      log(`pairs listener error: ${err.message}; retrying in 10s`);
      setTimeout(watchPairs, 10000);
    }
  );
}

log(`love-alarm-sender starting project=${serviceAccount.project_id} maxAge=${MAX_AGE_MINUTES}min`);
watchPairs();

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    log(`received ${signal}, shutting down`);
    for (const stop of pairListeners.values()) stop();
    process.exit(0);
  });
}
