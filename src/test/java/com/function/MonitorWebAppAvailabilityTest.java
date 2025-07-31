package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MonitorWebAppAvailabilityTest {

    private MonitorWebAppAvailability monitor;
    private ExecutionContext context;
    private Logger logger;

    @BeforeEach
    public void setup() {
        monitor = new MonitorWebAppAvailability();
        context = mock(ExecutionContext.class);
        logger = Logger.getLogger("TestLogger");
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void testCheckEndpointSuccess200() throws Exception {
        String url = "https://mock-success.com";

        HttpClient client = mock(HttpClient.class);
        HttpRequest expectedRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockResponse);

        Map<String, Object> result = monitor.testCheckEndpointWithClient(url, client, context);

        assertNotNull(result);
        assertEquals(200, result.get("status"));
        assertEquals(url, result.get("url"));
    }

    @Test
    public void testCheckEndpointServiceUnavailable503() throws Exception {
        String url = "https://mock-503.com";

        HttpClient client = mock(HttpClient.class);
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(503);
        when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockResponse);

        Map<String, Object> result = monitor.testCheckEndpointWithClient(url, client, context);

        assertNotNull(result);
        assertEquals(503, result.get("status"));
    }

    @Test
    public void testCheckEndpointWithIOException() throws Exception {
        String url = "https://mock-timeout.com";

        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenThrow(new IOException("Timeout simulated"));

        Map<String, Object> result = monitor.testCheckEndpointWithClient(url, client, context);

        assertNull(result); // Después de 3 reintentos fallidos
    }
}
