package de.keksuccino.fancymenu.customization.element.elements.dragger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class DraggerWidget extends AbstractWidget {

    @NotNull
    public DraggingCallback draggingCallback;
    @NotNull
    public MouseCallback mouseCallback;
    protected boolean leftMouseDown = false;

    public DraggerWidget(int x, int y, int width, int height, @NotNull DraggingCallback draggingCallback, @NotNull MouseCallback mouseCallback) {
        super(x, y, width, height, Component.empty());
        this.draggingCallback = draggingCallback;
        this.mouseCallback = mouseCallback;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int i, int i1, float v) {
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void playDownSound(@NotNull SoundManager soundManager) {
        //don't play click/down sound
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (this.canClick()) {
            this.leftMouseDown = true;
            this.mouseCallback.onClickOrRelease(event.x(), event.y(), false);
            return true;
        }
        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.leftMouseDown) {
            this.leftMouseDown = false;
            this.mouseCallback.onClickOrRelease(event.x(), event.y(), true);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.leftMouseDown) {
            this.draggingCallback.onDrag(event.x(), event.y(), dragX, dragY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    protected boolean canClick() {
        return (this.isHovered() && this.isActive() && this.visible);
    }

    @FunctionalInterface
    public interface DraggingCallback {
        void onDrag(double mouseX, double mouseY, double dragX, double dragY);
    }

    @FunctionalInterface
    public interface MouseCallback {
        void onClickOrRelease(double mouseX, double mouseY, boolean released);
    }

}
