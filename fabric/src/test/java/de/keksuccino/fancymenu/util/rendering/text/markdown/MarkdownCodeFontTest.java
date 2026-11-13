package de.keksuccino.fancymenu.util.rendering.text.markdown;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownLook;
import de.keksuccino.fancymenu.util.rendering.text.markdown.style.MarkdownStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownCodeFontTest {

    private static final int CHAR_WIDTH = 6;
    private static final String TEXT = "plain `inline` text\n```\nblock\n```";
    private static final ResourceLocation PIXEL_CODE_FONT = MarkdownStyle.PIXEL_CODE_FONT;

    @ParameterizedTest
    @EnumSource(MarkdownLook.class)
    void onlyThePixelLookGivesCodeAFontOfItsOwn(MarkdownLook look) {
        MarkdownTestRenderer renderer = renderer(TEXT, look);
        ResourceLocation codeFont = (look == MarkdownLook.PIXEL) ? PIXEL_CODE_FONT : Style.DEFAULT_FONT;

        assertEquals(look == MarkdownLook.PIXEL, renderer.getStyle().codeFont.isSet());
        assertEquals(codeFont, renderer.fragmentContaining("inline").getRenderComponent().getStyle().getFont());
        assertEquals(codeFont, renderer.fragmentContaining("block").getRenderComponent().getStyle().getFont());
        assertEquals(Style.DEFAULT_FONT, renderer.fragmentContaining("plain").getRenderComponent().getStyle().getFont());
        assertEquals(Style.DEFAULT_FONT, renderer.fragmentContaining("text").getRenderComponent().getStyle().getFont());
    }

    @Test
    void codeFontIsAnOptionOfEveryLook() {
        ResourceLocation font = new ResourceLocation("test", "mono");
        MarkdownTestRenderer renderer = renderer(TEXT, MarkdownLook.CLASSIC);
        int parses = renderer.parses;

        renderer.getStyle().codeFont.set(font);
        renderer.tick();

        // Another font has other widths, so the text gets measured again
        assertEquals(parses + 1, renderer.parses);
        assertEquals(font, renderer.fragmentContaining("inline").getRenderComponent().getStyle().getFont());
        assertEquals(font, renderer.fragmentContaining("block").getRenderComponent().getStyle().getFont());

        renderer.getStyle().codeFont.set(null);
        renderer.tick();

        assertNull(renderer.getStyle().codeFont.get());
        assertEquals(Style.DEFAULT_FONT, renderer.fragmentContaining("inline").getRenderComponent().getStyle().getFont());
    }

    @Test
    void charactersTheCodeFontDoesNotHaveKeepTheTextFont() {
        MarkdownTestRenderer renderer = renderer("a `ab\u00E4\u00F6cd\u4E2D` b", MarkdownLook.PIXEL);
        MarkdownTextFragment code = renderer.fragmentContaining("ab");
        Component component = code.getRenderComponent();

        assertEquals("ab\u00E4\u00F6cd\u4E2D ", code.text);
        assertEquals(code.text, component.getString());
        assertEquals(List.of("ab|" + PIXEL_CODE_FONT, "\u00E4\u00F6|" + Style.DEFAULT_FONT, "cd|" + PIXEL_CODE_FONT, "\u4E2D|" + Style.DEFAULT_FONT, " |" + PIXEL_CODE_FONT), fontRuns(component));
        // Every part keeps the color of code
        component.visit((style, text) -> {
            assertEquals(renderer.getStyle().inlineCodeTextColor.argb() & 0xFFFFFF, style.getColor().getValue() & 0xFFFFFF);
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(code.text.length() * CHAR_WIDTH, code.unscaledTextWidth);
    }

    @Test
    void codeThatTheCodeFontHasAllCharactersOfIsOneText() {
        MarkdownTestRenderer renderer = renderer("a `x = y[0];` b", MarkdownLook.PIXEL);
        Component component = renderer.fragmentContaining("x").getRenderComponent();

        assertTrue(component.getSiblings().isEmpty());
        assertEquals(PIXEL_CODE_FONT, component.getStyle().getFont());
    }

    @Test
    void codeFontThatHasNoCharactersLeavesCodeInTheTextFont() {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer("a `code` b", 400);
        renderer.fontCoverage = MarkdownFontCoverage.fromDefinitions(List.of());
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(MarkdownLook.PIXEL);
        renderer.tick();

        assertEquals(List.of("code |" + Style.DEFAULT_FONT), fontRuns(renderer.fragmentContaining("code").getRenderComponent()));
    }

    @Test
    void cellsOfACodeBlockDrawCharactersTheCodeFontDoesNotHaveInTheTextFont() {
        MarkdownTestRenderer renderer = renderer("```\nab\u00E4cd\n```", MarkdownLook.PIXEL);
        MarkdownTextFragment code = renderer.fragmentContaining("ab");
        MarkdownFixedWidthText cells = code.getFixedWidthText();

        assertTrue(code.hasFixedWidthCells());
        assertEquals(3, cells.getRunCount());
        assertEquals("ab", cells.getRun(0).getString());
        assertEquals(PIXEL_CODE_FONT, cells.getRun(0).getStyle().getFont());
        assertEquals("\u00E4", cells.getRun(1).getString());
        assertEquals(Style.DEFAULT_FONT, cells.getRun(1).getStyle().getFont());
        assertEquals("cd", cells.getRun(2).getString());
        assertEquals(PIXEL_CODE_FONT, cells.getRun(2).getStyle().getFont());
        // Every character stands in its cell, whatever font it has
        assertEquals(0.0F, cells.getRunOffset(0));
        assertEquals(2.0F * CHAR_WIDTH, cells.getRunOffset(1));
        assertEquals(3.0F * CHAR_WIDTH, cells.getRunOffset(2));
        assertEquals(5.0F * CHAR_WIDTH, code.unscaledTextWidth);
    }

    @Test
    void codeFontOfThePixelLookIsABitmapFontWithThePrintableCharactersOfAscii() throws IOException {
        JsonObject provider = pixelCodeFontProvider();
        StringBuilder characters = new StringBuilder();
        for (JsonElement row : provider.getAsJsonArray("chars")) characters.append(row.getAsString());
        StringBuilder printableAscii = new StringBuilder();
        for (char c = ' '; c <= '~'; c++) printableAscii.append(c);

        // Every Minecraft version loads bitmap fonts with these fields
        assertEquals("bitmap", provider.get("type").getAsString());
        assertEquals(8, provider.get("height").getAsInt());
        assertEquals(7, provider.get("ascent").getAsInt());
        assertEquals(printableAscii + "\u0000", characters.toString());
    }

    @Test
    void everyGlyphOfTheCodeFontOfThePixelLookHasTheSameAdvance() throws IOException {
        JsonObject provider = pixelCodeFontProvider();
        JsonArray rows = provider.getAsJsonArray("chars");
        String file = provider.get("file").getAsString();
        BufferedImage texture = ImageIO.read(assets().resolve("textures").resolve(file.substring(file.indexOf(':') + 1)).toFile());
        int columns = rows.get(0).getAsString().length();
        int slotWidth = texture.getWidth() / columns;
        int slotHeight = texture.getHeight() / rows.size();

        assertEquals(8, slotHeight);
        assertEquals(0, texture.getWidth() % columns);
        for (int row = 0; row < rows.size(); row++) {
            String characters = rows.get(row).getAsString();
            assertEquals(columns, characters.length());
            for (int column = 0; column < columns; column++) {
                char character = characters.charAt(column);
                int rightmost = -1;
                int visiblePixels = 0;
                for (int x = 0; x < slotWidth; x++) {
                    for (int y = 0; y < slotHeight; y++) {
                        int alpha = texture.getRGB((column * slotWidth) + x, (row * slotHeight) + y) >>> 24;
                        if (alpha != 0) rightmost = Math.max(rightmost, x);
                        if (alpha == 255) visiblePixels++;
                        // A pixel either is a part of the glyph or the one that only sets its advance
                        assertTrue((alpha == 0) || (alpha == 255) || ((alpha == 1) && (x == 4)), "pixel of '" + character + "' with alpha " + alpha);
                    }
                }
                if (character == 0) {
                    assertEquals(-1, rightmost);
                    continue;
                }
                // Minecraft gives a bitmap glyph the advance of its rightmost pixel that is not fully transparent, plus one
                assertEquals(4, rightmost, "rightmost pixel of '" + character + "'");
                assertEquals(character != ' ', visiblePixels > 0, "pixels of '" + character + "'");
            }
        }
    }

    private static MarkdownTestRenderer renderer(String text, MarkdownLook look) {
        MarkdownTestRenderer renderer = new MarkdownTestRenderer(text, 400);
        renderer.setParentRenderScale(3.0F);
        renderer.getStyle().applyLook(look);
        renderer.tick();
        return renderer;
    }

    /**
     * Returns the parts of the component that follow each other with another font, as "text|font".
     */
    private static List<String> fontRuns(Component component) {
        List<String> runs = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) runs.add(text + "|" + style.getFont());
            return Optional.empty();
        }, Style.EMPTY);
        return runs;
    }

    private static JsonObject pixelCodeFontProvider() throws IOException {
        Path definition = assets().resolve("font").resolve(MarkdownStyle.PIXEL_CODE_FONT.getPath() + ".json");
        JsonArray providers = JsonParser.parseString(Files.readString(definition, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("providers");
        assertEquals(1, providers.size());
        return providers.get(0).getAsJsonObject();
    }

    private static Path assets() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle")) || Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current.resolve("common/src/main/resources/assets").resolve(MarkdownStyle.PIXEL_CODE_FONT.getNamespace());
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root not found");
    }

}
