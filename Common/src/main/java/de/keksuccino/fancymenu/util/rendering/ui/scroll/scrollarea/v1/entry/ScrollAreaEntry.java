
package de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.v1.entry;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.scrollarea.v1.ScrollArea;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import net.minecraft.client.sounds.SoundManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@SuppressWarnings("unused")
public abstract class ScrollAreaEntry extends UIBase {

    public ScrollArea parent;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    public AdvancedButton buttonBase;
    protected Color backgroundColorIdle = UIBase.getUIColorScheme().area_background_color.getColor();
    protected Color backgroundColorHover = UIBase.getUIColorScheme().list_entry_color_selected_hovered.getColor();
    protected boolean selectable = true;
    protected boolean selected = false;
    protected boolean playClickSound = true;
    public boolean deselectOtherEntriesOnSelect = true;
    public boolean selectOnClick = true;
    public int index = 0;

    public ScrollAreaEntry(ScrollArea parent, int width, int height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        this.buttonBase = new AdvancedButton(0, 0, 0, 0, "", true, (button) -> {
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
            public void render(@NotNull PoseStack p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    this.isHovered = false;
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
            @Override
            public boolean isHoveredOrFocused() {
                if (ScrollAreaEntry.this.parent.isMouseInteractingWithGrabbers() || !ScrollAreaEntry.this.parent.isMouseInsideArea()) {
                    return false;
                }
                return super.isHoveredOrFocused();
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

    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

        this.updateEntry();

        this.buttonBase.render(matrix, mouseX, mouseY, partial);

    }

    public abstract void onClick(ScrollAreaEntry entry);

    public void updateEntry() {
        this.buttonBase.setX(this.x);
        this.buttonBase.setY(this.y);
        this.buttonBase.setWidth(this.width);
        ((IMixinAbstractWidget)this.buttonBase).setHeightFancyMenu(this.height);
        if (!this.isSelected()) {
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
