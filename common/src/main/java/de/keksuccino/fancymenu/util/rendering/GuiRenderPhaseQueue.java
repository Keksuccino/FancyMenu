package de.keksuccino.fancymenu.util.rendering;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Keeps non-vertex GUI work ordered at exact draw-list boundaries without depending on Minecraft render types. */
public final class GuiRenderPhaseQueue<P, A> {

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
        List<Entry<P, A>> drained = this.entries.stream().filter(entry -> entry.phase().equals(phase)).filter(entry -> entry.drawIndex() >= startIndex && entry.drawIndex() <= endIndex).sorted(Comparator.comparingInt(Entry<P, A>::drawIndex).thenComparingInt(Entry<P, A>::order)).toList();
        this.entries.removeAll(drained);
        return drained;
    }

    public List<Entry<P, A>> drainPhase(@Nonnull P phase) {
        List<Entry<P, A>> drained = this.entries.stream().filter(entry -> entry.phase().equals(phase)).sorted(Comparator.comparingInt(Entry<P, A>::drawIndex).thenComparingInt(Entry<P, A>::order)).toList();
        this.entries.removeAll(drained);
        return drained;
    }

    public List<Entry<P, A>> drainAll() {
        List<Entry<P, A>> drained = this.entries.stream().sorted(Comparator.comparingInt(Entry<P, A>::order)).toList();
        this.entries.clear();
        return drained;
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

    public record Entry<P, A>(int drawIndex, int order, @Nonnull P phase, @Nonnull A action) {
    }

}
