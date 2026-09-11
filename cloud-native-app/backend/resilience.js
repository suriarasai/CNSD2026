// backend/resilience.js  —  LAB 3: timeout + retry + circuit breaker + fallback
const CircuitBreaker = require('opossum');

const RATING_URL = process.env.RATING_URL || 'http://flaky:3002';
const TIMEOUT_MS = Number(process.env.RATING_TIMEOUT_MS || 400);
const RETRIES = Number(process.env.RATING_RETRIES || 2);

module.exports = function buildRatingClient(valkey, logger) {
  // 1. TIMEOUT - a call with no deadline is a resource leak waiting to happen.
  async function callOnce(taskId) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
    try {
      const response = await fetch(`${RATING_URL}/rating/${taskId}`, {
        signal: controller.signal,
      });
      if (!response.ok) throw new Error(`upstream responded ${response.status}`);
      return await response.json();
    } finally {
      clearTimeout(timer);
    }
  }

  // 2. RETRY - bounded, with exponential backoff AND jitter to avoid a
  //    synchronised retry storm from every replica at once.
  async function callWithRetry(taskId) {
    let lastError;
    for (let attempt = 0; attempt <= RETRIES; attempt += 1) {
      try {
        return await callOnce(taskId);
      } catch (error) {
        lastError = error;
        if (attempt === RETRIES) break;
        const ceiling = Math.min(150 * 2 ** attempt, 800);
        const wait = ceiling / 2 + Math.random() * (ceiling / 2);
        await new Promise((resolve) => setTimeout(resolve, wait));
      }
    }
    throw lastError;
  }

  // 3. CIRCUIT BREAKER - stop hammering a dependency that is already down.
  const breaker = new CircuitBreaker(callWithRetry, {
    name: 'rating-service',
    timeout: TIMEOUT_MS * (RETRIES + 1) + 500, // outer guard for the whole retry budget
    errorThresholdPercentage: 50,
    volumeThreshold: 5,
    rollingCountTimeout: 10000,
    resetTimeout: 10000, // after 10s, allow one trial call (HALF_OPEN)
  });

  // 4. FALLBACK - degrade, do not fail. Stale data beats a 500.
  breaker.fallback(async (taskId) => {
    try {
      const cached = await valkey.get(`rating:last:${taskId}`);
      if (cached) return { ...JSON.parse(cached), source: 'stale-cache', degraded: true };
    } catch (_) {
      /* Valkey unavailable too - fall through to the static default */
    }
    return { taskId, rating: null, source: 'default', degraded: true };
  });

  breaker.on('open', () => logger.warn('Circuit OPEN', { dependency: 'rating-service' }));
  breaker.on('halfOpen', () => logger.info('Circuit HALF_OPEN', { dependency: 'rating-service' }));
  breaker.on('close', () => logger.info('Circuit CLOSED', { dependency: 'rating-service' }));

  async function getRating(taskId) {
    const result = await breaker.fire(taskId);
    if (!result.degraded) {
      valkey
        .set(`rating:last:${taskId}`, JSON.stringify(result), { EX: 300 })
        .catch(() => {});
    }
    return result;
  }

  return { getRating, breaker };
};
