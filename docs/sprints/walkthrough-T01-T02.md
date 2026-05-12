# Walkthrough — T-01 (JWT Kong+Keycloak) + T-02 (Prometheus+Grafana)

**Rama:** `feauture/T-01-JWT-kong+keycloak`
**Sprint:** Sprint 3
**Autor:** Thomas Pérez

---

## Resumen Ejecutivo

Se implementaron y validaron en runtime dos tareas del sprint:

- **T-01:** Autenticación JWT en Kong usando Keycloak como Identity Provider
- **T-02:** Stack de observabilidad completo con Prometheus, Grafana y Elasticsearch Exporter

Ambas tareas fueron desarrolladas de forma incremental: primero una fase estática (sin Docker) y luego la fase de validación en runtime, donde se resolvieron 3 problemas críticos que no podían detectarse sin ejecutar los contenedores.

---

## T-02 — Prometheus + Grafana (ya estaba implementada)

La rama ya incluía la implementación completa de T-02:

### Archivos añadidos
| Archivo | Propósito |
|---|---|
| `deploy/prometheus/prometheus.yml` | Scrape de `search-api:8080/actuator/prometheus` y `elasticsearch-exporter:9114/metrics` |
| `deploy/prometheus/alert_rules.yml` | Alertas P95 latencia >100ms y cache hit rate <70% |
| `deploy/grafana/provisioning/datasources/prometheus.yml` | Datasource Prometheus auto-provisionado |
| `deploy/grafana/provisioning/dashboards/dashboards.yml` | Provider de dashboards |
| `deploy/grafana/dashboards/search-microservice-dashboard.json` | Dashboard con 7 paneles |

### Servicios añadidos al docker-compose.yml
- `elasticsearch-exporter` — Exporta métricas de ES en formato Prometheus
- `prometheus` — Recoge métricas con retención de 15 días
- `grafana` — Visualización con dashboard auto-provisionado

### Validado en runtime
- ✅ Grafana accesible en `http://localhost:3000` (admin/admin123)
- ✅ Prometheus accesible en `http://localhost:9090`
- ✅ Dashboard "Search Microservice" cargado automáticamente
- ✅ Sin conflictos de puertos con JWT stack

---

## T-01 — JWT Kong + Keycloak

### Fase 1: Implementación estática (sin Docker)

Se realizaron los siguientes cambios en la rama antes de la sesión de validación:

#### `deploy/keycloak/realm-export.json`
- Se añadió el cliente `search-client` (Public, `directAccessGrantsEnabled: true`)
- Se conservó `search-service` como `bearerOnly: true`
- **Razón:** Un cliente `bearerOnly` no puede emitir tokens. Se necesita separar la responsabilidad.

#### `deploy/kong/kong.yml`
- Plugin `jwt` configurado a nivel de `service` (no global, para no afectar métricas ni admin)
- Consumer `keycloak-issuer` con clave RSA placeholder

#### `deploy/docker-compose.yml`
- `depends_on: keycloak: condition: service_healthy` añadido al servicio `kong`
- Variables de entorno documentadas para Kong y Keycloak

---

### Fase 2: Validación en runtime — Problemas encontrados y soluciones

#### Problema 1 — Placeholder RSA inválido crasheaba Kong

**Síntoma:** Kong no arrancaba, error en logs:
```
in 'rsa_public_key': invalid key
```

**Causa:** El placeholder `<SE_EXTRAERA_DE_KEYCLOAK_EN_EJECUCION_FASE_2>` no es base64 válido.
Kong valida la sintaxis del PEM al cargar la configuración declarativa, no al primer uso.

**Fix:** Se reemplazó por un PEM dummy sintácticamente válido (líneas base64 reales, aunque la clave no sea funcional). Esto permite que Kong arranque y rechace tokens hasta que se inyecte la clave real.

---

#### Problema 2 — Healthcheck de Keycloak fallaba (`curl: command not found`)

**Síntoma:** `search_keycloak` quedaba en estado `unhealthy` indefinidamente. Kong nunca arrancaba.

```
/bin/sh: line 1: curl: command not found
```

**Causa:** La imagen `quay.io/keycloak/keycloak:23.0` usa `ubi-micro`, una imagen minimalista sin `curl`, `wget` ni herramientas de red.

**Fix:** Se cambió el healthcheck a un comando que usa solo el kernel de Linux:
```yaml
test: ["CMD-SHELL", "cat /proc/net/tcp6 | grep -q '1F90' || cat /proc/net/tcp | grep -q '1F90'"]
```
`0x1F90` es el puerto 8080 en hexadecimal. Este comando verifica si Keycloak está escuchando en ese puerto sin necesitar ninguna herramienta externa.

---

#### Problema 3 — Issuer mismatch (`iss` claim no coincidía con el key configurado)

**Síntoma:** Kong arrancaba correctamente y rechazaba requests sin token (401 ✅), pero también rechazaba requests con token válido:
```json
{"message":"No credentials found for given 'iss'"}
```

**Causa:** El `kong.yml` tenía configurado `key: "http://keycloak:8080/realms/ecommerce"` (hostname interno de Docker). Sin embargo, el token fue obtenido haciendo una petición a `http://localhost:8081`, por lo que Keycloak firmó el token con `iss: "http://localhost:8081/realms/ecommerce"`. Kong buscaba un consumer cuyo `key` coincidiera exactamente con el claim `iss` del token.

**Verificación del iss real:**
```powershell
# Decodificar el payload del JWT (segunda parte del token, separada por '.')
$payload = $token.Split('.')[1]
$padded = $payload + ('=' * ((4 - $payload.Length % 4) % 4))
[System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($padded)) | ConvertFrom-Json | Select iss
# Resultado: http://localhost:8081/realms/ecommerce
```

**Fix:** Se actualizó el `key` en `kong.yml` para que coincida con el `iss` real:
```yaml
- key: "http://localhost:8081/realms/ecommerce"
```

---

### Extracción de la clave RSA real de Keycloak

Una vez resueltos los problemas anteriores, se extrajo la clave RSA pública real usando Python:

```python
import base64
from cryptography import x509
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

# 1. Obtener JWKS de Keycloak
# GET http://localhost:8081/realms/ecommerce/protocol/openid-connect/certs
# Tomar el x5c del key con "use": "sig" y "alg": "RS256"

x5c = "<valor del campo x5c del primer key>"
cert = x509.load_der_x509_certificate(base64.b64decode(x5c))
pem = cert.public_key().public_bytes(Encoding.PEM, PublicFormat.SubjectPublicKeyInfo).decode()
print(pem)
```

La clave obtenida se inyectó directamente en `deploy/kong/kong.yml`.

---

### Validación final

| Test | Resultado | Comando |
|---|---|---|
| Sin token → 401 | ✅ `{"message":"Unauthorized"}` | `curl -i http://localhost:8000/api/search?q=test` |
| Con token válido → Kong acepta | ✅ Kong forward al backend | `curl -H "Authorization: Bearer $TOKEN" ...` |
| Keycloak healthcheck | ✅ `healthy` en ~60s | `docker ps` |
| Kong arranca | ✅ `healthy` | `docker ps` |
| Grafana dashboard | ✅ Auto-provisionado | `http://localhost:3000` |

---

## Commits realizados

| Hash | Mensaje |
|---|---|
| (rama origen) | `feat: JWT Kong+Keycloak Fase 1 + T-02 Prometheus/Grafana` |
| `8283bf0` | `fix(T-01): replace invalid RSA placeholder with valid dummy PEM and fix Keycloak healthcheck` |
| `c0bf435` | `fix(T-01): inject real Keycloak RSA public key and correct iss claim - Phase 2 complete` |

---

## Advertencia importante para el equipo

> **La clave RSA en `kong.yml` es efímera.** Keycloak usa `KC_DB=dev-mem` (base de datos en memoria), lo que significa que genera una nueva clave RSA cada vez que el contenedor se recrea (`docker compose down` + `docker compose up`).

**Si haces `docker compose down` y vuelves a levantar**, deberás:
1. Esperar que Keycloak esté `healthy`
2. Obtener el nuevo JWKS: `GET http://localhost:8081/realms/ecommerce/protocol/openid-connect/certs`
3. Extraer el `x5c` del key con `"use": "sig"`
4. Convertirlo a PEM con el script Python de arriba
5. Actualizar `rsa_public_key` en `deploy/kong/kong.yml`
6. `docker compose -f deploy/docker-compose.yml restart kong`

**Si solo haces `docker compose up -d` (sin `down`)**, los contenedores existentes se reusan y la clave sigue siendo válida. ✅
