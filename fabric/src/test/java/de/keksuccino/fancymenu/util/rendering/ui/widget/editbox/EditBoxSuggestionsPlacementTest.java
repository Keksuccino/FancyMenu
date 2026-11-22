package de.keksuccino.fancymenu.util.rendering.ui.widget.editbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditBoxSuggestionsPlacementTest {

    private static final int EDIT_BOX_HEIGHT = 20;
    private static final int SCREEN_HEIGHT = 250;
    private static final int FIVE_LINES = 5 * EditBoxSuggestions.LIST_LINE_HEIGHT;

    @Test
    void listGoesToItsOwnSideWhenItFitsThere() {
        int editBoxY = 120;
        assertEquals(above(editBoxY, FIVE_LINES), EditBoxSuggestions.getListYNextToEditBox(true, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
        assertEquals(below(editBoxY), EditBoxSuggestions.getListYNextToEditBox(false, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
    }

    @Test
    void listOfTheFirstFieldOfAWindowGoesBelowItBecauseItWouldGetCutOffAbove() {
        int editBoxY = 40;
        int listY = EditBoxSuggestions.getListYNextToEditBox(true, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT);
        assertEquals(below(editBoxY), listY);
    }

    @Test
    void listOfAFieldAtTheBottomGoesAboveItBecauseItWouldGetCutOffBelow() {
        int editBoxY = 200;
        int listY = EditBoxSuggestions.getListYNextToEditBox(false, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT);
        assertEquals(above(editBoxY, FIVE_LINES), listY);
    }

    @Test
    void listStaysAboveWhileItFitsExactly() {
        int editBoxY = FIVE_LINES + EditBoxSuggestions.LIST_SPACING + EditBoxSuggestions.LIST_EDGE_SPACE;
        assertEquals(above(editBoxY, FIVE_LINES), EditBoxSuggestions.getListYNextToEditBox(true, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
        assertEquals(below(editBoxY - 1), EditBoxSuggestions.getListYNextToEditBox(true, editBoxY - 1, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
    }

    @Test
    void listStaysBelowWhileItFitsExactly() {
        int editBoxY = SCREEN_HEIGHT - EditBoxSuggestions.LIST_EDGE_SPACE - FIVE_LINES - EditBoxSuggestions.LIST_SPACING - EDIT_BOX_HEIGHT;
        assertEquals(below(editBoxY), EditBoxSuggestions.getListYNextToEditBox(false, editBoxY, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
        assertEquals(above(editBoxY + 1, FIVE_LINES), EditBoxSuggestions.getListYNextToEditBox(false, editBoxY + 1, EDIT_BOX_HEIGHT, FIVE_LINES, SCREEN_HEIGHT));
    }

    @Test
    void listThatFitsNowhereGoesToTheSideWithMoreRoom() {
        int screenHeight = 100;
        int listHeight = 7 * EditBoxSuggestions.LIST_LINE_HEIGHT;
        assertEquals(below(20), EditBoxSuggestions.getListYNextToEditBox(true, 20, EDIT_BOX_HEIGHT, listHeight, screenHeight));
        assertEquals(above(60, listHeight), EditBoxSuggestions.getListYNextToEditBox(false, 60, EDIT_BOX_HEIGHT, listHeight, screenHeight));
    }

    @Test
    void listThatFitsNowhereWithTheSameRoomOnBothSidesGoesToItsOwnSide() {
        int screenHeight = 100;
        int editBoxY = (screenHeight - EDIT_BOX_HEIGHT) / 2;
        int listHeight = 7 * EditBoxSuggestions.LIST_LINE_HEIGHT;
        assertEquals(above(editBoxY, listHeight), EditBoxSuggestions.getListYNextToEditBox(true, editBoxY, EDIT_BOX_HEIGHT, listHeight, screenHeight));
        assertEquals(below(editBoxY), EditBoxSuggestions.getListYNextToEditBox(false, editBoxY, EDIT_BOX_HEIGHT, listHeight, screenHeight));
    }

    private static int above(int editBoxY, int listHeight) {
        return editBoxY - EditBoxSuggestions.LIST_SPACING - listHeight;
    }

    private static int below(int editBoxY) {
        return editBoxY + EDIT_BOX_HEIGHT + EditBoxSuggestions.LIST_SPACING;
    }

}
