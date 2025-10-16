package de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.util.Objects;

@SuppressWarnings("unused")
public abstract class ScrollAreaEntry extends UIBase implements Renderable {

    public ScrollArea parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    public ExtendedButton buttonBase;
    protected DrawableColor backgroundColorIdle = UIBase.getUIColorTheme().area_background_color;
    protected DrawableColor backgroundColorHover = UIBase.getUIColorTheme().list_entry_color_selected_hovered;
    protected boolean selectable = true;
    protected boolean selected = false;
    protected boolean playClickSound = true;
    public boolean deselectOtherEntriesOnSelect = true;
    public boolean selectOnClick = true;
    public int index = 0;

    /** Shared boolean to not chain-click entries if scroll areas change on click. **/
    protected static boolean leftMouseDown = false;

    public ScrollAreaEntry(ScrollArea parent, int width, int height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        this.buttonBase = new ExtendedButton(0, 0, 0, 0, "", var1 -> {
            if (this.selectOnClick) {
                this.setSelected(true);
            }
            this.onClick(this);
        }) {
            @Override
            public void playDownSound(@NotNull SoundManager p_93665_) {
                if (ScrollAreaEntry.this.playClickSound) {
                    super.playDownSound(p_93665_);
                }
            }
            @Override
            public void render(@NotNull GuiGraphics graphics, int p_93658_, int p_93659_, float p_93660_) {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    this.isHovered = false;
                }
                super.render(graphics, p_93658_, p_93659_, p_93660_);
            }
            @Override
            public boolean isHoveredOrFocused() {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    return false;
                }
                return super.isHoveredOrFocused();
            }
            @Override
            public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    return;
                }
                super.onClick(event, isDoubleClick);
            }
        };
        this.updateEntry();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.updateEntry();

        this.buttonBase.render(graphics, mouseX, mouseY, partial);

        if (MouseInput.isLeftMouseDown() && !leftMouseDown && this.isHovered()) {
            leftMouseDown = true;
            this.buttonBase.onClick(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(0, -1)), false);
        }
        if (!MouseInput.isLeftMouseDown()) leftMouseDown = false;

    }

    public abstract void onClick(ScrollAreaEntry entry);

    public void updateEntry() {
        this.buttonBase.setX(this.x);
        this.buttonBase.setY(this.y);
        this.buttonBase.setWidth(this.width);
        this.buttonBase.setHeight(this.height);
        if (!this.isSelected()) {
            this.buttonBase.setBackgroundColor(this.backgroundColorIdle, this.backgroundColorHover, this.backgroundColorIdle, this.backgroundColorIdle, this.backgroundColorHover, this.backgroundColorIdle);
        } else {
            this.buttonBase.setBackgroundColor(this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover);
        }
    }

    public void setX(int x) {
        this.x = x;
        this.updateEntry();
    }

    public int getX() {
        return this.x;
    }

    public void setY(int y) {
        this.y = y;
        this.updateEntry();
    }

    public int getY() {
        return this.y;
    }

    public void setWidth(int width) {
        this.width = width;
        this.updateEntry();
    }

    public int getWidth() {
        return this.width;
    }

    public void setHeight(int height) {
        this.height = height;
        this.updateEntry();
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isHovered() {
        return this.buttonBase.isHovered();
    }

    public boolean isSelected() {
        return this.selectable && this.selected;
    }

    public void setSelected(boolean selected) {
        if (this.selectable) {
            this.selected = selected;
            this.updateEntry();
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

    public void setPlayClickSound(boolean playClickSound) {
        this.playClickSound = playClickSound;
    }

    public boolean isPlayClickSound() {
        return this.playClickSound;
    }

    @NotNull
    public DrawableColor getBackgroundColorNormal() {
        return this.backgroundColorIdle;
    }

    @Deprecated
    public Color getBackgroundColorIdle() {
        return this.getBackgroundColorNormal().getColor();
    }

    public void setBackgroundColorNormal(@NotNull DrawableColor color) {
        this.backgroundColorIdle = Objects.requireNonNull(color);
        this.updateEntry();
    }

    @Deprecated
    public void setBackgroundColorIdle(@NotNull Color color) {
        this.setBackgroundColorNormal(DrawableColor.of(color));
    }

    @NotNull
    public DrawableColor getBackgroundColorHovered() {
        return this.backgroundColorHover;
    }

    @Deprecated
    public Color getBackgroundColorHover() {
        return this.getBackgroundColorHovered().getColor();
    }

    public void setBackgroundColorHovered(@NotNull DrawableColor color) {
        this.backgroundColorHover = Objects.requireNonNull(color);
        this.updateEntry();
    }

    @Deprecated
    public void setBackgroundColorHover(@NotNull Color color) {
        this.setBackgroundColorHovered(DrawableColor.of(color));
    }

    public void setTooltip(String... tooltipLines) {
        this.buttonBase.setTooltip(Tooltip.of(tooltipLines));
    }

}
