package de.keksuccino.fancymenu.customization.element;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
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
 * Widgets of children of a container always get registered through these: they map the mouse into a scaled content,
 * and they keep input away from everything the container cuts off or does not show.<br>
 * The listener is what the screen knows of the widget: it is what gets the focus, and it is as focusable and navigatable as the widget is.<br>
 * Use {@link #of(AbstractElement, GuiEventListener)} or {@link #ofChildWidget(AbstractElement, GuiEventListener)}, which keep the input routing
 * of FancyMenu UI components for widgets that are one.
 */
public class ElementSpaceInputListener implements GuiEventListener, NarratableEntry, NavigatableWidget {

    @NotNull
    protected final AbstractElement element;
    @NotNull
    protected final GuiEventListener widget;
    @NotNull
    protected final NarratableEntry narratable;
    /** If the rotation and the tilt of the element get taken out of the mouse. Widgets that work them out themselves only get the scale of a container taken out. */
    protected final boolean unrotatesMouse;

    /**
     * Wraps a widget that knows nothing about the rotation and the tilt of its element.
     */
    @NotNull
    public static <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener of(@NotNull AbstractElement element, @NotNull T widget) {
        return create(element, widget, true);
    }

    /**
     * Wraps a widget an element registers as it is, for an element that is a child of a container. Such a widget
     * works out the rotation and the tilt of its element on its own, if it follows them at all.
     */
    @NotNull
    public static <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener ofChildWidget(@NotNull AbstractElement element, @NotNull T widget) {
        return create(element, widget, false);
    }

    @NotNull
    private static <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener create(@NotNull AbstractElement element, @NotNull T widget, boolean unrotatesMouse) {
        // FancyMenu UI components get every click and scroll, not only the hovered ones, so the listener has to stay one of them
        return (widget instanceof FancyMenuUiComponent) ? new UiComponent(element, widget, unrotatesMouse) : new ElementSpaceInputListener(element, widget, unrotatesMouse);
    }

    /**
     * Returns the widget the listener hands its input on to, or the listener itself if it is none of these.
     */
    @NotNull
    public static GuiEventListener unwrap(@NotNull GuiEventListener listener) {
        return (listener instanceof ElementSpaceInputListener wrapper) ? wrapper.widget : listener;
    }

    protected <T extends GuiEventListener & NarratableEntry> ElementSpaceInputListener(@NotNull AbstractElement element, @NotNull T widget, boolean unrotatesMouse) {
        this.element = Objects.requireNonNull(element);
        this.widget = Objects.requireNonNull(widget);
        this.narratable = widget;
        this.unrotatesMouse = unrotatesMouse;
    }

    protected double toElementSpaceX(double mouseX, double mouseY) {
        return this.unrotatesMouse ? this.element.untransformMouseX(mouseX, mouseY) : this.element.unscaleMouseX(mouseX, mouseY);
    }

    protected double toElementSpaceY(double mouseX, double mouseY) {
        return this.unrotatesMouse ? this.element.untransformMouseY(mouseX, mouseY) : this.element.unscaleMouseY(mouseX, mouseY);
    }

    @NotNull
    protected MouseButtonEvent toElementSpace(@NotNull MouseButtonEvent event) {
        double x = this.toElementSpaceX(event.x(), event.y());
        double y = this.toElementSpaceY(event.x(), event.y());
        return ((x == event.x()) && (y == event.y())) ? event : new MouseButtonEvent(x, y, event.buttonInfo());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.widget.mouseMoved(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.element.isScreenPointVisibleInContainer(event.x(), event.y())) return false;
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
        double mappedDragX = mapped.x() - this.toElementSpaceX(previousX, previousY);
        double mappedDragY = mapped.y() - this.toElementSpaceY(previousX, previousY);
        return this.widget.mouseDragged(mapped, mappedDragX, mappedDragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.element.isScreenPointVisibleInContainer(mouseX, mouseY)) return false;
        return this.widget.mouseScrolled(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY), scrollX, scrollY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.element.isScreenPointVisibleInContainer(mouseX, mouseY)) return false;
        return this.widget.isMouseOver(this.toElementSpaceX(mouseX, mouseY), this.toElementSpaceY(mouseX, mouseY));
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (!this.element.isShownByContainer()) return false;
        return this.widget.keyPressed(event);
    }

    @Override
    public boolean keyReleased(@NotNull KeyEvent event) {
        return this.widget.keyReleased(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (!this.element.isShownByContainer()) return false;
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

    /**
     * The focus goes to this listener and not to the widget. The listener is what the screen has in its list, where the screen
     * looks for what has the focus when it moves it on, and what keeps keys away from a widget that does not show.
     */
    @Override
    public @Nullable ComponentPath nextFocusPath(@NotNull FocusNavigationEvent navigationEvent) {
        if (!this.element.isShownByContainer()) return null;
        return this.toOwnFocusPath(this.widget.nextFocusPath(navigationEvent));
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        return this.toOwnFocusPath(this.widget.getCurrentFocusPath());
    }

    /**
     * A widget that takes the focus itself answers with a path that ends at it, which becomes one that ends at this listener.
     * Paths of widgets that hold other widgets lead into them and stay as they are.
     */
    @Nullable
    private ComponentPath toOwnFocusPath(@Nullable ComponentPath widgetPath) {
        return (widgetPath instanceof ComponentPath.Leaf) ? ComponentPath.leaf(this) : widgetPath;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return this.widget.getRectangle();
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return this.widget.shouldTakeFocusAfterInteraction();
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

    @Override
    public boolean isFocusable() {
        return !(this.widget instanceof NavigatableWidget navigatable) || navigatable.isFocusable();
    }

    @Override
    public void setFocusable(boolean focusable) {
        if (this.widget instanceof NavigatableWidget navigatable) navigatable.setFocusable(focusable);
    }

    @Override
    public boolean isNavigatable() {
        return !(this.widget instanceof NavigatableWidget navigatable) || navigatable.isNavigatable();
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        if (this.widget instanceof NavigatableWidget navigatableWidget) navigatableWidget.setNavigatable(navigatable);
    }

    private static class UiComponent extends ElementSpaceInputListener implements FancyMenuUiComponent {

        private <T extends GuiEventListener & NarratableEntry> UiComponent(@NotNull AbstractElement element, @NotNull T widget, boolean unrotatesMouse) {
            super(element, widget, unrotatesMouse);
        }

    }

}
