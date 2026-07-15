package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.text.color.DynamicTextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftFormattingStateTest {

    private static final TextColorFormatter CUSTOM_FORMATTER = new TextColorFormatter('q', DrawableColor.of(new Color(18, 52, 86)));

    @Test
    void supportsEveryVanillaDecorationIncludingUnderlineAndObfuscated() {
        MinecraftFormattingState state = MinecraftFormattingState.EMPTY.apply(ChatFormatting.BOLD).apply(ChatFormatting.ITALIC).apply(ChatFormatting.UNDERLINE).apply(ChatFormatting.STRIKETHROUGH).apply(ChatFormatting.OBFUSCATED);

        Style style = state.applyTo(Style.EMPTY);

        assertTrue(style.isBold());
        assertTrue(style.isItalic());
        assertTrue(style.isUnderlined());
        assertTrue(style.isStrikethrough());
        assertTrue(style.isObfuscated());
    }

    @Test
    void vanillaColorClearsOnlyEarlierVanillaDecorations() {
        Style markdownStyle = Style.EMPTY.withBold(true).withUnderlined(true);
        MinecraftFormattingState state = MinecraftFormattingState.EMPTY.apply(ChatFormatting.ITALIC).apply(ChatFormatting.STRIKETHROUGH).apply(ChatFormatting.RED);

        Style combined = state.applyTo(markdownStyle);

        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), combined.getColor());
        assertTrue(combined.isBold());
        assertTrue(combined.isUnderlined());
        assertFalse(combined.isItalic());
        assertFalse(combined.isStrikethrough());
    }

    @Test
    void resetRestoresTheUnmodifiedMarkdownStyleWithoutCustomColor() {
        Style markdownStyle = Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true).withItalic(true).withStrikethrough(true);
        MinecraftFormattingState reset = MinecraftFormattingState.EMPTY.apply(ChatFormatting.AQUA).apply(ChatFormatting.UNDERLINE).apply(ChatFormatting.RESET);

        assertSame(markdownStyle, reset.applyTo(markdownStyle));
    }

    @Test
    void customColorSurvivesVanillaDecorationAndReset() {
        MinecraftFormattingState decorated = MinecraftFormattingState.EMPTY.apply(CUSTOM_FORMATTER).apply(ChatFormatting.BOLD);
        MinecraftFormattingState reset = decorated.apply(ChatFormatting.RESET);

        assertEquals(customColor(), decorated.applyTo(Style.EMPTY).getColor().getValue());
        assertTrue(decorated.applyTo(Style.EMPTY).isBold());
        assertEquals(customColor(), reset.applyTo(Style.EMPTY).getColor().getValue());
        assertFalse(reset.applyTo(Style.EMPTY).isBold());
    }

    @Test
    void lastColorSourceWinsAndVanillaResetRevealsStoredCustomColor() {
        MinecraftFormattingState vanillaThenCustom = MinecraftFormattingState.EMPTY.apply(ChatFormatting.RED).apply(CUSTOM_FORMATTER);
        MinecraftFormattingState customThenVanilla = MinecraftFormattingState.EMPTY.apply(CUSTOM_FORMATTER).apply(ChatFormatting.RED);
        MinecraftFormattingState revealedCustom = customThenVanilla.apply(ChatFormatting.RESET);

        assertEquals(customColor(), vanillaThenCustom.applyTo(Style.EMPTY).getColor().getValue());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), customThenVanilla.applyTo(Style.EMPTY).getColor());
        assertEquals(customColor(), revealedCustom.applyTo(Style.EMPTY).getColor().getValue());
    }

    @Test
    void customColorIsResolvedDynamicallyWhenAppliedToMarkdown() {
        AtomicReference<DrawableColor> color = new AtomicReference<>(DrawableColor.of(new Color(17, 34, 51)));
        DynamicTextColorFormatter formatter = new DynamicTextColorFormatter('q', color::get);
        MinecraftFormattingState state = MinecraftFormattingState.EMPTY.apply(formatter);

        assertEquals(0x112233, state.applyTo(Style.EMPTY).getColor().getValue());
        color.set(DrawableColor.of(new Color(68, 85, 102)));
        assertEquals(0x445566, state.applyTo(Style.EMPTY).getColor().getValue());
    }

    @Test
    void everyVanillaColorIsRecognizedAndReplacesThePreviousColor() {
        ChatFormatting[] colors = {
                ChatFormatting.BLACK, ChatFormatting.DARK_BLUE, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_AQUA,
                ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE, ChatFormatting.GOLD, ChatFormatting.GRAY,
                ChatFormatting.DARK_GRAY, ChatFormatting.BLUE, ChatFormatting.GREEN, ChatFormatting.AQUA,
                ChatFormatting.RED, ChatFormatting.LIGHT_PURPLE, ChatFormatting.YELLOW, ChatFormatting.WHITE
        };

        MinecraftFormattingState state = MinecraftFormattingState.EMPTY;
        for (ChatFormatting color : colors) {
            state = state.apply(color);
            assertEquals(TextColor.fromLegacyFormat(color), state.applyTo(Style.EMPTY).getColor());
        }
    }

    @Test
    void formattingRecognitionKeepsAmpersandCompatibilityContextual() {
        assertEquals(ChatFormatting.UNDERLINE, MinecraftFormattingState.getAmpersandFormattingAt("&nUnderlined", 0));
        assertEquals(ChatFormatting.RESET, MinecraftFormattingState.getAmpersandFormattingAt("&rReset", 0));
        assertEquals(ChatFormatting.RED, MinecraftFormattingState.getSectionFormattingAt("§cRed", 0));
        assertNull(MinecraftFormattingState.getAmpersandFormattingAt("&AUppercase", 0));
        assertNull(MinecraftFormattingState.getAmpersandFormattingAt("&xHexOrCustom", 0));
        assertNull(MinecraftFormattingState.getSectionFormattingAt("§", 0));
        assertNull(MinecraftFormattingState.getSectionFormattingAt("plain", 0));
    }

    private static int customColor() {
        return CUSTOM_FORMATTER.getColor().getColorInt() & 0xFFFFFF;
    }

}
