// flaky-service/server.js  —  LAB 3: a dependency you can break on demand
const express = require('express');

const app = express();
app.use(express.json());
const port = Number(process.env.PORT || 3002);
const INSTANCE_ID = process.env.HOSTNAME || 'flaky';

// mode: ok | slow | error | down
let chaos = { mode: 'ok', latencyMs: 0, errorRate: 0 };

const log = (msg, extra = {}) =>
  console.log(JSON.stringify({ ts: new Date().toISOString(), level: 'info', msg, ...extra }));

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

app.get('/healthz', (_req, res) => res.json({ status: 'alive', instance: INSTANCE_ID, chaos }));
app.get('/chaos', (_req, res) => res.json(chaos));

app.post('/chaos', (req, res) => {
  const { mode = 'ok', latencyMs = 0, errorRate = 0 } = req.body || {};
  if (!['ok', 'slow', 'error', 'down'].includes(mode)) {
    return res.status(400).json({ message: 'mode must be ok | slow | error | down' });
  }
  chaos = { mode, latencyMs: Number(latencyMs), errorRate: Number(errorRate) };
  log('chaos updated', { chaos });
  res.json(chaos);
});

app.get('/rating/:id', async (req, res) => {
  // 'down' deliberately never answers: this is the failure mode that eats your
  // connection pool if the CALLER has no timeout. Do not "fix" it here.
  if (chaos.mode === 'down') return;

  if (chaos.mode === 'slow') await sleep(chaos.latencyMs || 3000);

  if (chaos.mode === 'error' || Math.random() < chaos.errorRate) {
    return res.status(503).json({ message: 'rating engine unavailable' });
  }

  const id = String(req.params.id);
  const hash = [...id].reduce((acc, char) => acc + char.charCodeAt(0), 0);
  res.json({ taskId: id, rating: (hash % 5) + 1, source: 'rating-service' });
});

const server = app.listen(port, '0.0.0.0', () => log(`flaky-service listening on :${port}`));

['SIGTERM', 'SIGINT'].forEach((signal) =>
  process.on(signal, () => {
    log('shutting down', { signal });
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(1), 5000).unref();
  })
);
