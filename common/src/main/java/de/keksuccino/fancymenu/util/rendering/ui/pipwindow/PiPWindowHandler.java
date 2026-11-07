package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.Tickable;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.UISounds;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PiPWindowHandler implements GuiEventListener, Tickable, Renderable {

    public static final PiPWindowHandler INSTANCE = new PiPWindowHandler();
    private static final double DEFAULT_WINDOW_SIZE_SCALE_WIDTH = 0.4;
    private static final double DEFAULT_WINDOW_SIZE_SCALE_HEIGHT = 0.5;
    private static final long BLOCKED_INPUT_OVERLAY_DURATION_MS = 1500L;

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
    private long blockedInputOverlayUntilMs = -1L;
    @Nullable
    private PiPWindow blockedInputOverlayWindow;
    @Nullable
    private PiPWindow fullscreenInheritanceParentWindow;

    private PiPWindowHandler() {
    }

    public PiPWindow openWindow(@NotNull PiPWindow window, @Nullable PiPWindow parentWindow) {
        PiPWindow fullscreenInheritanceParent = (parentWindow != null) ? parentWindow : this.fullscreenInheritanceParentWindow;
        if (parentWindow != null) {
            parentWindow.registerChildWindow(window);
        }
        if (windows.contains(window)) {
            bringToFront(window);
            return window;
        }
        inheritTopLayerSettings(window);
        if (fullscreenInheritanceParent != null) {
            window.setMaximized(fullscreenInheritanceParent.isMaximized());
        }
        windows.add(window);
        window.addCloseCallback(() -> closeWindow(window));
        if (parentWindow == null && fullscreenInheritanceParent != null) {
            window.addCloseCallback(() -> fullscreenInheritanceParent.setMaximized(window.isMaximized()));
        }
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
        double guiScale = window.isSizeScaledToGuiScale() ? WindowHandler.getGuiScale() : 1.0;
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
        if (!window.isClosingViaCallback()) {
            window.close();
            return;
        }
        closeWindowInternal(window);
    }

    private void closeWindowInternal(@NotNull PiPWindow window) {
        PiPWindow parent = window.getParentWindow();
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
        if (parent != null) {
            parent.setMaximized(window.isMaximized());
            parent.unregisterChildWindow(window);
        }
        if (focusedWindow == window) {
            focusedWindow = getTopVisibleWindow();
        }
        if (activePointerWindow == window) {
            activePointerWindow = null;
            activePointerButton = -1;
        }
        enforceForceFocus();
    }

    public void closeAllWindows() {
        closeAllWindows(false);
    }

    public void forceCloseAllWindows() {
        closeAllWindows(true);
    }

    private void closeAllWindows(boolean force) {
        List<PiPWindow> copy = new ArrayList<>(windows);
        for (PiPWindow window : copy) {
            if (force) {
                window.setCloseWindowCheck(null);
            }
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

    @Nullable
    public PiPWindow getTopWindowAt(double mouseX, double mouseY) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (!window.isVisible()) {
                continue;
            }
            if (window.isMouseOver(mouseX, mouseY)) {
                return window;
            }
        }
        return null;
    }

    /**
     * Gives files that got dropped onto the game window to the window under the mouse.
     *
     * @return TRUE if a window took the files, so the screen behind it doesn't get them.
     */
    public boolean onFilesDrop(@NotNull List<Path> paths, double mouseX, double mouseY) {
        PiPWindow window = getTopWindowAt(mouseX, mouseY);
        // Drops follow the same rules as clicks, so neither blocked windows nor the screen behind a blocking window get files
        PiPWindow blockingWindow = getTopInputBlockingWindow();
        if ((blockingWindow != null) && (blockingWindow != window)) {
            triggerBlockedInputOverlay(blockingWindow);
            UISounds.playDefaultBeep();
            return true;
        }
        if (window == null) return false;
        if (window.isInputLocked()) {
            this.rejectInputOfLockedWindow(window);
            return true;
        }
        Screen screen = window.getScreen();
        if (screen != null) screen.onFilesDrop(paths);
        return true;
    }

    public boolean isPointVisibleForWindow(@NotNull PiPWindow window, double mouseX, double mouseY) {
        return getTopWindowAt(mouseX, mouseY) == window;
    }

    public boolean canMouseReachWindow(@NotNull PiPWindow window, double mouseX, double mouseY) {
        // Locked windows take no input, so they also show no hover effects or tooltips that would make them look usable
        if (window.isInputLocked()) {
            return false;
        }
        PiPWindow blockingWindow = getTopInputBlockingWindow();
        if (blockingWindow != null && blockingWindow != window) {
            return false;
        }
        return isPointVisibleForWindow(window, mouseX, mouseY);
    }

    public void refreshAllScreens() {
        for (PiPWindow window : new ArrayList<>(windows)) {
            window.refreshScreen();
        }
    }

    /**
     * Brings the window to the front and focuses it.<br>
     * Child windows block their parent, so they get raised with it and stay in front of it in their current order, and the top one of them gets the focus.
     */
    public void bringToFront(@NotNull PiPWindow window) {
        this.moveToFront(window);
        if (!window.getChildWindows().isEmpty()) {
            for (PiPWindow openWindow : new ArrayList<>(windows)) {
                if (isDescendantOf(openWindow, window)) this.moveToFront(openWindow);
            }
        }
        focusedWindow = this.getTopWindowOfFamily(window);
        enforceForceFocus();
    }

    void refreshWindowOrder(@NotNull PiPWindow window) {
        if (this.moveToFront(window)) {
            enforceForceFocus();
        }
    }

    /**
     * Moves the window to the front of its layer, without changing the focus.
     *
     * @return TRUE if the window is open.
     */
    private boolean moveToFront(@NotNull PiPWindow window) {
        if (!windows.remove(window)) {
            return false;
        }
        windows.add(getInsertIndexForFront(window), window);
        return true;
    }

    /**
     * Returns the top visible window out of the window and its child windows, or the window itself if none of its child windows is visible.
     */
    @NotNull
    private PiPWindow getTopWindowOfFamily(@NotNull PiPWindow window) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow openWindow = windows.get(i);
            if (openWindow == window) return window;
            if (openWindow.isVisible() && isDescendantOf(openWindow, window)) return openWindow;
        }
        return window;
    }

    private static boolean isDescendantOf(@NotNull PiPWindow window, @NotNull PiPWindow ancestor) {
        for (PiPWindow parent = window.getParentWindow(); parent != null; parent = parent.getParentWindow()) {
            if (parent == ancestor) return true;
        }
        return false;
    }

    /**
     * Child windows block their parent, so input on the parent only raises them, with the same overlay and beep as input outside a blocking window.
     */
    private void rejectInputOfLockedWindow(@NotNull PiPWindow window) {
        bringToFront(window);
        PiPWindow blockingChild = this.getTopWindowOfFamily(window);
        if (blockingChild != window) {
            triggerBlockedInputOverlay(blockingChild);
            UISounds.playDefaultBeep();
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        PiPWindow overlayTargetWindow = resolveBlockedInputOverlayWindow();
        isRendering = true;
        try {
            for (PiPWindow window : new ArrayList<>(windows)) {
                if (overlayTargetWindow == window) {
                    renderBlockedInputOverlay(graphics);
                }
                window.extractRenderState(graphics, mouseX, mouseY, partial);
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
        if (this.blockedInputOverlayUntilMs <= System.currentTimeMillis()) {
            this.blockedInputOverlayWindow = null;
        }
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
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return this.mouseClicked(event.x(), event.y(), event.button());
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        PiPWindow forcedWindow = getTopInputBlockingWindow();
        if (forcedWindow != null) {
            if (!isWindowFocused(forcedWindow)) {
                bringToFront(forcedWindow);
            }
            focusedWindow = forcedWindow;
            if (!forcedWindow.isMouseOver(mouseX, mouseY)) {
                triggerBlockedInputOverlay(forcedWindow);
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
                    if (!forcedWindow.isVisible()) {
                        this.focusTopVisibleWindowAfterClickedWindowUnavailable();
                        return true;
                    }
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
                    this.focusTopVisibleWindowAfterClickedWindowUnavailable();
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
                this.rejectInputOfLockedWindow(window);
                activePointerWindow = window;
                activePointerButton = button;
                enforceForceFocus();
                return true;
            }
            if (!isWindowFocused(window)) {
                bringToFront(window);
            }
            window.mouseClicked(mouseX, mouseY, button);
            if (windows.contains(window)) {
                if (!window.isVisible()) {
                    this.focusTopVisibleWindowAfterClickedWindowUnavailable();
                    return true;
                }
                PiPWindow topBlocking = getTopInputBlockingWindow();
                if (topBlocking != null && topBlocking != window) {
                    activePointerWindow = window;
                    activePointerButton = button;
                    enforceForceFocus();
                    return true;
                }
                // Keeps windows that the click opened in front, since child windows always stay in front of their parent
                bringToFront(window);
                activePointerWindow = window;
                activePointerButton = button;
                enforceForceFocus();
            } else {
                this.focusTopVisibleWindowAfterClickedWindowUnavailable();
            }
            return true;
        }

        focusedWindow = null;
        activePointerWindow = null;
        activePointerButton = -1;
        enforceForceFocus();
        return isAnyWindowBlockingMinecraftScreenInputs();
    }

    void handleWindowVisibilityChanged(@NotNull PiPWindow window) {
        if (!window.isVisible()) {
            if (focusedWindow == window) {
                focusedWindow = getTopVisibleWindow();
            }
            if (activePointerWindow == window) {
                activePointerWindow = null;
                activePointerButton = -1;
            }
        } else if (isInputBlockingWindow(window)) {
            focusedWindow = window;
        }
        enforceForceFocus();
    }

    private void focusTopVisibleWindowAfterClickedWindowUnavailable() {
        PiPWindow topVisible = getTopVisibleWindow();
        if (topVisible != null) {
            bringToFront(topVisible);
        } else {
            focusedWindow = null;
            enforceForceFocus();
        }
        activePointerWindow = null;
        activePointerButton = -1;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return this.mouseReleased(event.x(), event.y(), event.button());
    }
    
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
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }
    
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
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.keycode(), event.modifiers());
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
        return this.keyReleased(event.key(), event.keycode(), event.modifiers());
    }
    
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.keyReleased(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return this.charTyped((char)event.codepoint(), 0);
    }
    
    public boolean charTyped(char codePoint, int modifiers) {
        PiPWindow window = getFocusedWindow();
        if (window == null) {
            return false;
        }
        window.charTyped(codePoint, modifiers);
        return true;
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
        if (focusedWindow != null && focusedWindow.isVisible()) {
            return focusedWindow;
        }
        focusedWindow = null;
        return null;
    }

    @Nullable
    private PiPWindow getTopVisibleWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            PiPWindow window = windows.get(i);
            if (window.isVisible()) {
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

    public void withFullscreenInheritanceFrom(@Nullable PiPWindow parentWindow, @NotNull Runnable action) {
        PiPWindow previousParent = this.fullscreenInheritanceParentWindow;
        this.fullscreenInheritanceParentWindow = parentWindow;
        try {
            action.run();
        } finally {
            this.fullscreenInheritanceParentWindow = previousParent;
        }
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

    private void inheritTopLayerSettings(@NotNull PiPWindow window) {
        boolean shouldForceFocus = false;
        boolean shouldAlwaysOnTop = false;
        for (PiPWindow openWindow : windows) {
            if (!openWindow.isVisible()) {
                continue;
            }
            if (openWindow.isForceFocusEnabled()) {
                shouldForceFocus = true;
            }
            if (openWindow.isAlwaysOnTop()) {
                shouldAlwaysOnTop = true;
            }
            if (shouldForceFocus && shouldAlwaysOnTop) {
                break;
            }
        }
        if (shouldForceFocus) {
            window.setForceFocus(true);
        }
        if (shouldAlwaysOnTop) {
            window.setAlwaysOnTop(true);
        }
    }

    private void enforceForceFocus() {
        PiPWindow forced = getTopInputBlockingWindow();
        if (forced != null && forced.isVisible() && !forced.isInputLocked()) {
            focusedWindow = forced;
        }
    }

    private void triggerBlockedInputOverlay(@NotNull PiPWindow blockedWindow) {
        this.blockedInputOverlayUntilMs = System.currentTimeMillis() + BLOCKED_INPUT_OVERLAY_DURATION_MS;
        this.blockedInputOverlayWindow = blockedWindow;
    }

    @Nullable
    private PiPWindow resolveBlockedInputOverlayWindow() {
        if (this.blockedInputOverlayUntilMs <= System.currentTimeMillis()) {
            this.blockedInputOverlayWindow = null;
            return null;
        }
        // Child windows block their parent, so they get the overlay too, not only windows that block everything
        if (this.blockedInputOverlayWindow != null
                && this.windows.contains(this.blockedInputOverlayWindow)
                && this.blockedInputOverlayWindow.isVisible()
                && (isInputBlockingWindow(this.blockedInputOverlayWindow) || (this.blockedInputOverlayWindow.getParentWindow() != null))) {
            return this.blockedInputOverlayWindow;
        }
        this.blockedInputOverlayWindow = getTopInputBlockingWindow();
        return this.blockedInputOverlayWindow;
    }

    private void renderBlockedInputOverlay(@NotNull GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        RenderingUtils.setDepthTestLocked(true);
        graphics.fill(0, 0, screenWidth, screenHeight, UIBase.getUITheme().pip_input_blocked_overlay_color.getColorInt());
        RenderingUtils.setDepthTestLocked(false);
    }

}
