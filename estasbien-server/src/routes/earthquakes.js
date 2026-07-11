import { fetchUsgsQuakes } from '../services/usgs-client.js';

export async function earthquakesRoutes(app) {
  let cache = { data: null, ts: 0 };
  const CACHE_TTL = 60_000; // 1 minute

  app.get('/recent', async (req, reply) => {
    const now = Date.now();
    if (cache.data && now - cache.ts < CACHE_TTL) {
      return reply.send(cache.data);
    }

    const hours = Number(req.query.hours) || 72;
    const minMag = Number(req.query.minMag) || 3.0;

    const quakes = await fetchUsgsQuakes({ hoursBack: hours, minMagnitude: minMag });
    cache = { data: quakes, ts: now };
    reply.send(quakes);
  });
}
