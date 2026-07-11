package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import com.mojang.blaze3d.platform.Window;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoint;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.AnimationControllerElement;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeInterpolator;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframeSequence;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.MathUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.fancymenu.util.rendering.ui.widget.editbox.ExtendedEditBox;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.RangeSlider;
import de.keksuccino.fancymenu.util.window.WindowHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class KeyframeManagerScreen extends Screen {

    protected static final int KEY_MOVE_KEYFRAME_LEFT = InputConstants.KEY_LEFT;
    protected static final int KEY_MOVE_KEYFRAME_RIGHT = InputConstants.KEY_RIGHT;
    protected static final int KEY_DELETE_KEYFRAME = InputConstants.KEY_DELETE;
    protected static final int KEY_ADD_KEYFRAME = InputConstants.KEY_K;
    protected static final int KEY_TOGGLE_RECORDING = InputConstants.KEY_R;
    protected static final int KEY_TOGGLE_PAUSE_RECORDING = InputConstants.KEY_T;
    protected static final int KEY_TOGGLE_PLAYING = InputConstants.KEY_P;

    protected static final int KEYFRAME_LINE_WIDTH = 2;
    protected static final int KEYFRAME_DRAG_CRUMPLE_ZONE = 3;

    protected static final Component KEYFRAME_ADDED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.keyframe_added").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
    protected static final Component KEYFRAME_DELETED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.keyframe_deleted").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
    protected static final Component PLAYING_STARTED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.playing_started").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().success_color.getColorInt()));
    protected static final Component PLAYING_STOPPED_TEXT = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.playing_stopped").setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));

    protected final LayoutEditorScreen parentEditor;
    protected final Consumer<KeyframeEditorResult> resultCallback;
    protected final KeyframeEditorState editorState;
    protected final List<AnimationKeyframe> workingKeyframes;
    protected final KeyframePreviewElement previewElement;
    protected final KeyframePreviewEditorElement previewEditorElement;
    protected final AnimationPreviewViewport previewViewport = new AnimationPreviewViewport();
    protected final AnimationKeyframePreviewMapper previewMapper = new AnimationKeyframePreviewMapper(this.previewViewport);
    protected final KeyframeTimeline timeline;
    protected final KeyframeManagerRenderer renderer = new KeyframeManagerRenderer();
    protected final int initialParentViewportWidth;
    protected final int initialParentViewportHeight;
    protected final int initialFramebufferWidth;
    protected final int initialFramebufferHeight;
    protected final double initialParentGuiScale;
    protected boolean isDraggingProgress = false;
    protected boolean isPlaying = false;
    protected long playStartTime = -1;
    protected long currentPlayOrRecordPosition = 0;
    protected final List<AnimationKeyframe> selectedKeyframes;
    protected int draggingKeyframeIndex = -1;
    protected AnimationKeyframe lastShortcutModifierClickedFrameForDeselect = null;
    protected boolean framesGotMoved = false;
    protected int initialDragClickX = 0;
    protected boolean hasMovedFromClickPosition = false;
    protected boolean isRecording = false;
    protected boolean isRecordingPaused = false;
    protected long recordStartTime = -1;
    protected double recordingSpeed = 1.0;
    protected double cachedRecordingSpeed = 1.0D;
    protected final Map<Integer, Integer> cachedWidgetRowCurrentX = new HashMap<>();
    protected final List<KeyframeEditorNotification> activeNotifications = new ArrayList<>();
    protected boolean isShowingSmoothingInput = false;
    protected String lastSmoothingInputValue = null;
    protected boolean isShowingTimestampInput = false;
    protected boolean isOffsetMode = false;

    protected CycleButton<ElementAnchorPoint> anchorButton;
    protected CycleButton<CommonCycles.CycleEnabledDisabled> stickyButton;
    protected RangeSlider recordingSpeedSlider;
    protected ExtendedEditBox smoothingDistanceInput;
    protected ExtendedEditBox timestampInput;

    protected int lastGuiScaleCorrectionWidth = 0;
    protected int lastGuiScaleCorrectionHeight = 0;

    public KeyframeManagerScreen(@NotNull AnimationControllerElement controller, @NotNull Consumer<KeyframeEditorResult> resultCallback) {
        this(Objects.requireNonNull(LayoutEditorScreen.getCurrentInstance(), "The keyframe manager requires an active layout editor"), controller, resultCallback);
    }

    public KeyframeManagerScreen(@NotNull LayoutEditorScreen parentEditor, @NotNull AnimationControllerElement controller, @NotNull Consumer<KeyframeEditorResult> resultCallback) {
        super(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager"));
        this.parentEditor = Objects.requireNonNull(parentEditor);
        this.isOffsetMode = controller.offsetMode;
        this.resultCallback = Objects.requireNonNull(resultCallback);
        Window window = Minecraft.getInstance().getWindow();
        this.initialParentViewportWidth = Math.max(1, parentEditor.width);
        this.initialParentViewportHeight = Math.max(1, parentEditor.height);
        this.initialFramebufferWidth = Math.max(1, window.getWidth());
        this.initialFramebufferHeight = Math.max(1, window.getHeight());
        this.initialParentGuiScale = WindowHandler.getGuiScale();
        this.previewViewport.update(this.initialParentViewportWidth, this.initialParentViewportHeight, this.initialParentViewportWidth, this.initialParentViewportHeight);
        this.editorState = new KeyframeEditorState(controller.keyframes);
        this.workingKeyframes = this.editorState.getKeyframes();
        this.selectedKeyframes = this.editorState.getSelectedKeyframes();
        this.timeline = new KeyframeTimeline(this.workingKeyframes);

        this.previewElement = new KeyframePreviewElement(controller.getBuilder(), this.previewViewport);
        this.previewElement.baseWidth = 50;
        this.previewElement.baseHeight = 50;
        this.previewElement.posOffsetX = 0;
        this.previewElement.posOffsetY = 0;
        this.previewElement.stayOnScreen = false;
        this.previewElement.stickyAnchor = true;
        this.previewElement.anchorPoint = ElementAnchorPoints.MID_CENTERED;
        if (!this.workingKeyframes.isEmpty()) this.previewMapper.apply(this.workingKeyframes.getFirst(), this.previewElement, this.isOffsetMode);
        this.previewEditorElement = new KeyframePreviewEditorElement(this.previewElement, new LayoutEditorScreen(Layout.buildUniversal()), () -> this.isRecording, this.selectedKeyframes::size);
    }

    @Override
    protected void init() {

        if (this.correctGuiScaleAboveParentScale()) return;
        this.refreshPreviewViewport();

        this.timeline.updateBounds(this.width, this.height);

        this.cachedWidgetRowCurrentX.clear();
        AbstractWidget farRightWidget = KeyframeManagerControls.initialize(this);

        if (!isPlaying && !isRecording) {
            updateTimelineDurationToMaxTimestamp();
        }
        Window window = Minecraft.getInstance().getWindow();
        boolean resized = (window.getScreenWidth() != this.lastGuiScaleCorrectionWidth) || (window.getScreenHeight() != this.lastGuiScaleCorrectionHeight);
        this.lastGuiScaleCorrectionWidth = window.getScreenWidth();
        this.lastGuiScaleCorrectionHeight = window.getScreenHeight();
        boolean tooFarRight = (farRightWidget.getX() + farRightWidget.getWidth()) >= (this.width - 100);

        if (tooFarRight && (WindowHandler.getGuiScale() > 1)) {
            double newScale = WindowHandler.getGuiScale();
            newScale--;
            if (newScale < 1) newScale = 1;
            WindowHandler.setGuiScale(newScale);
            this.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        } else if (!tooFarRight && resized) {
            double parentScale = this.resolveParentGuiScale(window);
            if (Double.compare(WindowHandler.getGuiScale(), parentScale) != 0) {
                WindowHandler.setGuiScale(parentScale);
                this.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
        }

    }

    /**
     * A larger GUI scale would make the preview display viewport smaller than its source viewport and cause multiple
     * source coordinates to collapse onto the same display pixel. Keep the manager at or below the parent scale so
     * resize reinitialization always has a lossless source-space round trip.
     */
    protected boolean correctGuiScaleAboveParentScale() {
        Window window = Minecraft.getInstance().getWindow();
        double parentScale = this.resolveParentGuiScale(window);
        if (WindowHandler.getGuiScale() <= parentScale) return false;
        WindowHandler.setGuiScale(parentScale);
        this.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        return true;
    }

    /**
     * Preserves the preview in serialized keyframe space while the manager changes its GUI scale or the window is
     * resized. Reapplying through the updated viewport keeps the full visible manager canvas mapped to the parent
     * editor instead of leaving the parent canvas unscaled in the top-left portion of the screen.
     */
    protected void refreshPreviewViewport() {
        AnimationKeyframe previewState = new AnimationKeyframe(0, 0, 0, 0, 0, this.previewElement.anchorPoint, this.previewElement.stickyAnchor);
        this.previewMapper.capture(this.previewElement, previewState, this.isOffsetMode);

        Window window = Minecraft.getInstance().getWindow();
        int sourceWidth = this.resolveParentViewportWidth(window);
        int sourceHeight = this.resolveParentViewportHeight(window);
        this.previewViewport.update(sourceWidth, sourceHeight, this.width, this.height);
        this.previewMapper.apply(previewState, this.previewElement, this.isOffsetMode);
    }

    protected int resolveParentViewportWidth(@NotNull Window window) {
        if ((window.getWidth() == this.initialFramebufferWidth) && (window.getHeight() == this.initialFramebufferHeight)) return this.initialParentViewportWidth;
        return Math.max(1, (int)Math.ceil((double)window.getWidth() / this.resolveParentGuiScale(window)));
    }

    protected int resolveParentViewportHeight(@NotNull Window window) {
        if ((window.getWidth() == this.initialFramebufferWidth) && (window.getHeight() == this.initialFramebufferHeight)) return this.initialParentViewportHeight;
        return Math.max(1, (int)Math.ceil((double)window.getHeight() / this.resolveParentGuiScale(window)));
    }

    protected double resolveParentGuiScale(@NotNull Window window) {
        if ((window.getWidth() == this.initialFramebufferWidth) && (window.getHeight() == this.initialFramebufferHeight)) return this.initialParentGuiScale;
        double currentScale = window.calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().options.forceUnicodeFont().get());

        if (this.parentEditor.layout.forcedScale != 0) {
            currentScale = this.parentEditor.layout.forcedScale;
            if (currentScale <= 0.0D) currentScale = 1.0D;
        }

        if ((this.parentEditor.layout.autoScalingWidth != 0) && (this.parentEditor.layout.autoScalingHeight != 0)) {
            int viewportWidth = (int)Math.ceil((double)window.getWidth() / currentScale);
            int viewportHeight = (int)Math.ceil((double)window.getHeight() / currentScale);
            double guiWidth = (double)viewportWidth * currentScale;
            double guiHeight = (double)viewportHeight * currentScale;
            double newScaleX = (guiWidth / (double)this.parentEditor.layout.autoScalingWidth) * currentScale;
            double newScaleY = (guiHeight / (double)this.parentEditor.layout.autoScalingHeight) * currentScale;
            currentScale = Math.min(newScaleX, newScaleY);
            if (!Double.isFinite(currentScale) || (currentScale <= 0.0D)) currentScale = 1.0D;
        }
        return currentScale;
    }

    @Override
    public void removed() {
        RenderingUtils.resetGuiScale();
        super.removed();
    }

    /** Adds a widget to a bottom control row and advances that row's layout cursor. */
    protected <T extends AbstractWidget> T addBottomWidget(int row, int spacingAfterButtonOffset, @NotNull T widget) {
        if (row < 1) row = 1;
        if (!this.cachedWidgetRowCurrentX.containsKey(row)) {
            this.cachedWidgetRowCurrentX.put(row, this.timeline.getX());
        }
        int currentX = this.cachedWidgetRowCurrentX.get(row);
        int y = this.timeline.getY() - 25 - (25 * (row - 1));
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

    protected <T extends AbstractWidget> T addManagerWidget(@NotNull T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {


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

        long actualEndTime = this.timeline.getEndTime();

        if (isRecording && !isPlaying) {
            if (!isRecordingPaused) {
                long now = System.currentTimeMillis();
                long actualElapsed = now - recordStartTime;
                currentPlayOrRecordPosition = (long)(actualElapsed * recordingSpeed);
                long trimmedActualDuration = this.timeline.getEndTime() - KeyframeTimeline.EXTENSION_STEP;
                if (currentPlayOrRecordPosition >= trimmedActualDuration) {
                    this.timeline.extend();
                    actualEndTime = this.timeline.getEndTime();
                }
            }
        }

        if (isPlaying && !isRecording) {
            currentPlayOrRecordPosition = System.currentTimeMillis() - playStartTime;
            if (currentPlayOrRecordPosition > this.timeline.getDuration()) {
                isPlaying = false;
                currentPlayOrRecordPosition = 0;
            }
        }

        graphics.fill(0, 0, this.width, this.height, UIBase.getUITheme().ui_interface_background_color.getColorInt());

        LayoutEditorScreen.renderGrid(graphics, this.width, this.height);

        this.renderer.renderTimeline(graphics, this.timeline, this.workingKeyframes, this.selectedKeyframes, this.currentPlayOrRecordPosition, actualEndTime);

        this.tickAnimation(); // Preview interpolation must run before the preview body is extracted.

        this.renderPreview(graphics, mouseX, mouseY, partial);

        this.renderer.renderKeyframeInfo(graphics, this.timeline, this.selectedKeyframes);
        this.renderer.renderRecordingIndicator(graphics, this.width, this.isRecording, this.isRecordingPaused);
        this.renderer.renderOffsetModeCrosshair(graphics, this.width, this.height, this.isOffsetMode);
        this.renderer.renderNotifications(graphics, this.width, this.isRecording, this.activeNotifications);

        super.extractRenderState(graphics, mouseX, mouseY, partial);

    }

    protected void tickAnimation() {
        if (this.isRecording && !this.isRecordingPaused) return;
        if (!this.isPlaying && !this.isDraggingProgress) return;
        AnimationKeyframeSequence.Segment segment = AnimationKeyframeSequence.findSegment(this.workingKeyframes, this.currentPlayOrRecordPosition);
        if (segment == null) {
            if (!this.workingKeyframes.isEmpty() && (this.currentPlayOrRecordPosition == this.workingKeyframes.getLast().timestamp)) {
                this.selectKeyframeClearOldSelection(null);
                this.previewMapper.apply(this.workingKeyframes.getLast(), this.previewElement, this.isOffsetMode);
            }
            return;
        }
        this.selectKeyframeClearOldSelection(null);
        AnimationKeyframeInterpolator.Values values = AnimationKeyframeInterpolator.interpolate(segment.current(), segment.next(), segment.progress());
        this.previewMapper.apply(values, this.previewElement, this.isOffsetMode);
    }

    protected void updateDraggedKeyframes(double mouseX, long actualDuration) {
        if ((this.draggingKeyframeIndex < 0) || (this.draggingKeyframeIndex >= this.workingKeyframes.size())) return;
        AnimationKeyframe draggedKeyframe = this.workingKeyframes.get(this.draggingKeyframeIndex);
        if (!this.hasMovedFromClickPosition) {
            this.hasMovedFromClickPosition = Math.abs(mouseX - this.initialDragClickX) >= KEYFRAME_DRAG_CRUMPLE_ZONE;
            if (!this.hasMovedFromClickPosition) return;
            this.framesGotMoved = true;
            this.saveState();
        }

        long targetDuration = this.timeline.getDuration();
        if (!this.isRecording && (mouseX > this.timeline.getX() + this.timeline.getWidth() - 10)) targetDuration += KeyframeTimeline.EXTENSION_STEP;
        double progress = Math.max(0.0D, Math.min(1.0D, (mouseX - this.timeline.getX()) / this.timeline.getWidth()));
        long timeDelta = (long)(targetDuration * progress) - draggedKeyframe.timestamp;
        long minimumSelectedTimestamp = Long.MAX_VALUE;
        long deltaToLastSelectedFrame = 0L;
        for (AnimationKeyframe selectedKeyframe : this.selectedKeyframes) {
            minimumSelectedTimestamp = Math.min(minimumSelectedTimestamp, selectedKeyframe.timestamp);
            if (selectedKeyframe.timestamp > draggedKeyframe.timestamp) deltaToLastSelectedFrame = Math.max(deltaToLastSelectedFrame, selectedKeyframe.timestamp - draggedKeyframe.timestamp);
        }
        timeDelta = Math.max(timeDelta, -minimumSelectedTimestamp);
        if (this.isRecording) {
            long maximumDraggedTimestamp = actualDuration - deltaToLastSelectedFrame;
            if (draggedKeyframe.timestamp + timeDelta > maximumDraggedTimestamp) timeDelta = maximumDraggedTimestamp - draggedKeyframe.timestamp;
        }
        for (AnimationKeyframe selectedKeyframe : this.selectedKeyframes) {
            long timestamp = Math.max(0L, selectedKeyframe.timestamp + timeDelta);
            selectedKeyframe.timestamp = this.isRecording ? Math.min(timestamp, actualDuration) : timestamp;
        }
        this.updateTimelineDurationToMaxTimestamp();
    }

    protected void renderPreview(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (!isPlaying && ((this.selectedKeyframes.size() == 1) || isRecording)) {
            previewEditorElement.extractRenderState(graphics, mouseX, mouseY, partial);
        } else {
            this.previewEditorElement.renderPreviewBody(graphics);
        }

    }

    protected void updateTimelineDurationToMaxTimestamp() {
        this.timeline.updateDurationToKeyframes(this.workingKeyframes, this.isRecording);

    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {

        this.lastShortcutModifierClickedFrameForDeselect = null;
        this.framesGotMoved = false;

        if (super.mouseClicked(event, isDoubleClick)) return true;

        if (this.isShowingSmoothingInput) {
            if (!this.smoothingDistanceInput.isMouseOver(event.x(), event.y())) {
                this.lastSmoothingInputValue = this.smoothingDistanceInput.getValue();
                this.isShowingSmoothingInput = false;
            }
        }

        if (this.isShowingTimestampInput) {
            if (!this.timestampInput.isMouseOver(event.x(), event.y())) {
                this.isShowingTimestampInput = false;
            }
        }

        if (this.previewEditorElement.mouseClicked(event, isDoubleClick)) return true;

        if (isOverProgressLine((int) event.x(), (int) event.y()) && (!this.isRecording || this.isRecordingPaused)) {
            isDraggingProgress = true;
            return true;
        }
        if (isOverProgressLine((int) event.x(), (int) event.y()) && this.isRecording && !this.isRecordingPaused) {
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_drag_progress")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())), 6000);
            return true;
        }

        int clickedIndex = getKeyframeIndexAtPosition((int) event.x(), (int) event.y());
        if (!event.hasControlDownWithQuirk() && isInTimelineArea((int) event.x(), (int) event.y()) && (clickedIndex == -1)) {
            this.selectKeyframeClearOldSelection(null);
            return true;
        }
        if (this.isRecording && !this.isRecordingPaused && (clickedIndex >= 0)) {
            this.displayNotification(Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.pause_recording_to_edit_keyframe")
                    .setStyle(Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt())), 6000);
            return true;
        }
        if (clickedIndex >= 0) {
            initialDragClickX = (int)event.x();
            hasMovedFromClickPosition = false;
            draggingKeyframeIndex = clickedIndex;
            AnimationKeyframe keyframe = this.workingKeyframes.get(draggingKeyframeIndex);
            if (event.hasControlDownWithQuirk() && this.selectedKeyframes.contains(keyframe)) {
                this.lastShortcutModifierClickedFrameForDeselect = keyframe;
            } else {
                this.selectKeyframe(workingKeyframes.get(clickedIndex), event.hasControlDownWithQuirk());
            }
            return true;
        }

        return false;

    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingProgress = false;
        draggingKeyframeIndex = -1;
        hasMovedFromClickPosition = false;
        if (this.framesGotMoved) this.editorState.sort();
        boolean previewGotResized = this.previewEditorElement.wasRecentlyResized();
        boolean previewGotMoved = this.previewEditorElement.wasRecentlyMovedByDragging();
        this.previewEditorElement.mouseReleased(event);
        if (this.previewEditorElement.isSelected() && (previewGotResized || previewGotMoved) && (this.selectedKeyframes.size() == 1) && (!this.isRecording || this.isRecordingPaused) && !this.isPlaying) {
            saveState();
            AnimationKeyframe selectedKeyframe = this.selectedKeyframes.getFirst();
            this.previewMapper.capture(this.previewElement, selectedKeyframe, this.isOffsetMode);
            this.previewMapper.apply(selectedKeyframe, this.previewElement, this.isOffsetMode);
        }
        if ((this.lastShortcutModifierClickedFrameForDeselect != null) && !this.framesGotMoved) {
            if (this.selectedKeyframes.size() > 1) {
                this.editorState.deselect(this.lastShortcutModifierClickedFrameForDeselect);
                if (this.selectedKeyframes.size() == 1) {
                    AnimationKeyframe lastSelected = this.selectedKeyframes.getFirst();
                    // Re-select through the normal path so the preview and anchor controls refresh for single selection.
                    this.selectKeyframeClearOldSelection(null);
                    this.selectKeyframeClearOldSelection(lastSelected);
                }
            } else {
                this.selectKeyframeClearOldSelection(null);
            }
        }
        this.lastShortcutModifierClickedFrameForDeselect = null;
        this.framesGotMoved = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {

        int keyCode = event.key();

        String key = GLFW.glfwGetKeyName(event.key(), event.scancode());
        if (key == null) key = "";
        key = key.toLowerCase(Locale.ROOT);

        if (event.hasControlDownWithQuirk()) {
            if ("z".equals(key)) {
                undo();
                return true;
            } else if ("y".equals(key)) {
                redo();
                return true;
            }
        }

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

        if (this.isShowingTimestampInput && this.timestampInput.isFocused()) {
            if (keyCode == InputConstants.KEY_ENTER) {
                if ((this.selectedKeyframes.size() == 1) && MathUtils.isLong(this.timestampInput.getValue())) {
                    AnimationKeyframe selectedKeyframe = this.selectedKeyframes.getFirst();
                    long timestamp = Math.max(0L, Long.parseLong(this.timestampInput.getValue()));
                    if (selectedKeyframe.timestamp != timestamp) {
                        this.saveState();
                        selectedKeyframe.timestamp = timestamp;
                        this.editorState.sort();
                    }
                }
                this.isShowingTimestampInput = false;
                updateTimelineDurationToMaxTimestamp();
                return true;
            } else if (keyCode == InputConstants.KEY_ESCAPE) {
                this.isShowingTimestampInput = false;
                return true;
            }
        }

        if (!this.isRecording || this.isRecordingPaused) {
            if (event.hasControlDownWithQuirk() && (keyCode == InputConstants.KEY_A)) {
                this.editorState.selectAll();
                this.refreshSelectionPresentation();
                return true;
            }
        }

        if (!this.selectedKeyframes.isEmpty() && !FancyMenu.getOptions().arrowKeysMovePreview.getValue()) {
            if (keyCode == KEY_MOVE_KEYFRAME_LEFT || keyCode == KEY_MOVE_KEYFRAME_RIGHT) {
                long minSelectedTimestamp = Long.MAX_VALUE;
                long maxSelectedTimestamp = Long.MIN_VALUE;
                for (AnimationKeyframe selectedFrame : selectedKeyframes) {
                    minSelectedTimestamp = Math.min(minSelectedTimestamp, selectedFrame.timestamp);
                    maxSelectedTimestamp = Math.max(maxSelectedTimestamp, selectedFrame.timestamp);
                }

                long timeShift = keyCode == KEY_MOVE_KEYFRAME_LEFT ? -100 : 100;
                if (keyCode == KEY_MOVE_KEYFRAME_LEFT) {
                    if (minSelectedTimestamp + timeShift < 0) {
                        timeShift = -minSelectedTimestamp;
                    }
                } else {
                    if (maxSelectedTimestamp + timeShift > this.timeline.getDuration()) {
                        timeShift = this.timeline.getDuration() - maxSelectedTimestamp;
                    }
                }
                if (timeShift != 0) {
                    this.saveState();
                    for (AnimationKeyframe selectedKeyframe : this.selectedKeyframes) selectedKeyframe.timestamp += timeShift;
                    this.editorState.sort();
                    updateTimelineDurationToMaxTimestamp();
                }
                return true;
            }
        } else if (FancyMenu.getOptions().arrowKeysMovePreview.getValue() && (this.selectedKeyframes.size() == 1) && (!this.isRecording || this.isRecordingPaused) && !this.isPlaying) {
            if ((keyCode == InputConstants.KEY_LEFT) || (keyCode == InputConstants.KEY_RIGHT) || (keyCode == InputConstants.KEY_UP) || (keyCode == InputConstants.KEY_DOWN)) {
                this.saveState();
                this.isShowingTimestampInput = false;
                this.isShowingSmoothingInput = false;
                AnimationKeyframe selectedKeyframe = this.selectedKeyframes.getFirst();
                if (keyCode == InputConstants.KEY_LEFT) {
                    selectedKeyframe.posOffsetX -= 1;
                }
                if (keyCode == InputConstants.KEY_RIGHT) {
                    selectedKeyframe.posOffsetX += 1;
                }
                if (keyCode == InputConstants.KEY_UP) {
                    selectedKeyframe.posOffsetY -= 1;
                }
                if (keyCode == InputConstants.KEY_DOWN) {
                    selectedKeyframe.posOffsetY += 1;
                }
                this.previewMapper.apply(selectedKeyframe, this.previewElement, this.isOffsetMode);
                return true;
            }
        }

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

        return super.keyPressed(event);

    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        boolean timelineDrag = this.isDraggingProgress || (this.draggingKeyframeIndex >= 0);
        if (this.isDraggingProgress) this.updateDraggedProgress(event.x(), this.timeline.getEndTime());
        if (this.draggingKeyframeIndex >= 0) this.updateDraggedKeyframes(event.x(), this.timeline.getEndTime());
        if (super.mouseDragged(event, dragX, dragY)) return true;
        if (timelineDrag) return true;
        return this.previewEditorElement.mouseDragged(event, dragX, dragY);
    }

    protected void updateDraggedProgress(double mouseX, long actualDuration) {
        if (this.isRecording && !this.isRecordingPaused) return;
        long timestamp = this.timeline.xToTimestamp(mouseX);
        this.currentPlayOrRecordPosition = this.isRecording ? Math.min(timestamp, actualDuration) : timestamp;
        if (this.isPlaying) this.playStartTime = System.currentTimeMillis() - this.currentPlayOrRecordPosition;
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
        // Preserve the current timeline position when recording starts below full speed.
        recordStartTime = this.recordingSpeed > 0.0D ? System.currentTimeMillis() - (long)(currentPlayOrRecordPosition / recordingSpeed) : System.currentTimeMillis();
        this.selectKeyframeClearOldSelection(null);
        draggingKeyframeIndex = -1;
        previewEditorElement.setSelected(true);
    }

    protected void stopRecording() {
        isRecording = false;
        isRecordingPaused = false;
        recordStartTime = -1;
        this.selectKeyframeClearOldSelection(null);
        currentPlayOrRecordPosition = 0;
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
                    // Rebase elapsed time so changing speed does not jump the timeline position.
                    long now = System.currentTimeMillis();
                    this.recordStartTime = now - (long) (this.currentPlayOrRecordPosition / newSpeed);
                }
                this.selectKeyframeClearOldSelection(null);
                this.isRecordingPaused = false;
            } else {
                if (oldSpeed > 0.0D) this.cachedRecordingSpeed = oldSpeed;
                this.isRecordingPaused = true;
            }
        }
        this.recordingSpeed = newSpeed;
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
        AnimationKeyframe newKeyframe = new AnimationKeyframe(currentPlayOrRecordPosition, 0, 0, 0, 0, this.previewElement.anchorPoint, this.previewElement.stickyAnchor);
        this.previewMapper.capture(this.previewElement, newKeyframe, this.isOffsetMode);
        this.editorState.add(newKeyframe);
        this.displayNotification(KEYFRAME_ADDED_TEXT, 2000);
        updateTimelineDurationToMaxTimestamp();
    }

    protected void deleteSelectedKeyframes() {
        if (!this.editorState.deleteSelected()) return;
        this.previewEditorElement.setSelected(false);
        updateTimelineDurationToMaxTimestamp();
        this.displayNotification(KEYFRAME_DELETED_TEXT, 2000);
    }

    protected void saveState() {
        this.editorState.saveSnapshot();
    }

    protected void undo() {
        if (this.isPlaying) return;
        if (!this.editorState.undo()) return;
        updateTimelineDurationToMaxTimestamp();
        this.refreshSelectionPresentation();
    }

    protected void redo() {
        if (this.isPlaying) return;
        if (!this.editorState.redo()) return;
        updateTimelineDurationToMaxTimestamp();
        this.refreshSelectionPresentation();
    }

    protected void selectKeyframe(@Nullable AnimationKeyframe selected, boolean addToSelection) {
        if (selected == null) {
            this.editorState.clearSelection();
        } else {
            this.editorState.select(selected, addToSelection);
        }
        this.refreshSelectionPresentation();
    }

    protected void refreshSelectionPresentation() {
        if (this.selectedKeyframes.size() == 1) {
            AnimationKeyframe selected = this.selectedKeyframes.getFirst();
            this.previewMapper.apply(selected, this.previewElement, this.isOffsetMode);
            this.previewEditorElement.setSelected(true);
            if (this.isPlaying) this.togglePlayback();
            this.anchorButton.setSelectedValue(selected.anchorPoint);
            this.stickyButton.setSelectedValue(CommonCycles.CycleEnabledDisabled.getByBoolean(selected.stickyAnchor));
        } else if (!this.isRecording) {
            this.previewEditorElement.setSelected(false);
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
                this.smoothingDistanceInput.setValue("100");
            }
        }
    }

    protected void toggleTimestampInput() {
        if (this.selectedKeyframes.size() != 1) return;
        AnimationKeyframe selected = this.selectedKeyframes.get(0);
        this.isShowingTimestampInput = !this.isShowingTimestampInput;
        if (this.isShowingTimestampInput) {
            this.timestampInput.setValue(String.valueOf(selected.timestamp));
        }
    }

    protected void applySmoothingDistance() {
        String value = this.smoothingDistanceInput.getValue();
        if (MathUtils.isLong(value) && !value.isEmpty()) {
            try {
                if (this.editorState.smoothSelected(Long.parseLong(value))) updateTimelineDurationToMaxTimestamp();
            } catch (NumberFormatException ignored) {
            }
        }
        this.isShowingSmoothingInput = false;
    }

    protected boolean isInTimelineArea(int mouseX, int mouseY) {
        return this.timeline.contains(mouseX, mouseY);
    }

    protected boolean isOverProgressLine(int mouseX, int mouseY) {
        return this.timeline.isOverTimestamp(mouseX, mouseY, this.currentPlayOrRecordPosition, 5);
    }

    protected int getKeyframeIndexAtPosition(int mouseX, int mouseY) {
        return this.timeline.findKeyframeIndex(this.workingKeyframes, mouseX, mouseY, Math.max(1, KEYFRAME_LINE_WIDTH / 2));
    }

    protected void setAnchorPoint(ElementAnchorPoint newAnchor) {
        previewElement.anchorPoint = newAnchor;
        previewElement.posOffsetX = 0;
        previewElement.posOffsetY = 0;
        int startX = previewElement.getAbsoluteX();
        int startY = previewElement.getAbsoluteY();
        int endX = startX + previewElement.getAbsoluteWidth();
        int endY = startY + previewElement.getAbsoluteHeight();
        if (startX < 0 || startY < 0 || endX > this.width || endY > this.height) {
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
            this.previewMapper.capture(this.previewElement, selectedKeyframe, this.isOffsetMode);
            this.previewMapper.apply(selectedKeyframe, this.previewElement, this.isOffsetMode);
        }
    }

    protected void setStickyAnchor(boolean sticky) {
        if (this.selectedKeyframes.size() == 1) {
            saveState();
            this.selectedKeyframes.getFirst().stickyAnchor = sticky;
        }
        previewElement.stickyAnchor = sticky;
    }

    public void displayNotification(@NotNull Component message, long durationMs) {
        activeNotifications.add(new KeyframeEditorNotification(message, durationMs));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

}
