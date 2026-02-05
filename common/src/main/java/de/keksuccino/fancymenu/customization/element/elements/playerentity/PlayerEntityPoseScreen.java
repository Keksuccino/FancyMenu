package de.keksuccino.fancymenu.customization.element.elements.playerentity;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPCellWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindowHandler;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlayerEntityPoseScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 520;
    public static final int PIP_WINDOW_HEIGHT = 420;

    protected final PlayerEntityElement element;
    protected final LayoutEditorScreen editor;
    protected final PoseState originalPose;

    public String bodyXRot;
    public String bodyYRot;
    public String bodyZRot;
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
    public boolean bodyZRotAdvancedMode;
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

    protected PlayerEntityPoseScreen(@NotNull PlayerEntityElement element, @NotNull LayoutEditorScreen editor) {

        super(Component.translatable("fancymenu.elements.player_entity.edit_pose"));

        this.element = element;
        this.editor = editor;
        this.originalPose = PoseState.capture(element);

        this.bodyXRot = element.bodyXRot;
        this.bodyYRot = element.bodyYRot;
        this.bodyZRot = element.bodyZRot;

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
        this.bodyZRotAdvancedMode = element.bodyZRotAdvancedMode;
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
                this.poseSetter(s -> this.bodyXRot = s),
                () -> this.bodyXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.bodyXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("body_y_rot",
                () -> this.bodyYRot,
                this.poseSetter(s -> this.bodyYRot = s),
                () -> this.bodyYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.bodyYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("body_z_rot",
                () -> this.bodyZRot,
                this.poseSetter(s -> this.bodyZRot = s),
                () -> this.bodyZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.bodyZRotAdvancedMode = aBoolean)));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.head"));

        this.addCell(new RotationCell("head_x_rot",
                () -> this.headXRot,
                this.poseSetter(s -> this.headXRot = s),
                () -> this.headXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.headXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("head_y_rot",
                () -> this.headYRot,
                this.poseSetter(s -> this.headYRot = s),
                () -> this.headYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.headYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("head_z_rot",
                () -> this.headZRot,
                this.poseSetter(s -> this.headZRot = s),
                () -> this.headZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.headZRotAdvancedMode = aBoolean)));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.left_arm"));

        this.addCell(new RotationCell("left_arm_x_rot",
                () -> this.leftArmXRot,
                this.poseSetter(s -> this.leftArmXRot = s),
                () -> this.leftArmXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftArmXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("left_arm_y_rot",
                () -> this.leftArmYRot,
                this.poseSetter(s -> this.leftArmYRot = s),
                () -> this.leftArmYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftArmYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("left_arm_z_rot",
                () -> this.leftArmZRot,
                this.poseSetter(s -> this.leftArmZRot = s),
                () -> this.leftArmZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftArmZRotAdvancedMode = aBoolean)));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.right_arm"));

        this.addCell(new RotationCell("right_arm_x_rot",
                () -> this.rightArmXRot,
                this.poseSetter(s -> this.rightArmXRot = s),
                () -> this.rightArmXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightArmXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("right_arm_y_rot",
                () -> this.rightArmYRot,
                this.poseSetter(s -> this.rightArmYRot = s),
                () -> this.rightArmYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightArmYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("right_arm_z_rot",
                () -> this.rightArmZRot,
                this.poseSetter(s -> this.rightArmZRot = s),
                () -> this.rightArmZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightArmZRotAdvancedMode = aBoolean)));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.left_leg"));

        this.addCell(new RotationCell("left_leg_x_rot",
                () -> this.leftLegXRot,
                this.poseSetter(s -> this.leftLegXRot = s),
                () -> this.leftLegXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftLegXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("left_leg_y_rot",
                () -> this.leftLegYRot,
                this.poseSetter(s -> this.leftLegYRot = s),
                () -> this.leftLegYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftLegYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("left_leg_z_rot",
                () -> this.leftLegZRot,
                this.poseSetter(s -> this.leftLegZRot = s),
                () -> this.leftLegZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.leftLegZRotAdvancedMode = aBoolean)));

        this.addLabelCell(Component.translatable("fancymenu.elements.player_entity.pose.right_leg"));

        this.addCell(new RotationCell("right_leg_x_rot",
                () -> this.rightLegXRot,
                this.poseSetter(s -> this.rightLegXRot = s),
                () -> this.rightLegXRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightLegXRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("right_leg_y_rot",
                () -> this.rightLegYRot,
                this.poseSetter(s -> this.rightLegYRot = s),
                () -> this.rightLegYRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightLegYRotAdvancedMode = aBoolean)));

        this.addCell(new RotationCell("right_leg_z_rot",
                () -> this.rightLegZRot,
                this.poseSetter(s -> this.rightLegZRot = s),
                () -> this.rightLegZRotAdvancedMode,
                this.modeSetter(aBoolean -> this.rightLegZRotAdvancedMode = aBoolean)));

        this.addSpacerCell(20);

    }

    private Consumer<String> poseSetter(@NotNull Consumer<String> setter) {
        return value -> {
            setter.accept(value);
            this.applyLiveChanges();
        };
    }

    private Consumer<Boolean> modeSetter(@NotNull Consumer<Boolean> setter) {
        return value -> {
            setter.accept(value);
            this.applyLiveChanges();
        };
    }

    private void applyLiveChanges() {
        this.applyAdvancedMode();
        this.applyPose();
    }

    private void restoreOriginalPose() {
        this.applyPoseState(this.originalPose);
    }

    private void applyPoseState(@NotNull PoseState state) {
        this.element.bodyXRot = state.bodyXRot;
        this.element.bodyYRot = state.bodyYRot;
        this.element.bodyZRot = state.bodyZRot;
        this.element.headXRot = state.headXRot;
        this.element.headYRot = state.headYRot;
        this.element.headZRot = state.headZRot;
        this.element.leftArmXRot = state.leftArmXRot;
        this.element.leftArmYRot = state.leftArmYRot;
        this.element.leftArmZRot = state.leftArmZRot;
        this.element.rightArmXRot = state.rightArmXRot;
        this.element.rightArmYRot = state.rightArmYRot;
        this.element.rightArmZRot = state.rightArmZRot;
        this.element.leftLegXRot = state.leftLegXRot;
        this.element.leftLegYRot = state.leftLegYRot;
        this.element.leftLegZRot = state.leftLegZRot;
        this.element.rightLegXRot = state.rightLegXRot;
        this.element.rightLegYRot = state.rightLegYRot;
        this.element.rightLegZRot = state.rightLegZRot;
        this.element.bodyXRotAdvancedMode = state.bodyXRotAdvancedMode;
        this.element.bodyYRotAdvancedMode = state.bodyYRotAdvancedMode;
        this.element.bodyZRotAdvancedMode = state.bodyZRotAdvancedMode;
        this.element.headXRotAdvancedMode = state.headXRotAdvancedMode;
        this.element.headYRotAdvancedMode = state.headYRotAdvancedMode;
        this.element.headZRotAdvancedMode = state.headZRotAdvancedMode;
        this.element.leftArmXRotAdvancedMode = state.leftArmXRotAdvancedMode;
        this.element.leftArmYRotAdvancedMode = state.leftArmYRotAdvancedMode;
        this.element.leftArmZRotAdvancedMode = state.leftArmZRotAdvancedMode;
        this.element.rightArmXRotAdvancedMode = state.rightArmXRotAdvancedMode;
        this.element.rightArmYRotAdvancedMode = state.rightArmYRotAdvancedMode;
        this.element.rightArmZRotAdvancedMode = state.rightArmZRotAdvancedMode;
        this.element.leftLegXRotAdvancedMode = state.leftLegXRotAdvancedMode;
        this.element.leftLegYRotAdvancedMode = state.leftLegYRotAdvancedMode;
        this.element.leftLegZRotAdvancedMode = state.leftLegZRotAdvancedMode;
        this.element.rightLegXRotAdvancedMode = state.rightLegXRotAdvancedMode;
        this.element.rightLegYRotAdvancedMode = state.rightLegYRotAdvancedMode;
        this.element.rightLegZRotAdvancedMode = state.rightLegZRotAdvancedMode;
    }

    protected void applyPose() {
        this.element.bodyXRot = this.bodyXRot;
        this.element.bodyYRot = this.bodyYRot;
        this.element.bodyZRot = this.bodyZRot;
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
        this.element.bodyZRotAdvancedMode = this.bodyZRotAdvancedMode;
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
        this.restoreOriginalPose();
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        this.restoreOriginalPose();
        this.editor.history.saveSnapshot();
        this.applyLiveChanges();
        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.restoreOriginalPose();
    }

    protected static float stringToFloat(@Nullable String s) {
        if (s == null) return 0.0F;
        s = PlaceholderParser.replacePlaceholders(s);
        s = s.replace(" ", "");
        try {
            return Float.parseFloat(s);
        } catch (Exception ignore) {}
        return 0.0F;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull PlayerEntityPoseScreen screen, @Nullable PiPWindow parentWindow) {
        PiPWindow window = new PiPWindow(screen.getTitle())
                .setScreen(screen)
                .setForceFancyMenuUiScale(true)
                .setAlwaysOnTop(false)
                .setForceFocus(false)
                .setBlockMinecraftScreenInputs(false)
                .setMinSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT)
                .setSize(PIP_WINDOW_WIDTH, PIP_WINDOW_HEIGHT);
        PiPWindowHandler.INSTANCE.openWindowCentered(window, parentWindow);
        return window;
    }

    public static @NotNull PiPWindow openInWindow(@NotNull PlayerEntityPoseScreen screen) {
        return openInWindow(screen, null);
    }

    public class RotationCell extends RenderCell {

        public AbstractWidget activeWidget;
        public ExtendedButton rotationStringButton;
        public RangeSlider rotationSlider;
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
                Component title = Component.translatable("fancymenu.elements.player_entity.pose.advanced." + localizationKeySuffix);
                TextEditorWindowBody s = new TextEditorWindowBody(title, null, call -> {
                    if (call != null) {
                        rotationValueSetter.accept(call);
                    }
                });
                s.setText(rotationValueGetter.get());
                Dialogs.openGeneric(s, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
            });
            UIBase.applyDefaultWidgetSkinTo(this.rotationStringButton);

            this.rotationSlider = new RangeSlider(0, 0, 20, 20, Component.empty(), -180.0D, 180.0D, stringToFloat(rotationValueGetter.get()));
            this.rotationSlider.setShowAsInteger(true);
            this.rotationSlider.setRoundingDecimalPlace(-1);
            this.rotationSlider.setLabelSupplier(consumes -> Component.translatable("fancymenu.elements.player_entity.pose." + localizationKeySuffix, Component.literal(((RangeSlider)consumes).getValueDisplayText())));
            this.rotationSlider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
                if (!advancedModeGetter.get()) rotationValueSetter.accept("" + (float)((RangeSlider)slider1).getRangeValue());
            });
            UIBase.applyDefaultWidgetSkinTo(this.rotationSlider);

            this.activeWidget = advancedModeGetter.get() ? this.rotationStringButton : this.rotationSlider;
            this.children().add(this.activeWidget);

            this.children().add(this.toggleModeButton);

        }

        @Override
        public void renderCell(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

            int toggleModeButtonWidth = Minecraft.getInstance().font.width(this.toggleModeButton.getLabelSupplier().get(this.toggleModeButton)) + 6;

            this.activeWidget.setX(this.getX());
            this.activeWidget.setY(this.getY());
            this.activeWidget.setWidth(this.getWidth() - toggleModeButtonWidth - 5);

            this.toggleModeButton.setX(this.getX() + this.getWidth() - toggleModeButtonWidth);
            this.toggleModeButton.setY(this.getY());
            this.toggleModeButton.setWidth(toggleModeButtonWidth);

        }

    }

    private static final class PoseState {

        private final String bodyXRot;
        private final String bodyYRot;
        private final String bodyZRot;
        private final String headXRot;
        private final String headYRot;
        private final String headZRot;
        private final String leftArmXRot;
        private final String leftArmYRot;
        private final String leftArmZRot;
        private final String rightArmXRot;
        private final String rightArmYRot;
        private final String rightArmZRot;
        private final String leftLegXRot;
        private final String leftLegYRot;
        private final String leftLegZRot;
        private final String rightLegXRot;
        private final String rightLegYRot;
        private final String rightLegZRot;
        private final boolean bodyXRotAdvancedMode;
        private final boolean bodyYRotAdvancedMode;
        private final boolean bodyZRotAdvancedMode;
        private final boolean headXRotAdvancedMode;
        private final boolean headYRotAdvancedMode;
        private final boolean headZRotAdvancedMode;
        private final boolean leftArmXRotAdvancedMode;
        private final boolean leftArmYRotAdvancedMode;
        private final boolean leftArmZRotAdvancedMode;
        private final boolean rightArmXRotAdvancedMode;
        private final boolean rightArmYRotAdvancedMode;
        private final boolean rightArmZRotAdvancedMode;
        private final boolean leftLegXRotAdvancedMode;
        private final boolean leftLegYRotAdvancedMode;
        private final boolean leftLegZRotAdvancedMode;
        private final boolean rightLegXRotAdvancedMode;
        private final boolean rightLegYRotAdvancedMode;
        private final boolean rightLegZRotAdvancedMode;

        private PoseState(@NotNull PlayerEntityElement element) {
            this.bodyXRot = element.bodyXRot;
            this.bodyYRot = element.bodyYRot;
            this.bodyZRot = element.bodyZRot;
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
            this.bodyZRotAdvancedMode = element.bodyZRotAdvancedMode;
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

        private static @NotNull PoseState capture(@NotNull PlayerEntityElement element) {
            return new PoseState(element);
        }
    }

}
