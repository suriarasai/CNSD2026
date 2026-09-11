// loadtest/k6-script.js — LAB 4 load profile
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const servedBy = new Counter('served_by_instance');

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 }, // warm up
        { duration: '60s', target: 20 }, // steady state
        { duration: '30s', target: 60 }, // spike
        { duration: '30s', target: 0 },  // drain
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE = __ENV.BASE_URL || 'http://gateway:8080';

export default function () {
  const list = http.get(`${BASE}/api/tasks`, { tags: { endpoint: 'tasks' } });
  check(list, { 'tasks 200': (r) => r.status === 200 });

  const who = http.get(`${BASE}/api/whoami`, { tags: { endpoint: 'whoami' } });
  if (who.status === 200) {
    try {
      servedBy.add(1, { instance: who.json('instance') });
    } catch (_) {
      // response was not JSON — ignore for the lab
    }
  }
  sleep(0.4);
}
