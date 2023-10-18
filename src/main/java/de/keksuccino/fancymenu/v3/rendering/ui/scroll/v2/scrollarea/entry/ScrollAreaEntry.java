package de.keksuccino.fancymenu.v3.rendering.ui.scroll.v2.scrollarea.entry;

import de.keksuccino.fancymenu.v3.rendering.DrawableColor;
import de.keksuccino.fancymenu.v3.rendering.ui.UIBase;
import de.keksuccino.fancymenu.v3.rendering.ui.scroll.v2.scrollarea.ScrollArea;
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
    protected Supplier<DrawableColor> backgroundColorNormal = () -> area_background_color;
    @Nullable
    protected Supplier<DrawableColor> backgroundColorHover = () -> list_entry_color_selected_hovered;
//    @Nullable
//    protected Tooltip tooltip;
    protected boolean selectable = true;
    protected boolean selected = false;
    protected boolean clickable = true;
    protected boolean playClickSound = true;
    public boolean deselectOtherEntriesOnSelect = true;
    public boolean selectOnClick = true;
    public int index = 0;
    protected boolean hovered = false;

    public ScrollAreaEntry(ScrollArea parent, float width, float height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
    }

    public abstract void renderEntry(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial);

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.hovered = this.isMouseOver(mouseX, mouseY);
//        if (this.hovered && (this.tooltip != null)) TooltipHandler.INSTANCE.addTooltip(this.tooltip, () -> true, false, true);
        this.renderBackground(graphics, mouseX, mouseY, partial);
        this.renderEntry(graphics, mouseX, mouseY, partial);
    }

    protected void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (!this.isHovered() && !this.isSelected()) {
            if (this.backgroundColorNormal != null) {
                DrawableColor c = this.backgroundColorNormal.get();
                if (c != null) fillF(graphics.pose(), this.x, this.y, this.x + this.width, this.y + this.height, c.getColorInt());
            }
        } else if (this.backgroundColorHover != null) {
            DrawableColor c = this.backgroundColorHover.get();
            if (c != null) fillF(graphics.pose(), this.x, this.y, this.x + this.width, this.y + this.height, c.getColorInt());
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

//    public void setTooltip(@Nullable Tooltip tooltip) {
//        this.tooltip = tooltip;
//    }

}
