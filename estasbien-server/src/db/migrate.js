import { initDb } from './connection.js';

const SCHEMA = `
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  plus_code TEXT NOT NULL,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  fcm_token TEXT,
  created_at TEXT DEFAULT (datetime('now')),
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS emergency_contacts (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  phone TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS earthquake_alerts (
  id TEXT PRIMARY KEY,
  usgs_id TEXT UNIQUE NOT NULL,
  magnitude REAL NOT NULL,
  place TEXT,
  latitude REAL NOT NULL,
  longitude REAL NOT NULL,
  depth_km REAL,
  quake_time TEXT NOT NULL,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS responses (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  alert_id TEXT NOT NULL REFERENCES earthquake_alerts(id) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK(status IN ('SAFE','HELP','NO_RESPONSE')),
  responded_at TEXT DEFAULT (datetime('now')),
  UNIQUE(user_id, alert_id)
);

CREATE INDEX IF NOT EXISTS idx_users_location ON users(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_responses_alert ON responses(alert_id, status);
CREATE INDEX IF NOT EXISTS idx_alerts_time ON earthquake_alerts(created_at);
`;

const db = initDb();
db.exec(SCHEMA);
console.log('Migration complete.');
db.close();
