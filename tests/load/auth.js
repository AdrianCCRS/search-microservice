import http from 'k6/http';
import { check } from 'k6';

const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8081';
const REALM = __ENV.KEYCLOAK_REALM || 'ecommerce';
const CLIENT_ID = __ENV.KEYCLOAK_CLIENT_ID || 'search-client';
const USERNAME = __ENV.KEYCLOAK_USERNAME || 'testuser';
const PASSWORD = __ENV.KEYCLOAK_PASSWORD || 'testpassword';

function formEncode(payload) {
  return Object.entries(payload)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

function fetchAccessToken() {
  const tokenUrl = `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`;
  const payload = formEncode({
    grant_type: 'password',
    client_id: CLIENT_ID,
    username: USERNAME,
    password: PASSWORD,
  });

  const params = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    tags: { name: 'keycloak_token' },
  };

  const res = http.post(tokenUrl, payload, params);
  check(res, {
    'Keycloak token status 200': (r) => r.status === 200,
    'Keycloak returned access_token': (r) => r.json('access_token') !== undefined,
  });

  if (res.status !== 200) {
    throw new Error(`Keycloak no respondió correctamente. HTTP ${res.status}: ${res.body}`);
  }

  const token = res.json('access_token');
  if (!token) {
    throw new Error('Keycloak no devolvió access_token en la respuesta.');
  }

  return token;
}

/** Siempre obtiene un token nuevo (p.ej. scripts que hacen una sola petición). */
export function getToken() {
  return fetchAccessToken();
}

const refreshMs = Number(__ENV.K6_TOKEN_REFRESH_MS || 240000);

let cachedToken = '';
let cachedAtMs = 0;

/**
 * Token con renovación antes del expiry típico del realm (300s).
 * Cada VU de k6 tiene su propio estado de módulo.
 */
export function getTokenCached() {
  const now = Date.now();
  if (!cachedToken || now - cachedAtMs > refreshMs) {
    cachedToken = fetchAccessToken();
    cachedAtMs = now;
  }
  return cachedToken;
}
