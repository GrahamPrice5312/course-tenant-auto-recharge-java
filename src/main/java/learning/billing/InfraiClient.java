package learning.billing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient {
    private final InfraiConfig config;
    private final HttpClient http;

    public InfraiClient(InfraiConfig config) {
        this(config, HttpClient.newHttpClient());
    }

    InfraiClient(InfraiConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public Map<String, Object> get(String path) { return request("GET", path, null); }
    public Map<String, Object> post(String path, Map<String, Object> body) { return request("POST", path, body); }
    public Map<String, Object> put(String path, Map<String, Object> body) { return request("PUT", path, body); }
    public Map<String, Object> delete(String path) { return request("DELETE", path, null); }

    private Map<String, Object> request(String method, String path, Map<String, Object> body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(Json.stringify(body));
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + path))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, publisher)
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429 && attempt < 3) {
                    pause(retryDelay(response, attempt));
                    continue;
                }
                return envelope(response);
            } catch (IOException ex) {
                throw new IllegalStateException("Infrai transport failed", ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", ex);
            }
        }
        throw new IllegalStateException("Retry attempts exhausted");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> envelope(HttpResponse<String> response) {
        Object decoded = Json.parse(response.body());
        if (!(decoded instanceof Map<?, ?> raw)) throw new IllegalStateException("Expected an Infrai envelope");
        Map<String, Object> env = (Map<String, Object>) raw;
        if (!Boolean.TRUE.equals(env.get("ok"))) {
            Map<String, Object> error = env.get("error") instanceof Map<?, ?> value
                    ? (Map<String, Object>) value : Map.of("message", "Request rejected");
            throw new InfraiException(String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED")), response.statusCode(), error);
        }
        if (response.statusCode() >= 500) throw new IllegalStateException("Infrai transport status " + response.statusCode());
        return env.get("data") instanceof Map<?, ?> data ? (Map<String, Object>) data : Map.of();
    }

    private static Duration retryDelay(HttpResponse<?> response, int attempt) {
        String header = response.headers().firstValue("Retry-After").orElse("");
        try { return Duration.ofSeconds(Math.max(1, Long.parseLong(header))); }
        catch (NumberFormatException ignored) { return Duration.ofMillis(250L * (1L << attempt)); }
    }

    private static void pause(Duration duration) throws InterruptedException { Thread.sleep(duration.toMillis()); }
}
