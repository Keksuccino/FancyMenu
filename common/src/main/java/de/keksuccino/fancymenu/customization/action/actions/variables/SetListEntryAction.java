package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Replaces the entry at an index of a list variable.
 */
public class SetListEntryAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "set_list_entry";

    public SetListEntryAction() {
        this(VARIABLE_HANDLER);
    }

    SetListEntryAction(@NotNull ListVariableAccess variables) {
        super("set_list_entry", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        Config config = Config.parse(value);
        if (config == null) return;
        String listName = Variable.normalizeReferenceName(config.listName);
        if (listName.isEmpty()) return;
        Integer index = this.parseIndex(listName, config.index);
        if (index == null) return;
        this.logFailedEdit(listName, this.variables.setListEntry(listName, index, config.entry), index);
    }

    @Override
    public @NotNull String getValuePreset() {
        Config config = new Config();
        config.listName = "my_list";
        config.index = "0";
        config.entry = "new_value";
        return config.serialize();
    }

    @Override
    public @Nullable Component getValueSummary(@NotNull String value) {
        Config config = Objects.requireNonNull(Config.parse(value));
        return Component.translatable(LOCALIZATION_KEY + ".summary", config.listName, config.index, quoteForSummary(config.entry));
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new SetListEntryActionValueScreen(value, callback);
    }

    /**
     * What the action does, which is what its value holds.
     */
    public static final class Config {

        @NotNull
        public String listName = "";
        /** The index of the entry to replace. It stays text until the action runs, so it can be a placeholder. */
        @NotNull
        public String index = "";
        /** The new text of the entry. */
        @NotNull
        public String entry = "";

        /**
         * The entry comes last: it is the only part that can hold anything, the separator included.
         */
        @NotNull
        public String serialize() {
            return this.listName + VALUE_SEPARATOR + this.index + VALUE_SEPARATOR + this.entry;
        }

        /**
         * Reads a value. Parts a value does not have keep what a new action starts with.
         */
        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = splitValue(value, 3);
            Config config = new Config();
            config.listName = parts[0];
            if (parts.length >= 2) config.index = parts[1];
            if (parts.length >= 3) config.entry = parts[2];
            return config;
        }

    }

    public static class SetListEntryActionValueScreen extends ListActionValueWindowBody {

        @NotNull
        protected final Config config;

        protected SetListEntryActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.config = Objects.requireNonNullElseGet(Config.parse(value), Config::new);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addListNameCells(this.config.listName, s -> this.config.listName = s);

            this.addCellGroupEndSpacerCell();

            this.addIndexCells(Component.translatable(LIST_LOCALIZATION_KEY + ".index"), Component.translatable(LOCALIZATION_KEY + ".edit.index.desc"), this.config.index, false, s -> this.config.index = s);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.entry"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.entry = s)
                    .setText(this.config.entry)
                    .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.entry.desc")));

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.config.listName.isBlank() && this.isIndexInputValid();
        }

        @Override
        protected @NotNull String serialize() {
            return this.config.serialize();
        }

    }

}
