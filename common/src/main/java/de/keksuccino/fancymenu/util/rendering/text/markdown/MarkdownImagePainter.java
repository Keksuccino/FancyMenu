package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownImageScaling;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Measures and draws images: the image itself, the frame around it and the caption below it.<br>
 * An image never is wider than the text area, which is as wide as the widest line of the text, so its size depends on all other lines.
 */
final class MarkdownImagePainter {

    /** The width and height of an image whose texture is not loaded. */
    private static final float PLACEHOLDER_SIZE = 10.0F;

    private MarkdownImagePainter() {
    }

    /**
     * Returns the thickness of the frame around an image, which is made of two lines.
     */
    static float getFrameThickness(@NotNull MarkdownRenderer renderer) {
        return renderer.scaling.sized(renderer.style.imageFrameWidth.get()) * 2;
    }

    /**
     * Returns the width the image of the fragment gets drawn with, without its frame.
     */
    static float getImageWidth(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        ITexture texture = fragment.imageSupplier.get();
        if (texture == null) return PLACEHOLDER_SIZE;
        float available = renderer.getRealWidth() - (renderer.scaling.padding() * 2) - (getFrameThickness(renderer) * 2);
        float width = texture.getWidth();
        if (renderer.style.imageScaling.get() == MarkdownImageScaling.WHOLE_MULTIPLE) {
            width *= getWholeMultiple(texture.getWidth(), available, renderer.style.imageMaxScale.get());
        }
        return Math.min(width, available);
    }

    /**
     * Returns the biggest whole factor that keeps an image of the given width inside the available width, which is at least 1.
     */
    static int getWholeMultiple(float imageWidth, float availableWidth, float maxFactor) {
        if (imageWidth <= 0.0F) return 1;
        return (int) Math.max(1.0F, Math.min((float) Math.floor(maxFactor), (float) Math.floor(availableWidth / imageWidth)));
    }

    static float getImageHeight(@NotNull MarkdownTextFragment fragment) {
        ITexture texture = fragment.imageSupplier.get();
        if (texture == null) return PLACEHOLDER_SIZE;
        return texture.getAspectRatio().getAspectRatioHeight((int) getImageWidth(fragment));
    }

    /**
     * Returns the width of the image of the fragment with its frame.
     */
    static float getWidth(@NotNull MarkdownTextFragment fragment) {
        if (fragment.imageSupplier.get() == null) return PLACEHOLDER_SIZE;
        return getImageWidth(fragment) + (getFrameThickness(fragment.parent) * 2);
    }

    /**
     * Returns the height of the image of the fragment with its frame and its caption.
     */
    static float getHeight(@NotNull MarkdownTextFragment fragment) {
        if (fragment.imageSupplier.get() == null) return PLACEHOLDER_SIZE;
        MarkdownRenderer renderer = fragment.parent;
        float height = getImageHeight(fragment) + (getFrameThickness(renderer) * 2);
        Caption caption = fragment.imageCaption;
        if (caption != null) {
            height += renderer.scaling.sized(renderer.style.imageCaptionGap.get()) + (caption.lines.length * getCaptionLineHeight(fragment)) + ((caption.lines.length - 1) * renderer.scaling.lineSpacing());
        }
        return height;
    }

    private static float getCaptionLineHeight(@NotNull MarkdownTextFragment fragment) {
        return fragment.unscaledTextHeight * fragment.parent.scaling.bodyScale();
    }

    /**
     * Wraps the caption of the image of the fragment into the lines that fit the text area. Has to run before the fragment gets measured.
     */
    static void updateCaption(@NotNull MarkdownTextFragment fragment) {
        MarkdownRenderer renderer = fragment.parent;
        String text = fragment.imageAltText;
        float scale = renderer.scaling.bodyScale();
        if (!renderer.style.imageCaption.get() || (text == null) || text.trim().isEmpty() || (fragment.imageSupplier.get() == null) || (scale <= 0.0F)) {
            fragment.imageCaption = null;
            return;
        }
        float available = (renderer.getRealWidth() - (renderer.scaling.padding() * 2)) / scale;
        float spaceWidth = renderer.getUnscaledSpaceWidth();
        List<Component> lines = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        float lineWidth = 0.0F;
        for (String word : text.trim().split(" +")) {
            float wordWidth = renderer.getUnscaledTextWidth(word);
            if ((line.length() > 0) && ((lineWidth + spaceWidth + wordWidth) > available)) {
                lines.add(Component.literal(line.toString()));
                widths.add(lineWidth);
                line.setLength(0);
                lineWidth = 0.0F;
            }
            if (line.length() > 0) {
                line.append(' ');
                lineWidth += spaceWidth;
            }
            line.append(word);
            lineWidth += wordWidth;
        }
        lines.add(Component.literal(line.toString()));
        widths.add(lineWidth);
        float[] widthArray = new float[widths.size()];
        for (int i = 0; i < widthArray.length; i++) widthArray[i] = widths.get(i);
        fragment.imageCaption = new Caption(lines.toArray(new Component[0]), widthArray);
    }

    static void render(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment fragment) {
        ITexture texture = fragment.imageSupplier.get();
        ResourceLocation location = (texture != null) ? texture.getResourceLocation() : null;
        if (location == null) return;
        MarkdownRenderer renderer = fragment.parent;
        MarkdownStyle style = renderer.style;
        float lineThickness = renderer.scaling.sized(style.imageFrameWidth.get());
        float frame = lineThickness * 2;
        float width = getImageWidth(fragment);
        float height = getImageHeight(fragment);
        if (lineThickness > 0.0F) {
            MarkdownPaint.box(graphics, fragment.x, fragment.y, fragment.x + width + (frame * 2), fragment.y + height + (frame * 2), 0.0F, renderer.getRenderColor(style.imageFrameOuterColor));
            MarkdownPaint.box(graphics, fragment.x + lineThickness, fragment.y + lineThickness, fragment.x + width + frame + lineThickness, fragment.y + height + frame + lineThickness, 0.0F, renderer.getRenderColor(style.imageFrameInnerColor));
        }
        // Textured rectangles without a color of their own get drawn with the blend state they find, and everything drawn before them leaves blending disabled
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, renderer.textOpacity);
        RenderingUtils.blitF(graphics, location, fragment.x + frame, fragment.y + frame, 0.0F, 0.0F, width, height, width, height);
        RenderingUtils.resetShaderColor(graphics);
        if (fragment.imageCaption != null) renderCaption(graphics, fragment, fragment.imageCaption, fragment.y + height + (frame * 2) + renderer.scaling.sized(style.imageCaptionGap.get()));
    }

    /**
     * Draws the lines of the caption. They share the text area with the image and are aligned the way the line of the image is.
     */
    private static void renderCaption(@NotNull GuiGraphics graphics, @NotNull MarkdownTextFragment fragment, @NotNull Caption caption, float y) {
        MarkdownRenderer renderer = fragment.parent;
        MarkdownScaling scaling = renderer.scaling;
        float scale = scaling.bodyScale();
        float textAreaX = renderer.x + scaling.padding();
        float textAreaWidth = renderer.getRealWidth() - (scaling.padding() * 2);
        MarkdownRenderer.MarkdownLineAlignment alignment = (fragment.parentLine != null) ? fragment.parentLine.alignment : MarkdownRenderer.MarkdownLineAlignment.LEFT;
        int color = renderer.getRenderColor(renderer.style.imageCaptionColor);
        for (int i = 0; i < caption.lines.length; i++) {
            float free = Math.max(0.0F, textAreaWidth - (caption.widths[i] * scale));
            float offset = (alignment == MarkdownRenderer.MarkdownLineAlignment.CENTERED) ? (free / 2) : (alignment == MarkdownRenderer.MarkdownLineAlignment.RIGHT) ? free : 0.0F;
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(scaling.snapX(textAreaX + scaling.snapOffset(offset)), y, 0.0F);
                graphics.pose().scale(scale, scale, scale);
                renderer.renderText(graphics, caption.lines[i], 0.0F, 0.0F, color, renderer.hasTextShadow());
            } finally {
                graphics.pose().popPose();
            }
            y += getCaptionLineHeight(fragment) + scaling.lineSpacing();
        }
    }

    /**
     * The alternative text of an image, wrapped into the lines it shows as below the image.
     */
    static final class Caption {

        final Component[] lines;
        /** The width of every line, in pixels of text at its normal size. */
        final float[] widths;

        private Caption(@NotNull Component[] lines, @NotNull float[] widths) {
            this.lines = lines;
            this.widths = widths;
        }

    }

}
