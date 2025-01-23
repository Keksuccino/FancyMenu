package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

import static de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen.ELEMENT_DRAG_CRUMPLE_ZONE;

public class KeyframeManagerScreenNewCOPY extends Screen {

    protected static final int KEY_MOVE_KEYFRAME_LEFT = InputConstants.KEY_LEFT;
    protected static final int KEY_MOVE_KEYFRAME_RIGHT = InputConstants.KEY_RIGHT;
    protected static final int KEY_DELETE_KEYFRAME = InputConstants.KEY_DELETE;

    protected static final DrawableColor TIMELINE_COLOR = DrawableColor.of(new Color(0, 122, 204));
    protected static final DrawableColor KEYFRAME_COLOR = DrawableColor.of(new Color(255, 255, 255));
    protected static final DrawableColor KEYFRAME_COLOR_SELECTED = DrawableColor.of(new Color(255, 255, 0));
    protected static final DrawableColor PROGRESS_COLOR = DrawableColor.of(new Color(255, 0, 0));
    protected static final DrawableColor PREVIEW_COLOR = DrawableColor.of(new Color(0, 255, 0));
    protected static final DrawableColor TIMELINE_PADDING_COLOR = DrawableColor.of(new Color(255, 140, 0));

    protected static final int TIMELINE_HEIGHT = 50;
    protected static final int TIMELINE_Y_PADDING = 20;
    protected static final int KEYFRAME_LINE_WIDTH = 2;
    protected static final int KEYFRAME_LINE_HEIGHT = 30;
    protected static final int PROGRESS_LINE_WIDTH = 2;
    protected static final int MIN_TIMELINE_DURATION = 5000; // 5 seconds minimum
    protected static final int TIMELINE_EXTENSION_STEP = 2000; // Extend by 2 seconds
    protected static final long TIMELINE_PADDING_DURATION = 2000; // 2 seconds padding
    private static final int KEYFRAME_DRAG_CRUMPLE_ZONE = 3; // Pixels threshold before movement starts

    protected final AnimationControllerElement controller;
    protected final Consumer<List<AnimationKeyframe>> resultCallback;
    protected final List<AnimationKeyframe> workingKeyframes;
    protected final PreviewElement previewElement;
    protected final PreviewEditorElement previewEditorElement;

    protected boolean isDraggingProgress = false;
    protected boolean isPlaying = false;
    protected long playStartTime = -1;
    protected long currentPlayPosition = 0;
    protected AnimationKeyframe selectedKeyframe = null;
    protected int draggingKeyframeIndex = -1;
    protected int timelineX;
    protected int timelineWidth;
    protected int timelineY;
    protected long timelineDuration = MIN_TIMELINE_DURATION;
    private int initialDragClickX = 0;
    private boolean hasMovedFromClickPosition = false;

    protected final Stack<List<AnimationKeyframe>> undoStack = new Stack<>();
    protected final Stack<List<AnimationKeyframe>> redoStack = new Stack<>();

    protected CycleButton<ElementAnchorPoint> anchorButton;
    protected CycleButton<CommonCycles.CycleEnabledDisabled> stickyButton;
    protected ExtendedButton undoButton;
    protected ExtendedButton redoButton;
    protected ExtendedButton deleteKeyframeButton;
    protected ExtendedButton playButton;

    //TODO shortcut for adding keyframe

    //TODO shortcut for toggling sticky

    //TODO shortcut for cycling anchor

    //TODO shortcut to toggle the visibility of ALL ui elements except the preview element, so it is possible to resize/move it without ui elements getting into the way

    //TODO recording mode, where the preview element is selected without a selected keyframe, so it can be freely moved/resized/anchors changed/sticky toggled
    // - Adding a keyframe while in this mode will save the current state of the preview to the new keyframe
    // - In this mode, the play progress line will move like when it's playing, but when it reaches the current end of the timeline, the timeline will grow and snap back to the last keyframe when recording gets stopped
    // - Add button for "Start Recording", which will switch its label to "Stop Recording" while recording via labelSupplier

    //TODO shortcut for start/stop recording

    //TODO localize time display

    //TODO localize selected keyframe info

    public KeyframeManagerScreenNewCOPY(AnimationControllerElement controller, Consumer<List<AnimationKeyframe>> resultCallback) {
        super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager"));
        this.controller = controller;
        this.resultCallback = resultCallback;
        this.workingKeyframes = new ArrayList<>(controller.getKeyframes());

        // Set initial timeline duration based on last keyframe
        for (AnimationKeyframe kf : workingKeyframes) {
            timelineDuration = Math.max(timelineDuration, kf.timestamp + 2000);
        }

        // Create dummy element for preview
        this.previewElement = new PreviewElement(controller.builder);
        this.previewElement.baseWidth = 50;
        this.previewElement.baseHeight = 50;

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

        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonSpacing = 10;
        int currentX = timelineX;

        // Play button
        this.playButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(currentX, timelineY - 25, buttonWidth, buttonHeight,
                Component.empty(),
                button -> togglePlayback()));
        this.playButton.setLabelSupplier(consumes -> Component.translatable(isPlaying ? "fancymenu.elements.animation_controller.keyframe_manager.pause" : "fancymenu.elements.animation_controller.keyframe_manager.play"));
        this.addRenderableWidget(playButton);
        currentX += buttonWidth + buttonSpacing;

        // Add keyframe button
        ExtendedButton addKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(currentX, timelineY - 25, buttonWidth + 25, buttonHeight,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.add_keyframe"),
                button -> addKeyframeAtProgress()));
        this.addRenderableWidget(addKeyframeButton);
        currentX += buttonWidth + 25 + buttonSpacing;

        // Delete keyframe button
        this.deleteKeyframeButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(currentX, timelineY - 25, buttonWidth + 35, buttonHeight,
                Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.delete_keyframe"),
                button -> deleteSelectedKeyframe()));
        this.addRenderableWidget(deleteKeyframeButton);
        this.deleteKeyframeButton.active = false;
        currentX += buttonWidth + 35 + buttonSpacing;

        // Anchor point cycle button
        List<ElementAnchorPoint> anchorPoints = ElementAnchorPoints.getAnchorPoints();
        anchorPoints.remove(ElementAnchorPoints.ELEMENT);
        anchorPoints.remove(ElementAnchorPoints.VANILLA);
        this.anchorButton = new CycleButton<>(currentX, timelineY - 25, buttonWidth + 105, buttonHeight,
                CommonCycles.cycle("fancymenu.elements.animation_controller.keyframe_manager.anchor_point_cycle", anchorPoints, ElementAnchorPoints.TOP_LEFT)
                        .setValueNameSupplier(ElementAnchorPoint::getName)
                        .setValueComponentStyleSupplier(consumes -> Style.EMPTY.withColor(UIBase.getUIColorTheme().warning_text_color.getColorInt())),
                (value, button) -> {
                    this.setAnchorPoint(value);
                });
        this.anchorButton.active = false;
        this.addRenderableWidget(UIBase.applyDefaultWidgetSkinTo(this.anchorButton));
        currentX += buttonWidth + 105 + buttonSpacing;

        // Sticky anchor toggle
        this.stickyButton = new CycleButton<>(currentX, timelineY - 25, buttonWidth + 65, buttonHeight,
                CommonCycles.cycleEnabledDisabled("fancymenu.elements.animation_controller.keyframe_manager.sticky"),
                (value, button) -> {
                    this.setStickyAnchor(value.getAsBoolean());
                });
        this.stickyButton.active = false;
        this.addRenderableWidget(UIBase.applyDefaultWidgetSkinTo(this.stickyButton));

        // Undo button
        this.undoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(timelineX, timelineY - 25 - 25, buttonWidth, buttonHeight,
                Component.translatable("fancymenu.editor.edit.undo"),
                button -> undo()));
        this.addRenderableWidget(undoButton);

        // Redo button
        this.redoButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(timelineX + undoButton.getWidth() + buttonSpacing, timelineY - 25 - 25, buttonWidth, buttonHeight,
                Component.translatable("fancymenu.editor.edit.redo"),
                button -> redo()));
        this.addRenderableWidget(redoButton);

        // Cancel button
        ExtendedButton cancelButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(10, 10, 60, 20,
                Component.translatable("gui.cancel"),
                button -> this.resultCallback.accept(null)));
        this.addRenderableWidget(cancelButton);

        // Done button
        ExtendedButton doneButton = UIBase.applyDefaultWidgetSkinTo(new ExtendedButton(80, 10, 60, 20,
                Component.translatable("gui.done"),
                button -> this.resultCallback.accept(this.workingKeyframes)));
        this.addRenderableWidget(doneButton);

        updateTimelineDuration();

        updateUndoRedoButtons();

    }

    protected void setAnchorPoint(ElementAnchorPoint newAnchor) {
        if (selectedKeyframe != null) {
            saveState();
            selectedKeyframe.anchorPoint = newAnchor;
            previewElement.anchorPoint = newAnchor;
        }
    }

    protected void setStickyAnchor(boolean sticky) {
        if (selectedKeyframe != null) {
            saveState();
            selectedKeyframe.stickyAnchor = sticky;
            previewElement.stickyAnchor = sticky;
        }
    }

    protected void addKeyframeAtProgress() {
        if (isPlaying) {
            togglePlayback(); // Force pause
        }

        saveState();

        AnimationKeyframe newKeyframe = new AnimationKeyframe(
                currentPlayPosition,
                previewElement.posOffsetX,
                previewElement.posOffsetY,
                previewElement.baseWidth,
                previewElement.baseHeight,
                previewElement.anchorPoint,
                previewElement.stickyAnchor
        );

        workingKeyframes.add(newKeyframe);
        selectedKeyframe = newKeyframe;

        // Sort keyframes by timestamp
        workingKeyframes.sort(Comparator.comparingLong(k -> k.timestamp));

        updateTimelineDuration();

    }

    protected void deleteSelectedKeyframe() {
        if (selectedKeyframe != null) {
            saveState();
            workingKeyframes.remove(selectedKeyframe);
            selectedKeyframe = null;
            previewEditorElement.setSelected(false);
            updateTimelineDuration();
        }
    }

    protected void saveState() {
        // Push current state to undo stack
        undoStack.push(new ArrayList<>(workingKeyframes.stream()
                .map(kf -> new AnimationKeyframe(
                        kf.timestamp,
                        kf.posOffsetX,
                        kf.posOffsetY,
                        kf.baseWidth,
                        kf.baseHeight,
                        kf.anchorPoint,
                        kf.stickyAnchor
                ))
                .toList()));
        // Clear redo stack since we're creating a new branch of history
        redoStack.clear();
        // Update button states
        updateUndoRedoButtons();
    }

    protected void undo() {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack before undoing
            redoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from undo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(undoStack.pop());
            selectedKeyframe = null;
            previewEditorElement.setSelected(false);
            // Update buttons
            updateUndoRedoButtons();
        }
    }

    // Update redo method
    protected void redo() {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack before redoing
            undoStack.push(new ArrayList<>(workingKeyframes));
            // Pop and apply state from redo stack
            workingKeyframes.clear();
            workingKeyframes.addAll(redoStack.pop());
            selectedKeyframe = null;
            previewEditorElement.setSelected(false);
            // Update buttons
            updateUndoRedoButtons();
        }
    }

    protected void updateUndoRedoButtons() {
        if (this.undoButton != null && this.redoButton != null) {
            this.undoButton.active = !undoStack.isEmpty();
            this.redoButton.active = !redoStack.isEmpty();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        //Enable/Disable keyframe-related buttons depending on if a keyframe is selected
        if (this.selectedKeyframe != null) {
            this.stickyButton.active = true;
            this.deleteKeyframeButton.active = true;
            this.anchorButton.active = true;
        } else {
            this.stickyButton.active = false;
            this.deleteKeyframeButton.active = false;
            this.anchorButton.active = false;
        }

        graphics.fill(0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        long actualEndTime = 0;
        for (AnimationKeyframe kf : workingKeyframes) {
            actualEndTime = Math.max(actualEndTime, kf.timestamp);
        }

        // Draw main timeline background
        graphics.fill(RenderType.gui(),
                timelineX, timelineY,
                timelineX + (int)((float)actualEndTime / timelineDuration * timelineWidth),
                timelineY + TIMELINE_HEIGHT,
                TIMELINE_COLOR.getColorInt());

        // Draw padding area in different color
        int paddingStartX = timelineX + (int)((float)actualEndTime / timelineDuration * timelineWidth);
        graphics.fill(RenderType.gui(),
                paddingStartX, timelineY,
                timelineX + timelineWidth,
                timelineY + TIMELINE_HEIGHT,
                TIMELINE_PADDING_COLOR.getColorInt());

        // Update play position
        if (isPlaying) {
            long elapsedTime = System.currentTimeMillis() - playStartTime;
            currentPlayPosition = currentPlayPosition + elapsedTime;
            playStartTime = System.currentTimeMillis();

            // Stop if reached end
            if (currentPlayPosition > timelineDuration) {
                isPlaying = false;
                currentPlayPosition = 0;
            }
        }

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
                int dragDeltaX = (int)mouseX - initialDragClickX;
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
                // If dragging past timeline end, extend timeline and set keyframe timestamp
                if (mouseX > timelineX + timelineWidth - 10) {
                    timelineDuration += TIMELINE_EXTENSION_STEP;
                    // Calculate new progress based on extended duration
                    newProgress = (float)(mouseX - timelineX) / timelineWidth;
                }
                // Clamp progress to valid range (0-1)
                newProgress = Math.max(0, Math.min(1, newProgress));
                // Set keyframe timestamp based on progress
                keyframe.timestamp = (long)(timelineDuration * newProgress);
                // Update timeline duration to ensure padding
                updateTimelineDuration();
            }

        }

        // Draw progress line
        float playProgress = (float) currentPlayPosition / timelineDuration;
        int progressX = timelineX + (int)(timelineWidth * playProgress);

        if (isDraggingProgress) {
            float newProgress = (float)(mouseX - timelineX) / timelineWidth;
            // Clamp progress between 0 and 1
            newProgress = Math.max(0, Math.min(1, newProgress));
            currentPlayPosition = (long)(timelineDuration * newProgress);
            progressX = timelineX + (int)(timelineWidth * newProgress);
        }

        graphics.fill(RenderType.gui(),
                progressX - PROGRESS_LINE_WIDTH/2,
                timelineY,
                progressX + PROGRESS_LINE_WIDTH/2,
                timelineY + TIMELINE_HEIGHT,
                PROGRESS_COLOR.getColorInt());

        // Format both times
        String currentTimeStr = formatTime(currentPlayPosition);
        String totalTimeStr = formatTime(actualEndTime);

        // Determine current time color
        int currentTimeColor = currentPlayPosition > actualEndTime ?
                TIMELINE_PADDING_COLOR.getColorInt() :
                UIBase.getUIColorTheme().generic_text_base_color.getColorInt();

        // Draw current time in appropriate color
        String timePrefix = "Time: ";
        graphics.drawString(Minecraft.getInstance().font, timePrefix,
                timelineX, timelineY + TIMELINE_HEIGHT + 5,
                UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        // Draw current time
        graphics.drawString(Minecraft.getInstance().font, currentTimeStr,
                timelineX + Minecraft.getInstance().font.width(timePrefix),
                timelineY + TIMELINE_HEIGHT + 5,
                currentTimeColor, false);

        // Draw separator and total time
        String separator = " / ";
        graphics.drawString(Minecraft.getInstance().font, separator + totalTimeStr,
                timelineX + Minecraft.getInstance().font.width(timePrefix + currentTimeStr),
                timelineY + TIMELINE_HEIGHT + 5,
                UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        // Update preview element based on current time
        AnimationKeyframe currentFrame = null;
        AnimationKeyframe nextFrame = null;

        // Find current and next keyframes
        for (int i = 0; i < workingKeyframes.size() - 1; i++) {
            AnimationKeyframe k1 = workingKeyframes.get(i);
            AnimationKeyframe k2 = workingKeyframes.get(i + 1);

            if (currentPlayPosition >= k1.timestamp && currentPlayPosition < k2.timestamp) {
                currentFrame = k1;
                nextFrame = k2;
                break;
            }
        }

        // Apply interpolated values to preview element
        if ((this.isPlaying || this.isDraggingProgress) && (currentFrame != null) && (nextFrame != null)) {
            this.selectedKeyframe = null;
            this.previewEditorElement.setSelected(false);
            float progress = (float)(currentPlayPosition - currentFrame.timestamp) / (nextFrame.timestamp - currentFrame.timestamp);
            previewElement.posOffsetX = (int)lerp(currentFrame.posOffsetX, nextFrame.posOffsetX, progress);
            previewElement.posOffsetY = (int)lerp(currentFrame.posOffsetY, nextFrame.posOffsetY, progress);
            previewElement.baseWidth = (int)lerp(currentFrame.baseWidth, nextFrame.baseWidth, progress);
            previewElement.baseHeight = (int)lerp(currentFrame.baseHeight, nextFrame.baseHeight, progress);
            previewElement.anchorPoint = nextFrame.anchorPoint;
            previewElement.stickyAnchor = nextFrame.stickyAnchor;
        }

        // Render keyframe info
        if (selectedKeyframe != null) {
            String keyframeInfo = String.format(
                    "Selected Keyframe:%n" +
                            "Time: %s%n" +
                            "Position: (%d, %d)%n" +
                            "Size: %dx%d%n" +
                            "Anchor: %s%n" +
                            "Sticky: %s",
                    formatTime(selectedKeyframe.timestamp),
                    selectedKeyframe.posOffsetX,
                    selectedKeyframe.posOffsetY,
                    selectedKeyframe.baseWidth,
                    selectedKeyframe.baseHeight,
                    selectedKeyframe.anchorPoint.getName(),
                    selectedKeyframe.stickyAnchor ? "Yes" : "No"
            );

            String[] lines = keyframeInfo.split("\n");
            int yOffset = 10 + 20 + 10;
            for (String line : lines) {
                graphics.drawString(Minecraft.getInstance().font, line.trim(), 10, yOffset, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);
                yOffset += 10;
            }
        }

        // Render preview
        if ((selectedKeyframe != null) && !isPlaying) {
            // Render editor element for resizing when keyframe selected
            previewEditorElement.render(graphics, mouseX, mouseY, partial);
        } else {
            // Otherwise render regular preview
            graphics.fill(RenderType.gui(),
                    previewElement.getAbsoluteX(),
                    previewElement.getAbsoluteY(),
                    previewElement.getAbsoluteX() + previewElement.getAbsoluteWidth(),
                    previewElement.getAbsoluteY() + previewElement.getAbsoluteHeight(),
                    PREVIEW_COLOR.getColorInt());
        }

        super.render(graphics, mouseX, mouseY, partial);
    }

    protected void updateTimelineDuration() {
        // Find the furthest right keyframe
        long maxTimestamp = 0;
        for (AnimationKeyframe kf : workingKeyframes) {
            maxTimestamp = Math.max(maxTimestamp, kf.timestamp);
        }

        // Add padding after the last keyframe
        long newDuration = maxTimestamp + TIMELINE_PADDING_DURATION;

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

    private String formatTimeWithPadding(long currentTime, long actualEndTime) {
        String mainTime = formatTime(Math.min(currentTime, actualEndTime));

        // If we're in the padding area, add the padding time
        if (currentTime > actualEndTime) {
            long paddingTime = currentTime - actualEndTime;
            String paddingDisplay = String.format(" + %.1fs", paddingTime / 1000.0f);
            return mainTime + paddingDisplay;
        }

        return mainTime;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (this.previewEditorElement.mouseClicked(mouseX, mouseY, button)) return true;

        // Handle clicking progress line
        if (isOverProgressLine((int)mouseX, (int)mouseY)) {
            isDraggingProgress = true;
            return true;
        }

        // Handle clicking keyframe lines
        int clickedIndex = getKeyframeIndexAtPosition((int)mouseX, (int)mouseY);
        if (clickedIndex >= 0) {
            initialDragClickX = (int)mouseX;
            hasMovedFromClickPosition = false;
            draggingKeyframeIndex = clickedIndex;
            if (isPlaying) this.togglePlayback();
            selectedKeyframe = workingKeyframes.get(clickedIndex);
            //Set preview to clicked keyframe to show it
            this.applyKeyframeValuesToElement(selectedKeyframe, previewElement);
            previewEditorElement.setSelected(true);

            // Update buttons to match selected keyframe
            this.anchorButton.setSelectedValue(selectedKeyframe.anchorPoint);
            this.stickyButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(selectedKeyframe.stickyAnchor));
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
        if (this.previewEditorElement.isSelected() && (previewGotResized || previewGotMoved) && (this.selectedKeyframe != null)) {
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
                updateTimelineDuration();
                return true;
            }
        }

        // Handle DELETE key
        if (keyCode == KEY_DELETE_KEYFRAME) {
            if (selectedKeyframe != null) {
                deleteSelectedKeyframe();
                return true;
            }
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
        isPlaying = !isPlaying;
        if (isPlaying) {
            playStartTime = System.currentTimeMillis();
            selectedKeyframe = null;
            previewEditorElement.setSelected(false);
        }
    }

    protected boolean isOverProgressLine(int mouseX, int mouseY) {
        long maxTime = timelineDuration;
        float progress = (float) currentPlayPosition / maxTime;
        int progressX = timelineX + (int)(timelineWidth * progress);

        return mouseY >= timelineY && mouseY <= timelineY + TIMELINE_HEIGHT &&
                mouseX >= progressX - 5 && mouseX <= progressX + 5;
    }

    protected int getKeyframeIndexAtPosition(int mouseX, int mouseY) {
        if (mouseY < timelineY || mouseY > timelineY + TIMELINE_HEIGHT) {
            return -1;
        }

        for (int i = 0; i < workingKeyframes.size(); i++) {
            AnimationKeyframe keyframe = workingKeyframes.get(i);
            float progress = (float) keyframe.timestamp / timelineDuration;
            int lineX = timelineX + (int)(timelineWidth * progress);

            if (mouseX >= lineX - 5 && mouseX <= lineX + 5) {
                return i;
            }
        }

        return -1;
    }

    protected float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    protected static class PreviewElement extends AbstractElement {

        public PreviewElement(ElementBuilder<?, ?> builder) {
            super(builder);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        }

    }

    protected static class PreviewEditorElement extends AbstractEditorElement {

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
            graphics.fill(RenderType.gui(),
                    this.getX(), this.getY(),
                    this.getX() + this.getWidth(),
                    this.getY() + this.getHeight(),
                    PREVIEW_COLOR.getColorInt());

            // Render resize border and grabbers
            super.render(graphics, mouseX, mouseY, partial);
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
}