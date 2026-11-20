package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * An ordered list of live simulation objects (creatures, bubbles, lanterns) that keeps removed objects for reuse, so things can come
 * and go every frame without allocations. Objects keep their old field values when they come back, so whoever adds one sets every
 * field it reads, like the mockup's {@code mk} functions do. Use indexed loops, which need no iterator.
 *
 * @param <T> the type of the objects
 */
public final class CreaturePool<T> {

    @NotNull
    private final Supplier<T> factory;
    private final ArrayList<T> live = new ArrayList<>();
    private final ArrayList<T> spare = new ArrayList<>();

    public CreaturePool(@NotNull Supplier<T> factory) {
        this.factory = factory;
    }

    public int size() {
        return this.live.size();
    }

    @NotNull
    public T get(int index) {
        return this.live.get(index);
    }

    /**
     * Appends an object to the end of the list and returns it: a reused one if there is one, otherwise a new one.
     */
    @NotNull
    public T add() {
        T object = this.spare.isEmpty() ? this.factory.get() : this.spare.remove(this.spare.size() - 1);
        this.live.add(object);
        return object;
    }

    /**
     * Removes the object at the index. The others keep their order.
     */
    public void remove(int index) {
        this.spare.add(this.live.remove(index));
    }

    /**
     * Removes the objects from the given index to the end, the shrinking half of the mockup's {@code fitCount}.
     */
    public void truncate(int size) {
        for (int i = this.live.size() - 1; i >= Math.max(0, size); i--) this.remove(i);
    }

    public void clear() {
        this.truncate(0);
    }

}
