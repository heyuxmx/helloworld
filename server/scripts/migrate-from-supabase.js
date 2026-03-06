/**
 * 从 Supabase 迁移数据到本地 PostgreSQL
 * 运行方式: node scripts/migrate-from-supabase.js
 */

require('dotenv').config({ path: require('path').join(__dirname, '../.env') });
const { Pool } = require('pg');

const SUPABASE_URL = 'https://bvgtzgxscnqhugjirgzp.supabase.co';
const SUPABASE_SERVICE_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2Z3R6Z3hzY25xaHVnamlyZ3pwIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1OTUwMDk1NiwiZXhwIjoyMDc1MDc2OTU2fQ.iezSQ7OXSJE_uCKNDZl0Vcmf35wVS-yvSW-uIG9ai0I';

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

async function fetchFromSupabase(table, orderBy = 'created_at.asc') {
  const url = `${SUPABASE_URL}/rest/v1/${table}?select=*&order=${orderBy}&limit=10000`;
  const res = await fetch(url, {
    headers: {
      apikey: SUPABASE_SERVICE_KEY,
      Authorization: `Bearer ${SUPABASE_SERVICE_KEY}`,
    },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Supabase fetch ${table} failed: ${res.status} ${text}`);
  }
  return res.json();
}

async function migrateUsers(users) {
  console.log(`\n迁移 users (${users.length} 条)...`);
  for (const u of users) {
    await pool.query(
      `INSERT INTO users (id, username, avatar_url, fcm_token, created_at)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (id) DO UPDATE SET
         username   = EXCLUDED.username,
         avatar_url = EXCLUDED.avatar_url,
         fcm_token  = EXCLUDED.fcm_token`,
      [u.id, u.username, u.avatar_url, u.fcm_token, u.created_at]
    );
    console.log(`  user: ${u.id} (${u.username}) avatar: ${u.avatar_url || '无'}`);
  }
}

async function migratePosts(posts) {
  console.log(`\n迁移 posts (${posts.length} 条)...`);
  let maxId = 0;
  for (const p of posts) {
    await pool.query(
      `INSERT INTO posts (id, content, user_id, image_urls, video_url, likes, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (id) DO UPDATE SET
         content    = EXCLUDED.content,
         image_urls = EXCLUDED.image_urls,
         video_url  = EXCLUDED.video_url,
         likes      = EXCLUDED.likes`,
      [p.id, p.content, p.user_id, p.image_urls || [], p.video_url, p.likes || 0, p.created_at]
    );
    if (p.id > maxId) maxId = p.id;
    process.stdout.write('.');
  }
  if (maxId > 0) {
    await pool.query(`SELECT setval('posts_id_seq', $1)`, [maxId]);
  }
  console.log(`\n  ${posts.length} 条动态迁移完成，序列重置为 ${maxId}`);
}

async function migrateComments(comments) {
  console.log(`\n迁移 comments (${comments.length} 条)...`);
  let maxId = 0;
  for (const c of comments) {
    await pool.query(
      `INSERT INTO comments (id, post_id, user_id, content, created_at)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (id) DO NOTHING`,
      [c.id, c.post_id, c.user_id, c.content, c.created_at]
    );
    if (c.id > maxId) maxId = c.id;
    process.stdout.write('.');
  }
  if (maxId > 0) {
    await pool.query(`SELECT setval('comments_id_seq', $1)`, [maxId]);
  }
  console.log(`\n  ${comments.length} 条评论迁移完成，序列重置为 ${maxId}`);
}

async function main() {
  console.log('=== Supabase -> 本地 PostgreSQL 数据迁移 ===\n');
  try {
    console.log('正在从 Supabase 拉取数据...');
    const [users, posts, comments] = await Promise.all([
      fetchFromSupabase('users', 'id.asc'),
      fetchFromSupabase('posts', 'created_at.asc'),
      fetchFromSupabase('comments', 'created_at.asc'),
    ]);
    console.log(`拉取完成: ${users.length} 用户, ${posts.length} 动态, ${comments.length} 评论`);

    await migrateUsers(users);
    await migratePosts(posts);
    await migrateComments(comments);

    console.log('\n迁移完成！');
  } catch (err) {
    console.error('\n迁移失败:', err.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

main();
