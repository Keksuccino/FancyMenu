package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.helpers.ListenerPathFormatter;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.List;
import java.util.function.Function;

public class OnScreenshotTakenListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    private final Function<File, String> pathFormatter;
    protected @Nullable String screenshotPath;

    public OnScreenshotTakenListener() {
        this(file -> ListenerPathFormatter.toFriendlyPath(file.getAbsolutePath()));
    }

    OnScreenshotTakenListener(@NotNull Function<File, String> pathFormatter) {
        super("screenshot_taken");
        this.pathFormatter = pathFormatter;
    }

    /**
     * Gets called by the IO thread that saved the screenshot file, so the event gets deferred to the main thread.<br>
     * Never throws, because the caller runs inside Minecraft's screenshot saving code.
     */
    public void onScreenshotSaved(@NotNull File screenshotFile) {
        long listenerRevision = this.getActiveInstanceRevision();
        if (listenerRevision < 0L) {
            return;
        }
        try {
            String path = this.pathFormatter.apply(screenshotFile);
            MainThreadTaskExecutor.executeInMainThread(() -> { if (this.isActiveAtRevision(listenerRevision)) this.onScreenshotTaken(path); }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to handle saved screenshot for the 'On Screenshot Taken' listener: " + screenshotFile, ex);
        }
    }

    public void onScreenshotTaken(@NotNull String screenshotPath) {
        if (!this.hasInstancesListening()) {
            return;
        }
        this.screenshotPath = screenshotPath;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("screenshot_path", () -> (this.screenshotPath != null) ? this.screenshotPath : ""));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_screenshot_taken");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_screenshot_taken.desc"));
    }

}
