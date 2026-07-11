package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Creates and configures the keyframe manager's controls. */
final class KeyframeManagerControls {

    private static final int BASE_BUTTON_WIDTH = 60;

    private KeyframeManagerControls() {
    }

    @NotNull
    static AbstractWidget initialize(@NotNull KeyframeManagerScreen screen) {
        addTopControls(screen);
        addPrimaryControls(screen);
        AbstractWidget farRightWidget = addSecondaryControls(screen);
        addEditInputs(screen);
        return farRightWidget;
    }

    private static void addTopControls(@NotNull KeyframeManagerScreen screen) {
        ExtendedButton cancelButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(10, 10, BASE_BUTTON_WIDTH, 20, Component.translatable("gui.cancel"), button -> screen.resultCallback.accept(null)));
        ExtendedButton doneButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(80, 10, BASE_BUTTON_WIDTH, 20, Component.translatable("gui.done"), button -> screen.resultCallback.accept(new KeyframeEditorResult(screen.workingKeyframes, screen.isOffsetMode))));
        screen.addManagerWidget(cancelButton);
        screen.addManagerWidget(doneButton);
    }

    private static void addPrimaryControls(@NotNull KeyframeManagerScreen screen) {
        ExtendedButton playButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.empty(), button -> screen.togglePlayback()));
        playButton.setLabelSupplier(consumes -> Component.translatable(screen.isPlaying ? "fancymenu.elements.animation_controller.keyframe_manager.pause" : "fancymenu.elements.animation_controller.keyframe_manager.play"));
        playButton.setIsActiveSupplier(consumes -> !screen.isRecording);
        screen.addBottomWidget(1, 0, playButton);

        ExtendedButton recordingButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.empty(), button -> screen.toggleRecording()));
        recordingButton.setLabelSupplier(consumes -> Component.translatable(screen.isRecording ? "fancymenu.elements.animation_controller.keyframe_manager.stop_recording" : "fancymenu.elements.animation_controller.keyframe_manager.start_recording"));
        recordingButton.setIsActiveSupplier(consumes -> !screen.isPlaying);
        screen.addBottomWidget(1, 0, recordingButton);

        screen.recordingSpeedSlider = new RangeSlider(0, 0, BASE_BUTTON_WIDTH + 60, 20, Component.empty(), 0, 100, screen.recordingSpeed * 100);
        screen.recordingSpeedSlider.setShowAsInteger(true);
        screen.recordingSpeedSlider.setLabelSupplier(slider -> Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording_speed", slider.getValueDisplayText() + "%"));
        screen.recordingSpeedSlider.setSliderValueUpdateListener((slider, valueDisplayText, value) -> screen.setRecordingSpeed(value));
        screen.recordingSpeedSlider.setFocusable(true);
        screen.recordingSpeedSlider.setNavigatable(true);
        screen.addBottomWidget(1, 0, screen.recordingSpeedSlider);

        ExtendedButton addKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.add_keyframe"), button -> screen.addKeyframeAtProgress()));
        addKeyframeButton.setIsActiveSupplier(consumes -> screen.isRecording && screen.selectedKeyframes.isEmpty());
        screen.addBottomWidget(1, 0, addKeyframeButton);

        ExtendedButton deleteKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.delete_keyframe"), button -> screen.deleteSelectedKeyframes()));
        deleteKeyframeButton.setIsActiveSupplier(consumes -> !screen.selectedKeyframes.isEmpty());
        screen.addBottomWidget(1, 0, deleteKeyframeButton);

        ExtendedButton smoothingButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.smoothing"), button -> screen.toggleSmoothingInput()));
        smoothingButton.setIsActiveSupplier(consumes -> !screen.isPlaying && !screen.isRecording && (screen.selectedKeyframes.size() > 1) && !screen.isShowingTimestampInput);
        smoothingButton.setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.smoothing.desc")));
        screen.addBottomWidget(1, 0, smoothingButton);

        CycleButton<?> offsetModeButton = new CycleButton<>(0, 0, BASE_BUTTON_WIDTH, 0, CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.offset_mode", screen.isOffsetMode), (value, button) -> screen.setOffsetMode(value.getAsBoolean()));
        offsetModeButton.setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.offset_mode.desc")));
        screen.addBottomWidget(1, 0, offsetModeButton);
    }

    @NotNull
    private static AbstractWidget addSecondaryControls(@NotNull KeyframeManagerScreen screen) {
        ExtendedButton undoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.editor.edit.undo"), button -> screen.undo()));
        undoButton.setIsActiveSupplier(consumes -> screen.editorState.canUndo() && !screen.isPlaying);
        screen.addBottomWidget(2, 0, undoButton);

        ExtendedButton redoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.editor.edit.redo"), button -> screen.redo()));
        redoButton.setIsActiveSupplier(consumes -> screen.editorState.canRedo() && !screen.isPlaying);
        screen.addBottomWidget(2, 0, redoButton);

        List<ElementAnchorPoint> anchorPoints = ElementAnchorPoints.getAnchorPoints();
        anchorPoints.remove(ElementAnchorPoints.ELEMENT);
        anchorPoints.remove(ElementAnchorPoints.VANILLA);
        screen.anchorButton = new CycleButton<>(0, 0, BASE_BUTTON_WIDTH + 105, 0, CommonCycles.cycle("fancymenu.elements.animation_controller.keyframe_manager.anchor_point_cycle", anchorPoints, ElementAnchorPoints.TOP_LEFT).setValueNameSupplier(ElementAnchorPoint::getName).setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())), (value, button) -> screen.setAnchorPoint(value));
        screen.anchorButton.setIsActiveSupplier(consumes -> ((screen.selectedKeyframes.size() == 1) || screen.isRecording) && !screen.isOffsetMode);
        screen.addBottomWidget(2, 0, screen.anchorButton);

        screen.stickyButton = new CycleButton<>(0, 0, BASE_BUTTON_WIDTH + 65, 0, CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.sticky"), (value, button) -> screen.setStickyAnchor(value.getAsBoolean()));
        screen.stickyButton.setIsActiveSupplier(consumes -> ((screen.selectedKeyframes.size() == 1) || screen.isRecording) && !screen.isOffsetMode);
        screen.addBottomWidget(2, 0, screen.stickyButton);

        ExtendedButton timestampButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, BASE_BUTTON_WIDTH, 0, Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit"), button -> screen.toggleTimestampInput()));
        timestampButton.setIsActiveSupplier(consumes -> !screen.isPlaying && !screen.isRecording && (screen.selectedKeyframes.size() == 1) && !screen.isShowingSmoothingInput);
        timestampButton.setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit.desc")));
        screen.addBottomWidget(2, 0, timestampButton);

        CycleButton<?> previewMovingButton = new CycleButton<>(0, 0, BASE_BUTTON_WIDTH + 65, 0, CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.move_preview_with_arrow_keys", FancyMenu.getOptions().arrowKeysMovePreview.getValue()), (value, button) -> FancyMenu.getOptions().arrowKeysMovePreview.setValue(value.getAsBoolean()));
        previewMovingButton.setUITooltipSupplier(consumes -> UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.move_preview_with_arrow_keys.desc")));
        screen.addBottomWidget(2, 0, previewMovingButton);
        return previewMovingButton;
    }

    private static void addEditInputs(@NotNull KeyframeManagerScreen screen) {
        screen.smoothingDistanceInput = createLabeledInput(screen, "fancymenu.elements.animation_controller.keyframe_manager.smoothing.input");
        screen.smoothingDistanceInput.setCharacterFilter(CharacterFilter.buildIntegerFilter());
        screen.smoothingDistanceInput.setIsVisibleSupplier(consumes -> screen.isShowingSmoothingInput);
        screen.smoothingDistanceInput.setMaxLength(6);
        screen.addManagerWidget(screen.smoothingDistanceInput);

        screen.timestampInput = createLabeledInput(screen, "fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit.input");
        screen.timestampInput.setCharacterFilter(CharacterFilter.buildIntegerFilter());
        screen.timestampInput.setIsVisibleSupplier(consumes -> screen.isShowingTimestampInput);
        screen.timestampInput.setMaxLength(20);
        screen.addManagerWidget(screen.timestampInput);
    }

    @NotNull
    private static ExtendedEditBox createLabeledInput(@NotNull KeyframeManagerScreen screen, @NotNull String labelKey) {
        ExtendedEditBox input = new ExtendedEditBox(Minecraft.getInstance().font, (screen.width / 2) - 50, screen.stickyButton.y - 40, 100, 20, Component.empty()) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                MutableComponent label = Component.translatable(labelKey);
                int labelWidth = Minecraft.getInstance().font.width(label);
                graphics.drawString(Minecraft.getInstance().font, label, this.x + (this.getWidth() / 2) - (labelWidth / 2), this.y - Minecraft.getInstance().font.lineHeight - 5, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);
                super.renderWidget(graphics, mouseX, mouseY, partial);
            }
        };
        return UIBase.applyDefaultWidgetSkinTo(input);
    }

}
