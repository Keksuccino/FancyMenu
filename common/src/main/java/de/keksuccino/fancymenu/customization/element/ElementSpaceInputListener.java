package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    protected MouseButtonEvent toElementSpace(@NotNull MouseButtonEvent event) {
        double x = this.element.untransformMouseX(event.x(), event.y());
        double y = this.element.untransformMouseY(event.x(), event.y());
        return ((x == event.x()) && (y == event.y())) ? event : new MouseButtonEvent(x, y, event.buttonInfo());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.widget.mouseMoved(this.element.untransformMouseX(mouseX, mouseY), this.element.untransformMouseY(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        return this.widget.mouseClicked(this.toElementSpace(event), isDoubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        return this.widget.mouseReleased(this.toElementSpace(event));
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent mapped = this.toElementSpace(event);
        // The drag distance is the difference between the mapped positions, so it follows the rotation like the position does
        double previousX = event.x() - dragX;
        double previousY = event.y() - dragY;
        double mappedDragX = mapped.x() - this.element.untransformMouseX(previousX, previousY);
        double mappedDragY = mapped.y() - this.element.untransformMouseY(previousX, previousY);
        return this.widget.mouseDragged(mapped, mappedDragX, mappedDragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.widget.mouseScrolled(this.element.untransformMouseX(mouseX, mouseY), this.element.untransformMouseY(mouseX, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.widget.isMouseOver(this.element.untransformMouseX(mouseX, mouseY), this.element.untransformMouseY(mouseX, mouseY));
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        return this.widget.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        return this.widget.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        return this.widget.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(@Nullable PreeditEvent event) {
        return this.widget.preeditUpdated(event);
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
