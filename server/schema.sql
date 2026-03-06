-- ZhuDe App database init script
-- Run in psql:
--   CREATE DATABASE zhudedb;
--   \c zhudedb
--   \encoding UTF8
--   \i schema.sql
\encoding UTF8

CREATE TABLE IF NOT EXISTS users (
  id         TEXT PRIMARY KEY,
  username   TEXT,
  avatar_url TEXT,
  fcm_token  TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS posts (
  id         BIGSERIAL PRIMARY KEY,
  content    TEXT NOT NULL DEFAULT '',
  user_id    TEXT REFERENCES users(id),
  image_urls TEXT[] DEFAULT '{}',
  video_url  TEXT,
  likes      INTEGER DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS comments (
  id         BIGSERIAL PRIMARY KEY,
  post_id    BIGINT REFERENCES posts(id) ON DELETE CASCADE,
  user_id    TEXT REFERENCES users(id),
  content    TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 索引优化
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);

-- 初始化两个固定用户
INSERT INTO users (id, username) VALUES
  ('12345', '高猪猪'),
  ('67890', '徐大王')
ON CONFLICT (id) DO NOTHING;
