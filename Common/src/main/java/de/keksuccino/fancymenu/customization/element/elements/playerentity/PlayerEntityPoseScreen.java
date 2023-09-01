package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.ConfiguratorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.RangeSliderButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class PlayerEntityPoseScreen extends ConfiguratorScreen {

    protected static final int ENTITY_SCALE = 110;

    protected PlayerEntityElement element;
    protected LayoutEditorScreen editor;
    protected Runnable runOnClose;

    protected float bodyXRot;
    protected float bodyYRot;
    protected float headXRot;
    protected float headYRot;
    protected float headZRot;
    protected float leftArmXRot;
    protected float leftArmYRot;
    protected float leftArmZRot;
    protected float rightArmXRot;
    protected float rightArmYRot;
    protected float rightArmZRot;
    protected float leftLegXRot;
    protected float leftLegYRot;
    protected float leftLegZRot;
    protected float rightLegXRot;
    protected float rightLegYRot;
    protected float rightLegZRot;

    protected PlayerEntityPoseScreen(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor, @NotNull Runnable runOnClose) {

        super(Component.translatable("fancymenu.editor.elements.player_entity.edit_pose"));

        this.element = element;
        this.editor = editor;
        this.runOnClose = runOnClose;

        this.bodyXRot = element.bodyXRot;
        this.bodyYRot = element.bodyYRot;

        this.headXRot = element.headXRot;
        this.headYRot = element.headYRot;
        this.headZRot = element.headZRot;

        this.leftArmXRot = element.leftArmXRot;
        this.leftArmYRot = element.leftArmYRot;
        this.leftArmZRot = element.leftArmZRot;

        this.rightArmXRot = element.rightArmXRot;
        this.rightArmYRot = element.rightArmYRot;
        this.rightArmZRot = element.rightArmZRot;

        this.leftLegXRot = element.leftLegXRot;
        this.leftLegYRot = element.leftLegYRot;
        this.leftLegZRot = element.leftLegZRot;

        this.rightLegXRot = element.rightLegXRot;
        this.rightLegYRot = element.rightLegYRot;
        this.rightLegZRot = element.rightLegZRot;

    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.head"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.headXRot, (slider) -> {
            this.headXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.headx", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.headYRot, (slider) -> {
            this.headYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.heady", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.headZRot, (slider) -> {
            this.headZRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.head_z_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.body"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.bodyXRot, (slider) -> {
            this.bodyXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.bodyx", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.bodyYRot, (slider) -> {
            this.bodyYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.helper.editor.items.playerentity.rotation.bodyy", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.left_arm"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftArmXRot, (slider) -> {
            this.leftArmXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_arm_x_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftArmYRot, (slider) -> {
            this.leftArmYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_arm_y_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftArmZRot, (slider) -> {
            this.leftArmZRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_arm_z_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.right_arm"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightArmXRot, (slider) -> {
            this.rightArmXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_arm_x_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightArmYRot, (slider) -> {
            this.rightArmYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_arm_y_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightArmZRot, (slider) -> {
            this.rightArmZRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_arm_z_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.left_leg"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftLegXRot, (slider) -> {
            this.leftLegXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_leg_x_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftLegYRot, (slider) -> {
            this.leftLegYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_leg_y_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.leftLegZRot, (slider) -> {
            this.leftLegZRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.left_leg_z_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addLabelCell(Component.translatable("fancymenu.editor.elements.player_entity.pose.right_leg"));

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightLegXRot, (slider) -> {
            this.rightLegXRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_leg_x_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightLegYRot, (slider) -> {
            this.rightLegYRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_leg_y_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addWidgetCell(new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, this.rightLegZRot, (slider) -> {
            this.rightLegZRot = (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue();
        }) {
            @Override
            public String getSliderMessageWithoutPrefixSuffix() {
                return I18n.get("fancymenu.editor.elements.player_entity.pose.right_leg_z_rot", super.getSliderMessageWithoutPrefixSuffix());
            }
        }, true);

        this.addSpacerCell(20);

    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partial) {

        super.render(pose, mouseX, mouseY, partial);

        int entityX = this.width - 20 - (this.getRightSideButtonWidth() / 2) - ((int)(this.element.getActiveEntityProperties().getDimensions().width * ENTITY_SCALE) / 2);
        int entityY = (int) this.scrollArea.getYWithBorder() + 30;
        this.renderEntity(pose, mouseX, mouseY, partial, entityX, entityY);

        RenderingUtils.resetShaderColor();

    }

    protected void renderEntity(PoseStack pose, int mouseX, int mouseY, float partial, int posX, int posY) {

        float cachedBodyXRot = this.element.bodyXRot;
        float cachedBodyYRot = this.element.bodyYRot;
        float cachedHeadXRot = this.element.headXRot;
        float cachedHeadYRot = this.element.headYRot;
        float cachedHeadZRot = this.element.headZRot;
        float cachedLeftArmXRot = this.element.leftArmXRot;
        float cachedLeftArmYRot = this.element.leftArmYRot;
        float cachedLeftArmZRot = this.element.leftArmZRot;
        float cachedRightArmXRot = this.element.rightArmXRot;
        float cachedRightArmYRot = this.element.rightArmYRot;
        float cachedRightArmZRot = this.element.rightArmZRot;
        float cachedLeftLegXRot = this.element.leftLegXRot;
        float cachedLeftLegYRot = this.element.leftLegYRot;
        float cachedLeftLegZRot = this.element.leftLegZRot;
        float cachedRightLegXRot = this.element.rightLegXRot;
        float cachedRightLegYRot = this.element.rightLegYRot;
        float cachedRightLegZRot = this.element.rightLegZRot;
        int cachedScale = this.element.scale;
        ElementAnchorPoint cachedOrientation = this.element.anchorPoint;
        String cachedAdvancedX = this.element.advancedX;
        String cachedAdvancedY = this.element.advancedY;
        int cachedPosOffsetX = this.element.posOffsetX;
        int cachedPosOffsetY = this.element.posOffsetY;

        this.applyPose();
        this.element.scale = ENTITY_SCALE;
        this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        this.element.advancedX = null;
        this.element.advancedY = null;
        this.element.posOffsetX = posX;
        this.element.posOffsetY = posY;

        this.element.render(pose, mouseX, mouseY, partial);

        this.element.bodyXRot = cachedBodyXRot;
        this.element.bodyYRot = cachedBodyYRot;
        this.element.headXRot = cachedHeadXRot;
        this.element.headYRot = cachedHeadYRot;
        this.element.headZRot = cachedHeadZRot;
        this.element.leftArmXRot = cachedLeftArmXRot;
        this.element.leftArmYRot = cachedLeftArmYRot;
        this.element.leftArmZRot = cachedLeftArmZRot;
        this.element.rightArmXRot = cachedRightArmXRot;
        this.element.rightArmYRot = cachedRightArmYRot;
        this.element.rightArmZRot = cachedRightArmZRot;
        this.element.leftLegXRot = cachedLeftLegXRot;
        this.element.leftLegYRot = cachedLeftLegYRot;
        this.element.leftLegZRot = cachedLeftLegZRot;
        this.element.rightLegXRot = cachedRightLegXRot;
        this.element.rightLegYRot = cachedRightLegYRot;
        this.element.rightLegZRot = cachedRightLegZRot;
        this.element.scale = cachedScale;
        this.element.anchorPoint = cachedOrientation;
        this.element.advancedX = cachedAdvancedX;
        this.element.advancedY = cachedAdvancedY;
        this.element.posOffsetX = cachedPosOffsetX;
        this.element.posOffsetY = cachedPosOffsetY;

    }

    protected void applyPose() {
        this.element.bodyXRot = this.bodyXRot;
        this.element.bodyYRot = this.bodyYRot;
        this.element.headXRot = this.headXRot;
        this.element.headYRot = this.headYRot;
        this.element.headZRot = this.headZRot;
        this.element.leftArmXRot = this.leftArmXRot;
        this.element.leftArmYRot = this.leftArmYRot;
        this.element.leftArmZRot = this.leftArmZRot;
        this.element.rightArmXRot = this.rightArmXRot;
        this.element.rightArmYRot = this.rightArmYRot;
        this.element.rightArmZRot = this.rightArmZRot;
        this.element.leftLegXRot = this.leftLegXRot;
        this.element.leftLegYRot = this.leftLegYRot;
        this.element.leftLegZRot = this.leftLegZRot;
        this.element.rightLegXRot = this.rightLegXRot;
        this.element.rightLegYRot = this.rightLegYRot;
        this.element.rightLegZRot = this.rightLegZRot;
    }

    @Override
    protected void onCancel() {
        this.runOnClose.run();
    }

    @Override
    protected void onDone() {
        this.editor.history.saveSnapshot();
        this.applyPose();
        this.runOnClose.run();
    }

}
