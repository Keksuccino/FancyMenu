package de.keksuccino.fancymenu.customization.placeholder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Resolves {@code $$name} references, the short way to use FancyMenu's variables, Listener Variables and element values in texts.
 *
 * <p>Names can start with other names, like {@code coins} and {@code coins_record}. So the text gets read once from left to right,
 * and at every {@code $$} the longest name that starts right after it wins. {@code $$coins_record} therefore never resolves to the
 * value of {@code coins} followed by {@code _record}, no matter which other names exist or in which order they are stored.</p>
 */
public final class ShortReferenceResolver {

    public static final String PREFIX = "$$";

    private ShortReferenceResolver() {
    }

    /**
     * Replaces every reference in the text that the lookup finds a name for.
     *
     * <p>Replacements are not searched for references again, and text that a reference kept as it is gets skipped completely.</p>
     *
     * @param lookup Gets asked for the reference at every {@code $$} that is not part of an earlier reference.
     */
    @NotNull
    public static String resolve(@NotNull String text, @NotNull ReferenceLookup lookup) {
        int prefixIndex = text.indexOf(PREFIX);
        StringBuilder resolved = null;
        int copiedUntil = 0;
        while (prefixIndex >= 0) {
            int nameStart = prefixIndex + PREFIX.length();
            Reference reference = lookup.find(text, nameStart);
            if (reference == null) {
                prefixIndex = text.indexOf(PREFIX, prefixIndex + 1);
                continue;
            }
            int nameEnd = nameStart + reference.nameLength();
            if (reference.replacement() != null) {
                if (resolved == null) resolved = new StringBuilder(text.length() + 16);
                resolved.append(text, copiedUntil, prefixIndex).append(reference.replacement());
                copiedUntil = nameEnd;
            }
            prefixIndex = text.indexOf(PREFIX, nameEnd);
        }
        if (resolved == null) return text;
        return resolved.append(text, copiedUntil, text.length()).toString();
    }

    /**
     * Finds the candidate with the longest name that starts at the given index of the text. Empty names never match.
     *
     * @return The candidate, or {@code null} if no name starts there.
     */
    @Nullable
    public static <T> T findLongestName(@NotNull String text, int nameStart, @NotNull Iterable<T> candidates, @NotNull Function<T, String> nameGetter) {
        T longest = null;
        int longestLength = 0;
        for (T candidate : candidates) {
            String name = nameGetter.apply(candidate);
            if ((name.length() > longestLength) && text.startsWith(name, nameStart)) {
                longest = candidate;
                longestLength = name.length();
            }
        }
        return longest;
    }

    @FunctionalInterface
    public interface ReferenceLookup {

        /**
         * @param nameStart The index right after the {@code $$}.
         * @return The reference that starts there, or {@code null} if there is none.
         */
        @Nullable
        Reference find(@NotNull String text, int nameStart);

    }

    /**
     * @param nameLength The length of the reference's name, without the {@code $$}.
     * @param replacement What the reference gets replaced with, or {@code null} to keep the reference as it is, so a later resolve can still replace it.
     */
    public record Reference(int nameLength, @Nullable String replacement) {
    }

}
