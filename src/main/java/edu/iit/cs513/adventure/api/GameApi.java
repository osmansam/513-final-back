package edu.iit.cs513.adventure.api;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** HTTP routes translate validated input into game-service operations. */
public final class GameApi {
    private static final String CREATE_METHODS = "POST, OPTIONS";
    private static final String GAME_METHODS = "GET, DELETE, OPTIONS";
    private static final Pattern GAME_PATH = Pattern.compile("/api/games/([^/]+)(?:/(moves|reset))?");
    private final GameService games = new GameService();

    public Response serve(IHTTPSession request) {
        if ("/api/games".equals(request.getUri())) {
            return switch (request.getMethod()) {
                case POST -> {
                    GameView created = games.create(JsonRequest.optionalSeed(JsonRequest.read(request, true)));
                    Response response = ApiResponses.json(Response.Status.CREATED, created);
                    response.addHeader("Location", "/api/games/" + created.id());
                    yield response;
                }
                case OPTIONS -> ApiResponses.preflight(CREATE_METHODS);
                default -> ApiResponses.methodNotAllowed(CREATE_METHODS);
            };
        }
        Matcher path = GAME_PATH.matcher(request.getUri());
        if (!path.matches()) {
            return ApiResponses.error(Response.Status.NOT_FOUND, "Not found");
        }
        String id = path.group(1);
        String action = path.group(2);
        if (action == null) {
            return switch (request.getMethod()) {
                case GET -> ApiResponses.json(Response.Status.OK, games.get(id));
                case DELETE -> {
                    games.delete(id);
                    yield ApiResponses.noContent();
                }
                case OPTIONS -> ApiResponses.preflight(GAME_METHODS);
                default -> ApiResponses.methodNotAllowed(GAME_METHODS);
            };
        }
        return switch (request.getMethod()) {
            case POST -> {
                if ("moves".equals(action)) {
                    yield ApiResponses.json(Response.Status.OK,
                            games.move(id, JsonRequest.direction(JsonRequest.read(request, false))));
                }
                JsonRequest.allowOnly(JsonRequest.read(request, true), Set.of());
                yield ApiResponses.json(Response.Status.OK, games.reset(id));
            }
            case OPTIONS -> ApiResponses.preflight(CREATE_METHODS);
            default -> ApiResponses.methodNotAllowed(CREATE_METHODS);
        };
    }
}
