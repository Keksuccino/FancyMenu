package de.keksuccino.fancymenu.customization.element.elements.dragger;

import com.mojang.blaze3d.vertex.PoseStack;
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

    public DraggerWidget(int x, int y, int width, int height, @NotNull DraggingCallback draggingCallback, @NotNull MouseCallback mouseCallback) {
        super(x, y, width, height, Component.empty());
        this.draggingCallback = draggingCallback;
        this.mouseCallback = mouseCallback;
    }

    @Override
    public void renderButton(@NotNull PoseStack guiGraphics, int i, int i1, float v) {
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public void playDownSound(SoundManager $$0) {
        //don't play click/down sound
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        this.draggingCallback.onDrag(mouseX, mouseY, dragX, dragY);
        super.onDrag(mouseX, mouseY, dragX, dragY);
    }

    @Override
    public void onClick(double $$0, double $$1) {
        this.mouseCallback.onClickOrRelease($$0, $$1, false);
        super.onClick($$0, $$1);
    }

    @Override
    public void onRelease(double $$0, double $$1) {
        this.mouseCallback.onClickOrRelease($$0, $$1, true);
        super.onRelease($$0, $$1);
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
