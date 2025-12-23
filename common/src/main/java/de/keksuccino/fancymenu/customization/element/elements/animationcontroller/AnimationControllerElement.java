package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class AnimationControllerElement extends AbstractElement {

    @NotNull
    public List<AnimationKeyframe> keyframes = new ArrayList<>();
    @NotNull
    public List<TargetElement> targetElements = new ArrayList<>();
    public boolean loop = false;
    public boolean offsetMode = false;
    public boolean ignoreSize = false;
    public boolean ignorePosition = false;

    public AnimationControllerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (isEditor()) {

            if (this.shouldRender()) {

                int x = this.getAbsoluteX();
                int y = this.getAbsoluteY();
                int w = this.getAbsoluteWidth();
                int h = this.getAbsoluteHeight();
                graphics.fill(x, y, x + w, y + h, this.inEditorColor.getColorInt());
                graphics.enableScissor(x, y, x + w, y + h);
                graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
                graphics.disableScissor();

            }

        } else {

            boolean shouldPlay = AnimationControllerStateController.isPlaying(this.getInstanceIdentifier());
            if (!shouldPlay) {
                AnimationControllerHandler.resetController(this);
                return;
            }

            this.targetElements.forEach(targetElement -> {
                if (this.shouldRender()) {
                    if (AnimationControllerHandler.wasAnimatedInThePast(targetElement.targetElementId) && AnimationControllerHandler.isFinished(targetElement.targetElementId) && !AnimationControllerHandler.isAnimating(targetElement.targetElementId)) {
                        targetElement.animationApplied = true;
                    } else {
                        ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
                        if (layer != null) {
                            AbstractElement target = layer.getElementByInstanceIdentifier(targetElement.targetElementId);
                            if (target != null) targetElement.animationApplied = AnimationControllerHandler.applyAnimation(this, target);
                        }
                    }
                } else {
                    if (AnimationControllerHandler.wasAnimatedInThePast(targetElement.targetElementId) && !AnimationControllerHandler.isFinished(targetElement.targetElementId)) {
                        targetElement.animationApplied = false;
                    }
                }
            });

        }

    }

    public List<AnimationKeyframe> getKeyframes() {
        return new ArrayList<>(keyframes);
    }

    public static class TargetElement {

        public String targetElementId;
        public boolean animationApplied = false;

        public TargetElement() {
        }

        public TargetElement(String targetElementId) {
            this.targetElementId = targetElementId;
        }

    }

}
