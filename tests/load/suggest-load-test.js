import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getTokenCached } from './auth.js';

export const errorRate = new Rate('errors');
export const suggestLatency = new Trend('suggest_latency');

export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<50'],
    errors: ['rate<0.01'],
    http_req_failed: ['rate<0.01'],
  },
};

const API_URL = __ENV.API_URL || 'http://localhost:8000';
const prefixes = [
  'lap',
  'mac',
  'gam',
  'pro',
  'hea',
  'wir',
  'cam',
  'tab',
  'key',
  'sma',
];

function randomPrefix() {
  const base = prefixes[Math.floor(Math.random() * prefixes.length)];
  return base.substring(0, Math.min(3 + Math.floor(Math.random() * 3), base.length));
}

export default function () {
  const q = randomPrefix();
  const url = `${API_URL}/api/search/suggest?q=${encodeURIComponent(q)}`;
  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${getTokenCached()}`,
    },
    tags: { name: 'suggest_request' },
  });

  suggestLatency.add(res.timings.duration);

  let suggestions;
  let validJson = true;
  try {
    suggestions = res.json();
  } catch (e) {
    validJson = false;
  }

  const success = check(res, {
    'suggest status 200': (r) => r.status === 200,
    'suggest returned json': () => validJson,
    'suggest returned array': () => Array.isArray(suggestions),
  });

  errorRate.add(!success);

  if (!success) {
    console.error(`Suggest failure: ${res.status} ${res.body}`);
  }

  sleep(Math.random() * 0.8 + 0.2);
}
