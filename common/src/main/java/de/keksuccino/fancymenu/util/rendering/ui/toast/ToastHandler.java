package de.keksuccino.fancymenu.util.rendering.ui.toast;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ToastHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void showToast(@NotNull final SimpleToast toast, final long durationMs) {
        final long start = System.currentTimeMillis();
        Minecraft.getInstance().getToasts().addToast(toast);
        new Thread(() -> {
            try {
                while (true) {
                    long now = System.currentTimeMillis();
                    if ((start + durationMs) < now) {
                        toast.hide();
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (Exception ex) {
                try {
                    toast.hide();
                } catch (Exception ignore) {}
                LOGGER.error("[FANCYMENU] Error in timer thread of SimpleToast in ToastHandler!", ex);
            }
        }).start();
    }

}
