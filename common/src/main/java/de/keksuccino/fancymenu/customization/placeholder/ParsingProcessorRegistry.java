//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.util.ConsumingSupplier;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The parsing processors of {@link PlaceholderParser}, see {@link PlaceholderParser#addParsingProcessor(PlaceholderParser.ParsingProcessorTiming, ConsumingSupplier)}.
 *
 * <p>Registration mutates the ordered maps only while holding their lock, then publishes both processor phases as
 * one immutable volatile snapshot. A parser can therefore retain one coherent revision without holding a lock
 * while integration code executes.</p>
 */
final class ParsingProcessorRegistry {

    /** Processor IDs start at 1, so this never matches a processor. */
    static final long NO_PROCESSOR_ID = 0L;

    private static final Object REGISTRATION_LOCK = new Object();
    private static final Map<Long, ConsumingSupplier<String, String>> BEFORE_REPLACING_PLACEHOLDERS = new LinkedHashMap<>();
    private static final Map<Long, ConsumingSupplier<String, String>> AFTER_REPLACING_PLACEHOLDERS = new LinkedHashMap<>();
    private static long processorId = 0L;
    private static long revision = 0L;
    private static volatile Snapshot snapshot = new Snapshot(0L, List.of(), List.of());

    private ParsingProcessorRegistry() {
    }

    static long add(@NotNull PlaceholderParser.ParsingProcessorTiming timing, @NotNull ConsumingSupplier<String, String> processor) {
        Objects.requireNonNull(timing);
        Objects.requireNonNull(processor);
        synchronized (REGISTRATION_LOCK) {
            long id = Math.incrementExact(processorId);
            processorId = id;
            if (timing == PlaceholderParser.ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS) {
                BEFORE_REPLACING_PLACEHOLDERS.put(id, processor);
            } else {
                AFTER_REPLACING_PLACEHOLDERS.put(id, processor);
            }
            publishSnapshotLocked();
            return id;
        }
    }

    static void remove(long id) {
        synchronized (REGISTRATION_LOCK) {
            boolean removedBefore = BEFORE_REPLACING_PLACEHOLDERS.remove(id) != null;
            boolean removedAfter = AFTER_REPLACING_PLACEHOLDERS.remove(id) != null;
            if (removedBefore || removedAfter) publishSnapshotLocked();
        }
    }

    @NotNull
    static Snapshot snapshot() {
        return snapshot;
    }

    private static void publishSnapshotLocked() {
        List<RegisteredProcessor> beforeReplacement = createProcessorList(BEFORE_REPLACING_PLACEHOLDERS);
        List<RegisteredProcessor> afterReplacement = createProcessorList(AFTER_REPLACING_PLACEHOLDERS);
        long newRevision = Math.incrementExact(revision);
        revision = newRevision;
        snapshot = new Snapshot(newRevision, beforeReplacement, afterReplacement);
    }

    @NotNull
    private static List<RegisteredProcessor> createProcessorList(@NotNull Map<Long, ConsumingSupplier<String, String>> processors) {
        List<RegisteredProcessor> list = new ArrayList<>(processors.size());
        for (Map.Entry<Long, ConsumingSupplier<String, String>> processor : processors.entrySet()) {
            list.add(new RegisteredProcessor(processor.getKey(), processor.getValue()));
        }
        return List.copyOf(list);
    }

    private record RegisteredProcessor(long id, @NotNull ConsumingSupplier<String, String> processor) {
    }

    /**
     * Never changed after it got published. The revision grows with every change, so caches can tell which processors produced their content.
     */
    record Snapshot(long revision, @NotNull List<RegisteredProcessor> beforeReplacement, @NotNull List<RegisteredProcessor> afterReplacement) {

        Snapshot {
            beforeReplacement = List.copyOf(beforeReplacement);
            afterReplacement = List.copyOf(afterReplacement);
        }

        @NotNull
        String processBeforeReplacement(@NotNull String in) {
            for (RegisteredProcessor processor : this.beforeReplacement) {
                in = processor.processor().get(in);
            }
            return in;
        }

        /**
         * @param skippedProcessorId The ID of a processor to skip, or {@link #NO_PROCESSOR_ID}.
         */
        @NotNull
        String processAfterReplacement(@NotNull String in, long skippedProcessorId) {
            for (RegisteredProcessor processor : this.afterReplacement) {
                if (processor.id() == skippedProcessorId) continue;
                in = processor.processor().get(in);
            }
            return in;
        }

    }

}
