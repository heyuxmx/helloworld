const { Pool, types } = require('pg');
require('dotenv').config({ path: require('path').join(__dirname, '../../.env') });

// Return BIGINT (INT8) as JavaScript numbers instead of strings,
// so kotlinx.serialization can parse them as Long on Android.
types.setTypeParser(20, parseInt);

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

pool.on('error', (err) => {
  console.error('PostgreSQL pool error:', err);
});

module.exports = pool;
