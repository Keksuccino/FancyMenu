package de.keksuccino.fancymenu.util.lifecycle;

import de.keksuccino.fancymenu.customization.background.backgrounds.video.VideoBackgroundTaskController;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.fancymenu.customization.remote.RemoteServerConnectionManager;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.TaskExecutor;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.rinku.ActionBridge;
import de.keksuccino.fancymenu.util.rinku.BrowserHandler;
import de.keksuccino.fancymenu.util.rinku.RinkuExecutors;
import de.keksuccino.fancymenu.util.rinku.RinkuUtil;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.rendering.video.rinku.RinkuVideoManager;
import de.keksuccino.fancymenu.util.resource.resources.texture.TextureManagerReleaseDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/** Releases FancyMenu resources before vanilla tears down the client rendering infrastructure. */
public final class ClientShutdownHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean SHUTDOWN_STARTED = new AtomicBoolean();

    private ClientShutdownHandler() {
    }

    public static boolean isShuttingDown() {
        return SHUTDOWN_STARTED.get();
    }

    public static void shutdown() {
        if (!SHUTDOWN_STARTED.compareAndSet(false, true)) return;
        // Remote callbacks can enqueue main-thread listener work, so quiesce both of their owned worker pools first.
        runCleanup("remote server connections", RemoteServerConnectionManager::shutdown);
        runCleanup("internet availability monitor", WebUtils::shutdown);
        runCleanup("screen audio playback", ScreenCustomizationLayerHandler::shutdown);
        runCleanup("layouts", LayoutHandler::shutdown);
        runCleanup("video background scheduler", VideoBackgroundTaskController::shutdownSharedExecutor);
        runCleanup("scheduled tasks", TaskExecutor::shutdown);
        runCleanup("user variables", VariableHandler::shutdown);
        boolean rinkuPresent = isRinkuPresentSafely();
        if (rinkuPresent) {
            runCleanup("Rinku video players", () -> RinkuVideoManager.getInstance().disposeAll());
            runCleanup("Rinku browsers", BrowserHandler::closeAll);
        }
        runCleanup("panorama renderers", PanoramaHandler::shutdown);
        // GLFW cursor destruction must finish on the render thread while the window and GLFW are still alive.
        runCleanup("GLFW cursors", CursorHandler::shutdown);
        runCleanup("pending texture-manager releases", TextureManagerReleaseDispatcher::flushPendingReleases);
        if (rinkuPresent) {
            runCleanup("Rinku action bridge", ActionBridge::dispose);
        }
        runCleanup("Rinku executors", RinkuExecutors::shutdownAll);
    }

    private static boolean isRinkuPresentSafely() {
        try {
            return RinkuUtil.isRinkuPresent();
        } catch (Throwable throwable) {
            LOGGER.error("[FANCYMENU] Failed to check Rinku presence during client shutdown!", throwable);
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
