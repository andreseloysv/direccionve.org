import Database from 'better-sqlite3';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { existsSync, mkdirSync } from 'fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const DATA_DIR = join(__dirname, '..', '..', 'data');

export function initDb() {
  if (!existsSync(DATA_DIR)) mkdirSync(DATA_DIR, { recursive: true });

  const db = new Database(join(DATA_DIR, 'estasbien.db'));
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');
  return db;
}
