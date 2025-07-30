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
 * Azure Function para monitorear disponibilidad de endpoints y loguear los resultados.
 */
public class MonitorWebAppAvailability {

    @FunctionName("MonitorWebAppAvailability")
    public void run(
        @TimerTrigger(name = "monitorTrigger", schedule = "0 * * * * *") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("▶ Ejecutando MonitorWebAppAvailability: " + Instant.now());

        // Leer variables por región
        String endpointsUsJson = System.getenv("ENDPOINTS_US");
        String endpointsEuJson = System.getenv("ENDPOINTS_EU");

        List<String> allEndpoints = new ArrayList<>();

        allEndpoints.addAll(parseJsonEndpoints(endpointsUsJson, "ENDPOINTS_US", context));
        allEndpoints.addAll(parseJsonEndpoints(endpointsEuJson, "ENDPOINTS_EU", context));

        if (allEndpoints.isEmpty()) {
            context.getLogger().warning("❌ No se encontraron endpoints válidos en ninguna región.");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();

        for (String url : allEndpoints) {
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

                context.getLogger().info("[LOG] " + new Gson().toJson(log));

            } catch (IOException | InterruptedException e) {
                context.getLogger().warning("⚠ Error al consultar " + url + ": " + e.getMessage());
            }
        }
    }

    /**
     * Valida y parsea una variable JSON de endpoints.
     */
    private List<String> parseJsonEndpoints(String json, String envVarName, ExecutionContext context) {
        List<String> urls = new ArrayList<>();

        if (json == null || json.isBlank()) {
            context.getLogger().warning("⚠ Variable " + envVarName + " no definida o vacía.");
            return urls;
        }

        try {
            urls = new Gson().fromJson(json, new TypeToken<List<String>>() {}.getType());
        } catch (Exception e) {
            context.getLogger().warning("❌ Variable " + envVarName + " no tiene formato JSON válido: " + e.getMessage());
            return new ArrayList<>();
        }

        // Validar URLs
        List<String> validUrls = new ArrayList<>();
        for (String url : urls) {
            try {
                new URI(url); // validación básica
                validUrls.add(url);
            } catch (Exception e) {
                context.getLogger().warning("⚠ URL inválida en " + envVarName + ": " + url);
            }
        }

        return validUrls;
    }
}
