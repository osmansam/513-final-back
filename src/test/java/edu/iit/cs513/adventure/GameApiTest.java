package edu.iit.cs513.adventure;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameApiTest {
    private AdventureServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = new AdventureServer("127.0.0.1", 0, "http://localhost:3000");
        server.start(5000, true);
        baseUrl = "http://127.0.0.1:" + server.getListeningPort();
        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3)).build();
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
    void createsAGameAndReturnsItsLocationAndFrontendSnapshot() throws Exception {
        HttpResponse<String> response = request("POST", "/api/games", "", null);

        assertEquals(201, response.statusCode());
        JsonObject game = json(response);
        String id = game.get("id").getAsString();
        assertEquals(id, UUID.fromString(id).toString());
        assertEquals("/api/games/" + id, response.headers().firstValue("Location").orElseThrow());
        assertEquals("Location", response.headers().firstValue("Access-Control-Expose-Headers").orElseThrow());
        assertCors(response);
        assertEquals("no-store", response.headers().firstValue("Cache-Control").orElseThrow());
        JsonObject snapshot = game.getAsJsonObject("snapshot");
        assertEquals(Set.of("width", "height", "islands", "player", "treasure", "pirates", "monsters", "state", "turnCount"),
                snapshot.keySet());
        assertEquals(20, snapshot.get("width").getAsInt());
        assertEquals(20, snapshot.get("height").getAsInt());
        assertEquals(40, snapshot.getAsJsonArray("islands").size());
        assertEquals(2, snapshot.getAsJsonArray("pirates").size());
        assertEquals(2, snapshot.getAsJsonArray("monsters").size());
        assertEquals("PLAYING", snapshot.get("state").getAsString());
        assertEquals(0, snapshot.get("turnCount").getAsInt());
        assertEquals("HUNTER", snapshot.getAsJsonArray("pirates").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("INTERCEPTOR", snapshot.getAsJsonArray("pirates").get(1).getAsJsonObject().get("type").getAsString());
        assertEquals(Set.of("id", "position", "roamingArea"),
                snapshot.getAsJsonArray("monsters").get(0).getAsJsonObject().keySet());
        assertEquals(game, json(request("GET", "/api/games/" + id, null, null)));
    }

    @Test
    void gamesAreIndependentAndMovesReturnTheirResultingState() throws Exception {
        JsonObject first = createSeededGame();
        JsonObject second = createSeededGame();
        String firstId = first.get("id").getAsString();
        String secondId = second.get("id").getAsString();
        assertNotEquals(firstId, secondId);
        JsonObject before = first.getAsJsonObject("snapshot");
        Move move = legalMove(before);

        HttpResponse<String> response = request("POST", "/api/games/" + firstId + "/moves",
                "{\"direction\":\"" + move.direction() + "\"}", "application/json; charset=utf-8");

        assertEquals(200, response.statusCode());
        JsonObject result = json(response);
        assertEquals("APPLIED", result.get("status").getAsString());
        JsonObject after = result.getAsJsonObject("snapshot");
        assertEquals(move.x(), after.getAsJsonObject("player").get("x").getAsInt());
        assertEquals(move.y(), after.getAsJsonObject("player").get("y").getAsInt());
        assertEquals(1, after.get("turnCount").getAsInt());
        assertEquals(after, json(request("GET", "/api/games/" + firstId, null, null)).get("snapshot"));
        assertEquals(second, json(request("GET", "/api/games/" + secondId, null, null)));
        assertCors(response);
    }

    @Test
    void seededResetRestoresTheOriginalSnapshotAndKeepsTheGameId() throws Exception {
        JsonObject initial = createSeededGame();
        String path = "/api/games/" + initial.get("id").getAsString();
        Move move = legalMove(initial.getAsJsonObject("snapshot"));
        request("POST", path + "/moves", "{\"direction\":\"" + move.direction() + "\"}", "application/json");

        HttpResponse<String> reset = request("POST", path + "/reset", "{}", "application/json");

        assertEquals(200, reset.statusCode());
        assertEquals(initial, json(reset));
    }

    @Test
    void deletingAGameMakesAllOfItsOperationsReturn404() throws Exception {
        String path = "/api/games/" + createSeededGame().get("id").getAsString();
        HttpResponse<String> deleted = request("DELETE", path, null, null);
        assertEquals(204, deleted.statusCode());
        assertEquals("", deleted.body());

        for (HttpResponse<String> response : List.of(request("GET", path, null, null),
                request("POST", path + "/moves", "{\"direction\":\"EAST\"}", "application/json"),
                request("POST", path + "/reset", "", null), request("DELETE", path, null, null))) {
            assertEquals(404, response.statusCode());
            assertEquals("Game not found", json(response).get("error").getAsString());
            assertCors(response);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "true", "\"513\"", "1.5", "9223372036854775808", "{}", "[]"})
    void rejectsSeedsThatAreNotSigned64BitIntegers(String seed) throws Exception {
        assertJsonError(request("POST", "/api/games", "{\"seed\":" + seed + "}", "application/json"), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-9223372036854775808", "9223372036854775807"})
    void acceptsSigned64BitSeedBoundaries(String seed) throws Exception {
        assertEquals(201, request("POST", "/api/games", "{\"seed\":" + seed + "}", "application/json").statusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{seed:1}", "{\"seed\":1,}", "{/*comment*/\"seed\":1}", "{}{}", "null", "[]", "true"})
    void rejectsMalformedJsonAndNonObjectRequests(String body) throws Exception {
        assertJsonError(request("POST", "/api/games", body, "application/json"), 400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"direction\":null}", "{\"direction\":1}", "{\"direction\":true}",
            "{\"direction\":\"\"}", "{\"direction\":\"UP\"}", "{\"direction\":{}}", "{\"direction\":[]}"})
    void invalidMovesDoNotChangeTheGame(String body) throws Exception {
        JsonObject before = createSeededGame();
        String path = "/api/games/" + before.get("id").getAsString();

        assertJsonError(request("POST", path + "/moves", body, "application/json"), 400);
        assertEquals(before, json(request("GET", path, null, null)));
    }

    @Test
    void rejectsUnknownRequestFieldsRatherThanIgnoringThem() throws Exception {
        assertJsonError(request("POST", "/api/games", "{\"size\":50}", "application/json"), 400);
        String path = "/api/games/" + createSeededGame().get("id").getAsString();
        assertJsonError(request("POST", path + "/moves", "{\"direction\":\"EAST\",\"speed\":2}", "application/json"), 400);
        assertJsonError(request("POST", path + "/reset", "{\"seed\":12}", "application/json"), 400);
    }

    @Test
    void rejectsNonJsonBodiesAndOversizedRequests() throws Exception {
        assertJsonError(request("POST", "/api/games", "{}", "text/plain"), 415);
        assertJsonError(request("POST", "/api/games", "{}", null), 415);
        assertJsonError(request("POST", "/api/games", " ".repeat(4097), "application/json"), 413);
    }

    @ParameterizedTest
    @CsvSource(value = {"/api/games|POST, OPTIONS", "/api/games/missing|GET, DELETE, OPTIONS",
            "/api/games/missing/moves|POST, OPTIONS", "/api/games/missing/reset|POST, OPTIONS"}, delimiter = '|')
    void browserPreflightAdvertisesMethodsWithoutNeedingAnExistingGame(String path, String allowed) throws Exception {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(3))
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertCors(response);
        assertEquals(allowed, response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow());
        assertEquals("Content-Type", response.headers().firstValue("Access-Control-Allow-Headers").orElseThrow());
    }

    @ParameterizedTest
    @CsvSource(value = {"GET|/api/games|POST, OPTIONS", "PUT|/api/games|POST, OPTIONS",
            "POST|/api/games/missing|GET, DELETE, OPTIONS", "GET|/api/games/missing/moves|POST, OPTIONS",
            "DELETE|/api/games/missing/reset|POST, OPTIONS"}, delimiter = '|')
    void unsupportedMethodsReturn405WithAllow(String method, String path, String allowed) throws Exception {
        HttpResponse<String> response = request(method, path, null, null);
        assertJsonError(response, 405);
        assertEquals(allowed, response.headers().firstValue("Allow").orElseThrow());
    }

    private JsonObject createSeededGame() throws Exception {
        HttpResponse<String> response = request("POST", "/api/games", "{\"seed\":513}", "application/json");
        assertEquals(201, response.statusCode());
        return json(response);
    }

    private HttpResponse<String> request(String method, String path, String body, String contentType) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(3));
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject json(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static void assertJsonError(HttpResponse<String> response, int status) {
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(json(response).get("error").getAsString().length() > 0);
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
        assertCors(response);
    }

    private static void assertCors(HttpResponse<String> response) {
        assertEquals("http://localhost:3000", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
    }

    private static Move legalMove(JsonObject snapshot) {
        int x = snapshot.getAsJsonObject("player").get("x").getAsInt();
        int y = snapshot.getAsJsonObject("player").get("y").getAsInt();
        for (Move candidate : List.of(new Move("NORTH", x, y - 1), new Move("EAST", x + 1, y),
                new Move("SOUTH", x, y + 1), new Move("WEST", x - 1, y))) {
            if (candidate.x() < 0 || candidate.y() < 0 || candidate.x() >= snapshot.get("width").getAsInt()
                    || candidate.y() >= snapshot.get("height").getAsInt()) {
                continue;
            }
            boolean island = false;
            for (var element : snapshot.getAsJsonArray("islands")) {
                JsonObject position = element.getAsJsonObject();
                if (position.get("x").getAsInt() == candidate.x() && position.get("y").getAsInt() == candidate.y()) {
                    island = true;
                    break;
                }
            }
            if (!island) {
                return candidate;
            }
        }
        throw new AssertionError("Generated game has no legal first move");
    }

    private record Move(String direction, int x, int y) {
    }
}
