const express = require('express');
const router = express.Router();
const db = require('../db');

// POST /api/comments — 添加评论，返回含 author 的完整评论对象
router.post('/', async (req, res) => {
  const { post_id, content, user_id } = req.body;
  try {
    const { rows } = await db.query(
      `INSERT INTO comments (post_id, content, user_id)
       VALUES ($1, $2, $3)
       RETURNING
         id, post_id, user_id, content, created_at,
         (SELECT json_build_object('id', u.id, 'username', u.username, 'avatar_url', u.avatar_url)
          FROM users u WHERE u.id = user_id) AS author`,
      [post_id, content, user_id]
    );
    res.status(201).json(rows[0]);
  } catch (err) {
    console.error('POST /api/comments error:', err);
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/comments/:id — 删除评论
router.delete('/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM comments WHERE id = $1', [req.params.id]);
    res.status(204).send();
  } catch (err) {
    console.error('DELETE /api/comments error:', err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
