const { Pool } = require('pg');
require('dotenv').config({ path: require('path').join(__dirname, '../../.env') });

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

pool.on('error', (err) => {
  console.error('PostgreSQL pool error:', err);
});

module.exports = pool;
