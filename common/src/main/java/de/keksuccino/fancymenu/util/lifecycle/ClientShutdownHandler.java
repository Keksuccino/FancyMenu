package de.keksuccino.fancymenu.util.lifecycle;

import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.resources.texture.TextureManagerReleaseDispatcher;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Releases FancyMenu resources before vanilla tears down the client resource and rendering infrastructure.
 * The atomic guard keeps duplicate client-close paths from running the shutdown sequence more than once.
 */
public final class ClientShutdownHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean();

    private ClientShutdownHandler() {}

    public static boolean isShuttingDown() {
        return SHUTDOWN_STARTED.get();
    }

    public static void shutdown() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) return;

        runCleanup("internet availability monitor", WebUtils::shutdown);
        runCleanup("user variables", VariableHandler::shutdown);
        runCleanup("panorama renderers", PanoramaHandler::shutdown);
        // GLFW cursor destruction must finish on the render thread while the window and GLFW are still alive.
        runCleanup("GLFW cursors", CursorHandler::shutdown);
        runCleanup("pending texture-manager releases", TextureManagerReleaseDispatcher::flushPendingReleases);
    }

    private static void runCleanup(String name, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] Failed to clean up {} during client shutdown!", name, throwable);
        }
    }

}
