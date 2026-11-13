package edu.iit.cs513.adventure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ServerConfigTest {
    @Test
    void missingEnvironmentUsesLocalDevelopmentDefaults() {
        ServerConfig config = ServerConfig.fromEnvironment(Map.of());

        assertEquals("127.0.0.1", config.host());
        assertEquals(8080, config.port());
        assertEquals("http://localhost:5173", config.frontendOrigin());
    }

    @Test
    void environmentOverridesDefaults() {
        ServerConfig config = ServerConfig.fromEnvironment(Map.of(
                "HOST", "0.0.0.0", "PORT", "9090",
                "FRONTEND_ORIGIN", "https://adventure.example"));

        assertEquals("0.0.0.0", config.host());
        assertEquals(9090, config.port());
        assertEquals("https://adventure.example", config.frontendOrigin());
    }

    @ParameterizedTest
    @CsvSource({"http://localhost:80, http://localhost",
            "https://EXAMPLE.com:443, https://example.com",
            "HTTP://LOCALHOST:3000, http://localhost:3000"})
    void normalizesOriginsToMatchBrowserCorsHeaders(String configured, String expected) {
        assertEquals(expected,
                ServerConfig.fromEnvironment(Map.of("FRONTEND_ORIGIN", configured)).frontendOrigin());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "65535"})
    void acceptsPortBoundaries(String port) {
        assertEquals(Integer.parseInt(port),
                ServerConfig.fromEnvironment(Map.of("PORT", port)).port());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "65536", "abc", "", "8080.0", "999999999999"})
    void rejectsInvalidPortsWithHelpfulError(String port) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.fromEnvironment(Map.of("PORT", port)));

        assertEquals("PORT must be an integer between 1 and 65535", error.getMessage());
    }

    @Test
    void rejectsBlankBindAddress() {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.fromEnvironment(Map.of("HOST", " ")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "*", "localhost:5173", "ftp://example.com",
            "https://example.com/path", "https://example.com?x=1",
            "https://example.com#section", "https://user@example.com",
            "http://localhost:5173\r\nX-Injected: true"})
    void rejectsValuesThatAreNotSingleHttpOrigins(String origin) {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.fromEnvironment(Map.of("FRONTEND_ORIGIN", origin)));
    }
}
