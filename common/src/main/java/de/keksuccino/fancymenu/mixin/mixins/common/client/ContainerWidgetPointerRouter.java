package de.keksuccino.fancymenu.mixin.mixins.common.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Owns pointer capture for one container screen. Captures are keyed by mouse button so overlapping interactions cannot leak across buttons or screen instances.
 */
final class ContainerWidgetPointerRouter<T> {

    private final Map<Integer, T> targetsByButton = new HashMap<>();

    boolean mouseClicked(int button, Iterable<? extends T> candidates, Predicate<? super T> canClick, Predicate<? super T> clickHandler, Consumer<? super T> focusHandler, Runnable beginPrimaryDrag, Runnable endPrimaryDrag) {
        T previousTarget = this.targetsByButton.remove(button);
        if (previousTarget != null && button == 0) endPrimaryDrag.run();
        for (T candidate : candidates) {
            if (canClick.test(candidate) && clickHandler.test(candidate)) {
                this.targetsByButton.put(button, candidate);
                focusHandler.accept(candidate);
                if (button == 0) beginPrimaryDrag.run();
                return true;
            }
        }
        return false;
    }

    boolean mouseDragged(int button, Predicate<? super T> dragHandler) {
        T target = this.targetsByButton.get(button);
        if (target == null) return false;
        dragHandler.test(target);
        return true;
    }

    boolean mouseReleased(int button, Predicate<? super T> releaseHandler, Runnable endPrimaryDrag) {
        T target = this.targetsByButton.remove(button);
        if (target == null) return false;
        if (button == 0) endPrimaryDrag.run();
        releaseHandler.test(target);
        return true;
    }

}
