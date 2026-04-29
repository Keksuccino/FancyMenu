package de.keksuccino.fancymenu.util.input;

import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

public final class ClicksPerSecondTracker {

    private static final long TIME_WINDOW_MS_FANCYMENU = 1000L;
    private static final Object LOCK_FANCYMENU = new Object();
    private static final ArrayDeque<Long> LEFT_CLICKS_FANCYMENU = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT_CLICKS_FANCYMENU = new ArrayDeque<>();

    private ClicksPerSecondTracker() {
    }

    public static void recordClick(int mouseButton) {
        long now = Util.getMillis();

        synchronized (LOCK_FANCYMENU) {
            pruneOldClicks_FancyMenu(LEFT_CLICKS_FANCYMENU, now);
            pruneOldClicks_FancyMenu(RIGHT_CLICKS_FANCYMENU, now);

            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                LEFT_CLICKS_FANCYMENU.addLast(now);
            } else if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                RIGHT_CLICKS_FANCYMENU.addLast(now);
            }
        }
    }

    public static int getClicksPerSecond(boolean rightMouseButton) {
        long now = Util.getMillis();

        synchronized (LOCK_FANCYMENU) {
            ArrayDeque<Long> target = rightMouseButton ? RIGHT_CLICKS_FANCYMENU : LEFT_CLICKS_FANCYMENU;
            pruneOldClicks_FancyMenu(target, now);
            return target.size();
        }
    }

    private static void pruneOldClicks_FancyMenu(ArrayDeque<Long> clicks, long now) {
        long threshold = now - TIME_WINDOW_MS_FANCYMENU;
        while (!clicks.isEmpty()) {
            Long first = clicks.peekFirst();
            if (first == null || first >= threshold) {
                return;
            }
            clicks.removeFirst();
        }
    }
}
