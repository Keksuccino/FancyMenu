package de.keksuccino.fancymenu.util.lifecycle;

import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.threading.FancyMenuExecutors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/** Releases the asynchronous and GPU resources introduced by target-applicable lifecycle backports. */
public final class ClientShutdownHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean();

    private ClientShutdownHandler() {}

    public static void shutdown() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) return;

        try {
            runCleanup("internet availability monitor", WebUtils::shutdown);
            runCleanup("user variables", VariableHandler::shutdown);
            runCleanup("panorama renderers", PanoramaHandler::shutdown);
        } finally {
            runCleanup("FancyMenu executors", FancyMenuExecutors::shutdownAll);
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
