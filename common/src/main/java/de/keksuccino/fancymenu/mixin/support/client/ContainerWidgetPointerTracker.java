package de.keksuccino.fancymenu.mixin.support.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which widget owns each mouse button between press and release events.
 * Keeping this state per container-screen instance prevents interactions from leaking between screens.
 */
public class ContainerWidgetPointerTracker<T> {

    public final Map<Integer, T> ownersByButton = new HashMap<>();

    public void clear(int button) {
        this.ownersByButton.remove(button);
    }

    public void capture(int button, T owner) {
        this.ownersByButton.put(button, owner);
    }

    public T get(int button) {
        return this.ownersByButton.get(button);
    }

    public T release(int button) {
        return this.ownersByButton.remove(button);
    }

}
