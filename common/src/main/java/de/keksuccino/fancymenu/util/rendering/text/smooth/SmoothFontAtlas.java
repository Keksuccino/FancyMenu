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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.msdfgen.MSDFGen;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenMultichannelConfig;
import org.lwjgl.util.msdfgen.MSDFGenRange;
import org.lwjgl.util.msdfgen.MSDFGenTransform;
import org.lwjgl.util.msdfgen.MSDFGenVector2;

import javax.annotation.Nonnull;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

final class SmoothFontAtlas implements AutoCloseable {

    private static final int DEFAULT_ATLAS_SIZE = 1024;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DISABLE_MTSDF = Boolean.getBoolean("fancymenu.debugSmoothFontDisableMtsdf");

    private static final float MSDF_EDGE_THRESHOLD = 3.0F;
    private static boolean msdfAvailable = true;
    private static boolean msdfUnavailableLogged;

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
        applyLinearFilter();
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
            return new SmoothFontGlyph(this, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, 0.0F, advance, false, true);
        }

        // Ensure padding covers the full SDF range + a bit more for anti-aliasing safety
        int padding = Math.max(4, (int)Math.ceil(sdfRange + 1.0F));
        int glyphWidth = (int)Math.ceil(bounds.getWidth() + (padding * 2.0));
        int glyphHeight = (int)Math.ceil(bounds.getHeight() + (padding * 2.0));

        if (glyphWidth <= 0 || glyphHeight <= 0) {
            return new SmoothFontGlyph(this, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0, 0.0F, 0.0F, advance, false, true);
        }

        byte[] atlasPixels;
        boolean usesTrueSdf;

        GeneratedBitmap msdf = buildMsdf(outline, bounds, glyphWidth, glyphHeight, padding, sdfRange);
        if (msdf != null && msdf.pixels != null) {
            atlasPixels = msdf.pixels;
            usesTrueSdf = msdf.usesTrueSdf;
        } else {
            BufferedImage image = renderGlyphImage(glyphVector, bounds, glyphWidth, glyphHeight, padding);
            atlasPixels = buildRawRgba(image, glyphWidth, glyphHeight);
            usesTrueSdf = true;
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

        return new SmoothFontGlyph(this, u0, v0, u1, v1, glyphWidth, glyphHeight, offsetX, offsetY, advance, true, usesTrueSdf);
    }

    private Rect allocate(int width, int height) {
        // Safe spacing to avoid any filter bleeding
        int spacing = 2;
        int allocWidth = width + spacing;
        int allocHeight = height + spacing;

        if (allocWidth > atlasWidth || allocHeight > atlasHeight) {
            int targetWidth = Math.max(atlasWidth, allocWidth);
            int targetHeight = Math.max(atlasHeight, allocHeight);
            resizeAtlas(targetWidth, targetHeight);
        }

        if (cursorX + allocWidth > atlasWidth) {
            cursorX = 0;
            cursorY += rowHeight;
            rowHeight = 0;
        }

        if (cursorY + allocHeight > atlasHeight) {
            resizeAtlas(atlasWidth * 2, atlasHeight * 2);
        }

        Rect rect = new Rect(cursorX, cursorY);
        cursorX += allocWidth;
        rowHeight = Math.max(rowHeight, allocHeight);
        return rect;
    }

    private void resizeAtlas(int newWidth, int newHeight) {
        int targetWidth = Math.max(newWidth, atlasWidth);
        int targetHeight = Math.max(newHeight, atlasHeight);
        targetWidth = Math.min(targetWidth, 4096);
        targetHeight = Math.min(targetHeight, 4096);

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

        Runnable uploadTask = () -> {
            TextureUtil.prepareImage(dynamicTexture.getId(), atlasWidth, atlasHeight);
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
                    genResult = MSDFGen.msdf_generate_msdf_with_config(bitmap, shape, transform, config);
                    usesTrueSdf = false;
                } else {
                    allocResult = MSDFGen.msdf_bitmap_alloc(MSDFGen.MSDF_BITMAP_TYPE_MTSDF, width, height, bitmap);
                    genResult = MSDFGen.msdf_generate_mtsdf_with_config(bitmap, shape, transform, config);
                    usesTrueSdf = true;
                }

                if (allocResult != MSDFGen.MSDF_SUCCESS || genResult != MSDFGen.MSDF_SUCCESS) {
                    MSDFGen.msdf_bitmap_free(bitmap);
                    return null;
                }

                MsdfBitmapData data = readBitmapPixels(bitmap, width, height);
                MSDFGen.msdf_bitmap_free(bitmap);

                if (data == null || data.rgba == null) {
                    return null;
                }

                boolean trueSdf = usesTrueSdf || data.channels >= 4;
                return new GeneratedBitmap(data.rgba, trueSdf);
            } finally {
                MSDFGen.msdf_shape_free(shape);
            }
        } catch (Throwable ex) {
            msdfAvailable = false;
            if (!msdfUnavailableLogged) {
                LOGGER.warn("[FANCYMENU] MSDF unavailable, falling back to legacy render.", ex);
                msdfUnavailableLogged = true;
            }
            return null;
        }
    }

    private MsdfBitmapData readBitmapPixels(MSDFGenBitmap bitmap, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pixelPtr = stack.mallocPointer(1);
            MSDFGen.msdf_bitmap_get_pixels(bitmap, pixelPtr);
            long pixels = pixelPtr.get(0);

            PointerBuffer sizePtr = stack.mallocPointer(1);
            MSDFGen.msdf_bitmap_get_byte_size(bitmap, sizePtr);
            long byteSize = sizePtr.get(0);

            IntBuffer channelsBuf = stack.mallocInt(1);
            MSDFGen.msdf_bitmap_get_channel_count(bitmap, channelsBuf);
            int channels = channelsBuf.get(0);

            if (pixels == MemoryUtil.NULL || byteSize <= 0 || channels <= 0) {
                return null;
            }

            int pixelCount = width * height;
            int bytesPerChannel = (int)(byteSize / (long)(pixelCount * channels));
            ByteBuffer buffer = MemoryUtil.memByteBuffer(pixels, (int)byteSize).order(ByteOrder.nativeOrder());
            byte[] rgba = new byte[pixelCount * 4];

            if (bytesPerChannel == 4) {
                FloatBuffer floats = buffer.asFloatBuffer();
                for (int i = 0; i < pixelCount; i++) {
                    int srcBase = i * channels;
                    int dstBase = i * 4;

                    float r = floats.get(srcBase);
                    float g = channels > 1 ? floats.get(srcBase + 1) : r;
                    float b = channels > 2 ? floats.get(srcBase + 2) : r;
                    float a = channels > 3 ? floats.get(srcBase + 3) : 1.0F;

                    rgba[dstBase] = (byte)Math.round(Mth.clamp(r, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 1] = (byte)Math.round(Mth.clamp(g, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 2] = (byte)Math.round(Mth.clamp(b, 0.0F, 1.0F) * 255.0F);
                    rgba[dstBase + 3] = (byte)Math.round(Mth.clamp(a, 0.0F, 1.0F) * 255.0F);
                }
                return new MsdfBitmapData(rgba, channels);
            }
            return null;
        }
    }

    private static long buildShapeFromPath(PathIterator iterator) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer shapePtr = stack.mallocPointer(1);
            if (MSDFGen.msdf_shape_alloc(shapePtr) != MSDFGen.MSDF_SUCCESS) return MemoryUtil.NULL;
            long shape = shapePtr.get(0);
            long contour = MemoryUtil.NULL;
            double startX = 0, startY = 0, lastX = 0, lastY = 0;
            double[] coords = new double[6];

            while (!iterator.isDone()) {
                int seg = iterator.currentSegment(coords);
                if (seg == PathIterator.SEG_MOVETO) {
                    if (contour != MemoryUtil.NULL) closeContourIfOpen(contour, startX, startY, lastX, lastY);
                    PointerBuffer contourPtr = stack.mallocPointer(1);
                    MSDFGen.msdf_shape_add_contour(shape, contourPtr);
                    contour = contourPtr.get(0);
                    startX = coords[0]; startY = coords[1];
                    lastX = startX; lastY = startY;
                } else if (seg == PathIterator.SEG_LINETO) {
                    addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_LINEAR, lastX, lastY, coords[0], coords[1]);
                    lastX = coords[0]; lastY = coords[1];
                } else if (seg == PathIterator.SEG_QUADTO) {
                    addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_QUADRATIC, lastX, lastY, coords[0], coords[1], coords[2], coords[3]);
                    lastX = coords[2]; lastY = coords[3];
                } else if (seg == PathIterator.SEG_CUBICTO) {
                    addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_CUBIC, lastX, lastY, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    lastX = coords[4]; lastY = coords[5];
                } else if (seg == PathIterator.SEG_CLOSE) {
                    if (contour != MemoryUtil.NULL) closeContourIfOpen(contour, startX, startY, lastX, lastY);
                    contour = MemoryUtil.NULL;
                }
                iterator.next();
            }
            if (contour != MemoryUtil.NULL) closeContourIfOpen(contour, startX, startY, lastX, lastY);

            MSDFGen.msdf_shape_normalize(shape);
            return shape;
        }
    }

    private static void closeContourIfOpen(long contour, double startX, double startY, double lastX, double lastY) {
        if (Math.abs(lastX - startX) > 0.0001 || Math.abs(lastY - startY) > 0.0001) {
            addSegment(contour, MSDFGen.MSDF_SEGMENT_TYPE_LINEAR, lastX, lastY, startX, startY);
        }
    }

    private static void addSegment(long contour, int type, double... coords) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer segPtr = stack.mallocPointer(1);
            if (MSDFGen.msdf_segment_alloc(type, segPtr) != MSDFGen.MSDF_SUCCESS) return;
            long segment = segPtr.get(0);
            MSDFGenVector2 vec = MSDFGenVector2.calloc(stack);

            for (int i = 0; i < coords.length; i += 2) {
                vec.x(coords[i]).y(coords[i + 1]);
                MSDFGen.msdf_segment_set_point(segment, i / 2, vec);
            }
            MSDFGen.msdf_contour_add_edge(contour, segment);
        }
    }

    private static byte[] expandAlphaToRgba(byte[] alpha) {
        byte[] rgba = new byte[alpha.length * 4];
        for (int i = 0; i < alpha.length; i++) {
            byte a = alpha[i];
            int dst = i * 4;
            rgba[dst] = a;
            rgba[dst + 1] = a;
            rgba[dst + 2] = a;
            rgba[dst + 3] = a;
        }
        return rgba;
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
