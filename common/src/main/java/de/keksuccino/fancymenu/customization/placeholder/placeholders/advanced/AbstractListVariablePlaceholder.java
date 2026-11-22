package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * The base of the placeholders that read the entries of FancyMenu's list variables. All of them take the name of the list variable as
 * their {@code name} value, and treat missing variables and text variables as no list.
 *
 * <p>Placeholders get replaced every frame, so they read the entries straight from the immutable list the variables keep until the list
 * changes (see {@link VariableHandler#getListEntries(String)}), which copies nothing.</p>
 */
public abstract class AbstractListVariablePlaceholder extends Placeholder {

    protected static final String NAME_VALUE = "name";
    protected static final String DEFAULT_LIST_NAME = "some_list";

    @NotNull
    private final String localizationKey;
    @NotNull
    private final Function<String, List<String>> listLookup;

    /**
     * @param localizationKey The key of the name of the placeholder. The key of its description is this one with ".desc" behind it.
     * @param listLookup Returns the entries of the list variable with the given name, or null if there is no list variable with that name.
     */
    protected AbstractListVariablePlaceholder(@NotNull String identifier, @NotNull String localizationKey, @NotNull Function<String, List<String>> listLookup) {
        super(identifier);
        this.localizationKey = Objects.requireNonNull(localizationKey);
        this.listLookup = Objects.requireNonNull(listLookup);
    }

    /**
     * @return The entries of the list variable the {@code name} value names, or null if the name is missing or there is no list variable with that name.
     */
    @Nullable
    protected List<String> getListEntries(@NotNull DeserializedPlaceholderString dps) {
        String name = dps.values.get(NAME_VALUE);
        if (name == null) return null;
        name = Variable.normalizeReferenceName(name);
        if (name.isEmpty()) return null;
        return this.listLookup.apply(name);
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get(this.localizationKey);
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines(this.localizationKey + ".desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

}
