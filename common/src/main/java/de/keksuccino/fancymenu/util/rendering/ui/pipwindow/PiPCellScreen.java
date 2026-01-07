package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public abstract class PiPCellScreen extends CellScreen implements PipableScreen {

    @Nullable
    private PiPWindow window;
    protected boolean allowCloseOnEsc = true;

    public PiPCellScreen(Component title) {
        super(title);
    }

    public PiPCellScreen() {
        super(Component.empty());
    }

    public void closeWindow() {
        PiPWindow resolvedWindow = resolveWindow();
        if (resolvedWindow == null) {
            onScreenClosed();
            return;
        }
        resolvedWindow.markClosingFromScreen();
        resolvedWindow.setScreen(null);
        resolvedWindow.close();
    }

    public @Nullable PiPWindow getWindow() {
        return window;
    }

    @ApiStatus.Internal
    public void setWindow(@Nullable PiPWindow window) {
        this.window = window;
    }

    @Nullable
    private PiPWindow resolveWindow() {
        if (this.window != null) {
            return this.window;
        }
        for (PiPWindow openWindow : PiPWindowHandler.INSTANCE.getOpenWindows()) {
            if (openWindow.getScreen() == this) {
                return openWindow;
            }
        }
        return null;
    }

    public boolean isAllowCloseOnEsc() {
        return allowCloseOnEsc;
    }

    public PiPCellScreen setAllowCloseOnEsc(boolean allowCloseOnEsc) {
        this.allowCloseOnEsc = allowCloseOnEsc;
        return this;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.allowCloseOnEsc && (keyCode == InputConstants.KEY_ESCAPE)) {
            this.closeWindow();
            this.onWindowClosedExternally();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onScreenClosed() {
    }

    @Override
    public void onWindowClosedExternally() {
    }

    @Override
    public final void onClose() {
    }

    @Override
    public final void removed() {
    }

}
