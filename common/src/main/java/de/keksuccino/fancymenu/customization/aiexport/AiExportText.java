package de.keksuccino.fancymenu.customization.aiexport;

import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Turns names and descriptions into plain text for AI exports.
 */
public final class AiExportText {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern FORMAT_ARGUMENT_PATTERN = Pattern.compile("%(\\d+\\$)?s");

    private AiExportText() {
    }

    /**
     * @return The text of the component, without formatting codes.
     */
    @NotNull
    public static String plain(@Nullable Component component) {
        if (component == null) return "";
        return plain(component.getString());
    }

    /**
     * @return The text without formatting codes, with surrounding whitespace removed.
     */
    @NotNull
    public static String plain(@Nullable String text) {
        if (text == null) return "";
        String stripped = ChatFormatting.stripFormatting(text);
        return (stripped != null) ? stripped.strip() : "";
    }

    /**
     * @return The lines as one plain text, with one line per component.
     */
    @NotNull
    public static String plainLines(@Nullable Collection<? extends Component> lines) {
        if (lines == null) return "";
        StringBuilder builder = new StringBuilder();
        for (Component line : lines) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(plain(line));
        }
        return builder.toString().strip();
    }

    /**
     * @return The lines as one plain text, with one line per component.
     */
    @NotNull
    public static String plainLines(@Nullable Component[] lines) {
        if (lines == null) return "";
        return plainLines(Arrays.asList(lines));
    }

    /**
     * @return The lines as one plain text, with one line per string.
     */
    @NotNull
    public static String plainStringLines(@Nullable Collection<String> lines) {
        if (lines == null) return "";
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(plain(line));
        }
        return builder.toString().strip();
    }

    /**
     * Gets text from code of add-ons or other parts that could fail, so one broken name or description does not break the whole export.
     */
    @NotNull
    public static String safe(@NotNull Supplier<String> supplier, @NotNull String fallback) {
        try {
            String s = supplier.get();
            return (s != null) ? s : fallback;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to get text for an AI export!", ex);
            return fallback;
        }
    }

    /**
     * @return The localized text as plain text, with blank lines between paragraphs removed, or an empty string if the key does not exist.
     */
    @NotNull
    public static String localized(@NotNull String localizationKey) {
        Language language = Language.getInstance();
        if (!language.has(localizationKey)) return "";
        return plain(language.getOrDefault(localizationKey)).replace("\n\n", "\n");
    }

    /**
     * Turns the label of a menu entry into a name, like "Stretch Horizontally: %s" into "Stretch Horizontally".
     *
     * @return The label, or NULL if the localization key does not exist.
     */
    @Nullable
    public static String labelOfLocalizationKey(@Nullable String localizationKey) {
        if ((localizationKey == null) || localizationKey.isBlank()) return null;
        Language language = Language.getInstance();
        if (!language.has(localizationKey)) return null;
        String label = plain(FORMAT_ARGUMENT_PATTERN.matcher(language.getOrDefault(localizationKey)).replaceAll(""));
        int lineBreak = label.indexOf('\n');
        if (lineBreak >= 0) label = label.substring(0, lineBreak);
        label = label.strip();
        while (label.endsWith(":") || label.endsWith("-")) {
            label = label.substring(0, label.length() - 1).strip();
        }
        return label.isEmpty() ? null : label;
    }

    /**
     * @return The absolute path of the file, without parts like {@code /./} that make it harder to read.
     */
    @NotNull
    public static String path(@NotNull File file) {
        return file.toPath().toAbsolutePath().normalize().toString();
    }

    @NotNull
    public static String yesNo(boolean b) {
        return b ? "yes" : "no";
    }

}
