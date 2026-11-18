package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.placeholder.ShortReferenceResolver;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public interface ValuePlaceholderHolder {

    public static final String VALUE_PLACEHOLDER_PREFIX = ShortReferenceResolver.PREFIX;

    void addValuePlaceholder(@NotNull String placeholder, @NotNull Supplier<String> replaceWithSupplier);

    @NotNull
    Map<String, Supplier<String>> getValuePlaceholders();

    /**
     * Replaces the {@code $$name} references of value placeholders, like Listener Variables and element values.
     *
     * <p>FancyMenu's variables get resolved later by the placeholder parser, so a reference that uses the longer name of
     * a FancyMenu variable stays as it is for that. See {@link #applyValuePlaceholders(String, Map, Collection)}.</p>
     */
    @NotNull
    static String applyValuePlaceholders(@NotNull String value, @NotNull Map<String, Supplier<String>> placeholders) {
        if (placeholders.isEmpty() || !value.contains(VALUE_PLACEHOLDER_PREFIX)) return value;
        return applyValuePlaceholders(value, placeholders, VariableHandler.getVariableNames());
    }

    /**
     * Replaces the {@code $$name} references of value placeholders. At every {@code $$}, the longest name wins,
     * so {@code $$video_source_type} never resolves as {@code $$video_source} followed by {@code _type}.
     *
     * <p>The names of FancyMenu's variables count too: if a variable name that starts there is longer than every value
     * placeholder name, the reference stays as it is, so the placeholder parser can replace it with the variable's value later.
     * With a Checkbox's {@code $$value} and a variable named {@code value_backup}, {@code $$value_backup} stays for the variable.
     * If a value placeholder and a variable have the same name, the value placeholder wins.</p>
     *
     * @param variableNames The names of FancyMenu's variables.
     */
    @NotNull
    static String applyValuePlaceholders(@NotNull String value, @NotNull Map<String, Supplier<String>> placeholders, @NotNull Collection<String> variableNames) {
        if (placeholders.isEmpty()) return value;
        return ShortReferenceResolver.resolve(value, (text, nameStart) -> {
            Map.Entry<String, Supplier<String>> placeholder = ShortReferenceResolver.findLongestName(text, nameStart, placeholders.entrySet(), Map.Entry::getKey);
            int placeholderNameLength = (placeholder != null) ? placeholder.getKey().length() : 0;
            String variableName = ShortReferenceResolver.findLongestName(text, nameStart, variableNames, name -> name);
            if ((variableName != null) && (variableName.length() > placeholderNameLength)) return new ShortReferenceResolver.Reference(variableName.length(), null);
            if (placeholder == null) return null;
            String replacement = placeholder.getValue().get();
            return new ShortReferenceResolver.Reference(placeholderNameLength, (replacement != null) ? replacement : "");
        });
    }

}
