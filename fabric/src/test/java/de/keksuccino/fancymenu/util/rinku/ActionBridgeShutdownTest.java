package de.keksuccino.fancymenu.util.rinku;

import org.cef.browser.CefMessageRouter;
import org.cef.browser.RecordingMessageRouter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ActionBridgeShutdownTest {

    private final Map<Field, Object> originalState = new LinkedHashMap<>();

    @BeforeEach
    void saveBridgeState() throws ReflectiveOperationException {
        for (String name : new String[]{"messageRouter", "initialized", "shuttingDown"}) {
            Field field = field(name);
            this.originalState.put(field, field.get(null));
        }
        field("messageRouter").set(null, null);
        field("initialized").setBoolean(null, false);
        field("shuttingDown").setBoolean(null, false);
    }

    @AfterEach
    void restoreBridgeState() throws IllegalAccessException {
        for (Map.Entry<Field, Object> entry : this.originalState.entrySet()) {
            entry.getKey().set(null, entry.getValue());
        }
    }

    @Test
    void shutdownLeavesRegisteredRouterForItsCefOwner() throws ReflectiveOperationException {
        RecordingMessageRouter router = registerRouter();

        ActionBridge.dispose();

        assertEquals(0, router.getDisposalCount());
        assertStopped();
        // CEF owns registered routers and disposes them when its client shuts down.
        router.dispose();
        assertEquals(1, router.getDisposalCount());
    }

    @Test
    void shutdownAfterCefDoesNotDisposeRouterAgain() throws ReflectiveOperationException {
        RecordingMessageRouter router = registerRouter();
        router.dispose();

        ActionBridge.dispose();

        assertEquals(1, router.getDisposalCount());
        assertStopped();
    }

    @Test
    void repeatedShutdownDoesNotTouchOwnedRouter() throws ReflectiveOperationException {
        RecordingMessageRouter router = registerRouter();

        ActionBridge.dispose();
        ActionBridge.dispose();

        assertEquals(0, router.getDisposalCount());
        assertStopped();
    }

    @Test
    void shutdownWithoutInitializationStillPreventsInitialization() throws ReflectiveOperationException {
        ActionBridge.dispose();
        ActionBridge.dispose();

        assertStopped();
    }

    @Test
    void successfulRegistrationTransfersRouterWithoutDisposingIt() {
        RecordingMessageRouter router = new RecordingMessageRouter();
        List<CefMessageRouter> registered = new ArrayList<>();

        ActionBridge.registerMessageRouter(router, registered::add);

        assertEquals(List.of(router), registered);
        assertEquals(0, router.getDisposalCount());
    }

    @Test
    void rejectedHandlerReleasesUnregisteredRouter() {
        RecordingMessageRouter router = new RecordingMessageRouter();
        router.acceptHandler = false;

        assertThrows(IllegalStateException.class, () -> ActionBridge.registerMessageRouter(router, ignored -> fail("Router must not be registered without its handler")));

        assertEquals(1, router.getDisposalCount());
    }

    @Test
    void registrationFailureReleasesUnregisteredRouter() {
        RecordingMessageRouter router = new RecordingMessageRouter();
        RuntimeException failure = new IllegalStateException();

        assertSame(failure, assertThrows(IllegalStateException.class, () -> ActionBridge.registerMessageRouter(router, ignored -> {
            throw failure;
        })));

        assertEquals(1, router.getDisposalCount());
    }

    @Test
    void cleanupFailurePreservesRegistrationFailure() {
        RecordingMessageRouter router = new RecordingMessageRouter();
        router.disposalFailure = new IllegalStateException();
        Error failure = new LinkageError();

        assertSame(failure, assertThrows(LinkageError.class, () -> ActionBridge.registerMessageRouter(router, ignored -> {
            throw failure;
        })));

        assertEquals(1, router.getDisposalCount());
        assertEquals(1, failure.getSuppressed().length);
        assertSame(router.disposalFailure, failure.getSuppressed()[0]);
    }

    private static RecordingMessageRouter registerRouter() throws ReflectiveOperationException {
        RecordingMessageRouter router = new RecordingMessageRouter();
        field("messageRouter").set(null, router);
        field("initialized").setBoolean(null, true);
        return router;
    }

    private static void assertStopped() throws ReflectiveOperationException {
        assertNull(field("messageRouter").get(null));
        assertFalse(field("initialized").getBoolean(null));
        assertTrue(field("shuttingDown").getBoolean(null));
        assertFalse(ActionBridge.initializeIfNecessary());
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = ActionBridge.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

}
