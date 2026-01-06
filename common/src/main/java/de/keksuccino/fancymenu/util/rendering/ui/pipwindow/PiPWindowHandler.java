package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiPWindowHandler implements GuiEventListener, Tickable, Renderable {

    public static final PiPWindowHandler INSTANCE = new PiPWindowHandler();
    private static final double DEFAULT_WINDOW_SIZE_SCALE_WIDTH = 0.4;
    private static final double DEFAULT_WINDOW_SIZE_SCALE_HEIGHT = 0.5;

    private final List<PiPWindow> windows = new ArrayList<>();
    @Nullable
    private PiPWindow focusedWindow;
    @Nullable
    private PiPWindow activePointerWindow;
    private int activePointerButton = -1;
    private boolean windowClickedThisTick = false;
    @Nullable
    private PiPWindow lastClickedWindowThisTick;
    private boolean isRendering = false;
    @Nullable
    private PiPWindow activeScreenRenderWindow;
    private double activeScreenRenderScaleFactor = 1.0;

    private PiPWindowHandler() {
    }

    public void openWindow(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        if (parentWindow != null) {
            parentWindow.registerChildWindow(window);
        }
        if (windows.contains(window)) {
            bringToFront(window);
            return;
        }
        windows.add(window);
        window.addCloseCallback(() -> closeWindow(window));
        bringToFront(window);
    }

    public void openWindowCentered(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        window.setPosition(x, y);
        openWindow(window, parentWindow);
    }

    public void openWindowWithDefaultSizeAndPosition(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int targetWidth = Math.max(1, (int) Math.round(screenWidth * DEFAULT_WINDOW_SIZE_SCALE_WIDTH));
        int targetHeight = Math.max(1, (int) Math.round(screenHeight * DEFAULT_WINDOW_SIZE_SCALE_HEIGHT));
        double guiScale = window.isSizeScaledToGuiScale() ? minecraft.getWindow().getGuiScale() : 1.0;
        if (guiScale <= 1.0) {
            guiScale = 1.0;
        }
        int rawWidth = guiScale > 1.0 ? (int) Math.ceil(targetWidth * guiScale) : targetWidth;
        int rawHeight = guiScale > 1.0 ? (int) Math.ceil(targetHeight * guiScale) : targetHeight;
        int x = (screenWidth - targetWidth) / 2;
        int y = (screenHeight - targetHeight) / 2;
        window.setBounds(x, y, rawWidth, rawHeight);
        openWindow(window, parentWindow);
    }

    public void closeWindow(@NotNull PiPWindow window) {
        boolean closedByScreen = window.consumeClosingFromScreen();
        if (!closedByScreen) {
            var screen = window.getScreen();
            if (screen instanceof PipableScreen pipableScreen) {
                pipableScreen.onWindowClosedExternally();
            }
        }
        if (window.shouldCloseScreenWithWindow()) {
            window.setScreen(null);
        }
        if (!windows.remove(window)) {
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

    public void closeAllWindows() {
        List<PiPWindow> copy = new ArrayList<>(windows);
        for (PiPWindow window : copy) {
            closeWindow(window);
        }
        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
    }

    public List<PiPWindow> getOpenWindows() {
        return Collections.unmodifiableList(windows);
    }

    public boolean isAnyWindowOpen() {
        return !windows.isEmpty();
    }

    public void refreshAllScreens() {
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.refreshScreen();
        }
    }

    public void bringToFront(@NotNull PiPWindow window) {
        if (windows.remove(window)) {
            windows.add(window);
        }
        focusedWindow = window;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        isRendering = true;
        try {
            for (PiPWindow window : new ArrayList<>(windows)) {
                window.render(graphics, mouseX, mouseY, partial);
            }
        } finally {
            isRendering = false;
        }
    }

    public boolean wasWindowClickedThisTick() {
        return windowClickedThisTick;
    }

    @Nullable
    public PiPWindow getLastClickedWindowThisTick() {
        return lastClickedWindowThisTick;
    }

    public boolean isRendering() {
        return isRendering;
    }

    public void beginScreenRender(@NotNull PiPWindow window, double scaleFactor) {
        activeScreenRenderWindow = window;
        activeScreenRenderScaleFactor = scaleFactor;
    }

    public void endScreenRender(@NotNull PiPWindow window) {
        if (activeScreenRenderWindow == window) {
            activeScreenRenderWindow = null;
            activeScreenRenderScaleFactor = 1.0;
        }
    }

    public int getActiveScreenRenderOffsetX() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyX() : 0;
    }

    public int getActiveScreenRenderOffsetY() {
        return activeScreenRenderWindow != null ? activeScreenRenderWindow.getBodyY() : 0;
    }

    public double getActiveScreenRenderScaleFactor() {
        return activeScreenRenderWindow != null ? activeScreenRenderScaleFactor : 1.0;
    }

    public boolean isScreenRenderActive() {
        return activeScreenRenderWindow != null;
    }

    @Override
    public void tick() {
        windowClickedThisTick = false;
        lastClickedWindowThisTick = null;
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.tick();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<PiPWindow> snapshot = new ArrayList<>(windows);
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
            if (windows.contains(window)) {
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
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseReleased(mouseX, mouseY, button);
            if (button == activePointerButton) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
            if (handled) {
                return true;
            }
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            if (handled) {
                return true;
            }
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        List<PiPWindow> snapshot = new ArrayList<>(windows);
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
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            if (window.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            if (window.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window != null) {
            if (window.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Nullable
    private PiPWindow getFocusedWindow() {
        if (focusedWindow != null && focusedWindow.isVisible() && !focusedWindow.isInputLocked()) {
            return focusedWindow;
        }
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible() && !window.isInputLocked()) {
                focusedWindow = window;
                return window;
            }
        }
        return null;
    }

    public boolean isWindowFocused(@NotNull PiPWindow window) {
        return focusedWindow == window;
    }

    public boolean isAnyWindowBlockingMinecraftScreenInputs() {
        for (PiPWindow window : windows) {
            if (window.isVisible() && window.isBlockingMinecraftScreenInputs()) {
                return true;
            }
        }
        return false;
    }

}
