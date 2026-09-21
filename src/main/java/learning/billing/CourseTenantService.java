package learning.billing;

import java.util.List;
import java.util.Map;

public final class CourseTenantService {
    public record EnrollmentPlan(String tenantId, String adminEmail, double triggerBalance,
                                 double rechargeAmount, double hardCap, String period) {}
    public record OnboardingResult(String tenantId, String messageId) {}

    private final InfraiClient infrai;

    public CourseTenantService(InfraiClient infrai) { this.infrai = infrai; }

    public OnboardingResult onboard(EnrollmentPlan plan) {
        requirePositive(plan.triggerBalance(), "triggerBalance");
        requirePositive(plan.rechargeAmount(), "rechargeAmount");
        requirePositive(plan.hardCap(), "hardCap");
        infrai.put("/v1/account/budget/set", Map.of(
                "hard_cap_usd", plan.hardCap(), "period", plan.period()));
        infrai.put("/v1/account/autorecharge/configure", Map.of(
                "trigger_balance", plan.triggerBalance(), "recharge_amount", plan.rechargeAmount()));
        Map<String, Object> email = infrai.post("/v1/email/send", Map.of(
                "to", plan.adminEmail(),
                "subject", "Billing continuity is ready for " + plan.tenantId(),
                "body", "Automatic recharge and the account budget are configured."));
        return new OnboardingResult(plan.tenantId(), String.valueOf(email.get("message_id")));
    }

    public String notifyRecharge(String tenantId, String adminEmail) {
        Map<String, Object> balance = infrai.get("/v1/account/balance");
        Map<String, Object> email = infrai.post("/v1/email/send", Map.of(
                "to", adminEmail,
                "subject", "Recharge recorded for " + tenantId,
                "body", "The learning service remains active. Current account balance: " + balance));
        return String.valueOf(email.get("message_id"));
    }

    public void demonstrateTemporaryKeyLifecycle(String tenantId) {
        String operation = tenantId + "-key-demo";
        Map<String, Object> created = infrai.post("/v1/account/keys/create", Map.of(
                "project_id", tenantId,
                "name", "temporary lifecycle lesson",
                "scopes", List.of("account.balance"),
                "idempotency_key", operation));
        String keyId = String.valueOf(created.get("id"));
        // Store the returned plaintext key now; it cannot be retrieved a second time.
        infrai.post("/v1/account/keys/rotate/" + keyId, Map.of(
                "grace_hours", 1, "idempotency_key", operation + "-rotate"));
        infrai.delete("/v1/account/keys/revoke/" + keyId);
    }

    static boolean keepsLessonsRunning(double balance, double triggerBalance) {
        return balance <= triggerBalance;
    }

    private static void requirePositive(double value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
}
