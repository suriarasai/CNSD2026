const express = require('express');
const { createClient } = require('redis');

const app = express();
const port = process.env.PORT || 3000;
const redisHost = process.env.REDIS_HOST || '127.0.0.1';
const redisPort = process.env.REDIS_PORT || 6379;

const client = createClient({
  url: `redis://${redisHost}:${redisPort}`
});

client.on('error', (err) => console.error('Redis Client Error', err));

app.get('/api/health', async (req, res) => {
  res.json({ status: 'ok', service: 'backend-api', timestamp: new Date() });
});

app.get('/api/hits', async (req, res) => {
  try {
    const hits = await client.incr('page_hits');
    res.json({ hits, backend_pod: process.env.HOSTNAME || 'local' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

async function start() {
  await client.connect();
  app.listen(port, () => {
    console.log(`Backend listening on port ${port}, connected to Redis at ${redisHost}:${redisPort}`);
  });
}

start();