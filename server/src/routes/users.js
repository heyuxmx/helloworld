const express = require('express');
const router = express.Router();
const db = require('../db');

// GET /api/users — 获取所有用户
router.get('/', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM users ORDER BY created_at DESC');
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// GET /api/users/:id — 获取单个用户
router.get('/:id', async (req, res) => {
  try {
    const { rows } = await db.query('SELECT * FROM users WHERE id = $1', [req.params.id]);
    if (rows.length === 0) return res.status(404).json({ error: 'User not found' });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PATCH /api/users/:id — 更新用户名、头像URL 或 FCM Token
router.patch('/:id', async (req, res) => {
  const { username, avatar_url, fcm_token } = req.body;
  const updates = [];
  const values = [];
  let idx = 1;

  if (username !== undefined)   { updates.push(`username = $${idx++}`);  values.push(username); }
  if (avatar_url !== undefined) { updates.push(`avatar_url = $${idx++}`); values.push(avatar_url); }
  if (fcm_token !== undefined)  { updates.push(`fcm_token = $${idx++}`);  values.push(fcm_token); }

  if (updates.length === 0) return res.status(400).json({ error: 'No fields to update' });

  values.push(req.params.id);
  try {
    const { rows } = await db.query(
      `UPDATE users SET ${updates.join(', ')} WHERE id = $${idx} RETURNING *`,
      values
    );
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
