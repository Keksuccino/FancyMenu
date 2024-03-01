package de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollbar.ScrollBar;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class ScrollArea extends UIBase {

    private static final Logger LOGGER = LogManager.getLogger();

    public ScrollBar verticalScrollBar;
    public ScrollBar horizontalScrollBar;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    public Color backgroundColor = UIBase.getUIColorTheme().area_background_color.getColor();
    public Color borderColor = UIBase.getUIColorTheme().element_border_color_normal.getColor();
    protected int borderThickness = 1;
    public boolean makeEntriesWidthOfArea = false;
    public boolean minimumEntryWidthIsAreaWidth = true;
    protected List<ScrollAreaEntry> entries = new ArrayList<>();
    public int overriddenTotalScrollWidth = -1;
    public int overriddenTotalScrollHeight = -1;
    public boolean correctYOnAddingRemovingEntries = true;
    public float customGuiScale = -1F;

    public ScrollArea(int x, int y, int width, int height) {
        this.setX(x, true);
        this.setY(y, true);
        this.setWidth(width, true);
        this.setHeight(height, true);
        this.verticalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.VERTICAL, VERTICAL_SCROLL_BAR_WIDTH, VERTICAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, UIBase.getUIColorTheme().scroll_grabber_color_normal.getColor(), UIBase.getUIColorTheme().scroll_grabber_color_hover.getColor());
        this.verticalScrollBar.setScrollWheelAllowed(true);
        this.horizontalScrollBar = new ScrollBar(ScrollBar.ScrollBarDirection.HORIZONTAL, HORIZONTAL_SCROLL_BAR_WIDTH, HORIZONTAL_SCROLL_BAR_HEIGHT, 0, 0, 0, 0, UIBase.getUIColorTheme().scroll_grabber_color_normal.getColor(), UIBase.getUIColorTheme().scroll_grabber_color_hover.getColor());
        this.updateScrollArea();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateScrollArea();

        this.updateWheelScrollSpeed();

        this.resetScrollOnFit();

        this.renderBackground(graphics, mouseX, mouseY, partial);

        this.renderEntries(graphics, mouseX, mouseY, partial);

        this.renderBorder(graphics, mouseX, mouseY, partial);

        if (this.verticalScrollBar.active) {
            this.verticalScrollBar.render(graphics);
        }
        if (this.horizontalScrollBar.active) {
            this.horizontalScrollBar.render(graphics);
        }

    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(this.getInnerX(), this.getInnerY(), this.getInnerX() + this.getInnerWidth(), this.getInnerY() + this.getInnerHeight(), this.backgroundColor.getRGB());
    }

    public void renderBorder(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        renderBorder(graphics, this.getXWithBorder(), this.getYWithBorder(), this.getXWithBorder() + this.getWidthWithBorder(), this.getYWithBorder() + this.getHeightWithBorder(), this.getBorderThickness(), this.borderColor, true, true, true, true);
    }

    public void renderEntries(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        Window win = Minecraft.getInstance().getWindow();
        
        double scale = (this.customGuiScale != -1F) ? this.customGuiScale : win.getGuiScale();
        int sciBottomY = this.getInnerY() + this.getInnerHeight();
        RenderSystem.enableScissor((int)(this.getInnerX() * scale), (int)(win.getHeight() - (sciBottomY * scale)), (int)(this.getInnerWidth() * scale), (int)(this.getInnerHeight() * scale));

        this.updateEntries((entry) -> {
            int cachedWidth = -1;
            if (this.minimumEntryWidthIsAreaWidth) {
                cachedWidth = entry.getWidth();
                if (cachedWidth < this.getInnerWidth()) {
                    entry.setWidth(this.getInnerWidth());
                }
            }
            entry.render(graphics, mouseX, mouseY, partial);
            if (cachedWidth != -1) {
                entry.setWidth(cachedWidth);
            }
        });

        RenderSystem.disableScissor();

    }

    public int getEntryRenderOffsetX() {
        return this.getEntryRenderOffsetX(this.getTotalScrollWidth());
    }

    public int getEntryRenderOffsetY() {
        return this.getEntryRenderOffsetY(this.getTotalScrollHeight());
    }

    public int getEntryRenderOffsetX(float totalScrollWidth) {
        return -(int)((totalScrollWidth / 100.0F) * (this.horizontalScrollBar.getScroll() * 100.0F));
    }

    public int getEntryRenderOffsetY(float totalScrollHeight) {
        return -(int)((totalScrollHeight / 100.0F) * (this.verticalScrollBar.getScroll() * 100.0F));
    }

    public int getTotalScrollWidth() {
        if (this.overriddenTotalScrollWidth != -1) {
            return this.overriddenTotalScrollWidth;
        }
        return Math.max(0, this.getTotalEntryWidth() - this.getInnerWidth());
    }

    public int getTotalScrollHeight() {
        if (this.overriddenTotalScrollHeight != -1) {
            return this.overriddenTotalScrollHeight;
        }
        return Math.max(0, this.getTotalEntryHeight() - this.getInnerHeight());
    }

    public void updateEntries(@Nullable Consumer<ScrollAreaEntry> doAfterEachEntryUpdate) {
        try {
            int index = 0;
            int y = this.getInnerY();
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
        this.verticalScrollBar.setWheelScrollSpeed(1.0F / ((float)this.getTotalScrollHeight() / 500.0F));
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
            int yOld = this.getEntryRenderOffsetY(oldTotalScrollHeight);
            int yNew = this.getEntryRenderOffsetY();
            int yDiff = Math.max(yOld, yNew) - Math.min(yOld, yNew);
            if (this.getTotalScrollHeight() <= 0) {
                return;
            }
            float scrollDiff = Math.max(0.0F, Math.min(1.0F, (float)yDiff / (float)this.getTotalScrollHeight()));
            if (!removed) {
                scrollDiff = -scrollDiff;
            }
            this.verticalScrollBar.setScroll(this.verticalScrollBar.getScroll() + scrollDiff);
        }
    }

    public boolean isMouseInteractingWithGrabbers() {
        return this.verticalScrollBar.isGrabberGrabbed() || this.verticalScrollBar.isGrabberHovered() || this.horizontalScrollBar.isGrabberGrabbed() || this.horizontalScrollBar.isGrabberHovered();
    }

    public void setX(int x, boolean respectBorder) {
        this.x = x;
        if (respectBorder) {
            this.x += this.borderThickness;
        }
    }

    public void setX(int x) {
        this.setX(x, true);
    }

    public int getInnerX() {
        return this.x;
    }

    public int getXWithBorder() {
        return this.x - this.borderThickness;
    }

    public void setY(int y, boolean respectBorder) {
        this.y = y;
        if (respectBorder) {
            this.y += this.borderThickness;
        }
    }

    public void setY(int y) {
        this.setY(y, true);
    }

    public int getInnerY() {
        return this.y;
    }

    public int getYWithBorder() {
        return this.y - this.borderThickness;
    }

    public void setWidth(int width, boolean respectBorder) {
        this.width = width;
        if (respectBorder) {
            this.width -= (this.borderThickness * 2);
        }
    }

    public void setWidth(int width) {
        this.setWidth(width, true);
    }

    public int getInnerWidth() {
        return this.width;
    }

    public int getWidthWithBorder() {
        return this.width + (this.borderThickness * 2);
    }

    public void setHeight(int height, boolean respectBorder) {
        this.height = height;
        if (respectBorder) {
            this.height -= (this.borderThickness * 2);
        }
    }

    public void setHeight(int height) {
        this.setHeight(height, true);
    }

    public int getInnerHeight() {
        return this.height;
    }

    public int getHeightWithBorder() {
        return this.height + (this.borderThickness * 2);
    }

    public void setBorderThickness(int borderThickness) {
        this.borderThickness = borderThickness;
    }

    public int getBorderThickness() {
        return this.borderThickness;
    }

    public boolean isMouseInsideArea() {
        int mX = MouseInput.getMouseX();
        int mY = MouseInput.getMouseY();
        return (mX >= this.getInnerX()) && (mX <= this.getInnerX() + this.getInnerWidth()) && (mY >= this.getInnerY()) && (mY <= this.getInnerY() + this.getInnerHeight());
    }

    public int getEntryCount() {
        return this.entries.size();
    }

    public int getTotalEntryWidth() {
        int i = this.width;
        for (ScrollAreaEntry e : this.entries) {
            if (e.getWidth() > i) {
                i = e.getWidth();
            }
        }
        return i;
    }

    public int getTotalEntryHeight() {
        int i = 0;
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
        int totalWidth = this.getTotalEntryWidth();
        for (ScrollAreaEntry e : this.getEntries()) {
            e.setWidth(totalWidth);
        }
    }

}
