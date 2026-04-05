This file provides guidance to the Coding agent when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.coloncmd.hotpot.integration.WebhookEndpointTest"

# Run the server
./gradlew run

# Generate coverage report
./gradlew koverHtmlReport
```

## Architecture

HotPot is a Kotlin/Ktor library for building mock HTTP backends in tests. Users define routes via a DSL, and HotPot records every request/response for later assertion.

### DSL layer (`dsl/`)

- `StartScope` — top-level DSL receiver; collects `RouteGroup`s via `listen { }`
- `ListenScope` — inner DSL receiver; collects `RouteDefinition`s (via `post`/`get`) and `NotifyDefinition`s (via `notify`)
- `RouteDefinition` — sealed class (`Post` | `Get`) holding path, auth, signature, saveRequestResponse flag, and handler
- `RouteGroup` — aggregates a `basePath`, its `routes`, `notifyRoutes`, and the group-level `saveRequestResponse` default

### Server layer (`server/`)

`HotPotServer.configureApplication` wires everything together:
1. Installs Ktor `ContentNegotiation` (JSON)
2. Mounts query API via `QueryRouter`
3. Iterates `StartScope.routeGroups` to mount webhook routes and notify routes
4. Installs the runtime OpenAPI spec via `HotPotOpenApi`

For each inbound route, the server reads the raw body, runs `AuthPlugin` and `SignaturePlugin`, invokes the DSL handler, and optionally persists the `WebhookRequest`/`WebhookResponse` pair to `Storage`.

### Auth and signature (`auth/`, `signature/`, `plugin/`)

- `AuthStrategy` / `SignatureStrategy` — interfaces; implement either to plug in custom schemes
- `TokenAuthentication` — built-in `AuthStrategy` for `Bearer` tokens
- `HMACSignatureValidation` — built-in `SignatureStrategy` for HMAC-SHA256 headers
- `AuthPlugin` / `SignaturePlugin` — Ktor extension functions that run all strategies with AND semantics (all must pass)
- Raw body is read once and stored in `RawBodyKey` (Ktor attribute) so signature validation can access it after routing

### Storage (`storage/`)

- `Storage` — interface with `saveRequest`, `saveResponse`, `findRequests`, `findRequest`, `findResponseFor`, `clear`
- `SqlStorage` — Exposed + H2 implementation; `SqlStorage.inMemory()` is the default
- `InMemoryStorage` — in-memory implementation used in tests
- `StorageContractTest` — abstract Kotest `FunSpec` that both implementations extend to share contract tests

### Notifications (`notification/`)

- `NotifyDefinition` — sealed class (`Proxy` | `Custom`); `Proxy` forwards body to a target URL, `Custom` accepts a handler lambda with full Ktor client access
- `NotificationService` — dispatches a `NotifyDefinition` given an inbound `WebhookRequest`; uses a Ktor CIO HTTP client

### Query API (`routing/QueryRouter`)

Always mounted at `/hotpot/*`. Provides `GET /hotpot/requests`, `GET /hotpot/requests/{id}`, `GET /hotpot/requests/{id}/response`, and `DELETE /hotpot/requests`.

### OpenAPI (`openapi/HotPotOpenApi`)

Generates a runtime OpenAPI spec from the DSL-defined routes and mounts Swagger UI at `/hotpot/swagger`. Uses Ktor's `describe { }` DSL on each registered route.

## Testing patterns

Integration tests use `testApplication { application { HotPotServer.configureApplication(this, scope) } }` — no real server port is bound. `InMemoryStorage` is the preferred storage in tests.

New `Storage` implementations must extend `StorageContractTest` to satisfy the contract.

Tests should follow `arrange/act/assert` pattern for readability.
