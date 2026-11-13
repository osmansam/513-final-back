package edu.iit.cs513.adventure;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpRequestValidationTest {
    private AdventureServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new AdventureServer("127.0.0.1", 0, "http://localhost:5173");
        server.start(200, true);
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc", "99999999999999999999999"})
    void invalidContentLengthsReturnJsonErrorsInsteadOfCrashing(String length) throws Exception {
        assertJsonError(exchange("Content-Length: " + length + "\r\n", new byte[0], true), 400);
    }

    @Test
    void unsupportedChunkedEncodingIsRejectedBeforeReadingTheBody() throws Exception {
        assertJsonError(exchange("Transfer-Encoding: chunked\r\n", new byte[0], false), 400);
    }

    @Test
    void truncatedBodyReturns400WhenTheClientClosesItsOutput() throws Exception {
        assertJsonError(exchange("Content-Type: application/json\r\nContent-Length: 20\r\n",
                "{}".getBytes(StandardCharsets.UTF_8), true), 400);
    }

    @Test
    void incompleteBodyReturns408WhenTheClientNeverFinishesSending() throws Exception {
        assertJsonError(exchange("Content-Type: application/json\r\nContent-Length: 20\r\n",
                "{".getBytes(StandardCharsets.UTF_8), false), 408);
    }

    @Test
    void invalidUtf8ReturnsAJsonError() throws Exception {
        assertJsonError(exchange("Content-Type: application/json\r\nContent-Length: 2\r\n",
                new byte[]{(byte) 0xc3, 0x28}, true), 400);
    }

    @Test
    void acceptsAJsonBodyExactlyAtTheSizeLimit() throws Exception {
        byte[] body = ("{}" + " ".repeat(4094)).getBytes(StandardCharsets.UTF_8);
        String response = exchange("Content-Type: application/json\r\nContent-Length: 4096\r\n", body, true);

        assertTrue(response.startsWith("HTTP/1.1 201"), response);
        assertTrue(JsonParser.parseString(response.split("\r\n\r\n", 2)[1]).getAsJsonObject().has("id"));
    }

    @Test
    void rejectedBodyCannotBeReusedAsAnotherRequestOnTheSameConnection() throws Exception {
        String pipelined = "{}GET /api/health HTTP/1.1\r\nHost: localhost\r\n\r\n";
        String response = exchange("Content-Type: text/plain\r\nContent-Length: 2\r\n",
                pipelined.getBytes(StandardCharsets.UTF_8), true);

        assertJsonError(response, 415);
        assertEquals(1, response.split("HTTP/1.1 ", -1).length - 1);
        assertTrue(response.toLowerCase().contains("connection: close"));
    }

    private String exchange(String headers, byte[] body, boolean finishOutput) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.getListeningPort())) {
            socket.setSoTimeout(3000);
            socket.getOutputStream().write(("POST /api/games HTTP/1.1\r\nHost: localhost\r\n"
                    + headers + "\r\n").getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().write(body);
            socket.getOutputStream().flush();
            if (finishOutput) {
                socket.shutdownOutput();
            }
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertJsonError(String response, int status) {
        assertTrue(response.startsWith("HTTP/1.1 " + status), response);
        String[] parts = response.split("\r\n\r\n", 2);
        assertEquals(2, parts.length, response);
        assertTrue(parts[0].toLowerCase().contains("content-type: application/json"));
        assertTrue(parts[0].toLowerCase().contains("access-control-allow-origin: http://localhost:5173"));
        assertTrue(JsonParser.parseString(parts[1]).getAsJsonObject().has("error"));
    }
}
