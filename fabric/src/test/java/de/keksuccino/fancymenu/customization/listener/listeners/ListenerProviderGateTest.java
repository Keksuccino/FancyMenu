package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerProviderGateTest {

    @Test
    void inactiveProviderDoesNotUpdateCachedEventData() {
        OnTextClickedListener listener = new OnTextClickedListener();

        listener.onTextClicked("inactive_event");

        assertEquals("ERROR", customVariable(listener, "text_event_id"));
    }

    @Test
    void activeProviderUpdatesCachedEventData() {
        OnTextClickedListener listener = new OnTextClickedListener();
        listener.registerInstance(new ListenerInstance(listener));

        listener.onTextClicked("active_event");

        assertEquals("active_event", customVariable(listener, "text_event_id"));
    }

    @Test
    void deactivatingEventProviderClearsItsCachedData() {
        OnCharTypedListener listener = new OnCharTypedListener();
        ListenerInstance instance = new ListenerInstance(listener);
        listener.registerInstance(instance);
        listener.lastTypedChar = 'x';

        assertEquals("x", customVariable(listener, "char"));

        listener.unregisterInstance(instance);

        assertEquals("ERROR", customVariable(listener, "char"));
    }

    private static String customVariable(AbstractListener listener, String name) {
        return listener.getCustomVariables().stream().filter(variable -> variable.name().equals(name)).findFirst().orElseThrow().valueSupplier().get();
    }
}
