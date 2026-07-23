package de.keksuccino.fancymenu.util.reload;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ClientReloadListenerRegistration<L> {

    private final Supplier<? extends L> listenerFactory;
    @Nullable private FancyMenuResourceReload.ClientLoader registeredLoader;
    @Nullable private FancyMenuResourceReload.ClientLoader registeringLoader;

    ClientReloadListenerRegistration(@NotNull Supplier<? extends L> listenerFactory) {
        this.listenerFactory = Objects.requireNonNull(listenerFactory, "listenerFactory");
    }

    synchronized boolean register(@NotNull FancyMenuResourceReload.ClientLoader loader, @NotNull Consumer<? super L> registrar) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(registrar, "registrar");

        if (this.registeredLoader == loader) {
            return false;
        }
        if (this.registeredLoader != null) {
            throw new IllegalStateException("FancyMenu's client reload listener is already registered by " + this.registeredLoader + ", so " + loader + " cannot register it again");
        }
        // The loader registrar is a synchronous registry mutation. Keep it inside the monitor so ownership is atomic across callers,
        // and guard the monitor's same-thread reentrancy so a registrar cannot recursively create a second listener before the commit.
        if (this.registeringLoader != null) {
            throw new IllegalStateException("FancyMenu's client reload listener registration is already in progress for " + this.registeringLoader);
        }

        this.registeringLoader = loader;
        try {
            L listener = Objects.requireNonNull(this.listenerFactory.get(), "listenerFactory result");
            registrar.accept(listener);
            this.registeredLoader = loader;
            return true;
        } finally {
            this.registeringLoader = null;
        }
    }

}
