package edu.iit.cs513.adventure;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** Startup settings supplied through environment variables. */
public record ServerConfig(String host, int port, String frontendOrigin) {
    public ServerConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("HOST must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("PORT must be an integer between 1 and 65535");
        }
        frontendOrigin = normalizeOrigin(frontendOrigin);
    }

    public static ServerConfig fromEnvironment(Map<String, String> environment) {
        int port;
        try {
            port = Integer.parseInt(environment.getOrDefault("PORT", "8080"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT must be an integer between 1 and 65535");
        }
        return new ServerConfig(environment.getOrDefault("HOST", "127.0.0.1"), port,
                environment.getOrDefault("FRONTEND_ORIGIN", "http://localhost:5173"));
    }

    private static String normalizeOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (("http".equals(scheme) || "https".equals(scheme))
                    && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawPath().isEmpty() && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (uri.getPort() == -1 || (uri.getPort() >= 1 && uri.getPort() <= 65535))) {
                int port = uri.getPort();
                boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80)
                        || ("https".equals(scheme) && port == 443);
                return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                        + (defaultPort ? "" : ":" + port);
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            // Report the same configuration error for malformed URIs and invalid origins.
        }
        throw new IllegalArgumentException("FRONTEND_ORIGIN must be a single HTTP or HTTPS origin, "
                + "for example http://localhost:5173");
    }
}
