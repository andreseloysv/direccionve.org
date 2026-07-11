import { notifyEmergencyContacts } from './notify.js';

const CHECK_INTERVAL = 2 * 60_000; // every 2 minutes
const TIMEOUT_MINUTES = 30;

/**
 * Checks for users who haven't responded within 30 minutes.
 * Marks them as NO_RESPONSE and notifies their emergency contacts.
 */
export function startTimeoutChecker(db) {
  console.log(`Timeout checker started (every 2 min, ${TIMEOUT_MINUTES} min window)`);

  function check() {
    try {
      // Find NO_RESPONSE entries older than 30 minutes that haven't been escalated
      const expired = db.prepare(`
        SELECT r.user_id, r.alert_id, r.responded_at
        FROM responses r
        JOIN earthquake_alerts a ON r.alert_id = a.id
        WHERE r.status = 'NO_RESPONSE'
          AND a.created_at < datetime('now', '-${TIMEOUT_MINUTES} minutes')
          AND a.created_at > datetime('now', '-24 hours')
      `).all();

      for (const entry of expired) {
        notifyEmergencyContacts(db, entry.user_id, entry.alert_id, 'NO_RESPONSE');
      }

      if (expired.length > 0) {
        console.log(`Timeout checker: ${expired.length} users unresponsive`);
      }
    } catch (err) {
      console.error('Timeout check error:', err.message);
    }
  }

  setTimeout(check, 30_000);
  setInterval(check, CHECK_INTERVAL);
}
