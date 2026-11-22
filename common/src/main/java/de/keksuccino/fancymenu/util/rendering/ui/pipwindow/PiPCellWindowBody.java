package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PiPCellWindowBody extends CellScreen implements PipableScreen {

    /** The pointer position the body gets while it's covered, far outside of everything, like {@link PiPWindow} uses for bodies the pointer can't reach. */
    private static final int COVERED_POINTER_POSITION = -100000;

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

        boolean blur = UIBase.shouldBlur();

        UIBase.applyDefaultWidgetSkinTo(this.searchBar, blur);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton, blur);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton, blur);
        for (AbstractWidget widget : this.rightSideWidgets) {
            UIBase.applyDefaultWidgetSkinTo(widget, blur);
        }

        this.scrollArea.setSetupForBlurInterface(blur);
        if (this.descriptionScrollArea != null) {
            this.descriptionScrollArea.setSetupForBlurInterface(blur);
        }

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
    public final void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.renderMouseX = mouseX;
        this.renderMouseY = mouseY;
        // What renderLateBody() draws, like the open suggestion list of a field, lies on top of the rest of the body. While the pointer is over it,
        // the rest of the body gets a pointer far outside of it, so the widgets below don't show their hover state and their tooltips over it.
        boolean lateBodyHovered = this.isLateBodyHovered(mouseX, mouseY);
        int bodyMouseX = lateBodyHovered ? COVERED_POINTER_POSITION : mouseX;
        int bodyMouseY = lateBodyHovered ? COVERED_POINTER_POSITION : mouseY;
        this.renderBody(graphics, bodyMouseX, bodyMouseY, partial);
        super.extractRenderState(graphics, bodyMouseX, bodyMouseY, partial);
        this.renderLateBody(graphics, mouseX, mouseY, partial);
    }

    /**
     * @return If something {@link #renderLateBody} draws covers the given point, so the rest of the body must not react to the pointer there.
     */
    protected boolean isLateBodyHovered(int mouseX, int mouseY) {
        return false;
    }

    @Override
    protected int getContentTop() {
        // PiP screens render no title, so they need no room for it
        return PiPWindowBody.CONTENT_MARGIN;
    }

    @Override
    protected void autoScaleScreen(AbstractWidget topRightSideWidget) {
        // PiP screens should not scale itself
    }

    @Override
    public void renderCellScreenBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        // PiP screens should render no background
    }

    @Override
    protected void renderTitle(@NotNull GuiGraphicsExtractor graphics) {
        // PiP screens render no title, because it gets set as PiPWindow title instead
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return this.keyPressed(event.key(), event.keycode(), event.modifiers());
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.allowCloseOnEsc && (keyCode == InputConstants.KEY_ESCAPE)) {
            this.closeWindow();
            this.onWindowClosedExternally();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Cell rows precede their scroll area in the screen's child list. Minecraft only forwards a wheel event to the
     * first hovered child, so a row can prevent the scroll area from receiving it even though the row does not handle
     * scrolling. PiP cell windows need the legacy all-children routing used by {@link PiPWindowBody}.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        for (GuiEventListener listener : this.children()) {
            if (listener.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) {
                return true;
            }
        }
        return false;
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
