package de.keksuccino.fancymenu.util.resource.resources.video;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mp4VideoSoundEngineReloadCoordinatorTest {

    @Test
    void releaseRunsAfterReloadStateStartsAndRetryRunsAfterItCompletes() {
        Mp4VideoSoundEngineReloadCoordinator<String> coordinator = new Mp4VideoSoundEngineReloadCoordinator<>();
        coordinator.register("first");
        coordinator.register("second");
        List<String> released = new ArrayList<>();
        List<String> retried = new ArrayList<>();

        coordinator.beforeSoundEngineReload(instance -> {
            assertTrue(coordinator.isSoundEngineReloading());
            assertFalse(coordinator.hasSoundEngineReloadCompleted());
            released.add(instance);
        });
        coordinator.afterSoundEngineReload(instance -> {
            assertFalse(coordinator.isSoundEngineReloading());
            assertTrue(coordinator.hasSoundEngineReloadCompleted());
            retried.add(instance);
        });

        assertEquals(2, released.size());
        assertTrue(released.containsAll(List.of("first", "second")));
        assertEquals(2, retried.size());
        assertTrue(retried.containsAll(List.of("first", "second")));
    }

    @Test
    void duplicateRegistrationDoesNotDuplicateLifecycleCallbacks() {
        Mp4VideoSoundEngineReloadCoordinator<Object> coordinator = new Mp4VideoSoundEngineReloadCoordinator<>();
        Object video = new Object();
        coordinator.register(video);
        coordinator.register(video);
        List<Object> released = new ArrayList<>();

        coordinator.beforeSoundEngineReload(released::add);

        assertEquals(List.of(video), released);
    }

    @Test
    void instanceRegisteredDuringReloadIsOnlyRetriedAfterCompletion() {
        Mp4VideoSoundEngineReloadCoordinator<String> coordinator = new Mp4VideoSoundEngineReloadCoordinator<>();
        coordinator.register("existing");
        List<String> released = new ArrayList<>();
        List<String> retried = new ArrayList<>();

        coordinator.beforeSoundEngineReload(instance -> {
            released.add(instance);
            coordinator.register("created-during-reload");
        });
        coordinator.afterSoundEngineReload(retried::add);

        assertEquals(List.of("existing"), released);
        assertEquals(2, retried.size());
        assertTrue(retried.containsAll(List.of("existing", "created-during-reload")));
    }

    @Test
    void repeatedReloadCyclesReturnToCompletedState() {
        Mp4VideoSoundEngineReloadCoordinator<String> coordinator = new Mp4VideoSoundEngineReloadCoordinator<>();
        coordinator.register("video");

        coordinator.beforeSoundEngineReload(instance -> {});
        coordinator.afterSoundEngineReload(instance -> {});
        coordinator.beforeSoundEngineReload(instance -> {});

        assertTrue(coordinator.isSoundEngineReloading());
        assertTrue(coordinator.hasSoundEngineReloadCompleted());

        coordinator.afterSoundEngineReload(instance -> {});

        assertFalse(coordinator.isSoundEngineReloading());
        assertTrue(coordinator.hasSoundEngineReloadCompleted());
    }
}
