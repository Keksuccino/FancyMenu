package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

public class TextHeightPlaceholder extends Placeholder {

    private static final String NEWLINE_PERCENT = "%n%";

    @NotNull
    private final IntSupplier lineHeightSupplier;

    public TextHeightPlaceholder() {
        this(() -> Minecraft.getInstance().font.lineHeight);
    }

    TextHeightPlaceholder(@NotNull IntSupplier lineHeightSupplier) {
        super("text_height");
        this.lineHeightSupplier = Objects.requireNonNull(lineHeightSupplier);
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String text = dps.values.get("text");
        if (text != null) {
            return "" + (countLines(text) * this.lineHeightSupplier.getAsInt());
        }
        return null;
    }

    /**
     * Counts the lines of the given text without allocating line copies.<br>
     * Accepts the same line breaks as FancyMenu's text rendering: {@code %n%}, an escaped {@code \n} (backslash followed by "n"),
     * {@code \n}, {@code \r\n} and {@code \r}. Every line break starts a new line, even at the end of the text, while an empty text has no lines.
     */
    public static int countLines(@NotNull String text) {
        int length = text.length();
        if (length == 0) return 0;
        int lines = 1;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lines++;
            } else if (c == '\r') {
                if (((i + 1) < length) && (text.charAt(i + 1) == '\n')) i++;
                lines++;
            } else if ((c == '\\') && ((i + 1) < length) && (text.charAt(i + 1) == 'n')) {
                lines++;
                i++;
            } else if ((c == '%') && text.startsWith(NEWLINE_PERCENT, i)) {
                lines++;
                i += NEWLINE_PERCENT.length() - 1;
            }
        }
        return lines;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.text_height");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.text_height.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholderIdentifier = this.getIdentifier();
        dps.values.put("text", "This is an example text.%n%And this is its second line.");
        return dps;
    }

}
