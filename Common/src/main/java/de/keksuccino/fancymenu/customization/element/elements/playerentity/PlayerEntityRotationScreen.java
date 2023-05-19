package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.slider.RangeSliderButton;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class PlayerEntityRotationScreen extends Screen {

    protected static final Color SCREEN_BACKGROUND_COLOR = new Color(54, 54, 54);

    protected LayoutEditorScreen parent;

    protected AdvancedButton doneButton;
    protected AdvancedButton cancelButton;
    protected RangeSliderButton bodyXSlider;
    protected RangeSliderButton bodyYSlider;
    protected RangeSliderButton headXSlider;
    protected RangeSliderButton headYSlider;

    protected PlayerEntityElement element;
    protected float bodyRotationX;
    protected float bodyRotationY;
    protected float headRotationX;
    protected float headRotationY;

    protected PlayerEntityRotationScreen(LayoutEditorScreen parent, PlayerEntityElement element) {

        super(Component.literal(""));

        this.parent = parent;
        this.element = element;

        this.bodyRotationX = element.bodyRotationX;
        this.bodyRotationY = element.bodyRotationY;
        this.headRotationX = element.headRotationX;
        this.headRotationY = element.headRotationY;

        this.bodyXSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.bodyRotationX, (slider) -> {
            this.bodyRotationX = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.bodyx", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.bodyYSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.bodyRotationY, (slider) -> {
            this.bodyRotationY = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.bodyy", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.headXSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.headRotationX, (slider) -> {
            this.headRotationX = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.headx", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.headYSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.headRotationY, (slider) -> {
            this.headRotationY = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.heady", super.getSliderMessageWithoutPrefixSuffix());
            }
        };

        this.doneButton = new AdvancedButton(0, 0, 100, 20, I18n.get("fancymenu.guicomponents.done"), true, (press) -> {
            this.applyChanges();
            this.onClose();
        });
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 100, 20, I18n.get("fancymenu.guicomponents.cancel"), true, (press) -> {
            this.onClose();
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        //Draw screen background
        fill(pose, 0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR.getRGB());

        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        int sliderX = xCenter - 100;

        this.bodyXSlider.setPosition(sliderX, yCenter - 5 - 20 - 5 - 20);
        this.bodyXSlider.render(pose, mouseX, mouseY, partial);

        this.bodyYSlider.setPosition(sliderX, yCenter - 5 - 20);
        this.bodyYSlider.render(pose, mouseX, mouseY, partial);

        this.headXSlider.setPosition(sliderX, yCenter);
        this.headXSlider.render(pose, mouseX, mouseY, partial);

        this.headYSlider.setPosition(sliderX, yCenter + 20 + 5);
        this.headYSlider.render(pose, mouseX, mouseY, partial);

        this.doneButton.setX(xCenter - this.doneButton.getWidth() - 5);
        this.doneButton.setY(this.height - 35);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX(xCenter + 5);
        this.cancelButton.setY(this.height - 35);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        this.renderEntity(pose, mouseX, mouseY, partial, xCenter - 100 - (int)(this.element.getActiveEntityProperties().getDimensions().width * 40) - 30, yCenter - (int)(this.element.getActiveEntityProperties().getDimensions().height * 40) / 2);

    }

    protected void renderEntity(PoseStack pose, int mouseX, int mouseY, float partial, int posX, int posY) {

        float bX = this.element.bodyRotationX;
        float bY = this.element.bodyRotationY;
        float hX = this.element.headRotationX;
        float hY = this.element.headRotationY;
        int oriScale = this.element.scale;
        ElementAnchorPoint oriOrientation = this.element.anchorPoint;
        String oriAdX = this.element.advancedX;
        String oriAdY = this.element.advancedY;
        int oriPosX = this.element.baseX;
        int oriPosY = this.element.baseY;

        this.element.bodyRotationX = this.bodyRotationX;
        this.element.bodyRotationY = this.bodyRotationY;
        this.element.headRotationX = this.headRotationX;
        this.element.headRotationY = this.headRotationY;
        this.element.scale = 40;
        this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        this.element.advancedX = null;
        this.element.advancedY = null;
        this.element.baseX = posX;
        this.element.baseY = posY;

        this.element.render(pose, mouseX, mouseY, partial);

        this.element.bodyRotationX = bX;
        this.element.bodyRotationY = bY;
        this.element.headRotationX = hX;
        this.element.headRotationY = hY;
        this.element.scale = oriScale;
        this.element.anchorPoint = oriOrientation;
        this.element.advancedX = oriAdX;
        this.element.advancedY = oriAdY;
        this.element.baseX = oriPosX;
        this.element.baseY = oriPosY;

    }

    protected void applyChanges() {
        this.parent.history.saveSnapshot(this.parent.history.createSnapshot());
        this.element.bodyRotationX = this.bodyRotationX;
        this.element.bodyRotationY = this.bodyRotationY;
        this.element.headRotationX = this.headRotationX;
        this.element.headRotationY = this.headRotationY;
    }

    @Override
    public void onClose() {
        if (!PopupHandler.isPopupActive()) {
            Minecraft.getInstance().setScreen(this.parent);
        }
    }

}
