package de.keksuccino.fancymenu.customization.action.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActionScriptEditorDragAutoScrollTest {

    private static final float AREA_TOP = 65.0F;
    private static final float AREA_BOTTOM = 400.0F;
    private static final float SCROLL_STEP = 0.05F;

    @Test
    void scrollsUpWhilePointerIsAboveTheList() {
        assertEquals(-SCROLL_STEP, delta(20));
        assertEquals(-SCROLL_STEP, delta(-30));
    }

    @Test
    void scrollsUpWhilePointerIsOnTheTopEdge() {
        assertEquals(-SCROLL_STEP, delta(65));
    }

    @Test
    void scrollsDownWhilePointerIsBelowTheList() {
        assertEquals(SCROLL_STEP, delta(450));
    }

    @Test
    void scrollsDownWhilePointerIsOnTheBottomEdge() {
        assertEquals(SCROLL_STEP, delta(400));
    }

    @Test
    void keepsScrollWhilePointerIsInsideTheList() {
        assertEquals(0.0F, delta(66));
        assertEquals(0.0F, delta(230));
        assertEquals(0.0F, delta(399));
    }

    @Test
    void comparesPointerWithFractionalListEdges() {
        assertEquals(-SCROLL_STEP, ActionScriptEditorWindowBody.getDragAutoScrollDelta(64, 64.5F, 400.5F, SCROLL_STEP));
        assertEquals(0.0F, ActionScriptEditorWindowBody.getDragAutoScrollDelta(65, 64.5F, 400.5F, SCROLL_STEP));
        assertEquals(0.0F, ActionScriptEditorWindowBody.getDragAutoScrollDelta(400, 64.5F, 400.5F, SCROLL_STEP));
        assertEquals(SCROLL_STEP, ActionScriptEditorWindowBody.getDragAutoScrollDelta(401, 64.5F, 400.5F, SCROLL_STEP));
    }

    @Test
    void scrollsByTheGivenStep() {
        assertEquals(-0.2F, ActionScriptEditorWindowBody.getDragAutoScrollDelta(0, AREA_TOP, AREA_BOTTOM, 0.2F));
        assertEquals(0.2F, ActionScriptEditorWindowBody.getDragAutoScrollDelta(500, AREA_TOP, AREA_BOTTOM, 0.2F));
    }

    private static float delta(int mouseY) {
        return ActionScriptEditorWindowBody.getDragAutoScrollDelta(mouseY, AREA_TOP, AREA_BOTTOM, SCROLL_STEP);
    }

}
