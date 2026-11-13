package edu.iit.cs513.adventure;

import edu.iit.cs513.adventure.api.ApiException;
import edu.iit.cs513.adventure.api.ApiResponses;
import edu.iit.cs513.adventure.api.GameApi;
import fi.iki.elonen.NanoHTTPD;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** HTTP boundary for the game backend; gameplay remains independent of NanoHTTPD. */
public final class AdventureServer extends NanoHTTPD {
    private static final Logger LOG = Logger.getLogger(AdventureServer.class.getName());
    private static final String HEALTH_METHODS = "GET, OPTIONS";
    private final String frontendOrigin;
    private final GameApi gameApi = new GameApi();

    public AdventureServer(String host, int port, String frontendOrigin) {
        super(host, port);
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response response;
        try {
            if ("/api/health".equals(session.getUri())) {
                response = switch (session.getMethod()) {
                    case GET -> ApiResponses.json(Response.Status.OK,
                            new Health("ok", "columbus-adventure-backend"));
                    case OPTIONS -> ApiResponses.preflight(HEALTH_METHODS);
                    default -> ApiResponses.methodNotAllowed(HEALTH_METHODS);
                };
            } else {
                response = gameApi.serve(session);
            }
        } catch (ApiException exception) {
            response = ApiResponses.error(exception.status(), exception.getMessage());
        } catch (NoSuchElementException exception) {
            response = ApiResponses.error(Response.Status.NOT_FOUND, "Game not found");
        } catch (RuntimeException exception) {
            LOG.log(Level.SEVERE, "Unable to handle API request", exception);
            response = ApiResponses.error(Response.Status.INTERNAL_ERROR, "Internal server error");
        }
        // A fixed allowed origin also applies to errors so React can read them.
        response.addHeader("Access-Control-Allow-Origin", frontendOrigin);
        response.addHeader("Access-Control-Expose-Headers", "Location");
        response.addHeader("Cache-Control", "no-store");
        // Errors may reject a body before reading it; do not reuse a connection with unread bytes.
        response.closeConnection(true);
        return response;
    }

    private record Health(String status, String service) {
    }
}
