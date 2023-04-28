package de.keksuccino.fancymenu.menu.fancy.item.items.playerentity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.slider.RangeSliderButton;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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

    protected PlayerEntityCustomizationItem element;
    protected float bodyRotationX;
    protected float bodyRotationY;
    protected float headRotationX;
    protected float headRotationY;

    protected PlayerEntityRotationScreen(LayoutEditorScreen parent, PlayerEntityCustomizationItem element) {

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
                return Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.bodyx", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.bodyYSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.bodyRotationY, (slider) -> {
            this.bodyRotationY = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.bodyy", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.headXSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.headRotationX, (slider) -> {
            this.headRotationX = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.headx", super.getSliderMessageWithoutPrefixSuffix());
            }
        };
        this.headYSlider = new RangeSliderButton(0, 0, 200, 20, true, -180.0D, 180.0D, this.headRotationY, (slider) -> {
            this.headRotationY = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return Locals.localize("fancymenu.helper.editor.items.playerentity.rotation.heady", super.getSliderMessageWithoutPrefixSuffix());
            }
        };

        this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.guicomponents.done"), true, (press) -> {
            this.applyChanges();
            this.onClose();
        });
        UIBase.colorizeButton(this.doneButton);

        this.cancelButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.guicomponents.cancel"), true, (press) -> {
            this.onClose();
        });
        UIBase.colorizeButton(this.cancelButton);

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {

        RenderSystem.enableBlend();

        //Draw screen background
        fill(matrix, 0, 0, this.width, this.height, SCREEN_BACKGROUND_COLOR.getRGB());

        int xCenter = this.width / 2;
        int yCenter = this.height / 2;
        int sliderX = xCenter - 100;

        this.bodyXSlider.setPosition(sliderX, yCenter - 5 - 20 - 5 - 20);
        this.bodyXSlider.render(matrix, mouseX, mouseY, partialTicks);

        this.bodyYSlider.setPosition(sliderX, yCenter - 5 - 20);
        this.bodyYSlider.render(matrix, mouseX, mouseY, partialTicks);

        this.headXSlider.setPosition(sliderX, yCenter);
        this.headXSlider.render(matrix, mouseX, mouseY, partialTicks);

        this.headYSlider.setPosition(sliderX, yCenter + 20 + 5);
        this.headYSlider.render(matrix, mouseX, mouseY, partialTicks);

        this.doneButton.setX(xCenter - this.doneButton.getWidth() - 5);
        this.doneButton.setY(this.height - 35);
        this.doneButton.render(matrix, mouseX, mouseY, partialTicks);

        this.cancelButton.setX(xCenter + 5);
        this.cancelButton.setY(this.height - 35);
        this.cancelButton.render(matrix, mouseX, mouseY, partialTicks);

        this.renderEntity(matrix, xCenter - 100 - (int)(this.element.getActiveEntityProperties().getDimensions().width * 40) - 30, yCenter - (int)(this.element.getActiveEntityProperties().getDimensions().height * 40) / 2);

    }

    protected void renderEntity(PoseStack matrix, int posX, int posY) {

        float bX = this.element.bodyRotationX;
        float bY = this.element.bodyRotationY;
        float hX = this.element.headRotationX;
        float hY = this.element.headRotationY;
        int oriScale = this.element.scale;
        String oriOrientation = this.element.orientation;
        String oriAdX = this.element.advancedPosX;
        String oriAdY = this.element.advancedPosY;
        int oriPosX = this.element.posX;
        int oriPosY = this.element.posY;

        this.element.bodyRotationX = this.bodyRotationX;
        this.element.bodyRotationY = this.bodyRotationY;
        this.element.headRotationX = this.headRotationX;
        this.element.headRotationY = this.headRotationY;
        this.element.scale = 40;
        this.element.orientation = "top-left";
        this.element.advancedPosX = null;
        this.element.advancedPosY = null;
        this.element.posX = posX;
        this.element.posY = posY;

        this.element.render(matrix, this);

        this.element.bodyRotationX = bX;
        this.element.bodyRotationY = bY;
        this.element.headRotationX = hX;
        this.element.headRotationY = hY;
        this.element.scale = oriScale;
        this.element.orientation = oriOrientation;
        this.element.advancedPosX = oriAdX;
        this.element.advancedPosY = oriAdY;
        this.element.posX = oriPosX;
        this.element.posY = oriPosY;

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
