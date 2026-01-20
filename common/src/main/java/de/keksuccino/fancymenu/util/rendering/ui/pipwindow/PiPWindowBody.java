package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.input.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PiPWindowBody extends Screen implements PipableScreen {

    @Nullable
    private PiPWindow window;
    protected boolean allowCloseOnEsc = true;
    private int renderMouseX = 0;
    private int renderMouseY = 0;

    public PiPWindowBody(Component title) {
        super(title);
    }

    public PiPWindowBody() {
        super(Component.empty());
    }

    public void closeWindow() {
        PiPWindow resolvedWindow = resolveWindow();
        if (resolvedWindow == null) {
            onScreenClosed();
            return;
        }
        resolvedWindow.markClosingFromScreen();
        resolvedWindow.close();
    }

    public void setWindowVisible(boolean visible) {
        PiPWindow resolvedWindow = resolveWindow();
        if (resolvedWindow != null) resolvedWindow.setVisible(visible);
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

    public PiPWindowBody setAllowCloseOnEsc(boolean allowCloseOnEsc) {
        this.allowCloseOnEsc = allowCloseOnEsc;
        return this;
    }

    public int getRenderMouseX() {
        return renderMouseX;
    }

    public int getRenderMouseY() {
        return renderMouseY;
    }

    @Override
    public final void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderMouseX = mouseX;
        this.renderMouseY = mouseY;
        this.renderBody(graphics, mouseX, mouseY, partial);
        super.render(graphics, mouseX, mouseY, partial);
        this.renderLateBody(graphics, mouseX, mouseY, partial);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // PiP screens should render no background
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
