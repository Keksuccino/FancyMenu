package de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class ScrollAreaEntry extends UIBase implements Renderable {

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

    public abstract void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.hovered = this.isMouseOver(mouseX, mouseY);
        if (this.parent != null) {
            this.lastHoverUpdateFrameId = this.parent.getRenderFrameId();
        }
        if (this.hovered && (this.tooltip != null)) TooltipHandler.INSTANCE.addRenderTickTooltip(this.tooltip, () -> true);
        this.renderBackground(graphics, mouseX, mouseY, partial);
        this.renderEntry(graphics, mouseX, mouseY, partial);
    }

    protected void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
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

    protected void renderRoundedEntryBackground(@NotNull GuiGraphics graphics, float partial, int color) {
        if (this.parent == null || !this.parent.isRoundedStyle()) {
            fillF(graphics, this.x, this.y, this.x + this.width, this.y + this.height, color);
            return;
        }
        float areaY = this.parent.getInnerY() + 2;
        float areaBottom = this.parent.getInnerY() + this.parent.getInnerHeight() - 2;
        float entryY = this.y;
        float entryHeight = this.height;
        float visibleTop = Math.max(entryY, areaY);
        float visibleBottom = Math.min(entryY + entryHeight, areaBottom);
        float visibleHeight = visibleBottom - visibleTop;
        if (visibleHeight <= 0) {
            return;
        }
        boolean roundTop = entryY <= areaY + 5;
        boolean roundBottom = (entryY + entryHeight) >= areaBottom - 5;
        if (roundTop || roundBottom) {
            float radius = UIBase.getInterfaceCornerRoundingRadius();
            if (roundTop && roundBottom) {
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(graphics, this.x, visibleTop, this.width, visibleHeight, radius, radius, radius, radius, color, partial);
            } else if (roundTop) {
                SmoothRectangleRenderer.renderSmoothRectRoundTopCornersScaled(graphics, this.x, visibleTop, this.width, visibleHeight, radius, color, partial);
            } else {
                SmoothRectangleRenderer.renderSmoothRectRoundBottomCornersScaled(graphics, this.x, visibleTop, this.width, visibleHeight, radius, color, partial);
            }
        } else {
            fillF(graphics, this.x, visibleTop, this.x + this.width, visibleTop + visibleHeight, color);
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
        this.clickable = true;
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
