package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldSoundSubscriptionLifecycleTest {

    @Test
    void subscriptionTracksOnlyZeroToOneAndOneToZeroTransitions() {
        List<Boolean> subscriptionStates = new ArrayList<>();
        OnWorldSoundTriggeredListener listener = new OnWorldSoundTriggeredListener(subscriptionStates::add);
        ListenerInstance first = listener.createFreshInstance();
        ListenerInstance second = listener.createFreshInstance();

        listener.registerInstance(first);
        listener.registerInstance(second);
        listener.unregisterInstance(first);
        listener.unregisterInstance(second);

        assertEquals(List.of(true, false), subscriptionStates);
    }
}
