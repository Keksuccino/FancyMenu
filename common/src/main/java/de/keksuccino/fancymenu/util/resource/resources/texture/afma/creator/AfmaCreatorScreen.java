package de.keksuccino.fancymenu.util.resource.resources.texture.afma.creator;

import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedEnumValueCycle;
import de.keksuccino.fancymenu.util.file.FilenameComparator;
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
import net.minecraft.network.chat.MutableComponent;
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
    private static final int SECTION_GAP = 46;
    private static final int SOURCE_MAIN_ROW_Y = 54;
    private static final int SOURCE_ROW_GAP = 28;

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
    private @Nullable ExtendedEditBox customFrameTimesEditBox;
    private @Nullable ExtendedEditBox customIntroFrameTimesEditBox;

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
    private @Nullable CycleButton<CommonCycles.CycleEnabledDisabled> thumbnailCycleButton;

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
        this.thumbnailCycleButton = this.addToggleButton("fancymenu.afma.creator.thumbnail", this.state.isGenerateThumbnail(), value -> this.state.setGenerateThumbnail(value));

        this.exportButton = this.addStyledButton(Component.translatable("fancymenu.afma.creator.export"), button -> this.startExport());
        this.cancelJobButton = this.addStyledButton(Component.translatable("fancymenu.common_components.cancel"), button -> this.state.cancelCurrentJob());
        this.closeButton = this.addStyledButton(Component.translatable("fancymenu.common.close"), button -> this.onClose());

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
            } else if ((job.getStatus() == AfmaEncodeJob.Status.SUCCEEDED) && (job.getKind() == AfmaEncodeJob.Kind.EXPORT) && (job.getOutputFile() != null)) {
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
            if (this.customFrameTimesEditBox != null) this.customFrameTimesEditBox.setValue(this.state.getCustomFrameTimesText());
            if (this.customIntroFrameTimesEditBox != null) this.customIntroFrameTimesEditBox.setValue(this.state.getCustomIntroFrameTimesText());
            if (this.presetCycleButton != null) this.presetCycleButton.setSelectedValue(this.state.getOptimizationPreset(), false);
            if (this.rectCopyCycleButton != null) this.rectCopyCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isRectCopyEnabled()), false);
            if (this.duplicateCycleButton != null) this.duplicateCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isDuplicateFrameElision()), false);
            if (this.nearLosslessCycleButton != null) this.nearLosslessCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isNearLosslessEnabled()), false);
            if (this.thumbnailCycleButton != null) this.thumbnailCycleButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(this.state.isGenerateThumbnail()), false);
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
        int browseWidth = 84;
        int clearWidth = 64;
        int pathFieldWidth = leftWidth - browseWidth - 8;
        int introPathFieldWidth = leftWidth - browseWidth - clearWidth - 12;

        int y = SOURCE_MAIN_ROW_Y;
        y = this.layoutPathRow(leftX, y, pathFieldWidth, browseWidth, this.mainFramesPathEditBox, this.browseMainFramesButton);
        y += SOURCE_ROW_GAP;
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
        this.layoutWidget(this.nearLosslessCycleButton, leftX, y, halfWidth, FIELD_HEIGHT);
        this.layoutWidget(this.thumbnailCycleButton, leftX + halfWidth + 8, y, halfWidth, FIELD_HEIGHT);

        int bottomY = this.height - OUTER_PADDING - FIELD_HEIGHT;
        this.layoutWidget(this.exportButton, leftX, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.cancelJobButton, leftX + 128, bottomY, 120, FIELD_HEIGHT);
        this.layoutWidget(this.closeButton, this.width - OUTER_PADDING - 120, bottomY, 120, FIELD_HEIGHT);
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

        UIBase.renderText(graphics, this.title, OUTER_PADDING, 16, 0xFFFFFFFF, UIBase.getUITextSizeNormal());
    }

    protected void renderFieldLabels(@NotNull GuiGraphics graphics) {
        int leftX = OUTER_PADDING;
        int sourceHeaderY = 28;
        int introRowY = SOURCE_MAIN_ROW_Y + SOURCE_ROW_GAP;
        int outputRowY = introRowY + SECTION_GAP;
        int playbackHeaderY = outputRowY + FIELD_HEIGHT + 8;
        int playbackRowY = outputRowY + SECTION_GAP;
        int secondPlaybackRowY = playbackRowY + ROW_GAP + 10;
        int customMainRowY = secondPlaybackRowY + ROW_GAP + 10;
        int customIntroRowY = customMainRowY + ROW_GAP + 10;
        int optimizationHeaderY = customIntroRowY + FIELD_HEIGHT + 8;

        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.source"), leftX, sourceHeaderY, true);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.playback"), leftX, playbackHeaderY, true);
        this.drawFieldLabel(graphics, Component.translatable("fancymenu.afma.creator.section.optimization"), leftX, optimizationHeaderY, true);
    }

    protected void drawFieldLabel(@NotNull GuiGraphics graphics, @NotNull Component component, int x, int y, boolean header) {
        UIBase.renderText(graphics, component, x, y, header ? 0xFFFFFFFF : 0xFFD0D0D0, UIBase.getUITextSizeNormal());
    }

    protected void renderDiagnostics(@NotNull GuiGraphics graphics) {
        int rightPanelX = (this.width / 2) + 8;
        int rightWidth = this.width - rightPanelX - OUTER_PADDING;
        int textY = 58;
        int normalTextColor = 0xFFFFFFFF;

        AfmaEncodeJob job = this.state.getCurrentJob();
        if (job == null) {
            return;
        }

        AfmaEncodeProgress progress = job.getProgress();
        textY = this.renderWrappedUiText(graphics, Component.translatable("fancymenu.afma.creator.job_status", progress.task()), rightPanelX, textY, rightWidth, normalTextColor);
        if (progress.detail() != null && !progress.detail().isBlank()) {
            textY = this.renderWrappedUiText(graphics, Component.literal(progress.detail()), rightPanelX, textY, rightWidth, 0xFFD0D0D0);
        }
        int barWidth = rightWidth;
        graphics.fill(rightPanelX, textY, rightPanelX + barWidth, textY + 8, 0xFF202020);
        graphics.fill(rightPanelX, textY, rightPanelX + Math.round(barWidth * (float) progress.progress()), textY + 8, UIBase.getUITheme().success_color.getColorInt());
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

}
