# HotPot

[![Tests](https://github.com/techit-ung/hotpot/actions/workflows/test.yml/badge.svg)](https://github.com/techit-ung/hotpot/actions/workflows/test.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A codified mock backend and webhook sink for testing. Define HTTP endpoints in Kotlin using a clean DSL, record every request and response, and trigger outgoing notifications on demand to simulate async flows.

HotPot also exposes a runtime-generated OpenAPI spec and Swagger UI for the routes it mounts.

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.coloncmd:hotpot:0.1.0")
}
```

## Use cases

- **Automated tests** — spin up HotPot in-process, hit its endpoints, assert on captured payloads via the query API.
- **Manual testing** — run HotPot as a server, point any external system at it, inspect what arrived.
- **Async flow simulation** — test flows where a service receives a request, then later gets a callback from an external system.

## Quick start

```kotlin
fun main() = start(port = 8080) {
    orders()
    notifications()
}
```

Define scopes as extension functions on `StartScope`:

```kotlin
fun StartScope.orders() {
    listen(path = "/orders") {
        post(
            path = "/create",
            auth = setOf(TokenAuthentication("my-token")),
            signature = setOf(HMACSignatureValidation(secret = "my-secret")),
        ) { request ->
            HotPotResponse.ok(buildJsonObject { put("status", "created") })
        }

        // When a test POSTs to /orders/fulfilled, HotPot forwards the body to this URL
        notify(path = "/fulfilled", target = "http://your-service/callbacks/orders")
    }
}
```

## DSL reference

### `start { }`

Top-level entry point. Assembles the Ktor server from your DSL declarations.

```kotlin
start(
    port = 8080,                           // default: 8080
    storage = SqlStorage.inMemory(),       // default: H2 in-memory
    notificationService = NotificationService.create(),
) {
    // your scopes here
}
```

### `listen { }`

Groups routes under a base path. All requests and responses are saved by default.

```kotlin
listen(
    path = "/orders",            // all routes inside are mounted under this prefix
    saveRequestResponse = true,  // default: true; set false to skip persistence
) {
    // post, get, notify declarations
}
```

### `post` / `get`

Register an inbound endpoint handler.

```kotlin
post(
    path = "/create",
    auth = setOf(TokenAuthentication("token")),          // all must pass (AND semantics)
    signature = setOf(HMACSignatureValidation("secret")),
    saveRequestResponse = true,                          // overrides listen-level setting
) { request ->
    HotPotResponse.ok(buildJsonObject { put("id", "abc") })
}

get(path = "/health") { _ ->
    HotPotResponse.ok()
}
```

The handler receives a `WebhookRequest` and must return a `HotPotResponse`.

**`HotPotResponse` factory helpers:**

| Helper | Status |
|---|---|
| `HotPotResponse.ok(body?)` | 200 |
| `HotPotResponse.accepted(body?)` | 202 |
| `HotPotResponse.badRequest(body?)` | 400 |
| `HotPotResponse.unauthorized()` | 401 |
| `HotPotResponse.notFound()` | 404 |

### `notify`

Registers an endpoint that, when POSTed to, sends an outgoing request. HotPot reads the incoming body and either proxies it or runs a custom handler.

**Proxy form** — forwards the incoming body verbatim to a target URL:

```kotlin
notify(
    path = "/fulfilled",
    target = "http://your-service/callbacks/orders",
    headers = mapOf("X-Source" to "hotpot"),   // optional
)
```

**Custom form** — full control, can make multiple outgoing calls:

```kotlin
notify(path = "/multi-step") { request ->
    client.post("http://service-a/hook") { setBody(request.body) }
    client.post("http://service-b/hook") {
        setBody(buildJsonObject { put("event", "step-2") })
    }
}
```

Trigger either form from a test:

```
POST /orders/fulfilled
Content-Type: application/json

{"orderId": "123", "status": "fulfilled"}
```

HotPot responds `202 Accepted` and dispatches the outgoing call(s).

## Authentication

### `TokenAuthentication`

Validates a `Bearer` token in the `Authorization` header.

```kotlin
TokenAuthentication("my-static-token")
```

### Custom `AuthStrategy`

Implement `AuthStrategy` to plug in any authentication scheme:

```kotlin
class ApiKeyAuth(private val key: String) : AuthStrategy {
    override suspend fun validate(call: ApplicationCall): AuthResult {
        return if (call.request.headers["X-Api-Key"] == key) AuthResult.Success
        else AuthResult.Failure("invalid api key")
    }
}

post(path = "/create", auth = setOf(ApiKeyAuth("secret"))) { request ->
    HotPotResponse.ok()
}
```

## Signature validation

### `HMACSignatureValidation`

Verifies an HMAC-SHA256 signature header. Configurable to match any provider's convention.

```kotlin
HMACSignatureValidation(
    secret = "my-secret",
    headerName = "X-Hub-Signature-256",  // default
    algorithm = "HmacSHA256",            // default
    prefix = "sha256=",                  // default
)
```

### Custom `SignatureStrategy`

Implement `SignatureStrategy` for non-standard signature schemes:

```kotlin
class MySignatureStrategy(private val secret: String) : SignatureStrategy {
    override suspend fun validate(call: ApplicationCall, rawBody: ByteArray): SignatureResult {
        val header = call.request.headers["X-My-Signature"] ?: return SignatureResult.Invalid("missing header")
        return if (verify(rawBody, secret, header)) SignatureResult.Valid
        else SignatureResult.Invalid("signature mismatch")
    }
}
```

## Query API

Always mounted at `/hotpot/*`, regardless of your DSL configuration.

| Method | Path | Description |
|---|---|---|
| `GET` | `/hotpot/requests` | List saved requests. Filter with `?path=`, `?method=`, `?limit=` |
| `GET` | `/hotpot/requests/{id}` | Single request by ID |
| `GET` | `/hotpot/requests/{id}/response` | Response paired with a request |
| `DELETE` | `/hotpot/requests` | Clear all saved data |

## API docs

Always mounted under `/hotpot/*`.

| Method | Path | Description |
|---|---|---|
| `GET` | `/hotpot/openapi.json` | Runtime-generated OpenAPI spec for DSL routes and HotPot query APIs |
| `GET` | `/hotpot/swagger` | Swagger UI backed by the runtime OpenAPI spec |

The generated spec includes:

- user-defined `listen {}` routes
- `notify(...)` inbound trigger routes
- the built-in query API under `/hotpot/requests`

The docs endpoints themselves are intentionally excluded from the generated spec.

### Example: assert on a captured request

```kotlin
// trigger the flow under test
service.createOrder(payload)

// inspect what HotPot received
val requests = httpClient.get("http://localhost:8080/hotpot/requests?path=/orders/create")
    .body<List<WebhookRequest>>()

assert(requests.isNotEmpty())
assert(requests.first().body.contains("created"))
```

## Async callback flow example

```
1. Test calls your service  →  service POSTs to HotPot /orders/create
2. HotPot responds with { "status": "created" }
3. Test asserts the request was recorded via GET /hotpot/requests
4. Test POSTs to HotPot /orders/fulfilled with a callback payload
5. HotPot forwards that payload to your service's callback endpoint
6. Test asserts your service handled the callback correctly
```

## `saveRequestResponse`

Controls whether request/response pairs are persisted. Useful for silencing high-frequency or health-check routes.

```kotlin
listen(path = "/events", saveRequestResponse = false) {
    post(path = "/ping") { _ -> HotPotResponse.ok() }                             // not saved
    post(path = "/important", saveRequestResponse = true) { _ -> HotPotResponse.ok() }  // saved
}
```

The per-route value overrides the `listen`-level value.

## Example

See [`example/`](example/) for a standalone project that uses HotPot as a library — a mock payment webhook server with token auth, HMAC signature validation, and event forwarding.

## Running

```bash
./gradlew run
```

## Testing

```bash
./gradlew test
```

## Releasing

See [`RELEASING.md`](RELEASING.md) for the Maven Central release process.

## Tech stack

- **Kotlin 2.3** — sealed classes, data classes, coroutines, extension functions, DSL builders
- **Ktor 3.4** — CIO server, content negotiation, routing, runtime OpenAPI generation, Swagger UI
- **Exposed 1.2 + H2** — in-memory SQL storage
- **Kotest 5** — test framework
- **Gradle KTS + Version Catalog** — `gradle/libs.versions.toml`
