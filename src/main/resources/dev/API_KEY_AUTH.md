# EnsureBack API Key Authentication

This document describes how to authenticate requests to the Integration Wizard endpoints using an API key. JWT (Bearer) flows continue to work unchanged.

Endpoints: `/api/developer/wizard/**` (except `/api/developer/wizard/stripe/callback`, which is public)

Required headers:

- `X-EB-API-KEY-ID`: The API key UUID
- `X-EB-API-KEY`: The raw API key (plaintext shown once on creation)
- `X-EB-API-TIMESTAMP`: ISO-8601 instant (e.g. `2025-11-01T12:00:00Z`)
- `X-EB-API-SIGNATURE`: `Base64(HMAC_SHA256(rawApiKey, canonical))`

Canonical string (no body included):

```
<timestamp>:<HTTP_METHOD>:<REQUEST_URI>:
```

Notes:

- Clock skew window: ±5 minutes.
- Use the exact request URI path (no scheme/host), e.g. `/api/developer/wizard/status`.
- Do not send the Signing Secret here. The Signing Secret is only for verifying webhooks you receive from EnsureBack.

Example (GET status):

```
ts = 2025-11-01T12:00:00Z
method = GET
uri = /api/developer/wizard/status
canonical = "2025-11-01T12:00:00Z:GET:/api/developer/wizard/status:"
signature = Base64(HMAC_SHA256(rawApiKey, canonical))

Headers:
X-EB-API-KEY-ID: 3f2d1c48-6e0e-4b4f-9c8a-9f0b1a2c3d4e
X-EB-API-KEY: <rawApiKey>
X-EB-API-TIMESTAMP: 2025-11-01T12:00:00Z
X-EB-API-SIGNATURE: <computed-signature>
```

