package de.keksuccino.fancymenu.util.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialLoadingOverlayIconRefreshControllerTest {

    @Test
    void refreshesWhenInitialLoadingOverlayIsRemoved() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(false));
    }

    @Test
    void refreshesWhenInitialLoadingOverlayIsReplacedByNonLoadingOverlay() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void doesNotRefreshBetweenLoadingOverlayReplacements() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
    }

    @Test
    void ignoresLaterResourceReloadOverlayLifecycle() {
        InitialLoadingOverlayIconRefreshController controller = new InitialLoadingOverlayIconRefreshController();

        assertFalse(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertTrue(controller.afterOverlayAssignment(false));
        assertFalse(controller.afterOverlayAssignment(true));
        assertFalse(controller.afterOverlayAssignment(false));
    }

}
