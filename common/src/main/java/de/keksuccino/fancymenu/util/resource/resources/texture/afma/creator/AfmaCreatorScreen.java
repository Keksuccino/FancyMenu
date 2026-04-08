package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseDirectoryWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class AfmaCreatorScreen extends Screen {

    private static final int OUTER_PADDING = 20;
    private static final int PANEL_PADDING = 12;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int SECTION_GAP = 46;
    private static final int PANEL_TOP = 40;
    private static final int INLINE_LABEL_GAP = 8;
    private static final int COLUMN_GAP = 14;
    private static final int FIELD_BUTTON_GAP = 8;
    private static final int BUTTON_GROUP_GAP = 4;
    private static final int MIN_INPUT_WIDTH = 120;

    private final @NotNull Screen parentScreen;
    private final @NotNull AfmaCreatorState state = new AfmaCreatorState();
    private final @NotNull List<PiPWindow> childWindows = new ArrayList<>();
    private boolean syncingWidgets = false;
    private @Nullable AfmaEncodeJob handledTerminalJob = null;

    private @Nullable ExtendedEditBox mainFramesPathEditBox;
    private @Nullable ExtendedEditBox introFramesPathEditBox;
    private @Nullable ExtendedEditBox outputPathEditBox;
    private @Nullable ExtendedEditBox frameTimeEditBox;
    private @Nullable ExtendedEditBox introFrameTimeEditBox;
    private @Nullable ExtendedEditBox loopCountEditBox;
    private @Nullable ExtendedEditBox keyframeIntervalEditBox;
    private @Nullable ExtendedEditBox adaptiveMaxKeyframeIntervalEditBox;
    private @Nullable ExtendedEditBox adaptiveContinuationMinSavingsBytesEditBox;
    private @Nullable ExtendedEditBox adaptiveContinuationMinSavingsRatioEditBox;
    private @Nullable ExtendedEditBox perceptualVisibleColorDeltaEditBox;
    private @Nullable ExtendedEditBox perceptualAlphaDeltaEditBox;
    private @Nullable ExtendedEditBox perceptualAverageErrorEditBox;
    private @Nullable ExtendedEditBox maxCopySearchDistanceEditBox;
    private @Nullable ExtendedEditBox maxCandidateAxisOffsetsEditBox;

    private @Nullable ExtendedButton browseMainFramesButton;
    private @Nullable ExtendedButton browseIntroFramesButton;
    private @Nullable ExtendedButton clearIntroFramesButton;
    private @Nullable ExtendedButton browseOutputButton;
    private @Nullable ExtendedButton exportButton;
    private @Nullable ExtendedButton cancelJobButton;
    private @Nullable ExtendedButton closeButton;

    private @Nullable CycleButton<AfmaOptimizationPreset> presetCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> rectCopyCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> duplicateCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> nearLosslessCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> adaptiveKeyframeCycleButton;

    public AfmaCreatorScreen(@NotNull Screen parentScreen) {
        super(Component.translatable("fancymenu.afma.creator.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.childWindows.removeIf(window -> window == null);

        this.mainFramesPathEditBox = this.addStyledEditBox(Component.translatable("fancymenu.afma.creator.main_frames"));
        this.mainFramesPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setMainFramesDirectory(pathToDirectory(value));
        });

        this.introFramesPathEditBox = this.addStyledEditBox(Component.translatable("fancymenu.afma.creator.intro_frames"));
        this.introFramesPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setIntroFramesDirectory(pathToDirectory(value));
        });

        this.outputPathEditBox = this.addStyledEditBox(Component.translatable("fancymenu.afma.creator.output_file"));
        this.outputPathEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setOutputFile(pathToFile(value));
        });

        this.frameTimeEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.frame_time"));
        this.frameTimeEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setFrameTimeMs(Long.parseLong(value));
        });

        this.introFrameTimeEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.intro_frame_time"));
        this.introFrameTimeEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setIntroFrameTimeMs(Long.parseLong(value));
        });

        this.loopCountEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.loop_count"));
        this.loopCountEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setLoopCount(Integer.parseInt(value));
        });

        this.keyframeIntervalEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.keyframe_interval"));
        this.keyframeIntervalEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setKeyframeInterval(Integer.parseInt(value));
        });

        this.adaptiveMaxKeyframeIntervalEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.adaptive_max_keyframe_interval"));
        this.adaptiveMaxKeyframeIntervalEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setAdaptiveMaxKeyframeInterval(Integer.parseInt(value));
        });

        this.adaptiveContinuationMinSavingsBytesEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_bytes"));
        this.adaptiveContinuationMinSavingsBytesEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setAdaptiveContinuationMinSavingsBytes(Long.parseLong(value));
        });

        this.adaptiveContinuationMinSavingsRatioEditBox = this.addStyledDecimalEditBox(Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_ratio"));
        this.adaptiveContinuationMinSavingsRatioEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            double parsedValue = parseDoubleOrDefault(value, Double.NaN);
            if (Double.isFinite(parsedValue)) this.state.setAdaptiveContinuationMinSavingsRatio(parsedValue);
        });

        this.perceptualVisibleColorDeltaEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.perceptual_visible_color_delta"));
        this.perceptualVisibleColorDeltaEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setPerceptualBinIntraMaxVisibleColorDelta(Integer.parseInt(value));
        });

        this.perceptualAlphaDeltaEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.perceptual_alpha_delta"));
        this.perceptualAlphaDeltaEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setPerceptualBinIntraMaxAlphaDelta(Integer.parseInt(value));
        });

        this.perceptualAverageErrorEditBox = this.addStyledDecimalEditBox(Component.translatable("fancymenu.afma.creator.perceptual_average_error"));
        this.perceptualAverageErrorEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            double parsedValue = parseDoubleOrDefault(value, Double.NaN);
            if (Double.isFinite(parsedValue)) this.state.setPerceptualBinIntraMaxAverageError(parsedValue);
        });

        this.maxCopySearchDistanceEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.max_copy_search_distance"));
        this.maxCopySearchDistanceEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setMaxCopySearchDistance(Integer.parseInt(value));
        });

        this.maxCandidateAxisOffsetsEditBox = this.addStyledNumberEditBox(Component.translatable("fancymenu.afma.creator.max_candidate_axis_offsets"));
        this.maxCandidateAxisOffsetsEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            if (isInteger(value)) this.state.setMaxCandidateAxisOffsets(Integer.parseInt(value));
        });

        this.browseMainFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getMainFramesDirectory(), this.state::setMainFramesDirectory));
        this.browseIntroFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getIntroFramesDirectory(), this.state::setIntroFramesDirectory));
        this.clearIntroFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.clear"), button -> {
            this.state.clearIntroFramesDirectory();
            this.syncWidgetsFromState();
        });
        this.browseOutputButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openOutputChooser(this.state.getOutputFile()));

        LocalizedEnumValueCycle<AfmaOptimizationPreset> presetCycle = LocalizedEnumValueCycle.ofArray(
                "fancymenu.afma.creator.optimization_preset.label",
                AfmaOptimizationPreset.values()
        );
        presetCycle.setCurrentValue(this.state.getOptimizationPreset(), false);
        this.presetCycleButton = new CycleButton<>(0, 0, 200, FIELD_HEIGHT,
                presetCycle,
                (value, button) -> {
                    this.state.applyPreset(value);
                    this.syncWidgetsFromState();
                });
        UIBase.applyDefaultWidgetSkinTo(this.presetCycleButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.presetCycleButton);

        this.rectCopyCycleButton = this.addToggleButton("fancymenu.afma.creator.rect_copy", this.state.isRectCopyEnabled(), value -> this.state.setRectCopyEnabled(value));
        this.duplicateCycleButton = this.addToggleButton("fancymenu.afma.creator.duplicate_elision", this.state.isDuplicateFrameElision(), value -> this.state.setDuplicateFrameElision(value));
        this.nearLosslessCycleButton = this.addToggleButton("fancymenu.afma.creator.near_lossless", this.state.isNearLosslessEnabled(), value -> this.state.setNearLosslessEnabled(value));
        this.adaptiveKeyframeCycleButton = this.addToggleButton("fancymenu.afma.creator.adaptive_keyframes", this.state.isAdaptiveKeyframePlacement(), value -> this.state.setAdaptiveKeyframePlacement(value));

        this.exportButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.export"), button -> this.startExport());
        this.cancelJobButton = this.addStyledButton(Component.translatable("fancymenu.common_components.cancel"), button -> this.state.cancelCurrentJob());
        this.closeButton = this.addStyledButton(Component.translatable("fancymenu.common.close"), button -> this.onClose());

        this.configureTooltips();
        this.syncWidgetsFromState();
        this.repositionWidgets();
    }

    @Override
    public void tick() {
        super.tick();

        this.handleCompletedJobIfNeeded();
        this.updateButtonStates();
        this.repositionWidgets();
    }

    protected void handleCompletedJobIfNeeded() {
        AfmaEncodeJob job = this.state.getCurrentJob();
        if ((job != null) && !job.isRunning() && (job != this.handledTerminalJob)) {
            this.handledTerminalJob = job;
            if ((job.getStatus() == AfmaEncodeJob.Status.FAILED) && (job.getFailure() != null)) {
                Dialogs.openMessage(Component.literal(job.getFailure().getMessage() != null ? job.getFailure().getMessage() : "AFMA creator job failed."), MessageDialogStyle.ERROR);
            } else if ((job.getStatus() == AfmaEncodeJob.Status.SUCCEEDED) && (job.getOutputFile() != null)) {
                Dialogs.openMessage(Component.translatable("fancymenu.afma.creator.export.success", fileToPath(job.getOutputFile())), MessageDialogStyle.INFO);
            }
        }
    }

    protected void startExport() {
        try {
            this.syncStateFromWidgets();
            this.state.startExport();
        } catch (Exception ex) {
            Dialogs.openMessage(Component.literal(ex.getMessage() != null ? ex.getMessage() : "AFMA export failed to start."), MessageDialogStyle.ERROR);
        }
    }

    protected void updateButtonStates() {
        boolean jobRunning = this.state.isJobRunning();
        if (this.exportButton != null) this.exportButton.active = !jobRunning;
        if (this.cancelJobButton != null) {
            this.cancelJobButton.active = jobRunning;
            this.cancelJobButton.visible = jobRunning;
        }
        if (this.clearIntroFramesButton != null) this.clearIntroFramesButton.active = !this.state.getIntroFramesInputText().isBlank() && !jobRunning;

        boolean adaptiveKeyframesEnabled = this.state.isAdaptiveKeyframePlacement() && !jobRunning;
        this.setWidgetActive(this.adaptiveMaxKeyframeIntervalEditBox, adaptiveKeyframesEnabled);
        this.setWidgetActive(this.adaptiveContinuationMinSavingsBytesEditBox, adaptiveKeyframesEnabled);
        this.setWidgetActive(this.adaptiveContinuationMinSavingsRatioEditBox, adaptiveKeyframesEnabled);

        boolean perceptualControlsEnabled = this.state.isNearLosslessEnabled() && !jobRunning;
        this.setWidgetActive(this.perceptualVisibleColorDeltaEditBox, perceptualControlsEnabled);
        this.setWidgetActive(this.perceptualAlphaDeltaEditBox, perceptualControlsEnabled);
        this.setWidgetActive(this.perceptualAverageErrorEditBox, perceptualControlsEnabled);

        boolean generalAdvancedControlsEnabled = !jobRunning;
        this.setWidgetActive(this.mainFramesPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.introFramesPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.outputPathEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.frameTimeEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.introFrameTimeEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.loopCountEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.keyframeIntervalEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.maxCopySearchDistanceEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.maxCandidateAxisOffsetsEditBox, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.presetCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.rectCopyCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.duplicateCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.nearLosslessCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.adaptiveKeyframeCycleButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseMainFramesButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseIntroFramesButton, generalAdvancedControlsEnabled);
        this.setWidgetActive(this.browseOutputButton, generalAdvancedControlsEnabled);
    }

    protected void openDirectoryChooser(@Nullable File initialDirectory, @NotNull java.util.function.Consumer<File> callback) {
        File startDirectory = (initialDirectory != null && initialDirectory.isDirectory()) ? initialDirectory : GameDirectoryUtils.getGameDirectory();
        ChooseDirectoryWindowBody chooser = new ChooseDirectoryWindowBody(GameDirectoryUtils.getGameDirectory(), startDirectory, directory -> {
            if (directory != null) {
                callback.accept(directory);
                this.syncWidgetsFromState();
            }
        });
        this.childWindows.add(chooser.openInWindow(null));
    }

    protected void openOutputChooser(@Nullable File initialOutput) {
        File gameDir = GameDirectoryUtils.getGameDirectory();
        File startDir = (initialOutput != null && initialOutput.getParentFile() != null && initialOutput.getParentFile().isDirectory()) ? initialOutput.getParentFile() : gameDir;
        String fileName = (initialOutput != null) ? initialOutput.getName() : "animation.afma";
        SaveFileWindowBody chooser = new SaveFileWindowBody(gameDir, startDir, fileName, "afma", file -> {
            if (file != null) {
                this.state.setOutputFile(file);
                this.syncWidgetsFromState();
            }
        });
        chooser.setForceResourceFriendlyFileNames(false);
        this.childWindows.add(chooser.openInWindow(null));
    }

    protected void syncWidgetsFromState() {
        this.syncingWidgets = true;
        try {
            if (this.mainFramesPathEditBox != null) this.mainFramesPathEditBox.setValue(this.state.getMainFramesInputText());
            if (this.introFramesPathEditBox != null) this.introFramesPathEditBox.setValue(this.state.getIntroFramesInputText());
            if (this.outputPathEditBox != null) this.outputPathEditBox.setValue(fileToPath(this.state.getOutputFile()));
            if (this.frameTimeEditBox != null) this.frameTimeEditBox.setValue(String.valueOf(this.state.getFrameTimeMs()));
            if (this.introFrameTimeEditBox != null) this.introFrameTimeEditBox.setValue(String.valueOf(this.state.getIntroFrameTimeMs()));
            if (this.loopCountEditBox != null) this.loopCountEditBox.setValue(String.valueOf(this.state.getLoopCount()));
            if (this.keyframeIntervalEditBox != null) this.keyframeIntervalEditBox.setValue(String.valueOf(this.state.getKeyframeInterval()));
            if (this.adaptiveMaxKeyframeIntervalEditBox != null) this.adaptiveMaxKeyframeIntervalEditBox.setValue(String.valueOf(this.state.getAdaptiveMaxKeyframeInterval()));
            if (this.adaptiveContinuationMinSavingsBytesEditBox != null) this.adaptiveContinuationMinSavingsBytesEditBox.setValue(String.valueOf(this.state.getAdaptiveContinuationMinSavingsBytes()));
            if (this.adaptiveContinuationMinSavingsRatioEditBox != null) this.adaptiveContinuationMinSavingsRatioEditBox.setValue(Double.toString(this.state.getAdaptiveContinuationMinSavingsRatio()));
            if (this.perceptualVisibleColorDeltaEditBox != null) this.perceptualVisibleColorDeltaEditBox.setValue(String.valueOf(this.state.getPerceptualBinIntraMaxVisibleColorDelta()));
            if (this.perceptualAlphaDeltaEditBox != null) this.perceptualAlphaDeltaEditBox.setValue(String.valueOf(this.state.getPerceptualBinIntraMaxAlphaDelta()));
            if (this.perceptualAverageErrorEditBox != null) this.perceptualAverageErrorEditBox.setValue(Double.toString(this.state.getPerceptualBinIntraMaxAverageError()));
            if (this.maxCopySearchDistanceEditBox != null) this.maxCopySearchDistanceEditBox.setValue(String.valueOf(this.state.getMaxCopySearchDistance()));
            if (this.maxCandidateAxisOffsetsEditBox != null) this.maxCandidateAxisOffsetsEditBox.setValue(String.valueOf(this.state.getMaxCandidateAxisOffsets()));
            if (this.presetCycleButton != null) this.presetCycleButton.setSelectedValue(this.state.getOptimizationPreset(), false);
            if (this.rectCopyCycleButton != null) this.rectCopyCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isRectCopyEnabled()), false);
            if (this.duplicateCycleButton != null) this.duplicateCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isDuplicateFrameElision()), false);
            if (this.nearLosslessCycleButton != null) this.nearLosslessCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isNearLosslessEnabled()), false);
            if (this.adaptiveKeyframeCycleButton != null) this.adaptiveKeyframeCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isAdaptiveKeyframePlacement()), false);
        } finally {
            this.syncingWidgets = false;
        }
    }

    protected void syncStateFromWidgets() {
        if (this.syncingWidgets) return;
        if (this.mainFramesPathEditBox != null && !this.state.getMainFramesInputText().equals(this.mainFramesPathEditBox.getValue())) {
            this.state.setMainFramesDirectory(pathToDirectory(this.mainFramesPathEditBox.getValue()));
        }
        if (this.introFramesPathEditBox != null && !this.state.getIntroFramesInputText().equals(this.introFramesPathEditBox.getValue())) {
            this.state.setIntroFramesDirectory(pathToDirectory(this.introFramesPathEditBox.getValue()));
        }
        if (this.outputPathEditBox != null && !fileToPath(this.state.getOutputFile()).equals(this.outputPathEditBox.getValue())) {
            this.state.setOutputFile(pathToFile(this.outputPathEditBox.getValue()));
        }
        if (this.frameTimeEditBox != null) {
            long value = parseLongOrDefault(this.frameTimeEditBox.getValue(), 0L);
            if (value != this.state.getFrameTimeMs()) this.state.setFrameTimeMs(value);
        }
        if (this.introFrameTimeEditBox != null) {
            long value = parseLongOrDefault(this.introFrameTimeEditBox.getValue(), 0L);
            if (value != this.state.getIntroFrameTimeMs()) this.state.setIntroFrameTimeMs(value);
        }
        if (this.loopCountEditBox != null) {
            int value = (int) parseLongOrDefault(this.loopCountEditBox.getValue(), 0L);
            if (value != this.state.getLoopCount()) this.state.setLoopCount(value);
        }
        if (this.keyframeIntervalEditBox != null) {
            int value = (int) parseLongOrDefault(this.keyframeIntervalEditBox.getValue(), 0L);
            if (value != this.state.getKeyframeInterval()) this.state.setKeyframeInterval(value);
        }
        if (this.adaptiveMaxKeyframeIntervalEditBox != null) {
            int value = (int) parseLongOrDefault(this.adaptiveMaxKeyframeIntervalEditBox.getValue(), 0L);
            if (value != this.state.getAdaptiveMaxKeyframeInterval()) this.state.setAdaptiveMaxKeyframeInterval(value);
        }
        if (this.adaptiveContinuationMinSavingsBytesEditBox != null) {
            long value = parseLongOrDefault(this.adaptiveContinuationMinSavingsBytesEditBox.getValue(), 0L);
            if (value != this.state.getAdaptiveContinuationMinSavingsBytes()) this.state.setAdaptiveContinuationMinSavingsBytes(value);
        }
        if (this.adaptiveContinuationMinSavingsRatioEditBox != null) {
            double value = parseDoubleOrDefault(this.adaptiveContinuationMinSavingsRatioEditBox.getValue(), Double.NaN);
            if (Double.isFinite(value) && (Double.compare(value, this.state.getAdaptiveContinuationMinSavingsRatio()) != 0)) {
                this.state.setAdaptiveContinuationMinSavingsRatio(value);
            }
        }
        if (this.perceptualVisibleColorDeltaEditBox != null) {
            int value = (int) parseLongOrDefault(this.perceptualVisibleColorDeltaEditBox.getValue(), 0L);
            if (value != this.state.getPerceptualBinIntraMaxVisibleColorDelta()) this.state.setPerceptualBinIntraMaxVisibleColorDelta(value);
        }
        if (this.perceptualAlphaDeltaEditBox != null) {
            int value = (int) parseLongOrDefault(this.perceptualAlphaDeltaEditBox.getValue(), 0L);
            if (value != this.state.getPerceptualBinIntraMaxAlphaDelta()) this.state.setPerceptualBinIntraMaxAlphaDelta(value);
        }
        if (this.perceptualAverageErrorEditBox != null) {
            double value = parseDoubleOrDefault(this.perceptualAverageErrorEditBox.getValue(), Double.NaN);
            if (Double.isFinite(value) && (Double.compare(value, this.state.getPerceptualBinIntraMaxAverageError()) != 0)) {
                this.state.setPerceptualBinIntraMaxAverageError(value);
            }
        }
        if (this.maxCopySearchDistanceEditBox != null) {
            int value = (int) parseLongOrDefault(this.maxCopySearchDistanceEditBox.getValue(), 0L);
            if (value != this.state.getMaxCopySearchDistance()) this.state.setMaxCopySearchDistance(value);
        }
        if (this.maxCandidateAxisOffsetsEditBox != null) {
            int value = (int) parseLongOrDefault(this.maxCandidateAxisOffsetsEditBox.getValue(), 0L);
            if (value != this.state.getMaxCandidateAxisOffsets()) this.state.setMaxCandidateAxisOffsets(value);
        }
    }

    protected void repositionWidgets() {
        int contentX = this.getContentLeft();
        int contentWidth = this.getContentWidth();
        int inlineLabelWidth = this.getInlineLabelWidth();
        int browseWidth = 84;
        int clearWidth = 64;
        int y = this.getContentStartY();
        y = this.layoutLabeledPathRow(contentX, y, contentWidth, inlineLabelWidth, this.mainFramesPathEditBox, this.browseMainFramesButton, browseWidth, null, 0);
        y += ROW_GAP;
        y = this.layoutLabeledPathRow(contentX, y, contentWidth, inlineLabelWidth, this.introFramesPathEditBox, this.browseIntroFramesButton, browseWidth, this.clearIntroFramesButton, clearWidth);
        y += ROW_GAP;
        y = this.layoutLabeledPathRow(contentX, y, contentWidth, inlineLabelWidth, this.outputPathEditBox, this.browseOutputButton, browseWidth, null, 0);
        y += SECTION_GAP;

        int columnWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnNumericLayout = this.useSingleColumnNumericLayout(columnWidth, inlineLabelWidth);
        if (useSingleColumnNumericLayout) {
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.frameTimeEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.introFrameTimeEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.loopCountEditBox);
            y += SECTION_GAP;
        } else {
            y = this.layoutLabeledFieldRow(contentX, y, columnWidth, inlineLabelWidth, this.frameTimeEditBox);
            this.layoutLabeledFieldRow(contentX + columnWidth + COLUMN_GAP, y, columnWidth, inlineLabelWidth, this.introFrameTimeEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.loopCountEditBox);
            y += SECTION_GAP;
        }

        this.layoutWidget(this.presetCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
        y += ROW_GAP;
        y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.keyframeIntervalEditBox);
        y += ROW_GAP;

        int toggleWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnToggleLayout = toggleWidth < 220;
        if (useSingleColumnToggleLayout) {
            this.layoutWidget(this.rectCopyCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
            y += ROW_GAP;
            this.layoutWidget(this.duplicateCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
            y += ROW_GAP;
            this.layoutWidget(this.nearLosslessCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
        } else {
            this.layoutWidget(this.rectCopyCycleButton, contentX, y, toggleWidth, FIELD_HEIGHT);
            this.layoutWidget(this.duplicateCycleButton, contentX + toggleWidth + COLUMN_GAP, y, toggleWidth, FIELD_HEIGHT);
            y += ROW_GAP;
            this.layoutWidget(this.nearLosslessCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
        }

        y += SECTION_GAP;
        this.layoutWidget(this.adaptiveKeyframeCycleButton, contentX, y, contentWidth, FIELD_HEIGHT);
        y += ROW_GAP;

        int advancedColumnWidth = (contentWidth - COLUMN_GAP) / 2;
        boolean useSingleColumnAdvancedLayout = this.useSingleColumnNumericLayout(advancedColumnWidth, inlineLabelWidth);
        if (useSingleColumnAdvancedLayout) {
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.adaptiveMaxKeyframeIntervalEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsBytesEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsRatioEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.maxCopySearchDistanceEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.maxCandidateAxisOffsetsEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.perceptualVisibleColorDeltaEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.perceptualAlphaDeltaEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, contentWidth, inlineLabelWidth, this.perceptualAverageErrorEditBox);
        } else {
            y = this.layoutLabeledFieldRow(contentX, y, advancedColumnWidth, inlineLabelWidth, this.adaptiveMaxKeyframeIntervalEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, y, advancedColumnWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsBytesEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, advancedColumnWidth, inlineLabelWidth, this.adaptiveContinuationMinSavingsRatioEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, y, advancedColumnWidth, inlineLabelWidth, this.maxCopySearchDistanceEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, advancedColumnWidth, inlineLabelWidth, this.maxCandidateAxisOffsetsEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, y, advancedColumnWidth, inlineLabelWidth, this.perceptualVisibleColorDeltaEditBox);
            y += ROW_GAP;
            y = this.layoutLabeledFieldRow(contentX, y, advancedColumnWidth, inlineLabelWidth, this.perceptualAlphaDeltaEditBox);
            this.layoutLabeledFieldRow(contentX + advancedColumnWidth + COLUMN_GAP, y, advancedColumnWidth, inlineLabelWidth, this.perceptualAverageErrorEditBox);
        }

        int bottomY = this.getBottomButtonY();
        this.layoutWidget(this.exportButton, contentX, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.cancelJobButton, contentX + 128, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.closeButton, contentX + contentWidth - 120, bottomY, 120, FIELD_HEIGHT);
    }

    protected int layoutLabeledPathRow(int x, int y, int rowWidth, int labelWidth, @Nullable AbstractWidget field, @Nullable AbstractWidget primaryButton, int primaryButtonWidth, @Nullable AbstractWidget secondaryButton, int secondaryButtonWidth) {
        int buttonWidth = 0;
        if (primaryButton != null) buttonWidth += primaryButtonWidth + FIELD_BUTTON_GAP;
        if (secondaryButton != null) buttonWidth += secondaryButtonWidth + ((primaryButton != null) ? BUTTON_GROUP_GAP : 0);

        int fieldX = x + labelWidth + INLINE_LABEL_GAP;
        int fieldWidth = Math.max(MIN_INPUT_WIDTH, rowWidth - labelWidth - INLINE_LABEL_GAP - buttonWidth);
        this.layoutWidget(field, fieldX, y, fieldWidth, FIELD_HEIGHT);

        int buttonX = fieldX + fieldWidth + FIELD_BUTTON_GAP;
        if (primaryButton != null) {
            this.layoutWidget(primaryButton, buttonX, y, primaryButtonWidth, FIELD_HEIGHT);
            buttonX += primaryButtonWidth + BUTTON_GROUP_GAP;
        }
        if (secondaryButton != null) {
            this.layoutWidget(secondaryButton, buttonX, y, secondaryButtonWidth, FIELD_HEIGHT);
        }
        return y;
    }

    protected int layoutLabeledFieldRow(int x, int y, int rowWidth, int labelWidth, @Nullable AbstractWidget field) {
        int fieldX = x + labelWidth + INLINE_LABEL_GAP;
        int fieldWidth = Math.max(MIN_INPUT_WIDTH, rowWidth - labelWidth - INLINE_LABEL_GAP);
        this.layoutWidget(field, fieldX, y, fieldWidth, FIELD_HEIGHT);
        return y;
    }

    protected void layoutWidget(@Nullable AbstractWidget widget, int x, int y, int width, int height) {
        if (widget == null) return;
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
        widget.setHeight(height);
    }

    protected void setWidgetActive(@Nullable AbstractWidget widget, boolean active) {
        if (widget != null) {
            widget.active = active;
        }
    }

    protected @NotNull ExtendedEditBox addStyledEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = new ExtendedEditBox(this.font, 0, 0, 100, FIELD_HEIGHT, narrationMessage);
        editBox.setMaxLength(100000);
        UIBase.applyDefaultWidgetSkinTo(editBox, UIBase.shouldBlur());
        return this.addRenderableWidget(editBox);
    }

    protected @NotNull ExtendedEditBox addStyledNumberEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = this.addStyledEditBox(narrationMessage);
        editBox.setCharacterFilter(CharacterFilter.buildIntegerFilter());
        return editBox;
    }

    protected @NotNull ExtendedEditBox addStyledDecimalEditBox(@NotNull Component narrationMessage) {
        ExtendedEditBox editBox = this.addStyledEditBox(narrationMessage);
        editBox.setCharacterFilter(CharacterFilter.buildDecimalFiler());
        return editBox;
    }

    protected @NotNull ExtendedButton addStyledButton(@NotNull Component label, @NotNull Button.OnPress onPress) {
        ExtendedButton button = new ExtendedButton(0, 0, 100, FIELD_HEIGHT, label, onPress);
        UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
        return this.addRenderableWidget(button);
    }

    protected @NotNull CycleButton<CommonCycles.CycleEnabledDisabled> addToggleButton(@NotNull String key, boolean value, @NotNull java.util.function.Consumer<Boolean> setter) {
        CycleButton<CommonCycles.CycleEnabledDisabled> button = new CycleButton<>(0, 0, 120, FIELD_HEIGHT, CommonCycles.cycleEnabledDisabled(key, value), (cycleValue, cycleButton) -> setter.accept(cycleValue.getAsBoolean()));
        UIBase.applyDefaultWidgetSkinTo(button, UIBase.shouldBlur());
        this.addRenderableWidget(button);
        return button;
    }

    protected void configureTooltips() {
        this.setEditBoxTooltip(this.mainFramesPathEditBox, "fancymenu.afma.creator.main_frames.desc");
        this.setEditBoxTooltip(this.introFramesPathEditBox, "fancymenu.afma.creator.intro_frames.desc");
        this.setEditBoxTooltip(this.outputPathEditBox, "fancymenu.afma.creator.output_file.desc");
        this.setEditBoxTooltip(this.frameTimeEditBox, "fancymenu.afma.creator.frame_time.desc");
        this.setEditBoxTooltip(this.introFrameTimeEditBox, "fancymenu.afma.creator.intro_frame_time.desc");
        this.setEditBoxTooltip(this.loopCountEditBox, "fancymenu.afma.creator.loop_count.desc");
        this.setEditBoxTooltip(this.keyframeIntervalEditBox, "fancymenu.afma.creator.keyframe_interval.desc");
        this.setEditBoxTooltip(this.adaptiveMaxKeyframeIntervalEditBox, "fancymenu.afma.creator.adaptive_max_keyframe_interval.desc");
        this.setEditBoxTooltip(this.adaptiveContinuationMinSavingsBytesEditBox, "fancymenu.afma.creator.adaptive_continuation_min_savings_bytes.desc");
        this.setEditBoxTooltip(this.adaptiveContinuationMinSavingsRatioEditBox, "fancymenu.afma.creator.adaptive_continuation_min_savings_ratio.desc");
        this.setEditBoxTooltip(this.maxCopySearchDistanceEditBox, "fancymenu.afma.creator.max_copy_search_distance.desc");
        this.setEditBoxTooltip(this.maxCandidateAxisOffsetsEditBox, "fancymenu.afma.creator.max_candidate_axis_offsets.desc");
        this.setEditBoxTooltip(this.perceptualVisibleColorDeltaEditBox, "fancymenu.afma.creator.perceptual_visible_color_delta.desc");
        this.setEditBoxTooltip(this.perceptualAlphaDeltaEditBox, "fancymenu.afma.creator.perceptual_alpha_delta.desc");
        this.setEditBoxTooltip(this.perceptualAverageErrorEditBox, "fancymenu.afma.creator.perceptual_average_error.desc");

        this.setButtonTooltip(this.browseMainFramesButton, "fancymenu.afma.creator.browse_main_frames.desc");
        this.setButtonTooltip(this.browseIntroFramesButton, "fancymenu.afma.creator.browse_intro_frames.desc");
        this.setButtonTooltip(this.clearIntroFramesButton, "fancymenu.afma.creator.clear_intro_frames.desc");
        this.setButtonTooltip(this.browseOutputButton, "fancymenu.afma.creator.browse_output.desc");
        this.setButtonTooltip(this.exportButton, "fancymenu.afma.creator.export.desc");
        this.setButtonTooltip(this.cancelJobButton, "fancymenu.afma.creator.cancel.desc");
        this.setButtonTooltip(this.closeButton, "fancymenu.afma.creator.close.desc");

        this.setButtonTooltip(this.presetCycleButton, "fancymenu.afma.creator.optimization_preset.desc");
        this.setButtonTooltip(this.rectCopyCycleButton, "fancymenu.afma.creator.rect_copy.desc");
        this.setButtonTooltip(this.duplicateCycleButton, "fancymenu.afma.creator.duplicate_elision.desc");
        this.setButtonTooltip(this.nearLosslessCycleButton, "fancymenu.afma.creator.near_lossless.desc");
        this.setButtonTooltip(this.adaptiveKeyframeCycleButton, "fancymenu.afma.creator.adaptive_keyframes.desc");
    }

    protected void setEditBoxTooltip(@Nullable ExtendedEditBox editBox, @NotNull String localizationKey) {
        if (editBox != null) {
            editBox.setUITooltip(this.createTooltipSupplier(localizationKey));
        }
    }

    protected void setButtonTooltip(@Nullable ExtendedButton button, @NotNull String localizationKey) {
        if (button != null) {
            button.setUITooltip(this.createTooltip(localizationKey));
        }
    }

    protected @NotNull Supplier<UITooltip> createTooltipSupplier(@NotNull String localizationKey) {
        UITooltip tooltip = this.createTooltip(localizationKey);
        return () -> tooltip;
    }

    protected @NotNull UITooltip createTooltip(@NotNull String localizationKey) {
        return UITooltip.of(LocalizationUtils.splitLocalizedLines(localizationKey));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderFieldLabels(graphics);
        this.renderDiagnostics(graphics);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());
        RenderingUtils.resetShaderColor(graphics);
        this.renderCreatorPanels(graphics, mouseX, mouseY, partialTick);
    }

    protected void renderCreatorPanels(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float radius = UIBase.getInterfaceCornerRoundingRadius();
        int panelColor = UIBase.shouldBlur()
                ? UIBase.getUITheme().ui_blur_interface_area_background_color_type_1.getColorInt()
                : UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt();
        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                graphics,
                this.getPanelLeft(),
                this.getPanelTop(),
                this.getPanelRight() - this.getPanelLeft(),
                this.getPanelBottom() - this.getPanelTop(),
                radius,
                radius,
                radius,
                radius,
                panelColor,
                partialTick
        );

        UIBase.renderText(graphics, this.title, OUTER_PADDING, 16, this.getThemeLabelColor(false), UIBase.getUITextSizeNormal());
    }

    protected void renderFieldLabels(@NotNull GuiGraphics graphics) {
        int inlineLabelWidth = this.getInlineLabelWidth();

        this.drawInlineLabel(graphics, this.mainFramesPathEditBox, Component.translatable("fancymenu.afma.creator.main_frames"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.introFramesPathEditBox, Component.translatable("fancymenu.afma.creator.intro_frames"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.outputPathEditBox, Component.translatable("fancymenu.afma.creator.output_file"), inlineLabelWidth);

        if (this.frameTimeEditBox != null) {
            this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.playback"), this.getContentLeft(), this.frameTimeEditBox.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.frameTimeEditBox, Component.translatable("fancymenu.afma.creator.frame_time"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.introFrameTimeEditBox, Component.translatable("fancymenu.afma.creator.intro_frame_time"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.loopCountEditBox, Component.translatable("fancymenu.afma.creator.loop_count"), inlineLabelWidth);

        if (this.presetCycleButton != null) {
            this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.optimization"), this.getContentLeft(), this.presetCycleButton.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.keyframeIntervalEditBox, Component.translatable("fancymenu.afma.creator.keyframe_interval"), inlineLabelWidth);
        if (this.adaptiveKeyframeCycleButton != null) {
            this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.advanced"), this.getContentLeft(), this.adaptiveKeyframeCycleButton.getY() - 18, true);
        }
        this.drawInlineLabel(graphics, this.adaptiveMaxKeyframeIntervalEditBox, Component.translatable("fancymenu.afma.creator.adaptive_max_keyframe_interval"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.adaptiveContinuationMinSavingsBytesEditBox, Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_bytes"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.adaptiveContinuationMinSavingsRatioEditBox, Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_ratio"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.maxCopySearchDistanceEditBox, Component.translatable("fancymenu.afma.creator.max_copy_search_distance"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.maxCandidateAxisOffsetsEditBox, Component.translatable("fancymenu.afma.creator.max_candidate_axis_offsets"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualVisibleColorDeltaEditBox, Component.translatable("fancymenu.afma.creator.perceptual_visible_color_delta"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualAlphaDeltaEditBox, Component.translatable("fancymenu.afma.creator.perceptual_alpha_delta"), inlineLabelWidth);
        this.drawInlineLabel(graphics, this.perceptualAverageErrorEditBox, Component.translatable("fancymenu.afma.creator.perceptual_average_error"), inlineLabelWidth);
    }

    protected void drawFieldLabel(@NotNull GuiGraphics graphics, @NotNull Component component, int x, int y, boolean header) {
        UIBase.renderText(graphics, component, x, y, this.getThemeLabelColor(!header), UIBase.getUITextSizeNormal());
    }

    protected void drawInlineLabel(@NotNull GuiGraphics graphics, @Nullable AbstractWidget widget, @NotNull Component component, int labelWidth) {
        if (widget == null) return;
        float textWidth = UIBase.getUITextWidthNormal(component);
        float labelX = widget.getX() - INLINE_LABEL_GAP - labelWidth + Math.max(0.0F, labelWidth - textWidth);
        float labelY = widget.getY() + ((FIELD_HEIGHT - UIBase.getUITextHeightNormal()) / 2.0F);
        UIBase.renderText(graphics, component, labelX, labelY, this.getThemeLabelColor(true), UIBase.getUITextSizeNormal());
    }

    protected void renderDiagnostics(@NotNull GuiGraphics graphics) {
        AfmaEncodeJob job = this.state.getCurrentJob();
        if (job == null) return;

        int contentX = this.getContentLeft();
        int contentWidth = this.getContentWidth();
        int textY = this.getDiagnosticsStartY();
        AfmaEncodeProgress progress = job.getProgress();
        textY = this.renderWrappedUiText(graphics, Component.translatable("fancymenu.afma.creator.job_status", progress.task()), contentX, textY, contentWidth, this.getThemeLabelColor(false));
        if (progress.detail() != null && !progress.detail().isBlank()) {
            textY = this.renderWrappedUiText(graphics, Component.literal(progress.detail()), contentX, textY, contentWidth, this.getThemeLabelColor(true));
        }
        graphics.fill(contentX, textY, contentX + contentWidth, textY + 8, 0xFF202020);
        graphics.fill(contentX, textY, contentX + Math.round(contentWidth * (float) progress.progress()), textY + 8, UIBase.getUITheme().success_color.getColorInt());
    }

    protected int renderWrappedUiText(@NotNull GuiGraphics graphics, @NotNull Component text, int x, int y, int maxWidth, int color) {
        List<MutableComponent> lines = UIBase.lineWrapUIComponentsNormal(text, Math.max(20, maxWidth));
        int lineHeight = Math.max(10, Math.round(UIBase.getUITextHeightNormal()));
        for (MutableComponent line : lines) {
            UIBase.renderText(graphics, line, x, y, color, UIBase.getUITextSizeNormal());
            y += lineHeight + 2;
        }
        return y;
    }

    protected int getPanelLeft() {
        return OUTER_PADDING;
    }

    protected int getPanelTop() {
        return PANEL_TOP;
    }

    protected int getPanelRight() {
        return this.width - OUTER_PADDING;
    }

    protected int getPanelBottom() {
        return this.getBottomButtonY() - PANEL_PADDING;
    }

    protected int getBottomButtonY() {
        return this.height - OUTER_PADDING - FIELD_HEIGHT;
    }

    protected int getContentLeft() {
        return this.getPanelLeft() + PANEL_PADDING;
    }

    protected int getContentWidth() {
        return Math.max(0, (this.getPanelRight() - this.getPanelLeft()) - (PANEL_PADDING * 2));
    }

    protected int getContentStartY() {
        return this.getPanelTop() + PANEL_PADDING;
    }

    protected int getInlineLabelWidth() {
        float widestLabel = 0.0F;
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.main_frames")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.intro_frames")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.output_file")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.frame_time")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.intro_frame_time")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.loop_count")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.keyframe_interval")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.adaptive_max_keyframe_interval")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_bytes")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.adaptive_continuation_min_savings_ratio")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.max_copy_search_distance")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.max_candidate_axis_offsets")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.perceptual_visible_color_delta")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.perceptual_alpha_delta")));
        widestLabel = Math.max(widestLabel, UIBase.getUITextWidthNormal(Component.translatable("fancymenu.afma.creator.perceptual_average_error")));
        return Math.min(190, Math.max(118, Math.round(widestLabel) + 8));
    }

    protected boolean useSingleColumnNumericLayout(int columnWidth, int labelWidth) {
        return (columnWidth - labelWidth - INLINE_LABEL_GAP) < 150;
    }

    protected int getDiagnosticsStartY() {
        int contentStartY = this.getContentStartY();
        int widgetsBottom = Math.max(
                Math.max(
                        Math.max(this.getWidgetBottom(this.keyframeIntervalEditBox), this.getWidgetBottom(this.presetCycleButton)),
                        this.getWidgetBottom(this.adaptiveKeyframeCycleButton)
                ),
                Math.max(
                        Math.max(
                                Math.max(this.getWidgetBottom(this.rectCopyCycleButton), this.getWidgetBottom(this.duplicateCycleButton)),
                                this.getWidgetBottom(this.nearLosslessCycleButton)
                        ),
                        Math.max(
                                Math.max(this.getWidgetBottom(this.adaptiveMaxKeyframeIntervalEditBox), this.getWidgetBottom(this.adaptiveContinuationMinSavingsBytesEditBox)),
                                Math.max(
                                        Math.max(this.getWidgetBottom(this.adaptiveContinuationMinSavingsRatioEditBox), this.getWidgetBottom(this.maxCopySearchDistanceEditBox)),
                                        Math.max(
                                                Math.max(this.getWidgetBottom(this.maxCandidateAxisOffsetsEditBox), this.getWidgetBottom(this.perceptualVisibleColorDeltaEditBox)),
                                                Math.max(this.getWidgetBottom(this.perceptualAlphaDeltaEditBox), this.getWidgetBottom(this.perceptualAverageErrorEditBox))
                                        )
                                )
                        )
                )
        );
        return Math.max(contentStartY, widgetsBottom + 18);
    }

    protected int getWidgetBottom(@Nullable AbstractWidget widget) {
        return (widget != null) ? (widget.getY() + widget.getHeight()) : 0;
    }

    protected int getThemeLabelColor(boolean inactive) {
        UITheme theme = UIBase.getUITheme();
        if (UIBase.shouldBlur()) {
            return inactive
                    ? theme.ui_blur_interface_widget_label_color_inactive.getColorInt()
                    : theme.ui_blur_interface_widget_label_color_normal.getColorInt();
        }
        return inactive
                ? theme.ui_interface_widget_label_color_inactive.getColorInt()
                : theme.ui_interface_widget_label_color_normal.getColorInt();
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        if (paths.isEmpty()) return;

        List<File> droppedPngFiles = new ArrayList<>();
        for (Path path : paths) {
            File file = path.toFile();
            if (file.isDirectory()) {
                if (this.state.getMainFramesDirectory() == null) {
                    this.state.setMainFramesDirectory(file);
                } else if (this.state.getIntroFramesDirectory() == null) {
                    this.state.setIntroFramesDirectory(file);
                }
            } else if (file.isFile()) {
                String lowerName = file.getName().toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".afma")) {
                    this.state.setOutputFile(file);
                } else if (lowerName.endsWith(".png")) {
                    droppedPngFiles.add(file);
                }
            }
        }

        if (!droppedPngFiles.isEmpty()) {
            FilenameComparator comparator = new FilenameComparator();
            droppedPngFiles.sort((first, second) -> comparator.compare(first.getName(), second.getName()));
            if (this.state.getMainFramesDirectory() == null && this.state.getMainFramesInputText().isBlank()) {
                this.state.setMainFramesList(droppedPngFiles);
            } else if (this.state.getIntroFramesDirectory() == null && this.state.getIntroFramesInputText().isBlank()) {
                this.state.setIntroFramesList(droppedPngFiles);
            }
        }
        this.syncWidgetsFromState();
    }

    @Override
    public void onClose() {
        this.state.cancelCurrentJob();
        Minecraft.getInstance().setScreen(this.parentScreen);
    }

    @Override
    public void removed() {
        this.state.cancelCurrentJob();
        this.state.close();
        for (PiPWindow window : List.copyOf(this.childWindows)) {
            if (window != null) {
                try {
                    window.close();
                } catch (Exception ignored) {
                }
            }
        }
        this.childWindows.clear();
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    protected static @Nullable File pathToDirectory(@Nullable String value) {
        if ((value == null) || value.isBlank()) return null;
        return new File(value.replace("\\", "/"));
    }

    protected static @Nullable File pathToFile(@Nullable String value) {
        if ((value == null) || value.isBlank()) return null;
        return new File(value.replace("\\", "/"));
    }

    protected static @NotNull String fileToPath(@Nullable File file) {
        return (file != null) ? file.getPath().replace("\\", "/") : "";
    }

    protected static boolean isInteger(@Nullable String value) {
        if ((value == null) || value.isBlank()) return false;
        try {
            Long.parseLong(value.trim());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    protected static long parseLongOrDefault(@Nullable String value, long fallback) {
        if (!isInteger(value)) return fallback;
        return Long.parseLong(value.trim());
    }

    protected static double parseDoubleOrDefault(@Nullable String value, double fallback) {
        if ((value == null) || value.isBlank()) return fallback;
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

}
