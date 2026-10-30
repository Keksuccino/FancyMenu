package de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LiveEditSnapshotTest {

    @Test
    void confirmedEditSavesTheStateFromBeforeTheLivePreview() {
        HistoryBuilder builder = new HistoryBuilder();
        builder.value = 1;

        ContextMenuBuilder.LiveEditSnapshot undoSnapshot = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        // Live preview changes before the user confirms the edit
        builder.value = 5;
        builder.value = 7;
        undoSnapshot.save();

        assertEquals(List.of(1), builder.savedHistory);
        assertEquals(List.of("create:1", "save:1", "discard:1"), builder.calls);
    }

    @Test
    void cancelledEditOnlyReleasesItsSnapshot() {
        HistoryBuilder builder = new HistoryBuilder();
        builder.value = 3;

        ContextMenuBuilder.LiveEditSnapshot undoSnapshot = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        builder.value = 9;
        undoSnapshot.discard();

        assertEquals(List.of(), builder.savedHistory);
        assertEquals(List.of("create:3", "discard:3"), builder.calls);
    }

    @Test
    void finishingTwiceIsIgnored() {
        HistoryBuilder builder = new HistoryBuilder();

        ContextMenuBuilder.LiveEditSnapshot saved = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        saved.save();
        saved.discard();
        saved.save();
        ContextMenuBuilder.LiveEditSnapshot discarded = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        discarded.discard();
        discarded.save();

        assertEquals(List.of("create:0", "save:0", "discard:0", "create:0", "discard:0"), builder.calls);
    }

    @Test
    void buildersWithoutAdvanceSnapshotsSaveTheCurrentStateWhenConfirmed() {
        HistoryBuilder builder = new HistoryBuilder();
        builder.supportsSnapshots = false;

        ContextMenuBuilder.LiveEditSnapshot confirmed = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        confirmed.save();
        ContextMenuBuilder.LiveEditSnapshot cancelled = ContextMenuBuilder.LiveEditSnapshot.create(builder);
        cancelled.discard();

        assertEquals(List.of("create:none", "saveCurrent", "create:none"), builder.calls);
    }

    /** Records history calls, with the snapshot being the value that was current when it got created. */
    private static final class HistoryBuilder implements ContextMenuBuilder<HistoryBuilder> {

        private final List<String> calls = new ArrayList<>();
        private final List<Integer> savedHistory = new ArrayList<>();
        private final List<ContextMenuScreenOpenProcessor> processors = new ArrayList<>();
        private int value = 0;
        private boolean supportsSnapshots = true;

        @Override
        public @Nullable Screen getContextMenuCallbackScreen() {
            return null;
        }

        @Override
        public @NotNull HistoryBuilder self() {
            return this;
        }

        @Override
        public void saveSnapshot() {
            this.calls.add("saveCurrent");
            this.savedHistory.add(this.value);
        }

        @Override
        public void saveSnapshot(@NotNull Object snapshot) {
            this.calls.add("save:" + snapshot);
            this.savedHistory.add((Integer) snapshot);
        }

        @Override
        public @Nullable Object createSnapshot() {
            this.calls.add("create:" + (this.supportsSnapshots ? this.value : "none"));
            return this.supportsSnapshots ? this.value : null;
        }

        @Override
        public void discardSnapshot(@NotNull Object snapshot) {
            this.calls.add("discard:" + snapshot);
        }

        @Override
        public @NotNull List<HistoryBuilder> getFilteredStackableObjectsList(@Nullable ConsumingSupplier<HistoryBuilder, Boolean> filter) {
            return List.of(this);
        }

        @Override
        public @NotNull List<ContextMenuScreenOpenProcessor> getContextMenuScreenOpenProcessorList() {
            return this.processors;
        }

    }

}
