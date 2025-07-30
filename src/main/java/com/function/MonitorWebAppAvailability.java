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
       // @TimerTrigger(name = "monitorTrigger", schedule = "0 */5 * * * *") String timerInfo,
          @TimerTrigger(name = "monitorTrigger", schedule = "0 * * * * *") String timerInfo,
        final ExecutionContext context
    ) {
        context.getLogger().info("▶ Ejecutando MonitorWebAppAvailability: " + Instant.now());

        String endpointsJson = System.getenv("ENDPOINTS");

        if (endpointsJson == null || endpointsJson.isBlank()) {
            context.getLogger().warning("Variable ENDPOINTS no definida o vacía.");
            return;
        }

        List<String> urls = new Gson().fromJson(endpointsJson, new TypeToken<List<String>>(){}.getType());
        HttpClient client = HttpClient.newHttpClient();

        for (String url : urls) {
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
                context.getLogger().warning("Error al consultar " + url + ": " + e.getMessage());
            }
        }
    }
}
