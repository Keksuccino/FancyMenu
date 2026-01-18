package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import de.keksuccino.fancymenu.util.rendering.ui.UISounds;
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

    public PiPWindow openWindow(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        if (parentWindow != null) {
            parentWindow.registerChildWindow(window);
        }
        if (windows.contains(window)) {
            bringToFront(window);
            return window;
        }
        windows.add(window);
        window.addCloseCallback(() -> closeWindow(window));
        bringToFront(window);
        return window;
    }

    public PiPWindow openWindowCentered(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        window.setPosition(x, y);
        return openWindow(window, parentWindow);
    }

    public PiPWindow openWindowWithDefaultSizeAndPosition(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
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
        return openWindow(window, parentWindow);
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
        enforceForceFocus();
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
            int insertIndex = getInsertIndexForFront(window);
            windows.add(insertIndex, window);
        }
        focusedWindow = window;
        enforceForceFocus();
    }

    void refreshWindowOrder(@NotNull PiPWindow window) {
        if (!windows.remove(window)) {
            return;
        }
        int insertIndex = getInsertIndexForFront(window);
        windows.add(insertIndex, window);
        enforceForceFocus();
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
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null) {
            focusedWindow = forcedWindow;
            if (!forcedWindow.isMouseOver(mouseX, mouseY)) {
                UISounds.playDefaultBeep();
                return true;
            }
            if (forcedWindow.isInputLocked()) {
                activePointerWindow = forcedWindow;
                activePointerButton = button;
                return true;
            }
            if (forcedWindow.isMouseOver(mouseX, mouseY)) {
                forcedWindow.mouseClicked(mouseX, mouseY, button);
                if (windows.contains(forcedWindow)) {
                    PiPWindow topBlocking = getTopInputBlockingWindow();
                    if (topBlocking != null && topBlocking != forcedWindow) {
                        activePointerWindow = forcedWindow;
                        activePointerButton = button;
                        enforceForceFocus();
                        return true;
                    }
                    bringToFront(forcedWindow);
                    activePointerWindow = forcedWindow;
                    activePointerButton = button;
                } else {
                    focusedWindow = null;
                    activePointerWindow = null;
                    activePointerButton = -1;
                }
            }
            return true;
        }
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
                enforceForceFocus();
                return true;
            }
            window.mouseClicked(mouseX, mouseY, button);
            if (windows.contains(window)) {
                PiPWindow topBlocking = getTopInputBlockingWindow();
                if (topBlocking != null && topBlocking != window) {
                    activePointerWindow = window;
                    activePointerButton = button;
                    enforceForceFocus();
                    return true;
                }
                bringToFront(window);
                focusedWindow = window;
                activePointerWindow = window;
                activePointerButton = button;
                enforceForceFocus();
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
        enforceForceFocus();
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && activePointerWindow != null && activePointerWindow != forcedWindow) {
            if (button == activePointerButton) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
            return true;
        }
        if (forcedWindow != null && activePointerWindow == null) {
            return true;
        }
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
        return forcedWindow != null || isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && activePointerWindow != null && activePointerWindow != forcedWindow) {
            return true;
        }
        if (forcedWindow != null && activePointerWindow == null) {
            return true;
        }
        if (activePointerWindow != null) {
            boolean handled = activePointerWindow.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            if (handled) {
                return true;
            }
        }
        return forcedWindow != null || isAnyWindowBlockingMinecraftScreenInputs();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null) {
            if (forcedWindow.isInputLocked()) {
                return true;
            }
            if (forcedWindow.isMouseOver(mouseX, mouseY)) {
                forcedWindow.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
            }
            return true;
        }
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
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && forcedWindow.isVisible()) {
            forcedWindow.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
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
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && forcedWindow.isVisible()) {
            forcedWindow.keyReleased(keyCode, scanCode, modifiers);
            return true;
        }
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
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null && forcedWindow.isVisible()) {
            forcedWindow.charTyped(codePoint, modifiers);
            return true;
        }
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
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && forced.isVisible() && !forced.isInputLocked()) {
            focusedWindow = forced;
            return forced;
        }
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
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && !forced.isInputLocked()) {
            return forced == window;
        }
        return focusedWindow == window;
    }

    public boolean isAnyWindowBlockingMinecraftScreenInputs() {
        if (getTopForceFocusWindow() != null) {
            return true;
        }
        for (PiPWindow window : windows) {
            if (window.isVisible() && window.isBlockingMinecraftScreenInputs()) {
                return true;
            }
        }
        return false;
    }

    public boolean isForceFocusWindowOpen() {
        return getTopForceFocusWindow() != null;
    }

    @Nullable
    private PiPWindow getTopForceFocusWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible() && window.isForceFocusEnabled()) {
                return window;
            }
        }
        return null;
    }

    private boolean isInputBlockingWindow(@NotNull PiPWindow window) {
        return window.isForceFocusEnabled() || window.isBlockingMinecraftScreenInputs();
    }

    @Nullable
    private PiPWindow getTopInputBlockingWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible() && isInputBlockingWindow(window)) {
                return window;
            }
        }
        return null;
    }

    private int getInsertIndexForFront(@NotNull PiPWindow window) {
        int targetLayer = getWindowLayer(window);
        for (int i = 0; i < windows.size(); i++) {
            if (getWindowLayer(windows.get(i)) > targetLayer) {
                return i;
            }
        }
        return windows.size();
    }

    private int getWindowLayer(@NotNull PiPWindow window) {
        if (window.isForceFocusEnabled() || window.isBlockingMinecraftScreenInputs()) {
            return 2;
        }
        if (window.isAlwaysOnTop()) {
            return 1;
        }
        return 0;
    }

    private void enforceForceFocus() {
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && forced.isVisible() && !forced.isInputLocked()) {
            focusedWindow = forced;
        }
    }

}
