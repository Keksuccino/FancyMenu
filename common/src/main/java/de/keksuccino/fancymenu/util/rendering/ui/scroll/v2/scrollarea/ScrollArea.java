package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("all")
public class ScrollArea extends UIBase implements GuiEventListener, Renderable, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    public ScrollBar verticalScrollBar;
    public ScrollBar horizontalScrollBar;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    public Supplier<DrawableColor> backgroundColor = () -> getUIColorTheme().area_background_color;
    public Supplier<DrawableColor> borderColor = () -> getUIColorTheme().element_border_color_normal;
    protected float borderThickness = 1;
    public boolean makeEntriesWidthOfArea = false;
    public boolean minimumEntryWidthIsAreaWidth = true;
    public boolean makeAllEntriesWidthOfWidestEntry = true;
    protected List<ScrollAreaEntry> entries = new ArrayList<>();
    public float overriddenTotalScrollWidth = -1;
    public float overriddenTotalScrollHeight = -1;
    public boolean correctYOnAddingRemovingEntries = true;
    protected boolean applyScissor = true;
    /** Set this if the {@link ScrollArea} gets rendered in a custom-scaled render environment. **/
    @Nullable
    public Float renderScale = null;
    protected boolean hovered = false;
    protected boolean innerAreaHovered = false;

    public ScrollArea(float x, float y, float width, float height) {
        this.setX(x, true);
        this.setY(y, true);
        this.setWidth(width, true);
        this.setHeight(height, true);
        this.verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, VERTICAL_SCROLL_BAR_WIDTH, VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> getUIColorTheme().scroll_grabber_color_normal, () -> getUIColorTheme().scroll_grabber_color_hover);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, HORIZONTAL_SCROLL_BAR_WIDTH, HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> getUIColorTheme().scroll_grabber_color_normal, () -> getUIColorTheme().scroll_grabber_color_hover);
        this.updateScrollArea();
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.innerAreaHovered = this.isMouseOverInnerArea(mouseX, mouseY);

        this.updateScrollArea();
        this.updateWheelScrollSpeed();
        this.resetScrollOnFit();

        this.renderBackground(pose, mouseX, mouseY, partial);

        this.renderEntries(pose, mouseX, mouseY, partial);

        this.renderBorder(pose, mouseX, mouseY, partial);

        if (this.verticalScrollBar.active) {
            this.verticalScrollBar.render(pose, mouseX, mouseY, partial);
        }
        if (this.horizontalScrollBar.active) {
            this.horizontalScrollBar.render(pose, mouseX, mouseY, partial);
        }

    }

    public void renderBackground(PoseStack pose, int mouseX, int mouseY, float partial) {
        fillF(pose, this.getInnerX(), this.getInnerY(), this.getInnerX() + this.getInnerWidth(), this.getInnerY() + this.getInnerHeight(), this.backgroundColor.get().getColorInt());
    }

    public void renderBorder(PoseStack pose, int mouseX, int mouseY, float partial) {
        renderBorder(pose, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), this.borderColor.get().getColorInt(), true, true, true, true);
    }

    public void renderEntries(PoseStack pose, int mouseX, int mouseY, float partial) {
        
        if (this.isApplyScissor()) {
            //TODO this is probably broken if actually used (leftover from old scissor stuff)
            double scale = (this.renderScale != null) ? this.renderScale : 1;
//            float sciBottomY = this.getInnerY() + this.getInnerHeight();
//            RenderSystem.enableScissor((int)(this.getInnerX() * scale), (int)(win.getHeight() - (sciBottomY * scale)), (int)(this.getInnerWidth() * scale), (int)(this.getInnerHeight() * scale));
            int xMin = (int)(this.getInnerX() * scale);
            int yMin = (int)(this.getInnerY() * scale);
            int xMax = xMin + (int)this.getInnerWidth();
            int yMax = yMin + (int)this.getInnerHeight();
            this.enableScissor(xMin, yMin, xMax, yMax);
        }

        final float totalWidth = this.makeAllEntriesWidthOfWidestEntry ? this.getTotalEntryWidth() : 0;
        this.updateEntries((entry) -> {
            if (this.makeAllEntriesWidthOfWidestEntry) entry.setWidth(totalWidth);
            if (this.minimumEntryWidthIsAreaWidth && (entry.getWidth() < this.getInnerWidth())) {
                entry.setWidth(this.getInnerWidth());
            }
            entry.render(pose, mouseX, mouseY, partial);
        });

        if (this.isApplyScissor()) this.disableScissor();

    }

    public float getEntryRenderOffsetX() {
        return this.getEntryRenderOffsetX(this.getTotalScrollWidth());
    }

    public float getEntryRenderOffsetY() {
        return this.getEntryRenderOffsetY(this.getTotalScrollHeight());
    }

    public float getEntryRenderOffsetX(float totalScrollWidth) {
        return -((totalScrollWidth / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    public float getEntryRenderOffsetY(float totalScrollHeight) {
        return -((totalScrollHeight / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    public float getTotalScrollWidth() {
        if (this.overriddenTotalScrollWidth != -1) {
            return this.overriddenTotalScrollWidth;
        }
        return Math.max(0f, this.getTotalEntryWidth() - this.getInnerWidth());
    }

    public float getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return Math.max(0f, this.getTotalEntryHeight() - this.getInnerHeight());
    }

    public void updateEntries(@Nullable Consumer<ScrollAreaEntry> doAfterEachEntryUpdate) {
        try {
            int index = 0;
            float y = this.getInnerY();
            List<ScrollAreaEntry> l = new ArrayList<>(this.entries);
            for (ScrollAreaEntry e : l) {
                e.index = index;
                e.setX(this.getInnerX() + this.getEntryRenderOffsetX());
                e.setY(y + this.getEntryRenderOffsetY());
                if (this.makeEntriesWidthOfArea) {
                    e.setWidth(this.getInnerWidth());
                }
                if (doAfterEachEntryUpdate != null) {
                    doAfterEachEntryUpdate.accept(e);
                }
                index++;
                y += e.getHeight();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateScrollArea() {

        this.verticalScrollBar.scrollAreaStartX = this.getInnerX() + 1;
        this.verticalScrollBar.scrollAreaStartY = this.getInnerY() + 1;
        this.verticalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - 1;
        this.verticalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - this.horizontalScrollBar.grabberHeight - 2;

        this.horizontalScrollBar.scrollAreaStartX = this.getInnerX() + 1;
        this.horizontalScrollBar.scrollAreaStartY = this.getInnerY() + 1;
        this.horizontalScrollBar.scrollAreaEndX = this.getInnerX() + this.getInnerWidth() - this.verticalScrollBar.grabberWidth - 2;
        this.horizontalScrollBar.scrollAreaEndY = this.getInnerY() + this.getInnerHeight() - 1;

    }

    public void updateWheelScrollSpeed() {
        //Adjust the scroll wheel speed depending on the amount of entries
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / (this.getTotalScrollHeight() / 500.0F));
    }

    public void resetScrollOnFit() {
        //Reset scrolls if content fits area
        if (this.getTotalEntryWidth() <= this.getInnerWidth()) {
            this.horizontalScrollBar.setScroll(0.0F);
        }
        if (this.getTotalEntryHeight() <= this.getInnerHeight()) {
            this.verticalScrollBar.setScroll(0.0F);
        }
    }

    /**
     * Corrects the Y scroll after removing or adding entries.
     * @param removed If TRUE, the entry list will be treated as list of REMOVED entries. If FALSE, the list is treated as ADDED entries.
     * @param addedOrRemovedEntries List of entries that were added or removed.
     */
    public void correctYScrollAfterAddingOrRemovingEntries(boolean removed, ScrollAreaEntry... addedOrRemovedEntries) {
        if ((addedOrRemovedEntries != null) && (addedOrRemovedEntries.length > 0)) {
            float oldTotalScrollHeight;
            int totalHeightRemovedAdded = 0;
            for (ScrollAreaEntry e : addedOrRemovedEntries) {
                totalHeightRemovedAdded += e.getHeight();
            }
            if (!removed) {
                oldTotalScrollHeight = this.getTotalScrollHeight() - totalHeightRemovedAdded;
            } else {
                oldTotalScrollHeight = this.getTotalScrollHeight() + totalHeightRemovedAdded;
            }
            float yOld = this.getEntryRenderOffsetY(oldTotalScrollHeight);
            float yNew = this.getEntryRenderOffsetY();
            float yDiff = Math.max(yOld, yNew) - Math.min(yOld, yNew);
            if (this.getTotalScrollHeight() <= 0) {
                return;
            }
            float scrollDiff = Math.max(0f, Math.min(1f, yDiff / this.getTotalScrollHeight()));
            if (!removed) {
                scrollDiff = -scrollDiff;
            }
            this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + scrollDiff);
        }
    }

    public boolean isMouseInteractingWithGrabbers() {
        return this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered() || this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered();
    }

    public void setX(float x, boolean respectBorder) {
        this.x = x;
        if (respectBorder) {
            this.x += this.borderThickness;
        }
    }

    public void setX(float x) {
        this.setX(x, true);
    }

    public float getInnerX() {
        return this.x;
    }

    public float getXWithBorder() {
        return this.x - this.borderThickness;
    }

    public void setY(float y, boolean respectBorder) {
        this.y = y;
        if (respectBorder) {
            this.y += this.borderThickness;
        }
    }

    public void setY(float y) {
        this.setY(y, true);
    }

    public float getInnerY() {
        return this.y;
    }

    public float getYWithBorder() {
        return this.y - this.borderThickness;
    }

    public void setWidth(float width, boolean respectBorder) {
        this.width = width;
        if (respectBorder) {
            this.width -= (this.borderThickness * 2);
        }
    }

    public void setWidth(float width) {
        this.setWidth(width, true);
    }

    public float getInnerWidth() {
        return this.width;
    }

    public float getWidthWithBorder() {
        return this.width + (this.borderThickness * 2);
    }

    public void setHeight(float height, boolean respectBorder) {
        this.height = height;
        if (respectBorder) {
            this.height -= (this.borderThickness * 2);
        }
    }

    public void setHeight(float height) {
        this.setHeight(height, true);
    }

    public float getInnerHeight() {
        return this.height;
    }

    public float getHeightWithBorder() {
        return this.height + (this.borderThickness * 2);
    }

    public void setBorderThickness(float borderThickness) {
        this.borderThickness = borderThickness;
    }

    public float getBorderThickness() {
        return this.borderThickness;
    }

    public boolean isInnerAreaHovered() {
        return this.innerAreaHovered;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isMouseOverInnerArea(double mouseX, double mouseY) {
        return isXYInArea(mouseX, mouseY, this.getInnerX(), this.getInnerY(), this.getInnerWidth(), this.getInnerHeight());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isMouseInteractingWithGrabbers()) return true;
        return isXYInArea(mouseX, mouseY, this.getXWithBorder(), this.getYWithBorder(), this.getWidthWithBorder(), this.getHeightWithBorder());
    }

    public int getEntryCount() {
        return this.entries.size();
    }

    public float getTotalEntryWidth() {
        float i = this.width;
        for (ScrollAreaEntry e : this.entries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    public float getTotalEntryHeight() {
        float i = 0;
        for (ScrollAreaEntry e : this.entries) {
            i += e.getHeight();
        }
        return i;
    }

    @Nullable
    public ScrollAreaEntry getFocusedEntry() {
        for (ScrollAreaEntry e : this.entries) {
            if (e.isSelected()) {
                return e;
            }
        }
        return null;
    }

    /**
     * @return The index of the focused entry or -1 if no entry is focused.
     */
    public int getFocusedEntryIndex() {
        ScrollAreaEntry e = this.getFocusedEntry();
        if (e != null) {
            return this.getIndexOfEntry(e);
        }
        return -1;
    }

    public List<ScrollAreaEntry> getEntries() {
        return new ArrayList<>(this.entries);
    }

    @Nullable
    public ScrollAreaEntry getEntry(int index) {
        if (index <= this.entries.size()-1) {
            return this.entries.get(index);
        }
        return null;
    }

    public void addEntry(ScrollAreaEntry entry) {
        if (!this.entries.contains(entry)) {
            this.entries.add(entry);
            if (this.correctYOnAddingRemovingEntries) {
                this.correctYScrollAfterAddingOrRemovingEntries(false, entry);
            }
        }
        this.makeCurrentEntriesSameWidth();
    }

    public void addEntryAtIndex(ScrollAreaEntry entry, int index) {
        if (index > this.getEntryCount()) {
            index = this.getEntryCount();
        }
        this.entries.add(index, entry);
        if (this.correctYOnAddingRemovingEntries) {
            this.correctYScrollAfterAddingOrRemovingEntries(false, entry);
        }
        this.makeCurrentEntriesSameWidth();
    }

    public void removeEntry(ScrollAreaEntry entry) {
        this.entries.remove(entry);
        if (this.correctYOnAddingRemovingEntries) {
            this.correctYScrollAfterAddingOrRemovingEntries(true, entry);
        }
        this.makeCurrentEntriesSameWidth();
    }

    public void removeEntryAtIndex(int index) {
        if (index <= this.getEntryCount()-1) {
            ScrollAreaEntry entry = this.entries.remove(index);
            if ((entry != null) && this.correctYOnAddingRemovingEntries) {
                this.correctYScrollAfterAddingOrRemovingEntries(true, entry);
            }
        }
        this.makeCurrentEntriesSameWidth();
    }

    public void clearEntries() {
        this.entries.clear();
        this.verticalScrollBar.setScroll(0.0F);
        this.horizontalScrollBar.setScroll(0.0F);
    }

    /**
     * @return The index of the entry or -1 if the entry is not part of the ScrollArea.
     */
    public int getIndexOfEntry(ScrollAreaEntry entry) {
        return this.entries.indexOf(entry);
    }

    public void makeCurrentEntriesSameWidth() {
        float totalWidth = this.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

    public boolean isApplyScissor() {
        return this.applyScissor;
    }

    public void setApplyScissor(boolean apply) {
        this.applyScissor = apply;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        for (ScrollAreaEntry entry : this.entries) {
            if (entry.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.verticalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        if (this.horizontalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
        if (this.verticalScrollBar.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;
        if (this.horizontalScrollBar.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (this.verticalScrollBar.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
        if (this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, scrollDelta)) return true;
        return false;
    }

    @Override
    public void setFocused(boolean var1) {
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
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

}
