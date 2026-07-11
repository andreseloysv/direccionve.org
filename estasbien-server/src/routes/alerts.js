const respondSchema = {
  body: {
    type: 'object',
    required: ['userId', 'status'],
    properties: {
      userId: { type: 'string', minLength: 1 },
      status: { type: 'string', enum: ['SAFE', 'HELP'] },
    },
  },
};

export async function alertsRoutes(app) {
  const db = app.db;

  app.post('/:earthquakeId/respond', { schema: respondSchema }, (req, reply) => {
    const { earthquakeId } = req.params;
    const { userId, status } = req.body;

    const alert = db.prepare('SELECT id FROM earthquake_alerts WHERE id = ?').get(earthquakeId);
    if (!alert) return reply.code(404).send({ error: 'Alert not found' });

    const user = db.prepare('SELECT id FROM users WHERE id = ?').get(userId);
    if (!user) return reply.code(404).send({ error: 'User not found' });

    db.prepare(`
      INSERT INTO responses (user_id, alert_id, status)
      VALUES (?, ?, ?)
      ON CONFLICT(user_id, alert_id) DO UPDATE SET status = ?, responded_at = datetime('now')
    `).run(userId, earthquakeId, status, status);

    reply.send({ ok: true, status });
  });

  app.get('/:earthquakeId/responses', (req, reply) => {
    const { earthquakeId } = req.params;

    const summary = db.prepare(`
      SELECT status, COUNT(*) as count
      FROM responses WHERE alert_id = ?
      GROUP BY status
    `).all(earthquakeId);

    const zones = db.prepare(`
      SELECT u.plus_code, r.status, r.responded_at
      FROM responses r JOIN users u ON r.user_id = u.id
      WHERE r.alert_id = ?
    `).all(earthquakeId);

    reply.send({
      earthquakeId,
      summary: Object.fromEntries(summary.map(s => [s.status, s.count])),
      zones,
    });
  });

  app.get('/active', (req, reply) => {
    const alerts = db.prepare(`
      SELECT a.*, 
        (SELECT COUNT(*) FROM responses r WHERE r.alert_id = a.id AND r.status = 'SAFE') as safe_count,
        (SELECT COUNT(*) FROM responses r WHERE r.alert_id = a.id AND r.status = 'HELP') as help_count,
        (SELECT COUNT(*) FROM responses r WHERE r.alert_id = a.id AND r.status = 'NO_RESPONSE') as no_response_count
      FROM earthquake_alerts a
      WHERE a.created_at > datetime('now', '-24 hours')
      ORDER BY a.created_at DESC
    `).all();

    reply.send(alerts);
  });
}
