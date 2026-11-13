package edu.iit.cs513.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdventureServerTest {
    private AdventureServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = new AdventureServer("127.0.0.1", 0, "https://adventure.example");
        server.start(5000, true);
        baseUrl = "http://127.0.0.1:" + server.getListeningPort();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @AfterEach
    void stopServer() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthEndpointReturnsServiceStatusAsJson() throws Exception {
        HttpResponse<String> response = request("GET", "/api/health");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow()
                .startsWith("application/json"));
        assertEquals("{\"status\":\"ok\",\"service\":\"columbus-adventure-backend\"}", response.body());
        assertEquals("https://adventure.example",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    @Test
    void unknownRouteReturnsJsonErrorWithCors() throws Exception {
        HttpResponse<String> response = request("GET", "/api/unknown");

        assertEquals(404, response.statusCode());
        assertEquals("{\"error\":\"Not found\"}", response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow()
                .startsWith("application/json"));
        assertEquals("https://adventure.example",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void unsupportedHealthMethodsReturn405(String method) throws Exception {
        HttpResponse<String> response = request(method, "/api/health");

        assertEquals(405, response.statusCode());
        assertEquals("{\"error\":\"Method not allowed\"}", response.body());
        assertEquals("GET, OPTIONS", response.headers().firstValue("Allow").orElseThrow());
        assertEquals("https://adventure.example",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    @Test
    void preflightAdvertisesAllowedMethodsAndJsonHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
                .timeout(Duration.ofSeconds(3))
                .header("Origin", "https://adventure.example")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertEquals("https://adventure.example",
                response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("GET, OPTIONS",
                response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow());
        assertEquals("Content-Type",
                response.headers().firstValue("Access-Control-Allow-Headers").orElseThrow());
    }

    private HttpResponse<String> request(String method, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(3))
                        .method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
