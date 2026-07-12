package de.keksuccino.fancymenu.customization.action.actions.other;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.keksuccino.fancymenu.util.WebUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendHttpRequestActionTest {

    private HttpServer server;
    private Boolean originalInternetAvailability;

    @AfterEach
    void cleanUp() throws ReflectiveOperationException {
        if (this.server != null) {
            this.server.stop(0);
        }
        if (this.originalInternetAvailability != null) {
            setInternetAvailability(this.originalInternetAvailability);
        }
    }

    @Test
    void sendsConfiguredRequestWhenCachedInternetAvailabilityIsFalse() throws Exception {
        CountDownLatch requestReceived = new CountDownLatch(1);
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestHeader = new AtomicReference<>();
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/target", exchange -> captureRequest(exchange, requestReceived, requestMethod, requestBody, requestHeader));
        this.server.start();

        this.originalInternetAvailability = setInternetAvailability(false);
        SendHttpRequestAction.HttpRequestConfig config = new SendHttpRequestAction.HttpRequestConfig();
        config.url = "http://127.0.0.1:" + this.server.getAddress().getPort() + "/target";
        config.method = "POST";
        config.body = "request-body";
        config.timeout = 2;
        config.headers.add(new SendHttpRequestAction.HttpHeader("X-FancyMenu-Test", "expected"));

        new SendHttpRequestAction().execute(config.serialize());

        assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "The configured endpoint did not receive the request");
        assertEquals("POST", requestMethod.get());
        assertEquals("request-body", requestBody.get());
        assertEquals("expected", requestHeader.get());
    }

    private static void captureRequest(HttpExchange exchange, CountDownLatch requestReceived, AtomicReference<String> requestMethod, AtomicReference<String> requestBody, AtomicReference<String> requestHeader) {
        try {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestHeader.set(exchange.getRequestHeaders().getFirst("X-FancyMenu-Test"));
            exchange.sendResponseHeaders(204, -1);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            exchange.close();
            requestReceived.countDown();
        }
    }

    private static boolean setInternetAvailability(boolean available) throws ReflectiveOperationException {
        // This deliberately controls the private cache to reproduce the exact false-negative state from issue #1614.
        Field field = WebUtils.class.getDeclaredField("isConnectionAvailable");
        field.setAccessible(true);
        boolean previousValue = field.getBoolean(null);
        field.setBoolean(null, available);
        return previousValue;
    }
}
