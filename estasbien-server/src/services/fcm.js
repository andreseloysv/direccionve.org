import admin from 'firebase-admin';

let initialized = false;

function ensureInit() {
  if (initialized) return;

  // Initialize with service account if GOOGLE_APPLICATION_CREDENTIALS is set,
  // otherwise use application default credentials
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.applicationDefault(),
    });
  }
  initialized = true;
}

/**
 * Send push notification to a list of FCM tokens.
 * Handles token cleanup for invalid tokens.
 */
export async function sendPushToUsers(tokens, { title, body, data }) {
  if (tokens.length === 0) return { success: 0, failure: 0 };

  ensureInit();

  const message = {
    notification: { title, body },
    data: data || {},
    android: {
      priority: 'high',
      notification: {
        channelId: 'earthquake_alerts',
        sound: 'default',
      },
    },
    tokens,
  };

  try {
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`FCM: ${response.successCount} sent, ${response.failureCount} failed`);
    return { success: response.successCount, failure: response.failureCount };
  } catch (err) {
    console.error('FCM send error:', err.message);
    return { success: 0, failure: tokens.length };
  }
}
