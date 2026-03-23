package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.file.GameDirectoryUtils;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.rendering.ui.pipwindow.PiPWindow;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.ChooseDirectoryWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.screen.filebrowser.SaveFileWindowBody;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AfmaCreatorScreen extends Screen {

    private static final int OUTER_PADDING = 20;
    private static final int PANEL_GAP = 16;
    private static final int FIELD_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int SECTION_GAP = 34;
    private static final int PREVIEW_CONTROLS_HEIGHT = 24;

    private final @NotNull Screen parentScreen;
    private final @NotNull AfmaCreatorState state = new AfmaCreatorState();
    private final @NotNull AfmaPreviewController previewController = new AfmaPreviewController();
    private final @NotNull AfmaFfmpegBridge ffmpegBridge = new AfmaFfmpegBridge();
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
    private @Nullable ExtendedEditBox customFrameTimesEditBox;
    private @Nullable ExtendedEditBox customIntroFrameTimesEditBox;

    private @Nullable ExtendedButton browseMainFramesButton;
    private @Nullable ExtendedButton browseIntroFramesButton;
    private @Nullable ExtendedButton clearIntroFramesButton;
    private @Nullable ExtendedButton browseOutputButton;
    private @Nullable ExtendedButton analyzeButton;
    private @Nullable ExtendedButton exportButton;
    private @Nullable ExtendedButton cancelJobButton;
    private @Nullable ExtendedButton closeButton;
    private @Nullable ExtendedButton previewPlayPauseButton;
    private @Nullable ExtendedButton previewPreviousButton;
    private @Nullable ExtendedButton previewNextButton;

    private @Nullable CycleButton<AfmaOptimizationPreset> presetCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> rectCopyCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> duplicateCycleButton;
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> thumbnailCycleButton;

    private boolean timelineDragging = false;

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

        this.customFrameTimesEditBox = this.addStyledEditBox(Component.translatable("fancymenu.afma.creator.custom_frame_times"));
        this.customFrameTimesEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setCustomFrameTimesText(value);
        });

        this.customIntroFrameTimesEditBox = this.addStyledEditBox(Component.translatable("fancymenu.afma.creator.custom_intro_frame_times"));
        this.customIntroFrameTimesEditBox.setResponder(value -> {
            if (this.syncingWidgets) return;
            this.state.setCustomIntroFrameTimesText(value);
        });

        this.browseMainFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getMainFramesDirectory(), this.state::setMainFramesDirectory));
        this.browseIntroFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openDirectoryChooser(this.state.getIntroFramesDirectory(), this.state::setIntroFramesDirectory));
        this.clearIntroFramesButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.clear"), button -> {
            this.state.clearIntroFramesDirectory();
            this.syncWidgetsFromState();
        });
        this.browseOutputButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.browse"), button -> this.openOutputChooser(this.state.getOutputFile()));

        this.presetCycleButton = new CycleButton<>(0, 0, 200, FIELD_HEIGHT,
                LocalizedEnumValueCycle.ofArray("fancymenu.afma.creator.optimization_preset.label", AfmaOptimizationPreset.values()).setCurrentValue(this.state.getOptimizationPreset(), false),
                (value, button) -> {
                    this.state.applyPreset(value);
                    this.syncWidgetsFromState();
                });
        UIBase.applyDefaultWidgetSkinTo(this.presetCycleButton, UIBase.shouldBlur());
        this.addRenderableWidget(this.presetCycleButton);

        this.rectCopyCycleButton = this.addToggleButton("fancymenu.afma.creator.rect_copy", this.state.isRectCopyEnabled(), value -> this.state.setRectCopyEnabled(value));
        this.duplicateCycleButton = this.addToggleButton("fancymenu.afma.creator.duplicate_elision", this.state.isDuplicateFrameElision(), value -> this.state.setDuplicateFrameElision(value));
        this.thumbnailCycleButton = this.addToggleButton("fancymenu.afma.creator.thumbnail", this.state.isGenerateThumbnail(), value -> this.state.setGenerateThumbnail(value));

        this.analyzeButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.analyze"), button -> this.startAnalysis());
        this.exportButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.export"), button -> this.startExport());
        this.cancelJobButton = this.addStyledButton(Component.translatable("fancymenu.common_components.cancel"), button -> this.state.cancelCurrentJob());
        this.closeButton = this.addStyledButton(Component.translatable("fancymenu.common.close"), button -> this.onClose());

        this.previewPlayPauseButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.preview.play"), button -> this.previewController.togglePlaying());
        this.previewPreviousButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.preview.prev"), button -> this.previewController.stepPrevious());
        this.previewNextButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.preview.next"), button -> this.previewController.stepNext());

        this.syncWidgetsFromState();
        this.repositionWidgets();
    }

    @Override
    public void tick() {
        super.tick();

        this.previewController.tick();
        this.syncPreviewFromAnalysisIfNeeded();
        this.updateButtonStates();
        this.repositionWidgets();
    }

    protected void syncPreviewFromAnalysisIfNeeded() {
        AfmaCreatorAnalysisResult result = this.state.getAnalysisResult();
        if (result != null && result != this.previewController.getAnalysisResult()) {
            this.previewController.setAnalysisResult(result);
        } else if ((result == null) && (this.previewController.getAnalysisResult() != null)) {
            this.previewController.setAnalysisResult(null);
        }
        AfmaEncodeJob job = this.state.getCurrentJob();
        if ((job != null) && !job.isRunning() && (job != this.handledTerminalJob)) {
            this.handledTerminalJob = job;
            if ((job.getStatus() == AfmaEncodeJob.Status.FAILED) && (job.getFailure() != null)) {
                Dialogs.openMessage(Component.literal(job.getFailure().getMessage() != null ? job.getFailure().getMessage() : "AFMA creator job failed."), MessageDialogStyle.ERROR);
            } else if ((job.getStatus() == AfmaEncodeJob.Status.SUCCEEDED) && (job.getKind() == AfmaEncodeJob.Kind.EXPORT) && (job.getOutputFile() != null)) {
                Dialogs.openMessage(Component.translatable("fancymenu.afma.creator.export.success", fileToPath(job.getOutputFile())), MessageDialogStyle.INFO);
            }
        }
    }

    protected void startAnalysis() {
        try {
            this.syncStateFromWidgets();
            this.state.startAnalysis();
        } catch (Exception ex) {
            Dialogs.openMessage(Component.literal(ex.getMessage() != null ? ex.getMessage() : "AFMA analysis failed to start."), MessageDialogStyle.ERROR);
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
        boolean hasAnalysis = this.state.getAnalysisResult() != null;
        boolean analysisFresh = hasAnalysis && !this.state.isAnalysisDirty();
        if (this.analyzeButton != null) this.analyzeButton.active = !jobRunning;
        if (this.exportButton != null) this.exportButton.active = !jobRunning;
        if (this.cancelJobButton != null) {
            this.cancelJobButton.active = jobRunning;
            this.cancelJobButton.visible = jobRunning;
        }
        if (this.previewPlayPauseButton != null) {
            this.previewPlayPauseButton.active = analysisFresh;
            this.previewPlayPauseButton.setMessage(Component.translatable(this.previewController.isPlaying() ? "fancymenu.afma.creator.preview.pause" : "fancymenu.afma.creator.preview.play"));
        }
        if (this.previewPreviousButton != null) this.previewPreviousButton.active = analysisFresh;
        if (this.previewNextButton != null) this.previewNextButton.active = analysisFresh;
        if (this.clearIntroFramesButton != null) this.clearIntroFramesButton.active = this.state.getIntroFramesDirectory() != null && !jobRunning;
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
            if (this.mainFramesPathEditBox != null) this.mainFramesPathEditBox.setValue(fileToPath(this.state.getMainFramesDirectory()));
            if (this.introFramesPathEditBox != null) this.introFramesPathEditBox.setValue(fileToPath(this.state.getIntroFramesDirectory()));
            if (this.outputPathEditBox != null) this.outputPathEditBox.setValue(fileToPath(this.state.getOutputFile()));
            if (this.frameTimeEditBox != null) this.frameTimeEditBox.setValue(String.valueOf(this.state.getFrameTimeMs()));
            if (this.introFrameTimeEditBox != null) this.introFrameTimeEditBox.setValue(String.valueOf(this.state.getIntroFrameTimeMs()));
            if (this.loopCountEditBox != null) this.loopCountEditBox.setValue(String.valueOf(this.state.getLoopCount()));
            if (this.keyframeIntervalEditBox != null) this.keyframeIntervalEditBox.setValue(String.valueOf(this.state.getKeyframeInterval()));
            if (this.customFrameTimesEditBox != null) this.customFrameTimesEditBox.setValue(this.state.getCustomFrameTimesText());
            if (this.customIntroFrameTimesEditBox != null) this.customIntroFrameTimesEditBox.setValue(this.state.getCustomIntroFrameTimesText());
            if (this.presetCycleButton != null) this.presetCycleButton.setSelectedValue(this.state.getOptimizationPreset(), false);
            if (this.rectCopyCycleButton != null) this.rectCopyCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isRectCopyEnabled()), false);
            if (this.duplicateCycleButton != null) this.duplicateCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isDuplicateFrameElision()), false);
            if (this.thumbnailCycleButton != null) this.thumbnailCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isGenerateThumbnail()), false);
        } finally {
            this.syncingWidgets = false;
        }
    }

    protected void syncStateFromWidgets() {
        if (this.syncingWidgets) return;
        if (this.mainFramesPathEditBox != null && !fileToPath(this.state.getMainFramesDirectory()).equals(this.mainFramesPathEditBox.getValue())) {
            this.state.setMainFramesDirectory(pathToDirectory(this.mainFramesPathEditBox.getValue()));
        }
        if (this.introFramesPathEditBox != null && !fileToPath(this.state.getIntroFramesDirectory()).equals(this.introFramesPathEditBox.getValue())) {
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
        if (this.customFrameTimesEditBox != null && !this.state.getCustomFrameTimesText().equals(this.customFrameTimesEditBox.getValue())) {
            this.state.setCustomFrameTimesText(this.customFrameTimesEditBox.getValue());
        }
        if (this.customIntroFrameTimesEditBox != null && !this.state.getCustomIntroFrameTimesText().equals(this.customIntroFrameTimesEditBox.getValue())) {
            this.state.setCustomIntroFrameTimesText(this.customIntroFrameTimesEditBox.getValue());
        }
    }

    protected void repositionWidgets() {
        int leftX = OUTER_PADDING;
        int rightPanelX = (this.width / 2) + 8;
        int leftWidth = Math.max(320, (this.width / 2) - OUTER_PADDING - 12);
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        int browseWidth = 84;
        int clearWidth = 64;
        int pathFieldWidth = leftWidth - browseWidth - 8;
        int introPathFieldWidth = leftWidth - browseWidth - clearWidth - 12;

        int y = 54;
        y = this.layoutPathRow(leftX, y, pathFieldWidth, browseWidth, this.mainFramesPathEditBox, this.browseMainFramesButton);
        y += 4;
        if (this.introFramesPathEditBox != null) {
            this.introFramesPathEditBox.setX(leftX);
            this.introFramesPathEditBox.setY(y);
            this.introFramesPathEditBox.setWidth(introPathFieldWidth);
            this.introFramesPathEditBox.setHeight(FIELD_HEIGHT);
        }
        if (this.browseIntroFramesButton != null) {
            this.browseIntroFramesButton.setX(leftX + introPathFieldWidth + 4);
            this.browseIntroFramesButton.setY(y);
            this.browseIntroFramesButton.setWidth(browseWidth);
        }
        if (this.clearIntroFramesButton != null) {
            this.clearIntroFramesButton.setX(leftX + introPathFieldWidth + browseWidth + 8);
            this.clearIntroFramesButton.setY(y);
            this.clearIntroFramesButton.setWidth(clearWidth);
        }
        y += SECTION_GAP;

        y = this.layoutPathRow(leftX, y, pathFieldWidth, browseWidth, this.outputPathEditBox, this.browseOutputButton);
        y += SECTION_GAP;

        int halfWidth = (leftWidth - 8) / 2;
        this.layoutWidget(this.frameTimeEditBox, leftX, y, halfWidth, FIELD_HEIGHT);
        this.layoutWidget(this.introFrameTimeEditBox, leftX + halfWidth + 8, y, halfWidth, FIELD_HEIGHT);
        y += ROW_GAP + 10;
        this.layoutWidget(this.loopCountEditBox, leftX, y, halfWidth, FIELD_HEIGHT);
        this.layoutWidget(this.keyframeIntervalEditBox, leftX + halfWidth + 8, y, halfWidth, FIELD_HEIGHT);
        y += ROW_GAP + 10;

        this.layoutWidget(this.customFrameTimesEditBox, leftX, y, leftWidth, FIELD_HEIGHT);
        y += ROW_GAP + 10;
        this.layoutWidget(this.customIntroFrameTimesEditBox, leftX, y, leftWidth, FIELD_HEIGHT);
        y += SECTION_GAP;

        this.layoutWidget(this.presetCycleButton, leftX, y, leftWidth, FIELD_HEIGHT);
        y += ROW_GAP + 10;
        this.layoutWidget(this.rectCopyCycleButton, leftX, y, halfWidth, FIELD_HEIGHT);
        this.layoutWidget(this.duplicateCycleButton, leftX + halfWidth + 8, y, halfWidth, FIELD_HEIGHT);
        y += ROW_GAP + 10;
        this.layoutWidget(this.thumbnailCycleButton, leftX, y, leftWidth, FIELD_HEIGHT);

        int bottomY = this.height - OUTER_PADDING - FIELD_HEIGHT;
        this.layoutWidget(this.analyzeButton, leftX, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.exportButton, leftX + 128, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.cancelJobButton, leftX + 256, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.closeButton, leftX + Math.max(384, leftWidth - 120), bottomY, 120, FIELD_HEIGHT);

        int previewControlsY = this.height - OUTER_PADDING - FIELD_HEIGHT - 110;
        this.layoutWidget(this.previewPreviousButton, rightPanelX, previewControlsY, 90, FIELD_HEIGHT);
        this.layoutWidget(this.previewPlayPauseButton, rightPanelX + 98, previewControlsY, 110, FIELD_HEIGHT);
        this.layoutWidget(this.previewNextButton, rightPanelX + 216, previewControlsY, 90, FIELD_HEIGHT);
    }

    protected int layoutPathRow(int x, int y, int fieldWidth, int buttonWidth, @Nullable AbstractWidget field, @Nullable AbstractWidget button) {
        this.layoutWidget(field, x, y, fieldWidth, FIELD_HEIGHT);
        this.layoutWidget(button, x + fieldWidth + 8, y, buttonWidth, FIELD_HEIGHT);
        return y;
    }

    protected void layoutWidget(@Nullable AbstractWidget widget, int x, int y, int width, int height) {
        if (widget == null) return;
        widget.setX(x);
        widget.setY(y);
        widget.setWidth(width);
        widget.setHeight(height);
    }

    protected @NotNull ExtendedEditBox addStyledEditBox(@NotNull Component hint) {
        ExtendedEditBox editBox = new ExtendedEditBox(this.font, 0, 0, 100, FIELD_HEIGHT, Component.empty());
        editBox.setHintFancyMenu(consumes -> hint);
        editBox.setMaxLength(100000);
        UIBase.applyDefaultWidgetSkinTo(editBox, UIBase.shouldBlur());
        return this.addRenderableWidget(editBox);
    }

    protected @NotNull ExtendedEditBox addStyledNumberEditBox(@NotNull Component hint) {
        ExtendedEditBox editBox = this.addStyledEditBox(hint);
        editBox.setCharacterFilter(CharacterFilter.buildIntegerFilter());
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
        int leftX = OUTER_PADDING;
        int rightPanelX = (this.width / 2) + 8;
        int leftWidth = Math.max(320, (this.width / 2) - OUTER_PADDING - 12);
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        int panelTop = 40;
        int panelBottom = this.height - OUTER_PADDING - 40;

        graphics.fill(leftX - 8, panelTop, leftX + leftWidth + 8, panelBottom, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());
        graphics.fill(rightPanelX - 8, panelTop, rightPanelX + rightWidth + 8, panelBottom, UIBase.getUITheme().ui_interface_area_background_color_type_1.getColorInt());

        graphics.drawString(this.font, this.title, OUTER_PADDING, 16, 0xFFFFFF, false);

        int previewX = rightPanelX;
        int previewY = 58;
        int previewWidth = rightWidth;
        int previewHeight = Math.max(180, this.height - previewY - 220);
        graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, UIBase.getUITheme().ui_interface_background_color.getColorInt());

        ResourceLocation previewTexture = this.previewController.getTextureLocation();
        if (previewTexture != null) {
            int canvasWidth = Math.max(1, this.previewController.getCanvasWidth());
            int canvasHeight = Math.max(1, this.previewController.getCanvasHeight());
            float scale = Math.min((float) previewWidth / canvasWidth, (float) previewHeight / canvasHeight);
            int renderWidth = Math.max(1, Math.round(canvasWidth * scale));
            int renderHeight = Math.max(1, Math.round(canvasHeight * scale));
            int renderX = previewX + ((previewWidth - renderWidth) / 2);
            int renderY = previewY + ((previewHeight - renderHeight) / 2);
            graphics.blit(previewTexture, renderX, renderY, 0.0F, 0.0F, renderWidth, renderHeight, renderWidth, renderHeight);
        } else {
            graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.preview.empty"), previewX + 8, previewY + 8, 0xA0A0A0, false);
        }

        int timelineY = previewY + previewHeight + 14;
        int timelineX = previewX;
        int timelineWidth = previewWidth;
        graphics.fill(timelineX, timelineY, timelineX + timelineWidth, timelineY + 6, 0xFF202020);
        if (this.previewController.getTimelineSize() > 0 && this.previewController.getCurrentTimelineIndex() >= 0) {
            float progress = this.previewController.getTimelineSize() <= 1 ? 0.0F : (float) this.previewController.getCurrentTimelineIndex() / (float) (this.previewController.getTimelineSize() - 1);
            int fillWidth = Math.max(4, Math.round(progress * timelineWidth));
            graphics.fill(timelineX, timelineY, timelineX + fillWidth, timelineY + 6, UIBase.getUITheme().warning_color.getColorInt());
        }
    }

    protected void renderFieldLabels(@NotNull GuiGraphics graphics) {
        int leftX = OUTER_PADDING;
        int leftWidth = Math.max(320, (this.width / 2) - OUTER_PADDING - 12);
        int browseWidth = 84;
        int clearWidth = 64;
        int pathFieldWidth = leftWidth - browseWidth - 8;
        int introPathFieldWidth = leftWidth - browseWidth - clearWidth - 12;
        int halfWidth = (leftWidth - 8) / 2;

        int y = 42;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.source"), leftX, y - 14, true);
        y = 54;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.main_frames"), leftX, y - 12, false);
        y += 28;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.intro_frames"), leftX, y - 12, false);
        y += SECTION_GAP;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.output_file"), leftX, y - 12, false);
        y += SECTION_GAP;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.playback"), leftX, y - 14, true);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.frame_time"), leftX, y, false);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.intro_frame_time"), leftX + halfWidth + 8, y, false);
        y += ROW_GAP + 10;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.loop_count"), leftX, y, false);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.keyframe_interval"), leftX + halfWidth + 8, y, false);
        y += ROW_GAP + 10;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.custom_frame_times"), leftX, y, false);
        y += ROW_GAP + 10;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.custom_intro_frame_times"), leftX, y, false);
        y += SECTION_GAP;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.optimization"), leftX, y - 14, true);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.optimization_preset.title"), leftX, y, false);

        int rightX = (this.width / 2) + 8;
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.preview"), rightX, 42, true);
    }

    protected void drawFieldLabel(@NotNull GuiGraphics graphics, @NotNull Component component, int x, int y, boolean header) {
        graphics.drawString(this.font, component, x, y, header ? 0xFFFFFF : 0xD0D0D0, false);
    }

    protected void renderDiagnostics(@NotNull GuiGraphics graphics) {
        int rightPanelX = (this.width / 2) + 8;
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        int textY = this.height - OUTER_PADDING - 180;

        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.ffmpeg_status", this.ffmpegBridge.describeStatus()), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.ffmpeg_binary", this.ffmpegBridge.describeBinaryPath()), rightPanelX, textY, 0xA0A0A0, false);
        textY += 18;

        AfmaEncodeJob job = this.state.getCurrentJob();
        if (job != null) {
            AfmaEncodeProgress progress = job.getProgress();
            graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.job_status", progress.task()), rightPanelX, textY, 0xFFFFFF, false);
            textY += 12;
            if (progress.detail() != null && !progress.detail().isBlank()) {
                graphics.drawString(this.font, Component.literal(progress.detail()), rightPanelX, textY, 0xD0D0D0, false);
                textY += 12;
            }
            int barWidth = rightWidth;
            graphics.fill(rightPanelX, textY, rightPanelX + barWidth, textY + 8, 0xFF202020);
            graphics.fill(rightPanelX, textY, rightPanelX + Math.round(barWidth * (float) progress.progress()), textY + 8, UIBase.getUITheme().success_color.getColorInt());
            textY += 18;
        }

        AfmaCreatorAnalysisResult result = this.state.getAnalysisResult();
        if (result == null) {
            graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.analysis.pending"), rightPanelX, textY, 0xA0A0A0, false);
            return;
        }

        if (this.state.isAnalysisDirty()) {
            graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.analysis.stale").withStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())), rightPanelX, textY, UIBase.getUITheme().warning_color.getColorInt(), false);
            textY += 12;
        }

        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.summary.canvas", result.plan().getMetadata().getCanvasWidth(), result.plan().getMetadata().getCanvasHeight()), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.summary.frames", result.summary().mainFrameCount(), result.summary().introFrameCount()), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.summary.ops", result.summary().fullFrames(), result.summary().deltaRectFrames(), result.summary().sameFrames(), result.summary().copyRectPatchFrames()), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.summary.estimated_size", humanReadableBytes(result.estimatedArchiveBytes())), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.summary.alpha", result.alphaUsed() ? Component.translatable("fancymenu.afma.creator.summary.alpha.yes") : Component.translatable("fancymenu.afma.creator.summary.alpha.no")), rightPanelX, textY, 0xFFFFFF, false);
        textY += 12;
        graphics.drawString(this.font, Component.translatable("fancymenu.afma.creator.preview.current_frame", this.previewController.getCurrentFrameLabel(), this.previewController.getCurrentFrameDurationMs()), rightPanelX, textY, 0xFFFFFF, false);
        textY += 18;

        for (String warning : result.warnings()) {
            graphics.drawString(this.font, Component.literal("- " + warning), rightPanelX, textY, UIBase.getUITheme().warning_color.getColorInt(), false);
            textY += 12;
            if (textY > (this.height - OUTER_PADDING - 12)) {
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if ((button == 0) && this.isOverTimeline(mouseX, mouseY)) {
            this.timelineDragging = true;
            this.seekPreviewFromTimeline(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.timelineDragging && button == 0) {
            this.seekPreviewFromTimeline(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.timelineDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean isOverTimeline(double mouseX, double mouseY) {
        if (this.state.isAnalysisDirty() || (this.state.getAnalysisResult() == null)) {
            return false;
        }
        int rightPanelX = (this.width / 2) + 8;
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        int previewY = 58;
        int previewHeight = Math.max(180, this.height - previewY - 220);
        int timelineY = previewY + previewHeight + 14;
        return mouseX >= rightPanelX && mouseX <= (rightPanelX + rightWidth) && mouseY >= timelineY && mouseY <= (timelineY + 10);
    }

    protected void seekPreviewFromTimeline(double mouseX) {
        int rightPanelX = (this.width / 2) + 8;
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        double progress = (mouseX - rightPanelX) / Math.max(1, rightWidth);
        this.previewController.seekToProgress(progress);
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        if (paths.isEmpty()) return;

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
                    if (this.state.getMainFramesDirectory() == null) {
                        this.state.setMainFramesDirectory(file.getParentFile());
                    } else if (this.state.getIntroFramesDirectory() == null) {
                        this.state.setIntroFramesDirectory(file.getParentFile());
                    }
                }
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
        this.previewController.close();
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

    protected static @NotNull String humanReadableBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        double value = bytes / 1024.0D;
        if (value < 1024.0D) return String.format(Locale.ROOT, "%.1f KiB", value);
        value /= 1024.0D;
        if (value < 1024.0D) return String.format(Locale.ROOT, "%.1f MiB", value);
        value /= 1024.0D;
        return String.format(Locale.ROOT, "%.2f GiB", value);
    }

}
