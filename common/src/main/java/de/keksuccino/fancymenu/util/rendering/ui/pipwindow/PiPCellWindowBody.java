package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PiPCellWindowBody extends CellScreen implements PipableScreen {

    @Nullable
    private PiPWindow window;
    protected boolean allowCloseOnEsc = true;
    private int renderMouseX = 0;
    private int renderMouseY = 0;

    public PiPCellWindowBody(Component title) {
        super(title);
    }

    public PiPCellWindowBody() {
        super(Component.empty());
    }

    @Override
    protected void init() {

        super.init();

        UIBase.applyDefaultWidgetSkinTo(this.searchBar, UIBase.shouldBlur());
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, UIBase.shouldBlur());
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, UIBase.shouldBlur());

        this.scrollArea.setSetupForBlurInterface(true);

    }

    @Override
    protected <T extends AbstractWidget> T addRightSideWidget(@NotNull T widget) {
        return UIBase.applyDefaultWidgetSkinTo(super.addRightSideWidget(widget), UIBase.shouldBlur());
    }

    @Override
    protected @NotNull CellScreen.WidgetCell addWidgetCell(@NotNull AbstractWidget widget, boolean applyDefaultButtonSkin) {
        WidgetCell c = super.addWidgetCell(widget, applyDefaultButtonSkin);
        if (applyDefaultButtonSkin) UIBase.applyDefaultWidgetSkinTo(widget, UIBase.shouldBlur());
        return c;
    }

    @Override
    protected <T extends RenderCell> @NotNull T addCell(@NotNull T cell) {
        if (cell instanceof TextInputCell tc) {
            UIBase.applyDefaultWidgetSkinTo(tc.editBox, UIBase.shouldBlur());
            UIBase.applyDefaultWidgetSkinTo(tc.openEditorButton, UIBase.shouldBlur());
        }
        return super.addCell(cell);
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

    public PiPCellWindowBody setAllowCloseOnEsc(boolean allowCloseOnEsc) {
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
    protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        // PiP screens should not scale itself
    }

    @Override
    public void renderCellScreenBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        // PiP screens should render no background
    }

    @Override
    protected void renderTitle(@NotNull GuiGraphics graphics) {
        // PiP screens render no title, because it gets set as PiPWindow title instead
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
