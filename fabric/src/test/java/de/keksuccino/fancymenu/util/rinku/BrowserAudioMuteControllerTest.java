package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserAudioMuteControllerTest {

    @Test
    void initialAndUpdatedMuteStatesAreAppliedToTheBrowser() {
        List<Boolean> appliedStates = new ArrayList<>();

        BrowserAudioMuteController controller = new BrowserAudioMuteController(appliedStates::add, true);
        controller.setMuted(false);

        assertFalse(controller.isMuted());
        assertEquals(List.of(true, false), appliedStates);
    }

    @Test
    void currentMuteStateCanBeReappliedWhenNavigationStarts() {
        List<Boolean> appliedStates = new ArrayList<>();
        BrowserAudioMuteController controller = new BrowserAudioMuteController(appliedStates::add, false);

        controller.setMuted(true);
        controller.reapply();

        assertTrue(controller.isMuted());
        assertEquals(List.of(false, true, true), appliedStates);
    }

    @Test
    void failedNativeApplicationLeavesThePreviousStateRetryable() {
        AtomicBoolean rejectNextMute = new AtomicBoolean(true);
        List<Boolean> appliedStates = new ArrayList<>();
        BrowserAudioMuteController controller = new BrowserAudioMuteController(muted -> {
            if (muted && rejectNextMute.compareAndSet(true, false)) throw new IllegalStateException("CEF mute application failed");
            appliedStates.add(muted);
        }, false);

        assertThrows(IllegalStateException.class, () -> controller.setMuted(true));
        assertFalse(controller.isMuted());

        controller.setMuted(true);

        assertTrue(controller.isMuted());
        assertEquals(List.of(false, true), appliedStates);
    }

}
