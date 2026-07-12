package de.keksuccino.fancymenu.util.rendering;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuiDepthIsolationTest {

    @Test
    void flushesDrawBeforeClearingItsDepth() {
        List<String> operations = new ArrayList<>();

        GuiDepthIsolation.finishDepthWritingDraw(() -> operations.add("flush"), () -> operations.add("clearDepth"));

        assertEquals(List.of("flush", "clearDepth"), operations);
    }

    @Test
    void stillClearsDepthWhenFlushFails() {
        List<String> operations = new ArrayList<>();

        assertThrows(IllegalStateException.class, () -> GuiDepthIsolation.finishDepthWritingDraw(() -> {
            operations.add("flush");
            throw new IllegalStateException("failed draw");
        }, () -> operations.add("clearDepth")));

        assertEquals(List.of("flush", "clearDepth"), operations);
    }

}
