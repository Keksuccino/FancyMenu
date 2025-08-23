package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.CharacterFilter;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

import static de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen.ELEMENT_DRAG_CRUMPLE_ZONE;

public class KeyframeManagerScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger();

    protected static final int KEY_MOVE_KEYFRAME_LEFT = InputConstants.KEY_LEFT;
    protected static final int KEY_MOVE_KEYFRAME_RIGHT = InputConstants.KEY_RIGHT;
    protected static final int KEY_DELETE_KEYFRAME = InputConstants.KEY_DELETE;
    protected static final int KEY_ADD_KEYFRAME = InputConstants.KEY_K;
    protected static final int KEY_TOGGLE_RECORDING = InputConstants.KEY_R;
    protected static final int KEY_TOGGLE_PAUSE_RECORDING = InputConstants.KEY_T;
    protected static final int KEY_TOGGLE_PLAYING = InputConstants.KEY_P;

    protected static final DrawableColor TIMELINE_COLOR = DrawableColor.of(new Color(0, 122, 204));
    protected static final DrawableColor TIMELINE_PADDING_COLOR = DrawableColor.of(new Color(3, 83, 138));
    protected static final DrawableColor KEYFRAME_COLOR = DrawableColor.of(new Color(255, 255, 255));
    protected static final DrawableColor KEYFRAME_COLOR_SELECTED = DrawableColor.of(new Color(180, 37, 196));
    protected static final DrawableColor PROGRESS_COLOR = DrawableColor.of(new Color(255, 0, 0));
    protected static final DrawableColor PREVIEW_COLOR_NORMAL = DrawableColor.of(new Color(33, 176, 58));
    protected static final DrawableColor RECORDING_COLOR = DrawableColor.of(new Color(196, 37, 37));
    protected static final DrawableColor RECORDING_PAUSED_COLOR = DrawableColor.of(new Color(219, 108, 4));
    protected static final DrawableColor OFFSET_MODE_CROSSHAIR_COLOR = DrawableColor.of(new Color(219, 108, 4));

    protected static final int TIMELINE_HEIGHT = 50;
    protected static final int TIMELINE_Y_PADDING = 20;
    protected static final int KEYFRAME_LINE_WIDTH = 2;
    protected static final int KEYFRAME_LINE_HEIGHT = 30;
    protected static final int PROGRESS_LINE_WIDTH = 2;
    protected static final int MIN_TIMELINE_DURATION = 1000; // 1 second minimum
    protected static final int TIMELINE_EXTENSION_STEP = 2000; // Extend by 2 seconds
    protected static final long TIMELINE_PADDING_DURATION = 2000; // 2 seconds padding
    protected static final int KEYFRAME_DRAG_CRUMPLE_ZONE = 3; // Pixels threshold before movement starts
    protected static final long RECORDING_BLINK_INTERVAL = 600; // 600ms blink interval
    protected static final int NOTIFICATION_PADDING = 10;

    protected static final Component KEYFRAME_ADDED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.keyframe_added").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
    protected static final Component KEYFRAME_DELETED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.keyframe_deleted").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));
    protected static final Component PLAYING_STARTED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.playing_started").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().success_text_color.getColorInt()));
    protected static final Component PLAYING_STOPPED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.playing_stopped").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt()));

    protected final AnimationControllerElement controller;
    protected final Consumer<AnimationControllerMetadata> resultCallback;
    protected final List<AnimationKeyframe> workingKeyframes;
    protected final PreviewElement previewElement;
    protected final PreviewEditorElement previewEditorElement;
    protected boolean isDraggingProgress = false;
    protected boolean isPlaying = false;
    protected long playStartTime = -1;
    protected long currentPlayOrRecordPosition = 0;
    protected final List<AnimationKeyframe> selectedKeyframes = new ArrayList<>();
    protected int draggingKeyframeIndex = -1;
    protected AnimationKeyframe lastCtrlClickedFrameForDeselect = null;
    protected boolean framesGotMoved = false;
    protected int timelineX;
    protected int timelineWidth;
    protected int timelineY;
    protected long timelineDuration = MIN_TIMELINE_DURATION;
    protected int initialDragClickX = 0;
    protected boolean hasMovedFromClickPosition = false;
    protected boolean isRecording = false;
    protected boolean isRecordingPaused = false;
    protected long recordStartTime = -1;
    protected double recordingSpeed = 1.0; // 1.0 = 100%, 0.5 = 50%, etc.
    protected double cachedRecordingSpeed;
    protected final Map<Integer, Integer> cachedWidgetRowCurrentX = new HashMap<>();
    protected long lastRecordingBlinkTime = -1;
    protected boolean recordingBlinkState = true;
    protected final List<Notification> activeNotifications = new ArrayList<>();
    protected boolean isShowingSmoothingInput = false;
    protected String lastSmoothingInputValue = null;
    protected boolean isShowingTimestampInput = false;
    protected boolean isOffsetMode = false;

    protected final Stack<List<AnimationKeyframe>> undoStack = new Stack<>();
    protected final Stack<List<AnimationKeyframe>> redoStack = new Stack<>();

    protected CycleButton<ElementAnchorPoint> anchorButton;
    protected CycleButton<CommonCycles.CycleEnabledDisabled> stickyButton;
    protected ExtendedButton undoButton;
    protected ExtendedButton redoButton;
    protected ExtendedButton deleteKeyframeButton;
    protected ExtendedButton playButton;
    protected ExtendedButton startStopRecordingButton;
    protected RangeSlider recordingSpeedSlider;
    protected ExtendedButton smoothingButton;
    protected ExtendedEditBox smoothingDistanceInput;
    protected ExtendedEditBox timestampInput;

    protected int lastGuiScaleCorrectionWidth = 0;
    protected int lastGuiScaleCorrectionHeight = 0;

    public KeyframeManagerScreen(AnimationControllerElement controller, Consumer<AnimationControllerMetadata> resultCallback) {
        super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager"));
        this.controller = controller;
        this.isOffsetMode = this.controller.offsetMode;
        this.resultCallback = resultCallback;
        this.workingKeyframes = new ArrayList<>(controller.keyframes.stream()
                .map(AnimationKeyframe::clone)
                .toList());
        // Sort keyframes by timestamp
        this.workingKeyframes.sort(Comparator.comparingLong(k -> k.timestamp));

        // Set initial timeline duration based on last keyframe
        for (AnimationKeyframe kf : workingKeyframes) {
            timelineDuration = Math.max(timelineDuration, kf.timestamp);
        }
        timelineDuration += TIMELINE_PADDING_DURATION;

        // Create dummy element for preview
        this.previewElement = new PreviewElement(controller.builder);
        this.previewElement.baseWidth = 50;
        this.previewElement.baseHeight = 50;
        this.previewElement.posOffsetX = 0;
        this.previewElement.posOffsetY = 0;
        this.previewElement.stayOnScreen = false;
        this.previewElement.stickyAnchor = true;
        this.previewElement.anchorPoint = ElementAnchorPoints.MID_CENTERED;
        if (!this.workingKeyframes.isEmpty()) {
            this.applyKeyframeValuesToElement(this.workingKeyframes.getFirst(), this.previewElement);
        }

        // Create editor element wrapper for resizing
        this.previewEditorElement = new PreviewEditorElement(this.previewElement, new LayoutEditorScreen(Layout.buildUniversal()));

        // Save initial state for undo
        saveState();
    }

    @Override
    protected void init() {

        timelineX = 50;
        timelineWidth = width - 100;
        timelineY = height - TIMELINE_HEIGHT - TIMELINE_Y_PADDING;

        this.cachedWidgetRowCurrentX.clear();

        int buttonBaseWidth = 60;

        // TOP OF THE SCREEN --------------------------------------->

        // Cancel button
        ExtendedButton cancelButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(10, 10, buttonBaseWidth, 20,
                Component.translatable("gui.cancel"),
                button -> this.resultCallback.accept(null)));
        this.addRenderableWidget(cancelButton);

        // Done button
        ExtendedButton doneButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(80, 10, buttonBaseWidth, 20,
                Component.translatable("gui.done"),
                button -> this.resultCallback.accept(new AnimationControllerMetadata(this.workingKeyframes, this.isOffsetMode))));
        this.addRenderableWidget(doneButton);

        // BOTTOM BUTTON ROW 1 ------------------------------------>

        // Play button
        this.playButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.empty(),
                button -> togglePlayback()));
        this.playButton.setLabelSupplier(consumes -> Component.translatable(isPlaying ? "fancymenu.elements.animation_controller.keyframe_manager.pause" : "fancymenu.elements.animation_controller.keyframe_manager.play"));
        this.playButton.setIsActiveSupplier(consumes -> !this.isRecording);
        this.addBottomWidget(1, 0, this.playButton);

        // Start/Stop Recording button
        this.startStopRecordingButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.empty(),
                button -> toggleRecording()));
        this.startStopRecordingButton.setLabelSupplier(consumes ->
                Component.translatable(isRecording ?
                        "fancymenu.elements.animation_controller.keyframe_manager.stop_recording" :
                        "fancymenu.elements.animation_controller.keyframe_manager.start_recording"));
        this.startStopRecordingButton.setIsActiveSupplier(consumes -> !this.isPlaying);
        this.addBottomWidget(1, 0, this.startStopRecordingButton);

        // Recording speed slider
        this.recordingSpeedSlider = new RangeSlider(0, 0, buttonBaseWidth + 60, 20, Component.empty(), 0, 100, this.recordingSpeed * 100);
        recordingSpeedSlider.setShowAsInteger(true);
        recordingSpeedSlider.setLabelSupplier(slider -> {
            String speedText = slider.getValueDisplayText() + "%";
            return Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording_speed", speedText);
        });
        recordingSpeedSlider.setSliderValueUpdateListener((slider, valueDisplayText, value) -> {
            // value is not the range value, but actual slider value between 0.0 and 1.0
            this.setRecordingSpeed(value);
        });
        recordingSpeedSlider.setFocusable(true);
        recordingSpeedSlider.setNavigatable(true);
        UIBase.applyDefaultWidgetSkinTo(recordingSpeedSlider);
        this.addBottomWidget(1, 0, recordingSpeedSlider);

        // Add keyframe button
        ExtendedButton addKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.add_keyframe"),
                button -> addKeyframeAtProgress()));
        addKeyframeButton.setIsActiveSupplier(consumes -> this.isRecording && this.selectedKeyframes.isEmpty());
        this.addBottomWidget(1, 0, addKeyframeButton);

        // Delete keyframe button
        this.deleteKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.delete_keyframe"),
                button -> deleteSelectedKeyframes()));
        this.deleteKeyframeButton.setIsActiveSupplier(consumes -> !this.selectedKeyframes.isEmpty());
        this.addBottomWidget(1, 0, this.deleteKeyframeButton);

        // Smoothing button
        this.smoothingButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.smoothing"),
                button -> toggleSmoothingInput()));
        this.smoothingButton.setIsActiveSupplier(consumes -> !this.isPlaying && !this.isRecording && (this.selectedKeyframes.size() > 1) && !this.isShowingTimestampInput);
        this.smoothingButton.setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.smoothing.desc")));
        this.addBottomWidget(1, 0, this.smoothingButton);

        // Offset Mode button
        CycleButton<?> offsetModeButton = new CycleButton<>(0, 0, buttonBaseWidth, 0,
                CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.offset_mode", this.isOffsetMode),
                (value, button) -> this.setOffsetMode(value.getAsBoolean()));
        offsetModeButton.setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.offset_mode.desc")));
        this.addBottomWidget(1, 0, offsetModeButton);

        // BOTTOM BUTTON ROW 2 ------------------------------------>

        // Undo button
        this.undoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.editor.edit.undo"),
                button -> undo()));
        this.undoButton.setIsActiveSupplier(consumes -> !undoStack.isEmpty() && !this.isPlaying);
        this.addBottomWidget(2, 0, this.undoButton);

        // Redo button
        this.redoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.editor.edit.redo"),
                button -> redo()));
        this.redoButton.setIsActiveSupplier(consumes -> !redoStack.isEmpty() && !this.isPlaying);
        this.addBottomWidget(2, 0, this.redoButton);

        // Anchor point cycle button
        List<ElementAnchorPoint> anchorPoints = ElementAnchorPoints.getAnchorPoints();
        anchorPoints.remove(ElementAnchorPoints.ELEMENT);
        anchorPoints.remove(ElementAnchorPoints.VANILLA);
        this.anchorButton = new CycleButton<>(0, 0, buttonBaseWidth + 105, 0,
                CommonCycles.cycle("fancymenu.elements.animation_controller.keyframe_manager.anchor_point_cycle", anchorPoints, ElementAnchorPoints.TOP_LEFT)
                        .setValueNameSupplier(ElementAnchorPoint::getName)
                        .setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())),
                (value, button) -> this.setAnchorPoint(value));
        this.anchorButton.setIsActiveSupplier(consumes -> ((this.selectedKeyframes.size() == 1) || this.isRecording) && !this.isOffsetMode);
        this.addBottomWidget(2, 0, this.anchorButton);

        // Sticky anchor toggle
        this.stickyButton = new CycleButton<>(0, 0, buttonBaseWidth + 65, 0,
                CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.sticky"),
                (value, button) -> this.setStickyAnchor(value.getAsBoolean()));
        this.stickyButton.setIsActiveSupplier(consumes -> ((this.selectedKeyframes.size() == 1) || this.isRecording) && !this.isOffsetMode);
        this.addBottomWidget(2, 0, this.stickyButton);

        // Timestamp button
        ExtendedButton timestampButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit"),
                button -> toggleTimestampInput()));
        timestampButton.setIsActiveSupplier(consumes -> !this.isPlaying && !this.isRecording && (this.selectedKeyframes.size() == 1) && !this.isShowingSmoothingInput);
        timestampButton.setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit.desc")));
        this.addBottomWidget(2, 0, timestampButton);

        // Preview moving toggle
        CycleButton<?> previewMovingButton = new CycleButton<>(0, 0, buttonBaseWidth + 65, 0,
                CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.move_preview_with_arrow_keys", FancyMenu.getOptions().arrowKeysMovePreview.getValue()),
                (value, button) -> FancyMenu.getOptions().arrowKeysMovePreview.setValue(value.getAsBoolean()));
        previewMovingButton.setTooltipSupplier(consumes -> Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.move_preview_with_arrow_keys.desc")));
        this.addBottomWidget(2, 0, previewMovingButton);

        // ---------------------------------------------------------

        // Smoothing input box
        this.smoothingDistanceInput = new ExtendedEditBox(Minecraft.getInstance().font, (this.width / 2) - 50, this.stickyButton.getY() - 40, 100, 20, Component.empty()) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                MutableComponent c = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.smoothing.input");
                int cW = Minecraft.getInstance().font.width(c);
                graphics.drawString(Minecraft.getInstance().font, c,
                        this.getX() + (this.getWidth() / 2) - (cW / 2), this.getY() - Minecraft.getInstance().font.lineHeight - 5, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);
                super.renderWidget(graphics, mouseX, mouseY, partial);
            }
        };
        this.smoothingDistanceInput.setCharacterFilter(CharacterFilter.buildIntegerFiler());
        this.smoothingDistanceInput.setIsVisibleSupplier(consumes -> this.isShowingSmoothingInput);
        this.smoothingDistanceInput.setMaxLength(6); // Reasonable limit for ms value
        UIBase.applyDefaultWidgetSkinTo(this.smoothingDistanceInput);
        this.addRenderableWidget(this.smoothingDistanceInput);

        // ---------------------------------------------------------

        // Timestamp input box
        this.timestampInput = new ExtendedEditBox(Minecraft.getInstance().font, (this.width / 2) - 50, this.stickyButton.getY() - 40, 100, 20, Component.empty()) {
            @Override
            public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                MutableComponent c = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.timestamp_edit.input");
                int cW = Minecraft.getInstance().font.width(c);
                graphics.drawString(Minecraft.getInstance().font, c,
                        this.getX() + (this.getWidth() / 2) - (cW / 2), this.getY() - Minecraft.getInstance().font.lineHeight - 5, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);
                super.renderWidget(graphics, mouseX, mouseY, partial);
            }
        };
        this.timestampInput.setCharacterFilter(CharacterFilter.buildIntegerFiler());
        this.timestampInput.setIsVisibleSupplier(consumes -> this.isShowingTimestampInput);
        this.timestampInput.setMaxLength(20);
        UIBase.applyDefaultWidgetSkinTo(this.timestampInput);
        this.addRenderableWidget(this.timestampInput);

        // ---------------------------------------------------------

        if (!isPlaying && !isRecording) {
            updateTimelineDurationToMaxTimestamp();
        }

        AbstractWidget farRightWidget = previewMovingButton;
        Window window = Minecraft.getInstance().getWindow();
        boolean resized = (window.getScreenWidth() != this.lastGuiScaleCorrectionWidth) || (window.getScreenHeight() != this.lastGuiScaleCorrectionHeight);
        this.lastGuiScaleCorrectionWidth = window.getScreenWidth();
        this.lastGuiScaleCorrectionHeight = window.getScreenHeight();
        boolean tooFarRight = (farRightWidget.getX() + farRightWidget.getWidth()) >= (this.width - 100);

        //Adjust GUI scale to make all buttons fit in the screen
        if (tooFarRight && (window.getGuiScale() > 1)) {
            double newScale = window.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            window.setGuiScale(newScale);
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if (!tooFarRight && resized) {
            RenderingUtils.resetGuiScale();
            this.resize(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }

    }

    @SuppressWarnings("all")
    /**
     * @param row Row 1 is the first row and is the one that's closest to the timeline. Row 2 will be above row 1 and so on.
     * @param spacingAfterButtonOffset The spacing offset that gets added after the button. Set to 0 for default spacing.
     * @param widget The widget to add.
     * @return The new current X.
     */
    protected <T extends AbstractWidget> T addBottomWidget(int row, int spacingAfterButtonOffset, @NotNull T widget) {
        if (row < 1) row = 1;
        if (!this.cachedWidgetRowCurrentX.containsKey(row)) {
            this.cachedWidgetRowCurrentX.put(row, this.timelineX);
        }
        int currentX = this.cachedWidgetRowCurrentX.get(row);
        int y = this.timelineY - 25 - (25 * (row-1));
        int buttonSpacing = 5 + spacingAfterButtonOffset;
        widget.setX(currentX);
        widget.setY(y);
        widget.setHeight(20);
        int labelWidth = Minecraft.getInstance().font.width(widget.getMessage());
        if ((labelWidth + 10) > widget.getWidth()) widget.setWidth(labelWidth + 10);
        UIBase.applyDefaultWidgetSkinTo(widget);
        this.addRenderableWidget(widget);
        int newCurrentX = currentX + widget.getWidth() + buttonSpacing;
        this.cachedWidgetRowCurrentX.put(row, newCurrentX);
        return widget;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {


        if (this.isShowingSmoothingInput) {
            if (!this.smoothingDistanceInput.isFocused()) {
                this.smoothingDistanceInput.setFocusable(true);
                this.setFocused(this.smoothingDistanceInput);
            }
        } else {
            if (this.smoothingDistanceInput == this.getFocused()) this.clearFocus();
        }

        if (this.isShowingTimestampInput && (this.selectedKeyframes.size() != 1)) {
            this.isShowingTimestampInput = false;
        }
        if (this.isShowingTimestampInput) {
            if (!this.timestampInput.isFocused()) {
                this.timestampInput.setFocusable(true);
                this.setFocused(this.timestampInput);
            }
        } else {
            if (this.timestampInput == this.getFocused()) this.clearFocus();
        }

        long actualEndTime = timelineDuration - TIMELINE_PADDING_DURATION;

        // Update play position when recording
        if (isRecording && !isPlaying) {
            // When paused, currentPlayOrRecordPosition stays the same
            if (!isRecordingPaused) {
                // Calculate elapsed time with speed factor
                long now = System.currentTimeMillis();
                long actualElapsed = now - recordStartTime;
                currentPlayOrRecordPosition = (long)(actualElapsed * recordingSpeed);
                long trimmedActualDuration = (timelineDuration - TIMELINE_PADDING_DURATION) - 2000;
                // Extend timeline if we're near the end
                if (currentPlayOrRecordPosition >= trimmedActualDuration) {
                    this.updateTimelineDuration((timelineDuration - TIMELINE_PADDING_DURATION) + TIMELINE_EXTENSION_STEP);
                    actualEndTime = timelineDuration - TIMELINE_PADDING_DURATION;
                }
            }
        }

        // Update play position
        if (isPlaying && !isRecording) {
            currentPlayOrRecordPosition = System.currentTimeMillis() - playStartTime; // elapsed time
            // Stop if reached end
            if (currentPlayOrRecordPosition > timelineDuration) {
                isPlaying = false;
                currentPlayOrRecordPosition = 0;
            }
        }

        // Render screen background
        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        LayoutEditorScreen.renderGrid(graphics, this.width, this.height);

        this.renderTimelineBackground(graphics, actualEndTime);

        this.renderKeyframeLines(graphics, mouseX, mouseY, partial, actualEndTime);

        this.renderProgressLine(graphics, mouseX, mouseY, partial, actualEndTime);

        this.tickAnimation(); // It's important that this gets called before renderPreview()

        this.renderPreview(graphics, mouseX, mouseY, partial);

        this.renderTimeText(graphics, actualEndTime);

        this.renderKeyframeInfo(graphics, mouseX, mouseY, partial);

        this.renderRecordingIndicator(graphics, mouseX, mouseY, partial);

        this.renderOffsetModeCrosshair(graphics);

        this.renderNotifications(graphics, mouseX, mouseY, partial);

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void tickAnimation() {
        if (this.isRecording && !this.isRecordingPaused) return;

        // Update preview element based on current time
        AnimationKeyframe currentFrame = null;
        AnimationKeyframe nextFrame = null;

        // Find current and next keyframes
        for (int i = 0; i < workingKeyframes.size() - 1; i++) {
            AnimationKeyframe k1 = workingKeyframes.get(i);
            AnimationKeyframe k2 = workingKeyframes.get(i + 1);
            if ((currentPlayOrRecordPosition >= k1.timestamp) && (currentPlayOrRecordPosition < k2.timestamp)) {
                currentFrame = k1;
                nextFrame = k2;
                break;
            }
        }

        // Apply interpolated values to preview element
        if ((this.isPlaying || this.isDraggingProgress) && (currentFrame != null) && (nextFrame != null)) {
            this.selectKeyframeClearOldSelection(null);
            float progress = (float)(currentPlayOrRecordPosition - currentFrame.timestamp) / (float)(nextFrame.timestamp - currentFrame.timestamp);

            if (this.isOffsetMode) {
                previewElement.animatedOffsetX = (int)lerp(currentFrame.posOffsetX, nextFrame.posOffsetX, progress);
                previewElement.animatedOffsetY = (int)lerp(currentFrame.posOffsetY, nextFrame.posOffsetY, progress);
                previewElement.posOffsetX = 0;
                previewElement.posOffsetY = 0;
            } else {
                previewElement.posOffsetX = (int)lerp(currentFrame.posOffsetX, nextFrame.posOffsetX, progress);
                previewElement.posOffsetY = (int)lerp(currentFrame.posOffsetY, nextFrame.posOffsetY, progress);
            }

            previewElement.baseWidth = (int)lerp(currentFrame.baseWidth, nextFrame.baseWidth, progress);
            previewElement.baseHeight = (int)lerp(currentFrame.baseHeight, nextFrame.baseHeight, progress);
            previewElement.anchorPoint = this.isOffsetMode ? ElementAnchorPoints.MID_CENTERED : nextFrame.anchorPoint;
            previewElement.stickyAnchor = nextFrame.stickyAnchor;
        }
    }

    protected void renderOffsetModeCrosshair(@NotNull GuiGraphics graphics) {
        if (!this.isOffsetMode) return;
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        // Horizontal line
        graphics.fill(centerX - 10, centerY - 1, centerX + 10, centerY + 1, OFFSET_MODE_CROSSHAIR_COLOR.getColorInt());
        // Vertical line
        graphics.fill(centerX - 1, centerY - 10, centerX + 1, centerY + 10, OFFSET_MODE_CROSSHAIR_COLOR.getColorInt());
    }

    protected void renderTimelineBackground(@NotNull GuiGraphics graphics, long actualEndTime) {

        // Calculate what portion of the timeline width to use based on duration
        float usableWidth = timelineWidth * ((float)Math.max(actualEndTime + TIMELINE_PADDING_DURATION, MIN_TIMELINE_DURATION) / timelineDuration);

        // Draw main timeline background
        graphics.fill(RenderType.gui(),
                timelineX, timelineY,
                timelineX + (int)((float)actualEndTime / timelineDuration * usableWidth),
                timelineY + TIMELINE_HEIGHT,
                TIMELINE_COLOR.getColorInt());

        // Draw padding area
        int paddingStartX = timelineX + (int)((float)actualEndTime / timelineDuration * usableWidth);
        graphics.fill(RenderType.gui(),
                paddingStartX, timelineY,
                timelineX + (int)usableWidth,
                timelineY + TIMELINE_HEIGHT,
                TIMELINE_PADDING_COLOR.getColorInt());

    }

    protected void renderKeyframeLines(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, long actualDuration) {
        // Draw keyframe lines
        for (int i = 0; i < workingKeyframes.size(); i++) {
            AnimationKeyframe keyframe = workingKeyframes.get(i);

            // Calculate x position for keyframe line
            float progress = (float) keyframe.timestamp / timelineDuration;
            int lineX = timelineX + (int)(timelineWidth * progress);

            // Draw keyframe line
            DrawableColor color = selectedKeyframes.contains(keyframe) ?
                    KEYFRAME_COLOR_SELECTED : KEYFRAME_COLOR;

            graphics.fill(RenderType.gui(),
                    lineX - KEYFRAME_LINE_WIDTH/2,
                    timelineY + (TIMELINE_HEIGHT - KEYFRAME_LINE_HEIGHT)/2,
                    lineX + KEYFRAME_LINE_WIDTH/2,
                    timelineY + (TIMELINE_HEIGHT + KEYFRAME_LINE_HEIGHT)/2,
                    color.getColorInt());

            // Handle dragging keyframe
            if (draggingKeyframeIndex == i) {
                // Check if we've moved enough to start dragging
                int dragDeltaX = mouseX - initialDragClickX;
                if (!hasMovedFromClickPosition) {
                    hasMovedFromClickPosition = Math.abs(dragDeltaX) >= KEYFRAME_DRAG_CRUMPLE_ZONE;
                    if (!hasMovedFromClickPosition) {
                        continue;
                    }
                    this.framesGotMoved = true;
                    //Save state right before keyframe moving starts
                    saveState();
                }

                // Calculate mouse position relative to timeline
                float newProgress = (float)(mouseX - timelineX) / timelineWidth;
                long newDuration = this.timelineDuration;

                // If dragging past timeline end and not recording, extend timeline
                if (!this.isRecording && (mouseX > (timelineX + timelineWidth - 10))) {
                    newDuration = timelineDuration + TIMELINE_EXTENSION_STEP;
                    newProgress = (float)(mouseX - timelineX) / timelineWidth;
                }

                // Clamp progress
                newProgress = Math.max(0, Math.min(1, newProgress));

                // Calculate time difference for the dragged keyframe
                long timeDelta = (long)(newDuration * newProgress) - keyframe.timestamp;

                // Find the minimum and maximum timestamps among selected frames
                long minSelectedTimestamp = Long.MAX_VALUE;
                long maxSelectedTimestamp = Long.MIN_VALUE;
                long deltaToLastFrame = 0;
                for (AnimationKeyframe selectedFrame : selectedKeyframes) {
                    minSelectedTimestamp = Math.min(minSelectedTimestamp, selectedFrame.timestamp);
                    maxSelectedTimestamp = Math.max(maxSelectedTimestamp, selectedFrame.timestamp);
                    if (selectedFrame.timestamp > keyframe.timestamp) {
                        deltaToLastFrame = Math.max(deltaToLastFrame, selectedFrame.timestamp - keyframe.timestamp);
                    }
                }

                // Calculate how much we can shift based on the leftmost selected frame
                long maxLeftShift = -minSelectedTimestamp;  // Maximum we can shift left without going negative
                if (timeDelta < maxLeftShift) {
                    timeDelta = maxLeftShift;  // Limit the shift to prevent negative timestamps
                }

                // When recording, ensure we don't exceed actual duration while preserving spacing
                if (this.isRecording) {
                    long maxRightPosition = actualDuration - deltaToLastFrame;
                    if ((keyframe.timestamp + timeDelta) > maxRightPosition) {
                        timeDelta = maxRightPosition - keyframe.timestamp;
                    }
                }

                // Apply the adjusted time delta to all selected keyframes
                for (AnimationKeyframe selectedFrame : selectedKeyframes) {
                    long newTimestamp = selectedFrame.timestamp + timeDelta;
                    newTimestamp = Math.max(0, newTimestamp); // Ensure timestamp never goes negative

                    if (this.isRecording) {
                        newTimestamp = Math.min(newTimestamp, actualDuration); // Ensure no frame exceeds actual duration
                    }
                    selectedFrame.timestamp = newTimestamp;
                }

                // Update timeline duration to ensure padding
                updateTimelineDurationToMaxTimestamp();
            }
        }
    }

    protected void renderPreview(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Render preview
        if (!isPlaying && ((this.selectedKeyframes.size() == 1) || isRecording)) {
            // Render full preview element for resizing and moving when recording or when a keyframe is selected
            previewEditorElement.render(graphics, mouseX, mouseY, partial);
        } else {
            // Otherwise only render body to prevent the user from interacting with the preview element
            this.previewEditorElement.renderPreviewBody(graphics, mouseX, mouseY, partial);
        }

    }

    protected void renderProgressLine(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial, long actualDuration) {

        // Draw progress line
        float playProgress = (float) currentPlayOrRecordPosition / timelineDuration;
        int progressX = timelineX + (int)(timelineWidth * playProgress);

        //Handle progress line dragging
        if (isDraggingProgress && (!this.isRecording || this.isRecordingPaused)) {
            float newProgress = (float)(mouseX - timelineX) / timelineWidth;
            // Clamp progress between 0 and actual duration when recording
            if (this.isRecording) {
                float maxProgress = (float)actualDuration / timelineDuration;
                newProgress = Math.max(0, Math.min(maxProgress, newProgress));
            } else {
                // Regular clamping between 0 and 1 when not recording
                newProgress = Math.max(0, Math.min(1, newProgress));
            }
            currentPlayOrRecordPosition = (long)(timelineDuration * newProgress);
            if (this.isPlaying) {
                this.playStartTime = System.currentTimeMillis() - this.currentPlayOrRecordPosition;
            }
            progressX = timelineX + (int)(timelineWidth * newProgress);
        }

        graphics.fill(RenderType.gui(),
                progressX - PROGRESS_LINE_WIDTH/2,
                timelineY,
                progressX + PROGRESS_LINE_WIDTH/2,
                timelineY + TIMELINE_HEIGHT,
                PROGRESS_COLOR.getColorIntWithAlpha(0.7F));

    }

    protected void renderTimeText(@NotNull GuiGraphics graphics, long actualEndTime) {

        // Format both times
        String currentTimeStr = formatTime(currentPlayOrRecordPosition);
        String totalTimeStr = formatTime(actualEndTime);

        // Determine current time color
        int currentTimeColor = currentPlayOrRecordPosition > actualEndTime ?
                UIBase.getUIColorTheme().warning_text_color.getColorInt() :
                UIBase.getUIColorTheme().generic_text_base_color.getColorInt();

        MutableComponent currentTimeComp = Component.literal(currentTimeStr).setStyle(Style.EMPTY.withColor(currentTimeColor));
        MutableComponent totalTimeComp = Component.literal(totalTimeStr);
        MutableComponent baseComp = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.time", currentTimeComp, totalTimeComp);

        graphics.drawString(Minecraft.getInstance().font, baseComp, timelineX, timelineY + TIMELINE_HEIGHT + 5,
                UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

    }

    protected void renderKeyframeInfo(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.selectedKeyframes.size() == 1) {

            AnimationKeyframe selectedKeyframe = this.selectedKeyframes.getFirst();

            String yes = I18n.get("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info.yes");
            String no = I18n.get("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info.no");
            Component[] lines = LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info",
                    formatTime(selectedKeyframe.timestamp),
                    ""+selectedKeyframe.posOffsetX,
                    ""+selectedKeyframe.posOffsetY,
                    ""+selectedKeyframe.baseWidth,
                    ""+selectedKeyframe.baseHeight,
                    selectedKeyframe.anchorPoint.getName(),
                    selectedKeyframe.stickyAnchor ? yes : no);

            int yOffset = 10 + 20 + 10;
            for (Component line : lines) {
                graphics.drawString(Minecraft.getInstance().font, line, 10, yOffset, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);
                yOffset += 10;
            }

        }

    }

    protected void renderRecordingIndicator(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (isRecording) {

            long currentTime = System.currentTimeMillis();

            // Update blink state
            if ((currentTime - lastRecordingBlinkTime) > RECORDING_BLINK_INTERVAL) {
                recordingBlinkState = !recordingBlinkState;
                lastRecordingBlinkTime = currentTime;
            }

            // Calculate positions for recording indicator
            MutableComponent recordingText = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording").setStyle(Style.EMPTY.withColor(RECORDING_COLOR.getColorInt()));
            MutableComponent manualModeText = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording_speed.manual_mode").setStyle(Style.EMPTY.withColor(RECORDING_PAUSED_COLOR.getColorInt()));
            int recordingTextWidth = Minecraft.getInstance().font.width(recordingText);
            int manualModeTextWidth = Minecraft.getInstance().font.width(manualModeText);
            int rectSize = 20;
            int padding = 5;
            int totalWidthRecordingText = recordingTextWidth + padding + rectSize;
            int totalWidthManualModeText = manualModeTextWidth + padding + rectSize;
            int xOffset = 10;
            int yOffset = 10;
            int recordingTextX = width - totalWidthRecordingText - xOffset;
            int manualModeTextX = width - totalWidthManualModeText - xOffset;

            int recordingTextOffsetY = !isRecordingPaused ? (rectSize / 2) - (Minecraft.getInstance().font.lineHeight / 2) : 0;

            // Draw "Recording" text
            graphics.drawString(Minecraft.getInstance().font, recordingText, recordingTextX, yOffset + recordingTextOffsetY, -1, false);

            // Draw manual mode text
            if (this.isRecordingPaused) {
                graphics.drawString(Minecraft.getInstance().font, manualModeText, manualModeTextX, yOffset + rectSize - Minecraft.getInstance().font.lineHeight, -1, false);
            }

            // Draw indicator rectangle
            if (recordingBlinkState) {
                int rectX = recordingTextX + recordingTextWidth + padding;
                graphics.fill(RenderType.gui(), rectX, yOffset, rectX + rectSize, yOffset + rectSize, RECORDING_COLOR.getColorInt());
            }

        }

    }

    protected void renderNotifications(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Handle notifications
        Iterator<Notification> iterator = activeNotifications.iterator();
        int notificationYOffset = 10; // Start below top padding
        // If recording indicator is visible, start notifications below it
        if (isRecording) {
            notificationYOffset += 30; // Height of recording indicator plus padding
        }
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (notification.isExpired()) {
                iterator.remove();
                continue;
            }
            notification.updateOpacity();
            int textColor = UIBase.getUIColorTheme().generic_text_base_color.getColorIntWithAlpha(notification.opacity); // Base color with fade
            graphics.drawString(
                    Minecraft.getInstance().font,
                    notification.message,
                    width - Minecraft.getInstance().font.width(notification.message) - NOTIFICATION_PADDING,
                    notificationYOffset,
                    textColor,
                    false
            );
            notificationYOffset += notification.getHeight();
        }

    }

    protected void updateTimelineDurationToMaxTimestamp() {

        if (this.isRecording) return;

        // Find the furthest right keyframe
        long maxTimestamp = 0;
        for (AnimationKeyframe kf : workingKeyframes) {
            maxTimestamp = Math.max(maxTimestamp, kf.timestamp);
        }

        this.updateTimelineDuration(maxTimestamp);

    }

    protected void updateTimelineDuration(long newDurationWithoutPadding) {
        // Add padding after the last keyframe
        long newDuration = newDurationWithoutPadding + TIMELINE_PADDING_DURATION;
        // Don't let duration go below minimum
        timelineDuration = Math.max(MIN_TIMELINE_DURATION, newDuration);
    }

    protected String formatTime(long milliseconds) {
        if (timelineDuration < 2000) {
            return milliseconds + "ms";
        } else {
            return String.format("%.1fs", milliseconds / 1000.0f);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        this.lastCtrlClickedFrameForDeselect = null;
        this.framesGotMoved = false;

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.isShowingSmoothingInput) {
            // Check if click is outside the input box
            if (!this.smoothingDistanceInput.isMouseOver(mouseX, mouseY)) {
                this.lastSmoothingInputValue = this.smoothingDistanceInput.getValue();
                this.isShowingSmoothingInput = false;
            }
        }

        if (this.isShowingTimestampInput) {
            // Check if click is outside the input box
            if (!this.timestampInput.isMouseOver(mouseX, mouseY)) {
                this.isShowingTimestampInput = false;
            }
        }

        if (this.previewEditorElement.mouseClicked(mouseX, mouseY, button)) return true;

        // Handle clicking progress line
        if (isOverProgressLine((int)mouseX, (int)mouseY) && (!this.isRecording || this.isRecordingPaused)) {
            isDraggingProgress = true;
            return true;
        }
        if (isOverProgressLine((int)mouseX, (int)mouseY) && this.isRecording && !this.isRecordingPaused) {
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_drag_progress")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), 6000);
            return true;
        }

        // Handle clicking keyframe lines
        int clickedIndex = getKeyframeIndexAtPosition((int)mouseX, (int)mouseY);
        // Handle clicking empty timeline area to deselect frames
        if (!Screen.hasControlDown() && isInTimelineArea((int)mouseX, (int)mouseY) && (clickedIndex == -1)) {
            this.selectKeyframeClearOldSelection(null);
            return true;
        }
        if (this.isRecording && !this.isRecordingPaused && (clickedIndex >= 0)) {
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_edit_keyframe")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), 6000);
            return true;
        }
        if (clickedIndex >= 0) {
            initialDragClickX = (int)mouseX;
            hasMovedFromClickPosition = false;
            draggingKeyframeIndex = clickedIndex;
            AnimationKeyframe keyframe = this.workingKeyframes.get(draggingKeyframeIndex);
            if (Screen.hasControlDown() && this.selectedKeyframes.contains(keyframe)) {
                this.lastCtrlClickedFrameForDeselect = keyframe;
            } else {
                // Handle CTRL-click for multi-select
                this.selectKeyframe(workingKeyframes.get(clickedIndex), Screen.hasControlDown());
            }
            return true;
        }

        return false;

    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingProgress = false;
        draggingKeyframeIndex = -1;
        hasMovedFromClickPosition = false;
        boolean previewGotResized = this.previewEditorElement.recentlyResized;
        boolean previewGotMoved = this.previewEditorElement.recentlyMovedByDragging;
        this.previewEditorElement.mouseReleased(mouseX, mouseY, button);
        if (this.previewEditorElement.isSelected() && (previewGotResized || previewGotMoved) && (this.selectedKeyframes.size() == 1) && (!this.isRecording || this.isRecordingPaused) && !this.isPlaying) {
            saveState();
            this.applyElementValuesToKeyframe(this.previewElement, this.selectedKeyframes.getFirst());
        }
        if ((this.lastCtrlClickedFrameForDeselect != null) && !this.framesGotMoved) {
            if (this.selectedKeyframes.size() > 1) {
                this.selectedKeyframes.remove(this.lastCtrlClickedFrameForDeselect);
                if (this.selectedKeyframes.size() == 1) {
                    AnimationKeyframe lastSelected = this.selectedKeyframes.getFirst();
                    this.selectKeyframeClearOldSelection(null); // first clear all
                    this.selectKeyframeClearOldSelection(lastSelected); // then properly single-select the frame again
                }
            } else {
                this.selectKeyframeClearOldSelection(null);
            }
        }
        this.lastCtrlClickedFrameForDeselect = null;
        this.framesGotMoved = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        String key = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (key == null) key = "";
        key = key.toLowerCase();

        // Handle undo/redo keyboard shortcuts
        if (Screen.hasControlDown()) {
            if (key.equals("z")) {
                undo();
                return true;
            } else if (key.equals("y")) {
                redo();
                return true;
            }
        }

        // Handle smoothing input shortcuts
        if (this.isShowingSmoothingInput && this.smoothingDistanceInput.isFocused()) {
            this.lastSmoothingInputValue = this.smoothingDistanceInput.getValue();
            if (keyCode == InputConstants.KEY_ENTER) {
                applySmoothingDistance();
                return true;
            } else if (keyCode == InputConstants.KEY_ESCAPE) {
                this.isShowingSmoothingInput = false;
                return true;
            }
        }

        // Handle timestamp input shortcuts
        if (this.isShowingTimestampInput && this.timestampInput.isFocused()) {
            if (keyCode == InputConstants.KEY_ENTER) {
                this.saveState();
                if ((this.selectedKeyframes.size() == 1) && MathUtils.isLong(this.timestampInput.getValue())) {
                    this.selectedKeyframes.get(0).timestamp = Long.parseLong(this.timestampInput.getValue());
                }
                this.isShowingTimestampInput = false;
                updateTimelineDurationToMaxTimestamp();
                return true;
            } else if (keyCode == InputConstants.KEY_ESCAPE) {
                this.isShowingTimestampInput = false;
                return true;
            }
        }

        // Select all keyframes
        if (!this.isRecording || this.isRecordingPaused) {
            if (Screen.hasControlDown() && (keyCode == InputConstants.KEY_A)) {
                this.selectKeyframeClearOldSelection(null);
                this.workingKeyframes.forEach(keyframe -> this.selectKeyframe(keyframe, true));
            }
        }

        // Handle arrow keys for selected keyframes
        if (!this.selectedKeyframes.isEmpty() && !FancyMenu.getOptions().arrowKeysMovePreview.getValue()) {
            if (keyCode == KEY_MOVE_KEYFRAME_LEFT || keyCode == KEY_MOVE_KEYFRAME_RIGHT) {
                saveState(); // Save state before modifying timestamp

                // Find the minimum and maximum timestamps among selected frames
                long minSelectedTimestamp = Long.MAX_VALUE;
                long maxSelectedTimestamp = Long.MIN_VALUE;
                for (AnimationKeyframe selectedFrame : selectedKeyframes) {
                    minSelectedTimestamp = Math.min(minSelectedTimestamp, selectedFrame.timestamp);
                    maxSelectedTimestamp = Math.max(maxSelectedTimestamp, selectedFrame.timestamp);
                }

                // Calculate the time shift
                long timeShift = keyCode == KEY_MOVE_KEYFRAME_LEFT ? -100 : 100;

                // Check boundaries
                if (keyCode == KEY_MOVE_KEYFRAME_LEFT) {
                    // Don't move left if leftmost frame would go negative
                    if (minSelectedTimestamp + timeShift < 0) {
                        timeShift = -minSelectedTimestamp; // Adjust shift to exactly reach 0
                    }
                } else {
                    // Don't move right if rightmost frame would exceed timeline
                    if (maxSelectedTimestamp + timeShift > timelineDuration) {
                        timeShift = timelineDuration - maxSelectedTimestamp; // Adjust shift to exactly reach end
                    }
                }

                // Apply the adjusted time shift to all selected keyframes
                if (timeShift != 0) {
                    final long finalTimeShift = timeShift;
                    this.selectedKeyframes.forEach(selectedKeyframe -> {
                        selectedKeyframe.timestamp += finalTimeShift;
                    });
                    updateTimelineDurationToMaxTimestamp();
                }
                return true;
            }
        } else if (FancyMenu.getOptions().arrowKeysMovePreview.getValue() && (this.selectedKeyframes.size() == 1) && (!this.isRecording || this.isRecordingPaused) && !this.isPlaying) {
            if ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT) || (keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
                this.saveState();
                this.isShowingTimestampInput = false;
                this.isShowingSmoothingInput = false;
                if (keyCode == InputConstants.KEY_LEFT) {
                    this.previewElement.posOffsetX -= 1;
                }
                if (keyCode == InputConstants.KEY_RIGHT) {
                    this.previewElement.posOffsetX += 1;
                }
                if (keyCode == InputConstants.KEY_UP) {
                    this.previewElement.posOffsetY -= 1;
                }
                if (keyCode == InputConstants.KEY_DOWN) {
                    this.previewElement.posOffsetY += 1;
                }
                this.applyElementValuesToKeyframe(this.previewElement, this.selectedKeyframes.get(0));
                return true;
            }
        }

        // Handle DELETE key
        if (keyCode == KEY_DELETE_KEYFRAME) {
            if (!this.selectedKeyframes.isEmpty()) {
                this.deleteSelectedKeyframes();
                return true;
            }
        }

        if (keyCode == KEY_ADD_KEYFRAME) {
            this.addKeyframeAtProgress();
            return true;
        }

        if (keyCode == KEY_TOGGLE_PLAYING) {
            this.togglePlayback();
            return true;
        }

        if (keyCode == KEY_TOGGLE_RECORDING) {
            this.toggleRecording();
            return true;
        }

        if (keyCode == KEY_TOGGLE_PAUSE_RECORDING) {
            this.togglePauseRecording(true);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return this.previewEditorElement.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void applyElementValuesToKeyframe(@NotNull PreviewElement element, @NotNull AnimationKeyframe keyframe) {
        if (this.isOffsetMode) {
            // Calculate center points
            int screenCenterX = this.width / 2;
            int screenCenterY = this.height / 2;
            int elementCenterX = element.getAbsoluteX() + (element.getAbsoluteWidth() / 2);
            int elementCenterY = element.getAbsoluteY() + (element.getAbsoluteHeight() / 2);

            // Store offset from screen center
            keyframe.posOffsetX = elementCenterX - screenCenterX;
            keyframe.posOffsetY = elementCenterY - screenCenterY;
        } else {
            keyframe.posOffsetX = element.posOffsetX;
            keyframe.posOffsetY = element.posOffsetY;
        }
        keyframe.baseWidth = element.baseWidth;
        keyframe.baseHeight = element.baseHeight;
        keyframe.anchorPoint = this.isOffsetMode ? ElementAnchorPoints.MID_CENTERED : element.anchorPoint;
        keyframe.stickyAnchor = this.isOffsetMode ? true : element.stickyAnchor;
    }

    protected void applyKeyframeValuesToElement(@NotNull AnimationKeyframe keyframe, @NotNull PreviewElement element) {
        if (this.isOffsetMode) {
            element.animatedOffsetX = keyframe.posOffsetX;
            element.animatedOffsetY = keyframe.posOffsetY;
            element.posOffsetX = 0;
            element.posOffsetY = 0;
        } else {
            element.posOffsetX = keyframe.posOffsetX;
            element.posOffsetY = keyframe.posOffsetY;
        }
        element.baseWidth = keyframe.baseWidth;
        element.baseHeight = keyframe.baseHeight;
        element.anchorPoint = this.isOffsetMode ? ElementAnchorPoints.MID_CENTERED : keyframe.anchorPoint;
        element.stickyAnchor = this.isOffsetMode ? true : keyframe.stickyAnchor;
    }

    protected void togglePlayback() {
        if (isRecording) return;
        isPlaying = !isPlaying;
        if (isPlaying) {
            playStartTime = System.currentTimeMillis() - currentPlayOrRecordPosition;
            this.selectKeyframeClearOldSelection(null);
            draggingKeyframeIndex = -1;
            this.displayNotification(PLAYING_STARTED_TEXT, 2000);
        } else {
            this.displayNotification(PLAYING_STOPPED_TEXT, 2000);
        }
    }

    protected void toggleRecording() {
        if (isPlaying) return;
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    protected void startRecording() {
        if (isPlaying) return;
        isRecording = true;
        isRecordingPaused = (this.recordingSpeed == 0.0D);
        // Calculate the corrected start time based on current position and speed
        // This ensures recording starts at the correct position when speed < 100%
        recordStartTime = System.currentTimeMillis() - (long)(currentPlayOrRecordPosition / recordingSpeed);
        this.selectKeyframeClearOldSelection(null);
        draggingKeyframeIndex = -1;
        previewEditorElement.setSelected(true);
    }

    protected void stopRecording() {
        isRecording = false;
        isRecordingPaused = false;
        recordStartTime = -1;
        this.selectKeyframeClearOldSelection(null);
        // Reset play position to start
        currentPlayOrRecordPosition = 0;
        // Reset timeline duration if needed
        updateTimelineDurationToMaxTimestamp();
    }

    protected void togglePauseRecording(boolean updateSlider) {
        if (!this.isRecording) return;
        if (!this.isRecordingPaused) {
            this.cachedRecordingSpeed = this.recordingSpeed;
            this.setRecordingSpeed(0.0D);
        } else {
            this.setRecordingSpeed(this.cachedRecordingSpeed);
        }
        if (updateSlider && (this.recordingSpeedSlider != null)) {
            this.recordingSpeedSlider.setValue(this.recordingSpeed);
        }
    }

    /**
     * @param speed Value between 0.0 and 1.0
     */
    protected void setRecordingSpeed(double speed) {
        double oldSpeed = this.recordingSpeed;
        double newSpeed = Math.max(0.0D, Math.min(1.0D, speed));
        if (oldSpeed != newSpeed) {
            if (newSpeed > 0.0D) {
                if (this.isRecording) {
                    // Calculate and set new record start time to maintain current progress
                    long now = System.currentTimeMillis();
                    this.recordStartTime = now - (long) (this.currentPlayOrRecordPosition / newSpeed);
                }
                this.selectKeyframeClearOldSelection(null);
                this.isRecordingPaused = false;
            } else {
                this.isRecordingPaused = true;
            }
        }
        this.recordingSpeed = newSpeed;
    }

    protected void toggleOffsetMode() {
        this.setOffsetMode(!this.isOffsetMode);
    }

    protected void setOffsetMode(boolean offsetMode) {
        this.isOffsetMode = offsetMode;
        if (offsetMode) {
            this.previewElement.anchorPoint = ElementAnchorPoints.MID_CENTERED;
            this.anchorButton.setSelectedValue(ElementAnchorPoints.MID_CENTERED);
        }
    }

    protected void addKeyframeAtProgress() {
        if (!isRecording) return;

        saveState();

        AnimationKeyframe newKeyframe;

        if (this.isOffsetMode) {
            // Calculate center points
            int screenCenterX = this.width / 2;
            int screenCenterY = this.height / 2;
            int elementCenterX = previewElement.getAbsoluteX() + (previewElement.getAbsoluteWidth() / 2);
            int elementCenterY = previewElement.getAbsoluteY() + (previewElement.getAbsoluteHeight() / 2);

            // Create keyframe with offset from screen center
            newKeyframe = new AnimationKeyframe(
                    currentPlayOrRecordPosition,
                    elementCenterX - screenCenterX,  // Store offset X from screen center
                    elementCenterY - screenCenterY,  // Store offset Y from screen center
                    previewElement.baseWidth,
                    previewElement.baseHeight,
                    ElementAnchorPoints.MID_CENTERED,
                    true
            );
        } else {
            // Create keyframe with absolute position
            newKeyframe = new AnimationKeyframe(
                    currentPlayOrRecordPosition,
                    previewElement.posOffsetX,
                    previewElement.posOffsetY,
                    previewElement.baseWidth,
                    previewElement.baseHeight,
                    previewElement.anchorPoint,
                    previewElement.stickyAnchor
            );
        }

        workingKeyframes.add(newKeyframe);

        this.displayNotification(KEYFRAME_ADDED_TEXT, 2000);

        // Sort keyframes by timestamp
        workingKeyframes.sort(Comparator.comparingLong(k -> k.timestamp));

        updateTimelineDurationToMaxTimestamp();

    }

    protected void deleteSelectedKeyframes() {
        if (!this.selectedKeyframes.isEmpty()) {
            saveState();
            new ArrayList<>(this.selectedKeyframes).forEach(selectedKeyframe -> {
                workingKeyframes.remove(selectedKeyframe);
                this.selectKeyframeClearOldSelection(null);
                updateTimelineDurationToMaxTimestamp();
                this.displayNotification(KEYFRAME_DELETED_TEXT, 2000);
            });
        }
    }

    protected void saveState() {
        // Push current state to undo stack
        undoStack.push(new ArrayList<>(workingKeyframes.stream()
                .map(AnimationKeyframe::clone)
                .toList()));
        // Clear redo stack since we're creating a new branch of history
        redoStack.clear();
    }

    protected void undo() {
        List<String> selected = new ArrayList<>();
        this.selectedKeyframes.forEach(keyframe -> selected.add(keyframe.uniqueIdentifier));
        if (this.isPlaying) return;
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack before undoing
            redoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from undo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(undoStack.pop());
            this.selectKeyframeClearOldSelection(null);
            updateTimelineDurationToMaxTimestamp();
        }
        if (!selected.isEmpty() && !this.isRecording) {
            selected.forEach(s -> {
                AnimationKeyframe frame = null;
                for (AnimationKeyframe f : this.workingKeyframes) {
                    if (f.uniqueIdentifier.equals(s)) {
                        frame = f;
                        break;
                    }
                }
                this.selectKeyframe(frame, true);
            });
        }
    }

    protected void redo() {
        List<String> selected = new ArrayList<>();
        this.selectedKeyframes.forEach(keyframe -> selected.add(keyframe.uniqueIdentifier));
        if (this.isPlaying) return;
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack before redoing
            undoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from redo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(redoStack.pop());
            this.selectKeyframeClearOldSelection(null);
            updateTimelineDurationToMaxTimestamp();
        }
        if (!selected.isEmpty() && !this.isRecording) {
            selected.forEach(s -> {
                AnimationKeyframe frame = null;
                for (AnimationKeyframe f : this.workingKeyframes) {
                    if (f.uniqueIdentifier.equals(s)) {
                        frame = f;
                        break;
                    }
                }
                this.selectKeyframe(frame, true);
            });
        }
    }

    protected void selectKeyframe(@Nullable AnimationKeyframe selected, boolean addToSelection) {

        if (!addToSelection) {
            // Clear previous selection if not adding to it
            selectedKeyframes.clear();
        }

        if (selected != null) {

            if (!this.selectedKeyframes.contains(selected)) {

                // Add new keyframe to selection
                selectedKeyframes.add(selected);

                if (selectedKeyframes.size() == 1) {
                    // Set preview to clicked keyframe to show it
                    this.applyKeyframeValuesToElement(selected, previewElement);
                    previewEditorElement.setSelected(true);

                    // Pause when selecting a keyframe
                    if (this.isPlaying) this.togglePlayback();

                    // Update buttons to match selected keyframe
                    this.anchorButton.setSelectedValue(selected.anchorPoint);
                    this.stickyButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(selected.stickyAnchor));

                } else {

                    if (!this.isRecording) this.previewEditorElement.setSelected(false);

                }

            }

        } else {

            selectedKeyframes.clear();
            if (!this.isRecording) this.previewEditorElement.setSelected(false);

        }

    }

    protected void selectKeyframeClearOldSelection(@Nullable AnimationKeyframe keyframe) {
        selectKeyframe(keyframe, false);
    }

    protected void toggleSmoothingInput() {
        this.lastSmoothingInputValue = this.smoothingDistanceInput.getValue();
        this.isShowingSmoothingInput = !this.isShowingSmoothingInput;
        if (this.isShowingSmoothingInput) {
            this.smoothingDistanceInput.setValue(this.lastSmoothingInputValue);
            if (this.smoothingDistanceInput.getValue().isBlank()) {
                this.smoothingDistanceInput.setValue("100"); //Default value
            }
        }
    }

    protected void toggleTimestampInput() {
        if (this.selectedKeyframes.size() != 1) return;
        AnimationKeyframe selected = this.selectedKeyframes.get(0);
        this.isShowingTimestampInput = !this.isShowingTimestampInput;
        if (this.isShowingTimestampInput) {
            this.timestampInput.setValue("" + selected.timestamp);
        }
    }

    protected void applySmoothingDistance() {
        if (this.selectedKeyframes.size() > 1) {
            String value = this.smoothingDistanceInput.getValue();
            if (MathUtils.isLong(value) && !value.isEmpty()) {
                try {
                    long distanceMs = Long.parseLong(value);
                    if (distanceMs > 0) {
                        saveState();

                        // Sort selected keyframes by timestamp
                        List<AnimationKeyframe> sortedFrames = new ArrayList<>(this.selectedKeyframes);
                        sortedFrames.sort(Comparator.comparingLong(k -> k.timestamp));

                        // Keep first frame's position, space others evenly
                        long startTime = sortedFrames.get(0).timestamp;
                        for (int i = 1; i < sortedFrames.size(); i++) {
                            sortedFrames.get(i).timestamp = startTime + (distanceMs * i);
                        }

                        updateTimelineDurationToMaxTimestamp();
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        // Hide input after applying
        this.isShowingSmoothingInput = false;
    }

    protected boolean isInTimelineArea(int mouseX, int mouseY) {
        return mouseX >= timelineX &&
                mouseX <= timelineX + timelineWidth &&
                mouseY >= timelineY &&
                mouseY <= timelineY + TIMELINE_HEIGHT;
    }

    protected boolean isOverProgressLine(int mouseX, int mouseY) {
        long maxTime = timelineDuration;
        float progress = (float) currentPlayOrRecordPosition / maxTime;
        int progressX = timelineX + (int)(timelineWidth * progress);

        return mouseY >= timelineY && mouseY <= timelineY + TIMELINE_HEIGHT &&
                mouseX >= progressX - 5 && mouseX <= progressX + 5;
    }

    protected int getKeyframeIndexAtPosition(int mouseX, int mouseY) {

        if ((mouseY < timelineY) || (mouseY > timelineY + TIMELINE_HEIGHT)) {
            return -1;
        }

        for (int i = 0; i < workingKeyframes.size(); i++) {
            AnimationKeyframe keyframe = workingKeyframes.get(i);
            float progress = (float)keyframe.timestamp / (float)timelineDuration;
            int lineX = (int)((float)timelineX + ((float)timelineWidth * progress));
            int halfLineWidth = KEYFRAME_LINE_WIDTH / 2;
            if ((mouseX >= (lineX - halfLineWidth)) && (mouseX <= (lineX + halfLineWidth))) {
                return i;
            }
        }

        return -1;

    }

    protected void setAnchorPoint(ElementAnchorPoint newAnchor) {
        previewElement.anchorPoint = newAnchor;
        previewElement.posOffsetX = 0;
        previewElement.posOffsetY = 0;
        // Get element bounds
        int startX = previewElement.getAbsoluteX();
        int startY = previewElement.getAbsoluteY();
        int endX = startX + previewElement.getAbsoluteWidth();
        int endY = startY + previewElement.getAbsoluteHeight();
        // Check if element is partially or fully outside screen bounds
        if (startX < 0 || startY < 0 || endX > this.width || endY > this.height) {
            // Adjust position to ensure element stays within screen bounds
            if (startX < 0) {
                previewElement.posOffsetX = -startX;
            } else if (endX > this.width) {
                previewElement.posOffsetX = this.width - endX;
            }
            if (startY < 0) {
                previewElement.posOffsetY = -startY;
            } else if (endY > this.height) {
                previewElement.posOffsetY = this.height - endY;
            }
        }
        if (this.selectedKeyframes.size() == 1) {
            saveState();
            AnimationKeyframe selectedKeyframe = this.selectedKeyframes.getFirst();
            selectedKeyframe.anchorPoint = previewElement.anchorPoint;
            selectedKeyframe.posOffsetX = previewElement.posOffsetX;
            selectedKeyframe.posOffsetY = previewElement.posOffsetY;
        }
    }

    protected void setStickyAnchor(boolean sticky) {
        if (this.selectedKeyframes.size() == 1) {
            saveState();
            this.selectedKeyframes.getFirst().stickyAnchor = sticky;
        }
        previewElement.stickyAnchor = sticky;
    }

    protected float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void displayNotification(@NotNull Component message, long durationMs) {
        activeNotifications.add(new Notification(message, durationMs));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    protected static class PreviewElement extends AbstractElement {

        public PreviewElement(ElementBuilder<?, ?> builder) {
            super(builder);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

    }

    protected class PreviewEditorElement extends AbstractEditorElement {

        protected boolean elementMovingStarted = false;
        protected boolean resizingStarted = false;

        public PreviewEditorElement(@NotNull AbstractElement element, @NotNull LayoutEditorScreen editor) {
            super(element, editor);
            // Only allow resizing, disable all other editor features
            this.settings.setFadeable(false);
            this.settings.setAdvancedSizingSupported(false);
            this.settings.setAdvancedPositioningSupported(false);
            this.settings.setOpacityChangeable(false);
            this.settings.setDelayable(false);
            this.settings.setElementAnchorPointAllowed(false);
            this.settings.setStretchable(false);
            this.settings.setVanillaAnchorPointAllowed(false);
            this.settings.setOrderable(false);
            this.settings.setCopyable(false);
            this.settings.setDestroyable(false);
            this.settings.setIdentifierCopyable(false);
        }

        @Override
        public void init() {

            super.init();

            //Don't show border info for preview element
            this.topLeftDisplay.clearLines();
            this.bottomRightDisplay.clearLines();

        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            // Render preview rectangle
            this.renderPreviewBody(graphics, mouseX, mouseY, partial);
            // Render resize border and grabbers
            super.render(graphics, mouseX, mouseY, partial);
        }

        public void renderPreviewBody(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            DrawableColor c = PREVIEW_COLOR_NORMAL;
            if (KeyframeManagerScreen.this.isRecording) c = RECORDING_COLOR;
            if (KeyframeManagerScreen.this.selectedKeyframes.size() == 1) c = KEYFRAME_COLOR_SELECTED;
            graphics.fill(RenderType.gui(),
                    this.element.getAbsoluteX(),
                    this.element.getAbsoluteY(),
                    this.element.getAbsoluteX() + this.element.getAbsoluteWidth(),
                    this.element.getAbsoluteY() + this.element.getAbsoluteHeight(),
                    c.getColorInt());
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            int draggingDiffX = (int) (mouseX - this.leftMouseDownMouseX);
            int draggingDiffY = (int) (mouseY - this.leftMouseDownMouseY);
            this.movingCrumpleZonePassed = (Math.abs(draggingDiffX) >= ELEMENT_DRAG_CRUMPLE_ZONE) || (Math.abs(draggingDiffY) >= ELEMENT_DRAG_CRUMPLE_ZONE);
            if (this.movingCrumpleZonePassed && !this.elementMovingStarted) {
                this.updateMovingStartPos((int)mouseX, (int)mouseY);
                this.elementMovingStarted = true;
            }
            if (!this.resizingStarted) {
                this.updateResizingStartPos((int)mouseX, (int)mouseY);
                this.resizingStarted = true;
            }
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            this.movingCrumpleZonePassed = false;
            this.elementMovingStarted = false;
            this.resizingStarted = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

    }

    protected static class Notification {

        @NotNull
        public final Component message;
        public final long startTime;
        public final long duration;
        public float opacity = 1.0F;

        public Notification(@NotNull Component message, long duration) {
            this.message = message;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }

        public void updateOpacity() {
            long elapsedTime = System.currentTimeMillis() - startTime;
            float fadeStartTime = duration - 500; // Start fading 500ms before expiring

            if (elapsedTime > fadeStartTime) {
                opacity = 1.0F - ((elapsedTime - fadeStartTime) / 500F);
                opacity = Math.max(0.05F, Math.min(1.0F, opacity));
            }
        }

        public int getHeight() {
            return Minecraft.getInstance().font.lineHeight + 2; // Line height plus small padding
        }

    }

    public static record AnimationControllerMetadata(@NotNull List<AnimationKeyframe> keyframes, boolean isOffsetMode) {
    }

}