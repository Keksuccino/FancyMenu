package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
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

public class KeyframeManagerScreenORI extends Screen {

    //TODO FIXEN: Speed slider kann nicht gezogen/gedraggt werden

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
    protected final Consumer<List<AnimationKeyframe>> resultCallback;
    protected final List<AnimationKeyframe> workingKeyframes;
    protected final PreviewElement previewElement;
    protected final PreviewEditorElement previewEditorElement;
    protected boolean isDraggingProgress = false;
    protected boolean isPlaying = false;
    protected long playStartTime = -1;
    protected long currentPlayOrRecordPosition = 0;
    protected AnimationKeyframe selectedKeyframe = null;
    protected int draggingKeyframeIndex = -1;
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

    public KeyframeManagerScreenORI(AnimationControllerElement controller, Consumer<List<AnimationKeyframe>> resultCallback) {
        super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager"));
        this.controller = controller;
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
                button -> this.resultCallback.accept(this.workingKeyframes)));
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
        addKeyframeButton.setIsActiveSupplier(consumes -> this.isRecording && (this.selectedKeyframe == null));
        this.addBottomWidget(1, 0, addKeyframeButton);

        // Delete keyframe button
        this.deleteKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(0, 0, buttonBaseWidth, 0,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.delete_keyframe"),
                button -> deleteSelectedKeyframe()));
        this.deleteKeyframeButton.setIsActiveSupplier(consumes -> (this.selectedKeyframe != null));
        this.addBottomWidget(1, 0, this.deleteKeyframeButton);

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
        this.anchorButton.setIsActiveSupplier(consumes -> (this.selectedKeyframe != null) || this.isRecording);
        this.addBottomWidget(2, 0, this.anchorButton);

        // Sticky anchor toggle
        this.stickyButton = new CycleButton<>(0, 0, buttonBaseWidth + 65, 0,
                CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.sticky"),
                (value, button) -> this.setStickyAnchor(value.getAsBoolean()));
        this.stickyButton.setIsActiveSupplier(consumes -> (this.selectedKeyframe != null) || this.isRecording);
        this.addBottomWidget(2, 0, this.stickyButton);

        // ---------------------------------------------------------

        if (!isPlaying && !isRecording) {
            updateTimelineDurationToMaxTimestamp();
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

        this.renderTimelineBackground(graphics, actualEndTime);

        this.renderKeyframeLines(graphics, mouseX, mouseY, partial, actualEndTime);

        this.renderProgressLine(graphics, mouseX, mouseY, partial, actualEndTime);

        this.tickAnimation(); // It's important that this gets called before renderPreview()

        this.renderPreview(graphics, mouseX, mouseY, partial);

        this.renderTimeText(graphics, actualEndTime);

        this.renderKeyframeInfo(graphics, mouseX, mouseY, partial);

        this.renderRecordingIndicator(graphics, mouseX, mouseY, partial);

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
            this.setSelectedKeyframe(null);
            float progress = (float)(currentPlayOrRecordPosition - currentFrame.timestamp) / (float)(nextFrame.timestamp - currentFrame.timestamp);
            previewElement.posOffsetX = (int)lerp(currentFrame.posOffsetX, nextFrame.posOffsetX, progress);
            previewElement.posOffsetY = (int)lerp(currentFrame.posOffsetY, nextFrame.posOffsetY, progress);
            previewElement.baseWidth = (int)lerp(currentFrame.baseWidth, nextFrame.baseWidth, progress);
            previewElement.baseHeight = (int)lerp(currentFrame.baseHeight, nextFrame.baseHeight, progress);
            previewElement.anchorPoint = nextFrame.anchorPoint;
            previewElement.stickyAnchor = nextFrame.stickyAnchor;
        }

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
            DrawableColor color = (keyframe == selectedKeyframe) ?
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
                        continue; // Skip movement until we've moved past threshold
                    }
                    //Save state right before keyframe moving starts
                    saveState();
                }
                // Calculate mouse position relative to timeline
                float newProgress = (float)(mouseX - timelineX) / timelineWidth;
                long newDuration = this.timelineDuration;
                // If dragging past timeline end, extend timeline and set keyframe timestamp
                if (!this.isRecording && (mouseX > (timelineX + timelineWidth - 10))) {
                    newDuration = timelineDuration + TIMELINE_EXTENSION_STEP;
                    // Calculate new progress based on extended duration
                    newProgress = (float)(mouseX - timelineX) / timelineWidth;
                }
                // Clamp progress to valid range (0-1)
                newProgress = Math.max(0, Math.min(1, newProgress));
                // Set keyframe timestamp based on progress
                keyframe.timestamp = (long)(newDuration * newProgress);
                if (this.isRecording && (keyframe.timestamp > actualDuration)) {
                    keyframe.timestamp = actualDuration;
                }
                // Update timeline duration to ensure padding
                updateTimelineDurationToMaxTimestamp();
            }

        }

    }

    protected void renderPreview(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        // Render preview
        if (!isPlaying && ((selectedKeyframe != null) || isRecording)) {
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

        if (selectedKeyframe != null) {

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

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.previewEditorElement.mouseClicked(mouseX, mouseY, button)) return true;

        // Handle clicking progress line
        if (isOverProgressLine((int)mouseX, (int)mouseY) && (!this.isRecording || this.isRecordingPaused)) {
            isDraggingProgress = true;
            return true;
        }
        if (isOverProgressLine((int)mouseX, (int)mouseY) && this.isRecording && !this.isRecordingPaused) {
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_drag_progress").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), 6000);
            return true;
        }

        // Handle clicking keyframe lines
        int clickedIndex = getKeyframeIndexAtPosition((int)mouseX, (int)mouseY);
        if (this.isRecording && !this.isRecordingPaused && (clickedIndex >= 0)) {
            clickedIndex = -1;
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_edit_keyframe").setStyle(Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())), 6000);
        }
        if (clickedIndex >= 0) {
            initialDragClickX = (int)mouseX;
            hasMovedFromClickPosition = false;
            draggingKeyframeIndex = clickedIndex;
            this.setSelectedKeyframe(workingKeyframes.get(clickedIndex));
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
        if (this.previewEditorElement.isSelected() && (previewGotResized || previewGotMoved) && (this.selectedKeyframe != null) && (!this.isRecording || this.isRecordingPaused) && !this.isPlaying) {
            saveState();
            this.applyElementValuesToKeyframe(this.previewElement, this.selectedKeyframe);
        }
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

        // Handle arrow keys for selected keyframe
        if (selectedKeyframe != null) {
            if (keyCode == KEY_MOVE_KEYFRAME_LEFT || keyCode == KEY_MOVE_KEYFRAME_RIGHT) {
                saveState(); // Save state before modifying timestamp
                if (keyCode == KEY_MOVE_KEYFRAME_LEFT) {
                    selectedKeyframe.timestamp = Math.max(0, selectedKeyframe.timestamp - 100);
                } else { // RIGHT
                    selectedKeyframe.timestamp = Math.min(timelineDuration, selectedKeyframe.timestamp + 100);
                }
                updateTimelineDurationToMaxTimestamp();
                return true;
            }
        }

        // Handle DELETE key
        if (keyCode == KEY_DELETE_KEYFRAME) {
            if (selectedKeyframe != null) {
                this.deleteSelectedKeyframe();
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
            this.togglePauseRecording();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.previewEditorElement.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    protected void applyElementValuesToKeyframe(@NotNull PreviewElement element, @NotNull AnimationKeyframe keyframe) {
        keyframe.posOffsetX = element.posOffsetX;
        keyframe.posOffsetY = element.posOffsetY;
        keyframe.baseWidth = element.baseWidth;
        keyframe.baseHeight = element.baseHeight;
        keyframe.anchorPoint = element.anchorPoint;
        keyframe.stickyAnchor = element.stickyAnchor;
    }

    protected void applyKeyframeValuesToElement(@NotNull AnimationKeyframe keyframe, @NotNull PreviewElement element) {
        element.posOffsetX = keyframe.posOffsetX;
        element.posOffsetY = keyframe.posOffsetY;
        element.baseWidth = keyframe.baseWidth;
        element.baseHeight = keyframe.baseHeight;
        element.anchorPoint = keyframe.anchorPoint;
        element.stickyAnchor = keyframe.stickyAnchor;
    }

    protected void togglePlayback() {
        if (isRecording) return;
        isPlaying = !isPlaying;
        if (isPlaying) {
            playStartTime = System.currentTimeMillis() - currentPlayOrRecordPosition;
            this.setSelectedKeyframe(null);
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
        this.setSelectedKeyframe(null);
        draggingKeyframeIndex = -1;
        previewEditorElement.setSelected(true);
    }

    protected void stopRecording() {
        isRecording = false;
        isRecordingPaused = false;
        recordStartTime = -1;
        this.setSelectedKeyframe(null);
        // Reset play position to start
        currentPlayOrRecordPosition = 0;
        // Reset timeline duration if needed
        updateTimelineDurationToMaxTimestamp();
    }

    protected void togglePauseRecording() {
        if (!this.isRecording) return;
        if (!this.isRecordingPaused) {
            this.cachedRecordingSpeed = this.recordingSpeed;
            this.setRecordingSpeed(0.0D);
        } else {
            this.setRecordingSpeed(this.cachedRecordingSpeed);
        }
        if (this.recordingSpeedSlider != null) {
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
                this.setSelectedKeyframe(null);
                this.isRecordingPaused = false;
            } else {
                this.isRecordingPaused = true;
            }
        }
        this.recordingSpeed = newSpeed;
    }

    protected void addKeyframeAtProgress() {

        if (!isRecording) return;

        saveState();

        AnimationKeyframe newKeyframe = new AnimationKeyframe(
                currentPlayOrRecordPosition,
                previewElement.posOffsetX,
                previewElement.posOffsetY,
                previewElement.baseWidth,
                previewElement.baseHeight,
                previewElement.anchorPoint,
                previewElement.stickyAnchor
        );

        workingKeyframes.add(newKeyframe);

        this.displayNotification(KEYFRAME_ADDED_TEXT, 2000);

        // Sort keyframes by timestamp
        workingKeyframes.sort(Comparator.comparingLong(k -> k.timestamp));

        updateTimelineDurationToMaxTimestamp();

    }

    protected void deleteSelectedKeyframe() {
        if (selectedKeyframe != null) {
            saveState();
            workingKeyframes.remove(selectedKeyframe);
            this.setSelectedKeyframe(null);
            updateTimelineDurationToMaxTimestamp();
            this.displayNotification(KEYFRAME_DELETED_TEXT, 2000);
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
        String selected = (this.selectedKeyframe != null) ? this.selectedKeyframe.uniqueIdentifier : null;
        if (this.isPlaying) return;
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack before undoing
            redoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from undo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(undoStack.pop());
            this.setSelectedKeyframe(null);
            updateTimelineDurationToMaxTimestamp();
        }
        if ((selected != null) && !this.isRecording) {
            AnimationKeyframe frame = null;
            for (AnimationKeyframe f : this.workingKeyframes) {
                if (f.uniqueIdentifier.equals(selected)) {
                    frame = f;
                    break;
                }
            }
            this.setSelectedKeyframe(frame);
        }
    }

    protected void redo() {
        String selected = (this.selectedKeyframe != null) ? this.selectedKeyframe.uniqueIdentifier : null;
        if (this.isPlaying) return;
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack before redoing
            undoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from redo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(redoStack.pop());
            this.setSelectedKeyframe(null);
            updateTimelineDurationToMaxTimestamp();
        }
        if ((selected != null) && !this.isRecording) {
            AnimationKeyframe frame = null;
            for (AnimationKeyframe f : this.workingKeyframes) {
                if (f.uniqueIdentifier.equals(selected)) {
                    frame = f;
                    break;
                }
            }
            this.setSelectedKeyframe(frame);
        }
    }

    protected void setSelectedKeyframe(@Nullable AnimationKeyframe selected) {
        this.selectedKeyframe = selected;
        if (this.selectedKeyframe != null) {
            // Set preview to clicked keyframe to show it
            this.applyKeyframeValuesToElement(selectedKeyframe, previewElement);
            previewEditorElement.setSelected(true);
            // Pause when selecting a keyframe
            if (this.isPlaying) this.togglePlayback();
            // Update buttons to match selected keyframe
            this.anchorButton.setSelectedValue(selectedKeyframe.anchorPoint);
            this.stickyButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(selectedKeyframe.stickyAnchor));
        } else {
            if (!this.isRecording) this.previewEditorElement.setSelected(false);
        }
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
        if (selectedKeyframe != null) {
            saveState();
            selectedKeyframe.anchorPoint = previewElement.anchorPoint;
            selectedKeyframe.posOffsetX = previewElement.posOffsetX;
            selectedKeyframe.posOffsetY = previewElement.posOffsetY;
        }
    }

    protected void setStickyAnchor(boolean sticky) {
        if (selectedKeyframe != null) {
            saveState();
            selectedKeyframe.stickyAnchor = sticky;
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
            if (KeyframeManagerScreenORI.this.isRecording) c = RECORDING_COLOR;
            if (KeyframeManagerScreenORI.this.selectedKeyframe != null) c = KEYFRAME_COLOR_SELECTED;
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

}