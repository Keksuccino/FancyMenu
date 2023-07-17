package de.keksuccino.fancymenu.util.rendering.ui.cursor;

import de.keksuccino.fancymenu.events.ticking.ClientTickEvent;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class CursorHandler {

    public static final long CURSOR_RESIZE_HORIZONTAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_EW_CURSOR);
    public static final long CURSOR_RESIZE_VERTICAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_NS_CURSOR);
    public static final long CURSOR_RESIZE_ALL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_RESIZE_ALL_CURSOR);
    public static final long CURSOR_WRITING = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
    public static final long CURSOR_POINTING_HAND = GLFW.glfwCreateStandardCursor(GLFW.GLFW_POINTING_HAND_CURSOR);

    private static final long CURSOR_NORMAL = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);

    private static long clientTickCursor = -2;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventHandler.INSTANCE.registerListenersOf(new CursorHandler());
    }

    /**
     * Cursor gets reset every tick, so only set non-default cursors here.
     */
    public static void setClientTickCursor(long cursor) {
        clientTickCursor = cursor;
    }

    private static void setCursor(long cursor) {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), cursor);
    }

    @EventListener
    public void onClientTickPre(ClientTickEvent.Pre e) {

        if ((clientTickCursor != -1) && (clientTickCursor != -2)) {
            //Set the non-default cursor set by the mod
            setCursor(clientTickCursor);
            clientTickCursor = -1;
        } else if (clientTickCursor == -1) {
            //Reset the cursor to default, if no custom cursor was set last tick
            setCursor(CURSOR_NORMAL);
            clientTickCursor = -2;
        }

    }

}
