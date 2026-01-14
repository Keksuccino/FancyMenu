package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.FastColor;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Random;

public final class SmoothTextRenderer {

    private static final char FORMAT_PREFIX = ChatFormatting.PREFIX_CODE;
    private static final Random OBFUSCATION_RANDOM = new Random();
    private static final String OBFUSCATION_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private SmoothTextRenderer() {
    }

    /**
     * Renders smooth text at the given GUI coordinates. The Y coordinate is the top of the text line,
     * just like {@link net.minecraft.client.gui.Font#drawInBatch}.
     * <p>
     * Supports Minecraft-style formatting codes such as {@code \\u00A70-\\u00A7f} (colors), {@code \\u00A7l} (bold),
     * {@code \\u00A7o} (italic), {@code \\u00A7n} (underline), {@code \\u00A7m} (strikethrough), {@code \\u00A7k} (obfuscated),
     * {@code \\u00A7r} (reset), and hex colors in the {@code \\u00A7x\\u00A7R\\u00A7R\\u00A7G\\u00A7G\\u00A7B\\u00A7B} format.
     */
    public static void renderText(@Nonnull GuiGraphics graphics, @Nonnull SmoothFont font, @Nonnull String text, float x, float y, int color, float size, boolean shadow) {
        Objects.requireNonNull(graphics);
        Objects.requireNonNull(font);
        Objects.requireNonNull(text);
        if (text.isEmpty() || size <= 0.0F) {
            return;
        }
        if (shadow) {
            renderTextInternal(graphics, font, text, x + 1.0F, y + 1.0F, darkenColor(color), size);
        }
        renderTextInternal(graphics, font, text, x, y, color, size);
    }

    /**
     * Renders smooth text using screen-space (framebuffer pixel) coordinates. This is useful when your
     * rendering code already operates in pixel space rather than GUI-scaled coordinates.
     */
    public static void renderTextScreenSpace(@Nonnull GuiGraphics graphics, @Nonnull SmoothFont font, @Nonnull String text, float xPixels, float yPixels, int color, float sizePixels, boolean shadow) {
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        if (guiScale <= 0.0F) {
            return;
        }
        renderText(graphics, font, text, xPixels / guiScale, yPixels / guiScale, color, sizePixels / guiScale, shadow);
    }

    /**
     * Calculates a size in screen-space pixels that matches vanilla text at the current GUI scale.
     * Use this together with {@link #renderTextScreenSpace(GuiGraphics, SmoothFont, String, float, float, int, float, boolean)}
     * when you render in framebuffer coordinates or have applied additional scale factors.
     * <p>
     * Examples:
     * <ul>
     *     <li>GUI scale 2.0, additionalScale 1.0 -> returns ~18 (vanilla 9px text at 2x scale).</li>
     *     <li>GUI scale 2.0, additionalScale 0.5 -> returns ~9 (useful when you already halve the render scale).</li>
     * </ul>
     */
    public static float snapshotGuiScaledTextSize(float additionalScale) {
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        float baseSize = Minecraft.getInstance().font.lineHeight;
        float scale = Math.max(0.0F, additionalScale);
        return baseSize * guiScale * scale;
    }

    public static float getTextWidth(@Nonnull SmoothFont font, @Nonnull String text, float size) {
        Objects.requireNonNull(font);
        Objects.requireNonNull(text);
        if (text.isEmpty() || size <= 0.0F) {
            return 0.0F;
        }
        float scale = font.scaleForSize(size);
        float maxWidth = 0.0F;
        float lineWidth = 0.0F;
        StyleState style = new StyleState(0xFFFFFFFF);

        for (int index = 0; index < text.length(); ) {
            char c = text.charAt(index);
            if (c == '\n') {
                maxWidth = Math.max(maxWidth, lineWidth);
                lineWidth = 0.0F;
                index++;
                continue;
            }
            if (c == FORMAT_PREFIX && index + 1 < text.length()) {
                int consumed = applyFormatting(text, index + 1, style, 0xFFFFFFFF);
                if (consumed > 0) {
                    index += consumed + 1;
                    continue;
                }
            }
            int codepoint = text.codePointAt(index);
            index += Character.charCount(codepoint);
            SmoothFontGlyph glyph = font.getGlyph(codepoint, style.bold, style.italic);
            float advance = glyph.advance() * scale;
            if (style.bold) {
                advance += getBoldOffset(scale);
            }
            lineWidth += advance;
        }
        return Math.max(maxWidth, lineWidth);
    }

    public static float getTextHeight(@Nonnull SmoothFont font, @Nonnull String text, float size) {
        Objects.requireNonNull(font);
        Objects.requireNonNull(text);
        if (text.isEmpty() || size <= 0.0F) {
            return 0.0F;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return font.getLineHeight(size) * lines;
    }

    private static void renderTextInternal(GuiGraphics graphics, SmoothFont font, String text, float x, float y, int baseColor, float size) {
        float scale = font.scaleForSize(size);
        float ascent = font.getAscent(size);
        float lineHeight = font.getLineHeight(size);
        float penX = x;
        float lineY = y;
        float baseline = lineY + ascent;

        StyleState style = new StyleState(baseColor);

        BufferBuilder buffer = null;
        SmoothFontAtlas currentAtlas = null;
        int quadCount = 0;
        Matrix4f matrix = graphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(SmoothTextShader::getShader);
        SmoothTextShader.applyDefaults();

        float underlineStartX = 0.0F;
        float strikeStartX = 0.0F;
        int underlineColor = style.color;
        int strikeColor = style.color;

        for (int index = 0; index < text.length(); ) {
            char c = text.charAt(index);
            if (c == '\n') {
                quadCount = flushIfNeeded(buffer, quadCount);
                buffer = null;
                currentAtlas = null;
                drawLineIfNeeded(graphics, style.underline, underlineStartX, penX, baseline + (size * 0.9F), underlineColor, size);
                drawLineIfNeeded(graphics, style.strikethrough, strikeStartX, penX, baseline + (size * 0.5F), strikeColor, size);
                penX = x;
                lineY += lineHeight;
                baseline = lineY + ascent;
                if (style.underline) {
                    underlineStartX = penX;
                    underlineColor = style.color;
                }
                if (style.strikethrough) {
                    strikeStartX = penX;
                    strikeColor = style.color;
                }
                index++;
                continue;
            }
            if (c == FORMAT_PREFIX && index + 1 < text.length()) {
                boolean wasUnderline = style.underline;
                boolean wasStrikethrough = style.strikethrough;
                int previousColor = style.color;
                int consumed = applyFormatting(text, index + 1, style, baseColor);
                if (consumed > 0) {
                    if (wasUnderline && (!style.underline || previousColor != style.color)) {
                        quadCount = flushIfNeeded(buffer, quadCount);
                        buffer = null;
                        currentAtlas = null;
                        drawLineIfNeeded(graphics, true, underlineStartX, penX, baseline + (size * 0.9F), underlineColor, size);
                        underlineStartX = penX;
                        underlineColor = style.color;
                    } else if (!wasUnderline && style.underline) {
                        underlineStartX = penX;
                        underlineColor = style.color;
                    }
                    if (wasStrikethrough && (!style.strikethrough || previousColor != style.color)) {
                        quadCount = flushIfNeeded(buffer, quadCount);
                        buffer = null;
                        currentAtlas = null;
                        drawLineIfNeeded(graphics, true, strikeStartX, penX, baseline + (size * 0.5F), strikeColor, size);
                        strikeStartX = penX;
                        strikeColor = style.color;
                    } else if (!wasStrikethrough && style.strikethrough) {
                        strikeStartX = penX;
                        strikeColor = style.color;
                    }
                    index += consumed + 1;
                    continue;
                }
            }

            int codepoint = text.codePointAt(index);
            index += Character.charCount(codepoint);
            if (style.obfuscated) {
                codepoint = getObfuscatedCodepoint(codepoint);
            }
            SmoothFontGlyph glyph = font.getGlyph(codepoint, style.bold, style.italic);
            if (glyph.hasTexture()) {
                SmoothFontAtlas atlas = glyph.atlas();
                if (currentAtlas != atlas) {
                    quadCount = flushIfNeeded(buffer, quadCount);
                    buffer = null;
                    currentAtlas = atlas;
                    RenderSystem.setShaderTexture(0, atlas.getTextureLocation());
                }
                if (buffer == null) {
                    buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                }
                addGlyph(buffer, matrix, glyph, penX, baseline, scale, style.color, style.italic);
                quadCount++;
                if (style.bold) {
                    addGlyph(buffer, matrix, glyph, penX + getBoldOffset(scale), baseline, scale, style.color, style.italic);
                    quadCount++;
                }
            }
            float advance = glyph.advance() * scale;
            if (style.bold) {
                advance += getBoldOffset(scale);
            }
            penX += advance;
        }

        quadCount = flushIfNeeded(buffer, quadCount);
        drawLineIfNeeded(graphics, style.underline, underlineStartX, penX, baseline + (size * 0.9F), underlineColor, size);
        drawLineIfNeeded(graphics, style.strikethrough, strikeStartX, penX, baseline + (size * 0.5F), strikeColor, size);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.disableBlend();
        RenderingUtils.resetShaderColor(graphics);
    }

    private static int flushIfNeeded(BufferBuilder buffer, int quadCount) {
        if (buffer != null && quadCount > 0) {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        return 0;
    }

    private static void addGlyph(BufferBuilder buffer, Matrix4f matrix, SmoothFontGlyph glyph, float penX, float baseline, float scale, int color, boolean italic) {
        float width = glyph.width() * scale;
        float height = glyph.height() * scale;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        float drawX = penX + (glyph.xOffset() * scale);
        float drawY = baseline + (glyph.yOffset() * scale);
        float italicSkew = italic ? height * 0.25F : 0.0F;

        float x0 = drawX;
        float x1 = drawX + width;
        float y0 = drawY;
        float y1 = drawY + height;

        buffer.addVertex(matrix, x0 + italicSkew, y0, 0.0F).setUv(glyph.u0(), glyph.v0()).setColor(color);
        buffer.addVertex(matrix, x0, y1, 0.0F).setUv(glyph.u0(), glyph.v1()).setColor(color);
        buffer.addVertex(matrix, x1, y1, 0.0F).setUv(glyph.u1(), glyph.v1()).setColor(color);
        buffer.addVertex(matrix, x1 + italicSkew, y0, 0.0F).setUv(glyph.u1(), glyph.v0()).setColor(color);
    }

    private static float getBoldOffset(float scale) {
        return Math.max(1.0F, scale);
    }

    private static int applyFormatting(String text, int formatIndex, StyleState style, int baseColor) {
        if (formatIndex >= text.length()) {
            return 0;
        }
        char code = text.charAt(formatIndex);
        ChatFormatting formatting = ChatFormatting.getByCode(code);
        if (formatting == null && Character.toLowerCase(code) == 'x') {
            Integer hexColor = parseHexColor(text, formatIndex);
            if (hexColor != null) {
                style.setColor(hexColor, FastColor.ARGB32.alpha(baseColor));
                return 13;
            }
            return 0;
        }
        if (formatting == null) {
            return 0;
        }
        if (formatting == ChatFormatting.RESET) {
            style.resetToBase();
            return 1;
        }
        if (formatting.isColor()) {
            Integer color = formatting.getColor();
            if (color != null) {
                style.setColor(color, FastColor.ARGB32.alpha(baseColor));
            }
            return 1;
        }
        switch (formatting) {
            case BOLD -> style.bold = true;
            case ITALIC -> style.italic = true;
            case UNDERLINE -> style.underline = true;
            case STRIKETHROUGH -> style.strikethrough = true;
            case OBFUSCATED -> style.obfuscated = true;
            default -> {
            }
        }
        return 1;
    }

    private static Integer parseHexColor(String text, int formatIndex) {
        if (formatIndex + 13 >= text.length()) {
            return null;
        }
        int start = formatIndex + 1;
        int r = parseHexDigit(text.charAt(start + 1));
        int r2 = parseHexDigit(text.charAt(start + 3));
        int g = parseHexDigit(text.charAt(start + 5));
        int g2 = parseHexDigit(text.charAt(start + 7));
        int b = parseHexDigit(text.charAt(start + 9));
        int b2 = parseHexDigit(text.charAt(start + 11));
        if (r < 0 || r2 < 0 || g < 0 || g2 < 0 || b < 0 || b2 < 0) {
            return null;
        }
        if (text.charAt(start) != FORMAT_PREFIX || text.charAt(start + 2) != FORMAT_PREFIX || text.charAt(start + 4) != FORMAT_PREFIX
                || text.charAt(start + 6) != FORMAT_PREFIX || text.charAt(start + 8) != FORMAT_PREFIX || text.charAt(start + 10) != FORMAT_PREFIX) {
            return null;
        }
        int red = (r << 4) | r2;
        int green = (g << 4) | g2;
        int blue = (b << 4) | b2;
        return (red << 16) | (green << 8) | blue;
    }

    private static int parseHexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        c = Character.toLowerCase(c);
        if (c >= 'a' && c <= 'f') {
            return 10 + (c - 'a');
        }
        return -1;
    }

    private static int getObfuscatedCodepoint(int original) {
        if (original == ' ') {
            return original;
        }
        return OBFUSCATION_CHARS.charAt(OBFUSCATION_RANDOM.nextInt(OBFUSCATION_CHARS.length()));
    }

    private static int darkenColor(int color) {
        int alpha = FastColor.ARGB32.alpha(color);
        int red = (int)(FastColor.ARGB32.red(color) * 0.25F);
        int green = (int)(FastColor.ARGB32.green(color) * 0.25F);
        int blue = (int)(FastColor.ARGB32.blue(color) * 0.25F);
        return FastColor.ARGB32.color(alpha, red, green, blue);
    }

    private static void drawLineIfNeeded(GuiGraphics graphics, boolean enabled, float startX, float endX, float y, int color, float size) {
        if (!enabled || endX <= startX) {
            return;
        }
        float thickness = Math.max(1.0F, size * 0.075F);
        RenderingUtils.fillF(graphics, startX, y, endX, y + thickness, color);
    }

    private static final class StyleState {
        private final int baseColor;
        private int color;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;
        private boolean obfuscated;

        private StyleState(int baseColor) {
            this.baseColor = baseColor;
            this.color = baseColor;
        }

        private void resetToBase() {
            this.color = baseColor;
            this.bold = false;
            this.italic = false;
            this.underline = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }

        private void setColor(int rgb, int baseAlpha) {
            this.color = FastColor.ARGB32.color(baseAlpha, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
            this.bold = false;
            this.italic = false;
            this.underline = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }
    }

}
