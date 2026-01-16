package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

final class SmoothFontAtlas implements AutoCloseable {

    private static final int DEFAULT_ATLAS_SIZE = 1024;
    private static final Logger LOGGER = LogManager.getLogger();

    private final String debugName;
    private final Font awtFont;
    private final FontRenderContext fontRenderContext;
    private final float sdfRange;
    private final int padding;
    private final Int2ObjectOpenHashMap<SmoothFontGlyph> glyphs = new Int2ObjectOpenHashMap<>();

    private NativeImage atlasImage;
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureLocation;
    private int textureId;
    private int logicalWidth;
    private int logicalHeight;
    private volatile int gpuWidth;
    private volatile int gpuHeight;
    private int cursorX;
    private int cursorY;
    private int rowHeight;

    SmoothFontAtlas(@Nonnull SmoothFont parentFont, @Nonnull Font awtFont, @Nonnull FontRenderContext fontRenderContext, float sdfRange, @Nonnull String debugName, int initialSize) {
        this.debugName = Objects.requireNonNull(debugName);
        this.awtFont = Objects.requireNonNull(awtFont);
        this.fontRenderContext = Objects.requireNonNull(fontRenderContext);
        this.sdfRange = Math.max(1.0F, sdfRange);
        this.padding = (int) Math.ceil(this.sdfRange) + 2;

        this.logicalWidth = initialSize;
        this.logicalHeight = initialSize;
        this.gpuWidth = initialSize;
        this.gpuHeight = initialSize;

        this.atlasImage = new NativeImage(NativeImage.Format.RGBA, logicalWidth, logicalHeight, true);
        this.dynamicTexture = new DynamicTexture(atlasImage);
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        this.textureLocation = textureManager.register("fancymenu_smooth_font_" + debugName, dynamicTexture);
        this.textureId = dynamicTexture.getId();
        applyLinearFilter();
    }

    int getWidth() {
        return gpuWidth;
    }

    int getHeight() {
        return gpuHeight;
    }

    float getEffectiveSdfRange() {
        return sdfRange;
    }

    ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    int getTextureId() {
        return textureId;
    }

    SmoothFontGlyph getGlyph(int codepoint) {
        SmoothFontGlyph glyph = glyphs.get(codepoint);
        if (glyph != null) {
            return glyph;
        }
        SmoothFontGlyph created = createGlyph(codepoint);
        glyphs.put(codepoint, created);
        return created;
    }

    private SmoothFontGlyph createGlyph(int codepoint) {
        if (!awtFont.canDisplay(codepoint)) {
            codepoint = '?';
        }
        String glyphText = new String(Character.toChars(codepoint));
        GlyphVector glyphVector = awtFont.createGlyphVector(fontRenderContext, glyphText);
        GlyphMetrics metrics = glyphVector.getGlyphMetrics(0);
        float advance = metrics.getAdvanceX();
        java.awt.Shape outline = glyphVector.getGlyphOutline(0);
        Rectangle2D bounds = outline.getBounds2D();

        if (bounds == null || bounds.isEmpty() || bounds.getWidth() <= 0.0 || bounds.getHeight() <= 0.0) {
            return new SmoothFontGlyph(this, 0, 0, 0, 0, 0.0F, 0.0F, advance, false, true);
        }

        int glyphWidth = (int)Math.ceil(bounds.getWidth() + (padding * 2.0));
        int glyphHeight = (int)Math.ceil(bounds.getHeight() + (padding * 2.0));

        if (glyphWidth <= 0 || glyphHeight <= 0) {
            return new SmoothFontGlyph(this, 0, 0, 0, 0, 0.0F, 0.0F, advance, false, true);
        }

        BufferedImage image = renderGlyphImage(glyphVector, bounds, glyphWidth, glyphHeight, padding, sdfRange);
        byte[] atlasPixels = buildRawRgba(image, glyphWidth, glyphHeight);
        boolean usesTrueSdf = true;

        Rect slot = allocate(glyphWidth, glyphHeight);
        blitToAtlas(slot.x, slot.y, glyphWidth, glyphHeight, atlasPixels);

        // We store absolute pixel coordinates (slot.x, slot.y) in the glyph.
        // The UVs are calculated on demand in SmoothFontGlyph.u0()/v0() using the *current* atlas size.

        float offsetX = (float)bounds.getX() - padding;
        float offsetY = (float)bounds.getY() - padding;

        upload();

        return new SmoothFontGlyph(this, slot.x, slot.y, glyphWidth, glyphHeight, offsetX, offsetY, advance, true, usesTrueSdf);
    }

    private Rect allocate(int width, int height) {
        int spacing = 2;
        int allocWidth = width + spacing;
        int allocHeight = height + spacing;

        if (allocWidth > logicalWidth || allocHeight > logicalHeight) {
            int targetWidth = Math.max(logicalWidth, allocWidth);
            int targetHeight = Math.max(logicalHeight, allocHeight);
            resizeAtlas(targetWidth, targetHeight);
        }

        if (cursorX + allocWidth > logicalWidth) {
            cursorX = 0;
            cursorY += rowHeight;
            rowHeight = 0;
        }

        if (cursorY + allocHeight > logicalHeight) {
            resizeAtlas(logicalWidth * 2, logicalHeight * 2);
        }

        Rect rect = new Rect(cursorX, cursorY);
        cursorX += allocWidth;
        rowHeight = Math.max(rowHeight, allocHeight);
        return rect;
    }

    private void resizeAtlas(int newWidth, int newHeight) {
        int targetWidth = Math.max(newWidth, logicalWidth);
        int targetHeight = Math.max(newHeight, logicalHeight);
        targetWidth = Math.min(targetWidth, RenderSystem.maxSupportedTextureSize());
        targetHeight = Math.min(targetHeight, RenderSystem.maxSupportedTextureSize());

        if (targetWidth == logicalWidth && targetHeight == logicalHeight) {
            return;
        }

        NativeImage newImage = new NativeImage(NativeImage.Format.RGBA, targetWidth, targetHeight, true);
        newImage.copyFrom(atlasImage);
        atlasImage.close();
        atlasImage = newImage;
        logicalWidth = targetWidth;
        logicalHeight = targetHeight;
        dynamicTexture.setPixels(atlasImage);

        final int finalTargetWidth = targetWidth;
        final int finalTargetHeight = targetHeight;

        Runnable uploadTask = () -> {
            TextureUtil.prepareImage(dynamicTexture.getId(), finalTargetWidth, finalTargetHeight);
            dynamicTexture.upload();
            this.gpuWidth = finalTargetWidth;
            this.gpuHeight = finalTargetHeight;
            applyLinearFilter();
        };

        if (RenderSystem.isOnRenderThread()) {
            uploadTask.run();
        } else {
            RenderSystem.recordRenderCall(uploadTask::run);
        }
    }

    private void applyLinearFilter() {
        if (dynamicTexture == null) return;
        Runnable action = () -> dynamicTexture.setFilter(true, false);
        if (RenderSystem.isOnRenderThreadOrInit()) {
            action.run();
        } else {
            RenderSystem.recordRenderCall(action::run);
        }
    }

    private void blitToAtlas(int atlasX, int atlasY, int width, int height, byte[] rgba) {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rgba[index++] & 0xFF;
                int g = rgba[index++] & 0xFF;
                int b = rgba[index++] & 0xFF;
                int a = rgba[index++] & 0xFF;
                int color = FastColor.ABGR32.color(a, b, g, r);
                atlasImage.setPixelRGBA(atlasX + x, atlasY + y, color);
            }
        }
    }

    private void upload() {
        if (RenderSystem.isOnRenderThread()) {
            dynamicTexture.upload();
        } else {
            RenderSystem.recordRenderCall(dynamicTexture::upload);
        }
    }

    private static BufferedImage renderGlyphImage(GlyphVector glyphVector, Rectangle2D bounds, int width, int height, int padding, float sdfRange) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setFont(glyphVector.getFont());
        graphics.setColor(Color.WHITE);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        graphics.translate(padding - bounds.getX(), padding - bounds.getY());
        graphics.drawGlyphVector(glyphVector, 0, 0);
        graphics.dispose();

        // Apply blur to generate SDF-like gradient
        float sigma = sdfRange / 4.0F;
        if (sigma > 0.1F) {
            image = applyBlur(image, sigma);
        }

        return image;
    }

    private static BufferedImage applyBlur(BufferedImage src, float sigma) {
        int radius = (int) Math.ceil(sigma * 3.0F);
        int kernelSize = radius * 2 + 1;
        float[] kernelData = new float[kernelSize * kernelSize];
        float twoSigmaSq = 2.0F * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSq * Math.PI);
        float total = 0.0F;

        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float distanceSq = x * x + y * y;
                float value = (float) Math.exp(-distanceSq / twoSigmaSq) / sigmaRoot;
                kernelData[(y + radius) * kernelSize + (x + radius)] = value;
                total += value;
            }
        }

        for (int i = 0; i < kernelData.length; i++) {
            kernelData[i] /= total;
        }

        java.awt.image.Kernel kernel = new java.awt.image.Kernel(kernelSize, kernelSize, kernelData);
        java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        return op.filter(src, dst);
    }

    private static byte[] buildRawRgba(BufferedImage image, int width, int height) {
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        byte[] rgba = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int a = (argb >>> 24) & 0xFF;
            int dst = i * 4;
            rgba[dst] = (byte)255;
            rgba[dst + 1] = (byte)255;
            rgba[dst + 2] = (byte)255;
            rgba[dst + 3] = (byte)a;
        }
        return rgba;
    }

    private record Rect(int x, int y) {
    }

    @Override
    public void close() {
        glyphs.clear();
        if (dynamicTexture != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
            dynamicTexture = null;
        }
        atlasImage = null;
    }
}