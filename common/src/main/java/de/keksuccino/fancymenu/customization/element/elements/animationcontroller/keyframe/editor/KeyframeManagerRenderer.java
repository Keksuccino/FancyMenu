package de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.editor;

import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.animationcontroller.keyframe.AnimationKeyframe;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Rendering support for the keyframe manager. */
final class KeyframeManagerRenderer {

    private static final DrawableColor TIMELINE_COLOR = DrawableColor.of(new Color(0, 122, 204));
    private static final DrawableColor TIMELINE_PADDING_COLOR = DrawableColor.of(new Color(3, 83, 138));
    private static final DrawableColor KEYFRAME_COLOR = DrawableColor.of(new Color(255, 255, 255));
    private static final DrawableColor SELECTED_KEYFRAME_COLOR = DrawableColor.of(new Color(180, 37, 196));
    private static final DrawableColor PROGRESS_COLOR = DrawableColor.of(new Color(255, 0, 0));
    private static final DrawableColor RECORDING_COLOR = DrawableColor.of(new Color(196, 37, 37));
    private static final DrawableColor RECORDING_PAUSED_COLOR = DrawableColor.of(new Color(219, 108, 4));
    private static final DrawableColor OFFSET_MODE_CROSSHAIR_COLOR = DrawableColor.of(new Color(219, 108, 4));
    private static final int KEYFRAME_LINE_WIDTH = 2;
    private static final int KEYFRAME_LINE_HEIGHT = 30;
    private static final int PROGRESS_LINE_WIDTH = 2;
    private static final int NOTIFICATION_PADDING = 10;
    private static final long RECORDING_BLINK_INTERVAL = 600L;

    private long lastRecordingBlinkTime = -1L;
    private boolean recordingBlinkState = true;

    public void renderTimeline(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, @NotNull List<AnimationKeyframe> keyframes, @NotNull List<AnimationKeyframe> selectedKeyframes, long currentPosition, long actualEndTime) {
        this.renderTimelineBackground(graphics, timeline, actualEndTime);
        this.renderKeyframes(graphics, timeline, keyframes, selectedKeyframes);
        this.renderProgressLine(graphics, timeline, currentPosition);
        this.renderTimeText(graphics, timeline, currentPosition, actualEndTime);
    }

    public void renderKeyframeInfo(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, @NotNull List<AnimationKeyframe> selectedKeyframes) {
        if (selectedKeyframes.size() != 1) return;
        AnimationKeyframe keyframe = selectedKeyframes.get(0);
        String yes = I18n.get("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info.yes");
        String no = I18n.get("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info.no");
        String anchorPointName = keyframe.anchorPoint != null ? keyframe.anchorPoint.getName() : ElementAnchorPoints.TOP_LEFT.getName();
        Component[] lines = LocalizationUtils.splitLocalizedLines("fancymenu.elements.animation_controller.keyframe_manager.keyframe_info", timeline.formatTime(keyframe.timestamp), String.valueOf(keyframe.posOffsetX), String.valueOf(keyframe.posOffsetY), String.valueOf(keyframe.baseWidth), String.valueOf(keyframe.baseHeight), anchorPointName, keyframe.stickyAnchor ? yes : no);
        int y = 40;
        for (Component line : lines) {
            graphics.drawString(Minecraft.getInstance().font, line, 10, y, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);
            y += 10;
        }
    }

    public void renderRecordingIndicator(@NotNull GuiGraphics graphics, int screenWidth, boolean recording, boolean paused) {
        if (!recording) return;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - this.lastRecordingBlinkTime) > RECORDING_BLINK_INTERVAL) {
            this.recordingBlinkState = !this.recordingBlinkState;
            this.lastRecordingBlinkTime = currentTime;
        }
        MutableComponent recordingText = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording").setStyle(Style.EMPTY.withColor(RECORDING_COLOR.getColorInt()));
        MutableComponent manualModeText = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.recording_speed.manual_mode").setStyle(Style.EMPTY.withColor(RECORDING_PAUSED_COLOR.getColorInt()));
        int recordingTextWidth = Minecraft.getInstance().font.width(recordingText);
        int manualModeTextWidth = Minecraft.getInstance().font.width(manualModeText);
        int indicatorSize = 20;
        int padding = 5;
        int recordingTextX = screenWidth - recordingTextWidth - padding - indicatorSize - 10;
        int manualModeTextX = screenWidth - manualModeTextWidth - padding - indicatorSize - 10;
        int recordingTextY = paused ? 10 : 10 + (indicatorSize - Minecraft.getInstance().font.lineHeight) / 2;
        graphics.drawString(Minecraft.getInstance().font, recordingText, recordingTextX, recordingTextY, -1, false);
        if (paused) graphics.drawString(Minecraft.getInstance().font, manualModeText, manualModeTextX, 10 + indicatorSize - Minecraft.getInstance().font.lineHeight, -1, false);
        if (this.recordingBlinkState) {
            int indicatorX = recordingTextX + recordingTextWidth + padding;
            graphics.fill(indicatorX, 10, indicatorX + indicatorSize, 10 + indicatorSize, RECORDING_COLOR.getColorInt());
        }
    }

    public void renderOffsetModeCrosshair(@NotNull GuiGraphics graphics, int screenWidth, int screenHeight, boolean offsetMode) {
        if (!offsetMode) return;
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        graphics.fill(centerX - 10, centerY - 1, centerX + 10, centerY + 1, OFFSET_MODE_CROSSHAIR_COLOR.getColorInt());
        graphics.fill(centerX - 1, centerY - 10, centerX + 1, centerY + 10, OFFSET_MODE_CROSSHAIR_COLOR.getColorInt());
    }

    public void renderNotifications(@NotNull GuiGraphics graphics, int screenWidth, boolean recording, @NotNull List<KeyframeEditorNotification> notifications) {
        Iterator<KeyframeEditorNotification> iterator = notifications.iterator();
        long currentTime = System.currentTimeMillis();
        int y = recording ? 40 : 10;
        while (iterator.hasNext()) {
            KeyframeEditorNotification notification = iterator.next();
            if (notification.isExpired(currentTime)) {
                iterator.remove();
                continue;
            }
            int textColor = UIBase.getUITheme().ui_interface_generic_text_color.getColorIntWithAlpha(notification.getOpacity(currentTime));
            graphics.drawString(Minecraft.getInstance().font, notification.getMessage(), screenWidth - Minecraft.getInstance().font.width(notification.getMessage()) - NOTIFICATION_PADDING, y, textColor, false);
            y += notification.getHeight();
        }
    }

    private void renderTimelineBackground(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, long actualEndTime) {
        float usableWidth = timeline.getWidth() * ((float)Math.max(actualEndTime + KeyframeTimeline.PADDING_DURATION, KeyframeTimeline.MIN_DURATION) / timeline.getDuration());
        int paddingStartX = timeline.getX() + (int)((float)actualEndTime / timeline.getDuration() * usableWidth);
        graphics.fill(timeline.getX(), timeline.getY(), paddingStartX, timeline.getY() + KeyframeTimeline.HEIGHT, TIMELINE_COLOR.getColorInt());
        graphics.fill(paddingStartX, timeline.getY(), timeline.getX() + (int)usableWidth, timeline.getY() + KeyframeTimeline.HEIGHT, TIMELINE_PADDING_COLOR.getColorInt());
    }

    private void renderKeyframes(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, @NotNull List<AnimationKeyframe> keyframes, @NotNull List<AnimationKeyframe> selectedKeyframes) {
        Set<AnimationKeyframe> selectedKeyframeSet = new HashSet<>(selectedKeyframes);
        for (AnimationKeyframe keyframe : keyframes) {
            int lineX = timeline.timestampToX(keyframe.timestamp);
            DrawableColor color = selectedKeyframeSet.contains(keyframe) ? SELECTED_KEYFRAME_COLOR : KEYFRAME_COLOR;
            graphics.fill(lineX - KEYFRAME_LINE_WIDTH / 2, timeline.getY() + (KeyframeTimeline.HEIGHT - KEYFRAME_LINE_HEIGHT) / 2, lineX + KEYFRAME_LINE_WIDTH / 2, timeline.getY() + (KeyframeTimeline.HEIGHT + KEYFRAME_LINE_HEIGHT) / 2, color.getColorInt());
        }
    }

    private void renderProgressLine(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, long currentPosition) {
        int progressX = timeline.timestampToX(currentPosition);
        graphics.fill(progressX - PROGRESS_LINE_WIDTH / 2, timeline.getY(), progressX + PROGRESS_LINE_WIDTH / 2, timeline.getY() + KeyframeTimeline.HEIGHT, PROGRESS_COLOR.getColorIntWithAlpha(0.7F));
    }

    private void renderTimeText(@NotNull GuiGraphics graphics, @NotNull KeyframeTimeline timeline, long currentPosition, long actualEndTime) {
        int currentTimeColor = currentPosition > actualEndTime ? UIBase.getUITheme().warning_color.getColorInt() : UIBase.getUITheme().ui_interface_generic_text_color.getColorInt();
        MutableComponent currentTime = Component.literal(timeline.formatTime(currentPosition)).setStyle(Style.EMPTY.withColor(currentTimeColor));
        MutableComponent totalTime = Component.literal(timeline.formatTime(actualEndTime));
        MutableComponent text = Component.translatable("fancymenu.elements.animation_controller.keyframe_manager.time", currentTime, totalTime);
        graphics.drawString(Minecraft.getInstance().font, text, timeline.getX(), timeline.getY() + KeyframeTimeline.HEIGHT + 5, UIBase.getUITheme().ui_interface_generic_text_color.getColorInt(), false);
    }

}
