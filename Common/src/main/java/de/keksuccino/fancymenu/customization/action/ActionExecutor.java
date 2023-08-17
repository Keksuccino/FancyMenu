package de.keksuccino.fancymenu.customization.action;

import java.util.ArrayList;
import java.util.List;

public class ActionExecutor {

    public final List<ExecutableAction> actions = new ArrayList<>();

    public void execute() {
        for (ExecutableAction a : this.actions) {
            a.execute();
        }
    }

}
