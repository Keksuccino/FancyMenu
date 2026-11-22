package de.keksuccino.fancymenu.customization.variables;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Everything about the entries of list variables that doesn't need the variable store: their text form, reading text that gets
 * assigned to a list, indexes, and the edits of the list actions.
 *
 * <p><b>Text form:</b> a compact JSON array of strings, like {@code ["apple","banana"]}. It is lossless, so assigning the text form
 * of a list to another list gives the same entries, and the JSON placeholder can read it.</p>
 *
 * <p><b>Indexes:</b> the first entry has index 0, like in the Split Text placeholder. Negative indexes count from the end, so -1 is the
 * last entry.</p>
 *
 * <p>The edits only work on the list they get and never call foreign code, because the store runs them while it holds its lock. They
 * leave the list as it was whenever they don't return {@link ListEditResult#CHANGED}.</p>
 */
public final class VariableLists {

    public static final String EMPTY_LIST_TEXT = "[]";

    /** The adapter of JSON elements, which reads nested arrays and objects with the strictness of the reader it gets (unlike JsonParser, which makes readers lenient). */
    private static final TypeAdapter<JsonElement> JSON_ELEMENT_ADAPTER = new Gson().getAdapter(JsonElement.class);
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private VariableLists() {
    }

    /**
     * Returns the text form of the entries: a compact JSON array of strings.
     */
    @NotNull
    public static String toText(@NotNull List<String> entries) {
        int size = entries.size();
        if (size == 0) return EMPTY_LIST_TEXT;
        StringBuilder builder = new StringBuilder(2 + (size * 16));
        builder.append('[');
        for (int i = 0; i < size; i++) {
            if (i > 0) builder.append(',');
            appendJsonString(builder, entries.get(i));
        }
        builder.append(']');
        return builder.toString();
    }

    private static void appendJsonString(@NotNull StringBuilder builder, @NotNull String value) {
        builder.append('"');
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                // JSON allows these two, but JavaScript strings don't, so they get escaped like Gson does it
                case ' ', ' ' -> appendUnicodeEscape(builder, c);
                default -> {
                    if (c < 0x20) {
                        appendUnicodeEscape(builder, c);
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static void appendUnicodeEscape(@NotNull StringBuilder builder, char c) {
        builder.append("\\u").append(HEX_DIGITS[(c >> 12) & 0xF]).append(HEX_DIGITS[(c >> 8) & 0xF]).append(HEX_DIGITS[(c >> 4) & 0xF]).append(HEX_DIGITS[c & 0xF]);
    }

    /**
     * Reads text that is exactly one JSON array as entries. Strings become their text, numbers and booleans the JSON text they have
     * ({@code 1.50} stays {@code 1.50}), {@code null} becomes an empty entry, and nested arrays and objects become their compact JSON text.
     *
     * @return The entries as a new mutable list, or {@code null} if the text is no valid JSON array, also when something follows the array.
     */
    @Nullable
    public static List<String> parseText(@NotNull String text) {
        // Most text that gets assigned to lists is no JSON, so text that can't be an array never starts the JSON reader
        int start = 0;
        int end = text.length() - 1;
        while ((start <= end) && Character.isWhitespace(text.charAt(start))) start++;
        while ((end >= start) && Character.isWhitespace(text.charAt(end))) end--;
        if ((start >= end) || (text.charAt(start) != '[') || (text.charAt(end) != ']')) return null;
        // A JsonReader is strict unless it gets set lenient, in all Gson versions of the supported game versions
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            List<String> entries = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
                JsonToken token = reader.peek();
                switch (token) {
                    case STRING, NUMBER -> entries.add(reader.nextString());
                    case BOOLEAN -> entries.add(Boolean.toString(reader.nextBoolean()));
                    case NULL -> {
                        reader.nextNull();
                        entries.add("");
                    }
                    default -> entries.add(JSON_ELEMENT_ADAPTER.read(reader).toString());
                }
            }
            reader.endArray();
            return (reader.peek() == JsonToken.END_DOCUMENT) ? entries : null;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * Turns text that gets assigned to a list variable into its entries, for example through the Set Variable action, a command or an
     * input field: a JSON array becomes its entries, blank text an empty list, and any other text the only entry.
     *
     * @return The entries as a new mutable list.
     */
    @NotNull
    public static List<String> fromAssignedText(@Nullable String text) {
        if ((text == null) || text.isBlank()) return new ArrayList<>();
        List<String> entries = parseText(text);
        if (entries != null) return entries;
        entries = new ArrayList<>(1);
        entries.add(text);
        return entries;
    }

    /**
     * Returns the shortest text that gives exactly these entries again when it gets assigned to a list (the inverse of
     * {@link #fromAssignedText(String)}): an empty list gives empty text, a list with one entry gives that entry, and everything else the
     * text form. Things that show and edit a single value, like input fields and checkboxes, use it, so a list linked to them keeps the
     * value they wrote instead of turning it into JSON.
     *
     * <p>A single entry that is blank or is itself a JSON array would read back as something else, so it gets the text form too.</p>
     */
    @NotNull
    public static String toSingleValueText(@NotNull List<String> entries) {
        if (entries.isEmpty()) return "";
        if (entries.size() == 1) {
            String entry = entries.get(0);
            if (!entry.isBlank() && (parseText(entry) == null)) return entry;
        }
        return toText(entries);
    }

    /**
     * Splits text into entries at every occurrence of the separator, which is plain text (no regex). An empty separator splits the text
     * at its line breaks. Empty text gives no entries.
     *
     * @param trimEntries If whitespace at the start and end of every entry gets removed.
     * @param skipEmptyEntries If entries that are empty (after trimming them) get left out.
     * @return The entries as a new mutable list.
     */
    @NotNull
    public static List<String> split(@NotNull String text, @NotNull String separator, boolean trimEntries, boolean skipEmptyEntries) {
        List<String> entries = new ArrayList<>();
        if (text.isEmpty()) return entries;
        int start = 0;
        if (separator.isEmpty()) {
            int length = text.length();
            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                if ((c != '\n') && (c != '\r')) continue;
                addSplitPart(entries, text.substring(start, i), trimEntries, skipEmptyEntries);
                if ((c == '\r') && ((i + 1) < length) && (text.charAt(i + 1) == '\n')) i++;
                start = i + 1;
            }
        } else {
            int index;
            while ((index = text.indexOf(separator, start)) >= 0) {
                addSplitPart(entries, text.substring(start, index), trimEntries, skipEmptyEntries);
                start = index + separator.length();
            }
        }
        addSplitPart(entries, text.substring(start), trimEntries, skipEmptyEntries);
        return entries;
    }

    private static void addSplitPart(@NotNull List<String> entries, @NotNull String part, boolean trim, boolean skipEmpty) {
        String entry = trim ? part.strip() : part;
        if (!skipEmpty || !entry.isEmpty()) entries.add(entry);
    }

    /**
     * Reads an index that got typed in or comes from placeholders. Whole numbers like {@code 2}, {@code -1} or {@code 2.0} (what
     * calculating placeholders return) work, whitespace around them gets ignored.
     *
     * @return The index, or {@code null} if the text is no whole number that fits an int.
     */
    @Nullable
    public static Integer parseIndex(@Nullable String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
        }
        try {
            double value = Double.parseDouble(trimmed);
            if ((value == Math.rint(value)) && (value >= Integer.MIN_VALUE) && (value <= Integer.MAX_VALUE)) return (int) value;
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * Resolves the index of an existing entry. Negative indexes count from the end, so -1 is the last entry.
     *
     * @return The index from 0 to {@code size - 1}, or -1 if the list has no entry at the index.
     */
    public static int resolveEntryIndex(int index, int size) {
        long resolved = (index < 0) ? ((long) size + index) : index;
        return ((resolved >= 0) && (resolved < size)) ? (int) resolved : -1;
    }

    /**
     * Resolves where a new entry goes, so it has the given index afterward. Negative indexes count from the end, so -1 makes it the last
     * entry and -2 puts it before the last one. Indexes past either end put it at that end.
     *
     * @return The insertion index from 0 to {@code size}.
     */
    public static int resolveInsertIndex(int index, int size) {
        long resolved = (index < 0) ? ((long) size + 1 + index) : index;
        return (int) Math.max(0L, Math.min(size, resolved));
    }

    /**
     * @return The index of the first entry that matches the value, or -1 if no entry matches.
     */
    public static int indexOf(@NotNull List<String> entries, @NotNull String value, boolean ignoreCase) {
        if (!ignoreCase) return entries.indexOf(value);
        int size = entries.size();
        for (int i = 0; i < size; i++) {
            if (entries.get(i).equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    /**
     * Adds an entry at the end of the list, or at the index (see {@link #resolveInsertIndex(int, int)}).
     *
     * @param index The index the new entry gets, or {@code null} to add it at the end.
     * @param skipIfContained If nothing gets added when the list already has an entry with the value.
     */
    @NotNull
    public static ListEditResult add(@NotNull List<String> entries, @NotNull String entry, @Nullable Integer index, boolean skipIfContained, boolean ignoreCase) {
        Objects.requireNonNull(entry);
        if (skipIfContained && (indexOf(entries, entry, ignoreCase) >= 0)) return ListEditResult.UNCHANGED;
        if (index == null) {
            entries.add(entry);
        } else {
            entries.add(resolveInsertIndex(index, entries.size()), entry);
        }
        return ListEditResult.CHANGED;
    }

    /**
     * Replaces the entry at the index (see {@link #resolveEntryIndex(int, int)}).
     */
    @NotNull
    public static ListEditResult set(@NotNull List<String> entries, int index, @NotNull String entry) {
        Objects.requireNonNull(entry);
        int resolved = resolveEntryIndex(index, entries.size());
        if (resolved < 0) return ListEditResult.INDEX_OUT_OF_RANGE;
        if (entries.get(resolved).equals(entry)) return ListEditResult.UNCHANGED;
        entries.set(resolved, entry);
        return ListEditResult.CHANGED;
    }

    /**
     * Removes the entry at the index (see {@link #resolveEntryIndex(int, int)}).
     */
    @NotNull
    public static ListEditResult removeAt(@NotNull List<String> entries, int index) {
        int resolved = resolveEntryIndex(index, entries.size());
        if (resolved < 0) return ListEditResult.INDEX_OUT_OF_RANGE;
        entries.remove(resolved);
        return ListEditResult.CHANGED;
    }

    /**
     * Removes the first entry that matches the value, or all of them.
     */
    @NotNull
    public static ListEditResult remove(@NotNull List<String> entries, @NotNull String entry, boolean allMatches, boolean ignoreCase) {
        Objects.requireNonNull(entry);
        if (!allMatches) {
            int index = indexOf(entries, entry, ignoreCase);
            if (index < 0) return ListEditResult.UNCHANGED;
            entries.remove(index);
            return ListEditResult.CHANGED;
        }
        boolean removed = ignoreCase ? entries.removeIf(entry::equalsIgnoreCase) : entries.removeIf(entry::equals);
        return removed ? ListEditResult.CHANGED : ListEditResult.UNCHANGED;
    }

    @NotNull
    public static ListEditResult clear(@NotNull List<String> entries) {
        if (entries.isEmpty()) return ListEditResult.UNCHANGED;
        entries.clear();
        return ListEditResult.CHANGED;
    }

    /**
     * Replaces all entries of the list with the new ones.
     */
    @NotNull
    public static ListEditResult replaceAll(@NotNull List<String> entries, @NotNull List<String> newEntries) {
        if (entries.equals(newEntries)) return ListEditResult.UNCHANGED;
        for (String entry : newEntries) Objects.requireNonNull(entry);
        entries.clear();
        entries.addAll(newEntries);
        return ListEditResult.CHANGED;
    }

    /**
     * Rearranges the entries, see {@link ListArrangement}. All sorts are stable, so entries that sort the same keep their order.
     *
     * @param random The randomness of {@link ListArrangement#SHUFFLE}.
     */
    @NotNull
    public static ListEditResult arrange(@NotNull List<String> entries, @NotNull ListArrangement arrangement, @NotNull Random random) {
        if (entries.size() < 2) return ListEditResult.UNCHANGED;
        List<String> before = new ArrayList<>(entries);
        switch (arrangement) {
            case SORT_ASCENDING -> entries.sort(alphabeticalOrder());
            case SORT_DESCENDING -> entries.sort(alphabeticalOrder().reversed());
            case SORT_NUMBERS_ASCENDING -> sortNumbers(entries, true);
            case SORT_NUMBERS_DESCENDING -> sortNumbers(entries, false);
            case REVERSE -> Collections.reverse(entries);
            case SHUFFLE -> Collections.shuffle(entries, random);
            case REMOVE_DUPLICATES -> {
                LinkedHashSet<String> unique = new LinkedHashSet<>(entries);
                if (unique.size() == entries.size()) return ListEditResult.UNCHANGED;
                entries.clear();
                entries.addAll(unique);
                return ListEditResult.CHANGED;
            }
        }
        return entries.equals(before) ? ListEditResult.UNCHANGED : ListEditResult.CHANGED;
    }

    /**
     * Alphabetical order of the root locale, so accented letters sort next to the letters they are based on and case only decides between
     * entries that are otherwise the same. Collators are not thread-safe, so every sort gets its own.
     */
    @NotNull
    private static Comparator<String> alphabeticalOrder() {
        Collator collator = Collator.getInstance(Locale.ROOT);
        return collator::compare;
    }

    private static void sortNumbers(@NotNull List<String> entries, boolean ascending) {
        List<NumberEntry> numbers = new ArrayList<>(entries.size());
        List<String> others = new ArrayList<>();
        for (String entry : entries) {
            double value = parseNumber(entry);
            if (Double.isNaN(value)) {
                others.add(entry);
            } else {
                numbers.add(new NumberEntry(value, entry));
            }
        }
        Comparator<NumberEntry> order = Comparator.comparingDouble(NumberEntry::value);
        numbers.sort(ascending ? order : order.reversed());
        entries.clear();
        for (NumberEntry number : numbers) entries.add(number.entry());
        entries.addAll(others);
    }

    /**
     * @return The number of the entry, or {@code NaN} if the entry is no finite number.
     */
    private static double parseNumber(@NotNull String entry) {
        String trimmed = entry.trim();
        if (trimmed.isEmpty()) return Double.NaN;
        try {
            double value = Double.parseDouble(trimmed);
            return Double.isFinite(value) ? value : Double.NaN;
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private record NumberEntry(double value, @NotNull String entry) {
    }

}
