//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.entry;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget.AdvancedButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.scroll.scrollarea.ScrollArea;
import net.minecraft.client.audio.SoundHandler;

import java.awt.*;

public abstract class ScrollAreaEntry extends UIBase {

    public ScrollArea parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    public AdvancedButton buttonBase;
    protected Color backgroundColorIdle = AREA_BACKGROUND_COLOR;
    protected Color backgroundColorHover = ENTRY_COLOR_FOCUSED;
    protected boolean focusable = true;
    protected boolean focused = false;
    protected boolean playClickSound = true;
    public boolean unfocusOtherEntriesOnFocus = true;
    public boolean focusOnClick = true;
    public int index = 0;

    public ScrollAreaEntry(ScrollArea parent, int width, int height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        this.buttonBase = new AdvancedButton(0, 0, 0, 0, "", true, (button) -> {
            if (this.focusOnClick) {
                this.setFocused(true);
            }
            this.onClick(this);
        }) {
            @Override
            public void playDownSound(SoundHandler p_93665_) {
                if (ScrollAreaEntry.this.playClickSound) {
                    super.playDownSound(p_93665_);
                }
            }
            @Override
            public void render(int p_93658_, int p_93659_, float p_93660_) {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    this.hovered = false;
                }
                super.render(p_93658_, p_93659_, p_93660_);
            }
            @Override
            public boolean isHovered() {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    return false;
                }
                return super.isHovered();
            }
            @Override
            public void onClick(double p_93371_, double p_93372_) {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    return;
                }
                super.onClick(p_93371_, p_93372_);
            }
        };
        this.updateEntry();
    }

    public void render(int mouseX, int mouseY, float partial) {

        this.updateEntry();

        this.buttonBase.render(mouseX, mouseY, partial);

    }

    public abstract void onClick(ScrollAreaEntry entry);

    public void updateEntry() {
        this.buttonBase.setX(this.x);
        this.buttonBase.setY(this.y);
        this.buttonBase.setWidth(this.width);
        this.buttonBase.setHeight(this.height);
        if (!this.isFocused()) {
            this.buttonBase.setBackgroundColor(this.backgroundColorIdle, this.backgroundColorHover, this.backgroundColorIdle, this.backgroundColorHover, 1);
        } else {
            this.buttonBase.setBackgroundColor(this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover, this.backgroundColorHover, 1);
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

    public boolean isFocused() {
        return this.focusable && this.focused;
    }

    public void setFocused(boolean focused) {
        if (this.focusable) {
            this.focused = focused;
            this.updateEntry();
            if (focused && this.unfocusOtherEntriesOnFocus) {
                for (ScrollAreaEntry e : this.parent.getEntries()) {
                    if (e != this) {
                        e.setFocused(false);
                    }
                }
            }
        }
    }

    public boolean isFocusable() {
        return this.focusable;
    }

    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
        if (!focusable) {
            this.focused = false;
        }
    }

    public void setPlayClickSound(boolean playClickSound) {
        this.playClickSound = playClickSound;
    }

    public boolean isPlayClickSound() {
        return this.playClickSound;
    }

    public Color getBackgroundColorIdle() {
        return backgroundColorIdle;
    }

    public void setBackgroundColorIdle(Color backgroundColorIdle) {
        this.backgroundColorIdle = backgroundColorIdle;
        this.updateEntry();
    }

    public Color getBackgroundColorHover() {
        return backgroundColorHover;
    }

    public void setBackgroundColorHover(Color backgroundColorHover) {
        this.backgroundColorHover = backgroundColorHover;
        this.updateEntry();
    }

    public void setTooltip(String... tooltipLines) {
        this.buttonBase.setDescription(tooltipLines);
    }

}
