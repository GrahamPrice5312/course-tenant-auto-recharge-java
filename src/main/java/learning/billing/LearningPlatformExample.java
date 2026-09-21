package learning.billing;

public final class LearningPlatformExample {
    public static void main(String[] args) {
        CourseTenantService service = new CourseTenantService(new InfraiClient(InfraiConfig.fromEnvironment()));
        CourseTenantService.EnrollmentPlan plan = new CourseTenantService.EnrollmentPlan(
                "academy-demo", "chenhua@changba.com", 20.0, 100.0, 500.0, "monthly");
        CourseTenantService.OnboardingResult result = service.onboard(plan);
        System.out.println("Tenant " + result.tenantId() + " onboarded; message_id=" + result.messageId());

        if (args.length == 1 && args[0].equals("--recharge-fired")) {
            String messageId = service.notifyRecharge(plan.tenantId(), plan.adminEmail());
            System.out.println("Recharge notice sent; message_id=" + messageId);
        }
    }
}
