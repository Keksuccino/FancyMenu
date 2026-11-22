package de.keksuccino.fancymenu.customization.variables;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Text helpers of the variable windows: the separators of the conversions between text and list variables, and the single-line
 * previews of values and entries.
 *
 * <p><b>Separators</b> get typed into single-line fields, so {@code \n} in them stands for a line break. A separator that is
 * exactly one line break splits at every kind of line break ({@code \n}, {@code \r\n} and {@code \r}). An empty separator doesn't
 * split at all.</p>
 */
public final class VariableTextUtils {

    public static final String ELLIPSIS = "…";
    private static final String ESCAPED_LINE_BREAK = "\\n";

    private VariableTextUtils() {
    }

    /**
     * Turns a separator as it got typed into the separator it stands for, so {@code \n} becomes a line break.
     */
    @NotNull
    public static String resolveSeparator(@NotNull String typedSeparator) {
        return typedSeparator.replace(ESCAPED_LINE_BREAK, "\n");
    }

    /**
     * Splits the text of a text variable into the entries of a list (see the class description for how separators work).
     * Empty text gives no entries.
     *
     * @param trimEntries If whitespace at the start and end of every entry gets removed.
     * @param skipEmptyEntries If entries that are empty (after trimming them) get left out.
     * @return The entries as a new mutable list.
     */
    @NotNull
    public static List<String> splitToEntries(@NotNull String text, @NotNull String typedSeparator, boolean trimEntries, boolean skipEmptyEntries) {
        String separator = resolveSeparator(typedSeparator);
        if (separator.equals("\n")) {
            // An empty separator makes VariableLists split at every kind of line break
            return VariableLists.split(text, "", trimEntries, skipEmptyEntries);
        }
        if (!separator.isEmpty()) return VariableLists.split(text, separator, trimEntries, skipEmptyEntries);
        List<String> entries = new ArrayList<>(1);
        if (text.isEmpty()) return entries;
        String entry = trimEntries ? text.strip() : text;
        if (!skipEmptyEntries || !entry.isEmpty()) entries.add(entry);
        return entries;
    }

    /**
     * Joins the entries of a list into the text of a text variable.
     */
    @NotNull
    public static String joinEntries(@NotNull List<String> entries, @NotNull String typedSeparator) {
        return String.join(resolveSeparator(typedSeparator), entries);
    }

    /**
     * @return If the text contains a line break.
     */
    public static boolean containsLineBreak(@NotNull String text) {
        return (text.indexOf('\n') >= 0) || (text.indexOf('\r') >= 0);
    }

    /**
     * Makes a single line of the text for previews: every line break ({@code \n}, {@code \r\n} or {@code \r}) becomes the given marker,
     * and text after the first {@code maxChars} characters gets cut off with {@link #ELLIPSIS}. Only reads as much of the text as it
     * needs, so previews of huge values stay cheap.
     */
    @NotNull
    public static String toSingleLine(@NotNull String text, @NotNull String lineBreakMarker, int maxChars) {
        StringBuilder builder = new StringBuilder(Math.min(text.length(), maxChars) + 1);
        appendSingleLine(builder, text, lineBreakMarker, maxChars);
        return builder.toString();
    }

    /**
     * Makes a single-line preview of list entries like {@code apple, banana, cherry} (see {@link #toSingleLine(String, String, int)}),
     * cut off with {@link #ELLIPSIS} after the first {@code maxChars} characters.
     */
    @NotNull
    public static String toEntriesPreview(@NotNull List<String> entries, @NotNull String entrySeparator, @NotNull String lineBreakMarker, int maxChars) {
        StringBuilder builder = new StringBuilder(Math.min(maxChars, 64) + 1);
        int size = entries.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                if ((builder.length() + entrySeparator.length()) > maxChars) {
                    builder.append(ELLIPSIS);
                    return builder.toString();
                }
                builder.append(entrySeparator);
            }
            if (!appendSingleLine(builder, entries.get(i), lineBreakMarker, maxChars)) return builder.toString();
        }
        return builder.toString();
    }

    /**
     * @return {@code false} if the builder reached {@code maxChars} and got cut off.
     */
    private static boolean appendSingleLine(@NotNull StringBuilder builder, @NotNull String text, @NotNull String lineBreakMarker, int maxChars) {
        int length = text.length();
        for (int i = 0; i < length; i++) {
            if (builder.length() >= maxChars) {
                // Never keep half of a surrogate pair
                if (!builder.isEmpty() && Character.isHighSurrogate(builder.charAt(builder.length() - 1))) builder.setLength(builder.length() - 1);
                builder.append(ELLIPSIS);
                return false;
            }
            char c = text.charAt(i);
            if ((c == '\n') || (c == '\r')) {
                if ((c == '\r') && ((i + 1) < length) && (text.charAt(i + 1) == '\n')) i++;
                builder.append(lineBreakMarker);
            } else {
                builder.append(c);
            }
        }
        return true;
    }

}
