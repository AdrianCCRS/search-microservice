import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { getTokenCached } from './auth.js';

export const errorRate = new Rate('errors');

export const options = {
  vus: 50,
  duration: '5m',
  thresholds: {
    http_req_duration: ['p(95)<100'],
    errors: ['rate<0.01'],
    http_req_failed: ['rate<0.01'],
  },
};

const API_URL = __ENV.API_URL || 'http://localhost:8000';
const queries = [
  'laptop',
  'laptop gamer',
  'computer monitor',
  'office chair',
  'wireless headphones',
  'smartphone',
  'gaming keyboard',
  'tablet',
  '4k monitor',
  'external hard drive',
];

export default function () {
  const query = queries[Math.floor(Math.random() * queries.length)];
  const url = `${API_URL}/api/search?q=${encodeURIComponent(query)}`;
  const res = http.get(url, {
    headers: {
      Authorization: `Bearer ${getTokenCached()}`,
    },
    tags: { name: 'search_request' },
  });

  const success = check(res, {
    'search status 200': (r) => r.status === 200,
    'search response not empty': (r) => r.body && r.body.length > 0,
  });

  errorRate.add(!success);

  if (!success) {
    console.error(`Search failure: ${res.status} ${res.body}`);
  }

  sleep(Math.random() * 1.2 + 0.3);
}
