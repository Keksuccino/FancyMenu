package de.keksuccino.fancymenu.customization.element.elements.dragger;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canClick()) {
            this.leftMouseDown = true;
            this.mouseCallback.onClickOrRelease(mouseX, mouseY, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.leftMouseDown) {
            this.leftMouseDown = false;
            this.mouseCallback.onClickOrRelease(mouseX, mouseY, true);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.leftMouseDown) {
            this.draggingCallback.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
