package edu.iit.cs513.adventure;

import fi.iki.elonen.NanoHTTPD;

/** HTTP boundary for the game backend; gameplay remains independent of NanoHTTPD. */
public final class AdventureServer extends NanoHTTPD {
    private static final String JSON = "application/json; charset=utf-8";
    private static final String HEALTH_METHODS = "GET, OPTIONS";
    private final String frontendOrigin;

    public AdventureServer(String host, int port, String frontendOrigin) {
        super(host, port);
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        if (!"/api/health".equals(session.getUri())) {
            response = json(Response.Status.NOT_FOUND, "{\"error\":\"Not found\"}");
        } else {
            response = switch (session.getMethod()) {
                case GET -> json(Response.Status.OK,
                        "{\"status\":\"ok\",\"service\":\"columbus-adventure-backend\"}");
                case OPTIONS -> {
                    Response preflight = newFixedLengthResponse(Response.Status.NO_CONTENT, JSON, "");
                    preflight.addHeader("Access-Control-Allow-Methods", HEALTH_METHODS);
                    preflight.addHeader("Access-Control-Allow-Headers", "Content-Type");
                    yield preflight;
                }
                default -> {
                    Response error = json(Response.Status.METHOD_NOT_ALLOWED,
                            "{\"error\":\"Method not allowed\"}");
                    error.addHeader("Allow", HEALTH_METHODS);
                    yield error;
                }
            };
        }
        // A fixed allowed origin also applies to errors so React can read them.
        response.addHeader("Access-Control-Allow-Origin", frontendOrigin);
        return response;
    }

    private static Response json(Response.Status status, String body) {
        return newFixedLengthResponse(status, JSON, body);
    }
}
