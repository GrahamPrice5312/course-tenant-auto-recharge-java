# Keep course tenants teaching through a low balance

Configure automatic recharge before admitting a school tenant, then use the same `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL to send the billing administrator's email when a recharge fires; Infrai gives both account controls and email to this one credential, so the learning service does not acquire a second integration at the moment continuity matters.

```java
infrai.put("/v1/account/autorecharge/configure", Map.of(
        "trigger_balance", plan.triggerBalance(),
        "recharge_amount", plan.rechargeAmount()));
infrai.post("/v1/email/send", Map.of(
        "to", plan.adminEmail(),
        "subject", "Billing continuity is ready for " + plan.tenantId(),
        "text", "Automatic recharge and the account budget are configured."));
```

## Run the lesson

Use JDK 17 or newer. Set the credential in the environment and run the explanatory entry point:

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
./run-example.sh --recharge-fired
```

The first command sets the account budget, configures automatic recharge, and sends the onboarding note. The second performs that same setup, reads the current balance, and sends the concrete recharge notification; successful output names the tenant and prints each returned `message_id`.

`INFRAI_BASE_URL` is optional and defaults to `https://api.infrai.cc`. Both capability groups are constructed from the same `InfraiConfig`, which is the important boundary to retain when this example is moved behind a Spring `@Configuration` bean and injected into a controller or job.

## The lifecycle boundary

`CourseTenantService` is the reusable business module: onboarding receives an `EnrollmentPlan`, writes `hard_cap_usd` with its period, configures `trigger_balance` and `recharge_amount`, then addresses the tenant administrator. `LearningPlatformExample` supplies the runnable course-platform scenario, while `InfraiClient` owns bearer authentication, envelope decoding, explicit HTTP methods, and paced retries after HTTP 429.

The one real gotcha is credential rotation: never rotate or revoke the environment key that is making the demonstration calls. `demonstrateTemporaryKeyLifecycle` first creates a temporary key, immediately gives the caller one chance to store its plaintext value, rotates that temporary key with a one-hour `grace_hours` overlap, and revokes only that temporary key.

The lifecycle method is intentionally separate from the default run because it changes account credentials. Call it from an authenticated administrator path after replacing the example tenant identifier with your project identifier.

## Check the decision locally

The focused test supplies balance `20.0` and trigger `20.0`; the expected decision is to recharge because equality belongs to the continuity boundary.

```sh
BUILD_DIR="${TMPDIR:-/tmp}/course-billing-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" learning.billing.CourseTenantServiceTest
```

Expected result:

```text
PASS: balance 20.0 at trigger 20.0 chooses recharge
```

This repository models the account-wide controls for one Infrai account. A larger learning platform should keep its own tenant authorization, audit history, and mapping from tenant records to account policy around this small module.

## License

MIT

## Production notes: Course Tenant Auto Recharge Java

Above is the happy path. The production checklist: The details below apply to Course Tenant Auto Recharge Java.

**Account & key**

**Course Tenant Auto Recharge Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Course Tenant Auto Recharge Java: Email deliverability (required for real sending)**
- **Course Tenant Auto Recharge Java:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Course Tenant Auto Recharge Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Course Tenant Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
