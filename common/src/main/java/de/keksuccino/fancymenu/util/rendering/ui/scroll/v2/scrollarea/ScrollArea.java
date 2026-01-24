package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollbar.ScrollBar;
import net.minecraft.client.gui.GuiGraphics;
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

@SuppressWarnings("unused")
public class ScrollArea implements GuiEventListener, Renderable, NarratableEntry {

    private static final Logger LOGGER = LogManager.getLogger();

    public final ScrollBar verticalScrollBar;
    public final ScrollBar horizontalScrollBar;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    @Nullable
    public Supplier<DrawableColor> backgroundColor = () -> this.setupForBlurInterface ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1 : UIBase.getUITheme().ui_interface_area_background_color_type_1;
    @Nullable
    public Supplier<DrawableColor> borderColor = () -> this.setupForBlurInterface ? UIBase.getUITheme().ui_blur_interface_area_border_color : UIBase.getUITheme().ui_interface_area_border_color;
    protected float borderThickness = 1;
    public boolean makeEntriesWidthOfArea = false;
    public boolean minimumEntryWidthIsAreaWidth = true;
    public boolean makeAllEntriesWidthOfWidestEntry = true;
    protected List<ScrollAreaEntry> entries = new ArrayList<>();
    public float overriddenTotalScrollWidth = -1;
    public float overriddenTotalScrollHeight = -1;
    public boolean correctYOnAddingRemovingEntries = true;
    protected boolean hovered = false;
    protected boolean innerAreaHovered = false;
    protected boolean roundedStyle = true;
    protected boolean setupForBlurInterface = false;
    protected boolean scissorEnabled = true;
    private int renderFrameId = 0;

    public ScrollArea(float x, float y, float width, float height) {
        this.setX(x, true);
        this.setY(y, true);
        this.setWidth(width, true);
        this.setHeight(height, true);
        this.verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, UIBase.VERTICAL_SCROLL_BAR_WIDTH, UIBase.VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.verticalScrollBar.setRoundedGrabberEnabled(true);
        this.horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, UIBase.HORIZONTAL_SCROLL_BAR_WIDTH, UIBase.HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, () -> UIBase.getUITheme().scroll_grabber_color_normal, () -> UIBase.getUITheme().scroll_grabber_color_hover);
        this.horizontalScrollBar.setRoundedGrabberEnabled(true);
        this.updateScrollArea();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.hovered = this.isMouseOver(mouseX, mouseY);
        this.innerAreaHovered = this.isMouseOverInnerArea(mouseX, mouseY);

        this.updateScrollArea();
        this.updateWheelScrollSpeed();
        this.resetScrollOnFit();

        this.renderBackground(graphics, mouseX, mouseY, partial);

        this.renderEntries(graphics, mouseX, mouseY, partial);

        this.renderBorder(graphics, mouseX, mouseY, partial);

        if (this.isVerticalScrollBarVisible()) {
            this.verticalScrollBar.render(graphics, mouseX, mouseY, partial);
        }
        if (this.isHorizontalScrollBarVisible()) {
            this.horizontalScrollBar.render(graphics, mouseX, mouseY, partial);
        }

    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.backgroundColor == null) return;
        DrawableColor backColor = this.backgroundColor.get();
        if (backColor != null) {
            if (this.roundedStyle) {
                float radius = UIBase.getInterfaceCornerRoundingRadius();
                UIBase.renderRoundedRect(graphics, this.getInnerX(), this.getInnerY(), this.getInnerWidth(), this.getInnerHeight(), radius, radius, radius, radius, backColor.getColorInt());
            } else {
                UIBase.fillF(graphics, this.getInnerX(), this.getInnerY(), this.getInnerX() + this.getInnerWidth(), this.getInnerY() + this.getInnerHeight(), backColor.getColorInt());
            }
        }
    }

    public void renderBorder(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (this.borderColor == null) return;
        DrawableColor borColor = this.borderColor.get();
        if (borColor != null) {
            if (this.roundedStyle) {
                float radius = UIBase.getInterfaceCornerRoundingRadius();
                UIBase.renderRoundedBorder(graphics, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), radius, radius, radius, radius, borColor.getColorInt());
            } else {
                UIBase.renderBorder(graphics, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), borColor.getColorInt(), true, true, true, true);
            }
        }
    }

    public void renderEntries(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.renderFrameId++;

        final float totalWidth = this.makeAllEntriesWidthOfWidestEntry ? this.getTotalEntryWidth() : 0;
        if (this.scissorEnabled) {
            int scissorMinX = (int) (this.getInnerX() + 2);
            int scissorMinY = (int) (this.getInnerY() + 2);
            int scissorMaxX = (int) (this.getInnerX() + this.getInnerWidth() - 2);
            int scissorMaxY = (int) (this.getInnerY() + this.getInnerHeight() - 2);
            graphics.enableScissor(scissorMinX, scissorMinY, scissorMaxX, scissorMaxY);
        }
        this.updateEntriesForRender((entry) -> {
            if (this.makeAllEntriesWidthOfWidestEntry) entry.setWidth(totalWidth);
            if (this.minimumEntryWidthIsAreaWidth && (entry.getWidth() < this.getInnerWidth())) {
                entry.setWidth(this.getInnerWidth());
            }
            if (this.isEntryVisible(entry)) {
                entry.render(graphics, mouseX, mouseY, partial);
            }
        });
        if (this.scissorEnabled) {
            graphics.disableScissor();
        }

    }

    public int getRenderFrameId() {
        return this.renderFrameId;
    }
    private boolean isEntryVisible(@NotNull ScrollAreaEntry entry) {
        float innerX = this.getInnerX();
        float innerY = this.getInnerY();
        float innerMaxX = innerX + this.getInnerWidth();
        float innerMaxY = innerY + this.getInnerHeight();
        float entryMinX = entry.getX();
        float entryMinY = entry.getY();
        float entryMaxX = entryMinX + entry.getWidth();
        float entryMaxY = entryMinY + entry.getHeight();
        return (entryMaxX > innerX) && (entryMinX < innerMaxX) && (entryMaxY > innerY) && (entryMinY < innerMaxY);
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
        this.updateEntriesInternal(doAfterEachEntryUpdate, false);
    }

    private void updateEntriesForRender(@NotNull Consumer<ScrollAreaEntry> doAfterEachEntryUpdate) {
        this.updateEntriesInternal(doAfterEachEntryUpdate, true);
    }

    private void updateEntriesInternal(@Nullable Consumer<ScrollAreaEntry> doAfterEachEntryUpdate, boolean stopAfterVisibleRange) {
        try {
            int index = 0;
            float y = this.getInnerY();
            float renderOffsetX = this.getEntryRenderOffsetX();
            float renderOffsetY = this.getEntryRenderOffsetY();
            float innerMaxY = this.getInnerY() + this.getInnerHeight();
            List<ScrollAreaEntry> l = new ArrayList<>(this.entries);
            for (ScrollAreaEntry e : l) {
                e.index = index;
                e.setX(this.getInnerX() + renderOffsetX);
                e.setY(y + renderOffsetY);
                if (this.makeEntriesWidthOfArea) {
                    e.setWidth(this.getInnerWidth());
                }
                if (doAfterEachEntryUpdate != null) {
                    doAfterEachEntryUpdate.accept(e);
                }
                index++;
                y += e.getHeight();
                if (stopAfterVisibleRange && ((y + renderOffsetY) > innerMaxY)) {
                    break;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to update entries!", ex);
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
                totalHeightRemovedAdded += (int) e.getHeight();
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
        return (this.isVerticalScrollBarVisible() && (this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered()))
                || (this.isHorizontalScrollBarVisible() && (this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered()));
    }

    public boolean isVerticalScrollBarVisible() {
        return this.verticalScrollBar.active && (this.getTotalScrollHeight() > 0.0F);
    }

    public boolean isHorizontalScrollBarVisible() {
        return this.horizontalScrollBar.active && (this.getTotalScrollWidth() > 0.0F);
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

    public boolean isRoundedStyle() {
        return this.roundedStyle;
    }

    public ScrollArea setRoundedStyleEnabled(boolean rounded) {
        this.roundedStyle = rounded;
        this.verticalScrollBar.setRoundedGrabberEnabled(rounded);
        this.horizontalScrollBar.setRoundedGrabberEnabled(rounded);
        return this;
    }

    public boolean isSetupForBlurInterface() {
        return this.setupForBlurInterface;
    }

    public ScrollArea setSetupForBlurInterface(boolean setupForBlurInterface) {
        this.setupForBlurInterface = setupForBlurInterface;
        return this;
    }

    public ScrollArea setScissorEnabled(boolean scissorEnabled) {
        this.scissorEnabled = scissorEnabled;
        return this;
    }

    public boolean isScissorEnabled() {
        return this.scissorEnabled;
    }

    public boolean isMouseOverInnerArea(double mouseX, double mouseY) {
        return UIBase.isXYInArea(mouseX, mouseY, this.getInnerX(), this.getInnerY(), this.getInnerWidth(), this.getInnerHeight());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.isMouseInteractingWithGrabbers()) return true;
        return UIBase.isXYInArea(mouseX, mouseY, this.getXWithBorder(), this.getYWithBorder(), this.getWidthWithBorder(), this.getHeightWithBorder());
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
        for (ScrollAreaEntry entry : this.entries) {
            if (entry.mouseClicked(mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseReleased(mouseX, mouseY, button)) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double $$3, double $$4) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseDragged(mouseX, mouseY, button, $$3, $$4)) return true;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (this.isVerticalScrollBarVisible() && this.verticalScrollBar.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
        if (this.isHorizontalScrollBarVisible() && this.horizontalScrollBar.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY)) return true;
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
