package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public abstract class PiPScreen extends Screen implements PipableScreen {

    @Nullable
    private PiPWindow window;

    public PiPScreen(Component title) {
        super(title);
    }

    public PiPScreen() {
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

    @Override
    public void onScreenClosed() {
    }

    @Override
    public void onWindowClosedExternally() {
    }

    @Override
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public final void onClose() {
    }

    @Override
    public final void removed() {
    }

}
