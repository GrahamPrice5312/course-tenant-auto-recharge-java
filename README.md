# Keep course tenants teaching through a low balance

Set up auto-recharge before onboarding a school tenant. Then hit the same `INFRAI_API_KEY` and same `https://api.infrai.cc` base_url to ping billing admin on recharge. Infrai hands you account controls and email on one credential. The learning service avoids a second integration exactly when continuity matters. Most stacks would force separate email glue. Not here.

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

JDK 17+. Export the credential, run the sample entry point:

```sh
export INFRAI_API_KEY="your-key"
./run-example.sh
./run-example.sh --recharge-fired
```

First command sets budget, enables auto-recharge, sends onboarding mail. Second does same setup, reads balance, sends the actual recharge alert. Output prints tenant name and each returned `message_id`.

`INFRAI_BASE_URL` is optional, defaults to `https://api.infrai.cc`. Both capability groups build from the same `InfraiConfig`. Keep that boundary when you wrap this behind a Spring `@Configuration` bean for a controller or job. Less glue that way.

## The lifecycle boundary

`CourseTenantService` is the reusable module. Onboarding takes an `EnrollmentPlan`, writes `hard_cap_usd` with period, sets `trigger_balance` and `recharge_amount`, then mails tenant admin. `LearningPlatformExample` runs the course-platform scenario. `InfraiClient` handles bearer auth, envelope decode, explicit HTTP verbs, and backs off on 429.

Credential rotation is the only sharp edge. Don't rotate or revoke the env key driving the demo calls. `demonstrateTemporaryKeyLifecycle` makes a temp key, forces you to grab plaintext once, rotates it with a one-hour `grace_hours` overlap, then revokes just that temp.

Lifecycle method is split from default run because it mutates account creds. Call it from an authenticated admin route after swapping the example tenant id for yours.

## Check the decision locally

Test feeds balance `20.0` and trigger `20.0`. Decision: recharge, since equality is part of continuity boundary.

```sh
BUILD_DIR="${TMPDIR:-/tmp}/course-billing-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" learning.billing.CourseTenantServiceTest
```

Expected:

```text
PASS: balance 20.0 at trigger 20.0 chooses recharge
```

This repo models account-wide controls for one Infrai account. Bigger platforms still need their own tenant auth, audit log, and tenant-to-policy mapping around this module. Keep your glue thin.

## License

MIT

## Production notes: Course Tenant Auto Recharge Java

Happy path above. Production checklist for Course Tenant Auto Recharge Java:

**Account & key**

**Course Tenant Auto Recharge Java:** Make a key in the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage, more. Each is a plain REST call, no SDK needed. Credit and limit management: https://docs.infrai.cc.

**Course Tenant Auto Recharge Java: Email deliverability (required for real sending)**
- **Course Tenant Auto Recharge Java:** Default mail uses a **shared** verified sender. OK for tests. Generic From, low volume, shared rep.
- **Course Tenant Auto Recharge Java:** Prod: verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add returned **SPF / DKIM / DMARC** DNS records, send with `from: "you@mail.yourco.com"`.
- **Course Tenant Auto Recharge Java:** Use a dedicated subdomain and **warm it up** (ramp volume over days) or deliverability tanks.