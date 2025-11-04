package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.screen.CellScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v1.RangeSliderButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerEntityPoseScreen extends CellScreen {

    protected static final int ENTITY_SCALE = 110;

    protected PlayerEntityElement element;
    protected LayoutEditorScreen editor;
    protected Runnable runOnClose;

    public String bodyXRot;
    public String bodyYRot;
    public String headXRot;
    public String headYRot;
    public String headZRot;
    public String leftArmXRot;
    public String leftArmYRot;
    public String leftArmZRot;
    public String rightArmXRot;
    public String rightArmYRot;
    public String rightArmZRot;
    public String leftLegXRot;
    public String leftLegYRot;
    public String leftLegZRot;
    public String rightLegXRot;
    public String rightLegYRot;
    public String rightLegZRot;
    public boolean bodyXRotAdvancedMode;
    public boolean bodyYRotAdvancedMode;
    public boolean headXRotAdvancedMode;
    public boolean headYRotAdvancedMode;
    public boolean headZRotAdvancedMode;
    public boolean leftArmXRotAdvancedMode;
    public boolean leftArmYRotAdvancedMode;
    public boolean leftArmZRotAdvancedMode;
    public boolean rightArmXRotAdvancedMode;
    public boolean rightArmYRotAdvancedMode;
    public boolean rightArmZRotAdvancedMode;
    public boolean leftLegXRotAdvancedMode;
    public boolean leftLegYRotAdvancedMode;
    public boolean leftLegZRotAdvancedMode;
    public boolean rightLegXRotAdvancedMode;
    public boolean rightLegYRotAdvancedMode;
    public boolean rightLegZRotAdvancedMode;

    protected PlayerEntityPoseScreen(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor, @NotNull Runnable runOnClose) {

        super(Component.translatable("fancymenu.elements.player_entity.edit_pose"));

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

        this.bodyXRotAdvancedMode = element.bodyXRotAdvancedMode;
        this.bodyYRotAdvancedMode = element.bodyYRotAdvancedMode;
        this.headXRotAdvancedMode = element.headXRotAdvancedMode;
        this.headYRotAdvancedMode = element.headYRotAdvancedMode;
        this.headZRotAdvancedMode = element.headZRotAdvancedMode;
        this.leftArmXRotAdvancedMode = element.leftArmXRotAdvancedMode;
        this.leftArmYRotAdvancedMode = element.leftArmYRotAdvancedMode;
        this.leftArmZRotAdvancedMode = element.leftArmZRotAdvancedMode;
        this.rightArmXRotAdvancedMode = element.rightArmXRotAdvancedMode;
        this.rightArmYRotAdvancedMode = element.rightArmYRotAdvancedMode;
        this.rightArmZRotAdvancedMode = element.rightArmZRotAdvancedMode;
        this.leftLegXRotAdvancedMode = element.leftLegXRotAdvancedMode;
        this.leftLegYRotAdvancedMode = element.leftLegYRotAdvancedMode;
        this.leftLegZRotAdvancedMode = element.leftLegZRotAdvancedMode;
        this.rightLegXRotAdvancedMode = element.rightLegXRotAdvancedMode;
        this.rightLegYRotAdvancedMode = element.rightLegYRotAdvancedMode;
        this.rightLegZRotAdvancedMode = element.rightLegZRotAdvancedMode;

    }

    @Override
    protected void initCells() {

        this.addSpacerCell(20);

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.body"));

        this.addCell(new RotationCell("body_x_rot",
                () -> this.bodyXRot,
                s -> this.bodyXRot = s,
                () -> this.bodyXRotAdvancedMode,
                aBoolean -> this.bodyXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("body_y_rot",
                () -> this.bodyYRot,
                s -> this.bodyYRot = s,
                () -> this.bodyYRotAdvancedMode,
                aBoolean -> this.bodyYRotAdvancedMode = aBoolean));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.head"));

        this.addCell(new RotationCell("head_x_rot",
                () -> this.headXRot,
                s -> this.headXRot = s,
                () -> this.headXRotAdvancedMode,
                aBoolean -> this.headXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("head_y_rot",
                () -> this.headYRot,
                s -> this.headYRot = s,
                () -> this.headYRotAdvancedMode,
                aBoolean -> this.headYRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("head_z_rot",
                () -> this.headZRot,
                s -> this.headZRot = s,
                () -> this.headZRotAdvancedMode,
                aBoolean -> this.headZRotAdvancedMode = aBoolean));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.left_arm"));

        this.addCell(new RotationCell("left_arm_x_rot",
                () -> this.leftArmXRot,
                s -> this.leftArmXRot = s,
                () -> this.leftArmXRotAdvancedMode,
                aBoolean -> this.leftArmXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("left_arm_y_rot",
                () -> this.leftArmYRot,
                s -> this.leftArmYRot = s,
                () -> this.leftArmYRotAdvancedMode,
                aBoolean -> this.leftArmYRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("left_arm_z_rot",
                () -> this.leftArmZRot,
                s -> this.leftArmZRot = s,
                () -> this.leftArmZRotAdvancedMode,
                aBoolean -> this.leftArmZRotAdvancedMode = aBoolean));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.right_arm"));

        this.addCell(new RotationCell("right_arm_x_rot",
                () -> this.rightArmXRot,
                s -> this.rightArmXRot = s,
                () -> this.rightArmXRotAdvancedMode,
                aBoolean -> this.rightArmXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("right_arm_y_rot",
                () -> this.rightArmYRot,
                s -> this.rightArmYRot = s,
                () -> this.rightArmYRotAdvancedMode,
                aBoolean -> this.rightArmYRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("right_arm_z_rot",
                () -> this.rightArmZRot,
                s -> this.rightArmZRot = s,
                () -> this.rightArmZRotAdvancedMode,
                aBoolean -> this.rightArmZRotAdvancedMode = aBoolean));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.left_leg"));

        this.addCell(new RotationCell("left_leg_x_rot",
                () -> this.leftLegXRot,
                s -> this.leftLegXRot = s,
                () -> this.leftLegXRotAdvancedMode,
                aBoolean -> this.leftLegXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("left_leg_y_rot",
                () -> this.leftLegYRot,
                s -> this.leftLegYRot = s,
                () -> this.leftLegYRotAdvancedMode,
                aBoolean -> this.leftLegYRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("left_leg_z_rot",
                () -> this.leftLegZRot,
                s -> this.leftLegZRot = s,
                () -> this.leftLegZRotAdvancedMode,
                aBoolean -> this.leftLegZRotAdvancedMode = aBoolean));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.right_leg"));

        this.addCell(new RotationCell("right_leg_x_rot",
                () -> this.rightLegXRot,
                s -> this.rightLegXRot = s,
                () -> this.rightLegXRotAdvancedMode,
                aBoolean -> this.rightLegXRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("right_leg_y_rot",
                () -> this.rightLegYRot,
                s -> this.rightLegYRot = s,
                () -> this.rightLegYRotAdvancedMode,
                aBoolean -> this.rightLegYRotAdvancedMode = aBoolean));

        this.addCell(new RotationCell("right_leg_z_rot",
                () -> this.rightLegZRot,
                s -> this.rightLegZRot = s,
                () -> this.rightLegZRotAdvancedMode,
                aBoolean -> this.rightLegZRotAdvancedMode = aBoolean));

        this.addSpacerCell(20);

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        super.render(graphics, mouseX, mouseY, partial);

        int entityX = this.width - 20 - (this.getRightSideWidgetWidth() / 2) - ((int)(this.element.getActiveEntityProperties().getDimensions().width * ENTITY_SCALE) / 2);
        int entityY = (int) this.scrollArea.getYWithBorder() + 30;
        this.renderEntity(graphics, mouseX, mouseY, partial, entityX, entityY);

        RenderingUtils.resetShaderColor(graphics);

    }

    protected void renderEntity(GuiGraphics graphics, int mouseX, int mouseY, float partial, int posX, int posY) {

        String cachedBodyXRot = this.element.bodyXRot;
        String cachedBodyYRot = this.element.bodyYRot;
        String cachedHeadXRot = this.element.headXRot;
        String cachedHeadYRot = this.element.headYRot;
        String cachedHeadZRot = this.element.headZRot;
        String cachedLeftArmXRot = this.element.leftArmXRot;
        String cachedLeftArmYRot = this.element.leftArmYRot;
        String cachedLeftArmZRot = this.element.leftArmZRot;
        String cachedRightArmXRot = this.element.rightArmXRot;
        String cachedRightArmYRot = this.element.rightArmYRot;
        String cachedRightArmZRot = this.element.rightArmZRot;
        String cachedLeftLegXRot = this.element.leftLegXRot;
        String cachedLeftLegYRot = this.element.leftLegYRot;
        String cachedLeftLegZRot = this.element.leftLegZRot;
        String cachedRightLegXRot = this.element.rightLegXRot;
        String cachedRightLegYRot = this.element.rightLegYRot;
        String cachedRightLegZRot = this.element.rightLegZRot;
        String cachedScale = this.element.scale;
        ElementAnchorPoint cachedOrientation = this.element.anchorPoint;
        String cachedAdvancedX = this.element.advancedX;
        String cachedAdvancedY = this.element.advancedY;
        int cachedPosOffsetX = this.element.posOffsetX;
        int cachedPosOffsetY = this.element.posOffsetY;

        this.applyPose();
        this.element.scale = "" + ENTITY_SCALE;
        this.element.anchorPoint = ElementAnchorPoints.TOP_LEFT;
        this.element.advancedX = null;
        this.element.advancedY = null;
        this.element.posOffsetX = posX;
        this.element.posOffsetY = posY;

        this.element.render(graphics, mouseX, mouseY, partial);

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

    protected void applyAdvancedMode() {
        this.element.bodyXRotAdvancedMode = this.bodyXRotAdvancedMode;
        this.element.bodyYRotAdvancedMode = this.bodyYRotAdvancedMode;
        this.element.headXRotAdvancedMode = this.headXRotAdvancedMode;
        this.element.headYRotAdvancedMode = this.headYRotAdvancedMode;
        this.element.headZRotAdvancedMode = this.headZRotAdvancedMode;
        this.element.leftArmXRotAdvancedMode = this.leftArmXRotAdvancedMode;
        this.element.leftArmYRotAdvancedMode = this.leftArmYRotAdvancedMode;
        this.element.leftArmZRotAdvancedMode = this.leftArmZRotAdvancedMode;
        this.element.rightArmXRotAdvancedMode = this.rightArmXRotAdvancedMode;
        this.element.rightArmYRotAdvancedMode = this.rightArmYRotAdvancedMode;
        this.element.rightArmZRotAdvancedMode = this.rightArmZRotAdvancedMode;
        this.element.leftLegXRotAdvancedMode = this.leftLegXRotAdvancedMode;
        this.element.leftLegYRotAdvancedMode = this.leftLegYRotAdvancedMode;
        this.element.leftLegZRotAdvancedMode = this.leftLegZRotAdvancedMode;
        this.element.rightLegXRotAdvancedMode = this.rightLegXRotAdvancedMode;
        this.element.rightLegYRotAdvancedMode = this.rightLegYRotAdvancedMode;
        this.element.rightLegZRotAdvancedMode = this.rightLegZRotAdvancedMode;
    }

    @Override
    protected void onCancel() {
        this.runOnClose.run();
    }

    @Override
    protected void onDone() {
        this.editor.history.saveSnapshot();
        this.applyAdvancedMode();
        this.applyPose();
        this.runOnClose.run();
    }

    protected static float stringToFloat(@Nullable String s) {
        if (s == null) return 0.0F;
        s = PlaceholderParser.replacePlaceholders(s);
        s = s.replace(" ", "");
        try {
            float f = Float.parseFloat(s);
//            if (f > 180) f = 180;
//            if (f < -180) f = -180;
            return f;
        } catch (Exception ignore) {}
        return 0.0F;
    }

    public class RotationCell extends RenderCell {

        public AbstractWidget activeWidget;
        public ExtendedButton rotationStringButton;
        public RangeSliderButton rotationSlider;
        public CycleButton<CommonCycles.CycleEnabledDisabled> toggleModeButton;

        public RotationCell(@NotNull String localizationKeySuffix, @NotNull Supplier<String> rotationValueGetter, @NotNull Consumer<String> rotationValueSetter, @NotNull Supplier<Boolean> advancedModeGetter, @NotNull Consumer<Boolean> advancedModeSetter) {

            this.toggleModeButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.elements.player_entity.pose.advanced", advancedModeGetter.get()), (value, button) -> {
                advancedModeSetter.accept(value.getAsBoolean());
                if (this.activeWidget != null) this.children().remove(this.activeWidget);
                this.activeWidget = value.getAsBoolean() ? this.rotationStringButton : this.rotationSlider;
                this.children().add(this.activeWidget);
            });
            UIBase.applyDefaultWidgetSkinTo(this.toggleModeButton);

            this.rotationStringButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.elements.player_entity.pose.advanced." + localizationKeySuffix), button -> {
                TextEditorScreen s = new TextEditorScreen(Component.translatable("fancymenu.elements.player_entity.pose.advanced." + localizationKeySuffix), null, call -> {
                    if (call != null) {
                        rotationValueSetter.accept(call);
                    }
                    Minecraft.getInstance().setScreen(PlayerEntityPoseScreen.this);
                });
                s.setText(rotationValueGetter.get());
                Minecraft.getInstance().setScreen(s);
            });
            UIBase.applyDefaultWidgetSkinTo(this.rotationStringButton);

            this.rotationSlider = new RangeSliderButton(0, 0, 20, 20, false, -180.0D, 180.0D, stringToFloat(rotationValueGetter.get()), (slider) -> {
                if (!advancedModeGetter.get()) rotationValueSetter.accept("" + (float)((RangeSliderButton)slider).getSelectedRangeDoubleValue());
            }) {
                @Override
                public String getSliderMessageWithoutPrefixSuffix() {
                    return I18n.get("fancymenu.elements.player_entity.pose." + localizationKeySuffix, super.getSliderMessageWithoutPrefixSuffix());
                }
            };
            UIBase.applyDefaultWidgetSkinTo(this.rotationSlider);

            this.activeWidget = advancedModeGetter.get() ? this.rotationStringButton : this.rotationSlider;
            this.children().add(this.activeWidget);

            this.children().add(this.toggleModeButton);

        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            int toggleModeButtonWidth = Minecraft.getInstance().font.width(this.toggleModeButton.getLabelSupplier().get(this.toggleModeButton)) + 6;

            this.activeWidget.x = this.getX();
            this.activeWidget.y = this.getY();
            this.activeWidget.setWidth(this.getWidth() - toggleModeButtonWidth - 5);

            this.toggleModeButton.x = (this.getX() + this.getWidth() - toggleModeButtonWidth);
            this.toggleModeButton.y = (this.getY());
            this.toggleModeButton.setWidth(toggleModeButtonWidth);

        }

    }

}
