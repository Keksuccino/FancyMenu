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
    void twoNeoForgeRegistrationAttemptsStillInvokeTheExpensiveSequenceOncePerReload() {
        AtomicInteger listenerCreations = new AtomicInteger();
        AtomicInteger expensiveSequenceInvocations = new AtomicInteger();
        List<Runnable> registeredListeners = new ArrayList<>();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            listenerCreations.incrementAndGet();
            return expensiveSequenceInvocations::incrementAndGet;
        });

        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.NEOFORGE, registeredListeners::add));
        assertFalse(registration.register(FancyMenuResourceReload.ClientLoader.NEOFORGE, registeredListeners::add));
        assertEquals(1, listenerCreations.get());
        assertEquals(1, registeredListeners.size());

        registeredListeners.getFirst().run();
        assertEquals(1, expensiveSequenceInvocations.get());
        registeredListeners.getFirst().run();
        assertEquals(2, expensiveSequenceInvocations.get());
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
        AtomicInteger fabricRegistrations = new AtomicInteger();
        AtomicInteger neoForgeRegistrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});

        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> fabricRegistrations.incrementAndGet()));
        assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.NEOFORGE, listener -> neoForgeRegistrations.incrementAndGet()));
        assertEquals(1, fabricRegistrations.get());
        assertEquals(0, neoForgeRegistrations.get());
    }

    @Test
    void registrarFailureLeavesRegistrationRetryable() {
        AtomicInteger listenerCreations = new AtomicInteger();
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            listenerCreations.incrementAndGet();
            return () -> {};
        });
        Consumer<Runnable> failingRegistrar = listener -> {
            throw new IllegalStateException("not ready");
        };

        assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.NEOFORGE, failingRegistrar));
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.NEOFORGE, listener -> registrations.incrementAndGet()));

        assertEquals(2, listenerCreations.get());
        assertEquals(1, registrations.get());
    }

    @Test
    void reentrantRegistrarCannotCreateASecondListenerBeforeOwnershipIsCommitted() {
        AtomicInteger listenerCreations = new AtomicInteger();
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            listenerCreations.incrementAndGet();
            return () -> {};
        });
        Consumer<Runnable> reentrantRegistrar = listener -> {
            registrations.incrementAndGet();
            assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, nestedListener -> registrations.incrementAndGet()));
        };

        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, reentrantRegistrar));
        assertFalse(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> registrations.incrementAndGet()));

        assertEquals(1, listenerCreations.get());
        assertEquals(1, registrations.get());
    }

    @Test
    void listenerFactoryFailureLeavesRegistrationRetryable() {
        AtomicInteger factoryAttempts = new AtomicInteger();
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> {
            if (factoryAttempts.incrementAndGet() == 1) throw new IllegalStateException("not ready");
            return () -> {};
        });

        assertThrows(IllegalStateException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> registrations.incrementAndGet()));
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> registrations.incrementAndGet()));

        assertEquals(2, factoryAttempts.get());
        assertEquals(1, registrations.get());
    }

    @Test
    void listenerFactoryCannotReturnNull() {
        AtomicInteger registrations = new AtomicInteger();
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> null);

        assertThrows(NullPointerException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> registrations.incrementAndGet()));
        assertThrows(NullPointerException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> registrations.incrementAndGet()));
        assertEquals(0, registrations.get());
    }

    @Test
    void nullDependenciesAreRejectedWithoutChangingRegistrationState() {
        ClientReloadListenerRegistration<Runnable> registration = new ClientReloadListenerRegistration<>(() -> () -> {});

        assertThrows(NullPointerException.class, () -> registration.register(null, listener -> {}));
        assertThrows(NullPointerException.class, () -> registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, null));
        assertTrue(registration.register(FancyMenuResourceReload.ClientLoader.FABRIC, listener -> {}));
        assertThrows(NullPointerException.class, () -> new ClientReloadListenerRegistration<Runnable>(null));
    }

}
