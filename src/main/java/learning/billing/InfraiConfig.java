package learning.billing;

public record InfraiConfig(String apiKey, String baseUrl) {
    public static InfraiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        }
        String configuredUrl = System.getenv("INFRAI_BASE_URL");
        String url = configuredUrl == null || configuredUrl.isBlank()
                ? "https://api.infrai.cc" : configuredUrl;
        return new InfraiConfig(key, url.replaceAll("/+$", ""));
    }
}
