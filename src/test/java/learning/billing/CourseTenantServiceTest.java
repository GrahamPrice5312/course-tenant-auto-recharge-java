package learning.billing;

public final class CourseTenantServiceTest {
    public static void main(String[] args) {
        check(CourseTenantService.keepsLessonsRunning(20.0, 20.0), "equal balance should recharge");
        check(CourseTenantService.keepsLessonsRunning(8.0, 20.0), "low balance should recharge");
        check(!CourseTenantService.keepsLessonsRunning(20.01, 20.0), "balance above threshold should wait");
        System.out.println("PASS: balance 20.0 at trigger 20.0 chooses recharge");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
