package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import net.minecraft.client.gui.GuiGraphics;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiPWindowHandler {

    private static final List<PiPWindow> WINDOWS = new ArrayList<>();
    @Nullable
    private static PiPWindow focusedWindow;
    @Nullable
    private static PiPWindow activePointerWindow;
    private static int activePointerButton = -1;
    private static boolean windowClickedThisTick = false;
    @Nullable
    private static PiPWindow lastClickedWindowThisTick;
    private static boolean isRendering = false;
    @Nullable
    private static PiPWindow activeScreenRenderWindow;

    public static void openWindow(@Nonnull PiPWindow window) {
        if (WINDOWS.contains(window)) {
            bringToFront(window);
            return;
        }
        WINDOWS.add(window);
        window.addCloseCallback(() -> closeWindow(window));
        bringToFront(window);
    }

    public static void openChildWindow(@Nonnull PiPWindow parent, @Nonnull PiPWindow child) {
        parent.registerChildWindow(child);
        openWindow(child);
    }

    public static void closeWindow(@Nonnull PiPWindow window) {
        if (!WINDOWS.remove(window)) {
            return;
        }
        window.handleClosed();
        PiPWindow parent = window.getParentWindow();
        if (parent != null) {
            parent.unregisterChildWindow(window);
        }
        if (focusedWindow == window) {
            focusedWindow = null;
        }
        if (activePointerWindow == window) {
            activePointerWindow = null;
            activePointerButton = -1;
        }
    }

    public static void closeAllWindows() {
        List<PiPWindow> copy = new ArrayList<>(WINDOWS);
        WINDOWS.clear();
        for (PiPWindow window : copy) {
            window.handleClosed();
            PiPWindow parent = window.getParentWindow();
            if (parent != null) {
                parent.unregisterChildWindow(window);
            }
        }
        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
    }

    public static List<PiPWindow> getOpenWindows() {
        return Collections.unmodifiableList(WINDOWS);
    }

    public static boolean isAnyWindowOpen() {
        return !WINDOWS.isEmpty();
    }

    public static void bringToFront(@Nonnull PiPWindow window) {
        if (WINDOWS.remove(window)) {
            WINDOWS.add(window);
        }
        focusedWindow = window;
    }

    public static void renderAll(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        isRendering = true;
        try {
            for (PiPWindow window : new ArrayList<>(WINDOWS)) {
                window.render(graphics, mouseX, mouseY, partial);
            }
        } finally {
            isRendering = false;
        }
    }

    public static void tickAll() {
        windowClickedThisTick = false;
        lastClickedWindowThisTick = null;
        for (PiPWindow window : new ArrayList<>(WINDOWS)) {
            window.tick();
        }
    }

    public static void mouseMoved(double mouseX, double mouseY) {
        for (PiPWindow window : new ArrayList<>(WINDOWS)) {
            window.mouseMoved(mouseX, mouseY);
        }
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<PiPWindow> snapshot = new ArrayList<>(WINDOWS);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            PiPWindow window = snapshot.get(i);
            if (!window.isVisible()) {
                continue;
            }
            if (!window.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            windowClickedThisTick = true;
            lastClickedWindowThisTick = window;
            if (window.isInputLocked()) {
                bringToFront(window);
                focusedWindow = window;
                activePointerWindow = window;
                activePointerButton = button;
                return true;
            }
            boolean allowScreenInput = window == focusedWindow;
            if (allowScreenInput) {
                window.mouseClicked(mouseX, mouseY, button);
            } else {
                window.mouseClickedWithoutScreen(mouseX, mouseY, button);
            }
            if (WINDOWS.contains(window)) {
                bringToFront(window);
                focusedWindow = window;
                activePointerWindow = window;
                activePointerButton = button;
            } else {
                focusedWindow = null;
                activePointerWindow = null;
                activePointerButton = -1;
            }
            return true;
        }

        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
        return false;
    }

    public static boolean wasWindowClickedThisTick() {
        return windowClickedThisTick;
    }

    @Nullable
    public static PiPWindow getLastClickedWindowThisTick() {
        return lastClickedWindowThisTick;
    }

    public static boolean isRendering() {
        return isRendering;
    }

    public static void beginScreenRender(@Nonnull PiPWindow window) {
        activeScreenRenderWindow = window;
    }

    public static void endScreenRender(@Nonnull PiPWindow window) {
        if (activeScreenRenderWindow == window) {
            activeScreenRenderWindow = null;
        }
    }

    public static int getActiveScreenRenderOffsetX() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyX() : 0;
    }

    public static int getActiveScreenRenderOffsetY() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyY() : 0;
    }

    public static boolean isScreenRenderActive() {
        return activeScreenRenderWindow != null;
    }

    public static boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseReleased(mouseX, mouseY, button);
            if (button == activePointerButton) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
            return handled;
        }
        return false;
    }

    public static boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activePointerWindow != null) {
            return activePointerWindow.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        List<PiPWindow> snapshot = new ArrayList<>(WINDOWS);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            PiPWindow window = snapshot.get(i);
            if (!window.isVisible() || !window.isMouseOver(mouseX, mouseY)) {
                continue;
            }
            if (window.isInputLocked()) {
                return true;
            }
            return window.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
        }
        return false;
    }

    public static boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            return window.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public static boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            return window.keyReleased(keyCode, scanCode, modifiers);
        }
        return false;
    }

    public static boolean charTyped(char codePoint, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            return window.charTyped(codePoint, modifiers);
        }
        return false;
    }

    @Nullable
    private static PiPWindow getFocusedWindow() {
        if (focusedWindow != null && focusedWindow.isVisible() && !focusedWindow.isInputLocked()) {
            return focusedWindow;
        }
        for (int i = WINDOWS.size() - 1; i >= 0; i--) {
            PiPWindow window = WINDOWS.get(i);
            if (window.isVisible() && !window.isInputLocked()) {
                focusedWindow = window;
                return window;
            }
        }
        return null;
    }

    public static boolean isWindowFocused(@Nonnull PiPWindow window) {
        return focusedWindow == window;
    }
}
