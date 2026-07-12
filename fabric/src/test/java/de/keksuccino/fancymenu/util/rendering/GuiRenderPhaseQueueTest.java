package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiRenderPhaseQueueTest {

    @Test
    void ordersBoundariesByDrawIndexAndKeepsInsertionOrderAtSameBoundary() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(4, "before", "late");
        queue.add(2, "before", "first-at-two");
        queue.add(2, "before", "second-at-two");

        List<String> actions = queue.drainRange("before", 0, 5).stream().map(GuiRenderPhaseQueue.Entry::action).toList();

        assertEquals(List.of("first-at-two", "second-at-two", "late"), actions);
        assertTrue(queue.isEmpty());
    }

    @Test
    void includesActionsAtBothRangeBoundaries() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(2, "before", "start");
        queue.add(4, "before", "end");
        queue.add(5, "before", "outside");

        List<String> actions = queue.drainRange("before", 2, 4).stream().map(GuiRenderPhaseQueue.Entry::action).toList();

        assertEquals(List.of("start", "end"), actions);
        assertEquals(1, queue.size());
    }

    @Test
    void separatesBeforeAndAfterBlurAtTheSameNumericIndex() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(3, "before", "before-action");
        queue.add(3, "after", "after-action");

        assertEquals(List.of("before-action"), queue.drainRange("before", 0, 3).stream().map(GuiRenderPhaseQueue.Entry::action).toList());
        assertEquals(List.of("after-action"), queue.drainRange("after", 3, 6).stream().map(GuiRenderPhaseQueue.Entry::action).toList());
    }

    @Test
    void drainsAnOtherwiseEmptyPhaseInBoundaryOrder() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(5, "before", "five");
        queue.add(0, "before", "zero");
        queue.add(1, "after", "other-phase");

        assertEquals(List.of("zero", "five"), queue.drainPhase("before").stream().map(GuiRenderPhaseQueue.Entry::action).toList());
        assertEquals(1, queue.size());
    }

    @Test
    void actionOnlyFrameDrainsInOriginalSubmissionOrder() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(9, "after", "first-submitted");
        queue.add(0, "before", "second-submitted");

        assertEquals(List.of("first-submitted", "second-submitted"), queue.drainAll().stream().map(GuiRenderPhaseQueue.Entry::action).toList());
        assertTrue(queue.isEmpty());
    }

    @Test
    void reversedRangeDoesNotConsumeActions() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(2, "before", "kept");

        assertTrue(queue.drainRange("before", 4, 2).isEmpty());
        assertEquals(1, queue.size());
    }

    @Test
    void clearRemovesEntriesAndResetsStableOrderCounter() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();
        queue.add(0, "before", "old");
        queue.clear();
        queue.add(0, "before", "new");

        GuiRenderPhaseQueue.Entry<String, String> entry = queue.drainAll().getFirst();
        assertEquals(0, entry.order());
        assertEquals("new", entry.action());
    }

    @Test
    void rejectsInvalidEntries() {
        GuiRenderPhaseQueue<String, String> queue = new GuiRenderPhaseQueue<>();

        assertThrows(IllegalArgumentException.class, () -> queue.add(-1, "before", "action"));
        assertThrows(NullPointerException.class, () -> queue.add(0, null, "action"));
        assertThrows(NullPointerException.class, () -> queue.add(0, "before", null));
    }

}
