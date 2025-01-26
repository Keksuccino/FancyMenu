package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AnimationControllerElement extends AbstractElement {

    private static final DrawableColor BACKGROUND_COLOR = DrawableColor.of(new Color(0, 255, 0, 100));
    
    public List<AnimationKeyframe> keyframes = new ArrayList<>();
    public String targetElementId = null;
    public boolean loop = false;
    public boolean animationApplied = false;

    public AnimationControllerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if (isEditor()) {

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();
            RenderSystem.enableBlend();
            graphics.fill(RenderType.guiOverlay(), x, y, x + w, y + h, BACKGROUND_COLOR.getColorInt());
            graphics.enableScissor(x, y, x + w, y + h);
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
            graphics.disableScissor();

        } else {

            if ((this.targetElementId != null) && !this.animationApplied) {

                if (AnimationControllerHandler.wasAnimatedInThePast(this.targetElementId) && !AnimationControllerHandler.isAnimating(this.targetElementId)) {
                    this.animationApplied = true;
                } else {
                    ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
                    if (layer != null) {
                        AbstractElement target = layer.getElementByInstanceIdentifier(this.targetElementId);
                        if (target != null) this.animationApplied = AnimationControllerHandler.applyAnimation(this, target);
                    }
                }

            }

        }

    }

    public void setTargetElementId(String elementId) {
        this.targetElementId = elementId;
    }

    public String getTargetElementId() {
        return targetElementId;
    }

    public List<AnimationKeyframe> getKeyframes() {
        return new ArrayList<>(keyframes);
    }

}
