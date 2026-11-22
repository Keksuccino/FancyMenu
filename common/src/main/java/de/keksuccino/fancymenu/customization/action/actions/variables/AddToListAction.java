package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adds an entry to a list variable, at its end or at an index.
 */
public class AddToListAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "add_to_list";

    public AddToListAction() {
        this(VARIABLE_HANDLER);
    }

    AddToListAction(@NotNull ListVariableAccess variables) {
        super("add_to_list", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        Config config = Config.parse(value);
        if (config == null) return;
        String listName = Variable.normalizeReferenceName(config.listName);
        if (listName.isEmpty()) return;
        Integer index = null;
        // An empty index adds the entry at the end
        if (!config.index.isBlank()) {
            index = this.parseIndex(listName, config.index);
            if (index == null) return;
        }
        this.logFailedEdit(listName, this.variables.addListEntry(listName, config.entry, index, config.skipIfInList, config.ignoreCase));
    }

    @Override
    public @NotNull String getValuePreset() {
        Config config = new Config();
        config.listName = "my_list";
        config.entry = "some_entry";
        return config.serialize();
    }

    @Override
    public @Nullable Component getValueSummary(@NotNull String value) {
        Config config = Objects.requireNonNull(Config.parse(value));
        if (config.index.isBlank()) return Component.translatable(LOCALIZATION_KEY + ".summary", quoteForSummary(config.entry), config.listName);
        return Component.translatable(LOCALIZATION_KEY + ".summary.at_index", quoteForSummary(config.entry), config.listName, config.index);
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new AddToListActionValueScreen(value, callback);
    }

    /**
     * What the action does, which is what its value holds.
     */
    public static final class Config {

        @NotNull
        public String listName = "";
        /** The index the new entry gets. It stays text until the action runs, so it can be a placeholder. Empty adds the entry at the end. */
        @NotNull
        public String index = "";
        public boolean skipIfInList = false;
        /** If upper and lower case don't matter when looking for the entry in the list, see {@link #skipIfInList}. */
        public boolean ignoreCase = false;
        @NotNull
        public String entry = "";

        /**
         * The entry comes last: it is the only part that can hold anything, the separator included.
         */
        @NotNull
        public String serialize() {
            return this.listName + VALUE_SEPARATOR + this.index + VALUE_SEPARATOR + this.skipIfInList + VALUE_SEPARATOR + this.ignoreCase + VALUE_SEPARATOR + this.entry;
        }

        /**
         * Reads a value. Parts a value does not have, or can't be read, keep what a new action starts with.
         */
        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = splitValue(value, 5);
            Config config = new Config();
            config.listName = parts[0];
            if (parts.length >= 2) config.index = parts[1];
            if (parts.length >= 3) config.skipIfInList = parseBoolean(parts[2], config.skipIfInList);
            if (parts.length >= 4) config.ignoreCase = parseBoolean(parts[3], config.ignoreCase);
            if (parts.length >= 5) config.entry = parts[4];
            return config;
        }

    }

    public static class AddToListActionValueScreen extends ListActionValueWindowBody {

        @NotNull
        protected final Config config;

        protected AddToListActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.config = Objects.requireNonNullElseGet(Config.parse(value), Config::new);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addListNameCells(this.config.listName, s -> this.config.listName = s);

            this.addCellGroupEndSpacerCell();

            this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.entry"));
            this.addTextInputCell(null, true, true)
                    .setEditListener(s -> this.config.entry = s)
                    .setText(this.config.entry)
                    .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.entry.desc")));

            this.addCellGroupEndSpacerCell();

            TextInputCell indexCell = this.addIndexCells(Component.translatable(LOCALIZATION_KEY + ".edit.index"), Component.translatable(LOCALIZATION_KEY + ".edit.index.desc"), this.config.index, true, s -> this.config.index = s);
            Component emptyIndexHint = Component.translatable(LOCALIZATION_KEY + ".edit.index.empty");
            indexCell.editBox.setHintFancyMenu(consumes -> emptyIndexHint);

            this.addCellGroupEndSpacerCell();

            CycleButton<CommonCycles.CycleEnabledDisabled> skipButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled(LOCALIZATION_KEY + ".edit.skip_if_in_list", this.config.skipIfInList), (value, button) -> {
                this.config.skipIfInList = value.getAsBoolean();
                // Ignoring case only matters while looking for the entry
                this.rebuild();
            });
            skipButton.setUITooltip(UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.skip_if_in_list.desc")));
            this.addWidgetCell(skipButton, true);

            if (this.config.skipIfInList) {
                this.addCellGroupEndSpacerCell();
                CycleButton<CommonCycles.CycleEnabledDisabled> ignoreCaseButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled(LIST_LOCALIZATION_KEY + ".ignore_case", this.config.ignoreCase), (value, button) -> this.config.ignoreCase = value.getAsBoolean());
                ignoreCaseButton.setUITooltip(UITooltip.of(Component.translatable(LIST_LOCALIZATION_KEY + ".ignore_case.desc")));
                this.addWidgetCell(ignoreCaseButton, true);
            }

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
