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
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class SmoothFontAtlas implements AutoCloseable {

    private static final int DEFAULT_ATLAS_SIZE = 512;
    private static final float INF = 1.0E20F;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG_LOG = Boolean.getBoolean("fancymenu.debugSmoothFontLog");
    private static final boolean DEBUG_DUMP = Boolean.getBoolean("fancymenu.debugSmoothFontDump");
    private static final boolean DEBUG_USE_RAW_ALPHA = Boolean.getBoolean("fancymenu.debugSmoothFontRawAlpha");
    private static final int ALPHA_THRESHOLD = Math.max(1, Integer.getInteger("fancymenu.smoothFontAlphaThreshold", 1));
    private static final int DEBUG_DUMP_LIMIT = 8;
    private static int debugDumpCount;

    private final String debugName;
    private final Font awtFont;
    private final FontRenderContext fontRenderContext;
    private final float sdfRange;
    private final Int2ObjectOpenHashMap<SmoothFontGlyph> glyphs = new Int2ObjectOpenHashMap<>();

    private NativeImage atlasImage;
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureLocation;
    private int textureId;
    private int atlasWidth;
    private int atlasHeight;
    private int cursorX;
    private int cursorY;
    private int rowHeight;

    SmoothFontAtlas(@Nonnull SmoothFont parentFont, @Nonnull Font awtFont, @Nonnull FontRenderContext fontRenderContext, float sdfRange, @Nonnull String debugName) {
        this.debugName = Objects.requireNonNull(debugName);
        this.awtFont = Objects.requireNonNull(awtFont);
        this.fontRenderContext = Objects.requireNonNull(fontRenderContext);
        this.sdfRange = Math.max(1.0F, sdfRange);
        this.atlasWidth = DEFAULT_ATLAS_SIZE;
        this.atlasHeight = DEFAULT_ATLAS_SIZE;
        this.atlasImage = new NativeImage(NativeImage.Format.RGBA, atlasWidth, atlasHeight, true);
        this.dynamicTexture = new DynamicTexture(atlasImage);
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        this.textureLocation = textureManager.register("fancymenu_smooth_font_" + debugName, dynamicTexture);
        this.textureId = dynamicTexture.getId();
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
        GlyphVector glyphVector = awtFont.createGlyphVector(fontRenderContext, new int[]{codepoint});
        GlyphMetrics metrics = glyphVector.getGlyphMetrics(0);
        float advance = metrics.getAdvanceX();
        Rectangle bounds = glyphVector.getGlyphPixelBounds(0, fontRenderContext, 0, 0);
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) {
            return new SmoothFontGlyph(this, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, 0.0F, advance, false);
        }
        int padding = Math.max(2, (int)Math.ceil(sdfRange));
        int glyphWidth = bounds.width + (padding * 2);
        int glyphHeight = bounds.height + (padding * 2);

        BufferedImage image = new BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillRect(0, 0, glyphWidth, glyphHeight);
        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setFont(awtFont);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.translate(padding - bounds.x, padding - bounds.y);
        graphics.drawGlyphVector(glyphVector, 0, 0);
        graphics.dispose();

        AlphaStats rawStats = DEBUG_LOG || DEBUG_DUMP ? AlphaStats.fromImage(image) : null;
        byte[] sdfAlpha;
        if (DEBUG_USE_RAW_ALPHA) {
            sdfAlpha = buildRawAlpha(image, glyphWidth, glyphHeight);
        } else {
            sdfAlpha = buildSdf(image, glyphWidth, glyphHeight, sdfRange);
        }
        AlphaStats sdfStats = DEBUG_LOG || DEBUG_DUMP ? AlphaStats.fromAlphaBytes(sdfAlpha) : null;

        if (DEBUG_LOG && rawStats != null && sdfStats != null) {
            String glyphLabel = codepoint == 0 ? "?" : new String(Character.toChars(codepoint));
            LOGGER.info("[FANCYMENU] SmoothFontAtlas {} U+{} '{}' raw[min={}, max={}, zero={}, nonZero={}] sdf[min={}, max={}, zero={}, nonZero={}] threshold={}",
                    debugName,
                    String.format("%04X", codepoint),
                    glyphLabel,
                    rawStats.minAlpha,
                    rawStats.maxAlpha,
                    rawStats.zeroCount,
                    rawStats.nonZeroCount,
                    sdfStats.minAlpha,
                    sdfStats.maxAlpha,
                    sdfStats.zeroCount,
                    sdfStats.nonZeroCount,
                    ALPHA_THRESHOLD
            );
        }
        if (DEBUG_DUMP && rawStats != null && sdfStats != null && debugDumpCount < DEBUG_DUMP_LIMIT) {
            debugDumpCount++;
            dumpDebugImages(debugName, image, sdfAlpha, glyphWidth, glyphHeight, codepoint);
        }
        Rect slot = allocate(glyphWidth, glyphHeight);
        blitToAtlas(slot.x, slot.y, glyphWidth, glyphHeight, sdfAlpha);

        float u0 = (float)slot.x / (float)atlasWidth;
        float v0 = (float)slot.y / (float)atlasHeight;
        float u1 = (float)(slot.x + glyphWidth) / (float)atlasWidth;
        float v1 = (float)(slot.y + glyphHeight) / (float)atlasHeight;
        float offsetX = bounds.x - padding;
        float offsetY = bounds.y - padding;

        upload();
        return new SmoothFontGlyph(this, u0, v0, u1, v1, glyphWidth, glyphHeight, offsetX, offsetY, advance, true);
    }

    private Rect allocate(int width, int height) {
        if (width > atlasWidth || height > atlasHeight) {
            int targetWidth = atlasWidth;
            int targetHeight = atlasHeight;
            while (width > targetWidth) {
                targetWidth *= 2;
            }
            while (height > targetHeight) {
                targetHeight *= 2;
            }
            resizeAtlas(targetWidth, targetHeight);
        }
        if (cursorX + width > atlasWidth) {
            cursorX = 0;
            cursorY += rowHeight;
            rowHeight = 0;
        }
        if (cursorY + height > atlasHeight) {
            int targetWidth = atlasWidth;
            int targetHeight = atlasHeight;
            while (cursorY + height > targetHeight) {
                targetHeight *= 2;
            }
            while (cursorX + width > targetWidth) {
                targetWidth *= 2;
            }
            resizeAtlas(targetWidth, targetHeight);
        }
        Rect rect = new Rect(cursorX, cursorY);
        cursorX += width + 1;
        rowHeight = Math.max(rowHeight, height + 1);
        return rect;
    }

    private void resizeAtlas(int newWidth, int newHeight) {
        int targetWidth = Math.max(newWidth, atlasWidth);
        int targetHeight = Math.max(newHeight, atlasHeight);
        if (targetWidth == atlasWidth && targetHeight == atlasHeight) {
            return;
        }
        NativeImage newImage = new NativeImage(NativeImage.Format.RGBA, targetWidth, targetHeight, true);
        newImage.copyFrom(atlasImage);
        atlasImage.close();
        atlasImage = newImage;
        atlasWidth = targetWidth;
        atlasHeight = targetHeight;
        dynamicTexture.setPixels(atlasImage);
        if (RenderSystem.isOnRenderThread()) {
            TextureUtil.prepareImage(dynamicTexture.getId(), atlasWidth, atlasHeight);
            dynamicTexture.upload();
        } else {
            RenderSystem.recordRenderCall(() -> {
                TextureUtil.prepareImage(dynamicTexture.getId(), atlasWidth, atlasHeight);
                dynamicTexture.upload();
            });
        }
    }

    private void blitToAtlas(int atlasX, int atlasY, int width, int height, byte[] alpha) {
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = alpha[index++] & 0xFF;
                int color = FastColor.ABGR32.color(a, 255, 255, 255);
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

    private static byte[] buildSdf(BufferedImage image, int width, int height, float range) {
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        boolean[] inside = new boolean[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int coverage = coverageFromPixel(pixels[i]);
            inside[i] = coverage >= ALPHA_THRESHOLD;
        }

        float[] distToInside = computeDistance(inside, width, height, false);
        float[] distToOutside = computeDistance(inside, width, height, true);

        byte[] sdf = new byte[width * height];
        float invRange = 1.0F / (2.0F * Math.max(1.0F, range));
        for (int i = 0; i < sdf.length; i++) {
            float dist = (float)Math.sqrt(distToOutside[i]) - (float)Math.sqrt(distToInside[i]);
            float value = 0.5F + (dist * invRange);
            int alpha = (int)(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
            sdf[i] = (byte)alpha;
        }
        return sdf;
    }

    private static byte[] buildRawAlpha(BufferedImage image, int width, int height) {
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);
        byte[] alpha = new byte[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            alpha[i] = (byte)coverageFromPixel(pixels[i]);
        }
        return alpha;
    }

    private static void dumpDebugImages(String debugName, BufferedImage rawImage, byte[] sdfAlpha, int width, int height, int codepoint) {
        Path outputDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("fancymenu")
                .resolve("debug")
                .resolve("smooth_font");
        try {
            Files.createDirectories(outputDir);
            String baseName = debugName + "_glyph_" + String.format("%04X", codepoint);
            Path rawPath = outputDir.resolve(debugFileName(baseName, "raw"));
            ImageIO.write(rawImage, "png", rawPath.toFile());
            BufferedImage sdfImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int index = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int a = sdfAlpha[index++] & 0xFF;
                    int argb = (0xFF << 24) | (a << 16) | (a << 8) | a;
                    sdfImage.setRGB(x, y, argb);
                }
            }
            Path sdfPath = outputDir.resolve(debugFileName(baseName, "sdf"));
            ImageIO.write(sdfImage, "png", sdfPath.toFile());
            LOGGER.info("[FANCYMENU] SmoothFont debug dump written: {} and {}", rawPath, sdfPath);
        } catch (IOException ex) {
            LOGGER.warn("[FANCYMENU] SmoothFont debug dump failed", ex);
        }
    }

    private static String debugFileName(String baseName, String suffix) {
        String sanitized = baseName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return sanitized + "_" + suffix + ".png";
    }

    private static final class AlphaStats {
        private final int minAlpha;
        private final int maxAlpha;
        private final int zeroCount;
        private final int nonZeroCount;

        private AlphaStats(int minAlpha, int maxAlpha, int zeroCount, int nonZeroCount) {
            this.minAlpha = minAlpha;
            this.maxAlpha = maxAlpha;
            this.zeroCount = zeroCount;
            this.nonZeroCount = nonZeroCount;
        }

        private static AlphaStats fromImage(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            int min = 255;
            int max = 0;
            int zero = 0;
            int nonZero = 0;
            for (int pixel : pixels) {
                int coverage = coverageFromPixel(pixel);
                min = Math.min(min, coverage);
                max = Math.max(max, coverage);
                if (coverage == 0) {
                    zero++;
                } else {
                    nonZero++;
                }
            }
            return new AlphaStats(min, max, zero, nonZero);
        }

        private static AlphaStats fromAlphaBytes(byte[] alpha) {
            int min = 255;
            int max = 0;
            int zero = 0;
            int nonZero = 0;
            for (byte value : alpha) {
                int a = value & 0xFF;
                min = Math.min(min, a);
                max = Math.max(max, a);
                if (a == 0) {
                    zero++;
                } else {
                    nonZero++;
                }
            }
            return new AlphaStats(min, max, zero, nonZero);
        }
    }

    private static int coverageFromPixel(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return Math.max(alpha, Math.max(red, Math.max(green, blue)));
    }

    private static float[] computeDistance(boolean[] inside, int width, int height, boolean invert) {
        float[] f = new float[width * height];
        for (int i = 0; i < inside.length; i++) {
            boolean isInside = inside[i];
            boolean zero = invert ? !isInside : isInside;
            f[i] = zero ? 0.0F : INF;
        }
        float[] tmp = new float[width * height];
        float[] column = new float[Math.max(width, height)];
        float[] columnOut = new float[Math.max(width, height)];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                column[y] = f[y * width + x];
            }
            edt1d(column, columnOut, height);
            for (int y = 0; y < height; y++) {
                tmp[y * width + x] = columnOut[y];
            }
        }

        float[] result = new float[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                column[x] = tmp[y * width + x];
            }
            edt1d(column, columnOut, width);
            for (int x = 0; x < width; x++) {
                result[y * width + x] = columnOut[x];
            }
        }
        return result;
    }

    private static void edt1d(float[] f, float[] d, int n) {
        int[] v = new int[n];
        float[] z = new float[n + 1];
        int k = 0;
        v[0] = 0;
        z[0] = -INF;
        z[1] = INF;

        for (int q = 1; q < n; q++) {
            float s = ((f[q] + (q * (float)q)) - (f[v[k]] + (v[k] * (float)v[k]))) / (2.0F * (q - v[k]));
            while (s <= z[k]) {
                k--;
                s = ((f[q] + (q * (float)q)) - (f[v[k]] + (v[k] * (float)v[k]))) / (2.0F * (q - v[k]));
            }
            k++;
            v[k] = q;
            z[k] = s;
            z[k + 1] = INF;
        }

        k = 0;
        for (int q = 0; q < n; q++) {
            while (z[k + 1] < q) {
                k++;
            }
            float dx = q - v[k];
            d[q] = dx * dx + f[v[k]];
        }
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

    private record Rect(int x, int y) {
    }

}
