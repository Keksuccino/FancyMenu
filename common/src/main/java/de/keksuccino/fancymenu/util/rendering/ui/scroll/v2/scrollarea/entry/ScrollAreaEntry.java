package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class ScrollAreaEntry extends UIBase implements Renderable {

    /** Bigger than the anti-aliasing fringe of rounded shapes, so pushing a clip edge out by this keeps the fringe intact. */
    private static final float AREA_EDGE_CLIP_MARGIN = 4.0F;

    public ScrollArea parent;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    @Nullable
    protected Supplier<DrawableColor> backgroundColorNormal = () -> {
        if (this.parent.isSetupForBlurInterface() && UIBase.shouldBlur()) return DrawableColor.FULLY_TRANSPARENT;
        return getUITheme().ui_interface_area_background_color_type_1;
    };
    @Nullable
    protected Supplier<DrawableColor> backgroundColorHover = () -> {
        if (this.parent.isSetupForBlurInterface() && UIBase.shouldBlur()) return getUITheme().ui_blur_interface_area_entry_selected_color;
        return getUITheme().ui_interface_area_entry_selected_color;
    };
    @Nullable
    protected UITooltip tooltip;
    protected boolean selectable = true;
    protected boolean selected = false;
    protected boolean clickable = true;
    protected boolean playClickSound = true;
    public boolean deselectOtherEntriesOnSelect = true;
    public boolean selectOnClick = true;
    public int index = 0;
    protected boolean hovered = false;
    protected int lastHoverUpdateFrameId = -1;

    public ScrollAreaEntry(ScrollArea parent, float width, float height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
    }

    public abstract void renderEntry(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial);

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        this.hovered = this.isMouseOver(mouseX, mouseY);
        if (this.parent != null) {
            this.lastHoverUpdateFrameId = this.parent.getRenderFrameId();
        }
        if (this.hovered && (this.tooltip != null)) TooltipHandler.INSTANCE.addRenderTickTooltip(this.tooltip, () -> true);
        this.extractBackground(graphics, mouseX, mouseY, partial);
        this.renderEntry(graphics, mouseX, mouseY, partial);
    }

    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (!this.isHovered() && !this.isSelected()) {
            if (this.backgroundColorNormal != null) {
                DrawableColor c = this.backgroundColorNormal.get();
                if (c != null) this.renderRoundedEntryBackground(graphics, partial, c.getColorInt());
            }
        } else if (this.backgroundColorHover != null) {
            DrawableColor c = this.backgroundColorHover.get();
            if (c != null) this.renderRoundedEntryBackground(graphics, partial, c.getColorInt());
        }
    }

    /**
     * Fills the part of the parent's inner area that this entry covers, edge to edge. In rounded style, the fill is the area background shape
     * cut to the entry bounds, so entries at the top and bottom follow the rounded corners of the area, and the fills of all entries together
     * line up exactly with the area background, anti-aliased edges included.
     */
    protected void renderRoundedEntryBackground(@NotNull GuiGraphicsExtractor graphics, float partial, int color) {
        if ((color >>> 24) == 0) return; // fully transparent
        if (this.parent == null) {
            fillF(graphics, this.x, this.y, this.x + this.width, this.y + this.height, color);
            return;
        }
        float areaX = this.parent.getInnerX();
        float areaY = this.parent.getInnerY();
        float areaRight = areaX + this.parent.getInnerWidth();
        float areaBottom = areaY + this.parent.getInnerHeight();
        float entryRight = this.x + this.width;
        float entryBottom = this.y + this.height;
        if ((entryRight <= areaX) || (this.x >= areaRight) || (entryBottom <= areaY) || (this.y >= areaBottom)) return;
        boolean scissorLifted = this.parent.liftEntryScissor(graphics);
        try {
            if (this.parent.isRoundedStyle()) {
                // Entry edges that reach the area edges don't cut the shape, so the anti-aliased area edges stay intact there
                float clipMinX = (this.x <= areaX) ? (areaX - AREA_EDGE_CLIP_MARGIN) : this.x;
                float clipMinY = (this.y <= areaY) ? (areaY - AREA_EDGE_CLIP_MARGIN) : this.y;
                float clipMaxX = (entryRight >= areaRight) ? (areaRight + AREA_EDGE_CLIP_MARGIN) : entryRight;
                float clipMaxY = (entryBottom >= areaBottom) ? (areaBottom + AREA_EDGE_CLIP_MARGIN) : entryBottom;
                float radius = this.parent.getInnerCornerRadius();
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersClippedScaled(graphics, areaX, areaY, areaRight - areaX, areaBottom - areaY, radius, radius, radius, radius, clipMinX, clipMinY, clipMaxX, clipMaxY, color, partial);
            } else {
                fillF(graphics, Math.max(this.x, areaX), Math.max(this.y, areaY), Math.min(entryRight, areaRight), Math.min(entryBottom, areaBottom), color);
            }
        } finally {
            if (scissorLifted) this.parent.restoreEntryScissor(graphics);
        }
    }

    public abstract void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isClickable() && this.isHovered() && !this.parent.isMouseInteractingWithGrabbers() && this.parent.isInnerAreaHovered()) {
            if ((button == 0) && this.selectOnClick) {
                this.setSelected(true);
            }
            if ((button == 0) && this.playClickSound) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.onClick(this, mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return isXYInArea(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getX() {
        return this.x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getY() {
        return this.y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getWidth() {
        return this.width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getHeight() {
        return this.height;
    }

    public boolean isHovered() {
        if (this.parent != null && this.lastHoverUpdateFrameId != this.parent.getRenderFrameId()) return false;
        if (!this.parent.isInnerAreaHovered()) return false;
        if (this.parent.isMouseInteractingWithGrabbers()) return false;
        return this.hovered;
    }

    public boolean isSelected() {
        return this.selectable && this.selected;
    }

    public void setSelected(boolean selected) {
        if (this.selectable) {
            this.selected = selected;
            if (selected && this.deselectOtherEntriesOnSelect) {
                for (ScrollAreaEntry e : this.parent.getEntries()) {
                    if (e != this) {
                        e.setSelected(false);
                    }
                }
            }
        }
    }

    public boolean isSelectable() {
        return this.selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        if (!selectable) {
            this.selected = false;
        }
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
    }

    public boolean isClickable() {
        return this.clickable;
    }

    public void setPlayClickSound(boolean playClickSound) {
        this.playClickSound = playClickSound;
    }

    public boolean isPlayClickSound() {
        return this.playClickSound;
    }

    @Nullable
    public Supplier<DrawableColor> getBackgroundColorNormal() {
        return this.backgroundColorNormal;
    }

    public void setBackgroundColorNormal(@Nullable Supplier<DrawableColor> backgroundColorNormal) {
        this.backgroundColorNormal = backgroundColorNormal;
    }

    @Nullable
    public Supplier<DrawableColor> getBackgroundColorHover() {
        return this.backgroundColorHover;
    }

    public void setBackgroundColorHover(@Nullable Supplier<DrawableColor> backgroundColorHover) {
        this.backgroundColorHover = backgroundColorHover;
    }

    public void setTooltip(@Nullable UITooltip UITooltip) {
        this.tooltip = UITooltip;
    }

}

