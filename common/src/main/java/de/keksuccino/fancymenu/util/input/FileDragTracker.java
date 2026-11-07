package de.keksuccino.fancymenu.util.input;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps track of files that get dragged over the game window from outside, before they get dropped.<br>
 * Only window backends that report drags before the drop (SDL) feed this, so with others it never reports a drag.<br><br>
 *
 * The window backend can call this from its event thread, so all state is volatile.
 */
public final class FileDragTracker {

    private static volatile boolean dragging = false;
    @Nullable
    private static volatile Position lastWindowPosition = null;

    private FileDragTracker() {
    }

    /**
     * Gets called when a file drag enters the game window.
     */
    public static void onDragBegin() {
        lastWindowPosition = null;
        dragging = true;
    }

    /**
     * Gets called when a file drag moves over the game window.
     *
     * @param windowX The X position in window coordinates, like the cursor position.
     * @param windowY The Y position in window coordinates, like the cursor position.
     */
    public static void onDragPosition(double windowX, double windowY) {
        lastWindowPosition = new Position(windowX, windowY);
    }

    /**
     * Gets called when the drag leaves the game window or its files got dropped.<br>
     * The last position stays known, because the drop gets handled after this and needs it.
     */
    public static void onDragEnd() {
        dragging = false;
    }

    /**
     * @return TRUE while files get dragged over the game window.
     */
    public static boolean isDragging() {
        return dragging;
    }

    /**
     * @return The last known position of the current or last drag in GUI coordinates, or NULL if the window backend didn't report one.
     */
    @Nullable
    public static Position getLastGuiPosition() {
        Position position = lastWindowPosition;
        if (position == null) return null;
        Window window = Minecraft.getInstance().getWindow();
        if ((window.getScreenWidth() <= 0) || (window.getScreenHeight() <= 0)) return null;
        return new Position(position.x() * window.getGuiScaledWidth() / window.getScreenWidth(), position.y() * window.getGuiScaledHeight() / window.getScreenHeight());
    }

    public record Position(double x, double y) {
    }

}
