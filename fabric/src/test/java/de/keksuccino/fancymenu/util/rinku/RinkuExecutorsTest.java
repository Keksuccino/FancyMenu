package de.keksuccino.fancymenu.util.rinku;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RinkuExecutorsTest {

    @Test
    void createsNamedDaemonWorkersWithShutdownSafeSchedulingPolicies() throws Exception {
        ScheduledThreadPoolExecutor executor = RinkuExecutors.createExecutor("FancyMenu-Rinku-Test");
        try {
            assertTrue(executor.getRemoveOnCancelPolicy());
            assertFalse(executor.getExecuteExistingDelayedTasksAfterShutdownPolicy());
            assertFalse(executor.getContinueExistingPeriodicTasksAfterShutdownPolicy());
            WorkerProperties properties = executor.submit(() -> new WorkerProperties(Thread.currentThread().getName(), Thread.currentThread().isDaemon())).get(5L, TimeUnit.SECONDS);
            assertEquals("FancyMenu-Rinku-Test", properties.name());
            assertTrue(properties.daemon());
        } finally {
            executor.shutdownNow();
        }
    }

    private record WorkerProperties(String name, boolean daemon) {}

}
