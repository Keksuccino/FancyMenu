package de.keksuccino.fancymenu.mixin.mixins.common.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which widget owns each mouse button between press and release events.
 * Keeping this state per container-screen instance prevents interactions from leaking between screens.
 */
final class ContainerWidgetPointerTracker<T> {

    private final Map<Integer, T> ownersByButton = new HashMap<>();

    void clear(int button) {
        this.ownersByButton.remove(button);
    }

    void capture(int button, T owner) {
        this.ownersByButton.put(button, owner);
    }

    T get(int button) {
        return this.ownersByButton.get(button);
    }

    T release(int button) {
        return this.ownersByButton.remove(button);
    }

}
