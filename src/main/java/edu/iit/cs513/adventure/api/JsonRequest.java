package edu.iit.cs513.adventure.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import edu.iit.cs513.adventure.game.Direction;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/** Reads small UTF-8 JSON requests without creating upload files or trusting client field types. */
final class JsonRequest {
    private static final int MAX_BODY_BYTES = 4096;

    private JsonRequest() {
    }

    static JsonObject read(IHTTPSession session, boolean allowEmpty) {
        if (session.getHeaders().containsKey("transfer-encoding")) {
            throw badRequest("Transfer-Encoding is unsupported; send Content-Length");
        }
        long length;
        try {
            String header = session.getHeaders().getOrDefault("content-length", "0");
            if (!header.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            length = Long.parseLong(header);
        } catch (NumberFormatException exception) {
            throw badRequest("Content-Length must be a nonnegative integer");
        }
        if (length > MAX_BODY_BYTES) {
            throw new ApiException(Status.PAYLOAD_TOO_LARGE, "JSON body must not exceed 4096 bytes");
        }
        if (length == 0) {
            if (allowEmpty) {
                return new JsonObject();
            }
            throw badRequest("A JSON request body is required");
        }
        String contentType = session.getHeaders().getOrDefault("content-type", "");
        if (!contentType.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new ApiException(Status.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json");
        }
        String body;
        try {
            byte[] bytes = session.getInputStream().readNBytes((int) length);
            if (bytes.length != length) {
                throw badRequest("Request body is shorter than Content-Length");
            }
            body = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (SocketTimeoutException exception) {
            throw new ApiException(Status.REQUEST_TIMEOUT, "Timed out reading the request body");
        } catch (IOException exception) {
            throw badRequest("Could not read a UTF-8 JSON body");
        }
        try {
            JsonElement value = ApiResponses.GSON.fromJson(body, JsonElement.class);
            if (value == null || !value.isJsonObject()) {
                throw badRequest("Request body must be a JSON object");
            }
            return value.getAsJsonObject();
        } catch (JsonParseException exception) {
            throw badRequest("Malformed JSON request body");
        }
    }

    static void allowOnly(JsonObject body, Set<String> fields) {
        if (!fields.containsAll(body.keySet())) {
            throw badRequest("Unexpected request field");
        }
    }

    static Long optionalSeed(JsonObject body) {
        allowOnly(body, Set.of("seed"));
        if (!body.has("seed")) {
            return null;
        }
        JsonElement seed = body.get("seed");
        try {
            if (seed.isJsonPrimitive() && seed.getAsJsonPrimitive().isNumber()) {
                return seed.getAsBigDecimal().longValueExact();
            }
        } catch (ArithmeticException | NumberFormatException exception) {
            // Do not silently truncate a fractional or out-of-range seed.
        }
        throw badRequest("Seed must be a signed 64-bit integer");
    }

    static Direction direction(JsonObject body) {
        allowOnly(body, Set.of("direction"));
        JsonElement direction = body.get("direction");
        if (direction != null && direction.isJsonPrimitive() && direction.getAsJsonPrimitive().isString()) {
            try {
                return Direction.valueOf(direction.getAsString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                // Use the same error for unknown names and incorrect field types.
            }
        }
        throw badRequest("Direction must be NORTH, EAST, SOUTH, or WEST");
    }

    private static ApiException badRequest(String message) {
        return new ApiException(Status.BAD_REQUEST, message);
    }
}
