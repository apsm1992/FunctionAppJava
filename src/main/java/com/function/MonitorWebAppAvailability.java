package com.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Azure Function para monitorear disponibilidad de endpoints y enviar logs a Grail.
 */
public class MonitorWebAppAvailability {

    private static final String GRAIL_ENDPOINT = System.getenv("GRAIL_ENDPOINT");
    private static final String GRAIL_AUTH_TOKEN = System.getenv("GRAIL_AUTH_TOKEN");
    private static final int MAX_RETRIES = 3;

    @FunctionName("MonitorWebAppAvailability")
    public void run(
        @TimerTrigger(name = "monitorTrigger", schedule = "%MONITOR_SCHEDULE%") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("Ejecutando MonitorWebAppAvailability: " + Instant.now());

        String endpointsUsJson = System.getenv("ENDPOINTS_US");
        String endpointsEuJson = System.getenv("ENDPOINTS_EU");

        List<String> allEndpoints = new ArrayList<>();
        allEndpoints.addAll(parseJsonEndpoints(endpointsUsJson, "ENDPOINTS_US", context));
        allEndpoints.addAll(parseJsonEndpoints(endpointsEuJson, "ENDPOINTS_EU", context));

        if (allEndpoints.isEmpty()) {
            context.getLogger().warning("No se encontraron endpoints válidos en ninguna región.");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        List<Map<String, Object>> logs = new ArrayList<>();

        for (String url : allEndpoints) {
            Map<String, Object> log = checkEndpointWithRetries(url, client, context);
            if (log != null) {
                logs.add(log);
            }
        }

        if (!logs.isEmpty()) {
            sendLogsToGrail(client, logs, context);
        }
    }

    private Map<String, Object> checkEndpointWithRetries(String url, HttpClient client, ExecutionContext context) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                long start = System.currentTimeMillis();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                long duration = System.currentTimeMillis() - start;

                Map<String, Object> log = new HashMap<>();
                log.put("timestamp", Instant.now().toString());
                log.put("url", url);
                log.put("status", response.statusCode());
                log.put("responseTimeMs", duration);
                log.put("source", "MonitorWebAppAvailability");
                log.put("level", response.statusCode() >= 200 && response.statusCode() < 400 ? "INFO" : "ERROR");

                return log;

            } catch (IOException | InterruptedException e) {
                context.getLogger().warning("Intento " + (attempt + 1) + " fallido para " + url + ": " + e.getMessage());
            }

            attempt++;
            try {
                long backoff = (long) Math.pow(2, attempt);
                Thread.sleep(backoff * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        context.getLogger().severe("Todos los intentos fallaron para " + url);
        return null;
    }

    private List<String> parseJsonEndpoints(String json, String envVarName, ExecutionContext context) {
        List<String> urls = new ArrayList<>();

        if (json == null || json.isBlank()) {
            context.getLogger().warning("Variable " + envVarName + " no definida o vacía.");
            return urls;
        }

        try {
            urls = new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            context.getLogger().warning("Variable " + envVarName + " no tiene formato JSON válido: " + e.getMessage());
            return new ArrayList<>();
        }

        List<String> validUrls = new ArrayList<>();
        for (String url : urls) {
            try {
                new URI(url);
                validUrls.add(url);
            } catch (Exception e) {
                context.getLogger().warning("URL inválida en " + envVarName + ": " + url);
            }
        }

        return validUrls;
    }

    private void sendLogsToGrail(HttpClient client, List<Map<String, Object>> logs, ExecutionContext context) {
        if (GRAIL_ENDPOINT == null || GRAIL_AUTH_TOKEN == null) {
            context.getLogger().severe("GRAIL_ENDPOINT o GRAIL_AUTH_TOKEN no están definidos en el entorno.");
            return;
        }

        String jsonPayload = new Gson().toJson(logs);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAIL_ENDPOINT))
                .timeout(java.time.Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Authorization", GRAIL_AUTH_TOKEN)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            context.getLogger().info("Envío real a Grail completado. Código de respuesta: " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            context.getLogger().severe("Error al enviar logs reales a Grail: " + e.getMessage());
        }
    }

    // Método público auxiliar para pruebas (Card #8)
    public Map<String, Object> testCheckEndpointWithClient(String url, HttpClient client, ExecutionContext context) {
        return checkEndpointWithRetries(url, client, context);
    }
}
