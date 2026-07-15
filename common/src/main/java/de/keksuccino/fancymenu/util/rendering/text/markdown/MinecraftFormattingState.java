package de.keksuccino.fancymenu.util.rendering.text.markdown;

import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatter;
import de.keksuccino.fancymenu.util.rendering.text.color.TextColorFormatterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Tracks Minecraft formatting independently from Markdown formatting.
 *
 * <p>Vanilla codes retain their native transition behavior inside {@link #vanillaStyle}, including color codes
 * clearing earlier Vanilla decorations. Custom FancyMenu colors are tracked separately because {@code §r} resets
 * only Vanilla state and must reveal the last custom color again. Only active Vanilla decorations and the currently
 * selected color source are merged into Markdown, so Vanilla reset values cannot erase Markdown formatting.</p>
 */
final class MinecraftFormattingState {

    static final MinecraftFormattingState EMPTY = new MinecraftFormattingState(Style.EMPTY, null, ColorSource.NONE);

    @NotNull
    private final Style vanillaStyle;
    @Nullable
    private final TextColorFormatter customColorFormatter;
    @NotNull
    private final ColorSource colorSource;

    private MinecraftFormattingState(@NotNull Style vanillaStyle, @Nullable TextColorFormatter customColorFormatter, @NotNull ColorSource colorSource) {
        this.vanillaStyle = Objects.requireNonNull(vanillaStyle);
        this.customColorFormatter = customColorFormatter;
        this.colorSource = Objects.requireNonNull(colorSource);
    }

    @Nullable
    static ChatFormatting getSectionFormattingAt(@NotNull String text, int prefixIndex) {
        Objects.requireNonNull(text);
        if (!hasPrefixAndCode(text, prefixIndex, ChatFormatting.PREFIX_CODE)) return null;
        return ChatFormatting.getByCode(text.charAt(prefixIndex + 1));
    }

    @Nullable
    static ChatFormatting getAmpersandFormattingAt(@NotNull String text, int prefixIndex) {
        Objects.requireNonNull(text);
        if (!hasPrefixAndCode(text, prefixIndex, '&')) return null;
        char code = text.charAt(prefixIndex + 1);
        if (code != Character.toLowerCase(code)) return null;
        return ChatFormatting.getByCode(code);
    }

    @Nullable
    static TextColorFormatter getCustomFormattingAt(@NotNull String text, int prefixIndex) {
        Objects.requireNonNull(text);
        if (!hasPrefixAndCode(text, prefixIndex, ChatFormatting.PREFIX_CODE)) return null;
        if (ChatFormatting.getByCode(text.charAt(prefixIndex + 1)) != null) return null;
        return TextColorFormatterRegistry.getByCode(text.charAt(prefixIndex + 1));
    }

    private static boolean hasPrefixAndCode(@NotNull String text, int prefixIndex, char prefix) {
        return (prefixIndex >= 0) && (prefixIndex < text.length()) && (text.charAt(prefixIndex) == prefix) && ((prefixIndex + 1) < text.length());
    }

    @NotNull
    MinecraftFormattingState apply(@NotNull ChatFormatting formatting) {
        Objects.requireNonNull(formatting);
        Style updatedVanillaStyle = this.vanillaStyle.applyLegacyFormat(formatting);
        ColorSource updatedColorSource = this.colorSource;
        if (formatting == ChatFormatting.RESET) {
            updatedColorSource = this.customColorFormatter != null ? ColorSource.CUSTOM : ColorSource.NONE;
        } else if (TextColor.fromLegacyFormat(formatting) != null) {
            updatedColorSource = ColorSource.VANILLA;
        }
        if (updatedVanillaStyle.isEmpty() && (this.customColorFormatter == null)) return EMPTY;
        return new MinecraftFormattingState(updatedVanillaStyle, this.customColorFormatter, updatedColorSource);
    }

    @NotNull
    MinecraftFormattingState apply(@NotNull TextColorFormatter formatter) {
        return new MinecraftFormattingState(this.vanillaStyle, Objects.requireNonNull(formatter), ColorSource.CUSTOM);
    }

    @NotNull
    Style applyTo(@NotNull Style markdownStyle) {
        Style combined = Objects.requireNonNull(markdownStyle);
        if ((this.colorSource == ColorSource.VANILLA) && (this.vanillaStyle.getColor() != null)) combined = combined.withColor(this.vanillaStyle.getColor());
        if ((this.colorSource == ColorSource.CUSTOM) && (this.customColorFormatter != null)) combined = combined.withColor(this.customColorFormatter.getColor().getColorInt());
        if (this.vanillaStyle.isBold()) combined = combined.withBold(true);
        if (this.vanillaStyle.isItalic()) combined = combined.withItalic(true);
        if (this.vanillaStyle.isUnderlined()) combined = combined.withUnderlined(true);
        if (this.vanillaStyle.isStrikethrough()) combined = combined.withStrikethrough(true);
        if (this.vanillaStyle.isObfuscated()) combined = combined.withObfuscated(true);
        return combined;
    }

    private enum ColorSource {
        NONE,
        VANILLA,
        CUSTOM
    }

}
