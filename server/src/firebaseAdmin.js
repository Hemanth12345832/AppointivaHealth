const admin = require("firebase-admin");
const config = require("./config");

let initialized = false;

function initFirebase() {
  if (initialized) return;

  if (!config.FIREBASE_DATABASE_URL) {
    // Backend can still run in mock mode, but cannot persist.
    initialized = true;
    return;
  }

  try {
    if (config.FIREBASE_SERVICE_ACCOUNT_JSON) {
      const serviceAccount = JSON.parse(config.FIREBASE_SERVICE_ACCOUNT_JSON);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: config.FIREBASE_DATABASE_URL,
      });
    } else {
      // If running in GCP/with GOOGLE_APPLICATION_CREDENTIALS.
      admin.initializeApp({
        credential: admin.credential.applicationDefault(),
        databaseURL: config.FIREBASE_DATABASE_URL,
      });
    }
    initialized = true;
  } catch (e) {
    initialized = true;
    console.warn("[firebase] init failed, running in mock/non-persistent mode:", e?.message || e);
  }
}

function getDb() {
  initFirebase();
  try {
    return admin.database();
  } catch (e) {
    return null;
  }
}

module.exports = {
  initFirebase,
  getDb,
};

