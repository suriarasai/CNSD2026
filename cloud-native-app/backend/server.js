// backend/server.js  —  CNSD2026 cloud-native lab (final, Lab 5 state)
require('dotenv').config();

const crypto = require('crypto');
const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const session = require('express-session');
const { createClient } = require('redis');
const { RedisStore } = require('connect-redis');

const logger = require('./logger');

const app = express();
const port = Number(process.env.PORT || 3001);

// The container's hostname IS the instance identity. Never persist anything
// that is keyed only to this value on local disk - the replica is disposable.
const INSTANCE_ID = process.env.HOSTNAME || crypto.randomUUID().slice(0, 8);

app.set('trust proxy', 1);
app.use(cors({ origin: true, credentials: true }));
app.use(express.json());

// ---------------------------------------------------------------------------
// LAB 2 - External state: Valkey holds sessions + cache, not local memory
// ---------------------------------------------------------------------------
const valkey = createClient({
  url: process.env.VALKEY_URL || 'redis://valkey:6379',
  socket: { reconnectStrategy: (retries) => Math.min(retries * 200, 3000) },
});
valkey.on('error', (err) => logger.error('Valkey client error', { error: err.message }));
valkey.on('ready', () => logger.info('Valkey connected', { url: process.env.VALKEY_URL }));

// ---------------------------------------------------------------------------
// LAB 3 - Resilience: retry + timeout + circuit breaker around a flaky dep
// ---------------------------------------------------------------------------
const { getRating, breaker } = require('./resilience')(valkey, logger);

// ---------------------------------------------------------------------------
// Mongo: bounded connection timeout so a dead DB fails fast instead of hanging
// ---------------------------------------------------------------------------
mongoose
  .connect(process.env.MONGO_URI, {
    serverSelectionTimeoutMS: Number(process.env.MONGO_TIMEOUT_MS || 3000),
    socketTimeoutMS: 5000,
  })
  .then(() => logger.info('MongoDB connected successfully'))
  .catch((err) => logger.error('MongoDB connection error', { error: err.message }));

const taskSchema = new mongoose.Schema(
  { title: { type: String, required: true }, completed: { type: Boolean, default: false } },
  { timestamps: true }
);
const Task = mongoose.model('Task', taskSchema);

const CACHE_KEY = 'tasks:all';
const CACHE_TTL_SECONDS = Number(process.env.CACHE_TTL_SECONDS || 10);
const invalidate = () => valkey.del(CACHE_KEY).catch(() => {});

// ---------------------------------------------------------------------------
// Health endpoints. Liveness != readiness. Keep them cheap and unauthenticated.
// ---------------------------------------------------------------------------
let shuttingDown = false;

app.get('/healthz', (_req, res) => {
  res.status(200).json({ status: 'alive', instance: INSTANCE_ID });
});

app.get('/readyz', async (_req, res) => {
  if (shuttingDown) {
    return res.status(503).json({ status: 'draining', instance: INSTANCE_ID });
  }
  const checks = { mongo: mongoose.connection.readyState === 1, valkey: false };
  try {
    await valkey.ping();
    checks.valkey = true;
  } catch (_) {
    /* stays false */
  }
  const ready = checks.mongo && checks.valkey;
  res.status(ready ? 200 : 503).json({
    status: ready ? 'ready' : 'not-ready',
    checks,
    instance: INSTANCE_ID,
  });
});

// ---------------------------------------------------------------------------
// Sessions live in Valkey. This is what makes LAB 4 (--scale) work at all.
// ---------------------------------------------------------------------------
app.use(
  session({
    store: new RedisStore({ client: valkey, prefix: 'sess:' }),
    secret: process.env.SESSION_SECRET || 'cnsd-lab-secret-change-me',
    resave: false,
    saveUninitialized: true,
    name: 'cnsd.sid',
    cookie: { httpOnly: true, sameSite: 'lax', maxAge: 1000 * 60 * 30 },
  })
);

// Which replica answered, and does my session survive across replicas?
app.get('/whoami', async (req, res) => {
  req.session.views = (req.session.views || 0) + 1;
  await valkey.incr(`hits:${INSTANCE_ID}`).catch(() => {});
  res.json({
    instance: INSTANCE_ID,
    sessionId: req.sessionID,
    views: req.session.views,
    servedAt: new Date().toISOString(),
  });
});

// Aggregate request counts per replica - proof of load distribution in LAB 4.
app.get('/fleet', async (_req, res) => {
  try {
    const keys = await valkey.keys('hits:*'); // SCAN in production; KEYS is fine for a lab
    const fleet = {};
    for (const key of keys) fleet[key.slice(5)] = Number(await valkey.get(key));
    res.json({ replicas: Object.keys(fleet).length, hits: fleet });
  } catch (error) {
    res.status(503).json({ message: 'fleet view unavailable', error: error.message });
  }
});

app.post('/fleet/reset', async (_req, res) => {
  const keys = await valkey.keys('hits:*').catch(() => []);
  if (keys.length) await valkey.del(keys).catch(() => {});
  res.json({ reset: keys.length });
});

// Circuit breaker telemetry for LAB 3.
app.get('/resilience/stats', (_req, res) => {
  const s = breaker.stats;
  res.json({
    instance: INSTANCE_ID,
    state: breaker.opened ? 'OPEN' : breaker.halfOpen ? 'HALF_OPEN' : 'CLOSED',
    successes: s.successes,
    failures: s.failures,
    timeouts: s.timeouts,
    rejects: s.rejects,
    fallbacks: s.fallbacks,
  });
});

// ---------------------------------------------------------------------------
// Task API - cache-aside read path, write-through invalidation
// ---------------------------------------------------------------------------
app.get('/tasks', async (req, res) => {
  const bypass = req.query.fresh === '1';
  try {
    if (!bypass) {
      const cached = await valkey.get(CACHE_KEY).catch(() => null);
      if (cached) {
        res.set('X-Cache', 'HIT').set('X-Served-By', INSTANCE_ID);
        return res.send(JSON.parse(cached));
      }
    }

    const tasks = await Task.find().lean();
    const enriched = await Promise.all(
      tasks.map(async (task) => {
        const rating = await getRating(String(task._id));
        return { ...task, rating: rating.rating, ratingSource: rating.source };
      })
    );

    if (!bypass) {
      await valkey
        .set(CACHE_KEY, JSON.stringify(enriched), { EX: CACHE_TTL_SECONDS })
        .catch(() => {});
    }
    res.set('X-Cache', bypass ? 'BYPASS' : 'MISS').set('X-Served-By', INSTANCE_ID);
    res.send(enriched);
  } catch (error) {
    logger.error('Error fetching tasks', { error: error.message });
    res.status(500).send({ message: 'Error fetching tasks', error: error.message });
  }
});

app.post('/tasks', async (req, res) => {
  try {
    const task = new Task(req.body);
    await task.save();
    await invalidate();
    logger.info('Task created successfully', { taskId: task._id });
    res.status(201).send(task);
  } catch (error) {
    logger.error('Error creating task', { error: error.message });
    res.status(400).send({ message: 'Error creating task', error: error.message });
  }
});

app.put('/tasks/:id', async (req, res) => {
  try {
    const task = await Task.findByIdAndUpdate(req.params.id, req.body, {
      new: true,
      runValidators: true,
    });
    if (!task) {
      logger.warn('Update failed: Task not found', { taskId: req.params.id });
      return res.status(404).send({ message: 'Task not found' });
    }
    await invalidate();
    logger.info('Task updated successfully', { taskId: task._id });
    res.send(task);
  } catch (error) {
    logger.error('Error updating task', { error: error.message, taskId: req.params.id });
    res.status(400).send({ message: 'Error updating task', error: error.message });
  }
});

app.delete('/tasks/:id', async (req, res) => {
  try {
    const task = await Task.findByIdAndDelete(req.params.id);
    if (!task) {
      logger.warn('Delete failed: Task not found', { taskId: req.params.id });
      return res.status(404).send({ message: 'Task not found' });
    }
    await invalidate();
    logger.info('Task deleted successfully', { taskId: req.params.id });
    res.send({ message: 'Task deleted successfully' });
  } catch (error) {
    logger.error('Error deleting task', { error: error.message, taskId: req.params.id });
    res.status(500).send({ message: 'Error deleting task', error: error.message });
  }
});

// ---------------------------------------------------------------------------
// Startup + graceful shutdown (SIGTERM is how the orchestrator asks you to go)
// ---------------------------------------------------------------------------
let server;

(async () => {
  try {
    await valkey.connect();
  } catch (error) {
    logger.error('Valkey initial connect failed, continuing degraded', { error: error.message });
  }
  server = app.listen(port, '0.0.0.0', () =>
    logger.info(`Backend listening on :${port}`, { instance: INSTANCE_ID })
  );
})();

function shutdown(signal) {
  if (shuttingDown) return;
  shuttingDown = true;
  logger.info('Shutdown signal received, draining', { signal, instance: INSTANCE_ID });

  const hardExit = setTimeout(() => {
    logger.error('Graceful shutdown timed out, forcing exit');
    process.exit(1);
  }, 10000);
  hardExit.unref();

  server?.close(async () => {
    await mongoose.connection.close().catch(() => {});
    await valkey.quit().catch(() => {});
    logger.info('Drained cleanly, exiting', { instance: INSTANCE_ID });
    clearTimeout(hardExit);
    process.exit(0);
  });
}

['SIGTERM', 'SIGINT'].forEach((signal) => process.on(signal, () => shutdown(signal)));
