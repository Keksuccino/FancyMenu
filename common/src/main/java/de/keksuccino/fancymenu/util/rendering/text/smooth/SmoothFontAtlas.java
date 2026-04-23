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

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;

final class SmoothFontAtlas implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();

    private final String debugName;
    private final String sourceLabel;
    private final int sourceIndex;
    private final String sizeLabel;
    private final String styleLabel;
    private final Font awtFont;
    private final FontRenderContext fontRenderContext;
    private final float sdfRange;
    private final int padding;
    private final int initialSize;
    private final Int2ObjectOpenHashMap<SmoothFontGlyph> glyphs = new Int2ObjectOpenHashMap<>();

    private NativeImage atlasImage;
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureLocation;
    private int textureId;
    private volatile int logicalWidth;
    private volatile int logicalHeight;
    private int cursorX;
    private int cursorY;
    private int rowHeight;
    private RenderType renderType;

    SmoothFontAtlas(@Nonnull SmoothFont parentFont, @Nonnull Font awtFont, @Nonnull FontRenderContext fontRenderContext, float sdfRange, int padding, @Nonnull String debugName, int initialSize, @Nonnull String sourceLabel, int sourceIndex, @Nonnull String sizeLabel, @Nonnull String styleLabel) {
        this.debugName = Objects.requireNonNull(debugName);
        this.sourceLabel = Objects.requireNonNull(sourceLabel);
        this.sourceIndex = sourceIndex;
        this.sizeLabel = Objects.requireNonNull(sizeLabel);
        this.styleLabel = Objects.requireNonNull(styleLabel);
        this.awtFont = Objects.requireNonNull(awtFont);
        this.fontRenderContext = Objects.requireNonNull(fontRenderContext);
        this.sdfRange = Math.max(0.5F, sdfRange);
        this.padding = Math.max(1, padding);
        this.initialSize = Math.max(1, initialSize);

        this.logicalWidth = this.initialSize;
        this.logicalHeight = this.initialSize;
    }

    RenderType getRenderType() {
        if (renderType == null) {
            ensureInitialized();
            renderType = new RenderType(
                    "fancymenu_smooth_text_" + debugName,
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    () -> {
                        SmoothTextShader.applySdfRange(getEffectiveSdfRange());
                        SmoothTextShader.applyEdge(SmoothTextShader.getResolvedEdge());
                        SmoothTextShader.applySharpness(SmoothTextShader.getResolvedSharpness());
                        RenderSystem.setShader(SmoothTextShader::getShader);

                        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
                        textureManager.getTexture(textureLocation).setFilter(true, false);
                        RenderSystem.setShaderTexture(0, textureLocation);

                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            ) {};
        }
        return renderType;
    }

    int getWidth() {
        return logicalWidth;
    }

    int getHeight() {
        return logicalHeight;
    }

    float getEffectiveSdfRange() {
        return sdfRange;
    }

    ResourceLocation getTextureLocation() {
        ensureInitialized();
        return textureLocation;
    }

    int getTextureId() {
        ensureInitialized();
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
        if (glyphVector.getNumGlyphs() <= 0) {
            return new SmoothFontGlyph(this, 0, 0, 0, 0, 0.0F, 0.0F, 0.0F, false);
        }
        GlyphMetrics metrics = glyphVector.getGlyphMetrics(0);
        float advance = metrics.getAdvanceX();
        Rectangle2D bounds = glyphVector.getGlyphOutline(0).getBounds2D();

        if (bounds == null || bounds.isEmpty() || bounds.getWidth() <= 0.0 || bounds.getHeight() <= 0.0) {
            return new SmoothFontGlyph(this, 0, 0, 0, 0, 0.0F, 0.0F, advance, false);
        }

        double minX = Math.floor(bounds.getX()) - padding;
        double minY = Math.floor(bounds.getY()) - padding;
        double maxX = Math.ceil(bounds.getMaxX()) + padding;
        double maxY = Math.ceil(bounds.getMaxY()) + padding;
        int glyphWidth = (int) Math.ceil(maxX - minX);
        int glyphHeight = (int) Math.ceil(maxY - minY);

        if (glyphWidth <= 0 || glyphHeight <= 0) {
            return new SmoothFontGlyph(this, 0, 0, 0, 0, 0.0F, 0.0F, advance, false);
        }

        ensureInitialized();

        BufferedImage image = renderGlyphImage(glyphVector, minX, minY, glyphWidth, glyphHeight, sdfRange);
        byte[] atlasPixels = buildRawRgba(image, glyphWidth, glyphHeight);
        Rect slot = allocate(glyphWidth, glyphHeight);
        blitToAtlas(slot.x, slot.y, glyphWidth, glyphHeight, atlasPixels);

        // We store absolute pixel coordinates (slot.x, slot.y) in the glyph.
        // The UVs are calculated on demand in SmoothFontGlyph.u0()/v0() using the *current* atlas size.

        float offsetX = (float) minX;
        float offsetY = (float) minY;

        upload(slot.x, slot.y, glyphWidth, glyphHeight);

        return new SmoothFontGlyph(this, slot.x, slot.y, glyphWidth, glyphHeight, offsetX, offsetY, advance, true);
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
        if (dynamicTexture == null) {
            ensureInitialized();
        }
        int targetWidth = Math.max(newWidth, logicalWidth);
        int targetHeight = Math.max(newHeight, logicalHeight);
        targetWidth = Math.min(targetWidth, RenderSystem.maxSupportedTextureSize());
        targetHeight = Math.min(targetHeight, RenderSystem.maxSupportedTextureSize());

        if (targetWidth == logicalWidth && targetHeight == logicalHeight) {
            return;
        }

        LOGGER.info("[FANCYMENU] Resizing smooth font atlas '{}' ({}): {}x{} -> {}x{}", debugName, sizeLabel, logicalWidth, logicalHeight, targetWidth, targetHeight);

        NativeImage newImage = new NativeImage(NativeImage.Format.RGBA, targetWidth, targetHeight, true);
        
        // Manual copy to avoid potential issues with NativeImage.copyFrom
        int oldWidth = atlasImage.getWidth();
        int oldHeight = atlasImage.getHeight();
        for (int y = 0; y < oldHeight; y++) {
            for (int x = 0; x < oldWidth; x++) {
                newImage.setPixelRGBA(x, y, atlasImage.getPixelRGBA(x, y));
            }
        }
        
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
        Runnable action = () -> {
            dynamicTexture.setFilter(true, false);
            dynamicTexture.bind();
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
        };
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

    private void upload(int x, int y, int width, int height) {
        Runnable action = () -> {
            this.dynamicTexture.bind();
            this.atlasImage.upload(0, x, y, x, y, width, height, false, false);
        };
        if (RenderSystem.isOnRenderThread()) {
            action.run();
        } else {
            RenderSystem.recordRenderCall(action::run);
        }
    }

    private static BufferedImage renderGlyphImage(GlyphVector glyphVector, double minX, double minY, int width, int height, float sdfRange) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setFont(glyphVector.getFont());
        graphics.setColor(Color.WHITE);

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        graphics.translate(-minX, -minY);
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
        if (radius <= 0) return src;

        // Generate Gaussian kernel (1D)
        int kernelSize = radius * 2 + 1;
        float[] kernel = new float[kernelSize];
        float twoSigmaSq = 2.0F * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSq * Math.PI);
        float total = 0.0F;

        for (int i = -radius; i <= radius; i++) {
            float distanceSq = i * i;
            float value = (float) Math.exp(-distanceSq / twoSigmaSq) / sigmaRoot;
            kernel[i + radius] = value;
            total += value;
        }
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= total;
        }

        int width = src.getWidth();
        int height = src.getHeight();
        int[] pixels = src.getRGB(0, 0, width, height, null, 0, width);

        // Extract Alpha channel to float array for high-precision processing
        float[] sourceAlpha = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            sourceAlpha[i] = (pixels[i] >> 24) & 0xFF;
        }

        float[] tempAlpha = new float[pixels.length];

        // Horizontal pass: sourceAlpha -> tempAlpha
        convolveFloat1D(sourceAlpha, tempAlpha, width, height, kernel, radius, true);

        // Vertical pass: tempAlpha -> sourceAlpha (reuse source buffer for destination)
        convolveFloat1D(tempAlpha, sourceAlpha, width, height, kernel, radius, false);

        // Pack back to integer pixels
        for (int i = 0; i < pixels.length; i++) {
            int a = Math.min(255, Math.max(0, Math.round(sourceAlpha[i])));
            pixels[i] = (a << 24) | 0xFFFFFF; // White with new alpha
        }

        BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        dst.setRGB(0, 0, width, height, pixels, 0, width);
        return dst;
    }

    private static void convolveFloat1D(float[] src, float[] dest, int width, int height, float[] kernel, int radius, boolean horizontal) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float a = 0.0F;

                for (int k = -radius; k <= radius; k++) {
                    int kIndex = k + radius;
                    int px = horizontal ? x + k : x;
                    int py = horizontal ? y : y + k;

                    // Clamp edges
                    if (px < 0) px = 0;
                    else if (px >= width) px = width - 1;
                    if (py < 0) py = 0;
                    else if (py >= height) py = height - 1;

                    a += src[py * width + px] * kernel[kIndex];
                }

                dest[y * width + x] = a;
            }
        }
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

    private void ensureInitialized() {
        if (dynamicTexture != null) {
            return;
        }
        synchronized (this) {
            if (dynamicTexture != null) {
                return;
            }
            this.logicalWidth = initialSize;
            this.logicalHeight = initialSize;

            this.atlasImage = new NativeImage(NativeImage.Format.RGBA, logicalWidth, logicalHeight, true);
            this.dynamicTexture = new DynamicTexture(atlasImage);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            this.textureLocation = textureManager.register("fancymenu_smooth_font_" + debugName, dynamicTexture);
            this.textureId = dynamicTexture.getId();
            applyLinearFilter();
            LOGGER.info("[FANCYMENU] Smooth font atlas initialized: file='{}', source={}, size={}, style={}, sizePx={}x{}.", sourceLabel, sourceIndex, sizeLabel, styleLabel, logicalWidth, logicalHeight);
        }
    }
}
