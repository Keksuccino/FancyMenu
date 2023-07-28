package de.keksuccino.fancymenu.customization.action;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public class ExecutableAction {

    private static final Logger LOGGER = LogManager.getLogger();

    @NotNull
    public volatile Action action;
    @Nullable
    public volatile String value;

    public ExecutableAction(@NotNull Action action, @Nullable String value) {
        this.action = Objects.requireNonNull(action);
        this.value = value;
    }

    public void execute() {
        String v = this.value;
        try {
            if (v != null) {
                v = PlaceholderParser.replacePlaceholders(v);
            }
            if (this.action.hasValue()) {
                this.action.execute(v);
            } else {
                this.action.execute(null);
            }
        } catch (Exception ex) {
            LOGGER.error("################################");
            LOGGER.error("[FANCYMENU] An error occurred while trying to execute an action!");
            LOGGER.error("[FANCYMENU] Action: " + this.action.getIdentifier());
            LOGGER.error("[FANCYMENU] Value Raw: " + this.value);
            LOGGER.error("[FANCYMENU] Value: " + v);
            LOGGER.error("################################");
            ex.printStackTrace();
        }
    }

    @NotNull
    public ExecutableAction copy() {
        return new ExecutableAction(this.action, this.value);
    }

}
