import { sendPushToUsers } from './fcm.js';

/**
 * Send FCM push notification to affected users for an earthquake alert.
 */
export async function notifyUsersForAlert(db, alert, affectedUsers) {
  if (affectedUsers.length === 0) return;

  const tokens = affectedUsers.map(u => u.fcm_token).filter(Boolean);
  if (tokens.length === 0) return;

  // Create NO_RESPONSE entries for all affected users
  const insert = db.prepare(`
    INSERT OR IGNORE INTO responses (user_id, alert_id, status)
    VALUES (?, ?, 'NO_RESPONSE')
  `);

  const insertMany = db.transaction((users) => {
    for (const user of users) {
      insert.run(user.id, alert.id);
    }
  });
  insertMany(affectedUsers);

  // Send push
  await sendPushToUsers(tokens, {
    title: `Sismo M${alert.magnitude} detectado`,
    body: `${alert.place || 'Cerca de ti'} — ¿Estás bien? Toca para responder.`,
    data: {
      type: 'earthquake_alert',
      earthquakeId: alert.id,
      magnitude: String(alert.magnitude),
      place: alert.place || '',
    },
  });

  console.log(`Notified ${tokens.length} users for alert ${alert.id}`);
}

/**
 * Notify emergency contacts for users who need help or didn't respond.
 */
export async function notifyEmergencyContacts(db, userId, alertId, reason) {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(userId);
  if (!user) return;

  const contacts = db.prepare(
    'SELECT * FROM emergency_contacts WHERE user_id = ?'
  ).all(userId);

  if (contacts.length === 0) return;

  // TODO: Send SMS or push to contacts
  // For now, log the event
  console.log(
    `EMERGENCY: User ${user.name} (${reason}) — ${contacts.length} contacts to notify`
  );
}
