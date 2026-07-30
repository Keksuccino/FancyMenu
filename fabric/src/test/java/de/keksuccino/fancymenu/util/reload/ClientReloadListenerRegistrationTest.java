package de.keksuccino.fancymenu.util.reload;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientReloadListenerRegistrationTest {

    @Test
    void duplicateRegistrationInvokesTheExpensiveSequenceOncePerReload() {
        AtomicInteger listenerCreations = new AtomicInteger();
        AtomicInteger reloads = new AtomicInteger();
        List<Runnable> registeredListeners = new ArrayList<>();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            listenerCreations.incrementAndGet();
            return reloads::incrementAndGet;
        });

        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FORGE, registeredListeners::add));
        assertFalse(registration.register(FancyMenuResourceReload.ClientLoader.FORGE, registeredListeners::add));
        assertEquals(1, listenerCreations.get());
        assertEquals(1, registeredListeners.size());
        registeredListeners.get(0).run();
        assertEquals(1, reloads.get());
    }

    @Test
    void eitherLoaderCanOwnAnIsolatedRegistration() {
        for (FancyMenuResourceReload.ClientLoader loader : FancyMenuResourceReload.ClientLoader.values()) {
            AtomicInteger registrations = new AtomicInteger();
            ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
            assertTrue(registration.register(loader, listener -> registrations.incrementAndGet()));
            assertEquals(1, registrations.get());
        }
    }

    @Test
    void aDifferentLoaderCannotReplaceTheRegisteredOwner() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FORGE, listener -> {}));
    }

    @Test
    void registrarFailureLeavesRegistrationRetryable() {
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        Consumer<Runnable> failingRegistrar = listener -> {
            throw new IllegalStateException("not ready");
        };
        assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FORGE, failingRegistrar));
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FORGE, listener -> registrations.incrementAndGet()));
        assertEquals(1, registrations.get());
    }

    @Test
    void reentrantRegistrarCannotCreateASecondListener() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, nested -> {}))));
        assertFalse(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> {}));
    }

    @Test
    void nullDependenciesAreRejectedWithoutChangingState() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});
        assertThrows(NullPointerException.class, () -> registration.register(null, listener -> {}));
        assertThrows(NullPointerException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, null));
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(NullPointerException.class, () -> new ClientReloadListenerRegistration<Runnable>(null));
    }
}
