package de.keksuccino.fancymenu.util.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialLoadingOverlayIconRefreshControllerTest {

    @Test
    void initialLoadingOverlayEntryDoesNotRefresh() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
    }

    @Test
    void initialLoadingOverlayExitRefreshesExactlyOnce() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(false));
    }

    @Test
    void replacementByNonLoadingOverlayCompletesInitialLifecycle() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void replacementByAnotherLoadingOverlayKeepsInitialLifecycleActive() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void nullTransitionsBeforeInitialLoadingOverlayAreIgnored() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void nonLoadingReplacementBeforeInitialLoadingOverlayIsIgnored() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void laterReloadLifecycleDoesNotRefreshAgain() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(false));
    }

    @Test
    void completedLifecycleIgnoresAllLaterOverlayTransitions() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(false));
    }

}
