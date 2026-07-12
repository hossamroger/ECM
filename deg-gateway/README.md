# DEG Gateway

A single **dynamic Spring Boot controller** that consumes every `/deg/*` endpoint
and replays the exact flow of the Oracle Service Bus (OSB)
`ShjSocialDeptServicesPipeline`. It replaces the 22 near-identical OSB
route-nodes with one generic, path-through handler.

- **Java:** 8
- **Spring Boot:** 2.2.8.RELEASE
- **Packaging:** WAR (deployable to WebLogic / Tomcat, also runnable via `java -jar`)

## Flow (per request)

```
Client ──► GET/POST/PUT/DELETE /deg/**        (DegProxyController — one handler for all)
   │
   ├─ 1. VALIDATE USER  (UserValidationService)
   │      POST {deg.validation.url}  headers: dstoken, dscode(SS-004)
   │      if response statusCode == 401  ⇒  stop, return 401 UNAUTHORIZED
   │
   ├─ 2. GET ACCESS TOKEN  (DegTokenService, cached per dstoken)
   │      GET {deg.token-base-url}{deg.token-path}
   │      headers: Content-Type, dstoken, language, uuid, devicetype, s_token
   │      accessToken = response.data.token
   │
   ├─ 2b. [only /deg/aid-request] DB LOOKUP  (EntityTrnIdLookup)
   │      SELECT ENTITY_REQ_ID FROM BPM_PROCESS_TRANSACTIONS WHERE TRANSACTION_SEQ = :requestNo
   │      rewrite query param  requestNo → RequestNo = ENTITY_REQ_ID
   │
   └─ 3. CALL BACKEND  (DegProxyService)
          {method} {deg.backend-base-url}/<sub-path>   (query + body forwarded)
          header: Authorization: Bearer <accessToken>
          return backend status + body verbatim; add Access-Control-Allow-Origin: *
```

Step **2b applies only to `/deg/aid-request`**; the other 21 endpoints go
straight from token to backend. Adding a new `/deg/*` endpoint requires **no
code change** — it is forwarded automatically.

## Components

| Class | Responsibility |
|---|---|
| `web/DegProxyController` | The single `@RequestMapping("/deg/**")` handler; extracts the sub-path and delegates. |
| `service/DegProxyService` | Orchestrates validate → token → (db) → backend; header/response copying. |
| `service/UserValidationService` | Step 1 — validateUser call, 401 short-circuit. |
| `service/DegTokenService` | Step 2 — token retrieval with a short TTL cache. |
| `service/EntityTrnIdLookup` / `JdbcEntityTrnIdLookup` | Step 2b — `requestNo → ENTITY_REQ_ID`. |
| `config/DegProperties` | All externalised settings. |
| `config/RestTemplateConfig` | Shared `RestTemplate` with pass-through error handling. |
| `config/DataSourceConfig` | JNDI datasource (only when `deg.datasource.jndi-name` is set). |
| `web/GlobalExceptionHandler` | 401 body and the OSB-style 500 "Error Call Service OSB" fault. |

## Configuration

See `src/main/resources/application.properties`. Key values (override per environment):

| Property | Meaning |
|---|---|
| `deg.token-base-url` / `deg.token-path` | Token service (step 2). |
| `deg.backend-base-url` | Backend DEG API (step 3). |
| `deg.s-token` | Static credential OSB sent as `s_token`. **Supply via `DEG_S_TOKEN` env var — do not commit.** |
| `deg.validation.enabled` / `deg.validation.url` / `deg.validation.dscode` | User validation (step 1). |
| `deg.token-cache.enabled` / `deg.token-cache.ttl-seconds` | Token caching. |
| `deg.aid-request.*` | aid-request path and query-param names. |
| `deg.datasource.jndi-name` | JNDI datasource for the aid-request lookup (e.g. `jdbc/DigitalSharjah`). Leave blank to boot without a DB. |

> **Security note:** the OSB export had `s_token` hard-coded and called the
> backend over plain HTTP. Here `s_token` is externalised to an env var; rotate
> it and prefer HTTPS backends in production.

## Build

```bash
mvn clean package
# -> target/deg-gateway.war
```

## Run

```bash
# standalone
java -jar target/deg-gateway.war \
  --deg.token-base-url=https://stg-ds.sharjah.ae/socialdeptservices/api \
  --deg.backend-base-url=http://stg-sssd-api.shj.ae/api/deg \
  --deg.validation.url=<validateUser-url> \
  --deg.datasource.jndi-name=jdbc/DigitalSharjah

# or deploy target/deg-gateway.war to WebLogic / Tomcat
```

This is a **self-contained application**: the WAR embeds Tomcat, so `java -jar`
runs it standalone with no external server, and it also deploys to an external
container (WebLogic/Tomcat) via `ServletInitializer`. It boots with the bundled
defaults and needs no other service to start.

Health endpoint for monitoring/readiness (relative to the context path):

```
GET /online/shjsocialdeptservices/actuator/health   ->  {"status":"UP"}
```

## Tests

`mvn test` — the suite uses Spring's `MockRestServiceServer` and standalone
`MockMvc` (no Mockito), covering: the generic flow, the 401 short-circuit, the
aid-request `requestNo → ENTITY_REQ_ID` translation (and the literal `null`
case), backend error pass-through, and dynamic sub-path mapping for
list/nested/POST endpoints.
