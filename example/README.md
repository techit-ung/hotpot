# HotPot Example

A runnable example showing how to use [HotPot](../) as a library to build a mock payment webhook server.

## What it demonstrates

- Token authentication (`TokenAuthentication`)
- HMAC-SHA256 signature validation (`HMACSignatureValidation`)
- Custom response bodies using `buildJsonObject`
- Forwarding received events to another service via `notify`

## Endpoints

| Method | Path | Auth | Signature |
|---|---|---|---|
| `POST` | `/payments` | Bearer token | HMAC-SHA256 |
| `POST` | `/payments/refunds` | Bearer token | — |
| `GET` | `/hotpot/requests` | — | — |
| `GET` | `/hotpot/requests/{id}` | — | — |
| `GET` | `/hotpot/requests/{id}/response` | — | — |
| `DELETE` | `/hotpot/requests` | — | — |

## Running

```bash
cd example
./gradlew run
```

The server starts on `http://localhost:8080`.

The example uses the local checkout through Gradle composite build substitution (`includeBuild("..")`), while keeping the published dependency coordinates as `com.coloncmd:hotpot`. To try a specific published release outside this repo, depend on `com.coloncmd:hotpot:<version>`.

## Testing with curl

### POST /payments

Requires a Bearer token and a valid HMAC-SHA256 signature computed from the request body.

```bash
BODY='{"event":"payment.completed","amount":9900,"currency":"USD"}'
SIG="sha256=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "my-hmac-secret" | awk '{print $2}')"

curl -s http://localhost:8080/payments \
  -H "Authorization: Bearer secret-api-token" \
  -H "X-Hub-Signature-256: $SIG" \
  -H "Content-Type: application/json" \
  -d "$BODY"
```

Expected response (`202 Accepted`):

```json
{
  "message": "Payment event received",
  "id": "<request-id>"
}
```

### POST /payments/refunds

Requires a Bearer token only — no signature needed.

```bash
curl -s http://localhost:8080/payments/refunds \
  -H "Authorization: Bearer secret-api-token" \
  -H "Content-Type: application/json" \
  -d '{"event":"refund.issued","amount":2500,"orderId":"ord_123"}'
```

Expected response (`200 OK`):

```json
{
  "message": "Refund event received",
  "id": "<request-id>"
}
```

### Query stored requests

List all captured requests:

```bash
curl -s http://localhost:8080/hotpot/requests | jq
```

Filter by path and method:

```bash
curl -s "http://localhost:8080/hotpot/requests?path=/payments&method=POST&limit=10" | jq
```

Fetch a specific request by ID:

```bash
curl -s http://localhost:8080/hotpot/requests/<id> | jq
```

Fetch the response paired with a request:

```bash
curl -s http://localhost:8080/hotpot/requests/<id>/response | jq
```

Clear all stored data:

```bash
curl -s -X DELETE http://localhost:8080/hotpot/requests
```

### Verifying auth enforcement

Missing token → `401`:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{}'
```

Valid token but bad signature → `401`:

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/payments \
  -H "Authorization: Bearer secret-api-token" \
  -H "X-Hub-Signature-256: sha256=badhash" \
  -H "Content-Type: application/json" \
  -d '{}'
```
