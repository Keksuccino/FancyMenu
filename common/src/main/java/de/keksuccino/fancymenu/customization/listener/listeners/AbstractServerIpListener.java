package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base for multiplayer server listeners that provide the IP of the server they fired for as {@code $$server_ip} variable.
 */
public abstract class AbstractServerIpListener extends AbstractListener {

    private static final String UNKNOWN_SERVER_IP = "ERROR";

    @Nullable
    private String lastServerIp;
    private boolean notifying;

    public AbstractServerIpListener(@NotNull String identifier) {
        super(identifier);
    }

    /**
     * Has to get called on the main thread.<br>
     * Actions of these listeners can trigger the same event again while it's still notifying, like a Join Server action in "On Start Joining Server"
     * or a disconnect action in "On Server Left". These nested events get skipped, because they would otherwise recurse until the game crashes,
     * and because they would replace the server IP that the remaining actions of the outer event still use.
     */
    protected void notifyWithServerIp(@Nullable String serverIp) {
        if (this.notifying || !this.hasInstancesListening()) {
            return;
        }
        this.lastServerIp = serverIp;
        this.notifying = true;
        try {
            this.notifyAllInstances();
        } finally {
            this.notifying = false;
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("server_ip", () -> formatServerIp(this.lastServerIp)));
    }

    @NotNull
    private static String formatServerIp(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_SERVER_IP;
        }
        return value;
    }

}
