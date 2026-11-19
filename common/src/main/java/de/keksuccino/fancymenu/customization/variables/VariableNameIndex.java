package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable lookup of the variables of one {@link VariableStore} state by the {@code $$name} reference that starts at a position of a text.
 *
 * <p>The variables are grouped by the first character (UTF-16 code unit) of their name, and every group is sorted longest name first,
 * so the first name of a group that starts at the position is the longest one that starts there. Two names of the same length can't
 * both start there, because the names of a store are unique. Empty names are left out, so they never match.</p>
 *
 * <p>The index holds the {@link Variable} objects of the store's map, and reading their values stays the store's job. The store has to
 * build a new index whenever the entries of its map change, which includes replacing a variable with a new object of the same name.
 * Value changes need no new index. Names never change, so finding names works without the store's lock, which is what
 * {@link #findLongestNameLength(String, int)} is for: code that only needs the names can use an index without blocking the variables.</p>
 */
final class VariableNameIndex implements ShortReferenceResolver.NameLookup {

    private static final int ASCII_GROUP_COUNT = 128;
    private static final Comparator<Variable> BY_FIRST_CHARACTER = (first, second) -> Character.compare(first.getName().charAt(0), second.getName().charAt(0));

    static final VariableNameIndex EMPTY = new VariableNameIndex(0L, new Variable[ASCII_GROUP_COUNT][], new char[0], new Variable[0][], true);

    private final long revision;
    /** Groups of names that start with an ASCII character, at the index of that character, or {@code null} for no names. */
    private final Variable[][] asciiGroups;
    /** First characters of all other groups, sorted ascending for a binary search. */
    private final char[] otherFirstCharacters;
    private final Variable[][] otherGroups;
    private final boolean empty;

    private VariableNameIndex(long revision, @NotNull Variable[][] asciiGroups, @NotNull char[] otherFirstCharacters, @NotNull Variable[][] otherGroups, boolean empty) {
        this.revision = revision;
        this.asciiGroups = asciiGroups;
        this.otherFirstCharacters = otherFirstCharacters;
        this.otherGroups = otherGroups;
        this.empty = empty;
    }

    /**
     * Builds the index with one pass over the store's map: the variables get bucketed by their first character, then each group gets
     * sorted by name length. Scripts can change the variable set often (creating and deleting variables), so a rebuild has to stay cheap.
     *
     * @param revision The revision of the store's variable set the variables belong to.
     */
    @NotNull
    static VariableNameIndex build(long revision, @NotNull Collection<Variable> variables) {
        Variable[] named = new Variable[variables.size()];
        int namedCount = 0;
        int[] asciiGroupSizes = new int[ASCII_GROUP_COUNT];
        int otherCount = 0;
        for (Variable variable : variables) {
            String name = variable.getName();
            if (name.isEmpty()) continue;
            named[namedCount++] = variable;
            char firstCharacter = name.charAt(0);
            if (firstCharacter < ASCII_GROUP_COUNT) {
                asciiGroupSizes[firstCharacter]++;
            } else {
                otherCount++;
            }
        }
        Variable[][] asciiGroups = new Variable[ASCII_GROUP_COUNT][];
        if (namedCount == 0) return new VariableNameIndex(revision, asciiGroups, new char[0], new Variable[0][], true);
        for (int character = 0; character < ASCII_GROUP_COUNT; character++) {
            if (asciiGroupSizes[character] > 0) asciiGroups[character] = new Variable[asciiGroupSizes[character]];
        }
        Variable[] otherVariables = new Variable[otherCount];
        otherCount = 0;
        for (int i = 0; i < namedCount; i++) {
            Variable variable = named[i];
            char firstCharacter = variable.getName().charAt(0);
            // Fills every group from its end, counting its size down to 0.
            if (firstCharacter < ASCII_GROUP_COUNT) {
                asciiGroups[firstCharacter][--asciiGroupSizes[firstCharacter]] = variable;
            } else {
                otherVariables[otherCount++] = variable;
            }
        }
        for (Variable[] group : asciiGroups) {
            if (group != null) sortLongestNameFirst(group);
        }
        if (otherVariables.length == 0) return new VariableNameIndex(revision, asciiGroups, new char[0], new Variable[0][], false);
        // Names that start with a non-ASCII character are rare, so a comparator sort is fine for them.
        Arrays.sort(otherVariables, BY_FIRST_CHARACTER);
        StringBuilder otherFirstCharacters = new StringBuilder();
        List<Variable[]> otherGroups = new ArrayList<>();
        int groupStart = 0;
        while (groupStart < otherVariables.length) {
            char firstCharacter = otherVariables[groupStart].getName().charAt(0);
            int groupEnd = groupStart + 1;
            while ((groupEnd < otherVariables.length) && (otherVariables[groupEnd].getName().charAt(0) == firstCharacter)) groupEnd++;
            Variable[] group = Arrays.copyOfRange(otherVariables, groupStart, groupEnd);
            sortLongestNameFirst(group);
            otherFirstCharacters.append(firstCharacter);
            otherGroups.add(group);
            groupStart = groupEnd;
        }
        return new VariableNameIndex(revision, asciiGroups, otherFirstCharacters.toString().toCharArray(), otherGroups.toArray(new Variable[0][]), false);
    }

    /**
     * Sorts the group by name length, longest first, through a primitive sort of keys that hold the inverted length in the upper and the
     * position in the lower 32 bits. Both fit exactly, because lengths and positions are never negative ints.
     */
    private static void sortLongestNameFirst(@NotNull Variable[] group) {
        if (group.length < 2) return;
        long[] keys = new long[group.length];
        for (int i = 0; i < group.length; i++) keys[i] = ((long) (Integer.MAX_VALUE - group[i].getName().length()) << 32) | i;
        Arrays.sort(keys);
        Variable[] unsorted = group.clone();
        for (int i = 0; i < group.length; i++) group[i] = unsorted[(int) keys[i]];
    }

    long revision() {
        return this.revision;
    }

    boolean isEmpty() {
        return this.empty;
    }

    /**
     * Finds the variable with the longest name that starts at the given index of the text.
     *
     * @return The variable, or {@code null} if no name starts there.
     */
    @Nullable
    Variable findLongest(@NotNull String text, int nameStart) {
        if (nameStart >= text.length()) return null;
        Variable[] group = this.getGroup(text.charAt(nameStart));
        if (group == null) return null;
        for (Variable variable : group) {
            if (text.startsWith(variable.getName(), nameStart)) return variable;
        }
        return null;
    }

    @Override
    public int findLongestNameLength(@NotNull String text, int nameStart) {
        Variable variable = this.findLongest(text, nameStart);
        return (variable != null) ? variable.getName().length() : 0;
    }

    @Nullable
    private Variable[] getGroup(char firstCharacter) {
        if (firstCharacter < ASCII_GROUP_COUNT) return this.asciiGroups[firstCharacter];
        int index = Arrays.binarySearch(this.otherFirstCharacters, firstCharacter);
        return (index >= 0) ? this.otherGroups[index] : null;
    }

}
