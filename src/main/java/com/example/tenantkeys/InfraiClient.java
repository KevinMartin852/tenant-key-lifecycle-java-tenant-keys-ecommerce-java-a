package com.example.tenantkeys;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String apiKey;

    public InfraiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public String request(String method, String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");
            if ("GET".equals(method) || "DELETE".equals(method)) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!envelope.contains("\"ok\":true")) throw new IOException("Infrai rejected request: " + envelope);
            if (response.statusCode() == 429 && attempt < 3) {
                long wait = 250L * (1L << attempt);
                String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                if (retryAfter != null) try { wait = Long.parseLong(retryAfter) * 1000L; } catch (NumberFormatException ignored) { }
                Thread.sleep(wait);
                attempt++;
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport failure: HTTP " + response.statusCode());
            return envelope;
        }
    }

    public String createUser(String email, String name, String idempotencyKey) throws Exception {
        return request("POST", "/auth/user/create", "{\"email\":\"" + email + "\",\"name\":\"" + name + "\",\"idempotency_key\":\"" + idempotencyKey + "\"}");
    }

    public String deleteUser(String userId) throws Exception { return request("DELETE", "/auth/user/delete/" + userId, null); }

    public String createKey(String projectId, String name, String scopes, String idempotencyKey) throws Exception {
        return request("POST", "/account/keys/create", "{\"project_id\":\"" + projectId + "\",\"name\":\"" + name + "\",\"scopes\":[\"" + scopes + "\"],\"idempotency_key\":\"" + idempotencyKey + "\"}");
    }

    public String rotateKey(String id, int graceHours, String idempotencyKey) throws Exception {
        return request("POST", "/account/keys/rotate/" + id, "{\"grace_hours\":" + graceHours + ",\"idempotency_key\":\"" + idempotencyKey + "\"}");
    }

    public String revokeKey(String id) throws Exception { return request("DELETE", "/account/keys/revoke/" + id, null); }
}
