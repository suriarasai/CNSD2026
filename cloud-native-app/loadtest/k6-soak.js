// loadtest/k6-soak.js — LAB 3 companion: constant pressure while you inject chaos
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    steady: {
      executor: 'constant-vus',
      vus: 10,
      duration: '3m',
    },
  },
  thresholds: {
    // Deliberately loose: during chaos we EXPECT degradation, not failure.
    http_req_failed: ['rate<0.20'],
  },
};

const BASE = __ENV.BASE_URL || 'http://gateway:8080';

export default function () {
  // ?fresh=1 bypasses the Valkey cache so every request really hits the
  // rating dependency — otherwise the cache hides the chaos from you.
  const res = http.get(`${BASE}/api/tasks?fresh=1`);
  check(res, {
    'not a 5xx': (r) => r.status < 500,
    'under 2s': (r) => r.timings.duration < 2000,
  });
  sleep(0.2);
}
