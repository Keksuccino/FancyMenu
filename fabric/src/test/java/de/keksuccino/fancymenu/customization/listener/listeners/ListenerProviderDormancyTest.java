package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.action.Executable;
import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.ListenerInstance;
import de.keksuccino.fancymenu.events.screen.ScreenMouseMoveEvent;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListenerProviderDormancyTest {

    @Test
    void dormantProviderDoesNotMutateItsEventCache() {
        OnPositionChangedListener listener = new OnPositionChangedListener();

        listener.onPositionChanged(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));

        assertEquals("ERROR", variableValue(listener, "old_pos_x"));
        assertEquals("ERROR", variableValue(listener, "new_pos_x"));

        ListenerInstance instance = listener.createFreshInstance();
        listener.registerInstance(instance);
        listener.onPositionChanged(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6));

        assertEquals("1", variableValue(listener, "old_pos_x"));
        assertEquals("6", variableValue(listener, "new_pos_z"));
    }

    @Test
    void eventBusProviderClearsItsCacheWhenItBecomesDormant() {
        OnMouseMovedListener listener = new OnMouseMovedListener();
        AtomicInteger notifications = new AtomicInteger();
        ListenerInstance instance = createInstance(listener, notifications::incrementAndGet);
        ScreenMouseMoveEvent event = new ScreenMouseMoveEvent(null, 10.0D, 20.0D, 2.0D, 3.0D);

        listener.onMouseMoved(event);

        assertEquals("ERROR", variableValue(listener, "mouse_pos_x"));

        listener.registerInstance(instance);
        listener.onMouseMoved(event);
        listener.unregisterInstance(instance);

        assertEquals(1, notifications.get());
        assertEquals("ERROR", variableValue(listener, "mouse_pos_x"));
        assertEquals("ERROR", variableValue(listener, "mouse_move_delta_y"));
    }

    @Test
    void visibilityTrackingRunsWhileEitherPairedProviderIsActive() {
        OnEntityStopsBeingInSightListener stopListener = new OnEntityStopsBeingInSightListener();
        OnEntityStartsBeingInSightListener startListener = new OnEntityStartsBeingInSightListener(stopListener);
        ListenerInstance startInstance = startListener.createFreshInstance();
        ListenerInstance stopInstance = stopListener.createFreshInstance();

        assertFalse(startListener.onRenderFrameStart());

        startListener.registerInstance(startInstance);
        assertTrue(startListener.onRenderFrameStart());
        startListener.onRenderFrameEnd();
        startListener.unregisterInstance(startInstance);

        stopListener.registerInstance(stopInstance);
        assertTrue(startListener.onRenderFrameStart());
        startListener.onRenderFrameEnd();
        stopListener.unregisterInstance(stopInstance);

        assertFalse(startListener.onRenderFrameStart());
    }

    private static ListenerInstance createInstance(AbstractListener listener, Runnable action) {
        ListenerInstance instance = listener.createFreshInstance();
        instance.getActionScript().addExecutable(new TestExecutable(action));
        return instance;
    }

    private static String variableValue(AbstractListener listener, String name) {
        return listener.getCustomVariables().stream().filter(variable -> variable.name().equals(name)).findFirst().orElseThrow().valueSupplier().get();
    }

    private record TestExecutable(Runnable action) implements Executable {

        @Override
        public void execute() {
            this.action.run();
        }

        @Override
        public @NotNull String getIdentifier() {
            return "listener-provider-dormancy-test";
        }

        @Override
        public @NotNull Executable copy(boolean unique) {
            return this;
        }

        @Override
        public @NotNull PropertyContainer serialize() {
            return new PropertyContainer("test");
        }
    }
}
