package de.keksuccino.fancymenu.util.mcp;

import org.jetbrains.annotations.NotNull;

import java.net.SocketAddress;

final class FancyMenuMcpSession {

    private final @NotNull SocketAddress remoteAddress;
    private volatile boolean initializeCalled;
    private volatile boolean introCalled;
    private volatile boolean initialized;

    FancyMenuMcpSession(@NotNull SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @NotNull SocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    boolean isInitializeCalled() {
        return this.initializeCalled;
    }

    void setInitializeCalled(boolean initializeCalled) {
        this.initializeCalled = initializeCalled;
    }

    boolean isIntroCalled() {
        return this.introCalled;
    }

    void setIntroCalled(boolean introCalled) {
        this.introCalled = introCalled;
    }

    boolean isInitialized() {
        return this.initialized;
    }

    void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
