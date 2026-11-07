package de.keksuccino.fancymenu.util.rendering;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Keeps non-vertex GUI work ordered at exact draw-list boundaries without depending on Minecraft render types. */
public final class GuiRenderPhaseQueue<P, A> {

    private static final Comparator<Entry<?, ?>> DRAW_ORDER = Comparator.<Entry<?, ?>>comparingInt(Entry::drawIndex).thenComparingInt(Entry::order);
    private static final Comparator<Entry<?, ?>> INSERTION_ORDER = Comparator.comparingInt(Entry::order);

    private final List<Entry<P, A>> entries = new ArrayList<>();
    private int nextOrder;

    public void add(int drawIndex, @Nonnull P phase, @Nonnull A action) {
        if (drawIndex < 0) {
            throw new IllegalArgumentException("Draw index must not be negative");
        }
        this.entries.add(new Entry<>(drawIndex, this.nextOrder++, Objects.requireNonNull(phase), Objects.requireNonNull(action)));
    }

    public List<Entry<P, A>> drainRange(@Nonnull P phase, int startIndex, int endIndex) {
        if (startIndex > endIndex) {
            return List.of();
        }
        return this.drain(entry -> entry.phase().equals(phase) && entry.drawIndex() >= startIndex && entry.drawIndex() <= endIndex, DRAW_ORDER);
    }

    public List<Entry<P, A>> drainPhase(@Nonnull P phase) {
        return this.drain(entry -> entry.phase().equals(phase), DRAW_ORDER);
    }

    public List<Entry<P, A>> drainAll() {
        return this.drain(entry -> true, INSERTION_ORDER);
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    public void clear() {
        this.entries.clear();
        this.nextOrder = 0;
    }

    /**
     * Removes all matching entries in one pass and returns them sorted. Entries usually get added in their final order already, which keeps the sort linear.
     */
    private List<Entry<P, A>> drain(@Nonnull Predicate<Entry<P, A>> filter, @Nonnull Comparator<? super Entry<P, A>> order) {
        List<Entry<P, A>> drained = null;
        int kept = 0;
        for (int i = 0; i < this.entries.size(); i++) {
            Entry<P, A> entry = this.entries.get(i);
            if (filter.test(entry)) {
                if (drained == null) {
                    drained = new ArrayList<>();
                }
                drained.add(entry);
            } else {
                this.entries.set(kept++, entry);
            }
        }
        if (drained == null) {
            return List.of();
        }
        this.entries.subList(kept, this.entries.size()).clear();
        drained.sort(order);
        return drained;
    }

    public record Entry<P, A>(int drawIndex, int order, @Nonnull P phase, @Nonnull A action) {
    }

}
