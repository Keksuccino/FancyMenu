package de.keksuccino.fancymenu.util.mcp;

import de.keksuccino.fancymenu.FancyMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FancyMenuMcpManager {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final int DEFAULT_PORT = 48561;

    private static FancyMenuMcpServer server;
    private static int activeConfiguredPort = -1;

    private FancyMenuMcpManager() {
    }

    public static synchronized void syncWithOptions() {
        boolean enabled = FancyMenu.getOptions().mcpServerEnabled.getValue();
        if (enabled) {
            int configuredPort = sanitizePort(FancyMenu.getOptions().mcpServerPort.getValue());
            if (server != null && server.isRunning() && configuredPort != activeConfiguredPort) {
                stop();
            }
            start();
        } else {
            stop();
        }
    }

    public static synchronized boolean start() {
        if (server != null && server.isRunning()) {
            return true;
        }
        int configuredPort = FancyMenu.getOptions().mcpServerPort.getValue();
        int port = sanitizePort(configuredPort);
        try {
            FancyMenuMcpServer newServer = new FancyMenuMcpServer(port);
            newServer.start();
            server = newServer;
            activeConfiguredPort = port;
            LOGGER.info("[FANCYMENU MCP] Started server on port {}.", newServer.getBoundPort());
            return true;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU MCP] Failed to start MCP server.", ex);
            return false;
        }
    }

    public static synchronized void stop() {
        FancyMenuMcpActionEditorBridge.closeActive();
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU MCP] Failed to stop MCP server cleanly.", ex);
            }
            server = null;
        }
        activeConfiguredPort = -1;
    }

    public static synchronized boolean isRunning() {
        return server != null && server.isRunning();
    }

    public static synchronized int getBoundPort() {
        return (server != null) ? server.getBoundPort() : -1;
    }

    public static synchronized void onGameShutdown() {
        stop();
    }

    private static int sanitizePort(int configuredPort) {
        if (configuredPort < 1 || configuredPort > 65535) {
            FancyMenu.getOptions().mcpServerPort.setValue(DEFAULT_PORT);
            return DEFAULT_PORT;
        }
        return configuredPort;
    }
}
