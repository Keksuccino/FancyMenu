package de.keksuccino.fancymenu.util;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MouseUtil {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<Long, MouseButtonListener> CLICK_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseButtonListener> RELEASE_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseMoveListener> MOVE_LISTENERS = new LinkedHashMap<>();
    private static final Map<Long, MouseDragListener> DRAG_LISTENERS = new LinkedHashMap<>();

    private static long listenerId = 0L;
    private static boolean mouseStateInitialized = false;
    private static double lastMouseX = 0D;
    private static double lastMouseY = 0D;

    public static void tick() {
        double mouseX = getGuiScaledMouseX();
        double mouseY = getGuiScaledMouseY();

        if (!mouseStateInitialized) {
            mouseStateInitialized = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return;
        }

        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;

        if ((deltaX != 0D) || (deltaY != 0D)) {
            for (MouseMoveListener listener : new ArrayList<>(MOVE_LISTENERS.values())) {
                listener.onMouseMoved(mouseX, mouseY, deltaX, deltaY);
            }
            if (isLeftMouseDown()) {
                for (MouseDragListener listener : new ArrayList<>(DRAG_LISTENERS.values())) {
                    listener.onMouseDragged(GLFW.GLFW_MOUSE_BUTTON_LEFT, mouseX, mouseY, deltaX, deltaY);
                }
            }
            if (isRightMouseDown()) {
                for (MouseDragListener listener : new ArrayList<>(DRAG_LISTENERS.values())) {
                    listener.onMouseDragged(GLFW.GLFW_MOUSE_BUTTON_RIGHT, mouseX, mouseY, deltaX, deltaY);
                }
            }
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public static long addClickListener(@NotNull MouseButtonListener listener) {
        listenerId++;
        CLICK_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    public static long addReleaseListener(@NotNull MouseButtonListener listener) {
        listenerId++;
        RELEASE_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    public static long addMoveListener(@NotNull MouseMoveListener listener) {
        listenerId++;
        MOVE_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    public static long addDragListener(@NotNull MouseDragListener listener) {
        listenerId++;
        DRAG_LISTENERS.put(listenerId, listener);
        return listenerId;
    }

    public static boolean overrideClickListener(long id, @NotNull MouseButtonListener listener) {
        if (!CLICK_LISTENERS.containsKey(id)) {
            return false;
        }
        CLICK_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean overrideReleaseListener(long id, @NotNull MouseButtonListener listener) {
        if (!RELEASE_LISTENERS.containsKey(id)) {
            return false;
        }
        RELEASE_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean overrideMoveListener(long id, @NotNull MouseMoveListener listener) {
        if (!MOVE_LISTENERS.containsKey(id)) {
            return false;
        }
        MOVE_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean overrideDragListener(long id, @NotNull MouseDragListener listener) {
        if (!DRAG_LISTENERS.containsKey(id)) {
            return false;
        }
        DRAG_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean overrideListener(long id, @NotNull MouseButtonListener listener) {
        if (CLICK_LISTENERS.containsKey(id)) {
            CLICK_LISTENERS.put(id, listener);
            return true;
        }
        if (RELEASE_LISTENERS.containsKey(id)) {
            RELEASE_LISTENERS.put(id, listener);
            return true;
        }
        return false;
    }

    public static boolean overrideListener(long id, @NotNull MouseMoveListener listener) {
        if (!MOVE_LISTENERS.containsKey(id)) {
            return false;
        }
        MOVE_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean overrideListener(long id, @NotNull MouseDragListener listener) {
        if (!DRAG_LISTENERS.containsKey(id)) {
            return false;
        }
        DRAG_LISTENERS.put(id, listener);
        return true;
    }

    public static boolean removeListener(long id) {
        if (CLICK_LISTENERS.remove(id) != null) {
            return true;
        }
        if (RELEASE_LISTENERS.remove(id) != null) {
            return true;
        }
        if (MOVE_LISTENERS.remove(id) != null) {
            return true;
        }
        return DRAG_LISTENERS.remove(id) != null;
    }

    public static void clearListeners() {
        CLICK_LISTENERS.clear();
        RELEASE_LISTENERS.clear();
        MOVE_LISTENERS.clear();
        DRAG_LISTENERS.clear();
    }

    public static void onMouseButtonPressed(int button, double mouseX, double mouseY) {
        MouseButton mouseButton = MouseButton.fromGlfwButton(button);
        for (MouseButtonListener listener : new ArrayList<>(CLICK_LISTENERS.values())) {
            listener.onMouseButton(mouseButton, mouseX, mouseY);
        }
    }

    public static void onMouseButtonReleased(int button, double mouseX, double mouseY) {
        MouseButton mouseButton = MouseButton.fromGlfwButton(button);
        for (MouseButtonListener listener : new ArrayList<>(RELEASE_LISTENERS.values())) {
            listener.onMouseButton(mouseButton, mouseX, mouseY);
        }
    }

    public static boolean isMouseGrabbed() {
        return MC.mouseHandler.isMouseGrabbed();
    }

    public static boolean isLeftMouseDown() {
        return MC.mouseHandler.isLeftPressed();
    }

    public static boolean isRightMouseDown() {
        return MC.mouseHandler.isRightPressed();
    }

    public static double getGuiScaledMouseX() {
        return getMouseX() * (double) MC.getWindow().getGuiScaledWidth() / (double) MC.getWindow().getScreenWidth();
    }

    public static double getGuiScaledMouseY() {
        return getMouseY() * (double) MC.getWindow().getGuiScaledHeight() / (double) MC.getWindow().getScreenHeight();
    }

    public static double getMouseX() {
        return MC.mouseHandler.xpos();
    }

    public static double getMouseY() {
        return MC.mouseHandler.ypos();
    }

    public enum MouseButton {
        LEFT(GLFW.GLFW_MOUSE_BUTTON_LEFT),
        RIGHT(GLFW.GLFW_MOUSE_BUTTON_RIGHT),
        MIDDLE(GLFW.GLFW_MOUSE_BUTTON_MIDDLE),
        OTHER(-1);

        private final int glfwButton;

        MouseButton(int glfwButton) {
            this.glfwButton = glfwButton;
        }

        public int getGlfwButton() {
            return this.glfwButton;
        }

        public static @NotNull MouseButton fromGlfwButton(int button) {
            return switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> LEFT;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> RIGHT;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MIDDLE;
                default -> OTHER;
            };
        }
    }

    @FunctionalInterface
    public interface MouseButtonListener {
        void onMouseButton(@NotNull MouseButton button, double mouseX, double mouseY);
    }

    @FunctionalInterface
    public interface MouseMoveListener {
        void onMouseMoved(double mouseX, double mouseY, double deltaX, double deltaY);
    }

    @FunctionalInterface
    public interface MouseDragListener {
        void onMouseDragged(int button, double mouseX, double mouseY, double deltaX, double deltaY);
    }

}
