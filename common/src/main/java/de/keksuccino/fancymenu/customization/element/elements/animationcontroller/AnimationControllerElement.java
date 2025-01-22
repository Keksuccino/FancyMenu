package de.keksuccino.fancymenu.customization.element.elements.animationcontroller;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AnimationControllerElement extends AbstractElement {
    
    private static final DrawableColor CONTROLLER_COLOR = DrawableColor.of(new Color(0, 255, 0, 100));
    private static final DrawableColor RECORDING_COLOR = DrawableColor.of(new Color(255, 0, 0, 100));
    private static final DrawableColor PAUSED_COLOR = DrawableColor.of(new Color(219, 108, 4));
    
    protected List<AnimationKeyframe> keyframes = new ArrayList<>();
    protected String targetElementId = null;
    protected boolean isRecording = false;
    protected boolean isPaused = false;
    protected long pauseStartTime = -1;
    protected long recordStartTime = -1;
    protected AnimationKeyframe lastKeyframe = null;
    public boolean animationApplied = false;

    public AnimationControllerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        if (isEditor()) {

            // Get appropriate color based on state
            DrawableColor color = isRecording ?
                    (isPaused ? PAUSED_COLOR : RECORDING_COLOR) :
                    CONTROLLER_COLOR;

            graphics.fill(RenderType.gui(), this.getAbsoluteX(), this.getAbsoluteY(),
                    this.getAbsoluteX() + this.getAbsoluteWidth(),
                    this.getAbsoluteY() + this.getAbsoluteHeight(),
                    color.getColorIntWithAlpha(this.opacity));

            if (isRecording) {
                // Calculate elapsed time accounting for pauses
                long elapsedTime = isPaused ?
                        pauseStartTime - recordStartTime :
                        System.currentTimeMillis() - recordStartTime;

                String status = isPaused ? "Paused" : "Recording...";
                String info = String.format("%s %ds | %d keyframes",
                        status, elapsedTime / 1000, keyframes.size());

                graphics.drawString(Minecraft.getInstance().font, info,
                        this.getAbsoluteX() + 5, this.getAbsoluteY() + 5, -1);

                // Show recording hotkey overlay
                String hotkeyInfo = isPaused ?
                        "P -> Resume Recording | ESC -> Stop Recording" :
                        "K -> Add Keyframe | P -> Pause | ESC -> Stop Recording";

                graphics.drawString(Minecraft.getInstance().font, hotkeyInfo,
                        this.getAbsoluteX() + 5,
                        this.getAbsoluteY() + Minecraft.getInstance().font.lineHeight + 10, -1);
            } else {
                // Show start recording hotkey when not recording
                graphics.drawString(Minecraft.getInstance().font,
                        "Select Element + R -> Start Recording",
                        this.getAbsoluteX() + 5,
                        this.getAbsoluteY() + 5, -1);
            }

        } else {

            if ((this.targetElementId != null) && !this.animationApplied) {

                ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
                if (layer != null) {
                    AbstractElement target = layer.getElementByInstanceIdentifier(this.targetElementId);
                    if (target != null) this.animationApplied = AnimationControllerHandler.applyAnimation(this, target);
                }

            }

        }

    }

    public void startRecording() {
        isRecording = true;
        recordStartTime = System.currentTimeMillis();
        keyframes.clear();
        addCurrentKeyframe(); // Add initial keyframe
    }

    public void stopRecording() {
        isRecording = false;
        recordStartTime = -1;
    }

    public void pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }

    public void resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false;
            // Adjust recordStartTime to account for pause duration
            long pauseDuration = System.currentTimeMillis() - pauseStartTime;
            recordStartTime += pauseDuration;
            pauseStartTime = -1;
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void addCurrentKeyframe() {
        if (!isRecording) return;
        long timestamp = System.currentTimeMillis() - recordStartTime;
        AnimationKeyframe keyframe = new AnimationKeyframe(
                timestamp,
                this.posOffsetX,
                this.posOffsetY,
                this.baseWidth,
                this.baseHeight,
                this.anchorPoint,
                this.stickyAnchor
        );
        keyframes.add(keyframe);
        lastKeyframe = keyframe;
    }

    public void setTargetElementId(String elementId) {
        this.targetElementId = elementId;
    }

    public String getTargetElementId() {
        return targetElementId;
    }

    public List<AnimationKeyframe> getKeyframes() {
        return new ArrayList<>(keyframes);
    }

}
