package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.rendering.ui.texteditor.TextEditorFormattingRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A button action container.<br><br>
 *
 * Needs to be registered to the {@link ActionRegistry} at mod init.
 */
public abstract class Action {

    private final String identifier;

    public Action(@NotNull String uniqueIdentifier) {
        this.identifier = uniqueIdentifier;
    }

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

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @Nullable
    public List<TextEditorFormattingRule> getValueFormattingRules() {
        return null;
    }

}
