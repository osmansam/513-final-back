package edu.iit.cs513.adventure;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameIntegrationTest {
    private AdventureServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = new AdventureServer("127.0.0.1", 0, "http://localhost:5173");
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
    void fullWinningGameStopsAcceptingMovesAndReplaysIdenticallyAfterReset() throws Exception {
        JsonObject initial = create(2);
        String path = "/api/games/" + initial.get("id").getAsString();
        List<String> moves = List.of("SOUTH", "SOUTH", "EAST", "SOUTH");

        List<JsonObject> results = play(path, moves);
        JsonObject won = results.getLast().getAsJsonObject("snapshot");

        assertEquals("WON", won.get("state").getAsString());
        assertEquals(4, won.get("turnCount").getAsInt());
        assertEquals(10, won.getAsJsonObject("player").get("x").getAsInt());
        assertEquals(6, won.getAsJsonObject("player").get("y").getAsInt());
        assertEquals(won.get("treasure"), won.get("player"));
        assertFrozen(path, won);
        assertEquals(initial, send("POST", path + "/reset", ""));
        assertEquals(results, play(path, moves));
    }

    @Test
    void fullPirateCaptureStopsAcceptingMovesAndResetRestoresAllEnemyState() throws Exception {
        JsonObject initial = create(513);
        String path = "/api/games/" + initial.get("id").getAsString();
        List<String> moves = new ArrayList<>();
        for (int step = 0; step < 10; step++) {
            moves.add("EAST");
        }
        moves.add("SOUTH");

        List<JsonObject> results = play(path, moves);
        JsonObject lost = results.getLast().getAsJsonObject("snapshot");

        assertEquals("LOST", lost.get("state").getAsString());
        assertEquals(11, lost.get("turnCount").getAsInt());
        assertTrue(lost.getAsJsonArray("pirates").asList().stream()
                .anyMatch(pirate -> pirate.getAsJsonObject().get("position").equals(lost.get("player"))));
        assertFrozen(path, lost);
        assertEquals(initial, send("POST", path + "/reset", "{}"));
        assertEquals(results, play(path, moves));
    }

    @Test
    void blockedMovesAndMissingBodiesDoNotChangeAGame() throws Exception {
        JsonObject initial = create(513);
        String path = "/api/games/" + initial.get("id").getAsString();

        JsonObject blocked = send("POST", path + "/moves", "{\"direction\":\"WEST\"}");
        assertEquals("BLOCKED", blocked.get("status").getAsString());
        assertEquals(initial.get("snapshot"), blocked.get("snapshot"));
        assertEquals(400, request("POST", path + "/moves", "").statusCode());
        assertEquals(initial, send("GET", path, null));
        JsonObject moved = send("POST", path + "/moves", "{\"direction\":\" east \"}");
        assertEquals("APPLIED", moved.get("status").getAsString());
        assertEquals(1, moved.getAsJsonObject("snapshot").getAsJsonObject("player").get("x").getAsInt());
    }

    @Test
    void concurrentHttpMovesProduceDistinctCompleteTurns() throws Exception {
        String path = "/api/games/" + create(513).get("id").getAsString();
        List<Callable<JsonObject>> moves = new ArrayList<>();
        for (int step = 0; step < 8; step++) {
            moves.add(() -> send("POST", path + "/moves", "{\"direction\":\"EAST\"}"));
        }
        Set<Integer> turns = new HashSet<>();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (var future : executor.invokeAll(moves, 5, TimeUnit.SECONDS)) {
                JsonObject result = future.get();
                assertEquals("APPLIED", result.get("status").getAsString());
                JsonObject snapshot = result.getAsJsonObject("snapshot");
                int turn = snapshot.get("turnCount").getAsInt();
                assertEquals(turn, snapshot.getAsJsonObject("player").get("x").getAsInt());
                assertEquals(7, snapshot.getAsJsonObject("player").get("y").getAsInt());
                turns.add(turn);
            }
        }

        assertEquals(Set.of(1, 2, 3, 4, 5, 6, 7, 8), turns);
        assertEquals(8, send("GET", path, null).getAsJsonObject("snapshot").get("turnCount").getAsInt());
    }

    @Test
    void concurrentMoveAndResetResolveAsWholeOperations() throws Exception {
        JsonObject initial = create(513);
        String path = "/api/games/" + initial.get("id").getAsString();
        send("POST", path + "/moves", "{\"direction\":\"EAST\"}");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var reset = executor.submit(() -> send("POST", path + "/reset", "{}"));
            var move = executor.submit(() -> send("POST", path + "/moves", "{\"direction\":\"EAST\"}"));
            assertEquals(initial, reset.get(5, TimeUnit.SECONDS));
            JsonObject result = move.get(5, TimeUnit.SECONDS);
            assertEquals("APPLIED", result.get("status").getAsString());
            int turn = result.getAsJsonObject("snapshot").get("turnCount").getAsInt();
            assertTrue(turn == 1 || turn == 2);
            assertEquals(turn == 1 ? result.get("snapshot") : initial.get("snapshot"),
                    send("GET", path, null).get("snapshot"));
        }
    }

    @Test
    void differentServerInstancesDoNotShareTheirGameCollections() throws Exception {
        String id = create(513).get("id").getAsString();
        AdventureServer other = new AdventureServer("127.0.0.1", 0, "http://localhost:5173");
        try {
            other.start(5000, true);
            var response = client.send(HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + other.getListeningPort() + "/api/games/" + id))
                    .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode());
        } finally {
            other.stop();
        }
    }

    private JsonObject create(long seed) throws Exception {
        HttpResponse<String> response = request("POST", "/api/games", "{\"seed\":" + seed + "}");
        assertEquals(201, response.statusCode());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private List<JsonObject> play(String path, List<String> directions) throws Exception {
        List<JsonObject> results = new ArrayList<>();
        for (String direction : directions) {
            JsonObject result = send("POST", path + "/moves", "{\"direction\":\"" + direction + "\"}");
            assertEquals("APPLIED", result.get("status").getAsString());
            results.add(result);
        }
        return results;
    }

    private void assertFrozen(String path, JsonObject snapshot) throws Exception {
        for (String direction : List.of("NORTH", "EAST", "SOUTH", "WEST")) {
            JsonObject result = send("POST", path + "/moves", "{\"direction\":\"" + direction + "\"}");
            assertEquals("GAME_OVER", result.get("status").getAsString());
            assertEquals(snapshot, result.get("snapshot"));
        }
        assertEquals(snapshot, send("GET", path, null).get("snapshot"));
    }

    private JsonObject send(String method, String path, String body) throws Exception {
        HttpResponse<String> response = request(method, path, body);
        assertEquals(200, response.statusCode(), response.body());
        assertEquals("http://localhost:5173", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(3));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
