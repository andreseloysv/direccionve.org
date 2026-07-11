import crypto from 'crypto';

const registerSchema = {
  body: {
    type: 'object',
    required: ['name', 'phone', 'plusCode', 'latitude', 'longitude'],
    properties: {
      name: { type: 'string', minLength: 1, maxLength: 100 },
      phone: { type: 'string', minLength: 5, maxLength: 20 },
      plusCode: { type: 'string', minLength: 4, maxLength: 20 },
      latitude: { type: 'number', minimum: -90, maximum: 90 },
      longitude: { type: 'number', minimum: -180, maximum: 180 },
      fcmToken: { type: 'string' },
    },
  },
};

export async function usersRoutes(app) {
  const db = app.db;

  app.post('/register', { schema: registerSchema }, (req, reply) => {
    const { name, phone, plusCode, latitude, longitude, fcmToken } = req.body;
    const id = crypto.randomUUID();

    db.prepare(`
      INSERT INTO users (id, name, phone, plus_code, latitude, longitude, fcm_token)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `).run(id, name, phone, plusCode, latitude, longitude, fcmToken || null);

    reply.code(201).send({ id, name, plusCode });
  });

  app.put('/:id/token', (req, reply) => {
    const { fcmToken } = req.body || {};
    if (!fcmToken) return reply.code(400).send({ error: 'fcmToken required' });

    const result = db.prepare(
      `UPDATE users SET fcm_token = ?, updated_at = datetime('now') WHERE id = ?`
    ).run(fcmToken, req.params.id);

    if (result.changes === 0) return reply.code(404).send({ error: 'User not found' });
    reply.send({ ok: true });
  });

  app.get('/:id/export', (req, reply) => {
    const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
    if (!user) return reply.code(404).send({ error: 'User not found' });

    const responses = db.prepare(`
      SELECT r.status, r.responded_at, a.usgs_id, a.magnitude, a.place
      FROM responses r JOIN earthquake_alerts a ON r.alert_id = a.id
      WHERE r.user_id = ?
    `).all(req.params.id);

    const header = 'nombre,telefono,plus_code,latitud,longitud,sismo_usgs_id,magnitud,lugar,estado,respondido_en';
    const rows = responses.map(r =>
      `"${user.name}","${user.phone}","${user.plus_code}",${user.latitude},${user.longitude},"${r.usgs_id}",${r.magnitude},"${r.place || ''}","${r.status}","${r.responded_at || ''}"`
    );

    if (rows.length === 0) {
      rows.push(`"${user.name}","${user.phone}","${user.plus_code}",${user.latitude},${user.longitude},"","","","",""`);
    }

    reply.header('Content-Type', 'text/csv')
      .header('Content-Disposition', 'attachment; filename="estasbien_datos.csv"')
      .send([header, ...rows].join('\n'));
  });

  app.delete('/:id', (req, reply) => {
    const result = db.prepare('DELETE FROM users WHERE id = ?').run(req.params.id);
    if (result.changes === 0) return reply.code(404).send({ error: 'User not found' });
    reply.send({ ok: true, message: 'All data deleted' });
  });
}
