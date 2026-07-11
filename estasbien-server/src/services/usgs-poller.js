import crypto from 'crypto';
import { fetchUsgsQuakes } from './usgs-client.js';
import { findUsersNear } from './geo.js';
import { notifyUsersForAlert } from './notify.js';

const POLL_INTERVAL = 5 * 60_000; // 5 minutes
const MIN_MAGNITUDE = 4.0;
const RADIUS_KM = 500;

/**
 * Polls USGS every 5 minutes for new significant earthquakes.
 * When a new one is found, creates an alert and notifies nearby users.
 */
export function startUsgsPoller(db) {
  console.log('USGS poller started (every 5 min, M≥4.0)');

  const insertAlert = db.prepare(`
    INSERT OR IGNORE INTO earthquake_alerts (id, usgs_id, magnitude, place, latitude, longitude, depth_km, quake_time)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
  `);

  async function poll() {
    try {
      const quakes = await fetchUsgsQuakes({ hoursBack: 1, minMagnitude: MIN_MAGNITUDE });

      for (const quake of quakes) {
        const alertId = crypto.randomUUID();
        const result = insertAlert.run(
          alertId, quake.id, quake.magnitude, quake.place,
          quake.latitude, quake.longitude, quake.depthKm,
          new Date(quake.time).toISOString()
        );

        // New alert — notify users
        if (result.changes > 0) {
          const alert = { id: alertId, ...quake };
          const users = findUsersNear(db, quake.latitude, quake.longitude, RADIUS_KM);
          console.log(`New quake: M${quake.magnitude} ${quake.place} — ${users.length} users nearby`);
          await notifyUsersForAlert(db, alert, users);
        }
      }
    } catch (err) {
      console.error('USGS poll error:', err.message);
    }
  }

  // First poll after 10 seconds, then every 5 minutes
  setTimeout(poll, 10_000);
  setInterval(poll, POLL_INTERVAL);
}
