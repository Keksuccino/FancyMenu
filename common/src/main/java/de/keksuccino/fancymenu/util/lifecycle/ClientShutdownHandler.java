package de.keksuccino.fancymenu.util.lifecycle;

import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.server.ServerCache;
import de.keksuccino.fancymenu.util.mcef.ActionBridge;
import de.keksuccino.fancymenu.util.mcef.BrowserHandler;
import de.keksuccino.fancymenu.util.mcef.MCEFUtil;
import de.keksuccino.fancymenu.util.rendering.video.mcef.MCEFVideoManager;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.watermedia.WatermediaDeferredPlayerReleaseTracker;
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

        try {
            runCleanup("server cache", ServerCache::shutdown);
            // Stop recurring work before any resource it can touch is disposed. All managed workers are daemons as a final fallback.
            runCleanup("FancyMenu executors", FancyMenuExecutors::shutdownAll);
            runCleanup("deferred Watermedia players", WatermediaDeferredPlayerReleaseTracker::shutdown);
            runCleanup("main-thread task queue", MainThreadTaskExecutor::shutdown);
            boolean mcefPresent = isMCEFPresentSafely();
            if (mcefPresent) {
                runCleanup("MCEF video players", () -> MCEFVideoManager.getInstance().disposeAll());
                runCleanup("MCEF browsers", BrowserHandler::closeAll);
            }
            runCleanup("panorama renderers", PanoramaHandler::shutdown);
            runCleanup("resources", ResourceHandlers::shutdownAll);
            if (mcefPresent) {
                runCleanup("MCEF action bridge", ActionBridge::dispose);
            }
        } finally {
            runCleanup("FancyMenu executors", FancyMenuExecutors::shutdownAll);
        }
    }

    private static boolean isMCEFPresentSafely() {
        try {
            return MCEFUtil.isMCEFPresent();
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] Failed to check MCEF presence during client shutdown!", throwable);
            return false;
        }
    }

    private static void runCleanup(String name, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] Failed to clean up {} during client shutdown!", name, throwable);
        }
    }

}
