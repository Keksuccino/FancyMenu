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
    public void playDownSound(SoundManager $$0) {
        //don't play click/down sound
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        this.draggingCallback.onDrag(event.x(), event.y(), dragX, dragY);
        super.onDrag(event, dragX, dragY);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        this.mouseCallback.onClickOrRelease(event.x(), event.y(), false);
        super.onClick(event, isDoubleClick);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.mouseCallback.onClickOrRelease(event.x(), event.y(), true);
        super.onRelease(event);
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
