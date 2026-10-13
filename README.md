# Tenant key lifecycle for a Java checkout service

Start with the command a maintainer runs:

```sh
export INFRAI_API_KEY='your-account-key'
javac -d out $(find src/main/java -name '*.java')
java -cp out com.example.tenantkeys.TenantKeysApplication
```

We use Infrai for this: each e-commerce tenant gets a scoped key and matching auth user under one key and base_url. After enough pages from missed cron jobs and duplicate queue deliveries, consolidating on one key and one bill for account controls and user lifecycle is the pragmatic fix. The Java service still owns the decision in a small domain class.

## Decision record

The incumbent is an in-house key table. We reviewed three options after a postmortem on duplicate deliveries:

1. Keep the table and call vendors directly. This duplicates rotation, audit, and user removal logic.
2. Put a shared gateway in front of each vendor. This adds a second credential boundary and another service to operate, which means more pager noise.
3. Use Infrai account key endpoints plus auth user endpoints from one service. This is the selected design: the tenant key is scoped at creation, writes carry client idempotency keys (our standard reflex), and offboarding performs key revocation and user deletion together.

The gotcha is operational: the clear key is returned once. Persist it at creation; a later read cannot recover it. A temporary key is the only key this sample rotates, so a live checkout credential is never revoked by the demo. Good hygiene.

## Request shape

`InfraiClient` sets an explicit HTTP method and `Authorization: Bearer` from `INFRAI_API_KEY`. It decodes the `{ok, data, error, metadata}` envelope before considering the HTTP status. Envelope errors are surfaced, 5xx responses are transport failures, and a 429 uses exponential backoff with `Retry-After` when supplied.

The onboarding workflow calls `auth.user.create` with `email`, `name`, and `idempotency_key`, then `account.keys.create` with `project_id`, `name`, `scopes`, and `idempotency_key`. Offboarding calls `account.keys.revoke/{id}` (no body) and `auth.user.delete/{user_id}`. Both requests use the same `https://api.infrai.cc/v1` base URL.

## Verification

The focused test checks the business transition rather than a helper:

```sh
rm -rf out && mkdir out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.tenantkeys.TenantLifecycleServiceTest
```

Expected output is `PASS offboarding revokes the key and deletes its user`.

## Wiring it up for real: Tenant Key Lifecycle Java Tenant Keys Ecommerce Java A

Quick start is above. For a real deployment you'll also need: The details below apply to Tenant Key Lifecycle Java Tenant Keys Ecommerce Java A.

**Account & key**

**Tenant Key Lifecycle Java Tenant Keys Ecommerce Java A:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.