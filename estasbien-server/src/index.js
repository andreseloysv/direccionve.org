import 'dotenv/config';
import Fastify from 'fastify';
import cors from '@fastify/cors';
import rateLimit from '@fastify/rate-limit';
import { initDb } from './db/connection.js';
import { usersRoutes } from './routes/users.js';
import { alertsRoutes } from './routes/alerts.js';
import { earthquakesRoutes } from './routes/earthquakes.js';
import { startUsgsPoller } from './services/usgs-poller.js';
import { startTimeoutChecker } from './services/timeout-checker.js';

const app = Fastify({ logger: true });

await app.register(cors, { origin: true });
await app.register(rateLimit, { max: 100, timeWindow: '1 minute' });

const db = initDb();
app.decorate('db', db);

app.register(usersRoutes, { prefix: '/api/users' });
app.register(alertsRoutes, { prefix: '/api/alerts' });
app.register(earthquakesRoutes, { prefix: '/api/earthquakes' });

app.get('/health', () => ({ status: 'ok', service: 'estasbien' }));

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';

try {
  await app.listen({ port: PORT, host: HOST });
  startUsgsPoller(db);
  startTimeoutChecker(db);
} catch (err) {
  app.log.error(err);
  process.exit(1);
}
