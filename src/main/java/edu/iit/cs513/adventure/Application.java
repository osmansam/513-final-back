package edu.iit.cs513.adventure;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

/** Starts the backend and keeps it running until the process is stopped. */
public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        try {
            ServerConfig config = ServerConfig.fromEnvironment(System.getenv());
            AdventureServer server = new AdventureServer(config.host(), config.port(), config.frontendOrigin());
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "backend-shutdown"));
            System.out.printf("Columbus Adventure backend listening on %s:%d%n", config.host(), config.port());
        } catch (IllegalArgumentException | IOException exception) {
            System.err.println("Unable to start backend: " + exception.getMessage());
            System.exit(1);
        }
    }
}
