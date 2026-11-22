package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.ListArrangement;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.util.cycle.LocalizedGenericValueCycle;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Sorts, reverses or shuffles the entries of a list variable, or removes its duplicates, see {@link ListArrangement}.
 */
public class ArrangeListAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "arrange_list";

    public ArrangeListAction() {
        this(VARIABLE_HANDLER);
    }

    ArrangeListAction(@NotNull ListVariableAccess variables) {
        super("arrange_list", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        Config config = Config.parse(value);
        if (config == null) return;
        String listName = Variable.normalizeReferenceName(config.listName);
        if (listName.isEmpty()) return;
        this.logFailedEdit(listName, this.variables.arrangeList(listName, config.arrangement));
    }

    @Override
    public @NotNull String getValuePreset() {
        Config config = new Config();
        config.listName = "my_list";
        return config.serialize();
    }

    @Override
    public @Nullable Component getValueSummary(@NotNull String value) {
        Config config = Objects.requireNonNull(Config.parse(value));
        return Component.translatable(LOCALIZATION_KEY + ".summary", config.listName, Component.translatable(getArrangementLocalizationKey(config.arrangement)));
    }

    @NotNull
    private static String getArrangementLocalizationKey(@NotNull ListArrangement arrangement) {
        return LOCALIZATION_KEY + ".edit.arrangement." + arrangement.getSerializedName();
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new ArrangeListActionValueScreen(value, callback);
    }

    /**
     * What the action does, which is what its value holds.
     */
    public static final class Config {

        @NotNull
        public String listName = "";
        @NotNull
        public ListArrangement arrangement = ListArrangement.SORT_ASCENDING;

        @NotNull
        public String serialize() {
            return this.listName + VALUE_SEPARATOR + this.arrangement.getSerializedName();
        }

        /**
         * Reads a value. Parts a value does not have, or can't be read, keep what a new action starts with.
         */
        @Nullable
        public static Config parse(@Nullable String value) {
            if (value == null) return null;
            String[] parts = splitValue(value, 2);
            Config config = new Config();
            config.listName = parts[0];
            if (parts.length >= 2) config.arrangement = Objects.requireNonNullElse(ListArrangement.getBySerializedName(parts[1].strip()), config.arrangement);
            return config;
        }

    }

    /**
     * The window the value of the action gets edited in. A hint below the arrangement explains what it does.
     */
    public static class ArrangeListActionValueScreen extends ListActionValueWindowBody {

        @NotNull
        protected final Config config;

        protected ArrangeListActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.config = Objects.requireNonNullElseGet(Config.parse(value), Config::new);
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addListNameCells(this.config.listName, s -> this.config.listName = s);

            this.addCellGroupEndSpacerCell();

            LocalizedGenericValueCycle<ListArrangement> arrangementCycle = LocalizedGenericValueCycle.of(LOCALIZATION_KEY + ".edit.arrangement", ListArrangement.values());
            arrangementCycle.setValueNameSupplier(arrangement -> Component.translatable(getArrangementLocalizationKey(arrangement)).getString());
            arrangementCycle.setValueComponentStyleSupplier(arrangement -> Style.EMPTY.withColor(UIBase.getUITheme().warning_color.getColorInt()));
            arrangementCycle.setCurrentValue(this.config.arrangement);
            HintCell hint = new HintCell().setHint(Component.translatable(getArrangementLocalizationKey(this.config.arrangement) + ".desc"), false);
            this.addCycleButtonCell(arrangementCycle, true, (arrangement, button) -> {
                this.config.arrangement = arrangement;
                hint.setHint(Component.translatable(getArrangementLocalizationKey(arrangement) + ".desc"), false);
            });
            this.addCell(hint);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.config.listName.isBlank();
        }

        @Override
        protected @NotNull String serialize() {
            return this.config.serialize();
        }

    }

}
