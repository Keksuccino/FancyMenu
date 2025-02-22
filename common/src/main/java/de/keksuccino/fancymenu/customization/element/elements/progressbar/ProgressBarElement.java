package de.keksuccino.fancymenu.customization.element.elements.progressbar;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.enums.LocalizedCycleEnum;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

/**
 * A progress bar element that renders a filled bar based on a progress value.
 * <p>
 * It supports rendering with either a solid color or a texture, in four possible directions.
 * The filling can be smoothly interpolated (lerped) or set directly based on a toggle.
 */
public class ProgressBarElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public BarDirection direction = BarDirection.RIGHT;
    public DrawableColor barColor = DrawableColor.of(new Color(82, 149, 255));
    @Nullable
    public ResourceSupplier<ITexture> barTextureSupplier;
    public DrawableColor backgroundColor = DrawableColor.of(new Color(171, 200, 247));
    @Nullable
    public ResourceSupplier<ITexture> backgroundTextureSupplier;
    public boolean useProgressForElementAnchor = false;
    public String progressSource = null;
    public ProgressValueMode progressValueMode = ProgressValueMode.PERCENTAGE;
    public boolean smoothFillingAnimation = true;

    protected int lastRenderedProgressX = 0;
    protected int lastRenderedProgressY = 0;
    protected int lastRenderedProgressWidth = 0;
    protected int lastRenderedProgressHeight = 0;
    protected float smoothedProgress = 0.0F;

    public ProgressBarElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    /**
     * Render the progress bar element.
     */
    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        // First, render the background.
        renderBackground(graphics);
        // Then, render the progress (filled portion).
        renderProgress(graphics);

    }

    /**
     * Renders the progress (filled) portion of the bar.
     */
    protected void renderProgress(@NotNull GuiGraphics graphics) {

        // Clamp the actual progress between 0.0 and 1.0.
        float targetProgress = Math.max(0.0F, Math.min(1.0F, getCurrentProgress()));

        // Update the smoothed progress value based on the smoothLerpEnabled toggle.
        if (!smoothFillingAnimation) {
            // No smoothing: set progress directly.
            smoothedProgress = targetProgress;
        } else {
            if (targetProgress >= 1.0F) {
                smoothedProgress = 1.0F;
            } else {
                // Smoothly interpolate: 95% of previous value and 5% of target.
                smoothedProgress = Mth.clamp(smoothedProgress * 0.95F + targetProgress * 0.05F, 0.0F, 1.0F);
                // If the difference is negligible, snap to target.
                if (Math.abs(smoothedProgress - targetProgress) < 0.001F) {
                    smoothedProgress = targetProgress;
                }
            }
        }

        // Get the full dimensions and position of the element.
        int fullWidth = getAbsoluteWidth();
        int fullHeight = getAbsoluteHeight();
        int progressX = getAbsoluteX();
        int progressY = getAbsoluteY();
        float offsetX = 0.0F;
        float offsetY = 0.0F;
        int progressWidth = fullWidth;
        int progressHeight = fullHeight;

        // Adjust width/height based on the bar fill direction.
        if (direction == BarDirection.LEFT || direction == BarDirection.RIGHT) {
            progressWidth = (int) (fullWidth * smoothedProgress);
        }
        if (direction == BarDirection.UP || direction == BarDirection.DOWN) {
            progressHeight = (int) (fullHeight * smoothedProgress);
        }
        // For left/up directions, adjust the starting point.
        if (direction == BarDirection.LEFT) {
            progressX += fullWidth - progressWidth;
            offsetX = fullWidth - progressWidth;
        }
        if (direction == BarDirection.UP) {
            progressY += fullHeight - progressHeight;
            offsetY = fullHeight - progressHeight;
        }

        // Cache the computed progress bar area.
        lastRenderedProgressX = progressX;
        lastRenderedProgressY = progressY;
        lastRenderedProgressWidth = progressWidth;
        lastRenderedProgressHeight = progressHeight;

        // Enable blending for transparency.
        RenderSystem.enableBlend();

        // Render using a texture if available.
        if (barTextureSupplier != null) {
            ITexture texture = barTextureSupplier.get();
            if (texture != null) {
                ResourceLocation loc = texture.getResourceLocation();
                if (loc != null) {
                    DrawableColor.WHITE.setAsShaderColor(graphics, this.opacity);
                    graphics.blit(loc, progressX, progressY, offsetX, offsetY, progressWidth, progressHeight, fullWidth, fullHeight);
                }
            }
        }
        // Otherwise, render a solid colored bar.
        else if (barColor != null) {
            float colorAlpha = Math.min(1.0F, Math.max(0.0F, (float) FastColor.ARGB32.alpha(barColor.getColorInt()) / 255.0F));
            if (opacity <= colorAlpha) {
                colorAlpha = opacity;
            }
            graphics.fill(progressX, progressY, progressX + progressWidth, progressY + progressHeight, barColor.getColorIntWithAlpha(colorAlpha));
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

    /**
     * Renders the background of the progress bar element.
     */
    protected void renderBackground(@NotNull GuiGraphics graphics) {
        RenderSystem.enableBlend();
        if (backgroundTextureSupplier != null) {
            backgroundTextureSupplier.forRenderable((texture, location) -> {
                DrawableColor.WHITE.setAsShaderColor(graphics, this.opacity);
                graphics.blit(location, getAbsoluteX(), getAbsoluteY(), 0.0F, 0.0F, getAbsoluteWidth(), getAbsoluteHeight(), getAbsoluteWidth(), getAbsoluteHeight());
            });
        } else if (backgroundColor != null) {
            float colorAlpha = Math.min(1.0F, Math.max(0.0F, (float) FastColor.ARGB32.alpha(backgroundColor.getColorInt()) / 255.0F));
            if (opacity <= colorAlpha) {
                colorAlpha = opacity;
            }
            graphics.fill(getAbsoluteX(), getAbsoluteY(), getAbsoluteX() + getAbsoluteWidth(), getAbsoluteY() + getAbsoluteHeight(), backgroundColor.getColorIntWithAlpha(colorAlpha));
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Returns the current progress value (from 0.0F to 1.0F).
     * <p>
     * In editor mode, a fixed value of 0.5 is returned.
     */
    public float getCurrentProgress() {
        if (isEditor()) return 0.5F;
        if (progressSource != null) {
            // Replace placeholders and remove spaces.
            String progressString = StringUtils.replace(PlaceholderParser.replacePlaceholders(progressSource), " ", "");
            if (MathUtils.isFloat(progressString)) {
                // If progress is provided as a percentage, convert to 0.0-1.0.
                if (progressValueMode == ProgressValueMode.PERCENTAGE) {
                    return Float.parseFloat(progressString) / 100.0F;
                }
                return Float.parseFloat(progressString);
            }
        }
        return 0.0F;
    }

    @Override
    public int getChildElementAnchorPointX() {
        if (useProgressForElementAnchor) {
            // Anchor on the progress bar's right edge for RIGHT direction, left edge otherwise.
            if (direction == BarDirection.RIGHT) return getProgressX() + getProgressWidth();
            return getProgressX();
        }
        return super.getChildElementAnchorPointX();
    }

    @Override
    public int getChildElementAnchorPointY() {
        if (useProgressForElementAnchor) {
            // Anchor on the progress bar's bottom edge for DOWN direction, top edge otherwise.
            if (direction == BarDirection.DOWN) return getProgressY() + getProgressHeight();
            return getProgressY();
        }
        return super.getChildElementAnchorPointY();
    }

    public int getProgressX() {
        return lastRenderedProgressX;
    }

    public int getProgressY() {
        return lastRenderedProgressY;
    }

    public int getProgressWidth() {
        return lastRenderedProgressWidth;
    }

    public int getProgressHeight() {
        return lastRenderedProgressHeight;
    }

    public enum BarDirection implements LocalizedCycleEnum<BarDirection> {

        LEFT("left"),
        RIGHT("right"),
        UP("up"),
        DOWN("down");

        private final String name;

        BarDirection(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.editor.elements.progress_bar.direction";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull BarDirection[] getValues() {
            return BarDirection.values();
        }

        @Override
        public @Nullable BarDirection getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static BarDirection getByName(@NotNull String name) {
            for (BarDirection d : BarDirection.values()) {
                if (d.name.equals(name)) return d;
            }
            return null;
        }

    }

    public enum ProgressValueMode implements LocalizedCycleEnum<ProgressValueMode> {

        PERCENTAGE("percentage"),
        FLOATING_POINT("float");

        private final String name;

        ProgressValueMode(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getLocalizationKeyBase() {
            return "fancymenu.editor.elements.progress_bar.mode";
        }

        @Override
        public @NotNull Style getValueComponentStyle() {
            return WARNING_TEXT_STYLE.get();
        }

        @Override
        public @NotNull String getName() {
            return this.name;
        }

        @Override
        public @NotNull ProgressValueMode[] getValues() {
            return ProgressValueMode.values();
        }

        @Override
        public @Nullable ProgressValueMode getByNameInternal(@NotNull String name) {
            return getByName(name);
        }

        @Nullable
        public static ProgressValueMode getByName(@NotNull String name) {
            for (ProgressValueMode d : ProgressValueMode.values()) {
                if (d.name.equals(name)) return d;
            }
            return null;
        }

    }

}