package de.keksuccino.fancymenu.util.rendering.text.smooth;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
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

    public static void renderText(@Nonnull GuiGraphics graphics, @Nonnull SmoothFont font, @Nonnull String text, float x, float y, int color, float size, boolean shadow) {
        if (text.isEmpty() || size <= 0.0F) return;
        if (shadow) {
            renderTextInternal(graphics, font, text, x + 1.0F, y + 1.0F, darkenColor(color), size);
        }
        renderTextInternal(graphics, font, text, x, y, color, size);
    }

    public static void renderTextScreenSpace(@Nonnull GuiGraphics graphics, @Nonnull SmoothFont font, @Nonnull String text, float xPixels, float yPixels, int color, float sizePixels, boolean shadow) {
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        if (guiScale <= 0.0F) return;
        renderText(graphics, font, text, xPixels / guiScale, yPixels / guiScale, color, sizePixels / guiScale, shadow);
    }

    public static float getTextWidth(@Nonnull SmoothFont font, @Nonnull String text, float size) {
        if (text.isEmpty() || size <= 0.0F) return 0.0F;

        // Use LOD logic here too for metrics accuracy
        int lod = font.getLodLevel(size);
        float scale = font.getScaleForLod(lod, size);

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

            SmoothFontGlyph glyph = font.getGlyph(lod, codepoint, style.bold, style.italic);
            float advance = glyph.advance() * scale;
            lineWidth += advance;
        }
        return Math.max(maxWidth, lineWidth);
    }

    public static float getTextHeight(@Nonnull SmoothFont font, @Nonnull String text, float size) {
        if (text.isEmpty() || size <= 0.0F) return 0.0F;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return font.getLineHeight(size) * lines;
    }

    private static void renderTextInternal(GuiGraphics graphics, SmoothFont font, String text, float x, float y, int baseColor, float size) {
        // [Multi-LOD] Determine the best atlas set for this size
        int lod = font.getLodLevel(size);
        float scale = font.getScaleForLod(lod, size);

        float baseRange = font.getSdfRange();
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
        boolean currentUsesTrueSdf = true;

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        applyShaderState();
        SmoothTextShader.applySdfRange(baseRange);
        SmoothTextShader.applyUseTrueSdf(currentUsesTrueSdf);

        float underlineStartX = 0.0F;
        float strikeStartX = 0.0F;
        int underlineColor = style.color;
        int strikeColor = style.color;
        float underlineThickness = font.getUnderlineThickness(size);
        float strikeThickness = font.getStrikethroughThickness(size);

        for (int index = 0; index < text.length(); ) {
            char c = text.charAt(index);
            if (c == '\n') {
                quadCount = flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
                buffer = null;
                currentAtlas = null;
                drawLineIfNeeded(graphics, style.underline, underlineStartX, penX, baseline + font.getUnderlineOffset(size), underlineColor, underlineThickness);
                drawLineIfNeeded(graphics, style.strikethrough, strikeStartX, penX, baseline + font.getStrikethroughOffset(size), strikeColor, strikeThickness);
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
                        quadCount = flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
                        buffer = null;
                        currentAtlas = null;
                        drawLineIfNeeded(graphics, true, underlineStartX, penX, baseline + font.getUnderlineOffset(size), underlineColor, underlineThickness);
                        underlineStartX = penX;
                        underlineColor = style.color;
                    } else if (!wasUnderline && style.underline) {
                        underlineStartX = penX;
                        underlineColor = style.color;
                    }
                    if (wasStrikethrough && (!style.strikethrough || previousColor != style.color)) {
                        quadCount = flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
                        buffer = null;
                        currentAtlas = null;
                        drawLineIfNeeded(graphics, true, strikeStartX, penX, baseline + font.getStrikethroughOffset(size), strikeColor, strikeThickness);
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

            // [Multi-LOD] Fetch glyph from the specific LOD level
            SmoothFontGlyph glyph = font.getGlyph(lod, codepoint, style.bold, style.italic);

            if (glyph.hasTexture()) {
                SmoothFontAtlas atlas = glyph.atlas();
                boolean glyphUsesTrueSdf = glyph.usesTrueSdf();
                if (currentAtlas != atlas) {
                    quadCount = flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
                    buffer = null;
                    currentAtlas = atlas;
                    RenderSystem.setShaderTexture(0, atlas.getTextureId());
                    SmoothTextShader.applySdfRange(atlas.getEffectiveSdfRange());
                    SmoothTextShader.applyUseTrueSdf(glyphUsesTrueSdf);
                    currentUsesTrueSdf = glyphUsesTrueSdf;
                } else if (glyphUsesTrueSdf != currentUsesTrueSdf) {
                    quadCount = flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
                    buffer = null;
                    SmoothTextShader.applyUseTrueSdf(glyphUsesTrueSdf);
                    currentUsesTrueSdf = glyphUsesTrueSdf;
                }
                if (buffer == null) {
                    buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                }
                addGlyph(buffer, matrix, glyph, penX, baseline, scale, style.color, style.italic);
                quadCount++;
            }

            float advance = glyph.advance() * scale;
            penX += advance;
        }

        flushIfNeeded(buffer, quadCount, currentAtlas, currentAtlas != null ? currentAtlas.getEffectiveSdfRange() : baseRange, currentUsesTrueSdf);
        drawLineIfNeeded(graphics, style.underline, underlineStartX, penX, baseline + font.getUnderlineOffset(size), underlineColor, underlineThickness);
        drawLineIfNeeded(graphics, style.strikethrough, strikeStartX, penX, baseline + font.getStrikethroughOffset(size), strikeColor, strikeThickness);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.disableBlend();
        RenderingUtils.resetShaderColor(graphics);
    }

    private static int flushIfNeeded(BufferBuilder buffer, int quadCount, SmoothFontAtlas atlas, float sdfRange, boolean useTrueSdf) {
        if (buffer != null && quadCount > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            applyShaderState();
            SmoothTextShader.applySdfRange(sdfRange);
            SmoothTextShader.applyUseTrueSdf(useTrueSdf);
            if (atlas != null) {
                RenderSystem.setShaderTexture(0, atlas.getTextureId());
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        return 0;
    }

    private static void applyShaderState() {
        RenderSystem.setShader(SmoothTextShader::getShader);
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

    private static int applyFormatting(String text, int formatIndex, StyleState style, int baseColor) {
        if (formatIndex >= text.length()) return 0;
        char code = text.charAt(formatIndex);
        ChatFormatting formatting = ChatFormatting.getByCode(code);

        if (formatting == null && Character.toLowerCase(code) == 'x') {
            Integer hexColor = parseHexColor(text, formatIndex);
            if (hexColor != null) {
                style.setColor(hexColor, FastColor.ARGB32.alpha(baseColor));
                return 13;
            }
        }
        if (formatting == null) {
            TextColorFormatter formatter = TextColorFormatterRegistry.getByCode(code);
            if (formatter != null) {
                int formatterColor = formatter.getColor().getColorInt();
                int rgb = (FastColor.ARGB32.red(formatterColor) << 16)
                        | (FastColor.ARGB32.green(formatterColor) << 8)
                        | FastColor.ARGB32.blue(formatterColor);
                style.setColor(rgb, FastColor.ARGB32.alpha(baseColor));
                return 1;
            }
            return 0;
        }

        if (formatting == ChatFormatting.RESET) {
            style.resetToBase();
            return 1;
        }
        if (formatting.isColor()) {
            Integer color = formatting.getColor();
            if (color != null) style.setColor(color, FastColor.ARGB32.alpha(baseColor));
            return 1;
        }
        switch (formatting) {
            case BOLD -> style.bold = true;
            case ITALIC -> style.italic = true;
            case UNDERLINE -> style.underline = true;
            case STRIKETHROUGH -> style.strikethrough = true;
            case OBFUSCATED -> style.obfuscated = true;
        }
        return 1;
    }

    private static Integer parseHexColor(String text, int formatIndex) {
        if (formatIndex + 13 >= text.length()) return null;
        int start = formatIndex + 1;
        try {
            int r = Integer.parseInt(text.substring(start + 1, start + 2), 16);
            int r2 = Integer.parseInt(text.substring(start + 3, start + 4), 16);
            int g = Integer.parseInt(text.substring(start + 5, start + 6), 16);
            int g2 = Integer.parseInt(text.substring(start + 7, start + 8), 16);
            int b = Integer.parseInt(text.substring(start + 9, start + 10), 16);
            int b2 = Integer.parseInt(text.substring(start + 11, start + 12), 16);
            return ((r << 4 | r2) << 16) | ((g << 4 | g2) << 8) | (b << 4 | b2);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int getObfuscatedCodepoint(int original) {
        return original == ' ' ? original : OBFUSCATION_CHARS.charAt(OBFUSCATION_RANDOM.nextInt(OBFUSCATION_CHARS.length()));
    }

    private static int darkenColor(int color) {
        int alpha = FastColor.ARGB32.alpha(color);
        int red = (int)(FastColor.ARGB32.red(color) * 0.25F);
        int green = (int)(FastColor.ARGB32.green(color) * 0.25F);
        int blue = (int)(FastColor.ARGB32.blue(color) * 0.25F);
        return FastColor.ARGB32.color(alpha, red, green, blue);
    }

    private static void drawLineIfNeeded(GuiGraphics graphics, boolean enabled, float startX, float endX, float y, int color, float thickness) {
        if (!enabled || endX <= startX) return;
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
