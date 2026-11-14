package de.keksuccino.fancymenu.customization.element.elements.scrollarea;

import de.keksuccino.fancymenu.util.rendering.ui.FancyMenuUiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Takes the input of a scrollable area from the screen: the mouse wheel and the grabbers of its scroll bars.<br>
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
        return this.area.scrollByWheel(scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.area.isShowingChildren()) return false;
        return this.area.getBars().mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        this.area.getBars().mouseReleased(event.x(), event.y(), event.button());
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        return this.area.getBars().mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
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
