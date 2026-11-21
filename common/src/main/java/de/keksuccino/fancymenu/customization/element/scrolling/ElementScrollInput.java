package de.keksuccino.fancymenu.customization.element.scrolling;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementSpaceInputListener;
import de.keksuccino.fancymenu.customization.element.container.ElementContainer;
import de.keksuccino.fancymenu.customization.element.container.ElementContainerHierarchy;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Takes the input of a scrollable element from the screen: the mouse wheel, the grabbers of its scroll bars and the mouse that drags its content.<br>
 * It is a {@link FancyMenuUiComponent}, so it gets every click and scroll of the screen and decides on its own which ones are for the element.
 * An element in the content of another one gets its input before the outer one, and hands a scroll it has no use for on to it by not consuming it.<br><br>
 *
 * It takes the mouse where it is on the screen and maps it into the space the content scrolls in itself. An element whose content scrolls
 * in another space than the screen, because it can be rotated or tilted, has to register it to the screen as it is
 * ({@link AbstractElement#registersChildWidgetsDirectly()}), or the mouse gets mapped twice.
 */
public class ElementScrollInput implements GuiEventListener, NarratableEntry, FancyMenuUiComponent {

    @NotNull
    protected final ScrollableElement host;

    public ElementScrollInput(@NotNull ScrollableElement host) {
        this.host = host;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.host.isShowingScrollContent() || !this.host.isScrollPointVisible(mouseX, mouseY)) return false;
        // What scrolls in the content comes first, and the element takes over where that has nowhere to scroll to anymore
        if (this.host.scrollContentByWheel(mouseX, mouseY, scrollX, scrollY)) return true;
        return this.host.getScrolling().scrollByWheel(scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The bars sit next to what the element shows of its content, where a container around the element can still cut them off
        if (!this.host.isShowingScrollContent() || !this.host.getScrollElement().isScreenPointVisibleInContainer(mouseX, mouseY)) return false;
        double localX = this.host.toScrollSpaceX(mouseX, mouseY);
        double localY = this.host.toScrollSpaceY(mouseX, mouseY);
        if (this.host.getScrollBars().mouseClicked(localX, localY, button)) return true;
        // A press on the content that can drag it along is for the element, so nothing behind the element gets it as well.
        // Where something in the content or on top of the element takes the press, it stays a click for that.
        if ((button == InputConstants.MOUSE_BUTTON_LEFT) && this.host.isScrollPointVisible(mouseX, mouseY) && !this.host.takesContentPressAt(mouseX, mouseY) && !this.isCoveredAt(mouseX, mouseY)) {
            return this.host.getScrolling().onMousePressed((int) localX, (int) localY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ElementScrollBars bars = this.host.getScrollBars();
        boolean barWasGrabbed = bars.isGrabbed();
        bars.mouseReleased(this.host.toScrollSpaceX(mouseX, mouseY), this.host.toScrollSpaceY(mouseX, mouseY), button);
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.host.getScrolling().onMouseReleased();
            // A grabber the user let go of leaves the content wherever it was dragged to
            if (barWasGrabbed) this.host.getScrolling().snapToNearest();
        }
        return false;
    }

    /**
     * Returns if something that takes mouse input lies on top of the element at the given position on the screen, where a press is for that
     * and not for the element: a widget of an element that renders after it, or a widget of the screen itself while the element renders behind the screen.
     */
    public boolean isCoveredAt(double screenX, double screenY) {
        Screen screen = ScreenUtils.getScreen();
        ScreenCustomizationLayer layer = (screen != null) ? ScreenCustomizationLayerHandler.getLayerOfScreen(screen) : null;
        if (layer == null) return false;
        AbstractElement element = this.host.getScrollElement();
        ElementContainer container = (element instanceof ElementContainer elementContainer) ? elementContainer : null;
        List<AbstractElement> elements = layer.allElements;
        int ownIndex = elements.indexOf(element);
        boolean behindScreen = layer.normalElements.backgroundElements.contains(element);
        for (GuiEventListener listener : screen.children()) {
            // FancyMenu UI components take what is for them before the element gets asked
            if ((listener instanceof FancyMenuUiComponent) || !listener.isMouseOver(screenX, screenY)) continue;
            AbstractElement owner = findOwner(elements, listener);
            if (owner == null) {
                if (behindScreen) return true;
            } else if ((owner != element) && ((container == null) || !ElementContainerHierarchy.isInContainer(owner, container)) && (elements.indexOf(owner) > ownIndex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the element the widget belongs to, or NULL if it is a widget of the screen itself.
     */
    @Nullable
    private static AbstractElement findOwner(@NotNull List<AbstractElement> elements, @NotNull GuiEventListener widget) {
        GuiEventListener searched = ElementSpaceInputListener.unwrap(widget);
        for (int i = 0; i < elements.size(); i++) {
            List<GuiEventListener> widgets = elements.get(i).getWidgetsToRegister();
            if (widgets == null) continue;
            for (GuiEventListener elementWidget : widgets) {
                if (ElementSpaceInputListener.unwrap(elementWidget) == searched) return elements.get(i);
            }
        }
        return null;
    }

    /**
     * Always FALSE, like for elements: a hovered listener would take clicks away from everything behind the element.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {
    }

}
