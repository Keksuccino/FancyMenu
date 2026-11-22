package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.variables.Variable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Removes all entries of a list variable. Its value is only the name of the list.
 */
public class ClearListAction extends AbstractListAction {

    private static final String LOCALIZATION_KEY = LOCALIZATION_KEY_PREFIX + "clear_list";

    public ClearListAction() {
        this(VARIABLE_HANDLER);
    }

    ClearListAction(@NotNull ListVariableAccess variables) {
        super("clear_list", variables);
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null) return;
        String listName = Variable.normalizeReferenceName(value);
        if (listName.isEmpty()) return;
        this.logFailedEdit(listName, this.variables.clearList(listName));
    }

    @Override
    public @NotNull String getValuePreset() {
        return "my_list";
    }

    @Override
    protected @NotNull ListActionValueWindowBody createValueEditor(@NotNull String value, @NotNull Consumer<String> callback) {
        return new ClearListActionValueScreen(value, callback);
    }

    public static class ClearListActionValueScreen extends ListActionValueWindowBody {

        @NotNull
        protected String listName;

        protected ClearListActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable(LOCALIZATION_KEY + ".edit_value"), callback);
            this.listName = value;
        }

        @Override
        protected void initCells() {

            this.addStartEndSpacerCell();

            this.addListNameCells(this.listName, s -> this.listName = s);

            this.addStartEndSpacerCell();

        }

        @Override
        public boolean allowDone() {
            return !this.listName.isBlank();
        }

        @Override
        protected @NotNull String serialize() {
            return this.listName;
        }

    }

}
