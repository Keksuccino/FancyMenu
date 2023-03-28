package de.keksuccino.fancymenu.api.buttonaction;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.texteditor.TextEditorFormattingRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A button action container.<br><br>
 *
 * Needs to be registered to the {@link ButtonActionRegistry} at mod init.
 */
public abstract class ButtonActionContainer {

    private final String actionIdentifier;

    public ButtonActionContainer(@Nonnull String uniqueActionIdentifier) {
        this.actionIdentifier = uniqueActionIdentifier;
    }

    /**
     * Returns the name of this action.
     */
    public abstract String getAction();

    /**
     * If this action has a value or not.
     */
    public abstract boolean hasValue();

    /**
     * Is called when a button with this action gets clicked.<br>
     * @param value The value that was set to the action or NULL if this action has no value.
     */
    public abstract void execute(String value);

    /**
     * The description of the action.
     */
    public abstract String getActionDescription();

    /**
     * The description of the action value.
     */
    public abstract String getValueDescription();

    /**
     * An example of how the value of this action should look like.<br>
     * Return NULL if this action has no value.
     */
    @Nullable
    public abstract String getValueExample();

    //TODO übernehmen
    @Nullable
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

    public String getIdentifier() {
        return this.actionIdentifier;
    }

}
