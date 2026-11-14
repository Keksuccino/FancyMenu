package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;

/**
 * Takes the input of a scrollable area from the screen: the mouse wheel, the grabbers of its scroll bars and the mouse that drags its content.<br>
 * It is a {@link FancyMenuUiComponent}, so it gets every click and scroll of the screen and decides on its own which ones are for the area.
 * An area in another area gets its input before the outer one, and hands a scroll it has no use for on to it by not consuming it.
 */
public class ScrollAreaInput implements GuiEventListener, NarratableEntry, FancyMenuUiComponent {

    @NotNull
    protected final ScrollAreaElement area;

    public ScrollAreaInput(@NotNull ScrollAreaElement area) {
        this.area = area;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.area.isShowingChildren() || !this.area.isScreenPointVisible(mouseX, mouseY)) return false;
        // What scrolls in the area comes first, and the area takes over where that has nowhere to scroll to anymore
        if (this.area.scrollChildByWheel(mouseX, mouseY, scrollX, scrollY)) return true;
        return this.area.scrollByWheel(scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // The bars sit next to what the area shows, where a container around the area can still cut them off
        if (!this.area.isShowingChildren() || !this.area.isScreenPointVisibleInContainer(mouseX, mouseY)) return false;
        if (this.area.getBars().mouseClicked(mouseX, mouseY, button)) return true;
        // A press on the content that can drag it along is for the area, so nothing behind the area gets it as well.
        // Where something in the area or on top of it takes the press, it stays a click for that.
        if ((button == InputConstants.MOUSE_BUTTON_LEFT) && this.area.isScreenPointVisible(mouseX, mouseY) && !this.area.hasInteractiveChildAt(mouseX, mouseY) && !this.area.isCoveredAt(mouseX, mouseY)) {
            return this.area.getScrolling().onMousePressed((int) mouseX, (int) mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean barWasGrabbed = this.area.getBars().isGrabbed();
        this.area.getBars().mouseReleased(mouseX, mouseY, button);
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            this.area.getScrolling().onMouseReleased();
            // A grabber the user let go of leaves the content wherever it was dragged to
            if (barWasGrabbed) this.area.getScrolling().snapToNearest();
        }
        return false;
    }

    /**
     * Always FALSE, like for elements: a hovered listener would take clicks away from everything behind the area.
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
