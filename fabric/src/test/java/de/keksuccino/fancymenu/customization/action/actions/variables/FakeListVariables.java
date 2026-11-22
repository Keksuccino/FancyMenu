package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.ListArrangement;
import de.keksuccino.fancymenu.customization.variables.ListEditResult;
import de.keksuccino.fancymenu.customization.variables.VariableLists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Lists of the list actions under test, with the edits of {@link VariableLists} and the rules of the variable store for missing and
 * text variables: a missing variable or an empty text variable becomes a list, and a text variable with a value stays as it is.
 */
final class FakeListVariables implements AbstractListAction.ListVariableAccess {

    final Map<String, List<String>> lists = new HashMap<>();
    final Map<String, String> texts = new HashMap<>();
    /** The names of all edits, in the order they got asked for. */
    final List<String> editedNames = new ArrayList<>();

    @NotNull
    FakeListVariables withList(@NotNull String name, @NotNull String... entries) {
        this.lists.put(name, new ArrayList<>(List.of(entries)));
        return this;
    }

    @NotNull
    FakeListVariables withText(@NotNull String name, @NotNull String value) {
        this.texts.put(name, value);
        return this;
    }

    @NotNull
    private ListEditResult edit(@NotNull String name, @NotNull Function<List<String>, ListEditResult> edit) {
        this.editedNames.add(name);
        String text = this.texts.get(name);
        if ((text != null) && !text.isEmpty()) return ListEditResult.NOT_A_LIST;
        List<String> existing = this.lists.get(name);
        boolean claimed = existing == null;
        List<String> entries = claimed ? new ArrayList<>() : existing;
        ListEditResult result = edit.apply(entries);
        if (result == ListEditResult.INDEX_OUT_OF_RANGE) return result;
        if (claimed) {
            this.texts.remove(name);
            this.lists.put(name, entries);
            return ListEditResult.CHANGED;
        }
        return result;
    }

    @Override
    public @NotNull ListEditResult setList(@NotNull String name, @NotNull List<String> entries) {
        return this.edit(name, list -> VariableLists.replaceAll(list, entries));
    }

    @Override
    public @NotNull ListEditResult addListEntry(@NotNull String name, @NotNull String entry, @Nullable Integer index, boolean skipIfContained, boolean ignoreCase) {
        return this.edit(name, list -> VariableLists.add(list, entry, index, skipIfContained, ignoreCase));
    }

    @Override
    public @NotNull ListEditResult setListEntry(@NotNull String name, int index, @NotNull String entry) {
        return this.edit(name, list -> VariableLists.set(list, index, entry));
    }

    @Override
    public @NotNull ListEditResult removeListEntryAt(@NotNull String name, int index) {
        return this.edit(name, list -> VariableLists.removeAt(list, index));
    }

    @Override
    public @NotNull ListEditResult removeListEntries(@NotNull String name, @NotNull String entry, boolean allMatches, boolean ignoreCase) {
        return this.edit(name, list -> VariableLists.remove(list, entry, allMatches, ignoreCase));
    }

    @Override
    public @NotNull ListEditResult clearList(@NotNull String name) {
        return this.edit(name, VariableLists::clear);
    }

    @Override
    public @NotNull ListEditResult arrangeList(@NotNull String name, @NotNull ListArrangement arrangement) {
        return this.edit(name, list -> VariableLists.arrange(list, arrangement, new Random(0)));
    }

}
