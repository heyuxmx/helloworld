const express = require('express');
const router = express.Router();
const db = require('../db');
const { sendNotification } = require('../services/notification');

// GET /api/posts — 获取动态列表（含嵌套 author + comments + comment.author）
router.get('/', async (req, res) => {
  try {
    const { rows } = await db.query(`
      SELECT
        p.id, p.content, p.user_id, p.image_urls, p.video_url, p.likes, p.created_at,
        json_build_object(
          'id', u.id, 'username', u.username, 'avatar_url', u.avatar_url
        ) AS author,
        COALESCE(
          json_agg(
            json_build_object(
              'id',         c.id,
              'post_id',    c.post_id,
              'user_id',    c.user_id,
              'content',    c.content,
              'created_at', c.created_at,
              'author',     json_build_object(
                              'id', cu.id, 'username', cu.username, 'avatar_url', cu.avatar_url
                            )
            ) ORDER BY c.created_at ASC
          ) FILTER (WHERE c.id IS NOT NULL),
          '[]'::json
        ) AS comments
      FROM posts p
      LEFT JOIN users u  ON p.user_id = u.id
      LEFT JOIN comments c  ON c.post_id = p.id
      LEFT JOIN users cu ON c.user_id = cu.id
      GROUP BY p.id, u.id, u.username, u.avatar_url
      ORDER BY p.created_at DESC
      LIMIT 200
    `);
    res.json(rows);
  } catch (err) {
    console.error('GET /api/posts error:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/posts — 创建动态
router.post('/', async (req, res) => {
  const { content, user_id, image_urls = [], video_url = null } = req.body;
  try {
    const { rows } = await db.query(
      `INSERT INTO posts (content, user_id, image_urls, video_url)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [content, user_id, image_urls, video_url]
    );
    const post = rows[0];

    // 异步推送通知，不阻塞响应
    sendNotification(post).catch((err) => console.error('推送通知失败:', err));

    res.status(201).json(post);
  } catch (err) {
    console.error('POST /api/posts error:', err);
    res.status(500).json({ error: err.message });
  }
});

// DELETE /api/posts/:id — 删除动态
router.delete('/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM posts WHERE id = $1', [req.params.id]);
    res.status(204).send();
  } catch (err) {
    console.error('DELETE /api/posts error:', err);
    res.status(500).json({ error: err.message });
  }
});

// POST /api/posts/rpc/increment_likes — 点赞
router.post('/rpc/increment_likes', async (req, res) => {
  const { post_id } = req.body;
  try {
    await db.query('UPDATE posts SET likes = likes + 1 WHERE id = $1', [post_id]);
    res.json({ success: true });
  } catch (err) {
    console.error('increment_likes error:', err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
