package de.keksuccino.fancymenu.util;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebUtilsTest {

    @Test
    void reportsUnavailableBeforeTheFirstAsynchronousProbeCompletes() {
        assertFalse(WebUtils.isInternetAvailable());
    }

    @Test
    void usesGoogleAsInternetAvailabilityEndpoint() throws ReflectiveOperationException {
        // Keep the production configuration private while verifying the exact endpoint without live network I/O.
        Field endpointField = WebUtils.class.getDeclaredField("INTERNET_AVAILABILITY_ENDPOINT");
        endpointField.setAccessible(true);

        assertEquals(URI.create("https://google.com"), endpointField.get(null));
    }

    @Test
    void opensAndReadsAWebResource() throws Exception {
        byte[] body = "web-body".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/resource", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try (InputStream stream = WebUtils.openResourceStreamOrThrow(url(server, "/resource"), WebUtils.WebResourceType.TEXT)) {
            assertArrayEquals(body, stream.readAllBytes());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failedOpensThrowTheirCauseOrGiveNull() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1L);
            exchange.close();
        });
        server.start();

        try {
            String missing = url(server, "/missing");
            assertThrows(IOException.class, () -> WebUtils.openResourceStreamOrThrow(missing, WebUtils.WebResourceType.TEXT));
            assertNull(WebUtils.openResourceStream(missing, WebUtils.WebResourceType.TEXT));
        } finally {
            server.stop(0);
        }

        String refused = "http://127.0.0.1:" + unusedPort() + "/refused";
        IOException refusedFailure = assertThrows(IOException.class, () -> WebUtils.openResourceStreamOrThrow(refused, WebUtils.WebResourceType.TEXT));
        assertInstanceOf(ConnectException.class, refusedFailure);
        assertNull(WebUtils.openResourceStream(refused, WebUtils.WebResourceType.TEXT));
    }

    @Test
    void urlsThatAreNoHttpUrlsThrowOrGiveNull() {
        assertThrows(IOException.class, () -> WebUtils.openResourceStreamOrThrow("ftp://127.0.0.1/resource", WebUtils.WebResourceType.TEXT));
        assertThrows(IOException.class, () -> WebUtils.openResourceStreamOrThrow("not a url", WebUtils.WebResourceType.TEXT));
        assertNull(WebUtils.openResourceStream("ftp://127.0.0.1/resource", WebUtils.WebResourceType.TEXT));
    }

    private static String url(HttpServer server, String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            return socket.getLocalPort();
        }
    }
}
