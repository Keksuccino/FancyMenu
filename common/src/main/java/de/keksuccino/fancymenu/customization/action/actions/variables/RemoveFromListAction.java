package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Removes entries from a list variable: the one at an index, the first one with a value, or all of them.
 */
public class RemoveFromListAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "remove_from_list";

    public RemoveFromListAction() {
        this(VARIABLE_HANDLER);
    }

    RemoveFromListAction(@NotNull ListVariableAccess variables) {
        super("remove_from_list", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        Config config = Config.parse(value);
        if (config == null) return;
        String listName = Variable.normalizeReferenceName(config.listName);
        if (listName.isEmpty()) return;
        if (config.mode == Mode.AT_INDEX) {
            Integer index = this.parseIndex(listName, config.index);
            if (index == null) return;
            this.logFailedEdit(listName, this.variables.removeListEntryAt(listName, index), index);
        } else {
            this.logFailedEdit(listName, this.variables.removeListEntries(listName, config.entry, config.mode == Mode.ALL_MATCHES, config.ignoreCase));
        }
    }

    @Override
    public @NotNull String getValuePreset() {
        Config config = new Config();
        config.listName = "my_list";
        config.index = "0";
        return config.serialize();
    }

    @Override
    public @Nullable Component getValueSummary(@NotNull String value) {
        Config config = Objects.requireNonNull(Config.parse(value));
        if (config.mode == Mode.AT_INDEX) return Component.translatable(LOCALIZATION_KEY + ".summary.at_index", config.listName, config.index);
        return Component.translatable(LOCALIZATION_KEY + ".summary." + config.mode.serializedName, quoteForSummary(config.entry), config.listName);
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new RemoveFromListActionValueScreen(value, callback);
    }

    /**
     * Which entries the action removes.
     */
    public enum Mode {

        /** The entry at an index. */
        AT_INDEX,
        /** The first entry with a value. */
        FIRST_MATCH,
        /** Every entry with a value. */
        ALL_MATCHES;

        /** The name the mode gets saved with. Changing it breaks saved layouts. */
        private final String serializedName = this.name().toLowerCase(Locale.ROOT);

        @NotNull
        private static Mode parse(@NotNull String serializedName) {
            String trimmed = serializedName.strip();
            for (Mode mode : values()) {
                if (mode.serializedName.equals(trimmed)) return mode;
            }
            return AT_INDEX;
        }

    }

    /**
     * What the action does, which is what its value holds. It keeps the index and the value of both kinds of modes, so switching the mode
     * in the window loses nothing.
     */
    public static final class Config {

        @NotNull
        public String listName = "";
        @NotNull
        public Mode mode = Mode.AT_INDEX;
        /** The index of the entry to remove. It stays text until the action runs, so it can be a placeholder. */
        @NotNull
        public String index = "";
        /** If upper and lower case don't matter when looking for entries with the value. */
        public boolean ignoreCase = false;
        /** The value of the entries to remove. */
        @NotNull
        public String entry = "";

        /**
         * The value comes last: it is the only part that can hold anything, the separator included.
         */
        @NotNull
        public String serialize() {
            return this.listName + VALUE_SEPARATOR + this.mode.serializedName + VALUE_SEPARATOR + this.index + VALUE_SEPARATOR + this.ignoreCase + VALUE_SEPARATOR + this.entry;
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
            if (parts.length >= 2) config.mode = Mode.parse(parts[1]);
            if (parts.length >= 3) config.index = parts[2];
            if (parts.length >= 4) config.ignoreCase = parseBoolean(parts[3], config.ignoreCase);
            if (parts.length >= 5) config.entry = parts[4];
            return config;
        }

    }

    /**
     * The window the value of the action gets edited in. It only shows the inputs the chosen mode needs.
     */
    public static class RemoveFromListActionValueScreen extends ListActionValueWindowBody {

        @NotNull
        protected final Config config;

        protected RemoveFromListActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.config = Objects.requireNonNullElseGet(Config.parse(value), Config::new);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addListNameCells(this.config.listName, s -> this.config.listName = s);

            this.addCellGroupEndSpacerCell();

            LocalizedGenericValueCycle<Mode> modeCycle = LocalizedGenericValueCycle.of(LOCALIZATION_KEY + ".edit.mode", Mode.values());
            modeCycle.setValueNameSupplier(mode -> Component.translatable(LOCALIZATION_KEY + ".edit.mode." + mode.serializedName).getString());
            modeCycle.setValueComponentStyleSupplier(mode -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            modeCycle.setCurrentValue(this.config.mode);
            CycleButton<Mode> modeButton = new CycleButton<>(0, 0, 20, 20, modeCycle, (mode, button) -> {
                this.config.mode = mode;
                // Every mode has its own inputs
                this.rebuild();
            });
            modeButton.setUITooltip(UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.mode.desc")));
            this.addWidgetCell(modeButton, true);

            this.addCellGroupEndSpacerCell();

            if (this.config.mode == Mode.AT_INDEX) {
                this.addIndexCells(Component.translatable(LIST_LOCALIZATION_KEY + ".index"), Component.translatable(LOCALIZATION_KEY + ".edit.index.desc"), this.config.index, false, s -> this.config.index = s);
            } else {
                this.addLabelCell(Component.translatable(LOCALIZATION_KEY + ".edit.entry"));
                this.addTextInputCell(null, true, true)
                        .setEditListener(s -> this.config.entry = s)
                        .setText(this.config.entry)
                        .setTooltip(() -> UITooltip.of(Component.translatable(LOCALIZATION_KEY + ".edit.entry.desc")));
                this.addCellGroupEndSpacerCell();
                CycleButton<CommonCycles.CycleEnabledDisabled> ignoreCaseButton = new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled(LIST_LOCALIZATION_KEY + ".ignore_case", this.config.ignoreCase), (value, button) -> this.config.ignoreCase = value.getAsBoolean());
                ignoreCaseButton.setUITooltip(UITooltip.of(Component.translatable(LIST_LOCALIZATION_KEY + ".ignore_case.desc")));
                this.addWidgetCell(ignoreCaseButton, true);
            }

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            if (this.config.listName.isBlank()) return false;
            return (this.config.mode != Mode.AT_INDEX) || this.isIndexInputValid();
        }

        @Override
        protected @NotNull String serialize() {
            return this.config.serialize();
        }

    }

}
