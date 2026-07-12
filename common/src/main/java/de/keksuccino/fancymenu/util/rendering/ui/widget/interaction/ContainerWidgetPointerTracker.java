package de.keksuccino.fancymenu.util.rendering.ui.widget.interaction;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the widget that owns each mouse button until the matching release. Keeping this state per screen prevents one container screen from leaking an unfinished interaction into another.
 */
public final class ContainerWidgetPointerTracker<T> {

    private final Map<Integer, T> ownersByButton = new HashMap<>();

    public ContainerWidgetPointerTracker() {}

    void begin(int button) {
        this.ownersByButton.remove(button);
    }

    void claim(int button, T owner) {
        this.ownersByButton.put(button, owner);
    }

    @Nullable
    T owner(int button) {
        return this.ownersByButton.get(button);
    }

    @Nullable
    T release(int button) {
        return this.ownersByButton.remove(button);
    }

}
