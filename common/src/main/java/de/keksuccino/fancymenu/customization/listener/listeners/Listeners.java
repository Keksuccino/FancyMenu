package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.ListenerRegistry;

public class Listeners {

    public static final OnCharTypedListener ON_CHAR_TYPED = new OnCharTypedListener();
    public static final OnVariableUpdatedListener ON_VARIABLE_UPDATED = new OnVariableUpdatedListener();

    public static void registerAll() {

        ListenerRegistry.registerListener(ON_CHAR_TYPED);
        ListenerRegistry.registerListener(ON_VARIABLE_UPDATED);

    }

}
