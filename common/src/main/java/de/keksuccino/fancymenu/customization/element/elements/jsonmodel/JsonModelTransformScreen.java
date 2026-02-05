package de.keksuccino.fancymenu.customization.element.elements.jsonmodel;

import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.properties.Property;
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

public class JsonModelTransformScreen extends PiPCellWindowBody {

    public static final int PIP_WINDOW_WIDTH = 520;
    public static final int PIP_WINDOW_HEIGHT = 360;

    protected final JsonModelElement element;
    protected final LayoutEditorScreen editor;

    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelScale;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelOffsetX;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelOffsetY;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelOffsetZ;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelRotationX;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelRotationY;
    protected final Property.ManualInputProperty.ManualInputSnapshot<Float> originalModelRotationZ;

    protected boolean modelScaleAdvancedMode;
    protected boolean modelOffsetXAdvancedMode;
    protected boolean modelOffsetYAdvancedMode;
    protected boolean modelOffsetZAdvancedMode;
    protected boolean modelRotationXAdvancedMode;
    protected boolean modelRotationYAdvancedMode;
    protected boolean modelRotationZAdvancedMode;

    protected JsonModelTransformScreen(@NotNull JsonModelElement element, @NotNull LayoutEditorScreen editor) {
        super(Component.translatable("fancymenu.elements.json_model.menu.transform"));
        this.element = element;
        this.editor = editor;

        this.originalModelScale = element.modelScale.createValueSnapshot();
        this.originalModelOffsetX = element.modelOffsetX.createValueSnapshot();
        this.originalModelOffsetY = element.modelOffsetY.createValueSnapshot();
        this.originalModelOffsetZ = element.modelOffsetZ.createValueSnapshot();
        this.originalModelRotationX = element.modelRotationX.createValueSnapshot();
        this.originalModelRotationY = element.modelRotationY.createValueSnapshot();
        this.originalModelRotationZ = element.modelRotationZ.createValueSnapshot();

        this.modelScaleAdvancedMode = element.modelScale.hasManualInput();
        this.modelOffsetXAdvancedMode = element.modelOffsetX.hasManualInput();
        this.modelOffsetYAdvancedMode = element.modelOffsetY.hasManualInput();
        this.modelOffsetZAdvancedMode = element.modelOffsetZ.hasManualInput();
        this.modelRotationXAdvancedMode = element.modelRotationX.hasManualInput();
        this.modelRotationYAdvancedMode = element.modelRotationY.hasManualInput();
        this.modelRotationZAdvancedMode = element.modelRotationZ.hasManualInput();
    }

    @Override
    protected void initCells() {
        this.addSpacerCell(20);

        this.addLabelCell(Component.translatable("fancymenu.elements.json_model.transform.section.scale"));
        this.addCell(new TransformCell("model_scale",
                this.element.modelScale,
                () -> this.modelScaleAdvancedMode,
                this.modeSetter(value -> this.modelScaleAdvancedMode = value),
                0.0D, 5.0D, false, 2));

        this.addLabelCell(Component.translatable("fancymenu.elements.json_model.transform.section.offset"));
        this.addCell(new TransformCell("model_offset_x",
                this.element.modelOffsetX,
                () -> this.modelOffsetXAdvancedMode,
                this.modeSetter(value -> this.modelOffsetXAdvancedMode = value),
                -200.0D, 200.0D, false, 2));
        this.addCell(new TransformCell("model_offset_y",
                this.element.modelOffsetY,
                () -> this.modelOffsetYAdvancedMode,
                this.modeSetter(value -> this.modelOffsetYAdvancedMode = value),
                -200.0D, 200.0D, false, 2));
        this.addCell(new TransformCell("model_offset_z",
                this.element.modelOffsetZ,
                () -> this.modelOffsetZAdvancedMode,
                this.modeSetter(value -> this.modelOffsetZAdvancedMode = value),
                -200.0D, 200.0D, false, 2));

        this.addLabelCell(Component.translatable("fancymenu.elements.json_model.transform.section.rotation"));
        this.addCell(new TransformCell("model_rotation_x",
                this.element.modelRotationX,
                () -> this.modelRotationXAdvancedMode,
                this.modeSetter(value -> this.modelRotationXAdvancedMode = value),
                -180.0D, 180.0D, true, -1));
        this.addCell(new TransformCell("model_rotation_y",
                this.element.modelRotationY,
                () -> this.modelRotationYAdvancedMode,
                this.modeSetter(value -> this.modelRotationYAdvancedMode = value),
                -180.0D, 180.0D, true, -1));
        this.addCell(new TransformCell("model_rotation_z",
                this.element.modelRotationZ,
                () -> this.modelRotationZAdvancedMode,
                this.modeSetter(value -> this.modelRotationZAdvancedMode = value),
                -180.0D, 180.0D, true, -1));

        this.addSpacerCell(20);
    }

    private Consumer<Boolean> modeSetter(@NotNull Consumer<Boolean> setter) {
        return value -> {
            setter.accept(value);
            this.applyLiveChanges();
        };
    }

    private void applyLiveChanges() {
        // Properties update live as values change; this keeps parity with other PiP editors.
    }

    private void restoreOriginalTransform() {
        this.applySnapshot(this.element.modelScale, this.originalModelScale);
        this.applySnapshot(this.element.modelOffsetX, this.originalModelOffsetX);
        this.applySnapshot(this.element.modelOffsetY, this.originalModelOffsetY);
        this.applySnapshot(this.element.modelOffsetZ, this.originalModelOffsetZ);
        this.applySnapshot(this.element.modelRotationX, this.originalModelRotationX);
        this.applySnapshot(this.element.modelRotationY, this.originalModelRotationY);
        this.applySnapshot(this.element.modelRotationZ, this.originalModelRotationZ);
    }

    private void applySnapshot(@NotNull Property.FloatProperty property, @NotNull Property.ManualInputProperty.ManualInputSnapshot<Float> snapshot) {
        property.applyValueSnapshot(snapshot);
    }

    @Override
    protected void onCancel() {
        this.restoreOriginalTransform();
        this.closeWindow();
    }

    @Override
    protected void onDone() {
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelScaleSnapshot = this.element.modelScale.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelOffsetXSnapshot = this.element.modelOffsetX.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelOffsetYSnapshot = this.element.modelOffsetY.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelOffsetZSnapshot = this.element.modelOffsetZ.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelRotationXSnapshot = this.element.modelRotationX.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelRotationYSnapshot = this.element.modelRotationY.createValueSnapshot();
        Property.ManualInputProperty.ManualInputSnapshot<Float> modelRotationZSnapshot = this.element.modelRotationZ.createValueSnapshot();

        this.restoreOriginalTransform();
        this.editor.history.saveSnapshot();

        this.applySnapshot(this.element.modelScale, modelScaleSnapshot);
        this.applySnapshot(this.element.modelOffsetX, modelOffsetXSnapshot);
        this.applySnapshot(this.element.modelOffsetY, modelOffsetYSnapshot);
        this.applySnapshot(this.element.modelOffsetZ, modelOffsetZSnapshot);
        this.applySnapshot(this.element.modelRotationX, modelRotationXSnapshot);
        this.applySnapshot(this.element.modelRotationY, modelRotationYSnapshot);
        this.applySnapshot(this.element.modelRotationZ, modelRotationZSnapshot);

        this.closeWindow();
    }

    @Override
    public void onWindowClosedExternally() {
        this.restoreOriginalTransform();
    }

    public static @NotNull PiPWindow openInWindow(@NotNull JsonModelTransformScreen screen, @Nullable PiPWindow parentWindow) {
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

    public static @NotNull PiPWindow openInWindow(@NotNull JsonModelTransformScreen screen) {
        return openInWindow(screen, null);
    }

    public class TransformCell extends RenderCell {

        public AbstractWidget activeWidget;
        public ExtendedButton advancedInputButton;
        public RangeSlider valueSlider;
        public CycleButton<CommonCycles.CycleEnabledDisabled> toggleModeButton;

        public TransformCell(@NotNull String localizationKeySuffix, @NotNull Property.FloatProperty property, @NotNull Supplier<Boolean> advancedModeGetter, @NotNull Consumer<Boolean> advancedModeSetter,
                             double minValue, double maxValue, boolean showAsInteger, int roundingDecimalPlace) {

            this.toggleModeButton = new CycleButton<>(0, 0, 20, 20,
                    CommonCycles.cycleEnabledDisabled("fancymenu.elements.json_model.transform.advanced", advancedModeGetter.get()),
                    (value, button) -> {
                        boolean advanced = value.getAsBoolean();
                        advancedModeSetter.accept(advanced);
                        if (!advanced) {
                            float resolved = property.getFloat();
                            property.set(resolved);
                            this.valueSlider.setRangeValue(resolved);
                        }
                        if (this.activeWidget != null) this.children().remove(this.activeWidget);
                        this.activeWidget = advanced ? this.advancedInputButton : this.valueSlider;
                        this.children().add(this.activeWidget);
                    });
            UIBase.applyDefaultWidgetSkinTo(this.toggleModeButton);

            this.advancedInputButton = new ExtendedButton(0, 0, 20, 20, Component.translatable("fancymenu.elements.json_model.transform.advanced." + localizationKeySuffix), button -> {
                Component title = Component.translatable("fancymenu.elements.json_model.transform.advanced." + localizationKeySuffix);
                TextEditorWindowBody editor = new TextEditorWindowBody(title, null, call -> {
                    if (call != null) {
                        property.setManualInput(call);
                    }
                });
                String currentValue = property.getManualInput();
                if (currentValue == null || currentValue.isEmpty()) {
                    currentValue = String.valueOf(property.getFloat());
                }
                editor.setText(currentValue);
                Dialogs.openGeneric(editor, title, null, TextEditorWindowBody.PIP_WINDOW_WIDTH, TextEditorWindowBody.PIP_WINDOW_HEIGHT);
            });
            UIBase.applyDefaultWidgetSkinTo(this.advancedInputButton);

            double preset = property.getFloat();
            this.valueSlider = new RangeSlider(0, 0, 20, 20, Component.empty(), minValue, maxValue, preset);
            this.valueSlider.setShowAsInteger(showAsInteger);
            this.valueSlider.setRoundingDecimalPlace(roundingDecimalPlace);
            this.valueSlider.setLabelSupplier(consumes -> Component.translatable("fancymenu.elements.json_model.transform." + localizationKeySuffix,
                    Component.literal(((RangeSlider)consumes).getValueDisplayText())));
            this.valueSlider.setSliderValueUpdateListener((slider1, valueDisplayText, value) -> {
                if (!advancedModeGetter.get()) {
                    property.set((float)((RangeSlider)slider1).getRangeValue());
                }
            });
            UIBase.applyDefaultWidgetSkinTo(this.valueSlider);

            this.activeWidget = advancedModeGetter.get() ? this.advancedInputButton : this.valueSlider;
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
}
