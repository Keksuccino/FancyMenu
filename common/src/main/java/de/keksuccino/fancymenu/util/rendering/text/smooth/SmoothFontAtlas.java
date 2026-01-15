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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.msdfgen.MSDFGen;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenMultichannelConfig;
import org.lwjgl.util.msdfgen.MSDFGenRange;
import org.lwjgl.util.msdfgen.MSDFGenTransform;
import org.lwjgl.util.msdfgen.MSDFGenVector2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
    private static final boolean DEBUG_DUMP_ATLAS = Boolean.getBoolean("fancymenu.debugSmoothFontAtlasDump");
    private static final boolean DEBUG_DUMP_MSDF = Boolean.getBoolean("fancymenu.debugSmoothFontDumpMsdf");
    private static final boolean DISABLE_MTSDF = Boolean.getBoolean("fancymenu.debugSmoothFontDisableMtsdf");
    private static final int ALPHA_THRESHOLD = Math.max(1, Integer.getInteger("fancymenu.smoothFontAlphaThreshold", 128));
    private static final float MSDF_EDGE_THRESHOLD = 3.0F;
    private static boolean msdfAvailable = true;
    private static boolean msdfUnavailableLogged;
    private static final int DEBUG_DUMP_LIMIT = 8;
    private static int debugDumpCount;
    private static int debugAtlasDumpCount;

    private final String debugName;
    private final Font awtFont;
    private final FontRenderContext fontRenderContext;
    private final float sdfRange;
    private float msdfRangeScale = 1.0F;
    private boolean msdfRangeScaleInitialized;
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
        applyLinearFilter();
    }

    float getEffectiveSdfRange() {
        float scale = msdfRangeScaleInitialized ? msdfRangeScale : 1.0F;
        float effective = sdfRange * scale;
        return Mth.clamp(effective, 0.5F, sdfRange);
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
            return new SmoothFontGlyph(this, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, 0.0F, advance, false, true);
        }
        int padding = Math.max(2, (int)Math.ceil(sdfRange));
        int glyphWidth = (int)Math.ceil(bounds.getWidth() + (padding * 2.0));
        int glyphHeight = (int)Math.ceil(bounds.getHeight() + (padding * 2.0));
        if (glyphWidth <= 0 || glyphHeight <= 0) {
            return new SmoothFontGlyph(this, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, 0.0F, advance, false, true);
        }

        BufferedImage image = null;
        AlphaStats rawStats = null;
        if (DEBUG_USE_RAW_ALPHA || DEBUG_LOG || DEBUG_DUMP) {
            image = renderGlyphImage(glyphVector, bounds, glyphWidth, glyphHeight, padding);
            rawStats = AlphaStats.fromImage(image);
        }

        byte[] atlasPixels;
        boolean usesTrueSdf;
        if (DEBUG_USE_RAW_ALPHA) {
            if (image == null) {
                image = renderGlyphImage(glyphVector, bounds, glyphWidth, glyphHeight, padding);
            }
            atlasPixels = image != null ? buildRawRgba(image, glyphWidth, glyphHeight) : new byte[glyphWidth * glyphHeight * 4];
            usesTrueSdf = true;
        } else {
            GeneratedBitmap msdf = buildMsdf(outline, bounds, glyphWidth, glyphHeight, padding, sdfRange);
            if (msdf == null || msdf.pixels == null) {
                if (image == null) {
                    image = renderGlyphImage(glyphVector, bounds, glyphWidth, glyphHeight, padding);
                }
                if (rawStats == null && image != null && (DEBUG_LOG || DEBUG_DUMP)) {
                    rawStats = AlphaStats.fromImage(image);
                }
                byte[] fallbackSdf = image != null ? buildSdf(image, glyphWidth, glyphHeight, sdfRange) : null;
                atlasPixels = fallbackSdf != null ? expandAlphaToRgba(fallbackSdf) : new byte[glyphWidth * glyphHeight * 4];
                usesTrueSdf = true;
            } else {
                atlasPixels = msdf.pixels;
                usesTrueSdf = msdf.usesTrueSdf;
            }
        }

        AlphaStats sdfStats = DEBUG_LOG || DEBUG_DUMP ? AlphaStats.fromChannel(atlasPixels, 3) : null;

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
            if (!DEBUG_USE_RAW_ALPHA && msdfAvailable) {
                logMsdfStats(debugName, codepoint, atlasPixels);
            }
        }
        if ((DEBUG_DUMP || DEBUG_DUMP_MSDF) && rawStats != null && sdfStats != null && debugDumpCount < DEBUG_DUMP_LIMIT) {
            debugDumpCount++;
            dumpDebugImages(debugName, image, atlasPixels, glyphWidth, glyphHeight, codepoint);
        }
        Rect slot = allocate(glyphWidth, glyphHeight);
        blitToAtlas(slot.x, slot.y, glyphWidth, glyphHeight, atlasPixels);

        float u0 = (float)slot.x / (float)atlasWidth;
        float v0 = (float)slot.y / (float)atlasHeight;
        float u1 = (float)(slot.x + glyphWidth) / (float)atlasWidth;
        float v1 = (float)(slot.y + glyphHeight) / (float)atlasHeight;
        float offsetX = (float)bounds.getX() - padding;
        float offsetY = (float)bounds.getY() - padding;

        upload();
        if (DEBUG_DUMP_ATLAS && debugAtlasDumpCount < DEBUG_DUMP_LIMIT) {
            debugAtlasDumpCount++;
            dumpAtlasImage(debugName, atlasImage, debugAtlasDumpCount);
        }
        return new SmoothFontGlyph(this, u0, v0, u1, v1, glyphWidth, glyphHeight, offsetX, offsetY, advance, true, usesTrueSdf);
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
            applyLinearFilter();
        } else {
            RenderSystem.recordRenderCall(() -> {
                TextureUtil.prepareImage(dynamicTexture.getId(), atlasWidth, atlasHeight);
                dynamicTexture.upload();
                applyLinearFilter();
            });
        }
    }

    private void applyLinearFilter() {
        if (dynamicTexture == null) {
            return;
        }
        if (RenderSystem.isOnRenderThreadOrInit()) {
            dynamicTexture.setFilter(true, false);
        } else {
            RenderSystem.recordRenderCall(() -> dynamicTexture.setFilter(true, false));
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

    private static BufferedImage renderGlyphImage(GlyphVector glyphVector, Rectangle2D bounds, int width, int height, int padding) {
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
        graphics.translate(padding - bounds.getX(), padding - bounds.getY());
        graphics.drawGlyphVector(glyphVector, 0, 0);
        graphics.dispose();
        return image;
    }

    private GeneratedBitmap buildMsdf(java.awt.Shape outline, Rectangle2D bounds, int width, int height, int padding, float range) {
        if (!msdfAvailable) {
            return null;
        }
        try {
            long shape = buildShapeFromPath(outline.getPathIterator(null));
            if (shape == MemoryUtil.NULL) {
                return null;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (DEBUG_LOG) {
                    PointerBuffer contourCountPtr = stack.mallocPointer(1);
                    PointerBuffer edgeCountPtr = stack.mallocPointer(1);
                    int contourResult = MSDFGen.msdf_shape_get_contour_count(shape, contourCountPtr);
                    int edgeResult = MSDFGen.msdf_shape_get_edge_count(shape, edgeCountPtr);
                    long contours = contourCountPtr.get(0);
                    long edges = edgeCountPtr.get(0);
                    LOGGER.info("[FANCYMENU] SmoothFontAtlas {} MSDF shape stats: contours={} edges={} (result={}, {})",
                            debugName,
                            contours,
                            edges,
                            contourResult,
                            edgeResult
                    );
                }
                MSDFGen.msdf_shape_orient_contours(shape);
                MSDFGen.msdf_shape_edge_colors_ink_trap(shape, MSDF_EDGE_THRESHOLD);

                MSDFGenTransform transform = MSDFGenTransform.calloc(stack);
                transform.scale().x(1.0).y(1.0);
                transform.translation().x(padding - bounds.getX()).y(padding - bounds.getY());
                MSDFGenRange rangeStruct = transform.distance_mapping();
                rangeStruct.lower(-range);
                rangeStruct.upper(range);

                MSDFGenBitmap bitmap = MSDFGenBitmap.malloc(stack);
                MSDFGenMultichannelConfig config = MSDFGenMultichannelConfig.calloc(stack);
                config.overlap_support(MSDFGen.MSDF_TRUE);
                config.mode(MSDFGen.MSDF_ERROR_CORRECTION_MODE_EDGE_PRIORITY);
                config.distance_check_mode(MSDFGen.MSDF_DISTANCE_CHECK_MODE_ALWAYS);
                config.min_deviation_ratio(0.1);
                config.min_improve_ratio(0.1);

                int allocResult;
                int genResult;
                boolean usesTrueSdf;
                if (DISABLE_MTSDF) {
                    allocResult = MSDFGen.msdf_bitmap_alloc(MSDFGen.MSDF_BITMAP_TYPE_MSDF, width, height, bitmap);
                    if (allocResult != MSDFGen.MSDF_SUCCESS) {
                        LOGGER.warn("[FANCYMENU] MSDF bitmap allocation failed (code={})", allocResult);
                        return null;
                    }
                    genResult = MSDFGen.msdf_generate_msdf_with_config(bitmap, shape, transform, config);
                    usesTrueSdf = false;
                } else {
                    allocResult = MSDFGen.msdf_bitmap_alloc(MSDFGen.MSDF_BITMAP_TYPE_MTSDF, width, height, bitmap);
                    if (allocResult != MSDFGen.MSDF_SUCCESS) {
                        LOGGER.warn("[FANCYMENU] MTSDF bitmap allocation failed (code={})", allocResult);
                        return null;
                    }
                    genResult = MSDFGen.msdf_generate_mtsdf_with_config(bitmap, shape, transform, config);
                    usesTrueSdf = true;
                }

                if (genResult != MSDFGen.MSDF_SUCCESS) {
                    LOGGER.warn("[FANCYMENU] MSDF generation failed (code={})", genResult);
                    MSDFGen.msdf_bitmap_free(bitmap);
                    return null;
                }
                MsdfBitmapData data = readBitmapPixels(bitmap, width, height, range);
                MSDFGen.msdf_bitmap_free(bitmap);
                if (data == null || data.rgba == null) {
                    return null;
                }
                boolean trueSdf = usesTrueSdf || data.channels >= 4;
                updateMsdfRangeScale(data.rgba, trueSdf);
                return new GeneratedBitmap(data.rgba, trueSdf);
            } finally {
                MSDFGen.msdf_shape_free(shape);
            }
        } catch (Throwable ex) {
            msdfAvailable = false;
            if (!msdfUnavailableLogged) {
                LOGGER.warn("[FANCYMENU] MSDF unavailable, falling back to legacy SDF.", ex);
                msdfUnavailableLogged = true;
            }
            return null;
        }
    }

    private MsdfBitmapData readBitmapPixels(MSDFGenBitmap bitmap, int width, int height, float range) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pixelPtr = stack.mallocPointer(1);
            int pixelResult = MSDFGen.msdf_bitmap_get_pixels(bitmap, pixelPtr);
            long pixels = pixelPtr.get(0);
            PointerBuffer sizePtr = stack.mallocPointer(1);
            int sizeResult = MSDFGen.msdf_bitmap_get_byte_size(bitmap, sizePtr);
            long byteSize = sizePtr.get(0);
            IntBuffer channelsBuf = stack.mallocInt(1);
            int channelResult = MSDFGen.msdf_bitmap_get_channel_count(bitmap, channelsBuf);
            int channels = channelsBuf.get(0);
            if (pixelResult != MSDFGen.MSDF_SUCCESS || sizeResult != MSDFGen.MSDF_SUCCESS || channelResult != MSDFGen.MSDF_SUCCESS) {
                LOGGER.warn("[FANCYMENU] MSDF bitmap query failed (pixels={}, size={}, channels={})", pixelResult, sizeResult, channelResult);
                return null;
            }
            if (pixels == MemoryUtil.NULL || byteSize <= 0 || channels <= 0) {
                return null;
            }

            int pixelCount = width * height;
            int bytesPerChannel = (int)(byteSize / (long)(pixelCount * channels));
            ByteBuffer buffer = MemoryUtil.memByteBuffer(pixels, (int)byteSize).order(ByteOrder.nativeOrder());
            byte[] rgba = new byte[pixelCount * 4];

            if (DEBUG_LOG) {
                LOGGER.info("[FANCYMENU] MSDF bitmap stats: bytesPerChannel={} channels={} byteSize={}", bytesPerChannel, channels, byteSize);
            }

            if (bytesPerChannel == 1) {
                for (int i = 0; i < pixelCount; i++) {
                    int srcBase = i * channels;
                    int dstBase = i * 4;
                    byte r = buffer.get(srcBase);
                    byte g = channels > 1 ? buffer.get(srcBase + 1) : r;
                    byte b = channels > 2 ? buffer.get(srcBase + 2) : r;
                    byte a = channels > 3 ? buffer.get(srcBase + 3) : (byte)0xFF;
                    rgba[dstBase] = r;
                    rgba[dstBase + 1] = g;
                    rgba[dstBase + 2] = b;
                    rgba[dstBase + 3] = channels > 3 ? a : (byte)0xFF;
                }
                return new MsdfBitmapData(rgba, channels);
            }

            if (bytesPerChannel == 4) {
                FloatBuffer floats = buffer.asFloatBuffer();
                float min = Float.POSITIVE_INFINITY;
                float max = Float.NEGATIVE_INFINITY;
                for (int i = 0; i < pixelCount * channels; i++) {
                    float value = floats.get(i);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
                boolean needsNormalization = min < -0.01f || max > 1.01f;
                for (int i = 0; i < pixelCount; i++) {
                    int srcBase = i * channels;
                    int dstBase = i * 4;
                    float r = floats.get(srcBase);
                    float g = channels > 1 ? floats.get(srcBase + 1) : r;
                    float b = channels > 2 ? floats.get(srcBase + 2) : r;
                    float a = channels > 3 ? floats.get(srcBase + 3) : 1.0F;
                    if (needsNormalization && range > 0.0f) {
                        float invRange = 1.0f / (2.0f * range);
                        r = r * invRange + 0.5f;
                        g = g * invRange + 0.5f;
                        b = b * invRange + 0.5f;
                        a = a * invRange + 0.5f;
                    }
                    rgba[dstBase] = (byte)Math.round(Mth.clamp(r, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 1] = (byte)Math.round(Mth.clamp(g, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 2] = (byte)Math.round(Mth.clamp(b, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 3] = (byte)Math.round(Mth.clamp(channels > 3 ? a : 1.0F, 0.0F, 1.0F) * 255.0F);
                }
                return new MsdfBitmapData(rgba, channels);
            }

            LOGGER.warn("[FANCYMENU] Unsupported MSDF pixel format: channels={} bytesPerChannel={}", channels, bytesPerChannel);
            return null;
        }
    }

    private static long buildShapeFromPath(PathIterator iterator) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer shapePtr = stack.mallocPointer(1);
            int shapeResult = MSDFGen.msdf_shape_alloc(shapePtr);
            if (shapeResult != MSDFGen.MSDF_SUCCESS) {
                return MemoryUtil.NULL;
            }
            long shape = shapePtr.get(0);
            long contour = MemoryUtil.NULL;
            boolean contourHasEdges = false;
            boolean hasAnyEdges = false;
            double startX = 0.0;
            double startY = 0.0;
            double lastX = 0.0;
            double lastY = 0.0;
            double[] coords = new double[6];

            while (!iterator.isDone()) {
                int seg = iterator.currentSegment(coords);
                switch (seg) {
                    case PathIterator.SEG_MOVETO -> {
                        contour = closeContour(shape, contour, contourHasEdges, startX, startY, lastX, lastY);
                        contourHasEdges = false;
                        PointerBuffer contourPtr = stack.mallocPointer(1);
                        int contourResult = MSDFGen.msdf_shape_add_contour(shape, contourPtr);
                        contour = contourResult == MSDFGen.MSDF_SUCCESS ? contourPtr.get(0) : MemoryUtil.NULL;
                        startX = coords[0];
                        startY = coords[1];
                        lastX = startX;
                        lastY = startY;
                    }
                    case PathIterator.SEG_LINETO -> {
                        if (contour != MemoryUtil.NULL) {
                            addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_LINEAR, lastX, lastY, coords[0], coords[1]);
                            contourHasEdges = true;
                            hasAnyEdges = true;
                        }
                        lastX = coords[0];
                        lastY = coords[1];
                    }
                    case PathIterator.SEG_QUADTO -> {
                        if (contour != MemoryUtil.NULL) {
                            addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_QUADRATIC, lastX, lastY, coords[0], coords[1], coords[2], coords[3]);
                            contourHasEdges = true;
                            hasAnyEdges = true;
                        }
                        lastX = coords[2];
                        lastY = coords[3];
                    }
                    case PathIterator.SEG_CUBICTO -> {
                        if (contour != MemoryUtil.NULL) {
                            addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_CUBIC, lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                            contourHasEdges = true;
                            hasAnyEdges = true;
                        }
                        lastX = coords[4];
                        lastY = coords[5];
                    }
                    case PathIterator.SEG_CLOSE -> {
                        contour = closeContour(shape, contour, contourHasEdges, startX, startY, lastX, lastY);
                        contourHasEdges = false;
                    }
                    default -> {
                    }
                }
                iterator.next();
            }
            closeContour(shape, contour, contourHasEdges, startX, startY, lastX, lastY);
            if (!hasAnyEdges) {
                MSDFGen.msdf_shape_free(shape);
                return MemoryUtil.NULL;
            }
            return shape;
        }
    }

    private static long closeContour(long shape, long contour, boolean hasEdges, double startX, double startY, double lastX, double lastY) {
        if (contour == MemoryUtil.NULL) {
            return contour;
        }
        if (hasEdges && (Math.abs(lastX - startX) > 0.0001 || Math.abs(lastY - startY) > 0.0001)) {
            addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_LINEAR, lastX, lastY, startX, startY);
        }
        if (!hasEdges) {
            MSDFGen.msdf_shape_remove_contour(shape, contour);
            MSDFGen.msdf_contour_free(contour);
        }
        return MemoryUtil.NULL;
    }

    private static void addSegment(long contour, int type, double... coords) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer segPtr = stack.mallocPointer(1);
            int segResult = MSDFGen.msdf_segment_alloc(type, segPtr);
            if (segResult != MSDFGen.MSDF_SUCCESS) {
                return;
            }
            long segment = segPtr.get(0);
            MSDFGenVector2 vec = MSDFGenVector2.calloc(stack);
            if (type == MSDFGen.MSDF_SEGMENT_TYPE_LINEAR && coords.length >= 4) {
                setPoint(segment, 0, coords[0], coords[1], vec);
                setPoint(segment, 1, coords[2], coords[3], vec);
            } else if (type == MSDFGen.MSDF_SEGMENT_TYPE_QUADRATIC && coords.length >= 6) {
                setPoint(segment, 0, coords[0], coords[1], vec);
                setPoint(segment, 1, coords[2], coords[3], vec);
                setPoint(segment, 2, coords[4], coords[5], vec);
            } else if (type == MSDFGen.MSDF_SEGMENT_TYPE_CUBIC && coords.length >= 8) {
                setPoint(segment, 0, coords[0], coords[1], vec);
                setPoint(segment, 1, coords[2], coords[3], vec);
                setPoint(segment, 2, coords[4], coords[5], vec);
                setPoint(segment, 3, coords[6], coords[7], vec);
            } else {
                MSDFGen.msdf_segment_free(segment);
                return;
            }
            MSDFGen.msdf_contour_add_edge(contour, segment);
        }
    }

    private static void setPoint(long segment, int index, double x, double y, MSDFGenVector2 vec) {
        vec.x(x);
        vec.y(y);
        MSDFGen.msdf_segment_set_point(segment, index, vec);
    }


    private static byte[] expandAlphaToRgba(byte[] alpha) {
        byte[] rgba = new byte[alpha.length * 4];
        int src = 0;
        int dst = 0;
        while (src < alpha.length) {
            byte a = alpha[src++];
            rgba[dst++] = a;
            rgba[dst++] = a;
            rgba[dst++] = a;
            rgba[dst++] = a;
        }
        return rgba;
    }

    private static byte[] buildRawRgba(BufferedImage image, int width, int height) {
        return expandAlphaToRgba(buildRawAlpha(image, width, height));
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

    private void updateMsdfRangeScale(byte[] rgba, boolean useAlpha) {
        int min = 255;
        int max = 0;
        if (useAlpha) {
            for (int i = 3; i < rgba.length; i += 4) {
                int value = rgba[i] & 0xFF;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        } else {
            for (int i = 0; i < rgba.length; i += 4) {
                int r = rgba[i] & 0xFF;
                int g = rgba[i + 1] & 0xFF;
                int b = rgba[i + 2] & 0xFF;
                int med = median(r, g, b);
                min = Math.min(min, med);
                max = Math.max(max, med);
            }
        }
        int maxDeviation = Math.max(Math.abs(max - 128), Math.abs(min - 128));
        float scale = maxDeviation / 128.0F;
        if (scale <= 0.01F) {
            return;
        }
        if (!msdfRangeScaleInitialized) {
            msdfRangeScale = scale;
            msdfRangeScaleInitialized = true;
        } else {
            msdfRangeScale = Math.max(msdfRangeScale, scale);
        }
        if (DEBUG_LOG) {
            LOGGER.info("[FANCYMENU] SmoothFontAtlas {} MSDF range scale updated: min={} max={} scale={}",
                    debugName,
                    min,
                    max,
                    msdfRangeScale
            );
        }
    }

    private static int median(int a, int b, int c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    private static final class MsdfBitmapData {
        private final byte[] rgba;
        private final int channels;

        private MsdfBitmapData(byte[] rgba, int channels) {
            this.rgba = rgba;
            this.channels = channels;
        }
    }

    private static final class GeneratedBitmap {
        private final byte[] pixels;
        private final boolean usesTrueSdf;

        private GeneratedBitmap(byte[] pixels, boolean usesTrueSdf) {
            this.pixels = pixels;
            this.usesTrueSdf = usesTrueSdf;
        }
    }

    private static void dumpDebugImages(String debugName, BufferedImage rawImage, byte[] rgba, int width, int height, int codepoint) {
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
                    int a = rgba[index + 3] & 0xFF;
                    int argb = (0xFF << 24) | (a << 16) | (a << 8) | a;
                    sdfImage.setRGB(x, y, argb);
                    index += 4;
                }
            }
            Path sdfPath = outputDir.resolve(debugFileName(baseName, "sdf"));
            ImageIO.write(sdfImage, "png", sdfPath.toFile());
            if (DEBUG_DUMP_MSDF && !DEBUG_USE_RAW_ALPHA) {
                BufferedImage msdfImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                index = 0;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = rgba[index] & 0xFF;
                        int g = rgba[index + 1] & 0xFF;
                        int b = rgba[index + 2] & 0xFF;
                        int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                        msdfImage.setRGB(x, y, argb);
                        index += 4;
                    }
                }
                Path msdfPath = outputDir.resolve(debugFileName(baseName, "msdf_rgb"));
                ImageIO.write(msdfImage, "png", msdfPath.toFile());
                LOGGER.info("[FANCYMENU] SmoothFont debug dump written: {}, {}, {}", rawPath, sdfPath, msdfPath);
            } else {
                LOGGER.info("[FANCYMENU] SmoothFont debug dump written: {} and {}", rawPath, sdfPath);
            }
        } catch (IOException ex) {
            LOGGER.warn("[FANCYMENU] SmoothFont debug dump failed", ex);
        }
    }

    private static void logMsdfStats(String debugName, int codepoint, byte[] rgba) {
        ChannelStats r = ChannelStats.fromChannel(rgba, 0);
        ChannelStats g = ChannelStats.fromChannel(rgba, 1);
        ChannelStats b = ChannelStats.fromChannel(rgba, 2);
        ChannelStats a = ChannelStats.fromChannel(rgba, 3);
        LOGGER.info("[FANCYMENU] SmoothFontAtlas {} U+{} MSDF channels: r[min={}, max={}, zero={}, nonZero={}] g[min={}, max={}, zero={}, nonZero={}] b[min={}, max={}, zero={}, nonZero={}] a[min={}, max={}, zero={}, nonZero={}]",
                debugName,
                String.format("%04X", codepoint),
                r.min,
                r.max,
                r.zeroCount,
                r.nonZeroCount,
                g.min,
                g.max,
                g.zeroCount,
                g.nonZeroCount,
                b.min,
                b.max,
                b.zeroCount,
                b.nonZeroCount,
                a.min,
                a.max,
                a.zeroCount,
                a.nonZeroCount
        );
    }

    private static String debugFileName(String baseName, String suffix) {
        String sanitized = baseName.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return sanitized + "_" + suffix + ".png";
    }

    private static void dumpAtlasImage(String debugName, NativeImage atlas, int dumpIndex) {
        Path outputDir = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("fancymenu")
                .resolve("debug")
                .resolve("smooth_font");
        try {
            Files.createDirectories(outputDir);
            String baseName = debugName + "_" + dumpIndex;
            Path atlasPath = outputDir.resolve(debugFileName(baseName, "atlas"));
            atlas.writeToFile(atlasPath);
            LOGGER.info("[FANCYMENU] SmoothFont atlas dump written: {}", atlasPath);
        } catch (IOException ex) {
            LOGGER.warn("[FANCYMENU] SmoothFont atlas dump failed", ex);
        }
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

        private static AlphaStats fromChannel(byte[] rgba, int channelIndex) {
            int min = 255;
            int max = 0;
            int zero = 0;
            int nonZero = 0;
            for (int i = channelIndex; i < rgba.length; i += 4) {
                int a = rgba[i] & 0xFF;
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

    private static final class ChannelStats {
        private final int min;
        private final int max;
        private final int zeroCount;
        private final int nonZeroCount;

        private ChannelStats(int min, int max, int zeroCount, int nonZeroCount) {
            this.min = min;
            this.max = max;
            this.zeroCount = zeroCount;
            this.nonZeroCount = nonZeroCount;
        }

        private static ChannelStats fromChannel(byte[] rgba, int channelIndex) {
            int min = 255;
            int max = 0;
            int zero = 0;
            int nonZero = 0;
            for (int i = channelIndex; i < rgba.length; i += 4) {
                int value = rgba[i] & 0xFF;
                min = Math.min(min, value);
                max = Math.max(max, value);
                if (value == 0) {
                    zero++;
                } else {
                    nonZero++;
                }
            }
            return new ChannelStats(min, max, zero, nonZero);
        }
    }

    private static int coverageFromPixel(int argb) {
        return (argb >>> 24) & 0xFF;
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
