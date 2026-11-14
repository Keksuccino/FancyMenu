package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/**
 * Hands the mouse input a screen sends to a widget of an element on to that widget in the untransformed space of the element,
 * which is the space the element renders the widget in. Rotated and tilted elements register these instead of their widgets,
 * so the widgets react to the mouse where they appear on the screen.<br>
 * Use {@link #of(AbstractElement, GuiEventListener)}, which keeps the input routing of FancyMenu UI components for widgets that are one.
 */
public class ElementSpaceInputListener implements GuiEventListener, NarratableEntry {

    @NotNull
    protected final AbstractElement element;
    @NotNull
    protected final GuiEventListener widget;
    @NotNull
    protected final NarratableEntry narratable;

    @NotNull
    public static <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener of(@NotNull AbstractElement element, @NotNull T widget) {
        // FancyMenu UI components get every click and scroll, not only the hovered ones, so the listener has to stay one of them
        return (widget instanceof FancyMenuUiComponent) ? new UiComponent(element, widget) : new ElementSpaceInputListener(element, widget);
    }

    protected <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener(@NotNull AbstractElement element, @NotNull T widget) {
        this.element = Objects.requireNonNull(element);
        this.widget = Objects.requireNonNull(widget);
        this.narratable = widget;
    }

    @NotNull
    public GuiEventListener getWidget() {
        return this.widget;
    }

    protected double toElementSpaceX(double mouseX, double mouseY) {
        return this.element.untransformMouseX(mouseX, mouseY);
    }

    protected double toElementSpaceY(double mouseX, double mouseY) {
        return this.element.untransformMouseY(mouseX, mouseY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.widget.mouseMoved(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.widget.mouseClicked(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.widget.mouseReleased(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double x = this.toElementSpaceX(mouseX, mouseY);
        double y = this.toElementSpaceY(mouseX, mouseY);
        // The drag distance is the difference between the mapped positions, so it follows the rotation like the position does
        double previousX = mouseX - dragX;
        double previousY = mouseY - dragY;
        return this.widget.mouseDragged(x, y, button, x - this.toElementSpaceX(previousX, previousY), y - this.toElementSpaceY(previousX, previousY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.widget.mouseScrolled(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.widget.isMouseOver(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.widget.charTyped(codePoint, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        this.widget.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return this.widget.isFocused();
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return this.narratable.narrationPriority();
    }

    @Override
    public boolean isActive() {
        return this.narratable.isActive();
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {
        this.narratable.updateNarration(output);
    }

    private static class UiComponent extends ElementSpaceInputListener implements FancyMenuUiComponent {

        private <T extends GuiEventListener & NarratableEntry> UiComponent(@NotNull AbstractElement element, @NotNull T widget) {
            super(element, widget);
        }

    }

}
